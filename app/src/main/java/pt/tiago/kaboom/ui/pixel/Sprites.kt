package pt.tiago.kaboom.ui.pixel

import pt.tiago.kaboom.game.ItemType
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Todos os sprites do jogo, como grelhas de texto de 16×16 (ícones 10×10).
 * Cada letra é um "papel" de cor ('.' = transparente); a cor real vem de [Palette].
 * Código puro, sem Android: testado em JVM (dimensões e cores definidas).
 */
object Sprites {

    // ------------------------------------------------------------ herói
    private val front = listOf(
        "......kkkk......", "....kkyyyykk....", "...kyyywwyyyk...", "..kyyyyllyyyyk..",
        "..kYYYYYYYYYYk..", ".kYYYYYYYYYYYYk.", "..kssssssssssk..", "..kssessssessk..",
        "..kssessssessk..", "..kSssskksssSk..", "...kSSSSSSSSk...", "...kbbbbbbbbk...",
        "..kskbbyybbksk..", "..kskBBBBBBksk..", "...koookkoook...", "...kkkk..kkkk...",
    )
    private val back = listOf(
        "......kkkk......", "....kkyyyykk....", "...kyyyyyyyyk...", "..kyyyyyyyyyyk..",
        "..kYYYYYYYYYYk..", ".kYYYYYYYYYYYYk.", "..kooooooooook..", "..kooooooooook..",
        "..kooooooooook..", "..kSooooooooSk..", "...kSSSSSSSSk...", "...kbbbbbbbbk...",
        "..kskbbbbbbksk..", "..kskBBBBBBksk..", "...koookkoook...", "...kkkk..kkkk...",
    )
    private val side = listOf(
        "......kkkk......", "....kkyyyykk....", "...kyyyyyyywk...", "..kyyyyyyyyllk..",
        "..kYYYYYYYYYYYk.", ".kYYYYYYYYYYYYYk", "..koooossssssk..", "..kooosssssesk..",
        "..kooosssssesk..", "..kSoosssssssk..", "...kSSSSSSSSk...", "...kbbbbbbbbk...",
        "...kbbbbbbskk...", "...kbBBBBBBk....", "....kooooook....", "....kkkkkkkk....",
    )
    private val legsFrontB = listOf("...koook..kook..", "...kkkk...kk....")
    private val legsFrontC = listOf("..kook..koook...", "....kk...kkkk...")
    private val legsSideB = listOf("...kook..kook...", "...kkk...kkk....")
    private val legsSideC = listOf(".....koooook....", ".....kkkkkk.....")

    private fun withLegs(base: List<String>, legs: List<String>) = base.take(14) + legs

    /** 4 frames de andar: parado, passo esquerdo, parado, passo direito. */
    val heroDown = listOf(front, withLegs(front, legsFrontB), front, withLegs(front, legsFrontC))
    val heroUp = listOf(back, withLegs(back, legsFrontB), back, withLegs(back, legsFrontC))
    val heroRight = listOf(side, withLegs(side, legsSideB), side, withLegs(side, legsSideC))
    val heroLeft = heroRight.map(::mirror)

    /** Olhos em X e boca aberta. */
    val heroDead = front.toMutableList().also {
        it[6] = "..k" + "sesesseses" + "k.."
        it[7] = "..k" + "ssessssess" + "k.."
        it[8] = "..k" + "sesesseses" + "k.."
        it[9] = "..k" + "SsssOOsssS" + "k.."
    }.toList()

