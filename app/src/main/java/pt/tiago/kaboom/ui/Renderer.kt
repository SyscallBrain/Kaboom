package pt.tiago.kaboom.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import pt.tiago.kaboom.game.Boss
import pt.tiago.kaboom.game.BossKind
import pt.tiago.kaboom.game.BossState
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.Enemy
import pt.tiago.kaboom.game.EnemyKind
import pt.tiago.kaboom.game.Game
import pt.tiago.kaboom.game.Levels
import pt.tiago.kaboom.game.Phase
import pt.tiago.kaboom.game.Tile
import pt.tiago.kaboom.ui.pixel.Art
import pt.tiago.kaboom.ui.pixel.Sprites.FlameKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Cada casa tem 16×16 píxeis de arte; [k] é a ampliação (inteira) para o ecrã. */
const val ART = 16

/**
 * Onde e a que escala o mapa é desenhado: a maior ampliação inteira que cabe,
 * centrada. Pixel art só fica nítida com ampliações inteiras.
 */
class BoardLayout(private val width: Float, val k: Int) {
    val tile = ART * k
    private val boardW = Game.W * tile

    /**
     * Onde começa o tabuleiro no ecrã: centrado se cabe na largura; senão a câmara
     * segue [focusX] (em casas), sem sair do tabuleiro e sempre em passos de píxel de arte.
     */
    fun originX(focusX: Float): Int =
        if (boardW <= width) ((width - boardW) / 2).toInt()
        else -(((focusX * tile - width / 2).coerceIn(0f, boardW - width)).roundToInt() / k * k)

    companion object {
        /** Mínimo de colunas visíveis quando a câmara desliza. */
        const val MIN_COLS = 11

        /**
         * A maior ampliação que deixa pelo menos [minControls] px por baixo para os comandos
         * e mostra pelo menos [MIN_COLS] colunas. Pode não caber na largura: a câmara desliza.
         */
        fun scale(width: Float, height: Float, minControls: Float): Int {
            val byHeight = ((height - minControls) / (Game.H * ART)).toInt()
            val byWidth = (width / (MIN_COLS * ART)).toInt()
            return max(1, minOf(byHeight, byWidth))
        }
    }
}

/** Desenha um sprite ampliado [k] vezes, sem suavização. */
fun DrawScope.sprite(img: ImageBitmap, x: Int, y: Int, k: Int, alpha: Float = 1f, scale: Float = 1f) {
    val w = (img.width * k * scale).roundToInt()
    val h = (img.height * k * scale).roundToInt()
    val dx = x + (img.width * k - w) / 2
    val dy = y + (img.height * k - h) / 2
    drawImage(
        img,
        dstOffset = IntOffset(dx, dy),
        dstSize = IntSize(w, h),
        alpha = alpha,
        filterQuality = FilterQuality.None,
    )
}

/** Posição no ecrã (canto do sprite) de algo centrado em [c] casas, alinhada à grelha de arte. */
private fun snap(c: Float, k: Int) = ((c * ART - ART / 2f).roundToInt()) * k

fun DrawScope.drawGame(g: Game, k: Int) {
    val t = g.time
    val tile = ART * k
    val ts = Art.tiles(Art.themeOf(Levels.worldOf(g.level)))

    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        sprite(if ((x + y) % 2 == 0) ts.floor else ts.floor2, x * tile, y * tile, k)
    }
    if (g.doorRevealed) {
        val img = if (g.doorOpen) ts.doorOpen[(t * 10).toInt() % 8] else ts.door
        sprite(img, g.doorX * tile, g.doorY * tile, k)
    }
    for ((key, type) in g.items) {
        val bob = if ((t * 3).toInt() % 2 == 0) 0 else -k
        sprite(Art.item(type), (key % Game.W) * tile, (key / Game.W) * tile + bob, k)
    }

    // Sombra de 2 píxeis de arte para baixo e para a direita das paredes e tijolos
    val shadow = Color.Black.copy(alpha = 0.28f)
    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        if (g.tiles[y][x] != Tile.FLOOR) {
            drawRect(shadow, Offset((x * tile + 2 * k).toFloat(), (y * tile + 2 * k).toFloat()), Size(tile.toFloat(), tile.toFloat()))
        }
    }
    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        when (g.tiles[y][x]) {
            Tile.WALL -> sprite(ts.wall, x * tile, y * tile, k)
            Tile.BLOCK -> {
                val b = g.burn[y][x]
                val img = if (b > 0f) {
                    ts.burning[((1f - b / Game.BURN) * 4).toInt().coerceIn(0, 3)]
                } else {
                    ts.brick
                }
                sprite(img, x * tile, y * tile, k)
            }
            Tile.FLOOR -> Unit
        }
    }

    for (b in g.bombs) {
        val urgency = 1f - b.timer / Game.FUSE
        val spark = (t * 10).toInt() % 2
        val hot = urgency > 0.7f && (t * 12).toInt() % 2 == 0
        // Ao ser posta, a bomba "salta" (easeOutBack); depois pulsa cada vez mais depressa
        val a = (Game.FUSE - b.timer) / 0.2f
        val e = a.coerceIn(0f, 1f) - 1f
        val pop = 1f + 2.7f * e * e * e + 1.7f * e * e
        val beat = if ((t * (3f + urgency * 9f)).toInt() % 2 == 0) 1f else 1.08f
        // A deslizar (chutada): desenha-se entre a casa onde está e a seguinte
        val sx = b.slide?.let { (it.dx * b.slideT * ART).roundToInt() * k } ?: 0
        val sy = b.slide?.let { (it.dy * b.slideT * ART).roundToInt() * k } ?: 0
        val img = when {
            b.byBoss -> Art.bombMagic[spark]
            b.remote -> Art.bombRemote[(t * 3).toInt() % 2]
            hot -> Art.bombHot[spark]
            else -> Art.bomb[spark]
        }
        val scale = if (b.remote) pop else pop * beat
        sprite(img, b.x * tile + sx, b.y * tile + sy, k, scale = scale)
    }

    drawFlames(g, k)
    for (e in g.enemies) drawEnemy(e, k, t)
    // O yeti no ar é desenhado por cima de tudo (incluindo o jogador)
    g.boss?.let { if (!it.airborne) drawBoss(it, k, t) }
    if (g.phase != Phase.GAME_OVER) drawPlayer(g, k)
    g.boss?.let { if (it.airborne) drawBoss(it, k, t) }
}

