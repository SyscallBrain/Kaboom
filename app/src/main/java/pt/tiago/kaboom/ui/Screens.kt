package pt.tiago.kaboom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import pt.tiago.kaboom.audio.Feedback
import pt.tiago.kaboom.audio.MusicPlayer
import pt.tiago.kaboom.audio.Song
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.EnemyKind
import pt.tiago.kaboom.game.Game
import pt.tiago.kaboom.game.GameEvent
import pt.tiago.kaboom.game.ItemType
import pt.tiago.kaboom.game.Levels
import pt.tiago.kaboom.game.Phase
import pt.tiago.kaboom.ui.pixel.Art
import pt.tiago.kaboom.ui.pixel.PixelFont
import kotlin.math.roundToInt

/** Cores da interface, tiradas da paleta 16 bits dos sprites. */
object Ui {
    val Night = Color(0xFF160F26)
    val Panel = Color(0xFF2A1F45)
    val PanelEdge = Color(0xFF4A3A78)
    val Ink = Color(0xFF1D1430)
    val Text = Color(0xFFF2F4FF)
    val Muted = Color(0xFFAAB4E0)
    val Gold = Color(0xFFFFCF3A)
    val Orange = Color(0xFFFF7A1A)
    val Red = Color(0xFFE0402A)
    val Scrim = Color(0xE6160F26)
}

fun pixelTitle(size: TextUnit) = TextStyle(
    fontFamily = PixelFont,
    fontSize = size,
    brush = Brush.verticalGradient(listOf(Color(0xFFFFE35A), Ui.Gold, Ui.Orange, Ui.Red)),
    shadow = Shadow(Ui.Ink, Offset(6f, 6f), 0f),
)

fun pixelText(size: TextUnit, color: Color = Ui.Text) = TextStyle(
    fontFamily = PixelFont,
    fontSize = size,
    color = color,
    shadow = Shadow(Ui.Ink, Offset(3f, 3f), 0f),
)

/** Botão quadrado ao estilo dos menus antigos: afunda quando se carrega. */
@Composable
fun PixelButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    height: Dp = 52.dp,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val face = if (primary) Ui.Orange else Ui.Panel
    val edge = if (primary) Color(0xFF8A3A08) else Ui.Ink
    val drop = if (pressed) 0.dp else 4.dp
    Box(modifier.height(height + 4.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = 4.dp)
                .background(edge),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .offset(y = 4.dp - drop)
                .background(face)
                .border(3.dp, edge)
                .clickable(interactionSource = source, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = pixelText(if (primary) 16.sp else 12.sp, if (primary) Color.White else Ui.Text))
        }
    }
}

@Composable
private fun PixelIcon(img: ImageBitmap, size: Dp) {
    Image(img, contentDescription = null, modifier = Modifier.size(size), filterQuality = FilterQuality.None)
}

// ------------------------------------------------------------------ menu inicial

@Composable
fun TitleScreen(best: Int, bestLevel: Int, onSettings: () -> Unit, onPlay: () -> Unit) {
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            if (last != 0L) t += (now - last) / 1e9f
            last = now
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1F45), Ui.Night)))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Cena de abertura: o herói a fugir da gota pela relva, com uma bomba no muro
        Canvas(Modifier.fillMaxWidth().aspectRatio(9f / 2f)) {
            val k = (size.width / (9 * ART)).toInt().coerceAtLeast(1)
            val ox = ((size.width - 9 * ART * k) / 2).toInt()
            val tile = ART * k
            val ts = Art.tiles(Art.themeOf(0))
            for (x in 0 until 9) {
                sprite(ts.wall, ox + x * tile, 0, k)
                sprite(if (x % 2 == 0) ts.floor else ts.floor2, ox + x * tile, tile, k)
            }
            val run = (t * 40f) % (12 * ART) - ART
            val hx = ox + run.roundToInt() * k
            sprite(Art.hero(Dir.RIGHT, (t * 8).toInt() % 4), hx, tile, k)
            sprite(Art.enemy(EnemyKind.BLOB, 0, false), hx - 2 * tile, tile + if ((t * 4).toInt() % 2 == 0) 0 else k, k)
            sprite(Art.bomb[(t * 10).toInt() % 2], ox + 4 * tile, 0, k)
        }
        Spacer(Modifier.height(28.dp))
        Text("KABOOM!", style = pixelTitle(38.sp))
        Spacer(Modifier.height(16.dp))
        Text(
            "Rebenta os blocos, apanha poderes, derrota os monstros e encontra a porta.",
            color = Ui.Muted,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(32.dp))
        PixelButton("JOGAR", Modifier.width(240.dp), primary = true, height = 64.dp, onClick = onPlay)
        Spacer(Modifier.height(14.dp))
        PixelButton("DEFINIÇÕES", Modifier.width(240.dp), onClick = onSettings)
        Spacer(Modifier.height(28.dp))
        if (best > 0) {
            Text("RECORDE $best", style = pixelText(14.sp, Ui.Gold))
            Spacer(Modifier.height(8.dp))
            Text("MELHOR NÍVEL $bestLevel", style = pixelText(10.sp, Ui.Muted))
            Spacer(Modifier.height(24.dp))
        }
        // Os 8 poderes, em duas filas
        for (row in ItemType.entries.chunked(4)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (type in row) Legend(Art.item(type), itemLabel(type))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

fun itemLabel(type: ItemType) = when (type) {
    ItemType.BOMB -> "BOMBA"
    ItemType.FIRE -> "FOGO"
    ItemType.SPEED -> "RAPIDEZ"
    ItemType.KICK -> "CHUTO"
    ItemType.REMOTE -> "DETONAR"
    ItemType.SHIELD -> "ESCUDO"
    ItemType.PIERCE -> "PERFURA"
    ItemType.LIFE -> "VIDA"
}

@Composable
private fun Legend(img: ImageBitmap, text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
        PixelIcon(img, 40.dp)
        Spacer(Modifier.height(6.dp))
        Text(text, style = pixelText(8.sp, Ui.Muted))
    }
}

