package pt.tiago.kaboom.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/** Poderes novos e inimigos novos. */
class PowersTest {

    private fun game(vararg rows: String) = Game(Random(3)).apply { loadMap(rows.toList()) }

    private fun Game.run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            update(1f / 60f)
            t += 1f / 60f
        }
    }

    @Test
    fun chutarBombaFazADeslizarAteAParede() {
        val g = game(
            "##########",
            "#P.......#",
            "##########",
        )
        g.player.kick = true
        g.bombs += Bomb(2, 1, 1).apply { ownerInside = false }
        g.input = Dir.RIGHT
        g.run(0.2f)
        g.input = null
        assertTrue("a bomba ficou a deslizar", g.bombs.single().slide != null || g.bombs.single().x > 2)
        assertTrue(GameEvent.KICK in g.events)
        g.run(1.5f)
        val b = g.bombs.singleOrNull()
        // Ou ainda não rebentou e está encostada à parede, ou rebentou lá
        if (b != null) assertEquals(8, b.x)
    }

    @Test
    fun semPoderDeChutarABombaNaoMexe() {
        val g = game(
            "##########",
            "#P.......#",
            "##########",
        )
        g.bombs += Bomb(2, 1, 1).apply { ownerInside = false }
        g.input = Dir.RIGHT
        g.run(0.5f)
        assertEquals(2, g.bombs.single().x)
        assertNull(g.bombs.single().slide)
    }

    @Test
    fun detonadorRemoto() {
        val g = game(
            "########",
            "#P.....#",
            "########",
        )
        g.player.remote = true
        g.requestBomb()
        g.update(0.01f)
        g.input = Dir.RIGHT
        g.run(Game.FUSE + 1f)
        assertEquals("não rebenta sozinha", 1, g.bombs.size)
        g.input = null
        g.requestDetonate()
        g.update(0.01f)
        assertTrue(g.bombs.isEmpty())
        assertTrue(GameEvent.EXPLOSION in g.events)
    }

    @Test
    fun escudoAguentaUmGolpe() {
        val g = game(
            "#####",
            "#P..#",
            "#####",
        )
        g.player.shield = true
        g.requestBomb()
        g.run(Game.FUSE + 0.3f)
        assertEquals("sobreviveu", Phase.PLAYING, g.phase)
        assertFalse(g.player.shield)
        assertTrue(GameEvent.SHIELD_BREAK in g.events)

        // Sem escudo, a próxima já mata
        g.run(Game.SHIELD_BREAK_TIME)
        g.requestBomb()
        g.run(Game.FUSE + 0.3f)
        assertEquals(Phase.DYING, g.phase)
    }

    @Test
    fun fogoPerfuranteAtravessaTijolos() {
        val g = game(
            "#########",
            "#P..BB..#",
            "#########",
        )
        g.bombs += Bomb(3, 1, 4, pierce = true).apply { ownerInside = false; timer = 0.01f }
        g.update(0.02f)
        assertTrue(g.burn[1][4] > 0f)
        assertTrue(g.burn[1][5] > 0f)
        assertTrue("passou para lá dos tijolos", g.flame[1][6] > 0f)

        val n = game(
            "#########",
            "#P..BB..#",
            "#########",
        )
        n.bombs += Bomb(3, 1, 4).apply { ownerInside = false; timer = 0.01f }
        n.update(0.02f)
        assertTrue(n.burn[1][4] > 0f)
        assertEquals("sem perfurar pára no primeiro", 0f, n.burn[1][5])
    }

    @Test
    fun vidaExtra() {
        val g = game(
            "#####",
            "#P..#",
            "#####",
        )
        val before = g.lives
        g.items[1 * Game.W + 2] = ItemType.LIFE
        g.input = Dir.RIGHT
        g.run(0.3f)
        assertEquals(before + 1, g.lives)
    }

    @Test
    fun bonecoDeNevePrecisaDeDuasExplosoes() {
        val g = game(
            "#############",
            "#P..........#",
            "#####.#######",
            "#############",
        )
        val snow = Enemy(EnemyKind.SNOWMAN, 5, 2)
        g.enemies += snow
        g.bombs += Bomb(5, 1, 1).apply { ownerInside = false; timer = 0.01f }
        g.update(0.02f)
        assertTrue("aguentou a primeira", snow.alive)
        assertEquals(1, snow.hp)
        assertTrue(GameEvent.ENEMY_HURT in g.events)
        assertTrue("ficou mais rápido", snow.speed > EnemyKind.SNOWMAN.speed)
        g.run(Game.FLAME + Game.HURT_TIME)
        g.bombs += Bomb(5, 1, 1).apply { ownerInside = false; timer = 0.01f }
        // Garante que está na casa do fogo
        snow.x = 5.5f; snow.y = 2.5f; snow.fromX = 5; snow.fromY = 2; snow.toX = 5; snow.toY = 2
        g.update(0.02f)
        assertFalse("a segunda mata", snow.alive)
    }

    @Test
    fun cobraAndaSempreEmFrente() {
        val g = game(
            "#############",
            "#P..........#",
            "#.#.#.#.#.#.#",
            "#...........#",
            "#############",
        )
        val snake = Enemy(EnemyKind.SNAKE, 11, 3).apply { dir = Dir.LEFT }
        g.enemies += snake
        g.player.invulnerable = 99f
        g.run(1.5f)
        // Passou por vários cruzamentos e continua na mesma linha, para a esquerda
        assertEquals(3, snake.cellY)
        assertTrue(snake.x < 8f)
    }

    @Test
    fun magoTeletransportaSeParaLongeDoJogador() {
        val g = game(
            "#############",
            "#P..........#",
            "#...........#",
            "#...........#",
            "#...........#",
            "#############",
        )
        val wiz = Enemy(EnemyKind.WIZARD, 6, 3).apply { teleportIn = 0f }
        g.enemies += wiz
        g.player.invulnerable = 99f
        g.update(0.02f)
        assertTrue(GameEvent.TELEPORT in g.events)
        val dist = abs(wiz.cellX - g.player.cellX) + abs(wiz.cellY - g.player.cellY)
        assertTrue("longe do jogador: $dist", dist >= 4)
        assertTrue("tempo até ao próximo foi reposto", wiz.teleportIn > 2f)
    }
}
