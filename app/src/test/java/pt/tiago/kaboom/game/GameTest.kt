package pt.tiago.kaboom.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {

    private fun game(vararg rows: String) = Game(Random(1)).apply { loadMap(rows.toList()) }

    /** Avança o jogo em passos de 1/60 s, como no telemóvel. */
    private fun Game.run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            update(1f / 60f)
            t += 1f / 60f
        }
    }

    @Test
    fun bombaRebentaBlocoAoLadoMasNaoOQueEstaMaisLonge() {
        val g = game(
            "#######",
            "#P.BB.#",
            "#######",
        )
        g.input = Dir.RIGHT
        g.run(0.32f) // anda até à casa (2,1)
        g.input = null
        assertEquals(2, g.player.cellX)
        g.requestBomb()
        g.update(0.01f)
        assertEquals(1, g.bombs.size)
        g.input = Dir.LEFT
        g.run(Game.FUSE + Game.BURN + 0.1f)
        assertEquals(Tile.FLOOR, g.tiles[1][3])
        assertEquals("alcance 1 não chega ao segundo bloco", Tile.BLOCK, g.tiles[1][4])
    }

    @Test
    fun explosaoEmCadeia() {
        val g = game(
            "#########",
            "#P......#",
            "#########",
        )
        g.bombs += Bomb(4, 1, 1).apply { ownerInside = false; timer = 0.05f }
        g.bombs += Bomb(5, 1, 1).apply { ownerInside = false; timer = 99f }
        g.update(0.1f)
        assertTrue("as duas bombas rebentaram no mesmo frame", g.bombs.isEmpty())
        assertTrue(g.flame[1][6] > 0f)
    }

    @Test
    fun paredesTravamOFogo() {
        val g = game(
            "#####",
            "#P..#",
            "###.#",
            "#####",
        )
        g.bombs += Bomb(3, 1, 3).apply { ownerInside = false; timer = 0.01f }
        g.update(0.02f)
        assertTrue(g.flame[2][3] > 0f)
        assertEquals(0f, g.flame[3][3])
    }

    @Test
    fun naoAtravessaParedes() {
        val g = game(
            "###",
            "#P#",
            "###",
        )
        g.input = Dir.LEFT
        g.run(1f)
        assertEquals(1.5f, g.player.x, 0.0001f)
        g.input = Dir.UP
        g.run(1f)
        assertEquals(1.5f, g.player.y, 0.0001f)
    }

    @Test
    fun deslizaParaOCarrilLivreNasEsquinas() {
        val g = game(
            "#####",
            "#P..#",
            "#.#.#",
            "#####",
        )
        // Meio desalinhado, a tocar na parede (2,2): carregar para a direita
        // tem de o puxar para cima, para a linha 1, e depois seguir.
        g.player.y = 2.3f
        g.input = Dir.RIGHT
        g.run(0.5f)
        assertEquals(1.5f, g.player.y, 0.0001f)
        assertTrue(g.player.x > 1.6f)
    }

    @Test
    fun saiDaPropriaBombaMasNaoVolta() {
        val g = game(
            "######",
            "#P...#",
            "######",
        )
        g.requestBomb()
        g.update(0.01f)
        g.input = Dir.RIGHT
        g.run(0.5f)
        assertTrue(g.player.x >= 2.5f)
        g.input = Dir.LEFT
        g.run(1f)
        assertEquals("pára antes de entrar na casa da bomba", 2.5f, g.player.x, 0.0001f)
    }

    @Test
    fun inimigoNoFogoMorreEAPortaAbre() {
        val g = game(
            "#############",
            "#P..........#",
            "#####.#######",
            "#####E#######",
            "#############",
            "#D...........",
        )
        g.bombs += Bomb(5, 2, 1).apply { ownerInside = false; timer = 0.05f }
        assertFalse(g.doorOpen)
        g.run(0.2f)
        assertTrue(g.doorOpen)
        assertTrue(GameEvent.ENEMY_DIED in g.events)
        assertTrue(GameEvent.DOOR_OPEN in g.events)
        assertEquals(100, g.score)
    }

    @Test
    fun fogoMataOJogadorEPerdeUmaVida() {
        val g = game(
            "#####",
            "#P..#",
            "#####",
        )
        g.requestBomb()
        g.run(Game.FUSE + 0.1f)
        assertEquals(Phase.DYING, g.phase)
        g.run(Game.DEATH_TIME + 0.1f)
        assertEquals(Game.START_LIVES - 1, g.lives)
        assertEquals(Phase.START, g.phase)
    }

    @Test
    fun ultimaVidaDaFimDeJogo() {
        val g = game(
            "#####",
            "#P..#",
            "#####",
        )
        repeat(Game.START_LIVES) {
            g.loadMap(listOf("#####", "#P..#", "#####"))
            g.requestBomb()
            g.run(Game.FUSE + Game.DEATH_TIME + 0.3f)
        }
        assertEquals(Phase.GAME_OVER, g.phase)
        assertTrue(GameEvent.GAME_OVER in g.events)
    }

    @Test
    fun escudoAoRenascerProtegeDosInimigosMasNaoDoFogo() {
        val g = game(
            "#######",
            "#P.E#",
            "#######",
        )
        g.player.invulnerable = 1f
        g.run(0.5f)
        assertEquals("inimigo encostado não o mata com escudo", Phase.PLAYING, g.phase)
        g.run(1f)
        assertEquals("sem escudo, o inimigo já mata", Phase.DYING, g.phase)

        val f = game(
            "#####",
            "#P..#",
            "#####",
        )
        f.player.invulnerable = 99f
        f.requestBomb()
        f.run(Game.FUSE + 0.1f)
        assertEquals(Phase.DYING, f.phase)
    }

    @Test
    fun efeitosVisuaisSaoEmitidos() {
        val g = game(
            "#######",
            "#P..B.#",
            "#######",
        )
        g.bombs += Bomb(3, 1, 1).apply { ownerInside = false; timer = 0.01f }
        g.update(0.02f)
        assertTrue(g.fx.any { it.kind == FxKind.EXPLOSION && it.x == 3.5f })
        assertTrue(g.fx.any { it.kind == FxKind.BLOCK_BROKEN && it.x == 4.5f })
    }

    @Test
    fun apanharItemDaPoder() {
        val g = game(
            "#####",
            "#P..#",
            "#####",
        )
        g.items[1 * Game.W + 2] = ItemType.FIRE
        g.input = Dir.RIGHT
        g.run(0.3f)
        assertEquals(2, g.player.range)
        assertTrue(g.items.isEmpty())
    }

    @Test
    fun entrarNaPortaAbertaPassaDeNivel() {
        val g = game(
            "#####",
            "#PD.#",
            "#####",
        )
        g.input = Dir.RIGHT
        g.run(0.4f)
        g.input = null
        assertEquals(Phase.LEVEL_CLEAR, g.phase)
        g.run(Game.CLEAR_TIME + 0.1f)
        assertEquals(2, g.level)
        assertEquals(Phase.START, g.phase)
    }

    @Test
    fun niveisGeradosSaoJogaveis() {
        repeat(30) { seed ->
            val g = Game(Random(seed))
            g.newGame(Levels.count + 1) // depois dos desenhados à mão vêm os gerados
            // Zona de partida livre para pôr a primeira bomba e fugir
            for ((x, y) in listOf(1 to 1, 2 to 1, 1 to 2)) assertEquals(Tile.FLOOR, g.tiles[y][x])
            // Porta escondida debaixo de um bloco e inimigos longe do jogador
            assertEquals(Tile.BLOCK, g.tiles[g.doorY][g.doorX])
            assertTrue(g.enemies.isNotEmpty())
            assertTrue(g.enemies.all { it.cellX + it.cellY >= 9 })
            assertEquals(3, g.hiddenItems.size)
            assertFalse(g.hiddenItems.containsKey(g.doorY * Game.W + g.doorX))
        }
    }
}
