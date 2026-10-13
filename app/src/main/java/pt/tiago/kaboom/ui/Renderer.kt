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
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.Enemy
import pt.tiago.kaboom.game.EnemyKind
import pt.tiago.kaboom.game.Game
import pt.tiago.kaboom.game.Phase
import pt.tiago.kaboom.game.Tile
import pt.tiago.kaboom.ui.pixel.Art
import pt.tiago.kaboom.ui.pixel.Sprites.FlameKind
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Cada casa tem 16×16 píxeis de arte; [k] é a ampliação (inteira) para o ecrã. */
const val ART = 16

/**
 * Onde e a que escala o mapa é desenhado: a maior ampliação inteira que cabe,
 * centrada. Pixel art só fica nítida com ampliações inteiras.
 */
class BoardLayout(width: Float, height: Float) {
    val k = max(1, minOf(width / (Game.W * ART), height / (Game.H * ART)).toInt())
    val tile = ART * k
    val ox = ((width - Game.W * tile) / 2).toInt()
    val oy = ((height - Game.H * tile) / 2).toInt()
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

    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        sprite(if ((x + y) % 2 == 0) Art.floor else Art.floor2, x * tile, y * tile, k)
    }
    if (g.doorRevealed) {
        val img = if (g.doorOpen) Art.doorOpen[(t * 10).toInt() % 8] else Art.door
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
            Tile.WALL -> sprite(Art.wall, x * tile, y * tile, k)
            Tile.BLOCK -> {
                val b = g.burn[y][x]
                val img = if (b > 0f) {
                    Art.burning[((1f - b / Game.BURN) * 4).toInt().coerceIn(0, 3)]
                } else {
                    Art.brick
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
        sprite((if (hot) Art.bombHot else Art.bomb)[spark], b.x * tile, b.y * tile, k, scale = pop * beat)
    }

    drawFlames(g, k)
    for (e in g.enemies) drawEnemy(e, k, t)
    if (g.phase != Phase.GAME_OVER) drawPlayer(g, k)
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
    if (!e.alive) {
        if (e.dying < 0.45f) {
            // Pisca em branco antes de desaparecer
            val white = (e.dying * 14f).toInt() % 2 == 0
            val img = when (e.kind) {
                EnemyKind.BLOB -> if (white) Art.blobWhite else Art.blob
                EnemyKind.EYE -> if (white) Art.batWhite else Art.bat[0]
                EnemyKind.GHOST -> if (white) Art.ghostWhite else Art.ghost
            }
            sprite(img, x, y, k)
        } else {
            val f = ((e.dying - 0.45f) / 0.55f * 4).toInt().coerceIn(0, 3)
            sprite(Art.poof[f], x, y, k)
        }
        return
    }
    when (e.kind) {
        EnemyKind.BLOB -> sprite(Art.blob, x, y + if ((t * 4).toInt() % 2 == 0) 0 else k, k)
        EnemyKind.EYE -> sprite(Art.bat[(t * 8).toInt() % 2], x, y + if ((t * 4).toInt() % 2 == 0) -k else 0, k)
        EnemyKind.GHOST -> sprite(Art.ghost, x, y + intArrayOf(0, -1, -1, 0)[(t * 4).toInt() % 4] * k, k)
    }
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
