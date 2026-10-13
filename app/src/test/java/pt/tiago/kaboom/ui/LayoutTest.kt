package pt.tiago.kaboom.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.kaboom.game.Game

class LayoutTest {
    // Samsung A26: 1080 px de largura; ~2030 px por baixo do HUD; 200 dp ≈ 525 px de comandos
    private val k = BoardLayout.scale(1080f, 2030f, 525f)

    @Test
    fun casasMaioresNoA26() {
        assertEquals(6, k)
        assertTrue("sobra espaço para os comandos", 2030 - Game.H * ART * k >= 525)
    }

    @Test
    fun camaraNuncaSaiDoTabuleiro() {
        val b = BoardLayout(1080f, k)
        val boardW = Game.W * b.tile
        for (i in 0..120) {
            val ox = b.originX(i / 10f)
            assertTrue(ox <= 0 && ox + boardW >= 1080)
            assertEquals("em passos de píxel de arte", 0, ox % k)
        }
        assertEquals(0, b.originX(1f))
        assertEquals(1080 - boardW, b.originX(Game.W - 1f))
    }

    @Test
    fun tabuleiroPequenoFicaCentrado() {
        val b = BoardLayout(1080f, 5)
        assertEquals((1080 - Game.W * 80) / 2, b.originX(3f))
        assertEquals(b.originX(3f), b.originX(9f))
    }
}
