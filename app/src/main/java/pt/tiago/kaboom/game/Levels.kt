package pt.tiago.kaboom.game

/**
 * Nível desenhado à mão. Legenda dos mapas (13 colunas × 15 linhas):
 *
 * - `#` parede · `.` chão · `B` tijolo · `P` início do jogador
 * - `D` porta escondida debaixo de um tijolo
 * - `b` `f` `s` tijolo com poder escondido: bomba, fogo, rapidez
 * - `E` gota · `Y` morcego · `G` fantasma
 */
class LevelDef(val name: String, val rows: List<String>)

object Levels {
    val world1 = listOf(
        LevelDef(
            "PRIMEIROS PASSOS",
            listOf(
                "#############",
                "#P..B...B...#",
                "#.#B#.#.#B#.#",
                "#...B.....B.#",
                "#.#.#B#.#.#.#",
                "#B....f..B..#",
                "#.#.#.#B#.#.#",
                "#..B.....E..#",
                "#.#.#B#.#.#B#",
                "#.b...B.....#",
                "#B#.#.#.#B#.#",
                "#...B...D...#",
                "#.#.#.#B#.#.#",
                "#..E....B...#",
                "#############",
            ),
        ),
        LevelDef(
            "TIJOLOS POR TODO O LADO",
            listOf(
                "#############",
                "#P.BBB.B.BB.#",
                "#.#B#B#B#.#B#",
                "#.B.B.BBB.B.#",
                "#B#B#.#B#B#.#",
                "#..BBf.B..BB#",
                "#B#.#B#.#B#.#",
                "#.B.B.BEB..B#",
                "#B#B#.#B#.#.#",
                "#..BB.bB.BBE#",
                "#B#.#B#.#B#B#",
                "#.BB..BDB.B.#",
                "#B#B#.#B#.#.#",
                "#.B.s.BB..E.#",
                "#############",
            ),
        ),
        LevelDef(
            "OLHO VIVO",
            listOf(
                "#############",
                "#P....B.....#",
                "#.#B#.#.#B#.#",
                "#.B...B...B.#",
                "#B#.#B#B#.#B#",
                "#...B.Y.B...#",
                "#.#B#.#.#B#.#",
                "#B..f.B.b..B#",
                "#.#B#.#.#B#.#",
                "#...B.E.B...#",
                "#B#.#B#B#.#B#",
                "#.B...B...B.#",
                "#.#B#.#.#B#.#",
                "#Y...BDB...E#",
                "#############",
            ),
        ),
        LevelDef(
            "CORREDORES",
            listOf(
                "#############",
                "#P.B....B..E#",
                "#.#########.#",
                "#..B.f..B...#",
                "###.#####.###",
                "#...B...B..Y#",
                "#.###.#.###.#",
                "#..B..D..B..#",
                "#.###.#.###.#",
                "#Y..B...B..E#",
                "###.#####.###",
                "#...B..s.B..#",
                "#.#########.#",
                "#E.B....B..b#",
                "#############",
            ),
        ),
        LevelDef(
            "O JARDIM",
            listOf(
                "#############",
                "#P..B...B...#",
                "#.#.#.#.#.#.#",
                "#..BBBBBBB..#",
                "#.#B.....B#.#",
                "#..B.Y.Y.B..#",
                "#.#B..#..B#.#",
                "#..B..D..B..#",
                "#.#B..#..B#.#",
                "#..B.E.E.B..#",
                "#.#B.....B#.#",
                "#..BBBfBBB..#",
                "#.#.#.#.#.#.#",
                "#E..b...s..E#",
                "#############",
            ),
        ),
        LevelDef(
            "FANTASMAS",
            listOf(
                "#############",
                "#P.BB.B.BB..#",
                "#.#B#B#B#B#.#",
                "#..B.BBB.BBB#",
                "#B#B#.#.#B#B#",
                "#BB.BBGBB.BB#",
                "#.#B#B#B#.#B#",
                "#B.BB.b.BB.B#",
                "#B#.#B#B#B#.#",
                "#.BB.BDBB.BB#",
                "#B#B#.#.#B#B#",
                "#BBGBB.BBBfB#",
                "#.#B#B#B#.#B#",
                "#..BB.sBB.EG#",
                "#############",
            ),
        ),
        LevelDef(
            "LABIRINTO",
            listOf(
                "#############",
                "#P..#.....#Y#",
                "#.#.#.###.#.#",
                "#.#...#...B.#",
                "#.#####.###.#",
                "#...B.#.#...#",
                "###.#.#.#.###",
                "#E..#.B.#..Y#",
                "#.###.#####.#",
                "#.....#..B..#",
                "#.###B#.###.#",
                "#.#.f.#.#D#.#",
                "#.#.###.#.#.#",
                "#E..B..b..s.#",
                "#############",
            ),
        ),
        LevelDef(
            "GRANDE FINAL",
            listOf(
                "#############",
                "#P..BB.BB.Y.#",
                "#.#B#B#.#B#.#",
                "#..B.B.BB.BG#",
                "#B#.#B#B#.#B#",
                "#BB.E.BB.BBB#",
                "#.#B#.#B#B#.#",
                "#Y.BB.B.D.BB#",
                "#B#.#B#B#.#B#",
                "#.BB.BfB.E..#",
                "#B#B#.#B#B#.#",
                "#..BBBB.BB.B#",
                "#.#.#B#B#.#.#",
                "#G.B..b.s.BY#",
                "#############",
            ),
        ),
    )

    val count get() = world1.size

    /** Nível desenhado número [n] (a partir de 1), ou null se já for o modo infinito. */
    fun get(n: Int): LevelDef? = world1.getOrNull(n - 1)

    /**
     * Poderes com que se começa ao escolher um nível no menu: quem salta para o
     * nível 7 não pode começar com 1 bomba de alcance 1.
     */
    fun startBombs(n: Int) = 1 + (n - 1) / 3
    fun startRange(n: Int) = 1 + (n - 1) / 3
}
