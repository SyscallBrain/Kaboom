package pt.tiago.kaboom.audio

/** Como o baixo toca cada acorde. */
enum class BassStyle { WALK, DRIVE, SLOW }

/**
 * Uma música em formato de "partitura" de texto. Cada compasso tem 16 semicolcheias.
 *
 * - [chords]: um acorde por compasso ("C", "Am", "F#m", "Bb"); "F/G" = meio compasso cada.
 * - [lead]: a melodia, um compasso por entrada: "nota:duração" separados por espaços,
 *   duração em semicolcheias (a soma tem de dar 16); "R" é pausa. Ex.: "G5:2 E5:2 C6:4 R:8".
 * - [drums]: 16 passos por compasso: K bombo · S tarola · h prato · . nada.
 *
 * A música toca duas vezes seguidas: na segunda, a harmonia passa de arpejos
 * a acompanhar a melodia uma oitava abaixo — assim o ciclo não cansa tão depressa.
 */
class SongDef(
    val bpm: Int,
    val chords: List<String>,
    val lead: List<String>,
    val drums: String,
    val bass: BassStyle,
    /** Largura do pulso da melodia (0.125 fininho, 0.25 clássico, 0.5 "redondo"). */
    val duty: Float = 0.25f,
    /** Oitava dos arpejos (4 normal, 5 a cintilar). */
    val arpOctave: Int = 4,
)

enum class Song(val def: SongDef) {
    TITLE(
        SongDef(
            bpm = 140,
            chords = listOf("C", "G", "Am", "F", "C", "G", "F/G", "C"),
            lead = listOf(
                "G5:2 E5:2 G5:2 C6:4 B5:2 A5:2 G5:2",
                "D6:4 B5:2 G5:2 D5:4 G5:4",
                "E6:2 D6:2 C6:2 B5:2 A5:4 E5:4",
                "F5:2 A5:2 C6:2 A5:2 F5:4 R:4",
                "G5:2 E5:2 G5:2 C6:4 E6:2 D6:2 C6:2",
                "B5:2 D6:2 G6:4 F6:2 E6:2 D6:4",
                "C6:2 A5:2 F5:2 A5:2 B5:2 G5:2 D6:4",
                "C6:8 G5:4 R:4",
            ),
            drums = "K.h.S.h.K.h.S.hh",
            bass = BassStyle.DRIVE,
        ),
    ),
    MEADOW(
        SongDef(
            bpm = 132,
            chords = listOf("G", "Em", "C", "D", "G", "Em", "C/D", "G"),
            lead = listOf(
                "D5:2 G5:2 B5:2 D6:2 B5:4 G5:4",
                "E5:2 G5:2 B5:4 A5:2 G5:2 E5:4",
                "C5:2 E5:2 G5:2 C6:2 B5:2 A5:2 G5:4",
                "F#5:4 A5:4 D6:4 C6:2 A5:2",
                "B5:2 B5:2 D6:2 B5:2 G5:4 D5:4",
                "E5:2 F#5:2 G5:2 B5:2 E6:4 D6:4",
                "C6:2 B5:2 A5:2 G5:2 F#5:2 G5:2 A5:4",
                "G5:8 R:2 D5:2 G5:4",
            ),
            drums = "K.h.S.h.K.h.S.h.",
            bass = BassStyle.WALK,
        ),
    ),
    DESERT(
        SongDef(
            bpm = 120,
            chords = listOf("Dm", "Dm", "Gm", "A", "Dm", "Bb", "A", "Dm"),
            lead = listOf(
                "D5:3 E5:1 F5:2 E5:2 D5:4 A4:4",
                "D5:2 F5:2 A5:2 Bb5:2 A5:4 F5:2 E5:2",
                "G5:3 A5:1 Bb5:2 A5:2 G5:4 D5:4",
                "C#5:2 E5:2 G5:2 E5:2 C#5:4 A4:4",
                "A5:2 Bb5:2 A5:2 G5:2 F5:4 E5:2 F5:2",
                "D6:4 C6:2 Bb5:2 A5:4 G5:4",
                "E5:2 F5:2 G5:2 A5:2 Bb5:2 A5:2 G5:2 E5:2",
                "D5:8 R:4 A4:4",
            ),
            drums = "K..hS.h.K.KhS.h.",
            bass = BassStyle.WALK,
            duty = 0.125f,
        ),
    ),
    ICE(
        SongDef(
            bpm = 104,
            chords = listOf("Am", "F", "C", "G", "Am", "F", "E", "Am"),
            lead = listOf(
                "E6:4 C6:4 A5:4 B5:4",
                "C6:6 A5:2 F5:8",
                "G5:4 C6:4 E6:4 D6:4",
                "B5:8 D6:4 G5:4",
                "A5:2 B5:2 C6:4 E6:4 A6:4",
                "G6:4 F6:4 E6:4 C6:4",
                "B5:4 G#5:4 E5:4 B5:4",
                "A5:12 R:4",
            ),
            drums = "K.......S.....h.",
            bass = BassStyle.SLOW,
            duty = 0.5f,
            arpOctave = 5,
        ),
    ),
    CASTLE(
        SongDef(
            bpm = 126,
            chords = listOf("Dm", "C", "Bb", "A", "Dm", "Gm", "A", "Dm"),
            lead = listOf(
                "D5:4 F5:2 A5:2 D6:4 C6:2 A5:2",
                "G5:4 E5:2 C5:2 G5:4 E5:4",
                "F5:4 D5:2 Bb4:2 F5:2 G5:2 A5:4",
                "A5:4 C#6:4 E6:4 C#6:4",
                "D6:2 C6:2 A5:2 F5:2 D5:4 F5:4",
                "G5:2 Bb5:2 D6:4 C6:2 Bb5:2 A5:4",
                "E5:2 G5:2 A5:2 C#6:2 E6:4 C#6:4",
                "D6:8 A5:4 D5:4",
            ),
            drums = "K.hhS.h.K.hhS.SS",
            bass = BassStyle.DRIVE,
        ),
    ),
    BOSS(
        SongDef(
            bpm = 160,
            chords = listOf("Em", "Em", "C", "D", "Em", "Em", "C", "B"),
            lead = listOf(
                "E5:2 E5:2 G5:2 E5:2 B5:4 A5:2 G5:2",
                "F#5:2 G5:2 F#5:2 E5:2 D5:4 E5:4",
                "C6:2 B5:2 A5:2 G5:2 E5:4 G5:4",
                "F#5:2 A5:2 D6:2 C6:2 B5:4 A5:4",
                "E6:2 D6:2 B5:2 G5:2 E5:2 G5:2 B5:4",
                "E6:4 F#6:2 G6:2 F#6:4 E6:4",
                "G6:2 F#6:2 E6:2 C6:2 E6:4 G6:4",
                "F#6:4 D#6:4 B5:4 F#5:4",
            ),
            drums = "K.S.K.S.KKS.K.SS",
            bass = BassStyle.DRIVE,
            duty = 0.125f,
        ),
    ),
    ;

    companion object {
        /** Música de cada mundo (0..3). */
        fun forWorld(world: Int): Song = listOf(MEADOW, DESERT, ICE, CASTLE)[world % 4]
    }
}
