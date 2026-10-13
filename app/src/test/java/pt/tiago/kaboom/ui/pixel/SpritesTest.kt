package pt.tiago.kaboom.ui.pixel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.kaboom.game.ItemType

class SpritesTest {

    private fun all16(): Map<String, List<String>> = buildMap {
        Sprites.heroDown.forEachIndexed { i, g -> put("heroDown$i", g) }
        Sprites.heroUp.forEachIndexed { i, g -> put("heroUp$i", g) }
        Sprites.heroRight.forEachIndexed { i, g -> put("heroRight$i", g) }
        Sprites.heroLeft.forEachIndexed { i, g -> put("heroLeft$i", g) }
        put("heroDead", Sprites.heroDead)
        put("blob", Sprites.blob)
        put("bat1", Sprites.bat1)
        put("bat2", Sprites.bat2)
        put("ghost", Sprites.ghost)
        put("bomb1", Sprites.bomb1)
        put("bomb2", Sprites.bomb2)
        put("bombHot1", Sprites.bombHot1)
        put("brick", Sprites.brick)
        put("wall", Sprites.wall)
        put("floor", Sprites.floor)
        put("floor2", Sprites.floor2)
        put("door", Sprites.door)
        for (t in 0 until 8) put("doorOpen$t", Sprites.doorOpen(t))
        for (f in 0 until 4) put("burning$f", Sprites.burning(f))
        for (f in 0 until 4) put("poof$f", Sprites.poof(f))
        for (k in Sprites.FlameKind.entries) for (p in 0..2) put("flame$k$p", Sprites.flame(k, p))
        for (t in ItemType.entries) put("item$t", Sprites.item(t))
    }

    @Test
    fun spritesTem16x16() {
        for ((name, g) in all16()) {
            assertEquals("$name: linhas", 16, g.size)
            g.forEachIndexed { i, row -> assertEquals("$name linha $i: '$row'", 16, row.length) }
        }
    }

    @Test
    fun iconesTem10x10() {
        for (icon in listOf(Sprites.iconBomb, Sprites.iconFire, Sprites.iconSpeed, Sprites.iconHeart)) {
            assertEquals(10, icon.size)
            icon.forEach { assertEquals("'$it'", 10, it.length) }
        }
    }

    @Test
    fun todasAsLetrasTemCor() {
        for ((name, g) in all16()) {
            for (row in g) for (ch in row) {
                assertTrue("$name usa '$ch' sem cor na paleta", ch == '.' || ch in Palette.colors)
            }
        }
    }

    @Test
    fun contornoTomaCorEscuraDoVizinho() {
        // Contorno ao lado do azul do fato não pode ser o contorno "genérico"
        val px = Palette.rasterize(listOf("kb", ".."))
        val generic = (0xff shl 24) or Palette.colors.getValue('k')
        assertTrue(px[0] != generic)
        assertEquals(0, px[2]) // transparente
    }

    @Test
    fun espelhoInverteAsLinhas() {
        assertEquals(listOf("cba"), Sprites.mirror(listOf("abc")))
    }
}
