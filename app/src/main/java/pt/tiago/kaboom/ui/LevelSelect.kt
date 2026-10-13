package pt.tiago.kaboom.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.Game
import pt.tiago.kaboom.game.LevelDef
import pt.tiago.kaboom.game.Levels
import pt.tiago.kaboom.ui.pixel.Art
import kotlin.math.max

/**
 * Escolha de nível: cada nível mostra uma miniatura do próprio mapa.
 * [unlocked] = nível mais alto disponível (Levels.count + 1 = modo infinito).
 */
@Composable
fun LevelSelectScreen(unlocked: Int, onPick: (Int) -> Unit, onBack: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(Ui.Night),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
                Text("MUNDO 1", style = pixelTitle(26.sp))
                Spacer(Modifier.height(10.dp))
                Text("O PRADO", style = pixelText(12.sp, Ui.Muted))
            }
        }
        itemsIndexed(Levels.world1) { i, def ->
            val n = i + 1
            LevelCard(n, def, open = n <= unlocked, onClick = { onPick(n) })
        }
        item(span = { GridItemSpan(2) }) {
            EndlessCard(open = unlocked > Levels.count, onClick = { onPick(Levels.count + 1) })
        }
        item(span = { GridItemSpan(2) }) {
            PixelButton("VOLTAR", Modifier.fillMaxWidth(), onClick = onBack)
        }
    }
}

@Composable
private fun LevelCard(n: Int, def: LevelDef, open: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .background(Ui.Panel)
            .border(3.dp, if (open) Ui.PanelEdge else Ui.Ink)
            .clickable(enabled = open, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(Game.W.toFloat() / Game.H), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) { drawLevelPreview(def) }
            if (!open) {
                Box(Modifier.fillMaxSize().background(Color(0xCC160F26)))
                Text("?", style = pixelTitle(32.sp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("NÍVEL $n", style = pixelText(12.sp, if (open) Ui.Gold else Ui.Muted))
        Spacer(Modifier.height(6.dp))
        Text(
            if (open) def.name else "BLOQUEADO",
            style = pixelText(8.sp, Ui.Muted),
            textAlign = TextAlign.Center,
            minLines = 2,
        )
    }
}

@Composable
private fun EndlessCard(open: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Ui.Panel)
            .border(3.dp, if (open) Ui.Orange else Ui.Ink)
            .clickable(enabled = open, onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("MODO INFINITO", style = pixelText(14.sp, if (open) Ui.Gold else Ui.Muted))
        Spacer(Modifier.height(10.dp))
        Text(
            if (open) "Níveis gerados ao acaso, cada vez mais difíceis." else "Termina os 8 níveis do Mundo 1 para desbloquear.",
            color = Ui.Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Miniatura do mapa com os sprites verdadeiros (os poderes e a porta ficam escondidos). */
private fun DrawScope.drawLevelPreview(def: LevelDef) {
    val k = max(1, (size.width / (Game.W * ART)).toInt())
    val tile = ART * k
    val ox = ((size.width - Game.W * tile) / 2).toInt()
    val oy = ((size.height - Game.H * tile) / 2).toInt()
    for ((y, row) in def.rows.withIndex()) for ((x, c) in row.withIndex()) {
        val px = ox + x * tile
        val py = oy + y * tile
        sprite(if ((x + y) % 2 == 0) Art.floor else Art.floor2, px, py, k)
        when (c) {
            '#' -> sprite(Art.wall, px, py, k)
            'B', 'D', 'b', 'f', 's' -> sprite(Art.brick, px, py, k)
            'P' -> sprite(Art.hero(Dir.DOWN, 0), px, py, k)
            'E' -> sprite(Art.blob, px, py, k)
            'Y' -> sprite(Art.bat[0], px, py, k)
            'G' -> sprite(Art.ghost, px, py, k)
        }
    }
}