// ------------------------------------------------------------------ jogo

@Composable
fun GameScreen(
    startLevel: Int,
    feedback: Feedback,
    music: MusicPlayer,
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onGameOver: (score: Int, level: Int) -> Unit,
    onLevelCleared: (level: Int) -> Unit,
    onExit: () -> Unit,
) {
    val game = remember { Game().apply { newGame(startLevel) } }
    val effects = remember { Effects() }
    val measurer = rememberTextMeasurer()
    var frame by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    effects.shakeLevel = settings.shake

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
                    if (GameEvent.LEVEL_CLEAR in game.events) onLevelCleared(game.level)
                    game.events.clear()
                    effects.consume(game.fx)
                    game.fx.clear()
                    effects.update(dt)
                }
                last = now
                frame = now
            }
        }
    }

    // O Compose não vê mudanças dentro do objecto Game (é sempre a mesma instância),
    // por isso o HUD e os avisos recebem valores simples, lidos a cada frame.
    @Suppress("UNUSED_VARIABLE") val tick = frame
    val p = game.player
    val specials = buildList {
        if (p.kick) add(ItemType.KICK)
        if (p.remote) add(ItemType.REMOTE)
        if (p.shield) add(ItemType.SHIELD)
        if (p.pierce) add(ItemType.PIERCE)
    }
    val hud = HudState(game.lives, Levels.title(game.level), p.maxBombs, p.range, p.speedUps, game.score, specials)
    val bossBar = game.boss?.takeIf { !it.defeated }?.let { it.kind.title to it.hp.coerceAtLeast(0) }

    // Música do mundo (ou do boss) enquanto se joga; silêncio ao morrer, ao passar o nível e no fim
    val song = when (game.phase) {
        Phase.START, Phase.PLAYING -> if (game.boss != null) Song.BOSS else Song.forWorld(Levels.worldOf(game.level))
        else -> null
    }
    LaunchedEffect(song) { music.play(song) }
    LaunchedEffect(paused || showSettings) { music.duck(paused || showSettings) }
    val banner = bannerFor(game)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Hud(hud, onPause = { paused = true })
            Box(Modifier.fillMaxWidth().aspectRatio(Game.W.toFloat() / Game.H).background(Ui.Ink)) {
                Canvas(Modifier.fillMaxSize().clipToBounds()) {
                    @Suppress("UNUSED_EXPRESSION") frame // redesenha a cada frame
                    val b = BoardLayout(size.width, size.height)
                    // O tremor anda em passos de píxel de arte, para não "desfocar" a pixel art
                    val sh = effects.shakeOffset()
                    val sx = (sh.x * ART).roundToInt() * b.k
                    val sy = (sh.y * ART).roundToInt() * b.k
                    translate((b.ox + sx).toFloat(), (b.oy + sy).toFloat()) {
                        drawGame(game, b.k)
                        effects.draw(this, b.k, measurer)
                    }
                    drawIris(game, Offset(b.ox + game.player.x * b.tile, b.oy + game.player.y * b.tile))
                }
                if (bossBar != null) BossBar(bossBar.first, bossBar.second, game.boss!!.kind.hp)
                if (banner != null) Banner(banner.first, banner.second)
            }
            Controls(
                Modifier.fillMaxWidth().weight(1f).background(Ui.Night),
                scheme = settings.scheme,
                leftHanded = settings.leftHanded,
                showDetonate = p.remote,
                onDir = { d: Dir? -> game.input = d },
                onBomb = { game.requestBomb() },
                onDetonate = { game.requestDetonate() },
            )
        }

        if (game.phase == Phase.GAME_OVER) {
            Overlay {
                Text("FIM DE JOGO", style = pixelTitle(26.sp))
                Spacer(Modifier.height(20.dp))
                Text("PONTOS ${game.score}", style = pixelText(16.sp))
                Spacer(Modifier.height(10.dp))
                Text("CHEGASTE AO ${Levels.title(game.level)}", style = pixelText(10.sp, Ui.Muted))
                Spacer(Modifier.height(32.dp))
                PixelButton("OUTRA VEZ", Modifier.width(260.dp), primary = true) {
                    effects.clear()
                    game.newGame(startLevel)
                }
                Spacer(Modifier.height(14.dp))
                PixelButton("ESCOLHER NÍVEL", Modifier.width(260.dp), onClick = onExit)
            }
        } else if (paused) {
            Overlay {
                Text("PAUSA", style = pixelTitle(32.sp))
                Spacer(Modifier.height(32.dp))
                PixelButton("CONTINUAR", Modifier.width(260.dp), primary = true) { paused = false }
                Spacer(Modifier.height(14.dp))
                PixelButton("DEFINIÇÕES", Modifier.width(260.dp)) { showSettings = true }
                Spacer(Modifier.height(14.dp))
                PixelButton("ESCOLHER NÍVEL", Modifier.width(260.dp), onClick = onExit)
            }
        }
        if (showSettings) {
            BackHandler { showSettings = false }
            SettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onPreviewSound = feedback::previewSound,
                onPreviewVibration = feedback::previewVibration,
                onBack = { showSettings = false },
            )
        }
    }
}

