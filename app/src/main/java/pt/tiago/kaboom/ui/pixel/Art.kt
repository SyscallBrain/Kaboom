package pt.tiago.kaboom.ui.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import pt.tiago.kaboom.R
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.ItemType

/** Letra pixel art (Press Start 2P, licença OFL — ver licenses/). */
val PixelFont = FontFamily(Font(R.font.press_start_2p))

/**
 * Todos os sprites já convertidos em imagens, criados uma só vez.
 * Desenham-se sempre ampliados por um número inteiro e sem suavização.
 */
object Art {
    private fun img(grid: List<String>, white: Boolean = false): ImageBitmap {
        val w = grid[0].length
        val h = grid.size
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(Palette.rasterize(grid, white), 0, w, 0, 0, w, h)
        return bmp.asImageBitmap()
    }

    private val heroFrames: Map<Dir, List<ImageBitmap>> by lazy {
        mapOf(
            Dir.DOWN to Sprites.heroDown.map { img(it) },
            Dir.UP to Sprites.heroUp.map { img(it) },
            Dir.RIGHT to Sprites.heroRight.map { img(it) },
            Dir.LEFT to Sprites.heroLeft.map { img(it) },
        )
    }

    fun hero(facing: Dir, frame: Int): ImageBitmap = heroFrames.getValue(facing)[frame % 4]
    val heroDead by lazy { img(Sprites.heroDead) }
    val heroDeadWhite by lazy { img(Sprites.heroDead, white = true) }

    val blob by lazy { img(Sprites.blob) }
    val blobWhite by lazy { img(Sprites.blob, white = true) }
    val bat by lazy { listOf(img(Sprites.bat1), img(Sprites.bat2)) }
    val batWhite by lazy { img(Sprites.bat1, white = true) }
    val ghost by lazy { img(Sprites.ghost) }
    val ghostWhite by lazy { img(Sprites.ghost, white = true) }
    val poof by lazy { (0 until 4).map { img(Sprites.poof(it)) } }

    val bomb by lazy { listOf(img(Sprites.bomb1), img(Sprites.bomb2)) }
    val bombHot by lazy { listOf(img(Sprites.bombHot1), img(Sprites.bombHot2)) }

    val brick by lazy { img(Sprites.brick) }
    val burning by lazy { (0 until 4).map { img(Sprites.burning(it)) } }
    val wall by lazy { img(Sprites.wall) }
    val floor by lazy { img(Sprites.floor) }
    val floor2 by lazy { img(Sprites.floor2) }
    val door by lazy { img(Sprites.door) }
    val doorOpen by lazy { (0 until 8).map { img(Sprites.doorOpen(it)) } }

    private val flames by lazy {
        Sprites.FlameKind.entries.associateWith { k -> (0..2).map { img(Sprites.flame(k, it)) } }
    }

    fun flame(kind: Sprites.FlameKind, phase: Int): ImageBitmap = flames.getValue(kind)[phase]

    private val items by lazy { ItemType.entries.associateWith { img(Sprites.item(it)) } }
    fun item(type: ItemType): ImageBitmap = items.getValue(type)

    // Ícones da barra de cima (12×12, com contorno)
    val iconHeart by lazy { img(Sprites.outlined(Sprites.iconHeart)) }
    private val icons by lazy { ItemType.entries.associateWith { img(Sprites.outlined(Sprites.iconFor(it))) } }
    fun icon(type: ItemType): ImageBitmap = icons.getValue(type)
}
