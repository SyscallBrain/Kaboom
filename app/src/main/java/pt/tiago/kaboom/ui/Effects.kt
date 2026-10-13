package pt.tiago.kaboom.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import pt.tiago.kaboom.game.Fx
import pt.tiago.kaboom.game.FxKind
import pt.tiago.kaboom.ui.pixel.PixelFont
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Cores da paleta 16 bits
private val FlameYellow = Color(0xFFFFE35A)
private val FlameOrange = Color(0xFFFF7A1A)
private val Brick = Color(0xFFC8582A)
private val Mortar = Color(0xFF6E2614)
private val Gold = Color(0xFFFFCF3A)
private val Smoke = Color(0xFF8E8AA6)
private val Magic = Color(0xFF9A6CFF)
private val Shield = Color(0xFF7FE8FF)

/**
 * Efeitos só visuais (partículas, pontos a flutuar, tremor do ecrã). Ficam fora da lógica
 * do jogo de propósito: não mudam nada nas regras e podem ser trocados à vontade.
 * Tudo em unidades de casa, como no [pt.tiago.kaboom.game.Game].
 */
class Effects {
    private class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val life: Float,
        val color: Color,
        val size: Float,
        val gravity: Float,
        val square: Boolean,
    ) {
        var age = 0f
    }

    private class Popup(val x: Float, var y: Float, val text: String) {
        var age = 0f
    }

    private val parts = ArrayList<Particle>()
    private val popups = ArrayList<Popup>()
    private val rnd = Random(42)

    /** Segundos de tremor que faltam. */
    private var shake = 0f

    /** Força do tremor escolhida nas definições. */
    var shakeLevel = Level.MEDIUM

    fun consume(list: List<Fx>) {
        for (f in list) when (f.kind) {
            FxKind.EXPLOSION -> {
                shake = min(shake + 0.35f, 0.6f)
                repeat(12) { burst(f, speed = 2f + rnd.nextFloat() * 3.5f, life = 0.25f + rnd.nextFloat() * 0.3f, color = if (rnd.nextBoolean()) FlameYellow else FlameOrange, size = 0.06f + rnd.nextFloat() * 0.06f, gravity = 0f, square = false) }
                repeat(5) { burst(f, speed = 0.6f, life = 0.7f, color = Smoke, size = 0.14f, gravity = -1.2f, square = false) } // fumo
            }
            FxKind.BLOCK_BROKEN -> repeat(8) {
                parts += Particle(
                    f.x + (rnd.nextFloat() - 0.5f) * 0.6f,
                    f.y + (rnd.nextFloat() - 0.5f) * 0.6f,
                    (rnd.nextFloat() - 0.5f) * 4f,
                    -1.5f - rnd.nextFloat() * 2.5f,
                    0.55f + rnd.nextFloat() * 0.3f,
                    if (rnd.nextBoolean()) Brick else Mortar,
                    0.1f + rnd.nextFloat() * 0.1f,
                    gravity = 11f,
                    square = true,
                )
            }
            FxKind.ITEM -> repeat(14) { i ->
                val a = i / 14f * 6.2832f
                parts += Particle(f.x, f.y, cos(a) * 2.6f, sin(a) * 2.6f, 0.45f, if (i % 2 == 0) Color.White else Gold, 0.07f, 0f, false)
            }
            FxKind.SCORE -> popups += Popup(f.x, f.y, "+${f.value}")
            FxKind.TELEPORT -> repeat(14) {
                burst(f, speed = 1.5f + rnd.nextFloat() * 2f, life = 0.5f, color = if (rnd.nextBoolean()) Magic else Color.White, size = 0.08f, gravity = -2f, square = true)
            }
            FxKind.SHIELD_BREAK -> {
                shake = min(shake + 0.2f, 0.6f)
                repeat(16) { burst(f, speed = 3f + rnd.nextFloat() * 2f, life = 0.5f, color = if (rnd.nextBoolean()) Shield else Color.White, size = 0.1f, gravity = 6f, square = true) }
            }
            FxKind.HURT -> repeat(10) {
                burst(f, speed = 2.5f, life = 0.35f, color = Color.White, size = 0.08f, gravity = 0f, square = true)
            }
            FxKind.KICK -> repeat(6) {
                burst(f, speed = 1.2f, life = 0.4f, color = Smoke, size = 0.1f, gravity = -1f, square = true)
            }
        }
    }

    private fun burst(f: Fx, speed: Float, life: Float, color: Color, size: Float, gravity: Float, square: Boolean) {
        val a = rnd.nextFloat() * 6.2832f
        parts += Particle(f.x, f.y, cos(a) * speed, sin(a) * speed, life, color, size, gravity, square)
    }

    fun update(dt: Float) {
        shake = (shake - dt).coerceAtLeast(0f)
        for (p in parts) {
            p.age += dt
            p.vy += p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
        }
        parts.removeAll { it.age >= it.life }
        for (p in popups) {
            p.age += dt
            p.y -= 0.9f * dt
        }
        popups.removeAll { it.age >= 0.9f }
    }

    /** Deslocamento do mapa (em casas) para o tremor das explosões. */
    fun shakeOffset(): Offset {
        if (shake <= 0f) return Offset.Zero
        val strength = when (shakeLevel) {
            Level.OFF -> return Offset.Zero
            Level.LOW -> 0.12f
            Level.MEDIUM -> 0.24f
            Level.HIGH -> 0.4f
        }
        // Diminui devagar no início e depressa no fim, para se sentir o "impacto".
        val a = strength * sqrt(shake / 0.6f)
        return Offset((rnd.nextFloat() * 2 - 1) * a, (rnd.nextFloat() * 2 - 1) * a)
    }

    fun clear() {
        parts.clear()
        popups.clear()
        shake = 0f
    }

    /**
     * Desenha em pixel art: cada partícula é um quadrado de 1 ou 2 píxeis de arte,
     * alinhado à grelha, e os pontos usam a letra pixel. [k] = ampliação da arte.
     */
    fun draw(scope: DrawScope, k: Int, measurer: TextMeasurer) = with(scope) {
        for (p in parts) {
            val life = 1f - p.age / p.life
            val px = if (p.square || life > 0.5f) (p.size * 16f).roundToInt().coerceAtLeast(1) else 1
            val x = (p.x * 16f - px / 2f).roundToInt() * k
            val y = (p.y * 16f - px / 2f).roundToInt() * k
            drawRect(p.color, Offset(x.toFloat(), y.toFloat()), Size((px * k).toFloat(), (px * k).toFloat()), alpha = life.coerceIn(0f, 1f))
        }
        val style = TextStyle(
            color = Color.White,
            fontFamily = PixelFont,
            fontSize = (6f * k / (density * fontScale)).sp,
            shadow = Shadow(Color(0xFF1D1430), Offset(k.toFloat(), k.toFloat()), 0f),
        )
        for (p in popups) {
            val life = 1f - p.age / 0.9f
            val layout = measurer.measure(p.text, style)
            val x = (p.x * 16f).roundToInt() * k - layout.size.width / 2
            val y = (p.y * 16f).roundToInt() * k - layout.size.height / 2
            drawText(layout, topLeft = Offset(x.toFloat(), y.toFloat()), alpha = (life * 1.6f).coerceIn(0f, 1f))
        }
    }
}
