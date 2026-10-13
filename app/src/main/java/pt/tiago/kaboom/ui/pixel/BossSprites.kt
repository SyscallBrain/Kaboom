package pt.tiago.kaboom.ui.pixel

import pt.tiago.kaboom.game.BossKind

/**
 * Bosses em 32×32, pintados com formas simples (elipses, triângulos, rectângulos) e
 * contornados no fim — com 1024 píxeis cada, escrevê-los à mão letra a letra seria ilegível.
 * Usam as mesmas letras de cor da [Palette] (nunca as dos cenários).
 */
object BossSprites {

    private class Painter {
        val g = Array(32) { CharArray(32) { '.' } }

        fun dot(x: Int, y: Int, c: Char) {
            if (x in 0 until 32 && y in 0 until 32) g[y][x] = c
        }

        fun ellipse(cx: Double, cy: Double, rx: Double, ry: Double, c: Char) {
            for (y in 0 until 32) for (x in 0 until 32) {
                val dx = (x + 0.5 - cx) / rx
                val dy = (y + 0.5 - cy) / ry
                if (dx * dx + dy * dy <= 1.0) g[y][x] = c
            }
        }

        fun rect(x0: Int, y0: Int, x1: Int, y1: Int, c: Char) {
            for (y in y0..y1) for (x in x0..x1) dot(x, y, c)
        }

        fun tri(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, c: Char) {
            fun side(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double) =
                (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2)
            for (y in 0 until 32) for (x in 0 until 32) {
                val px = x + 0.5
                val py = y + 0.5
                val d1 = side(px, py, ax, ay, bx, by)
                val d2 = side(px, py, bx, by, cx, cy)
                val d3 = side(px, py, cx, cy, ax, ay)
                val neg = d1 < 0 || d2 < 0 || d3 < 0
                val pos = d1 > 0 || d2 > 0 || d3 > 0
                if (!(neg && pos)) g[y][x] = c
            }
        }

        /** Espelha a metade esquerda para a direita (para formas simétricas). */
        fun mirror() {
            for (y in 0 until 32) for (x in 0 until 16) g[y][31 - x] = g[y][x]
        }

        /** Contorno 'k' à volta de tudo o que foi pintado. */
        fun outline(): List<String> {
            val out = Array(32) { y -> g[y].copyOf() }
            for (y in 0 until 32) for (x in 0 until 32) {
                if (g[y][x] != '.') continue
                val touches = listOf(0 to -1, -1 to 0, 1 to 0, 0 to 1).any { (dx, dy) ->
                    val c = g.getOrNull(y + dy)?.getOrNull(x + dx)
                    c != null && c != '.' && c != 'k'
                }
                if (touches) out[y][x] = 'k'
            }
            return out.map { String(it) }
        }
    }

    /** Rei Gota: gota gigante cor-de-rosa com coroa. [f] 0/1 = esticada/achatada. */
    fun kingSlime(f: Int): List<String> = Painter().run {
        val s = f.toDouble()
        ellipse(16.0, 19.0 + s, 14.0 + s, 11.0 - s, 'P')
        ellipse(15.0, 18.0 + s, 13.0 + s, 10.0 - s, 'p')
        rect(0, 30, 31, 31, '.')
        ellipse(7.0, 12.0 + s, 2.2, 1.5, 'w')
        // Olhos zangados
        ellipse(11.0, 18.0 + s, 2.8, 3.2, 'w')
        ellipse(21.0, 18.0 + s, 2.8, 3.2, 'w')
        ellipse(11.8, 19.0 + s, 1.3, 1.7, 'e')
        ellipse(20.2, 19.0 + s, 1.3, 1.7, 'e')
        for (i in 0..4) {
            dot(8 + i, 14 + s.toInt() + i / 2, 'e')
            dot(23 - i, 14 + s.toInt() + i / 2, 'e')
        }
        rect(13, 24 + s.toInt(), 18, 25 + s.toInt(), 'e')
        dot(14, 24 + s.toInt(), 'w')
        dot(17, 24 + s.toInt(), 'w')
        // Coroa
        val cy = 4 + s.toInt()
        rect(10, cy + 3, 21, cy + 6, 'y')
        rect(10, cy + 6, 21, cy + 6, 'Y')
        for (px in listOf(10.0, 14.5, 19.0)) tri(px, cy + 3.5, px + 1.5, cy - 1.0, px + 3.0, cy + 3.5, 'y')
        dot(15, cy + 4, 'r')
        dot(16, cy + 4, 'r')
        dot(12, cy + 4, '6')
        dot(19, cy + 4, '6')
        outline()
    }

