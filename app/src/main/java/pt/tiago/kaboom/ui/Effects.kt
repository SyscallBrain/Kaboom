package pt.tiago.kaboom.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pt.tiago.kaboom.game.Fx
import pt.tiago.kaboom.game.FxKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
                repeat(12) { burst(f, speed = 2f + rnd.nextFloat() * 3.5f, life = 0.25f + rnd.nextFloat() * 0.3f, color = if (rnd.nextBoolean()) Pal.flameMid else Pal.flameOuter, size = 0.06f + rnd.nextFloat() * 0.06f, gravity = 0f, square = false) }
                repeat(5) { burst(f, speed = 0.6f, life = 0.7f, color = Color(0xFF6B7280), size = 0.14f, gravity = -1.2f, square = false) } // fumo
            }
            FxKind.BLOCK_BROKEN -> repeat(8) {
                parts += Particle(
                    f.x + (rnd.nextFloat() - 0.5f) * 0.6f,
                    f.y + (rnd.nextFloat() - 0.5f) * 0.6f,
                    (rnd.nextFloat() - 0.5f) * 4f,
                    -1.5f - rnd.nextFloat() * 2.5f,
                    0.55f + rnd.nextFloat() * 0.3f,
                    if (rnd.nextBoolean()) Pal.brick else Pal.mortar,
                    0.1f + rnd.nextFloat() * 0.1f,
                    gravity = 11f,
                    square = true,
                )
            }
            FxKind.ITEM -> repeat(14) { i ->
                val a = i / 14f * 6.2832f
                parts += Particle(f.x, f.y, cos(a) * 2.6f, sin(a) * 2.6f, 0.45f, if (i % 2 == 0) Color.White else Pal.helmet, 0.07f, 0f, false)
            }
            FxKind.SCORE -> popups += Popup(f.x, f.y, "+${f.value}")
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

    fun draw(scope: DrawScope, s: Float, measurer: TextMeasurer) = with(scope) {
        for (p in parts) {
            val k = 1f - p.age / p.life
            val r = p.size * s * (if (p.square) 1f else 0.5f + 0.5f * k)
            if (p.square) {
                drawRect(p.color, Offset(p.x * s - r / 2, p.y * s - r / 2), Size(r, r), alpha = k.coerceIn(0f, 1f))
            } else {
                drawCircle(p.color, r, Offset(p.x * s, p.y * s), alpha = (k * 0.9f).coerceIn(0f, 1f))
            }
        }
        val style = TextStyle(
            color = Color.White,
            fontSize = (s * 0.32f / (density * fontScale)).sp,
            fontWeight = FontWeight.Black,
            shadow = Shadow(Color.Black, Offset(2f, 3f), 2f),
        )
        for (p in popups) {
            val k = 1f - p.age / 0.9f
            val layout = measurer.measure(p.text, style)
            drawText(
                layout,
                topLeft = Offset(p.x * s - layout.size.width / 2f, p.y * s - layout.size.height / 2f),
                alpha = (k * 1.6f).coerceIn(0f, 1f),
            )
        }
    }
}
