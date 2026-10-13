package pt.tiago.kaboom.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.ui.pixel.Art
import kotlin.math.abs

enum class ControlScheme(val short: String) {
    DPAD("Cruz clássica"),
    FLOATING("Joystick flutuante"),
}

/**
 * Metade esquerda: direcção. Metade direita: bomba (trocadas com [leftHanded]). Multitoque.
 *
 * - [ControlScheme.DPAD]: cruz pequena e fixa; o dedo pode tocar em qualquer sítio da metade
 *   esquerda, a direcção conta-se a partir do centro da cruz.
 * - [ControlScheme.FLOATING]: o joystick nasce onde o polegar pousa e arrasta-se atrás dele
 *   se o dedo for longe demais — nunca é preciso "procurar" o controlo.
 */
@Composable
fun Controls(
    modifier: Modifier,
    scheme: ControlScheme,
    leftHanded: Boolean,
    onDir: (Dir?) -> Unit,
    onBomb: () -> Unit,
) {
    val dirCb by rememberUpdatedState(onDir)
    val bombCb by rememberUpdatedState(onBomb)
    val schemeNow by rememberUpdatedState(scheme)
    val leftNow by rememberUpdatedState(leftHanded)
    val density = LocalDensity.current
    val padR = with(density) { 62.dp.toPx() }
    val stickR = with(density) { 46.dp.toPx() }
    val bombR = with(density) { 50.dp.toPx() }

    var origin by remember { mutableStateOf<Offset?>(null) } // centro do joystick flutuante
    var knob by remember { mutableStateOf<Offset?>(null) }   // relativo ao centro
    var active by remember { mutableStateOf<Dir?>(null) }
    var bombDown by remember { mutableStateOf(false) }

    Canvas(
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                val isPad = HashMap<PointerId, Boolean>()
                while (true) {
                    val ev = awaitPointerEvent()
                    val split = size.width * 0.5f
                    for (c in ev.changes) {
                        if (c.changedToDown()) {
                            val pad = (c.position.x < split) != leftNow
                            isPad[c.id] = pad
                            if (!pad) bombCb()
                            if (pad && schemeNow == ControlScheme.FLOATING) origin = c.position
                        }
                        if (!c.pressed) isPad.remove(c.id)
                        c.consume()
                    }
                    bombDown = ev.changes.any { it.pressed && isPad[it.id] == false }
                    val finger = ev.changes.firstOrNull { it.pressed && isPad[it.id] == true }

                    val newDir = if (finger == null) {
                        knob = null
                        origin = null
                        null
                    } else {
                        val floating = schemeNow == ControlScheme.FLOATING
                        val r = if (floating) stickR else padR
                        val center = if (floating) {
                            origin ?: finger.position
                        } else {
                            padCenter(size.width.toFloat(), size.height.toFloat(), padR, leftNow)
                        }
                        var v = finger.position - center
                        val d = v.getDistance()
                        if (floating && d > r) {
                            // A base segue o dedo: arrastar longe não "prende" o joystick.
                            val newOrigin = finger.position - v * (r / d)
                            origin = newOrigin
                            v = finger.position - newOrigin
                        }
                        knob = if (v.getDistance() > r) v * (r / v.getDistance()) else v
                        pickDir(v, r * 0.25f, active)
                    }
                    if (newDir != active) {
                        active = newDir
                        dirCb(newDir)
                    }
                }
            }
        },
    ) {
        when (scheme) {
            ControlScheme.DPAD -> drawDpad(padCenter(size.width, size.height, padR, leftHanded), padR, active)
            ControlScheme.FLOATING -> {
                val c = origin
                if (c == null) {
                    // Sugestão discreta de onde pôr o polegar
                    val hint = padCenter(size.width, size.height, padR, leftHanded)
                    drawCircle(Color.White.copy(alpha = 0.06f), stickR, hint)
                    drawCircle(Color.White.copy(alpha = 0.18f), stickR, hint, style = Stroke(stickR * 0.04f))
                    drawCircle(Color.White.copy(alpha = 0.15f), stickR * 0.45f, hint)
                } else {
                    drawCircle(Color.White.copy(alpha = 0.1f), stickR, c)
                    drawCircle(Color.White.copy(alpha = 0.35f), stickR, c, style = Stroke(stickR * 0.05f))
                    val k = c + (knob ?: Offset.Zero)
                    drawCircle(Color(0xFFE5E7EB), stickR * 0.45f, k)
                }
            }
        }
        val bombX = if (leftHanded) bombR * 1.6f else size.width - bombR * 1.6f
        drawBombButton(Offset(bombX, size.height * 0.55f), bombR, bombDown)
    }
}