    /** Escorpião Rei: corpo vermelho, tenazes e cauda com ferrão por cima. [f] = tenazes. */
    fun scorpion(f: Int): List<String> = Painter().run {
        // Patas (só a metade esquerda; o espelho faz a outra)
        for ((i, py) in listOf(18, 21, 24).withIndex()) rect(4 - i % 2, py, 9, py, 'q')
        // Braço e tenaz
        ellipse(8.0, 14.0, 3.0, 2.0, 'q')
        val open = if (f == 0) 0.0 else 1.0
        ellipse(4.0, 9.0 - open, 3.2, 2.2, 'R')
        ellipse(4.0, 12.5 + open, 3.2, 1.6, 'R')
        // Corpo e cabeça
        ellipse(16.0, 21.0, 9.0, 7.0, 'q')
        ellipse(15.5, 20.5, 8.0, 6.0, 'R')
        ellipse(16.0, 13.0, 6.0, 4.5, 'q')
        ellipse(15.5, 12.5, 5.0, 3.5, 'R')
        mirror()
        // Cauda a subir por trás, com ferrão amarelo
        for ((cx, cy) in listOf(16.0 to 27.0, 21.0 to 26.0, 25.0 to 21.5, 26.0 to 15.5, 24.0 to 9.5)) {
            ellipse(cx, cy, 2.8, 2.6, 'q')
            ellipse(cx - 0.4, cy - 0.4, 2.0, 1.8, 'R')
        }
        tri(20.0, 6.0, 22.0, 2.0, 25.0, 7.0, 'Q')
        // Olhos e riscas
        rect(12, 11, 13, 12, 'w')
        rect(18, 11, 19, 12, 'w')
        dot(13, 12, 'e')
        dot(18, 12, 'e')
        for (yy in listOf(19, 22, 25)) rect(10, yy, 21, yy, 'q')
        outline()
    }

    /** Yeti: bola de pelo branca com cara azulada. [f] = braços em baixo/no ar. */
    fun yeti(f: Int): List<String> = Painter().run {
        val armY = if (f == 0) 20.0 else 13.0
        ellipse(4.5, armY, 3.5, 6.0, 'J')
        ellipse(4.0, armY - 0.5, 2.8, 5.2, 'I')
        ellipse(16.0, 19.0, 12.0, 11.0, 'J')
        ellipse(15.0, 18.0, 11.0, 10.0, 'I')
        for (px in listOf(9.0, 13.0)) tri(px, 10.0, px + 2.0, 4.0, px + 4.0, 10.0, 'I')
        ellipse(10.0, 29.0, 4.0, 2.2, 'J')
        mirror()
        // Cara
        ellipse(16.0, 15.0, 6.5, 5.0, 'J')
        rect(12, 13, 13, 14, 'e')
        rect(19, 13, 20, 14, 'e')
        dot(12, 13, 'w')
        dot(19, 13, 'w')
        rect(13, 17, 19, 18, 'e')
        dot(14, 17, 'w')
        dot(18, 17, 'w')
        ellipse(10.0, 23.0, 2.0, 1.5, 'J')
        ellipse(22.0, 23.0, 2.0, 1.5, 'J')
        outline()
    }

    /** Feiticeiro: manto roxo, barba, chapéu com estrelas e bastão. [f] = cor da esfera. */
    fun sorcerer(f: Int): List<String> = Painter().run {
        tri(16.0, 10.0, 3.0, 31.0, 29.0, 31.0, 'V')
        tri(15.0, 11.0, 5.0, 30.0, 25.0, 30.0, 'v')
        ellipse(7.0, 21.0, 3.0, 4.5, 'v')
        ellipse(25.0, 21.0, 3.0, 4.5, 'V')
        ellipse(16.0, 12.0, 4.5, 4.0, 's')
        tri(11.5, 13.0, 20.5, 13.0, 16.0, 25.0, 'w')
        tri(16.0, 0.0, 8.0, 9.0, 24.0, 9.0, 'v')
        rect(7, 8, 25, 9, 'V')
        dot(14, 5, 'Q')
        dot(18, 3, 'Q')
        dot(17, 7, 'Q')
        dot(14, 11, 'Q')
        dot(18, 11, 'Q')
        // Bastão com esfera
        rect(28, 7, 28, 31, 'o')
        ellipse(28.5, 5.0, 2.8, 2.8, if (f == 0) '6' else 'X')
        ellipse(28.0, 4.5, 1.2, 1.2, '7')
        ellipse(26.0, 21.0, 1.6, 1.6, 's')
        outline()
    }

    fun frames(kind: BossKind): List<List<String>> = when (kind) {
        BossKind.KING_SLIME -> listOf(kingSlime(0), kingSlime(1))
        BossKind.SCORPION -> listOf(scorpion(0), scorpion(1))
        BossKind.YETI -> listOf(yeti(0), yeti(1))
        BossKind.SORCERER -> listOf(sorcerer(0), sorcerer(1))
    }
}
