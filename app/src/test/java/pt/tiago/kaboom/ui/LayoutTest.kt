package pt.tiago.kaboom.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.kaboom.game.Game

class LayoutTest {
    @Test
    fun noA26OTabuleiroCabeNaLarguraEDeixaEspacoAosComandos() {
        // Samsung A26: 1080 px de largura; ~2020 px por baixo do HUD; 160 dp ≈ 420 px de comandos
        val k = BoardLayout.scale(1080f, 2020f, 420f)
        assertEquals(5, k)
        assertTrue("cabe na largura", Game.W * ART * k <= 1080)
        assertTrue("sobra espaço para os comandos", 2020 - Game.H * ART * k >= 420)
    }

    @Test
    fun ecraBaixoEncolheEmVezDeTaparOsComandos() {
        val k = BoardLayout.scale(1080f, 1500f, 420f)
        assertTrue(1500 - Game.H * ART * k >= 420)
        assertEquals((1080 - Game.W * ART * k) / 2, BoardLayout(1080f, k).ox)
    }
}
