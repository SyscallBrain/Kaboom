package pt.tiago.kaboom.game

import kotlin.math.floor

enum class Tile { FLOOR, WALL, BLOCK }

enum class Dir(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    val opposite: Dir
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
}

enum class ItemType { BOMB, FIRE, SPEED }

/**
 * [chase]: probabilidade de, num cruzamento, escolher o caminho que o aproxima do jogador.
 * [ghost]: atravessa blocos destrutíveis (mas não paredes nem bombas).
 */
enum class EnemyKind(val speed: Float, val points: Int, val chase: Float, val ghost: Boolean) {
    BLOB(1.4f, 100, 0f, false),
    EYE(2.1f, 200, 0.55f, false),
    GHOST(1.2f, 400, 0.7f, true),
}

enum class Phase { START, PLAYING, DYING, LEVEL_CLEAR, GAME_OVER }

/** Coisas que aconteceram neste frame — a UI transforma-as em som e vibração. */
enum class GameEvent { BOMB_PLACED, EXPLOSION, ITEM, ENEMY_DIED, PLAYER_DIED, DOOR_OPEN, LEVEL_CLEAR, GAME_OVER }

class Bomb(val x: Int, val y: Int, val range: Int) {
    var timer = Game.FUSE

    /** O jogador acabou de a pôr e ainda está em cima dela: pode sair, mas não voltar. */
    var ownerInside = true
    var exploded = false
}

/**
 * Os inimigos andam de centro de casa em centro de casa: [fromX]/[fromY] é a casa de onde
 * partiram, [toX]/[toY] a casa para onde vão. Só decidem nova direcção ao chegar.
 */
class Enemy(val kind: EnemyKind, cellX: Int, cellY: Int) {
    var x = cellX + 0.5f
    var y = cellY + 0.5f
    var fromX = cellX
    var fromY = cellY
    var toX = cellX
    var toY = cellY
    var dir = Dir.LEFT

    /** < 0: vivo. >= 0: segundos desde que morreu (para a animação). */
    var dying = -1f
    val alive get() = dying < 0f
    val cellX get() = floor(x).toInt()
    val cellY get() = floor(y).toInt()
}

/** Posição em unidades de casa: (1.5, 1.5) é o centro da casa (1, 1). */
class Player {
    var x = 1.5f
    var y = 1.5f
    var facing = Dir.DOWN
    var moving = false

    /** Distância total percorrida — só serve para animar as pernas. */
    var walk = 0f
    var dying = -1f
    var maxBombs = 1
    var range = 1
    var speedUps = 0
    val speed get() = 3.2f + 0.55f * speedUps
    val cellX get() = floor(x).toInt()
    val cellY get() = floor(y).toInt()
}
