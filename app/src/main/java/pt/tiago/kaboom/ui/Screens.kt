package pt.tiago.kaboom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import pt.tiago.kaboom.audio.Feedback
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.Game
import pt.tiago.kaboom.game.GameEvent
import pt.tiago.kaboom.game.Phase

private val Accent = Color(0xFFF97316)
private val Gold = Color(0xFFFACC15)
private val Panel = Color(0xE6111827)

private val bigTitle = TextStyle(
    fontSize = 56.sp,
    fontWeight = FontWeight.Black,
    brush = Brush.verticalGradient(listOf(Gold, Accent, Color(0xFFDC2626))),
    shadow = Shadow(Color.Black, Offset(4f, 6f), 4f),
)

@Composable
fun TitleScreen(best: Int, bestLevel: Int, onPlay: () -> Unit) {
    val anim = rememberInfiniteTransition(label = "title")
    val t by anim.animateFloat(0f, 100f, infiniteRepeatable(tween(100_000, easing = LinearEasing)), label = "t")
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF14532D), Color(0xFF0B1F12))))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(170.dp)) {
            val s = size.minDimension
            drawBomb(s / 2, s / 2, s * 1.1f, (t % 2.6f) / 2.6f, t)
        }
        Text("KABOOM!", style = bigTitle)
        Spacer(Modifier.height(8.dp))
        Text(
            "Rebenta os blocos, apanha poderes,\nderrota os monstros e encontra a porta.",
            color = Color(0xFFD1FAE5),
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            modifier = Modifier.width(220.dp).height(64.dp),
        ) {
            Text("JOGAR", fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(28.dp))
        if (best > 0) {
            Text("🏆 Recorde: $best", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Melhor nível: $bestLevel", color = Color(0xFFD1FAE5), fontSize = 15.sp)
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Legend("💣", "+1 bomba")
            Legend("🔥", "+alcance")
            Legend("⚡", "+velocidade")
        }
    }
}

@Composable
private fun Legend(icon: String, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 26.sp)
        Text(text, color = Color(0xFFA7F3D0), fontSize = 12.sp)
    }
}

@Composable
fun GameScreen(feedback: Feedback, onGameOver: (score: Int, level: Int) -> Unit, onExit: () -> Unit) {
    val game = remember { Game().apply { newGame() } }
    var frame by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(feedback.soundOn) }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { paused = true }
    BackHandler { paused = !paused }

    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    // Limite ao dt: se o telemóvel engasgar, o jogo abranda em vez de "saltar"
                    val dt = ((now - last) / 1e9f).coerceAtMost(1f / 20f)
                    game.update(dt)
                    for (e in game.events) feedback.on(e)
                    if (GameEvent.GAME_OVER in game.events) onGameOver(game.score, game.level)
                    game.events.clear()
                }
                last = now
                frame = now
            }
        }
    }

    // Ler o frame aqui faz o HUD e os avisos actualizarem-se a cada frame.
    @Suppress("UNUSED_VARIABLE") val tick = frame

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud(game, onPause = { paused = true })
            Box(Modifier.fillMaxWidth().aspectRatio(Game.W.toFloat() / Game.H)) {
                Canvas(Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION") frame // redesenha a cada frame
                    drawGame(game)
                }
                Banner(game)
            }
            Controls(
                Modifier.fillMaxWidth().weight(1f).background(Color(0xFF0F172A)),
                onDir = { d: Dir? -> game.input = d },
                onBomb = { game.requestBomb() },
            )
        }

        if (game.phase == Phase.GAME_OVER) {
            Overlay {
                Text("FIM DE JOGO", style = bigTitle.copy(fontSize = 42.sp))
                Spacer(Modifier.height(12.dp))
                Text("Pontos: ${game.score}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Chegaste ao nível ${game.level}", color = Color(0xFFCBD5E1), fontSize = 17.sp)
                Spacer(Modifier.height(28.dp))
                BigButton("Jogar outra vez") { game.newGame() }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onExit, modifier = Modifier.width(240.dp)) { Text("Menu", fontSize = 18.sp) }
            }
        } else if (paused) {
            Overlay {
                Text("PAUSA", style = bigTitle.copy(fontSize = 44.sp))
                Spacer(Modifier.height(28.dp))
                BigButton("Continuar") { paused = false }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        soundOn = !soundOn
                        feedback.soundOn = soundOn
                    },
                    modifier = Modifier.width(240.dp),
                ) { Text(if (soundOn) "🔊 Som ligado" else "🔇 Som desligado", fontSize = 18.sp) }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onExit, modifier = Modifier.width(240.dp)) { Text("Sair para o menu", fontSize = 18.sp) }
            }
        }
    }
}

@Composable
private fun Hud(game: Game, onPause: () -> Unit) {
    val p = game.player
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HudText("❤️ ${game.lives}")
        HudText("NÍVEL ${game.level}", Gold)
        HudText("💣${p.maxBombs} 🔥${p.range} ⚡${p.speedUps}")
        HudText("${game.score}", Color.White)
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .clickable(onClick = onPause),
            contentAlignment = Alignment.Center,
        ) { Text("II", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
    }
}

@Composable
private fun HudText(text: String, color: Color = Color(0xFFE2E8F0)) {
    Text(text, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Banner(game: Game) {
    val text: String?
    val sub: String?
    when {
        game.phase == Phase.START -> {
            text = "NÍVEL ${game.level}"
            sub = "Prepara-te!"
        }
        game.phase == Phase.LEVEL_CLEAR -> {
            text = "NÍVEL CONCLUÍDO!"
            sub = "+${500 + 100 * game.level} pontos"
        }
        game.phase == Phase.PLAYING && game.time - game.doorOpenedAt < 2.2f -> {
            text = "A PORTA ABRIU!"
            sub = "Encontra a saída"
        }
        else -> {
            text = null
            sub = null
        }
    }
    if (text == null) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xB3000000))
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text, style = bigTitle.copy(fontSize = 34.sp))
            if (sub != null) Text(sub, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Panel)
            // Engole os toques para não chegarem aos controlos por baixo
            .clickable(enabled = true, indication = null, interactionSource = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Accent),
        modifier = Modifier.width(240.dp).height(56.dp),
    ) { Text(text, fontSize = 20.sp, fontWeight = FontWeight.Black) }
}
