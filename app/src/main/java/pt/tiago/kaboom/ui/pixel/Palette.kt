package pt.tiago.kaboom.ui.pixel

/**
 * Paleta "16 bits" (proposta B). Cada letra dos sprites é um papel de cor.
 * O contorno 'k' não tem cor fixa: toma uma versão escura da cor vizinha
 * ("selective outline", como nos jogos da Super Nintendo).
 */
object Palette {
    private const val GHOST_ALPHA = 0xC7 // fantasmas ligeiramente transparentes

    val colors: Map<Char, Int> = mapOf(
        'k' to 0x1d1430, 'w' to 0xfff7e8, 's' to 0xffc9a0, 'S' to 0xd8875a,
        'y' to 0xffcf3a, 'Y' to 0xc9851a, 'l' to 0xfffde0,
        'b' to 0x3a6df0, 'B' to 0x2440a8, 'o' to 0x9a5a32, 'O' to 0x5a301c, 'e' to 0x24152e,
        'p' to 0xff7ab8, 'P' to 0xc23a86, 'v' to 0x9a6cff, 'V' to 0x5a37b8, 'r' to 0xff4a3a,
        'g' to 0xf2f4ff, 'G' to 0xaab4e0,
        'd' to 0x3d3a52, 'D' to 0x22202f, 'f' to 0xb07a3a, 'F' to 0xff7a1a, 'Q' to 0xffe35a, 'M' to 0x8a8aa0,
        'h' to 0xf0985c, 'c' to 0xc8582a, 'C' to 0x6e2614,
        'm' to 0x8e8aa6, 'n' to 0xc4c0da,
        '1' to 0x3aa64a, '2' to 0x2b8a3e, '3' to 0x6fd06a, '4' to 0x349a44,
        'R' to 0xe0402a, 'U' to 0x3a6df0, 'N' to 0x2fa84a,
        '5' to 0x14204a, '6' to 0x2f8cff, '7' to 0xc8f4ff,
        // cobra, boneco de neve, fundos dos poderes novos, bolha do escudo
        'A' to 0x6ccf4a, 'T' to 0x2f7d2a, 'I' to 0xf4f8ff, 'J' to 0xa9c4e8,
        'K' to 0xe08a2a, 'X' to 0x8a4ad0, 'H' to 0x2fb8c8, 'W' to 0xb0307a, 'L' to 0xff9ac8,
        'Z' to 0x7fe8ff, 'q' to 0x8a1c14,
    )

    /**
     * Cenários: cada mundo troca só as cores do chão (1-4), das paredes (m n M)
     * e dos tijolos (h c C). As personagens nunca usam estas letras.
     */
    enum class Theme(val overrides: Map<Char, Int>) {
        MEADOW(emptyMap()),
        DESERT(
            mapOf(
                '1' to 0xe3c07a, '2' to 0xcfa660, '3' to 0xf2d89a, '4' to 0xdbb66c,
                'm' to 0xc08a58, 'n' to 0xe6b682, 'M' to 0x7e5232,
                'h' to 0xe8a060, 'c' to 0xc06a30, 'C' to 0x6a3414,
            ),
        ),
        ICE(
            mapOf(
                '1' to 0xe6f2ff, '2' to 0xc9def5, '3' to 0xffffff, '4' to 0xd9e9fb,
                'm' to 0x6fa8dc, 'n' to 0xb8dcff, 'M' to 0x3d6fa8,
                'h' to 0xc4f0ff, 'c' to 0x7fcbe8, 'C' to 0x3a7aa8,
            ),
        ),
        CASTLE(
            mapOf(
                '1' to 0x6e6a80, '2' to 0x5e5a70, '3' to 0x85819a, '4' to 0x66627a,
                'm' to 0x4a4560, 'n' to 0x7a7594, 'M' to 0x2a2640,
                'h' to 0xc99a5a, 'c' to 0x9a6a36, 'C' to 0x4e3018,
            ),
        ),
    }

    private val neighbours = listOf(0 to -1, -1 to 0, 1 to 0, 0 to 1, -1 to -1, 1 to -1, -1 to 1, 1 to 1)

    /** Converte uma grelha em píxeis ARGB. [white] = silhueta branca (para o piscar ao morrer). */
    fun rasterize(grid: List<String>, white: Boolean = false, theme: Theme = Theme.MEADOW): IntArray {
        fun color(ch: Char) = theme.overrides[ch] ?: colors.getValue(ch)
        val h = grid.size
        val w = grid[0].length
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val ch = grid[y][x]
            if (ch == '.') continue
            var rgb = color(ch)
            if (ch == 'k') {
                val near = neighbours.firstNotNullOfOrNull { (dx, dy) ->
                    grid.getOrNull(y + dy)?.getOrNull(x + dx)?.takeIf { it != '.' && it != 'k' && it != 'e' }
                }
                if (near != null) rgb = darken(color(near))
            }
            if (white) rgb = 0xffffff
            val alpha = when (ch) {
                'g', 'G' -> GHOST_ALPHA
                'Z' -> 0xB0
                else -> 0xff
            }
            out[y * w + x] = (alpha shl 24) or rgb
        }
        return out
    }

    private fun darken(rgb: Int): Int {
        val r = ((rgb shr 16) and 0xff) * 0.42
        val g = ((rgb shr 8) and 0xff) * 0.42
        val b = (rgb and 0xff) * 0.42 * 1.08 + 8
        fun c(v: Double) = v.toInt().coerceIn(0, 255)
        return (c(r) shl 16) or (c(g) shl 8) or c(b)
    }
}
