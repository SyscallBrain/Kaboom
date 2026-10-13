package pt.tiago.kaboom.game

/** Os quatro bosses, um no fim de cada mundo. */
enum class BossKind(val title: String, val hp: Int, val points: Int) {
    KING_SLIME("REI GOTA", 6, 3000),
    SCORPION("ESCORPIÃO REI", 8, 4000),
    YETI("YETI", 8, 5000),
    SORCERER("FEITICEIRO", 10, 6000),
}

/**
 * O que o boss está a fazer agora.
 * CHARGE: escorpião a preparar a investida (aviso) · DASH: a investir · STUN: tonto depois de bater ·
 * JUMP: yeti no ar · CAST: feiticeiro a lançar a bomba mágica.
 */
enum class BossState { MOVE, CHARGE, DASH, STUN, JUMP, CAST }

/**
 * Boss: ocupa 2×2 casas e move-se livremente (não em carris como os outros inimigos).
 * (x, y) é o centro, em casas; [Game.BOSS_R] é o "raio" da caixa de colisão.
 */
class Boss(val kind: BossKind, var x: Float, var y: Float) {
    var hp = kind.hp
    var vx = 0f
    var vy = 0f
    var state = BossState.MOVE
    var stateTime = 0f

    /** Segundos até ao próximo ataque especial. */
    var cooldown = 3f

    /** > 0: acabou de levar uma explosão e está imune (a piscar). */
    var hurt = 0f

    /** < 0: vivo. >= 0: segundos desde a derrota (sequência de explosões). */
    var dying = -1f
    var defeated = false

    var dashDir = Dir.DOWN
    var jumpFromX = 0f
    var jumpFromY = 0f
    var jumpToX = 0f
    var jumpToY = 0f
    var nextBoom = 0f

    val alive get() = dying < 0f
    val airborne get() = state == BossState.JUMP

    /** Fica mais agressivo à medida que perde vida (1.0 → 1.6). */
    val rage get() = 1f + 0.6f * (1f - hp.toFloat() / kind.hp)
}