private fun DrawScope.drawBoss(b: Boss, k: Int, t: Float) {
    if (b.defeated) return
    val size = 32
    val x = ((b.x * ART - size / 2f).roundToInt()) * k
    var y = ((b.y * ART - size / 2f).roundToInt()) * k
    if (b.airborne) {
        // Sombra no sítio onde vai aterrar, a crescer; o yeti sobe e desce num arco
        val p = (b.stateTime / 1.1f).coerceIn(0f, 1f)
        val sx = ((b.jumpToX * ART).roundToInt()) * k.toFloat()
        val sy = ((b.jumpToY * ART + 10).roundToInt()) * k.toFloat()
        val w = (14 + 10 * p) * k
        drawOval(Color.Black.copy(alpha = 0.25f + 0.25f * p), Offset(sx - w, sy - w * 0.35f), Size(w * 2, w * 0.7f))
        val lift = (sin(p * PI.toFloat()) * 3.2f * ART).roundToInt() * k
        y -= lift
    } else {
        drawOval(Color.Black.copy(alpha = 0.3f), Offset(x + 4f * k, y + 27f * k), Size(24f * k, 6f * k))
    }
    val frame = when (b.kind) {
        BossKind.YETI -> if (b.airborne || b.state == BossState.STUN && b.stateTime < 0.3f) 1 else (t * 2).toInt()
        BossKind.SCORPION -> if (b.state == BossState.DASH) 1 else (t * 3).toInt()
        else -> (t * 3).toInt()
    }
    val dyingAlpha = if (b.alive) 1f else 1f - (b.dying / 1.6f).coerceIn(0f, 1f)
    // Aviso antes da investida e piscar quando é ferido
    val flash = (b.state == BossState.CHARGE && (t * 16).toInt() % 2 == 0) ||
        (b.hurt > 0f && (b.hurt * 14f).toInt() % 2 == 0) ||
        (!b.alive && (b.dying * 12f).toInt() % 2 == 0)
    val shake = if (b.state == BossState.CHARGE || b.state == BossState.STUN && b.kind == BossKind.SCORPION) ((t * 40).toInt() % 3 - 1) * k else 0
    sprite(if (flash) Art.bossWhite(b.kind) else Art.boss(b.kind, frame), x + shake, y, k, alpha = dyingAlpha)
    if (b.state == BossState.STUN) {
        // Estrelinhas de tonto
        for (i in 0 until 3) {
            val a = t * 5f + i * 2.1f
            val sx = x + (16 + cos(a) * 12).roundToInt() * k
            val sy = y + (2 + sin(a) * 3).roundToInt() * k
            drawRect(Color(0xFFFFE35A), Offset(sx.toFloat(), sy.toFloat()), Size(2f * k, 2f * k))
        }
    }
}

private fun DrawScope.drawFlames(g: Game, k: Int) {
    val tile = ART * k
    fun on(x: Int, y: Int, h: Boolean): Boolean {
        if (x !in 0 until Game.W || y !in 0 until Game.H || g.flame[y][x] <= 0f) return false
        return if (h) g.flameH[y][x] else g.flameV[y][x]
    }
    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        val left = g.flame[y][x]
        if (left <= 0f) continue
        val h = g.flameH[y][x]
        val v = g.flameV[y][x]
        val kind = when {
            h && v -> FlameKind.CENTER
            h && !on(x - 1, y, true) -> FlameKind.LEFT
            h && !on(x + 1, y, true) -> FlameKind.RIGHT
            h -> FlameKind.H
            v && !on(x, y - 1, false) -> FlameKind.UP
            v && !on(x, y + 1, false) -> FlameKind.DOWN
            else -> FlameKind.V
        }
        // Cresce depressa, fica grande, e encolhe no fim
        val ph = 1f - left / Game.FLAME
        val phase = when {
            ph < 0.1f -> 1
            ph < 0.5f -> 2
            ph < 0.8f -> 1
            else -> 0
        }
        sprite(Art.flame(kind, phase), x * tile, y * tile, k)
    }
}

