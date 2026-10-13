package pt.tiago.kaboom.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Garante que cada nível desenhado à mão é jogável. */
class LevelsTest {

    private fun cells(def: LevelDef, chars: String) = buildList {
        for ((y, row) in def.rows.withIndex()) for ((x, c) in row.withIndex()) if (c in chars) add(x to y)
    }

    @Test
    fun tamanhoEBordas() {
        for ((i, def) in Levels.all.withIndex()) {
            val n = "nível ${i + 1} (${def.name})"
            assertEquals("$n: linhas", Game.H, def.rows.size)
            def.rows.forEachIndexed { y, row ->
                assertEquals("$n linha $y: '$row'", Game.W, row.length)
                assertTrue("$n linha $y: caracteres desconhecidos em '$row'", row.all { it in "#.BPDbfskrhplEYGCNM" })
            }
            for (x in 0 until Game.W) {
                assertEquals("$n: borda de cima", '#', def.rows[0][x])
                assertEquals("$n: borda de baixo", '#', def.rows[Game.H - 1][x])
            }
            for (y in 0 until Game.H) {
                assertEquals("$n: borda esquerda", '#', def.rows[y][0])
                assertEquals("$n: borda direita", '#', def.rows[y][Game.W - 1])
            }
        }
    }

    @Test
    fun umJogadorUmaPortaEInimigos() {
        for ((i, def) in Levels.all.withIndex()) {
            val n = "nível ${i + 1}"
            assertEquals("$n: um 'P'", 1, cells(def, "P").size)
            assertEquals("$n: uma 'D'", 1, cells(def, "D").size)
            assertTrue("$n: tem inimigos", cells(def, "EYGCNM").isNotEmpty())
            assertTrue("$n: tem poderes", cells(def, "bfskrhpl").size >= 2)
        }
    }

    @Test
    fun inimigosComecamLonge() {
        for ((i, def) in Levels.all.withIndex()) {
            val (px, py) = cells(def, "P").single()
            for ((ex, ey) in cells(def, "EYGCNM")) {
                assertTrue("nível ${i + 1}: inimigo em ($ex,$ey) perto demais", abs(ex - px) + abs(ey - py) >= 5)
            }
        }
    }

    @Test
    fun chegaSeATodoOLado() {
        // Rebentando tijolos, todas as casas que não são parede têm de estar ligadas ao início.
        for ((i, def) in Levels.all.withIndex()) {
            val start = cells(def, "P").single()
            val seen = flood(def, start) { it != '#' }
            val open = cells(def, ".BPDbfskrhplEYGCNM")
            val lost = open.filter { it !in seen }
            assertTrue("nível ${i + 1}: casas isoladas $lost", lost.isEmpty())
        }
    }

    @Test
    fun haSempreParaOndeFugirDaPrimeiraBomba() {
        for ((i, def) in Levels.all.withIndex()) {
            val n = i + 1
            val (px, py) = cells(def, "P").single()
            val range = Levels.startRange(n)
            // Casas que se alcançam sem partir tijolos
            val walk = flood(def, px to py) { it in ".PEYGCNM" }
            val safe = walk.any { (x, y) ->
                val inLine = (x == px && abs(y - py) <= range) || (y == py && abs(x - px) <= range)
                !inLine
            }
            assertTrue("nível $n: não há fuga da primeira bomba", safe)
        }
    }

    @Test
    fun jogoCarregaONivelDesenhado() {
        val g = Game()
        g.newGame(1)
        val def = Levels.get(1)!!
        assertEquals(def.name, g.levelName)
        val (dx, dy) = cells(def, "D").single()
        assertEquals(dx, g.doorX)
        assertEquals(dy, g.doorY)
        assertEquals(Tile.BLOCK, g.tiles[dy][dx])
        assertEquals(cells(def, "EYGCNM").size, g.enemies.size)
        assertEquals(cells(def, "bfskrhpl").size, g.hiddenItems.size)
        assertEquals(1.5f, g.player.x)
    }

    @Test
    fun depoisDosDesenhadosVemOInfinito() {
        val g = Game()
        g.newGame(Levels.count + 1)
        assertEquals(null, g.levelName)
        assertTrue(g.enemies.isNotEmpty())
    }

    @Test
    fun quatroMundosDeOitoNiveis() {
        assertEquals(4, Levels.worlds.size)
        Levels.worlds.forEach { assertEquals(it.name, Levels.PER_WORLD, it.levels.size) }
        assertEquals("1-1", Levels.label(1))
        assertEquals("2-3", Levels.label(11))
        assertEquals("4-8", Levels.label(32))
        assertEquals("∞1", Levels.label(33))
        assertTrue(Levels.isWorldEnd(16))
        assertEquals("24 é o último do mundo 3 (índice 2)", 2, Levels.worldOf(24))
    }

    @Test
    fun comecarMaisAFrenteDaPoderes() {
        val g = Game()
        g.newGame(7)
        assertEquals(Levels.startBombs(7), g.player.maxBombs)
        assertEquals(Levels.startRange(7), g.player.range)
    }

    private fun flood(def: LevelDef, start: Pair<Int, Int>, passable: (Char) -> Boolean): Set<Pair<Int, Int>> {
        val seen = hashSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            for ((dx, dy) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                val nx = x + dx
                val ny = y + dy
                val c = def.rows.getOrNull(ny)?.getOrNull(nx) ?: continue
                if (passable(c) && seen.add(nx to ny)) queue += nx to ny
            }
        }
        return seen
    }
}
