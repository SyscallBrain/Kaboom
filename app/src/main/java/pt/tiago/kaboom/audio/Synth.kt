package pt.tiago.kaboom.audio

import pt.tiago.kaboom.game.GameEvent
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Efeitos sonoros estilo 8-bit, gerados por código: o jogo não precisa de ficheiros de áudio. */
object Synth {
    const val RATE = 22050

    // Tons acima de ~600 Hz: o altifalante de um telemóvel quase não reproduz graves.
    fun forEvent(e: GameEvent): FloatArray = when (e) {
        GameEvent.BOMB_PLACED -> sweep(1100f, 620f, 0.12f, 0.45f)
        GameEvent.EXPLOSION -> explosion()
        GameEvent.ITEM -> notes(floatArrayOf(880f, 1175f, 1568f, 2093f), 0.06f, 0.4f)
        GameEvent.ENEMY_DIED -> sweep(600f, 1800f, 0.18f, 0.4f)
        GameEvent.PLAYER_DIED -> sweep(1400f, 200f, 0.95f, 0.45f, vibrato = true)
        GameEvent.DOOR_OPEN -> notes(floatArrayOf(1046f, 1318f, 1568f), 0.1f, 0.4f)
        GameEvent.LEVEL_CLEAR -> notes(floatArrayOf(784f, 988f, 1175f, 1568f, 1175f, 1568f, 1976f), 0.11f, 0.4f)
        GameEvent.GAME_OVER -> notes(floatArrayOf(784f, 740f, 698f, 523f), 0.28f, 0.4f)
        GameEvent.KICK -> sweep(900f, 1500f, 0.07f, 0.4f)
        GameEvent.SHIELD_BREAK -> notes(floatArrayOf(2093f, 1568f, 1175f, 880f), 0.05f, 0.35f)
        GameEvent.ENEMY_HURT -> sweep(1600f, 900f, 0.1f, 0.35f)
        GameEvent.TELEPORT -> sweep(500f, 2200f, 0.25f, 0.3f, vibrato = true)
        GameEvent.BOSS_HURT -> sweep(1200f, 300f, 0.25f, 0.45f, vibrato = true)
        GameEvent.BOSS_DEFEATED -> notes(floatArrayOf(523f, 659f, 784f, 1046f, 1318f, 1568f, 2093f), 0.1f, 0.4f)
        GameEvent.BOSS_ATTACK -> sweep(700f, 1400f, 0.3f, 0.35f, vibrato = true)
        GameEvent.BOSS_LAND -> explosion()
    }

    private fun square(phase: Double) = if (sin(phase) >= 0) 1f else -1f

    private fun sweep(f0: Float, f1: Float, dur: Float, vol: Float, vibrato: Boolean = false): FloatArray {
        val n = (dur * RATE).toInt()
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / RATE
            val k = t / dur
            var f = f0 * (f1 / f0).pow(k)
            if (vibrato) f *= 1f + 0.04f * sin(2 * PI * 11 * t).toFloat()
            phase += 2 * PI * f / RATE
            val env = (1f - k).pow(1.4f) * minOf(1f, t * 200f)
            out[i] = square(phase) * vol * env
        }
        return out
    }

    private fun notes(freqs: FloatArray, each: Float, vol: Float): FloatArray {
        val per = (each * RATE).toInt()
        // A última nota fica a soar o dobro do tempo.
        val out = FloatArray(per * (freqs.size + 1))
        for ((j, f) in freqs.withIndex()) {
            val len = if (j == freqs.lastIndex) per * 2 else per
            var phase = 0.0
            for (i in 0 until len) {
                val t = i.toFloat() / RATE
                phase += 2 * PI * f / RATE
                val env = (1f - i.toFloat() / len).pow(0.8f) * minOf(1f, t * 300f)
                out[j * per + i] = square(phase) * vol * env
            }
        }
        return out
    }

    private fun explosion(): FloatArray {
        val dur = 0.85f
        val n = (dur * RATE).toInt()
        val out = FloatArray(n)
        val rnd = Random(7)
        var low = 0f
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toFloat() / RATE
            // Ruído filtrado cada vez mais grave: o "boom" a desvanecer
            val a = 0.5f * exp(-t * 4f) + 0.03f
            low += a * (rnd.nextFloat() * 2f - 1f - low)
            phase += 2 * PI * (70f * exp(-t * 3f) + 35f) / RATE
            val thump = sin(phase).toFloat() * exp(-t * 7f) * 0.7f
            val v = (low * 2.2f * exp(-t * 4.5f) + thump) * 0.8f
            out[i] = v.coerceIn(-1f, 1f)
        }
        return out
    }

    fun writeWav(file: File, samples: FloatArray) {
        val data = ByteBuffer.allocate(44 + samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVE".toByteArray())
        data.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(RATE).putInt(RATE * 2).putShort(2).putShort(16)
        data.put("data".toByteArray()).putInt(samples.size * 2)
        for (s in samples) data.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
        file.writeBytes(data.array())
    }
}
