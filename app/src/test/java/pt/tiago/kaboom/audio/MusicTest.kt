package pt.tiago.kaboom.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MusicTest {

    @Test
    fun notasEAcordes() {
        assertEquals(60, MusicSynth.midi("C4"))
        assertEquals(69, MusicSynth.midi("A4"))
        assertEquals(MusicSynth.midi("A#4"), MusicSynth.midi("Bb4"))
        assertEquals(440.0, MusicSynth.freq(69), 0.001)
        assertTrue(MusicSynth.chord("Am").contentEquals(intArrayOf(9, 12, 16)))
        assertTrue(MusicSynth.chord("F#").contentEquals(intArrayOf(6, 10, 13)))
    }

    @Test
    fun cadaCompassoSoma16() {
        for (song in Song.entries) {
            val d = song.def
            assertEquals("$song: um acorde por compasso", d.lead.size, d.chords.size)
            assertEquals("$song: bateria de 16 passos", 16, d.drums.length)
            assertTrue("$song: bateria só com K S h .", d.drums.all { it in "KSh." })
            d.lead.forEachIndexed { i, bar ->
                val sum = MusicSynth.parseBar(bar).sumOf { it.steps }
                assertEquals("$song compasso ${i + 1}: '$bar'", 16, sum)
            }
            d.chords.forEach { c -> c.split("/").forEach { MusicSynth.chord(it) } }
        }
    }

    @Test
    fun melodiasNumRegistoAgradavel() {
        for (song in Song.entries) for (bar in song.def.lead) for (n in MusicSynth.parseBar(bar)) {
            val m = n.midi ?: continue
            // Entre A4 (440 Hz) e A6 (1760 Hz): nem grave demais para o altifalante, nem estridente
            assertTrue("$song: ${n.midi} fora do registo", m in 57..93)
        }
    }

    @Test
    fun geraSomSemSaturarComDuracaoCerta() {
        for (song in Song.entries) {
            val d = song.def
            val pcm = MusicSynth.render(d)
            val seconds = d.lead.size * 4 * 2 * 60.0 / d.bpm
            assertEquals("$song: duração", seconds, pcm.size.toDouble() / MusicSynth.RATE, 0.01)
            val peak = pcm.maxOf { abs(it.toInt()) }
            assertTrue("$song: tem som ($peak)", peak > 5000)
            assertTrue("$song: não satura ($peak)", peak <= 30000)
        }
    }
}
