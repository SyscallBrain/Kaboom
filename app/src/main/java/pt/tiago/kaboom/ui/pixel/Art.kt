package pt.tiago.kaboom.ui.pixel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import pt.tiago.kaboom.R
import pt.tiago.kaboom.game.BossKind
import pt.tiago.kaboom.game.Dir
import pt.tiago.kaboom.game.EnemyKind
import pt.tiago.kaboom.game.ItemType
import pt.tiago.kaboom.ui.pixel.Palette.Theme

/** Letra pixel art (Press Start 2P, licença OFL — ver licenses/). */
val PixelFont = FontFamily(Font(R.font.press_start_2p))

/** Cenário de um mundo: chão, paredes, tijolos e porta com as cores desse mundo. */
class TileSet(
    val floor: ImageBitmap,
    val floor2: ImageBitmap,
    val wall: ImageBitmap,
    val brick: ImageBitmap,
    val burning: List<ImageBitmap>,
    val door: ImageBitmap,
    val doorOpen: List<ImageBitmap>,
)

/**
 * Todos os sprites já convertidos em imagens, criados uma só vez.
 * Desenham-se sempre ampliados por um número inteiro e sem suavização.
 */
object Art {
    private fun img(grid: List<String>, white: Boolean = false, theme: Theme = Theme.MEADOW): ImageBitmap {
        val w = grid[0].length
        val h = grid.size
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(Palette.rasterize(grid, white, theme), 0, w, 0, 0, w, h)
        return bmp.asImageBitmap()
    }

    private val tileSets = HashMap<Theme, TileSet>()

    fun tiles(theme: Theme): TileSet = tileSets.getOrPut(theme) {
        val stone = theme == Theme.CASTLE
        TileSet(
            floor = img(if (stone) Sprites.floorStone else Sprites.floor, theme = theme),
            floor2 = img(if (stone) Sprites.floorStone2 else Sprites.floor2, theme = theme),
            wall = img(Sprites.wall, theme = theme),
            brick = img(Sprites.brick, theme = theme),
            burning = (0 until 4).map { img(Sprites.burning(it), theme = theme) },
            door = img(Sprites.door, theme = theme),
            doorOpen = (0 until 8).map { img(Sprites.doorOpen(it), theme = theme) },
        )
    }

    /** Cenário de cada mundo (0..3). */
    fun themeOf(world: Int): Theme = Theme.entries[world % Theme.entries.size]

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
    val shieldBubble by lazy { img(Sprites.shieldBubble) }

    /** Frames de animação de cada inimigo, e a versão já ferida (só o boneco de neve muda). */
    private val enemyFrames: Map<EnemyKind, List<ImageBitmap>> by lazy {
        mapOf(
            EnemyKind.BLOB to listOf(img(Sprites.blob)),
            EnemyKind.EYE to listOf(img(Sprites.bat1), img(Sprites.bat2)),
            EnemyKind.GHOST to listOf(img(Sprites.ghost)),
            EnemyKind.SNAKE to listOf(img(Sprites.snake1), img(Sprites.snake2)),
            EnemyKind.SNOWMAN to listOf(img(Sprites.snowman)),
            EnemyKind.WIZARD to listOf(img(Sprites.wizard1), img(Sprites.wizard2)),
        )
    }
    private val enemyWhite: Map<EnemyKind, ImageBitmap> by lazy {
        mapOf(
            EnemyKind.BLOB to img(Sprites.blob, white = true),
            EnemyKind.EYE to img(Sprites.bat1, white = true),
            EnemyKind.GHOST to img(Sprites.ghost, white = true),
            EnemyKind.SNAKE to img(Sprites.snake1, white = true),
            EnemyKind.SNOWMAN to img(Sprites.snowman, white = true),
            EnemyKind.WIZARD to img(Sprites.wizard1, white = true),
        )
    }
    private val snowmanSmall by lazy { img(Sprites.snowmanSmall) }
    private val snowmanSmallWhite by lazy { img(Sprites.snowmanSmall, white = true) }

    fun enemy(kind: EnemyKind, frame: Int, hurt: Boolean): ImageBitmap =
        if (kind == EnemyKind.SNOWMAN && hurt) snowmanSmall else enemyFrames.getValue(kind).let { it[frame % it.size] }

    fun enemyWhite(kind: EnemyKind, hurt: Boolean): ImageBitmap =
        if (kind == EnemyKind.SNOWMAN && hurt) snowmanSmallWhite else enemyWhite.getValue(kind)

    val poof by lazy { (0 until 4).map { img(Sprites.poof(it)) } }

    val bomb by lazy { listOf(img(Sprites.bomb1), img(Sprites.bomb2)) }
    val bombHot by lazy { listOf(img(Sprites.bombHot1), img(Sprites.bombHot2)) }
    val bombRemote by lazy { listOf(img(Sprites.bombRemote1), img(Sprites.bombRemote2)) }
    val bombMagic by lazy { listOf(img(Sprites.bombMagic1), img(Sprites.bombMagic2)) }

    private val bossFrames by lazy { BossKind.entries.associateWith { k -> BossSprites.frames(k).map { img(it) } } }
    private val bossWhite by lazy { BossKind.entries.associateWith { k -> img(BossSprites.frames(k)[0], white = true) } }
    fun boss(kind: BossKind, frame: Int): ImageBitmap = bossFrames.getValue(kind).let { it[frame % it.size] }
    fun bossWhite(kind: BossKind): ImageBitmap = bossWhite.getValue(kind)

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