    // ------------------------------------------------------------ inimigos
    val blob = listOf(
        "................", "................", "................", "......kkkk......",
        "....kkppppkk....", "...kppwwppppk...", "..kppwppppppPk..", "..kppwwppwwpPk..",
        ".kpppwkppwkppPk.", ".kppppppppppPPk.", ".kppppppppppPPk.", "kpppppppppppPPPk",
        "kPpppppppppPPPPk", "kPPPPPPPPPPPPPPk", ".kkkkkkkkkkkkkk.", "................",
    )
    val bat1 = listOf(
        "................", "................", "......kkkk......", "....kkvvvvkk....",
        "k..kvvvvvvvvk..k", "kk.kvvwwwwvvk.kk", "kVkkvwwwwwwvkkVk", "kVVkvwwrrwwvkVVk",
        "kVVkvwrkkrwvkVVk", "kVkkvwwrrwwvkkVk", "k..kvvwwwwvvk..k", "....kvvvvvvk....",
        ".....kvvvvk.....", "......kkkk......", "................", "................",
    )
    val bat2 = listOf(
        "................", "................", "k.....kkkk.....k", "kk..kkvvvvkk..kk",
        "kVkkvvvvvvvvkkVk", "kVVkvvwwwwvvkVVk", "kkkkvwwwwwwvkkkk", "...kvwwrrwwvk...",
        "...kvwrkkrwvk...", "...kvwwrrwwvk...", "....kvwwwwvk....", "....kvvvvvvk....",
        ".....kvvvvk.....", "......kkkk......", "................", "................",
    )
    val ghost = listOf(
        "................", "......kkkk......", "....kkggggkk....", "...kggggggggk...",
        "..kggggggggggk..", "..kggeeggeeggk..", ".kgggeeggeegggk.", ".kgggeeggeegggk.",
        ".kggggggggggGGk.", ".kgggggggggGGGk.", ".kggggggggggGGk.", ".kGggggggggGGGk.",
        ".kGGgGGggGGgGGk.", ".kGkkGGkkGGkkGk.", "..k..kk..kk..k..", "................",
    )

    val snake1 = listOf(
        "................", "......kkkk......", ".....kAAAAk.....", "....kAeAAeAk....",
        "....kAAAAAAk....", ".....kAArAk.....", "......kAAk......", "......kAAk......",
        ".....kAAAk......", "...kkAAAk.kkk...", "..kAAAAk.kAAAk..", ".kAATTAAkAATTAk.",
        ".kATTTTAAATTTAk.", "..kAAAAAAAAAAk..", "...kkkkkkkkkk...", "................",
    )
    val snake2 = snake1.toMutableList().also {
        it[5] = ".....kAAAAk....."
        it[11] = ".kATTAAAkAATTAk."
        it[12] = ".kAATTTAATTTAAk."
    }.toList()

    val snowman = listOf(
        ".....kkkkkk.....", ".....kDDDDk.....", "....kkDDDDkk....", ".....kIIIIk.....",
        "....kIeIIeIk....", "....kIIFFIJk....", ".....kRRRRk.....", "....kRRRRRRk....",
        "...kIIIdIIIJk...", "..kIIIIIIIIJJk..", "..kIIIIdIIIJJk..", "..kIIIIIIIIJJk..",
        "..kJIIIIIIJJJk..", "...kJJJJJJJJk...", "....kkkkkkkk....", "................",
    )

    /** Depois da primeira explosão: sem chapéu, mais pequeno (e mais rápido). */
    val snowmanSmall = listOf(
        "................", "................", "................", "................",
        "................", "................", ".....kkkkkk.....", "....kIeIIeIk....",
        "....kIIFFIJk....", "....kRRRRRRk....", "...kIIIIIIIJk...", "...kIIIdIIIJk...",
        "...kJIIIIIJJk...", "....kJJJJJJk....", ".....kkkkkk.....", "................",
    )

    val wizard1 = listOf(
        "........k.......", ".......kvk......", "......kvvQk.....", ".....kvvvvvk....",
        "....kVVVVVVVk...", ".....kseesk.k7k.", ".....kwwwwk..o..", "....kvwwwwvk.o..",
        "...kvvvvvvvvks..", "...kvvvvvvvvko..", "..kVvvvvvVk.o...", "..kVVvvvvVVk.o..",
        "..kVVVVVVVVk.o..", "...kkkkkkkk..o..", "...kook.kook.o..", "...kkk..kkk..k..",
    )
    val wizard2 = wizard1.map { it.replace('7', 'Q') }

