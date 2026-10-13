package pt.tiago.kaboom.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sintetizador chiptune (estilo NES): converte uma [SongDef] em som PCM mono de 16 bits.
 * Quatro canais: melodia (pulso), harmonia (pulso), baixo (triângulo) e bateria (ruído).
 * Código puro, sem Android — testado em JVM.
 */
object MusicSynth {
    const val RATE = 22050

    private const val LEAD = 0.17f
    private const val HARMONY = 0.07f
    private const val BASS = 0.24f
    private const val KICK = 0.36f
    private const val SNARE = 0.17f
    private const val HAT = 0.05f

    private val names = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

    /** "C#5" → nota MIDI (C4 = 60). */
    fun midi(note: String): Int {
        var i = 0
        var semi = names[note[i++]] ?: error("nota inválida: $note")
        if (i < note.length && note[i] == '#') { semi++; i++ }
        if (i < note.length && note[i] == 'b') { semi--; i++ }
        val octave = note.substring(i).toInt()
        return 12 * (octave + 1) + semi
    }

    fun freq(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** "F#m" → notas do acorde (fundamental, terceira, quinta) como meios-tons a partir de C. */
    fun chord(name: String): IntArray {
        var i = 0
        var root = names[name[i++]] ?: error("acorde inválido: $name")
        if (i < name.length && name[i] == '#') { root++; i++ }
        if (i < name.length && name[i] == 'b') { root--; i++ }
        val minor = i < name.length && name[i] == 'm'
        return intArrayOf(root, root + if (minor) 3 else 4, root + 7)
    }

    class Note(val midi: Int?, val steps: Int)

    /** Lê um compasso da melodia ("G5:2 R:4 ..."). */
    fun parseBar(bar: String): List<Note> = bar.trim().split(Regex("\\s+")).map { tok ->
        val (n, d) = tok.split(":")
        Note(if (n == "R") null else midi(n), d.toInt())
    }

    /** Acorde de cada meio compasso (para o baixo e os arpejos). */
    private fun halfBarChords(def: SongDef): List<IntArray> = def.chords.flatMap { c ->
        val parts = c.split("/")
        if (parts.size == 2) listOf(chord(parts[0]), chord(parts[1])) else listOf(chord(c), chord(c))
    }

    fun render(def: SongDef): ShortArray {
        val stepLen = RATE * 60.0 / def.bpm / 4.0
        val bars = def.lead.size
        val stepsPerPass = bars * 16
        val total = (stepsPerPass * 2 * stepLen).toInt()
        val out = FloatArray(total)
        val halves = halfBarChords(def)

        fun at(step: Int) = (step * stepLen).toInt()

        for (pass in 0..1) {
            val base = pass * stepsPerPass
            // Melodia e (2.ª volta) a mesma melodia uma oitava abaixo
            var step = 0
            for (bar in def.lead) for (n in parseBar(bar)) {
                if (n.midi != null) {
                    val start = at(base + step)
                    val len = at(base + step + n.steps) - start
                    pulse(out, start, len, freq(n.midi), def.duty, LEAD, vibrato = true)
                    if (pass == 1) pulse(out, start, len, freq(n.midi - 12), 0.5f, HARMONY * 1.2f, vibrato = false)
                }
                step += n.steps
            }
            for (s in 0 until stepsPerPass) {
                val chordNotes = halves[s / 8]
                val start = at(base + s)
                val len = at(base + s + 1) - start
                // Arpejos na 1.ª volta: fundamental, terceira, quinta, terceira...
                if (pass == 0) {
                    val tone = chordNotes[intArrayOf(0, 1, 2, 1)[s % 4]]
                    pulse(out, start, len, freq(12 * (def.arpOctave + 1) + tone), 0.5f, HARMONY, vibrato = false, pluck = true)
                }
                bass(def.bass, s, chordNotes)?.let { (midi, steps) ->
                    triangle(out, start, at(base + s + steps) - start, freq(midi), BASS)
                }
                when (def.drums[s % 16]) {
                    'K' -> kick(out, start)
                    'S' -> snare(out, start, s)
                    'h' -> hat(out, start, s)
                }
            }
        }

        // Mistura final com "limitador" suave, para nunca saturar
        val pcm = ShortArray(total)
        for (i in 0 until total) {
            val v = out[i]
            val soft = v / (1f + abs(v) * 0.6f)
            pcm[i] = (soft.coerceIn(-1f, 1f) * 30000f).toInt().toShort()
        }
        return pcm
    }

    /** Nota do baixo que começa no passo [s] (e quantos passos dura), ou null. */
    private fun bass(style: BassStyle, s: Int, chord: IntArray): Pair<Int, Int>? {
        val root = 12 * 3 + chord[0] // oitava 2
        return when (style) {
            BassStyle.SLOW -> when (s % 16) {
                0 -> root to 8
                8 -> root + 7 to 8
                else -> null
            }
            BassStyle.WALK -> when (s % 4) {
                0 -> intArrayOf(root, root + 7, root + 12, root + 7)[(s / 4) % 4] to 4
                else -> null
            }
            BassStyle.DRIVE -> if (s % 2 == 0) (if ((s / 2) % 4 == 2) root + 12 else root) to 2 else null
        }
    }

    private fun pulse(
        out: FloatArray, start: Int, len: Int, f: Double, duty: Float, gain: Float,
        vibrato: Boolean, pluck: Boolean = false,
    ) {
        var phase = 0.0
        val sounding = (len - RATE * 0.012).toInt().coerceAtLeast(len / 2) // pequena separação entre notas
        for (i in 0 until sounding) {
            val idx = start + i
            if (idx >= out.size) break
            val t = i.toDouble() / RATE
            val vib = if (vibrato && t > 0.15) 1.0 + 0.004 * sin(2 * PI * 6 * t) else 1.0
            phase += f * vib / RATE
            val frac = phase - phase.toInt()
            val wave = if (frac < duty) 1f else -1f
            val env = if (pluck) {
                exp(-t * 18).toFloat()
            } else {
                val attack = (t / 0.006).coerceAtMost(1.0)
                val decay = 0.65 + 0.35 * exp(-t * 10)
                val release = ((sounding - i).toDouble() / (RATE * 0.02)).coerceAtMost(1.0)
                (attack * decay * release).toFloat()
            }
            out[idx] += wave * gain * env
        }
    }

    private fun triangle(out: FloatArray, start: Int, len: Int, f: Double, gain: Float) {
        var phase = 0.0
        val sounding = (len - RATE * 0.01).toInt().coerceAtLeast(len / 2)
        for (i in 0 until sounding) {
            val idx = start + i
            if (idx >= out.size) break
            phase += f / RATE
            val frac = phase - phase.toInt()
            val wave = (4 * abs(frac - 0.5) - 1).toFloat()
            val release = ((sounding - i).toFloat() / (RATE * 0.015f)).coerceAtMost(1f)
            out[idx] += wave * gain * release
        }
    }

    private fun kick(out: FloatArray, start: Int) {
        val n = (RATE * 0.12).toInt()
        var phase = 0.0
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= out.size) break
            val t = i.toDouble() / RATE
            phase += (50 + 110 * exp(-t * 30)) / RATE
            out[idx] += (sin(2 * PI * phase) * exp(-t * 22)).toFloat() * KICK
        }
    }

    private fun snare(out: FloatArray, start: Int, seed: Int) {
        val rnd = Random(seed)
        val n = (RATE * 0.13).toInt()
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= out.size) break
            val t = i.toDouble() / RATE
            val noise = rnd.nextFloat() * 2 - 1
            val tone = sin(2 * PI * 185 * t).toFloat() * 0.5f
            out[idx] += (noise + tone * exp(-t * 30).toFloat()) * exp(-t * 24).toFloat() * SNARE
        }
    }

    private fun hat(out: FloatArray, start: Int, seed: Int) {
        val rnd = Random(seed * 31 + 7)
        val n = (RATE * 0.035).toInt()
        var prev = 0f
        for (i in 0 until n) {
            val idx = start + i
            if (idx >= out.size) break
            val t = i.toDouble() / RATE
            val noise = rnd.nextFloat() * 2 - 1
            val high = noise - prev // filtro passa-alto simples: só o "chiar"
            prev = noise
            out[idx] += high * exp(-t * 90).toFloat() * HAT
        }
    }
}
