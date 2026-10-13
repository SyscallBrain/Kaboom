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
    )

    private val neighbours = listOf(0 to -1, -1 to 0, 1 to 0, 0 to 1, -1 to -1, 1 to -1, -1 to 1, 1 to 1)

    /** Converte uma grelha em píxeis ARGB. [white] = silhueta branca (para o piscar ao morrer). */
    fun rasterize(grid: List<String>, white: Boolean = false): IntArray {
        val h = grid.size
        val w = grid[0].length
        val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val ch = grid[y][x]
            if (ch == '.') continue
            var rgb = colors.getValue(ch)
            if (ch == 'k') {
                val near = neighbours.firstNotNullOfOrNull { (dx, dy) ->
                    grid.getOrNull(y + dy)?.getOrNull(x + dx)?.takeIf { it != '.' && it != 'k' && it != 'e' }
                }
                if (near != null) rgb = darken(colors.getValue(near))
            }
            if (white) rgb = 0xffffff
            val alpha = if (ch == 'g' || ch == 'G') GHOST_ALPHA else 0xff
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