private fun DrawScope.drawEnemy(e: Enemy, k: Int, t: Float) {
    val x = snap(e.x, k)
    val y = snap(e.y, k)
    val wounded = e.hp < e.kind.hp
    if (!e.alive) {
        if (e.dying < 0.45f) {
            // Pisca em branco antes de desaparecer
            val white = (e.dying * 14f).toInt() % 2 == 0
            sprite(if (white) Art.enemyWhite(e.kind, wounded) else Art.enemy(e.kind, 0, wounded), x, y, k)
        } else {
            val f = ((e.dying - 0.45f) / 0.55f * 4).toInt().coerceIn(0, 3)
            sprite(Art.poof[f], x, y, k)
        }
        return
    }
    // Acabou de levar uma explosão mas aguentou: pisca em branco
    if (e.hurt > 0f && (e.hurt * 14f).toInt() % 2 == 0) {
        sprite(Art.enemyWhite(e.kind, wounded), x, y, k)
        return
    }
    val step = (t * 4).toInt()
    val (frame, bob) = when (e.kind) {
        EnemyKind.BLOB -> 0 to (if (step % 2 == 0) 0 else 1)
        EnemyKind.EYE -> (t * 8).toInt() to (if (step % 2 == 0) -1 else 0)
        EnemyKind.GHOST -> 0 to intArrayOf(0, -1, -1, 0)[step % 4]
        EnemyKind.SNAKE -> (t * 6).toInt() to 0
        EnemyKind.SNOWMAN -> 0 to (if (step % 2 == 0) 0 else 1)
        EnemyKind.WIZARD -> (t * 3).toInt() to intArrayOf(0, -1, -1, 0)[step % 4]
    }
    sprite(Art.enemy(e.kind, frame, wounded), x, y + bob * k, k)
}

private fun DrawScope.drawPlayer(g: Game, k: Int) {
    val p = g.player
    val x = snap(p.x, k)
    val y = snap(p.y, k)
    when {
        g.phase == Phase.LEVEL_CLEAR -> {
            // Rodopia (vira-se para os 4 lados) e desaparece na porta
            val spin = listOf(Dir.DOWN, Dir.RIGHT, Dir.UP, Dir.LEFT)[(g.phaseTime * 12).toInt() % 4]
            val alpha = 1f - ((g.phaseTime - 0.6f) / 0.6f).coerceIn(0f, 1f)
            if (alpha > 0f) sprite(Art.hero(spin, 0), x, y, k, alpha)
        }
        p.dying >= 0f -> {
            val white = p.dying < 0.35f && (p.dying * 14f).toInt() % 2 == 0
            val alpha = 1f - ((p.dying - 1.0f) / 0.5f).coerceIn(0f, 1f)
            if (alpha > 0f) sprite(if (white) Art.heroDeadWhite else Art.heroDead, x, y, k, alpha)
        }
        // A piscar enquanto tem o escudo de quem acabou de renascer
        p.invulnerable > 0f && (g.time * 14f).toInt() % 2 == 0 -> Unit
        else -> {
            val frame = if (p.moving) (p.walk * 4f).toInt() % 4 else 0
            sprite(Art.hero(p.facing, frame), x, y, k)
            if (p.shield) sprite(Art.shieldBubble, x, y, k, alpha = 0.6f + 0.3f * ((g.time * 4).toInt() % 2))
        }
    }
}

/**
 * Transição em círculo (como nos jogos antigos): fecha sobre o jogador ao sair do nível
 * ou ao morrer, e abre a partir dele no início do nível seguinte. [center] em píxeis do ecrã.
 */
fun DrawScope.drawIris(g: Game, center: Offset) {
    val k = when (g.phase) {
        Phase.START -> (g.phaseTime / 0.7f).coerceIn(0f, 1f)
        Phase.LEVEL_CLEAR -> 1f - ((g.phaseTime - 1.3f) / 0.9f).coerceIn(0f, 1f)
        Phase.DYING -> 1f - ((g.phaseTime - 1.4f) / 0.7f).coerceIn(0f, 1f)
        else -> 1f
    }
    if (k >= 1f) return
    val maxR = sqrt(size.width * size.width + size.height * size.height)
    val r = maxR * k * k
    val band = maxR * 2f
    drawCircle(Color.Black, r + band / 2, center, style = Stroke(band))
}