    // ------------------------------------------------------------ bomba
    val bomb1 = listOf(
        "..........Q.....", ".........QFQ....", "..........f.....", ".........f......",
        "........kMk.....", ".....kkkkkk.....", "...kkddddddkk...", "..kddwwdddddDk..",
        "..kdwwddddddDk..", ".kddwdddddddDDk.", ".kdddddddddDDDk.", ".kdddddddddDDDk.",
        ".kDddddddddDDDk.", "..kDdddddDDDDk..", "...kkDDDDDDkk...", ".....kkkkkk.....",
    )
    val bomb2 = listOf("..........F.....", ".........FQF....") + bomb1.drop(2)

    /** Bomba do detonador: antena com luz a piscar em vez de rastilho. */
    val bombRemote1 = listOf(".........r......", ".........k......", ".........k......", ".........k......") + bomb1.drop(4)
    val bombRemote2 = listOf(".........q......") + bombRemote1.drop(1)

    /** Bolha do escudo à volta do herói. */
    val shieldBubble: List<String> = (0 until 16).map { y ->
        (0 until 16).map { x ->
            val d = hypot(x - 7.5, y - 7.5)
            when {
                x in 3..4 && y in 3..4 -> 'w'
                d in 6.6..7.6 -> 'Z'
                else -> '.'
            }
        }.joinToString("")
    }

    /** A piscar a vermelho quando está quase a rebentar. */
    val bombHot1 = bomb1.map { it.replace('d', 'R').replace('D', 'q') }
    val bombHot2 = bomb2.map { it.replace('d', 'R').replace('D', 'q') }

    // ------------------------------------------------------------ cenário
    val brick = listOf(
        "hhhhhhhChhhhhhhC", "cccccccCcccccccC", "cccccccCcccccccC", "CCCCCCCCCCCCCCCC",
        "hhhChhhhhhhhChhh", "cccCccccccccCccc", "cccCccccccccCccc", "CCCCCCCCCCCCCCCC",
        "hhhhhhhChhhhhhhC", "cccccccCcccccccC", "cccccccCcccccccC", "CCCCCCCCCCCCCCCC",
        "hhhChhhhhhhhChhh", "cccCccccccccCccc", "cccCccccccccCccc", "CCCCCCCCCCCCCCCC",
    )
    val wall = listOf(
        "nnnnnnnnnnnnnnnM", "nnnnnnnnnnnnnnMM", "nnmmmmmmmmmmmmMM", "nnmmmmmmmmmmmmMM",
        "nnmMmmmmmmmmMmMM", "nnmmmmmmmmmmmmMM", "nnmmmmmmmmmmmmMM", "nnmmmmmmmmmmmmMM",
        "nnmmmmmmmmmmmmMM", "nnmmmmmmmmmmmmMM", "nnmmmmmmmmmmmmMM", "nnmMmmmmmmmmMmMM",
        "nnmmmmmmmmmmmmMM", "nnmmmmmmmmmmmmMM", "nMMMMMMMMMMMMMMM", "MMMMMMMMMMMMMMMM",
    )
    val floor = listOf(
        "1111111111111111", "1111111111112111", "1131111111111111", "1313111111111111",
        "1111111111111111", "1111111211111111", "1111111111111311", "1111111111113131",
        "1111111111111111", "1121111111111111", "1111111111111111", "1111113111111111",
        "1111131311111111", "1111111111111121", "1111111111111111", "1111111111111111",
    )

    /** Mesma relva num tom diferente, para o xadrez do chão. */
    val floor2 = floor.map { it.replace('1', '4') }

    /** Lajes de pedra (castelo). */
    val floorStone = listOf(
        "3333333233333333", "1111111211111111", "1111111211111111", "1111111211111111",
        "1111111211111111", "1111111211111111", "1111111211111111", "2222222222222222",
        "3332333333333333", "1112111111111111", "1112111111111111", "1112111111111111",
        "1112111111111111", "1112111111111111", "1112111111111111", "2222222222222222",
    )
    val floorStone2 = floorStone.map { it.replace('1', '4') }

