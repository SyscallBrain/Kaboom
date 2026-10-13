package pt.tiago.kaboom.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import pt.tiago.kaboom.game.Dir
import kotlin.math.abs
import kotlin.math.min

/**
 * Metade esquerda: joystick (o dedo pode estar em qualquer sítio da metade esquerda).
 * Metade direita: botão da bomba. Funciona com os dois dedos ao mesmo tempo.
 */
@Composable
fun Controls(modifier: Modifier, onDir: (Dir?) -> Unit, onBomb: () -> Unit) {
    val dirCb by rememberUpdatedState(onDir)
    val bombCb by rememberUpdatedState(onBomb)
    var knob by remember { mutableStateOf<Offset?>(null) }
    var active by remember { mutableStateOf<Dir?>(null) }
    var bombDown by remember { mutableStateOf(false) }

    Canvas(
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                val isPad = HashMap<PointerId, Boolean>()
                while (true) {
                    val ev = awaitPointerEvent()
                    val split = size.width * 0.55f
                    val center = Offset(size.width * 0.27f, size.height * 0.5f)
                    val radius = min(size.width * 0.21f, size.height * 0.38f)
                    for (c in ev.changes) {
                        if (c.changedToDown()) {
                            val pad = c.position.x < split
                            isPad[c.id] = pad
                            if (!pad) bombCb()
                        }
                        if (!c.pressed) isPad.remove(c.id)
                        c.consume()
                    }
                    bombDown = ev.changes.any { it.pressed && isPad[it.id] == false }
                    val finger = ev.changes.firstOrNull { it.pressed && isPad[it.id] == true }
                    val newDir = if (finger == null) {
                        knob = null
                        null
                    } else {
                        val v = finger.position - center
                        val d = v.getDistance()
                        knob = if (d > radius) v * (radius / d) else v
                        when {
                            d < radius * 0.18f -> null
                            // Histerese: só troca de eixo quando o outro eixo ganha com clareza,
                            // para não "tremer" nas diagonais.
                            active?.dx != 0 && active != null && abs(v.x) >= abs(v.y) * 0.75f ->
                                if (v.x > 0) Dir.RIGHT else Dir.LEFT
                            active?.dy != 0 && active != null && abs(v.y) >= abs(v.x) * 0.75f ->
                                if (v.y > 0) Dir.DOWN else Dir.UP
                            abs(v.x) > abs(v.y) -> if (v.x > 0) Dir.RIGHT else Dir.LEFT
                            else -> if (v.y > 0) Dir.DOWN else Dir.UP
                        }
                    }
                    if (newDir != active) {
                        active = newDir
                        dirCb(newDir)
                    }
                }
            }
        },
    ) {
        val center = Offset(size.width * 0.27f, size.height * 0.5f)
        val radius = min(size.width * 0.21f, size.height * 0.38f)
        // Joystick
        drawCircle(Color.White.copy(alpha = 0.08f), radius * 1.15f, center)
        drawCircle(Color.White.copy(alpha = 0.25f), radius * 1.15f, center, style = Stroke(radius * 0.03f))
        for (d in Dir.entries) {
            val angle = when (d) {
                Dir.UP -> 0f
                Dir.RIGHT -> 90f
                Dir.DOWN -> 180f
                Dir.LEFT -> 270f
            }
            val on = d == active
            rotate(angle, center) {
                val tip = Offset(center.x, center.y - radius * 0.98f)
                val arrow = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(tip.x - radius * 0.17f, tip.y + radius * 0.2f)
                    lineTo(tip.x + radius * 0.17f, tip.y + radius * 0.2f)
                    close()
                }
                drawPath(arrow, if (on) Color(0xFFFACC15) else Color.White.copy(alpha = 0.45f))
            }
        }
        val k = center + (knob ?: Offset.Zero)
        drawCircle(Color.Black.copy(alpha = 0.3f), radius * 0.45f, k + Offset(0f, radius * 0.05f))
        drawCircle(if (knob != null) Color(0xFFE5E7EB) else Color(0xFF9CA3AF), radius * 0.42f, k)
        drawCircle(Color.White.copy(alpha = 0.5f), radius * 0.42f, k, style = Stroke(radius * 0.04f))

        // Botão da bomba
        val bc = Offset(size.width * 0.76f, size.height * 0.5f)
        val br = min(size.width * 0.17f, size.height * 0.32f)
        val press = if (bombDown) 0.92f else 1f
        drawCircle(Color.Black.copy(alpha = 0.35f), br, bc + Offset(0f, br * 0.08f))
        drawCircle(if (bombDown) Color(0xFFB91C1C) else Color(0xFFDC2626), br * press, bc)
        drawCircle(Color.White.copy(alpha = 0.25f), br * press, bc, style = Stroke(br * 0.05f))
        drawBomb(bc.x, bc.y, br * 1.05f * press, 0f, 0f)
    }
}