private fun padCenter(w: Float, h: Float, r: Float, leftHanded: Boolean) =
    Offset(if (leftHanded) w - r * 1.6f else r * 1.6f, h * 0.55f)

/** Eixo dominante, com histerese para não "tremer" nas diagonais. */
private fun pickDir(v: Offset, dead: Float, current: Dir?): Dir? {
    if (v.getDistance() < dead) return null
    val horizontal = when {
        current?.dx != null && current.dx != 0 && abs(v.x) >= abs(v.y) * 0.75f -> true
        current?.dy != null && current.dy != 0 && abs(v.y) >= abs(v.x) * 0.75f -> false
        else -> abs(v.x) > abs(v.y)
    }
    return if (horizontal) {
        if (v.x > 0) Dir.RIGHT else Dir.LEFT
    } else {
        if (v.y > 0) Dir.DOWN else Dir.UP
    }
}

/** Cruz ao estilo das consolas portáteis antigas. */
private fun DrawScope.drawDpad(c: Offset, r: Float, active: Dir?) {
    val arm = r * 0.62f
    val body = Color(0xFF1F2937)
    val edge = Color(0xFF4B5563)
    // Sombra, bordo e corpo da cruz
    for ((color, grow, dy) in listOf(Triple(Color.Black.copy(alpha = 0.4f), 0f, r * 0.08f), Triple(edge, r * 0.05f, 0f), Triple(body, 0f, 0f))) {
        drawRoundRect(color, Offset(c.x - r - grow, c.y - arm / 2 - grow + dy), Size(2 * (r + grow), arm + 2 * grow), CornerRadius(r * 0.12f))
        drawRoundRect(color, Offset(c.x - arm / 2 - grow, c.y - r - grow + dy), Size(arm + 2 * grow, 2 * (r + grow)), CornerRadius(r * 0.12f))
    }
    for (d in Dir.entries) {
        val angle = when (d) {
            Dir.UP -> 0f
            Dir.RIGHT -> 90f
            Dir.DOWN -> 180f
            Dir.LEFT -> 270f
        }
        rotate(angle, c) {
            if (d == active) {
                drawRoundRect(Color(0xFF374151), Offset(c.x - arm / 2, c.y - r), Size(arm, r - arm / 2), CornerRadius(r * 0.1f))
            }
            val tip = Offset(c.x, c.y - r * 0.78f)
            val tri = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - arm * 0.25f, tip.y + arm * 0.32f)
                lineTo(tip.x + arm * 0.25f, tip.y + arm * 0.32f)
                close()
            }
            drawPath(tri, if (d == active) Color(0xFFFACC15) else Color(0xFF9CA3AF))
        }
    }
    drawCircle(Color(0xFF111827), arm * 0.28f, c)
}

private fun DrawScope.drawBombButton(c: Offset, r: Float, down: Boolean) {
    val press = if (down) 0.92f else 1f
    drawCircle(Color.Black.copy(alpha = 0.4f), r, c + Offset(0f, r * 0.1f))
    drawCircle(if (down) Color(0xFF9E2A1C) else Ui.Red, r * press, c)
    drawCircle(Color.White.copy(alpha = 0.25f), r * press, c, style = Stroke(r * 0.05f))
    // Sprite da bomba ampliado por um inteiro, para ficar nítido
    val k = (r * 1.25f / ART).toInt().coerceAtLeast(1)
    val half = ART * k / 2
    sprite(Art.bomb[0], (c.x - half).toInt(), (c.y - half).toInt(), k, scale = press)
}
