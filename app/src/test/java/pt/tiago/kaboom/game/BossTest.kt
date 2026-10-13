package pt.tiago.kaboom.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BossTest {

    private val arena = listOf(
        "#############",
        "#P..........#",
        "#...........#",
        "#...........#",
        "#...........#",
        "#.....O.....#",
        "#...........#",
        "#...........#",
        "#...........#",
        "#############",
    )

    private fun game(kind: BossKind, rows: List<String> = arena) =
        Game(Random(5)).apply { loadMap(rows, kind) }

    private fun Game.run(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            update(1f / 60f)
            t += 1f / 60f
        }
    }

    /** Explosão com fogo do jogador na casa do boss. */
    private fun Game.hitBoss() {
        val b = boss!!
        bombs += Bomb(b.x.toInt(), b.y.toInt(), 1).apply { ownerInside = false; timer = 0.001f }
    }

    @Test
    fun arenaDoMundoCarregaOBoss() {
        val g = Game()
        g.newGame(Levels.PER_WORLD)
        val b = g.boss!!
        assertEquals(BossKind.KING_SLIME, b.kind)
        assertFalse(g.doorRevealed)
        assertFalse("porta fechada enquanto o boss vive", g.doorOpen)
        assertEquals("O REI GOTA", g.levelName)
    }

    @Test
    fun fogoTiraUmaVidaDeCadaVez() {
        val g = game(BossKind.YETI)
        g.player.invulnerable = 99f
        g.player.fireproof = 99f
        val hp = BossKind.YETI.hp
        g.hitBoss()
        g.update(0.01f)
        assertEquals(hp - 1, g.boss!!.hp)
        assertTrue(GameEvent.BOSS_HURT in g.events)
        // O mesmo fogo não volta a ferir enquanto pisca
        g.run(0.3f)
        assertEquals(hp - 1, g.boss!!.hp)
    }

    @Test
    fun derrotarOBossAbreAPorta() {
        val g = game(BossKind.KING_SLIME)
        g.player.invulnerable = 99f
        g.player.fireproof = 99f
        val b = g.boss!!
        b.hp = 1
        g.enemies += Enemy(EnemyKind.BLOB, 10, 7)
        g.hitBoss()
        g.update(0.01f)
        assertFalse(b.alive)
        assertTrue(GameEvent.BOSS_DEFEATED in g.events)
        assertFalse("ainda a explodir", g.doorRevealed)
        g.run(2f)
        assertTrue(b.defeated)
        assertTrue(g.doorRevealed)
        assertTrue("lacaios caíram com o boss e a porta abriu", g.doorOpen)
        assertEquals(Tile.FLOOR, g.tiles[g.doorY][g.doorX])
    }

    @Test
    fun tocarNoBossMata() {
        val g = game(BossKind.SORCERER)
        val b = g.boss!!
        b.cooldown = 99f
        g.player.x = b.x - 0.6f
        g.player.y = b.y
        g.update(0.01f)
        assertEquals(Phase.DYING, g.phase)
    }

    @Test
    fun escorpiaoInvesteQuandoAlinhadoEFicaTonto() {
        val g = game(BossKind.SCORPION)
        val b = g.boss!!
        b.cooldown = 0f
        g.player.invulnerable = 99f
        g.player.x = 1.5f
        g.player.y = b.y
        g.update(0.01f)
        assertEquals(BossState.CHARGE, b.state)
        g.run(0.7f)
        assertEquals(BossState.DASH, b.state)
        g.run(1.5f)
        assertEquals("bateu na parede", BossState.STUN, b.state)
        assertTrue("foi para a esquerda", b.x < 3f)
    }

    @Test
    fun yetiAterraOndeOJogadorEstava() {
        val g = game(BossKind.YETI)
        val b = g.boss!!
        b.cooldown = 0f
        g.player.x = 3.5f
        g.player.y = 7.5f
        g.update(0.01f)
        assertEquals(BossState.JUMP, b.state)
        assertTrue("no ar não se fere nem fere", b.airborne)
        g.run(1.2f)
        assertEquals(Phase.DYING, g.phase)
        assertEquals(3.5f, b.x, 0.01f)
    }

    @Test
    fun bombaMagicaNaoFereOFeiticeiroNemContaParaOJogador() {
        val g = game(BossKind.SORCERER)
        val b = g.boss!!
        g.player.fireproof = 99f
        g.player.invulnerable = 99f
        b.cooldown = 0f
        g.run(0.6f)
        val magic = g.bombs.singleOrNull { it.byBoss }
        assertNotNull("lançou a bomba mágica", magic)
        b.cooldown = 99f
        // O jogador (1 bomba) continua a poder pôr a sua
        g.player.x = 5.5f
        g.player.y = 1.5f
        g.requestBomb()
        g.update(0.01f)
        assertEquals(1, g.bombs.count { !it.byBoss })
        g.bombs.removeAll { !it.byBoss }
        // Mesmo que a bomba mágica rebente em cima dele, o boss não perde vida
        magic!!.x = b.x.toInt()
        magic.y = b.y.toInt()
        val hp = b.hp
        g.run(2f)
        assertEquals(hp, b.hp)
    }

    @Test
    fun reiGotaLargaGotas() {
        val g = game(BossKind.KING_SLIME)
        g.player.invulnerable = 99f
        g.boss!!.cooldown = 0f
        g.boss!!.vx = 1f
        g.boss!!.vy = 1f
        g.update(0.01f)
        assertTrue(g.enemies.isNotEmpty())
        assertTrue(GameEvent.BOSS_ATTACK in g.events)
    }
}
