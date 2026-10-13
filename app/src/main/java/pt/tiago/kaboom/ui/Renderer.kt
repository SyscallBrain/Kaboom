package pt.tiago.kaboom.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.Enemy
import pt.tiago.kaboom.game.EnemyKind
import pt.tiago.kaboom.game.Game
import pt.tiago.kaboom.game.ItemType
import pt.tiago.kaboom.game.Phase
import pt.tiago.kaboom.game.Tile
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Pal {
    val grass1 = Color(0xFF3F8F3F)
    val grass2 = Color(0xFF398537)
    val grassTuft = Color(0xFF4CA64A)
    val wall = Color(0xFF6B7280)
    val wallLight = Color(0xFF9CA3AF)
    val wallDark = Color(0xFF4B5563)
    val brick = Color(0xFFB45F2A)
    val brickLight = Color(0xFFD9824A)
    val mortar = Color(0xFF6E3515)
    val bomb = Color(0xFF1F2937)
    val fuse = Color(0xFF92400E)
    val flameOuter = Color(0xFFF97316)
    val flameMid = Color(0xFFFDE047)
    val flameCore = Color(0xFFFFFBEB)
    val suit = Color(0xFF2563EB)
    val belt = Color(0xFF1E3A8A)
    val boots = Color(0xFF3F2A1D)
    val skin = Color(0xFFF5C99B)
    val helmet = Color(0xFFFACC15)
    val helmetDark = Color(0xFFCA8A04)
    val blob = Color(0xFFEC4899)
    val eyeBat = Color(0xFF8B5CF6)
    val ghost = Color(0xFFF1F5F9)
}

private val TAU = (2 * PI).toFloat()

fun DrawScope.drawGame(g: Game) {
    val s = size.width / Game.W
    val t = g.time

    drawFloor(s)
    if (g.doorRevealed) drawDoor(g.doorX * s, g.doorY * s, s, g.doorOpen, t)
    for ((key, type) in g.items) drawItem((key % Game.W) * s, (key / Game.W) * s, s, type, t)

    // Sombras para dar volume às paredes e blocos
    val shadow = Color.Black.copy(alpha = 0.22f)
    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        if (g.tiles[y][x] != Tile.FLOOR) {
            drawRect(shadow, Offset(x * s + s * 0.1f, y * s + s * 0.12f), Size(s, s))
        }
    }
    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        when (g.tiles[y][x]) {
            Tile.WALL -> drawWall(x * s, y * s, s)
            Tile.BLOCK -> {
                val b = g.burn[y][x]
                if (b > 0f) drawBurning(x * s, y * s, s, 1f - b / Game.BURN, t) else drawBrick(x * s, y * s, s)
            }
            Tile.FLOOR -> Unit
        }
    }

    for (b in g.bombs) drawBomb(b.x * s + s / 2, b.y * s + s / 2, s, 1f - b.timer / Game.FUSE, t, age = Game.FUSE - b.timer)
    drawFlames(g, s, t)
    for (e in g.enemies) drawEnemy(e, s, t, g.player.x, g.player.y)
    if (g.phase != Phase.GAME_OVER) drawPlayer(g, s)
}