/** Linha 1: vidas, nível, pontos, pausa. Linha 2: poderes (os especiais só quando os tens). */
@Composable
private fun Hud(h: HudState, onPause: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Ui.Night).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HudStat(Art.iconHeart, "${h.lives}")
            Text(h.level, style = pixelText(11.sp, Ui.Gold))
            Text("${h.score}", style = pixelText(11.sp))
            Box(
                Modifier
                    .size(36.dp)
                    .background(Ui.Panel)
                    .border(3.dp, Ui.Ink)
                    .clickable(onClick = onPause),
                contentAlignment = Alignment.Center,
            ) { Text("II", style = pixelText(12.sp)) }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            HudStat(Art.icon(ItemType.BOMB), "${h.bombs}")
            HudStat(Art.icon(ItemType.FIRE), "${h.range}")
            HudStat(Art.icon(ItemType.SPEED), "${h.speed}")
            for (type in h.specials) PixelIcon(Art.icon(type), 24.dp)
        }
    }
}

@Composable
private fun HudStat(icon: ImageBitmap, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        PixelIcon(icon, 24.dp)
        Text(value, style = pixelText(10.sp))
    }
}

private data class HudState(
    val lives: Int,
    val level: String,
    val bombs: Int,
    val range: Int,
    val speed: Int,
    val score: Int,
    val specials: List<ItemType>,
)

/** Título e subtítulo do aviso a mostrar por cima do mapa, ou null se não houver. */
private fun bannerFor(game: Game): Pair<String, String>? = when {
    game.phase == Phase.START && game.boss != null -> "BOSS!" to (game.levelName ?: "")
    game.phase == Phase.START -> Levels.title(game.level) to (game.levelName ?: "MODO INFINITO")
    game.phase == Phase.LEVEL_CLEAR && Levels.isWorldEnd(game.level) ->
        "VITÓRIA!" to "${Levels.worlds[Levels.worldOf(game.level)].name} CONCLUÍDO"
    game.phase == Phase.LEVEL_CLEAR -> "CONCLUÍDO!" to "+${500 + 100 * game.level} PONTOS"
    game.phase == Phase.PLAYING && game.time - game.doorOpenedAt < 2.2f -> "PORTA ABERTA!" to "ENCONTRA A SAÍDA"
    else -> null
}

@Composable
private fun Banner(text: String, sub: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .background(Ui.Scrim)
                .border(4.dp, Ui.PanelEdge)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text, style = pixelTitle(22.sp))
            Spacer(Modifier.height(12.dp))
            Text(sub, style = pixelText(10.sp))
        }
    }
}

/** Nome do boss e a vida em blocos, no topo do mapa. */
@Composable
private fun BossBar(name: String, hp: Int, max: Int) {
    Box(Modifier.fillMaxSize().padding(top = 6.dp), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.background(Ui.Scrim).border(3.dp, Ui.PanelEdge).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(name, style = pixelText(9.sp, Ui.Gold))
            Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(max) { i ->
                    Box(
                        Modifier
                            .size(width = 14.dp, height = 9.dp)
                            .background(if (i < hp) Ui.Red else Ui.Ink)
                            .border(1.dp, Ui.Ink),
                    )
                }
            }
        }
    }
}

@Composable
private fun Overlay(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Ui.Scrim)
            // Engole os toques para não chegarem aos controlos por baixo
            .clickable(enabled = true, indication = null, interactionSource = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