    val door = listOf(
        "....kkkkkkkk....", "..kkMMMMMMMMkk..", ".kMMkkkkkkkkMMk.", ".kMkooooooookMk.",
        "kMkooooooooookMk", "kMkooOooooOookMk", "kMkooOooooOookMk", "kMkooOooooOookMk",
        "kMkooOooooOookMk", "kMkooOooooOQokMk", "kMkooOooooOookMk", "kMkooOooooOookMk",
        "kMkooOooooOookMk", "kMkooOooooOookMk", "kMkooOooooOookMk", "kkkkkkkkkkkkkkkk",
    )

    /** Portal da porta aberta: anéis a rodar ([t] = frame da animação). */
    fun doorOpen(t: Int): List<String> = door.mapIndexed { y, row ->
        row.mapIndexed { x, ch ->
            if (ch != 'o' && ch != 'O' && ch != 'Q') {
                ch
            } else {
                val d = hypot(x - 7.5, y - 9.0) + t * 0.45
                "5676"[d.toInt() % 4]
            }
        }.joinToString("")
    }

    /** Tijolo a desfazer-se em fogo, 4 frames. */
    fun burning(f: Int): List<String> = brick.mapIndexed { y, row ->
        row.mapIndexed { x, ch ->
            val h = noise(x, y) % 8
            when (f) {
                0 -> if (h < 3) 'F' else if (h < 4) 'Q' else ch
                1 -> if (h < 3) 'Q' else if (h < 6) 'F' else ch
                2 -> if (h < 2) 'Q' else if (h < 5) 'F' else if (h < 6) 'C' else '.'
                else -> if (h < 2) 'F' else if (h == 2) 'C' else '.'
            }
        }.joinToString("")
    }

    /** Nuvem de fumo quando um inimigo morre, 4 frames. */
    fun poof(f: Int): List<String> = (0 until 16).map { y ->
        (0 until 16).map { x ->
            val d = hypot(x - 7.5, y - 7.5)
            val r = 3.0 + f * 1.5
            val gone = f >= 2 && noise(x, y) % 4 < f - 1
            when {
                d > r || gone -> '.'
                d > r - 1.5 -> 'G'
                f < 2 -> 'g'
                else -> '.'
            }
        }.joinToString("")
    }

    // ------------------------------------------------------------ fogo
    enum class FlameKind { CENTER, H, V, LEFT, RIGHT, UP, DOWN }

    /** Braço de fogo com camadas laranja/amarelo/branco; [phase] 0..2 = espessura. */
    fun flame(kind: FlameKind, phase: Int): List<String> {
        val half = 5 + phase
        val horiz = kind in setOf(FlameKind.CENTER, FlameKind.H, FlameKind.LEFT, FlameKind.RIGHT)
        val vert = kind in setOf(FlameKind.CENTER, FlameKind.V, FlameKind.UP, FlameKind.DOWN)
        return (0 until 16).map { y ->
            (0 until 16).map { x ->
                var d = 99.0
                if (horiz) {
                    var lim = half.toDouble()
                    if (kind == FlameKind.LEFT) lim = min(lim, x - 1.0)
                    if (kind == FlameKind.RIGHT) lim = min(lim, 14.0 - x)
                    val hx = abs(y - 7.5)
                    if (hx <= lim) d = min(d, hx / max(lim, 0.5))
                }
                if (vert) {
                    var lim = half.toDouble()
                    if (kind == FlameKind.UP) lim = min(lim, y - 1.0)
                    if (kind == FlameKind.DOWN) lim = min(lim, 14.0 - y)
                    val vx = abs(x - 7.5)
                    if (vx <= lim) d = min(d, vx / max(lim, 0.5))
                }
                when {
                    d > 1 -> '.'
                    d > 0.66 -> 'F'
                    d > 0.3 -> 'Q'
                    else -> 'w'
                }
            }.joinToString("")
        }
    }