private fun DrawScope.drawFloor(s: Float) {
    drawRect(Pal.grass1, Offset.Zero, size)
    for (y in 0 until Game.H) for (x in 0 until Game.W) {
        if ((x + y) % 2 == 0) drawRect(Pal.grass2, Offset(x * s, y * s), Size(s, s))
        // Tufos de erva em sítios "aleatórios" mas fixos
        val h = (x * 73 + y * 151) % 7
        if (h == 0) {
            val cx = x * s + s * (0.25f + (y % 3) * 0.2f)
            val cy = y * s + s * (0.3f + (x % 3) * 0.18f)
            drawLine(Pal.grassTuft, Offset(cx, cy), Offset(cx - s * 0.05f, cy - s * 0.1f), s * 0.035f, StrokeCap.Round)
            drawLine(Pal.grassTuft, Offset(cx, cy), Offset(cx + s * 0.05f, cy - s * 0.11f), s * 0.035f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawWall(x: Float, y: Float, s: Float) {
    val b = s * 0.12f
    drawRect(Pal.wall, Offset(x, y), Size(s + 0.5f, s + 0.5f))
    drawRect(Pal.wallLight, Offset(x, y), Size(s, b))
    drawRect(Pal.wallLight, Offset(x, y), Size(b, s))
    drawRect(Pal.wallDark, Offset(x, y + s - b), Size(s, b))
    drawRect(Pal.wallDark, Offset(x + s - b, y), Size(b, s))
    drawCircle(Pal.wallDark, s * 0.05f, Offset(x + s * 0.5f, y + s * 0.5f))
}

private fun DrawScope.drawBrick(x: Float, y: Float, s: Float, alpha: Float = 1f) {
    val m = s * 0.055f
    drawRect(Pal.brick, Offset(x, y), Size(s + 0.5f, s + 0.5f), alpha = alpha)
    val rh = s / 3f
    for (r in 0 until 3) {
        val yy = y + r * rh
        drawRect(Pal.brickLight, Offset(x, yy + m / 2), Size(s, m * 0.8f), alpha = alpha)
        if (r > 0) drawLine(Pal.mortar, Offset(x, yy), Offset(x + s, yy), m, alpha = alpha)
        val joints = if (r % 2 == 0) listOf(0.5f) else listOf(0.25f, 0.75f)
        for (j in joints) drawLine(Pal.mortar, Offset(x + s * j, yy), Offset(x + s * j, yy + rh), m, alpha = alpha)
    }
    drawRect(Pal.mortar, Offset(x + m / 2, y + m / 2), Size(s - m, s - m), style = Stroke(m), alpha = alpha)
}

private fun DrawScope.drawBurning(x: Float, y: Float, s: Float, p: Float, t: Float) {
    val c = Offset(x + s / 2, y + s / 2)
    scale(1f - 0.35f * p, c) { drawBrick(x, y, s, alpha = 1f - p) }
    val r = s * (0.35f + 0.2f * p) * (1f + 0.08f * sin(t * 40f))
    drawCircle(
        Brush.radialGradient(
            listOf(Pal.flameCore, Pal.flameMid, Pal.flameOuter, Color.Transparent),
            center = c,
            radius = r,
        ),
        r,
        c,
        alpha = 1f - p * 0.6f,
    )
}

fun DrawScope.drawBomb(cx: Float, cy: Float, s: Float, urgency: Float, t: Float, age: Float = 1f) {
    // Ao ser posta, a bomba "salta" para o chão (easeOutBack)
    val k = (age / 0.2f).coerceIn(0f, 1f) - 1f
    val pop = 1f + 2.7f * k * k * k + 1.7f * k * k
    val pulse = pop * (1f + 0.07f * sin(t * (7f + urgency * 20f)))
    val r = s * 0.34f * pulse
    drawOval(Color.Black.copy(alpha = 0.3f), Offset(cx - r, cy + r * 0.55f), Size(r * 2, r * 0.55f))
    val body = Offset(cx, cy + s * 0.04f)
    drawCircle(Pal.bomb, r, body)
    if (urgency > 0.7f && sin(t * 35f) > 0f) drawCircle(Color(0xFFEF4444), r, body, alpha = 0.45f)
    drawCircle(Color.White.copy(alpha = 0.35f), r * 0.28f, Offset(cx - r * 0.35f, cy - r * 0.25f))
    // Tampa e rastilho
    val capC = Offset(cx + r * 0.55f, cy - r * 0.6f)
    drawCircle(Color(0xFF4B5563), r * 0.22f, capC)
    val tip = Offset(cx + r * 0.95f, cy - r * 1.15f)
    drawLine(Pal.fuse, capC, tip, s * 0.06f, StrokeCap.Round)
    val spark = s * (0.09f + 0.035f * sin(t * 45f))
    drawCircle(Pal.flameOuter, spark, tip)
    drawCircle(Pal.flameMid, spark * 0.55f, tip)
}

private fun DrawScope.drawFlames(g: Game, s: Float, t: Float) {
    fun conn(x: Int, y: Int, h: Boolean): Boolean {
        if (x !in 0 until Game.W || y !in 0 until Game.H || g.flame[y][x] <= 0f) return false
        return if (h) g.flameH[y][x] else g.flameV[y][x]
    }
    val layers = listOf(Pal.flameOuter to 1f, Pal.flameMid to 0.62f, Pal.flameCore to 0.28f)
    // Camada a camada em todas as casas, para as junções entre casas não se notarem.
    for ((color, f) in layers) {
        for (y in 0 until Game.H) for (x in 0 until Game.W) {
            val k = g.flame[y][x]
            if (k <= 0f) continue
            val ph = 1f - k / Game.FLAME
            val grow = if (ph < 0.12f) ph / 0.12f else 1f - (ph - 0.12f) / 0.88f * 0.75f
            val w = s * 0.86f * grow * f * (1f + 0.07f * sin(t * 45f + x * 1.7f + y * 2.3f))
            if (w <= 0.5f) continue
            val cx = x * s + s / 2
            val cy = y * s + s / 2
            val h = g.flameH[y][x]
            val v = g.flameV[y][x]
            val cap = s * 0.42f
            if (h) {
                val l = if (conn(x - 1, y, true)) x * s - w / 2 else cx - cap
                val r = if (conn(x + 1, y, true)) x * s + s + w / 2 else cx + cap
                drawRoundRect(color, Offset(l, cy - w / 2), Size(r - l, w), CornerRadius(w / 2))
            }
            if (v) {
                val top = if (conn(x, y - 1, false)) y * s - w / 2 else cy - cap
                val bot = if (conn(x, y + 1, false)) y * s + s + w / 2 else cy + cap
                drawRoundRect(color, Offset(cx - w / 2, top), Size(w, bot - top), CornerRadius(w / 2))
            }
            if (h && v) drawCircle(color, w * 0.62f, Offset(cx, cy))
        }
    }
}

private fun DrawScope.drawItem(x: Float, y: Float, s: Float, type: ItemType, t: Float) {
    val pad = s * 0.1f
    val glow = 0.5f + 0.5f * sin(t * 5f)
    val bg = when (type) {
        ItemType.BOMB -> Color(0xFF2563EB)
        ItemType.FIRE -> Color(0xFFDC2626)
        ItemType.SPEED -> Color(0xFF16A34A)
    }
    drawRoundRect(bg, Offset(x + pad, y + pad), Size(s - 2 * pad, s - 2 * pad), CornerRadius(s * 0.16f))
    drawRoundRect(
        Color.White.copy(alpha = 0.45f + 0.45f * glow),
        Offset(x + pad, y + pad),
        Size(s - 2 * pad, s - 2 * pad),
        CornerRadius(s * 0.16f),
        style = Stroke(s * 0.05f),
    )
    val cx = x + s / 2
    val cy = y + s / 2
    when (type) {
        ItemType.BOMB -> {
            drawCircle(Pal.bomb, s * 0.19f, Offset(cx - s * 0.02f, cy + s * 0.04f))
            drawCircle(Color.White.copy(alpha = 0.4f), s * 0.05f, Offset(cx - s * 0.08f, cy - s * 0.02f))
            drawLine(Pal.fuse, Offset(cx + s * 0.1f, cy - s * 0.1f), Offset(cx + s * 0.17f, cy - s * 0.19f), s * 0.05f, StrokeCap.Round)
            drawCircle(Pal.flameMid, s * 0.05f, Offset(cx + s * 0.18f, cy - s * 0.2f))
        }
        ItemType.FIRE -> {
            drawPath(flamePath(cx, cy, s * 0.56f), Pal.flameOuter)
            drawPath(flamePath(cx, cy + s * 0.08f, s * 0.32f), Pal.flameMid)
        }
        ItemType.SPEED -> drawPath(boltPath(cx, cy, s * 0.55f), Pal.flameMid)
    }
}

private fun flamePath(cx: Float, cy: Float, h: Float) = Path().apply {
    moveTo(cx, cy - h * 0.5f)
    cubicTo(cx + h * 0.5f, cy - h * 0.05f, cx + h * 0.38f, cy + h * 0.5f, cx, cy + h * 0.5f)
    cubicTo(cx - h * 0.38f, cy + h * 0.5f, cx - h * 0.5f, cy - h * 0.05f, cx, cy - h * 0.5f)
    close()
}

private fun boltPath(cx: Float, cy: Float, h: Float) = Path().apply {
    moveTo(cx + 0.12f * h, cy - 0.5f * h)
    lineTo(cx - 0.3f * h, cy + 0.06f * h)
    lineTo(cx - 0.02f * h, cy + 0.06f * h)
    lineTo(cx - 0.12f * h, cy + 0.5f * h)
    lineTo(cx + 0.3f * h, cy - 0.06f * h)
    lineTo(cx + 0.02f * h, cy - 0.06f * h)
    close()
}

private fun DrawScope.drawDoor(x: Float, y: Float, s: Float, open: Boolean, t: Float) {
    val cx = x + s / 2
    val archY = y + s * 0.42f
    // Moldura de pedra em arco
    drawCircle(Pal.wallDark, s * 0.4f, Offset(cx, archY))
    drawRect(Pal.wallDark, Offset(x + s * 0.1f, archY), Size(s * 0.8f, s * 0.52f))
    val inR = s * 0.31f
    if (open) {
        drawCircle(Color(0xFF0C1E3A), inR, Offset(cx, archY))
        drawRect(Color(0xFF0C1E3A), Offset(cx - inR, archY), Size(inR * 2, s * 0.48f))
        val pulse = 0.75f + 0.25f * sin(t * 6f)
        val c = Offset(cx, archY + s * 0.12f)
        drawCircle(
            Brush.radialGradient(listOf(Color.White, Color(0xFF67E8F9), Color(0xFF2563EB), Color.Transparent), c, inR * 1.1f * pulse),
            inR * 1.1f * pulse,
            c,
        )
        // Brilhos a rodar
        for (i in 0 until 5) {
            val a = t * 3f + i * TAU / 5
            drawCircle(Color.White, s * 0.03f, Offset(c.x + cos(a) * inR * 0.7f, c.y + sin(a) * inR * 0.7f))
        }
    } else {
        val wood = Color(0xFF8B5A2B)
        drawCircle(wood, inR, Offset(cx, archY))
        drawRect(wood, Offset(cx - inR, archY), Size(inR * 2, s * 0.48f))
        for (i in -1..1) {
            val lx = cx + i * inR * 0.5f
            drawLine(Color(0xFF5C3A1B), Offset(lx, archY - inR * 0.8f), Offset(lx, y + s * 0.9f), s * 0.03f)
        }
        drawCircle(Color(0xFFFACC15), s * 0.04f, Offset(cx + inR * 0.6f, archY + s * 0.2f))
    }
}

private fun DrawScope.drawPlayer(g: Game, s: Float) {
    val p = g.player
    val c = Offset(p.x * s, p.y * s)
    var sc = 1f
    var rot = 0f
    if (p.dying >= 0f) {
        val k = (p.dying / 1.3f).coerceIn(0f, 1f)
        rot = k * 720f
        sc = 1f - k
    }
    if (g.phase == Phase.LEVEL_CLEAR) {
        // Entra na porta a rodopiar
        val k = (g.phaseTime / 1.1f).coerceIn(0f, 1f)
        rot = k * 540f
        sc = 1f - k
    }
    if (sc <= 0.01f) return
    // A piscar enquanto tem o escudo de quem acabou de renascer
    if (p.invulnerable > 0f && (g.time * 14f).toInt() % 2 == 0) return
    withTransform({
        rotate(rot, c)
        scale(sc, sc, c)
    }) {
        drawHero(c.x, c.y, s, p.facing, p.walk, p.moving)
    }
}

fun DrawScope.drawHero(cx: Float, cy: Float, s: Float, facing: Dir, walk: Float, moving: Boolean) {
    val step = if (moving) sin(walk * TAU * 1.1f) else 0f
    val bob = abs(step) * s * 0.03f
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(cx - s * 0.27f, cy + s * 0.3f), Size(s * 0.54f, s * 0.13f))
    // Pernas
    val legW = s * 0.13f
    val legH = s * 0.17f
    drawRoundRect(Pal.boots, Offset(cx - s * 0.18f, cy + s * 0.2f - step * s * 0.05f), Size(legW, legH), CornerRadius(legW / 2))
    drawRoundRect(Pal.boots, Offset(cx + s * 0.05f, cy + s * 0.2f + step * s * 0.05f), Size(legW, legH), CornerRadius(legW / 2))
    // Corpo e braços
    val by = cy - bob
    drawCircle(Pal.skin, s * 0.07f, Offset(cx - s * 0.27f, by + s * 0.1f + step * s * 0.05f))
    drawCircle(Pal.skin, s * 0.07f, Offset(cx + s * 0.27f, by + s * 0.1f - step * s * 0.05f))
    drawRoundRect(Pal.suit, Offset(cx - s * 0.24f, by - s * 0.03f), Size(s * 0.48f, s * 0.29f), CornerRadius(s * 0.12f))
    drawRect(Pal.belt, Offset(cx - s * 0.235f, by + s * 0.12f), Size(s * 0.47f, s * 0.05f))
    drawRect(Pal.helmet, Offset(cx - s * 0.035f, by + s * 0.115f), Size(s * 0.07f, s * 0.06f))
    // Cabeça
    val hy = by - s * 0.17f
    drawCircle(Pal.skin, s * 0.21f, Offset(cx, hy))
    if (facing != Dir.UP) {
        val ex = facing.dx * s * 0.07f
        val ey = hy + s * 0.03f + facing.dy * s * 0.02f
        if (facing.dx == 0) {
            drawCircle(Color(0xFF1F2937), s * 0.035f, Offset(cx - s * 0.075f, ey))
            drawCircle(Color(0xFF1F2937), s * 0.035f, Offset(cx + s * 0.075f, ey))
            drawLine(Color(0xFFB45309), Offset(cx - s * 0.04f, ey + s * 0.08f), Offset(cx + s * 0.04f, ey + s * 0.08f), s * 0.025f, StrokeCap.Round)
        } else {
            drawCircle(Color(0xFF1F2937), s * 0.035f, Offset(cx + ex, ey))
            drawCircle(Color(0xFF1F2937), s * 0.035f, Offset(cx + ex + facing.dx * s * 0.08f, ey))
        }
    }
    // Capacete de mineiro com lanterna
    drawArc(Pal.helmet, 180f, 180f, true, Offset(cx - s * 0.24f, hy - s * 0.27f), Size(s * 0.48f, s * 0.44f))
    drawRoundRect(Pal.helmetDark, Offset(cx - s * 0.27f, hy - s * 0.07f), Size(s * 0.54f, s * 0.065f), CornerRadius(s * 0.03f))
    if (facing != Dir.UP) {
        val lamp = Offset(cx + facing.dx * s * 0.1f, hy - s * 0.15f)
        drawCircle(Color.White.copy(alpha = 0.35f), s * 0.09f, lamp)
        drawCircle(Color.White, s * 0.05f, lamp)
    }
}

private fun DrawScope.drawEnemy(e: Enemy, s: Float, t: Float, px: Float, py: Float) {
    val c = Offset(e.x * s, e.y * s)
    var sc = 1f
    var flash = false
    if (!e.alive) {
        sc = 1f - e.dying
        flash = (e.dying * 18f).toInt() % 2 == 0
    }
    if (sc <= 0.01f) return
    // Olhos a seguir o jogador
    val vx = px - e.x
    val vy = py - e.y
    val len = sqrt(vx * vx + vy * vy).coerceAtLeast(0.001f)
    val look = Offset(vx / len, vy / len)
    val seed = (e.hashCode() % 100) * 0.37f
    scale(sc, c) {
        when (e.kind) {
            EnemyKind.BLOB -> drawBlob(c, s, t + seed, look)
            EnemyKind.EYE -> drawBat(c, s, t + seed, look)
            EnemyKind.GHOST -> drawGhost(c, s, t + seed, look)
        }
        if (flash) drawCircle(Color.White, s * 0.38f, c, alpha = 0.7f)
    }
}

private fun DrawScope.drawEyes(c: Offset, s: Float, look: Offset, gap: Float, r: Float) {
    for (side in listOf(-1f, 1f)) {
        val ec = Offset(c.x + side * gap, c.y)
        drawCircle(Color.White, r, ec)
        drawCircle(Color(0xFF111827), r * 0.5f, ec + look * (r * 0.45f))
    }
}

private fun DrawScope.drawBlob(c: Offset, s: Float, t: Float, look: Offset) {
    val wob = sin(t * 7f)
    val w = s * 0.72f * (1f + 0.07f * wob)
    val h = s * 0.62f * (1f - 0.07f * wob)
    val bottom = c.y + s * 0.33f
    drawOval(Color.Black.copy(alpha = 0.25f), Offset(c.x - s * 0.3f, bottom - s * 0.05f), Size(s * 0.6f, s * 0.12f))
    drawRoundRect(Pal.blob, Offset(c.x - w / 2, bottom - h), Size(w, h), CornerRadius(w / 2, h * 0.5f))
    drawCircle(Color.White.copy(alpha = 0.35f), s * 0.06f, Offset(c.x - w * 0.28f, bottom - h * 0.75f))
    drawEyes(Offset(c.x, bottom - h * 0.55f), s, look, s * 0.12f, s * 0.1f)
}

private fun DrawScope.drawBat(c: Offset, s: Float, t: Float, look: Offset) {
    val body = c + Offset(0f, sin(t * 5f) * s * 0.05f)
    val flap = sin(t * 18f) * s * 0.13f
    drawOval(Color.Black.copy(alpha = 0.2f), Offset(c.x - s * 0.22f, c.y + s * 0.3f), Size(s * 0.44f, s * 0.1f))
    for (side in listOf(-1f, 1f)) {
        val wing = Path().apply {
            moveTo(body.x + side * s * 0.18f, body.y - s * 0.08f)
            lineTo(body.x + side * s * 0.48f, body.y - s * 0.12f + flap)
            lineTo(body.x + side * s * 0.38f, body.y + s * 0.06f + flap * 0.5f)
            lineTo(body.x + side * s * 0.2f, body.y + s * 0.1f)
            close()
        }
        drawPath(wing, Color(0xFF6D28D9))
    }
    drawCircle(Pal.eyeBat, s * 0.27f, body)
    drawCircle(Color.White, s * 0.16f, body)
    drawCircle(Color(0xFFDC2626), s * 0.09f, body + look * (s * 0.06f))
    drawCircle(Color(0xFF111827), s * 0.045f, body + look * (s * 0.07f))
}

private fun DrawScope.drawGhost(c: Offset, s: Float, t: Float, look: Offset) {
    val body = c + Offset(0f, sin(t * 3f) * s * 0.06f - s * 0.03f)
    val a = 0.82f
    drawOval(Color.Black.copy(alpha = 0.15f), Offset(c.x - s * 0.2f, c.y + s * 0.32f), Size(s * 0.4f, s * 0.09f))
    drawCircle(Pal.ghost, s * 0.28f, body + Offset(0f, -s * 0.05f), alpha = a)
    drawRect(Pal.ghost, Offset(body.x - s * 0.28f, body.y - s * 0.05f), Size(s * 0.56f, s * 0.25f), alpha = a)
    for (i in -1..1) {
        val wave = sin(t * 8f + i) * s * 0.025f
        drawCircle(Pal.ghost, s * 0.095f, Offset(body.x + i * s * 0.187f, body.y + s * 0.2f + wave), alpha = a)
    }
    for (side in listOf(-1f, 1f)) {
        val ec = body + Offset(side * s * 0.1f, -s * 0.06f) + look * (s * 0.03f)
        drawOval(Color(0xFF1E1B4B), Offset(ec.x - s * 0.045f, ec.y - s * 0.07f), Size(s * 0.09f, s * 0.14f))
    }
}

/**
 * Transição em círculo (como nos jogos antigos): fecha sobre o jogador ao sair do nível
 * ou ao morrer, e abre a partir dele no início do nível seguinte.
 */
fun DrawScope.drawIris(g: Game) {
    val k = when (g.phase) {
        Phase.START -> (g.phaseTime / 0.7f).coerceIn(0f, 1f)
        Phase.LEVEL_CLEAR -> 1f - ((g.phaseTime - 1.3f) / 0.9f).coerceIn(0f, 1f)
        Phase.DYING -> 1f - ((g.phaseTime - 1.4f) / 0.7f).coerceIn(0f, 1f)
        else -> 1f
    }
    if (k >= 1f) return
    val s = size.width / Game.W
    val c = Offset(g.player.x * s, g.player.y * s)
    val maxR = sqrt(size.width * size.width + size.height * size.height)
    val r = maxR * k * k
    val band = maxR * 2f
    drawCircle(Color.Black, r + band / 2, c, style = Stroke(band))
}
