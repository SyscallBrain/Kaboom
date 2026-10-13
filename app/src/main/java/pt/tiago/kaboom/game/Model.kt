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

/**
 * Poderes. Os três primeiros acumulam; os outros são "ligados" uma vez.
 * KICK: chutar bombas · REMOTE: detonador · SHIELD: aguenta um golpe ·
 * PIERCE: o fogo atravessa tijolos · LIFE: vida extra.
 */
enum class ItemType { BOMB, FIRE, SPEED, KICK, REMOTE, SHIELD, PIERCE, LIFE }

/**
 * [chase]: probabilidade de, num cruzamento, escolher o caminho que o aproxima do jogador.
 * [ghost]: atravessa blocos destrutíveis (mas não paredes nem bombas).
 * [hp]: explosões precisas para morrer. [straight]: só vira quando bate.
 * [teleport]: de vez em quando desaparece e aparece noutro sítio.
 */
enum class EnemyKind(
    val speed: Float,
    val points: Int,
    val chase: Float,
    val ghost: Boolean,
    val hp: Int = 1,
    val straight: Boolean = false,
    val teleport: Boolean = false,
) {
    BLOB(1.4f, 100, 0f, false),
    EYE(2.1f, 200, 0.55f, false),
    GHOST(1.2f, 400, 0.7f, true),
    SNAKE(2.6f, 300, 0f, false, straight = true),
    SNOWMAN(1.0f, 500, 0.3f, false, hp = 2),
    WIZARD(1.5f, 600, 0.5f, false, teleport = true),
}

enum class Phase { START, PLAYING, DYING, LEVEL_CLEAR, GAME_OVER }

/** Efeitos visuais com posição (em unidades de casa) — partículas, pontos a flutuar, tremor. */
enum class FxKind { EXPLOSION, BLOCK_BROKEN, ITEM, SCORE, TELEPORT, SHIELD_BREAK, HURT, KICK, BOSS_LAND, BOSS_HIT }

class Fx(val kind: FxKind, val x: Float, val y: Float, val value: Int = 0)

/** Coisas que aconteceram neste frame — a UI transforma-as em som e vibração. */
enum class GameEvent {
    BOMB_PLACED, EXPLOSION, ITEM, ENEMY_DIED, PLAYER_DIED, DOOR_OPEN, LEVEL_CLEAR, GAME_OVER,
    KICK, SHIELD_BREAK, ENEMY_HURT, TELEPORT,
    BOSS_HURT, BOSS_DEFEATED, BOSS_ATTACK, BOSS_LAND,
}

/**
 * [remote]: não rebenta sozinha, só com o detonador (ou apanhada por outra explosão).
 * [pierce]: o fogo atravessa tijolos. [slide]: a deslizar depois de chutada;
 * [slideT] (0..1) é quanto já andou da casa (x,y) para a seguinte.
 */
class Bomb(
    var x: Int,
    var y: Int,
    val range: Int,
    val remote: Boolean = false,
    val pierce: Boolean = false,
    /** Bomba mágica do feiticeiro: não conta para as bombas do jogador e não fere o boss. */
    val byBoss: Boolean = false,
) {
    var timer = Game.FUSE
    var slide: Dir? = null
    var slideT = 0f

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
    var hp = kind.hp

    /** > 0: acabou de levar uma explosão e está imune (a piscar). */
    var hurt = 0f

    /** Segundos até ao próximo teletransporte (só magos). */
    var teleportIn = 3f

    /** Quem já levou uma explosão (boneco de neve) fica mais pequeno e mais rápido. */
    val speed get() = if (hp < kind.hp) kind.speed * 1.7f else kind.speed
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

    /** > 0: acabou de renascer e os inimigos não lhe tocam (segundos que faltam). */
    var invulnerable = 0f

    /** > 0: o escudo acabou de partir e nem o fogo lhe faz mal (segundos que faltam). */
    var fireproof = 0f
    var kick = false
    var remote = false
    var shield = false
    var pierce = false
    var maxBombs = 1
    var range = 1
    var speedUps = 0
    val speed get() = 3.2f + 0.55f * speedUps
    val cellX get() = floor(x).toInt()
    val cellY get() = floor(y).toInt()
}