    // ------------------------------------------------------------ poderes e ícones
    val iconFire = listOf(
        "....F.....", "...FF.....", "...FFF....", "..FFQFF...", "..FQQFF...",
        ".FFQQQFF..", ".FQQwQQF..", ".FQwwwQF..", "..FQwQF...", "...FFF....",
    )
    val iconBomb = listOf(
        ".......Q..", "......f...", "...kkkf...", "..kddddk..", ".kdwdddDk.",
        ".kwddddDk.", ".kddddDDk.", ".kdddDDDk.", "..kDDDDk..", "...kkkk...",
    )
    val iconSpeed = listOf(
        ".....QQQ..", "....QQQ...", "...QQQ....", "..QQQQQQ..", "....QQQ...",
        "...QQQ....", "..QQQ.....", ".QQ.......", "Q.........", "..........",
    )
    val iconKick = listOf(
        "..........", "..kkkk....", "..kook....", "..kook....", "..kook....",
        "..koookk..", "..kooooook", ".kooooooo.", ".kwwwwwwwk", "..kkkkkkk.",
    )
    val iconRemote = listOf(
        "....Q.....", "....k.....", "....k.....", "..kkkkkk..", "..kddddk..",
        "..kdRRdk..", "..kdRRdk..", "..kddddk..", "..kddddk..", "..kkkkkk..",
    )
    val iconShield = listOf(
        ".kkkkkkkk.", ".k776666k.", ".k766666k.", ".k666666k.", ".k666666k.",
        ".k666666k.", "..k6666k..", "..k6666k..", "...k66k...", "....kk....",
    )

    /** Fogo azul: o fogo perfurante. */
    val iconPierce = iconFire.map { it.replace('F', '6').replace('Q', '7') }
    val iconHeart = listOf(
        "..........", ".kk...kk..", "krrk.krrk.", "krwrkrrrk.", "krrrrrrrk.",
        ".krrrrrk..", "..krrrk...", "...krk....", "....k.....", "..........",
    )

    fun iconFor(type: ItemType) = when (type) {
        ItemType.BOMB -> iconBomb
        ItemType.FIRE -> iconFire
        ItemType.SPEED -> iconSpeed
        ItemType.KICK -> iconKick
        ItemType.REMOTE -> iconRemote
        ItemType.SHIELD -> iconShield
        ItemType.PIERCE -> iconPierce
        ItemType.LIFE -> iconHeart
    }

    /** Ícone com contorno de 1 píxel, numa grelha 12×12 (para a barra de cima). */
    fun outlined(icon: List<String>): List<String> {
        val g = Array(12) { CharArray(12) { '.' } }
        for (y in icon.indices) for (x in icon[y].indices) {
            if (icon[y][x] == '.') continue
            for ((dx, dy) in listOf(0 to -1, -1 to 0, 1 to 0, 0 to 1)) g[y + 1 + dy][x + 1 + dx] = 'k'
        }
        for (y in icon.indices) for (x in icon[y].indices) if (icon[y][x] != '.') g[y + 1][x + 1] = icon[y][x]
        return g.map { String(it) }
    }

    /** Poder no chão: moldura com a cor do poder e o ícone contornado no meio. */
    fun item(type: ItemType): List<String> {
        val bg = when (type) {
            ItemType.FIRE -> 'R'
            ItemType.BOMB -> 'U'
            ItemType.SPEED -> 'N'
            ItemType.KICK -> 'K'
            ItemType.REMOTE -> 'X'
            ItemType.SHIELD -> 'H'
            ItemType.PIERCE -> 'W'
            ItemType.LIFE -> 'L'
        }
        val g = Array(16) { y ->
            CharArray(16) { x ->
                val corner = (x == 1 || x == 14) && (y == 1 || y == 14)
                when {
                    x < 1 || x > 14 || y < 1 || y > 14 || corner -> '.'
                    x == 1 || x == 14 || y == 1 || y == 14 -> 'k'
                    x == 2 || y == 2 -> 'w'
                    else -> bg
                }
            }
        }
        val icon = outlined(iconFor(type))
        for (y in 0 until 12) for (x in 0 until 12) if (icon[y][x] != '.') g[y + 2][x + 2] = icon[y][x]
        return g.map { String(it) }
    }

    fun mirror(grid: List<String>) = grid.map { it.reversed() }

    /** Ruído fixo por píxel (sempre igual para o mesmo x,y), para padrões "aleatórios". */
    private fun noise(x: Int, y: Int): Int {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        return (h xor (h ushr 16)) and 0x7fffffff
    }
}
