package pt.tiago.kaboom.game

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Toda a lógica do jogo, sem nada de Android: a UI chama [update] uma vez por frame
 * e depois desenha o estado. Por isso pode ser testada em JVM pura.
 */
class Game(private val rng: Random = Random.Default) {

    companion object {
        const val W = 13
        const val H = 15
        const val FUSE = 2.6f
        const val FLAME = 0.6f
        const val BURN = 0.55f
        const val START_TIME = 1.8f
        const val DEATH_TIME = 2.2f
        const val CLEAR_TIME = 2.8f
        const val START_LIVES = 3
        const val MAX_BOMBS = 8
        const val MAX_RANGE = 8
        const val MAX_SPEED = 4
        const val RESPAWN_SHIELD = 2.5f
        const val SHIELD_BREAK_TIME = 2f
        const val SLIDE_SPEED = 7f
        const val HURT_TIME = 0.8f
        const val MAX_LIVES = 9
        const val BOSS_R = 0.85f
        const val BOSS_HURT_TIME = 1.0f
        private const val EPS = 0.001f
    }

    val tiles = Array(H) { Array(W) { Tile.FLOOR } }

    /** > 0: o bloco está a arder e desaparece quando chegar a 0. */
    val burn = Array(H) { FloatArray(W) }

    /** > 0: há fogo nesta casa. [flameH]/[flameV] dizem em que sentido, para o desenho. */
    val flame = Array(H) { FloatArray(W) }
    val flameH = Array(H) { BooleanArray(W) }
    val flameV = Array(H) { BooleanArray(W) }

    /** Fogo só das bombas mágicas do boss (não lhe faz mal a ele). */
    val flameBoss = Array(H) { BooleanArray(W) }

    var boss: Boss? = null; private set
    private var bossLanded = false

    /** Itens escondidos debaixo de blocos e itens já à vista. Chave: y * W + x. */
    val hiddenItems = HashMap<Int, ItemType>()
    val items = HashMap<Int, ItemType>()

    var doorX = -1; private set
    var doorY = -1; private set
    var doorRevealed = false; private set

    /** Instante (em [time]) em que a porta abriu, para a UI mostrar o aviso. */
    var doorOpenedAt = -100f; private set
    private var doorWasOpen = false

    val bombs = ArrayList<Bomb>()
    val enemies = ArrayList<Enemy>()
    var player = Player(); private set
    var level = 1; private set

    /** Nome do nível desenhado à mão, ou null no modo infinito (níveis gerados). */
    var levelName: String? = null; private set
    var lives = START_LIVES; private set
    var score = 0; private set
    var phase = Phase.START; private set
    var phaseTime = 0f; private set
    var time = 0f; private set

    val events = ArrayList<GameEvent>()
    val fx = ArrayList<Fx>()

    /** Direcção que o jogador está a carregar (null = parado). Escrita pela UI. */
    var input: Dir? = null
    private var bombRequested = false
    private var detonateRequested = false

    val doorOpen get() = enemies.none { it.alive } && boss?.defeated != false

    fun newGame(startLevel: Int = 1) {
        lives = START_LIVES
        score = 0
        player = Player().apply {
            maxBombs = Levels.startBombs(startLevel)
            range = Levels.startRange(startLevel)
        }
        loadLevel(startLevel)
    }

    fun requestBomb() {
        bombRequested = true
    }

    /** Detonador remoto: rebenta a bomba mais antiga. */
    fun requestDetonate() {
        detonateRequested = true
    }

    fun update(dt: Float) {
        time += dt
        phaseTime += dt
        when (phase) {
            Phase.START -> {
                bombRequested = false
                detonateRequested = false
                if (phaseTime >= START_TIME) setPhase(Phase.PLAYING)
            }
            Phase.PLAYING -> {
                updatePlayer(dt)
                updateWorld(dt)
                if (phase == Phase.PLAYING) checkPlayerDeath()
            }
            Phase.DYING -> {
                player.dying += dt
                updateWorld(dt)
                if (phaseTime >= DEATH_TIME) afterDeath()
            }
            Phase.LEVEL_CLEAR -> if (phaseTime >= CLEAR_TIME) loadLevel(level + 1)
            Phase.GAME_OVER -> Unit
        }
    }

    // ---------------------------------------------------------------- níveis

    private fun setPhase(p: Phase) {
        phase = p
        phaseTime = 0f
    }

    private fun resetBoard() {
        for (y in 0 until H) for (x in 0 until W) {
            tiles[y][x] = Tile.WALL
            burn[y][x] = 0f
            flame[y][x] = 0f
            flameH[y][x] = false
            flameV[y][x] = false
        }
        hiddenItems.clear()
        items.clear()
        boss = null
        for (row in flameBoss) row.fill(false)
        fx.clear()
        bombs.clear()
        enemies.clear()
        doorX = -1
        doorY = -1
        doorRevealed = false
        doorOpenedAt = -100f
        bombRequested = false
        detonateRequested = false
        // O input não se limpa: é a UI que o controla, e quem segura o joystick
        // durante a mudança de nível deve continuar a andar.
        // Os poderes ganhos mantêm-se; só a posição e o estado de morte voltam ao início.
        player.x = 1.5f
        player.y = 1.5f
        player.facing = Dir.DOWN
        player.dying = -1f
        player.moving = false
        player.invulnerable = 0f
    }

    private fun loadLevel(n: Int) {
        level = n
        resetBoard()
        val def = Levels.get(n)
        levelName = def?.name
        if (def != null) loadDesigned(def) else generate(n)
        doorWasOpen = doorOpen
        setPhase(Phase.START)
    }

    private fun loadDesigned(def: LevelDef) {
        for ((y, row) in def.rows.withIndex()) for ((x, c) in row.withIndex()) {
            tiles[y][x] = when (c) {
                '#' -> Tile.WALL
                'B', 'D', 'b', 'f', 's', 'k', 'r', 'h', 'p', 'l' -> Tile.BLOCK
                else -> Tile.FLOOR
            }
            when (c) {
                'P' -> {
                    player.x = x + 0.5f
                    player.y = y + 0.5f
                }
                'D' -> {
                    doorX = x
                    doorY = y
                }
                'b' -> hiddenItems[y * W + x] = ItemType.BOMB
                'f' -> hiddenItems[y * W + x] = ItemType.FIRE
                's' -> hiddenItems[y * W + x] = ItemType.SPEED
                'k' -> hiddenItems[y * W + x] = ItemType.KICK
                'r' -> hiddenItems[y * W + x] = ItemType.REMOTE
                'h' -> hiddenItems[y * W + x] = ItemType.SHIELD
                'p' -> hiddenItems[y * W + x] = ItemType.PIERCE
                'l' -> hiddenItems[y * W + x] = ItemType.LIFE
                'E' -> enemies += Enemy(EnemyKind.BLOB, x, y)
                'Y' -> enemies += Enemy(EnemyKind.EYE, x, y)
                'G' -> enemies += Enemy(EnemyKind.GHOST, x, y)
                'C' -> enemies += Enemy(EnemyKind.SNAKE, x, y)
                'N' -> enemies += Enemy(EnemyKind.SNOWMAN, x, y)
                'M' -> enemies += Enemy(EnemyKind.WIZARD, x, y)
                'O' -> boss = Boss(requireNotNull(def.boss) { "${def.name}: 'O' sem boss" }, x + 0.5f, y + 0.5f).also { it.cooldown = 2.5f }
            }
        }
        boss?.let { b ->
            if (b.kind == BossKind.KING_SLIME) {
                b.vx = 1f
                b.vy = 1f
            }
        }
    }

    private fun generate(n: Int) {
        for (y in 0 until H) for (x in 0 until W) {
            val border = x == 0 || y == 0 || x == W - 1 || y == H - 1
            tiles[y][x] = if (border || (x % 2 == 0 && y % 2 == 0)) Tile.WALL else Tile.FLOOR
        }
        // As casas junto ao início ficam livres para dar para pôr a primeira bomba e fugir.
        val safe = setOf(1 to 1, 2 to 1, 1 to 2, 3 to 1, 1 to 3)
        val density = min(0.34f + 0.02f * n, 0.5f)
        val blocks = ArrayList<Int>()
        for (y in 0 until H) for (x in 0 until W) {
            if (tiles[y][x] == Tile.FLOOR && (x to y) !in safe && rng.nextFloat() < density) {
                tiles[y][x] = Tile.BLOCK
                blocks += y * W + x
            }
        }
        blocks.shuffle(rng)
        if (blocks.isNotEmpty()) {
            doorX = blocks[0] % W
            doorY = blocks[0] / W
        }
        // Os dois primeiros itens de cada nível são sempre fogo e bomba; o terceiro é sorte.
        val itemTypes = listOf(ItemType.FIRE, ItemType.BOMB, randomItem())
        for ((i, type) in itemTypes.withIndex()) {
            if (i + 1 < blocks.size) hiddenItems[blocks[i + 1]] = type
        }

        val spots = ArrayList<Pair<Int, Int>>()
        for (y in 0 until H) for (x in 0 until W) {
            if (tiles[y][x] == Tile.FLOOR && x + y >= 9) spots += x to y
        }
        spots.shuffle(rng)
        val count = min(2 + n, 9)
        for (i in 0 until min(count, spots.size)) {
            // No modo infinito (n > 32) entram todos os tipos; antes disso, aos poucos.
            val pool = when {
                n > Levels.count -> EnemyKind.entries
                n >= 4 -> listOf(EnemyKind.BLOB, EnemyKind.EYE, EnemyKind.GHOST)
                n >= 2 -> listOf(EnemyKind.BLOB, EnemyKind.EYE)
                else -> listOf(EnemyKind.BLOB)
            }
            val kind = pool[i % pool.size]
            enemies += Enemy(kind, spots[i].first, spots[i].second)
        }
    }

    private fun randomItem(): ItemType = when (rng.nextInt(100)) {
        in 0..24 -> ItemType.BOMB
        in 25..49 -> ItemType.FIRE
        in 50..64 -> ItemType.SPEED
        in 65..76 -> ItemType.KICK
        in 77..84 -> ItemType.SHIELD
        in 85..91 -> ItemType.REMOTE
        in 92..97 -> ItemType.PIERCE
        else -> ItemType.LIFE
    }

    /**
     * Carrega um mapa desenhado em texto (para testes): '#' parede, 'B' bloco, '.' chão,
     * 'P' jogador, 'E' inimigo, 'D' porta já à vista, 'O' boss ([bossKind]). O que faltar fica parede.
     */
    fun loadMap(rows: List<String>, bossKind: BossKind? = null) {
        resetBoard()
        for ((y, row) in rows.withIndex()) for ((x, c) in row.withIndex()) {
            if (x >= W || y >= H) continue
            tiles[y][x] = when (c) {
                '#' -> Tile.WALL
                'B' -> Tile.BLOCK
                else -> Tile.FLOOR
            }
            when (c) {
                'P' -> {
                    player.x = x + 0.5f
                    player.y = y + 0.5f
                }
                'E' -> enemies += Enemy(EnemyKind.BLOB, x, y)
                'O' -> boss = Boss(requireNotNull(bossKind), x + 0.5f, y + 0.5f)
                'D' -> {
                    doorX = x
                    doorY = y
                    doorRevealed = true
                }
            }
        }
        doorWasOpen = doorOpen
        setPhase(Phase.PLAYING)
    }

    // ---------------------------------------------------------------- jogador

    private fun updatePlayer(dt: Float) {
        val p = player
        if (bombRequested) {
            bombRequested = false
            placeBomb()
        }
        if (detonateRequested) {
            detonateRequested = false
            bombs.firstOrNull { it.remote && !it.exploded }?.let { explode(it) }
        }
        p.moving = false
        if (p.invulnerable > 0f) p.invulnerable = max(0f, p.invulnerable - dt)
        if (p.fireproof > 0f) p.fireproof = max(0f, p.fireproof - dt)
        input?.let { d ->
            p.facing = d
            val ox = p.x
            val oy = p.y
            move(d, p.speed * dt)
            val moved = abs(p.x - ox) + abs(p.y - oy)
            if (moved > 0f) {
                p.moving = true
                p.walk += moved
            }
        }
        for (b in bombs) {
            if (b.ownerInside && (abs(p.x - b.x - 0.5f) >= 1f - EPS || abs(p.y - b.y - 0.5f) >= 1f - EPS)) {
                b.ownerInside = false
            }
        }

        items.remove(p.cellY * W + p.cellX)?.let { type ->
            when (type) {
                ItemType.BOMB -> p.maxBombs = min(p.maxBombs + 1, MAX_BOMBS)
                ItemType.FIRE -> p.range = min(p.range + 1, MAX_RANGE)
                ItemType.SPEED -> p.speedUps = min(p.speedUps + 1, MAX_SPEED)
                ItemType.KICK -> p.kick = true
                ItemType.REMOTE -> p.remote = true
                ItemType.SHIELD -> p.shield = true
                ItemType.PIERCE -> p.pierce = true
                ItemType.LIFE -> lives = min(lives + 1, MAX_LIVES)
            }
            score += 50
            events += GameEvent.ITEM
            fx += Fx(FxKind.ITEM, p.x, p.y)
            fx += Fx(FxKind.SCORE, p.x, p.y - 0.3f, 50)
        }

        if (doorRevealed && doorOpen && p.cellX == doorX && p.cellY == doorY &&
            abs(p.x - doorX - 0.5f) < 0.3f && abs(p.y - doorY - 0.5f) < 0.3f
        ) {
            score += 500 + 100 * level
            events += GameEvent.LEVEL_CLEAR
            setPhase(Phase.LEVEL_CLEAR)
        }
    }

    private fun placeBomb() {
        val p = player
        val x = p.cellX
        val y = p.cellY
        if (bombs.count { !it.byBoss } >= p.maxBombs || bombAt(x, y) != null || tiles[y][x] != Tile.FLOOR) return
        bombs += Bomb(x, y, p.range, remote = p.remote, pierce = p.pierce)
        events += GameEvent.BOMB_PLACED
    }

    private fun bombAt(x: Int, y: Int): Bomb? = bombs.firstOrNull { it.x == x && it.y == y }

    private fun playerFree(x: Int, y: Int): Boolean {
        if (x !in 0 until W || y !in 0 until H) return false
        if (tiles[y][x] != Tile.FLOOR) return false
        val b = bombAt(x, y)
        return b == null || b.ownerInside
    }

    /**
     * Movimento "em carris": o jogador anda sempre alinhado com uma linha ou coluna de casas.
     * Se estiver desalinhado e carregar para um lado, primeiro desliza para o carril livre mais
     * próximo — é isto que evita ficar preso nas esquinas com controlos de toque.
     */
    private fun move(d: Dir, distance: Float) {
        val p = player
        var dist = distance
        val horizontal = d.dx != 0
        val cx = p.cellX
        val cy = p.cellY
        val off = if (horizontal) p.y - (cy + 0.5f) else p.x - (cx + 0.5f)

        if (abs(off) > EPS) {
            val laneHere = if (horizontal) cy else cx
            val laneNext = if (off > 0) laneHere + 1 else laneHere - 1
            fun ahead(lane: Int) = if (horizontal) playerFree(cx + d.dx, lane) else playerFree(lane, cy + d.dy)
            fun beside(lane: Int) = if (horizontal) playerFree(cx, lane) else playerFree(lane, cy)
            val lane = when {
                ahead(laneHere) -> laneHere
                beside(laneNext) && ahead(laneNext) -> laneNext
                else -> -1
            }
            if (lane >= 0) {
                val target = lane + 0.5f
                val cur = if (horizontal) p.y else p.x
                val step = min(abs(target - cur), dist)
                val next = if (target > cur) cur + step else cur - step
                dist -= step
                val aligned = abs(target - next) <= EPS
                val final = if (aligned) target else next
                if (horizontal) p.y = final else p.x = final
                if (!aligned) return
            }
        }
        if (dist <= 0f) return

        val ncx = p.cellX
        val ncy = p.cellY
        if (horizontal) {
            var nx = p.x + d.dx * dist
            if (!playerFree(ncx + d.dx, ncy)) {
                val limit = ncx + 0.5f
                nx = if (d.dx > 0) min(nx, max(p.x, limit)) else max(nx, min(p.x, limit))
                if (abs(nx - limit) < 0.05f) tryKick(ncx + d.dx, ncy, d)
            }
            p.x = nx
        } else {
            var ny = p.y + d.dy * dist
            if (!playerFree(ncx, ncy + d.dy)) {
                val limit = ncy + 0.5f
                ny = if (d.dy > 0) min(ny, max(p.y, limit)) else max(ny, min(p.y, limit))
                if (abs(ny - limit) < 0.05f) tryKick(ncx, ncy + d.dy, d)
            }
            p.y = ny
        }
    }

    /** Com o poder de chutar, encostar a uma bomba põe-na a deslizar. */
    private fun tryKick(x: Int, y: Int, d: Dir) {
        if (!player.kick) return
        val b = bombAt(x, y) ?: return
        if (b.ownerInside || b.slide != null || !bombFree(x + d.dx, y + d.dy)) return
        b.slide = d
        b.slideT = 0f
        events += GameEvent.KICK
        fx += Fx(FxKind.KICK, x + 0.5f, y + 0.5f)
    }

    /** Uma bomba a deslizar só entra em chão vazio: sem bombas nem inimigos. */
    private fun bombFree(x: Int, y: Int): Boolean {
        if (x !in 0 until W || y !in 0 until H) return false
        if (tiles[y][x] != Tile.FLOOR || bombAt(x, y) != null) return false
        if (player.cellX == x && player.cellY == y) return false
        return enemies.none { it.alive && ((it.cellX == x && it.cellY == y) || (it.toX == x && it.toY == y)) }
    }

    private fun updateSlide(b: Bomb, dt: Float) {
        val d = b.slide ?: return
        // Alguém se meteu à frente antes de meio caminho: a bomba pára onde está.
        if (b.slideT < 0.5f && !bombFree(b.x + d.dx, b.y + d.dy)) {
            b.slide = null
            b.slideT = 0f
            return
        }
        b.slideT += SLIDE_SPEED * dt
        while (b.slideT >= 1f) {
            b.x += d.dx
            b.y += d.dy
            b.slideT -= 1f
            if (!bombFree(b.x + d.dx, b.y + d.dy)) {
                b.slide = null
                b.slideT = 0f
                return
            }
        }
    }

    private fun checkPlayerDeath() {
        val p = player
        val b = boss
        val touchBoss = b != null && b.alive && !b.airborne && abs(b.x - p.x) < BOSS_R + 0.3f && abs(b.y - p.y) < BOSS_R + 0.3f
        val crushed = bossLanded && b != null && abs(b.x - p.x) < 1.4f && abs(b.y - p.y) < 1.4f
        bossLanded = false
        val hitByEnemy = p.invulnerable <= 0f &&
            (touchBoss || crushed || enemies.any { it.alive && abs(it.x - p.x) < 0.7f && abs(it.y - p.y) < 0.7f })
        val burnt = p.fireproof <= 0f && flame[p.cellY][p.cellX] > 0f
        if ((burnt || hitByEnemy) && p.shield) {
            // O escudo parte e dá uns segundos de protecção total para fugir
            p.shield = false
            p.invulnerable = SHIELD_BREAK_TIME
            p.fireproof = SHIELD_BREAK_TIME
            events += GameEvent.SHIELD_BREAK
            fx += Fx(FxKind.SHIELD_BREAK, p.x, p.y)
            return
        }
        if (burnt || hitByEnemy) {
            p.dying = 0f
            p.moving = false
            events += GameEvent.PLAYER_DIED
            setPhase(Phase.DYING)
        }
    }

    private fun afterDeath() {
        lives--
        if (lives <= 0) {
            events += GameEvent.GAME_OVER
            setPhase(Phase.GAME_OVER)
        } else {
            loadLevel(level)
            player.invulnerable = RESPAWN_SHIELD
        }
    }

    // ---------------------------------------------------------------- mundo

    private fun updateWorld(dt: Float) {
        for (b in bombs.toList()) {
            if (b.exploded) continue
            updateSlide(b, dt)
            if (!b.remote) b.timer -= dt
            if (b.timer <= 0f) explode(b)
        }

        for (y in 0 until H) for (x in 0 until W) {
            if (flame[y][x] > 0f) {
                flame[y][x] -= dt
                if (flame[y][x] <= 0f) {
                    flame[y][x] = 0f
                    flameH[y][x] = false
                    flameV[y][x] = false
                    flameBoss[y][x] = false
                }
            }
            if (burn[y][x] > 0f) {
                burn[y][x] -= dt
                if (burn[y][x] <= 0f) {
                    burn[y][x] = 0f
                    tiles[y][x] = Tile.FLOOR
                    if (x == doorX && y == doorY) {
                        doorRevealed = true
                    } else {
                        hiddenItems.remove(y * W + x)?.let { items[y * W + x] = it }
                    }
                }
            }
        }

        for (e in enemies) {
            updateEnemy(e, dt)
            if (e.alive && e.hurt <= 0f && flame[e.cellY][e.cellX] > 0f) {
                e.hp--
                if (e.hp > 0) {
                    e.hurt = HURT_TIME
                    events += GameEvent.ENEMY_HURT
                    fx += Fx(FxKind.HURT, e.x, e.y)
                } else {
                    e.dying = 0f
                    score += e.kind.points
                    events += GameEvent.ENEMY_DIED
                    fx += Fx(FxKind.SCORE, e.x, e.y - 0.3f, e.kind.points)
                }
            }
        }
        enemies.removeAll { it.dying > 1f }
        updateBoss(dt)

        val open = doorOpen
        if (open && !doorWasOpen) {
            doorOpenedAt = time
            events += GameEvent.DOOR_OPEN
        }
        doorWasOpen = open
    }

    /** Explode a bomba e, em cadeia, todas as que o fogo apanhar — tudo no mesmo frame. */
    private fun explode(first: Bomb) {
        val queue = ArrayDeque<Bomb>()
        first.exploded = true
        queue += first
        while (queue.isNotEmpty()) {
            val b = queue.removeFirst()
            bombs.remove(b)
            ignite(b.x, b.y, h = true, v = true, byBoss = b.byBoss)
            fx += Fx(FxKind.EXPLOSION, b.x + 0.5f, b.y + 0.5f)
            for (d in Dir.entries) {
                for (i in 1..b.range) {
                    val x = b.x + d.dx * i
                    val y = b.y + d.dy * i
                    if (x !in 0 until W || y !in 0 until H) break
                    if (tiles[y][x] == Tile.WALL) break
                    if (tiles[y][x] == Tile.BLOCK) {
                        if (burn[y][x] <= 0f) {
                            burn[y][x] = BURN
                            score += 10
                            fx += Fx(FxKind.BLOCK_BROKEN, x + 0.5f, y + 0.5f)
                        }
                        // Fogo perfurante continua pelos tijolos fora
                        if (!b.pierce) break
                        continue
                    }
                    val other = bombAt(x, y)
                    if (other != null) {
                        if (!other.exploded) {
                            other.exploded = true
                            queue += other
                        }
                        break
                    }
                    ignite(x, y, h = d.dx != 0, v = d.dy != 0, byBoss = b.byBoss)
                    // O fogo destrói os itens à vista e pára aí.
                    if (items.remove(y * W + x) != null) break
                }
            }
        }
        events += GameEvent.EXPLOSION
    }

    private fun ignite(x: Int, y: Int, h: Boolean, v: Boolean, byBoss: Boolean = false) {
        // Fogo do jogador por cima de fogo mágico passa a ferir o boss
        flameBoss[y][x] = if (byBoss) flame[y][x] <= 0f || flameBoss[y][x] else false
        flame[y][x] = FLAME
        if (h) flameH[y][x] = true
        if (v) flameV[y][x] = true
    }

    // ---------------------------------------------------------------- bosses

    /** A caixa do boss centrada em (x, y) bate nalguma parede ou tijolo? */
    private fun bossBlocked(x: Float, y: Float): Boolean {
        val r = BOSS_R
        for (cy in floor(y - r).toInt()..floor(y + r - 0.001f).toInt()) {
            for (cx in floor(x - r).toInt()..floor(x + r - 0.001f).toInt()) {
                if (cx !in 0 until W || cy !in 0 until H || tiles[cy][cx] != Tile.FLOOR) return true
            }
        }
        return false
    }

    /** Move o boss; devolve (bateu na horizontal, bateu na vertical). */
    private fun bossMove(b: Boss, dx: Float, dy: Float): Pair<Boolean, Boolean> {
        val hitX = bossBlocked(b.x + dx, b.y)
        if (!hitX) b.x += dx
        val hitY = bossBlocked(b.x, b.y + dy)
        if (!hitY) b.y += dy
        return hitX to hitY
    }

    private fun setBossState(b: Boss, s: BossState) {
        b.state = s
        b.stateTime = 0f
    }

    private fun updateBoss(dt: Float) {
        val b = boss ?: return
        if (!b.alive) {
            updateBossDeath(b, dt)
            return
        }
        b.stateTime += dt
        b.cooldown -= dt
        if (b.hurt > 0f) b.hurt = max(0f, b.hurt - dt)

        when (b.kind) {
            BossKind.KING_SLIME -> updateKingSlime(b, dt)
            BossKind.SCORPION -> updateScorpion(b, dt)
            BossKind.YETI -> updateYeti(b, dt)
            BossKind.SORCERER -> updateSorcerer(b, dt)
        }

        if (!b.airborne && b.hurt <= 0f && bossInFire(b)) {
            b.hp--
            b.hurt = BOSS_HURT_TIME
            fx += Fx(FxKind.BOSS_HIT, b.x, b.y)
            if (b.hp > 0) {
                events += GameEvent.BOSS_HURT
            } else {
                b.dying = 0f
                score += b.kind.points
                events += GameEvent.BOSS_DEFEATED
                fx += Fx(FxKind.SCORE, b.x, b.y - 1f, b.kind.points)
                // Os lacaios caem com o chefe
                for (e in enemies) if (e.alive) e.dying = 0f
            }
        }
    }

    private fun bossInFire(b: Boss): Boolean {
        val r = BOSS_R
        for (cy in floor(b.y - r).toInt()..floor(b.y + r - 0.001f).toInt()) {
            for (cx in floor(b.x - r).toInt()..floor(b.x + r - 0.001f).toInt()) {
                if (cx in 0 until W && cy in 0 until H && flame[cy][cx] > 0f && !flameBoss[cy][cx]) return true
            }
        }
        return false
    }

    /** Explosões em série durante 1,6 s, e depois aparece a porta onde o boss caiu. */
    private fun updateBossDeath(b: Boss, dt: Float) {
        if (b.defeated) return
        b.dying += dt
        b.nextBoom -= dt
        if (b.nextBoom <= 0f) {
            b.nextBoom = 0.22f
            fx += Fx(FxKind.EXPLOSION, b.x + (rng.nextFloat() - 0.5f) * 1.6f, b.y + (rng.nextFloat() - 0.5f) * 1.6f)
            events += GameEvent.EXPLOSION
        }
        if (b.dying >= 1.6f) {
            b.defeated = true
            val dx = floor(b.x).toInt()
            val dy = floor(b.y).toInt()
            tiles[dy][dx] = Tile.FLOOR
            doorX = dx
            doorY = dy
            doorRevealed = true
        }
    }

    /** Rei Gota: salta em diagonal pela arena e larga gotas pequenas. */
    private fun updateKingSlime(b: Boss, dt: Float) {
        val speed = 1.7f * b.rage
        val len = sqrt(b.vx * b.vx + b.vy * b.vy).coerceAtLeast(0.001f)
        val (hitX, hitY) = bossMove(b, b.vx / len * speed * dt, b.vy / len * speed * dt)
        if (hitX) b.vx = -b.vx
        if (hitY) b.vy = -b.vy
        if (b.cooldown <= 0f) {
            b.cooldown = 6f / b.rage
            if (enemies.count { it.alive } < 4) {
                var spawned = false
                for ((ox, oy) in listOf(-2 to 0, 2 to 0, 0 to 2, 0 to -2)) {
                    val cx = floor(b.x).toInt() + ox
                    val cy = floor(b.y).toInt() + oy
                    val far = abs(cx - player.cellX) + abs(cy - player.cellY) >= 3
                    if (cx in 0 until W && cy in 0 until H && tiles[cy][cx] == Tile.FLOOR && bombAt(cx, cy) == null && far) {
                        enemies += Enemy(EnemyKind.BLOB, cx, cy)
                        fx += Fx(FxKind.TELEPORT, cx + 0.5f, cy + 0.5f)
                        spawned = true
                        if (enemies.count { it.alive } >= 4) break
                    }
                }
                if (spawned) events += GameEvent.BOSS_ATTACK
            }
        }
    }

    /** Escorpião Rei: passeia; se ficares alinhado com ele, prepara-se e investe até bater. */
    private fun updateScorpion(b: Boss, dt: Float) {
        val p = player
        when (b.state) {
            BossState.MOVE -> {
                if (b.vx == 0f && b.vy == 0f || b.stateTime > 1.6f) {
                    val d = Dir.entries[rng.nextInt(4)]
                    b.vx = d.dx.toFloat()
                    b.vy = d.dy.toFloat()
                    b.stateTime = 0f
                }
                val (hitX, hitY) = bossMove(b, b.vx * 1.6f * dt, b.vy * 1.6f * dt)
                if (hitX || hitY) {
                    b.vx = 0f
                    b.vy = 0f
                }
                if (b.cooldown <= 0f) {
                    val dir = when {
                        abs(p.y - b.y) < 0.7f -> if (p.x > b.x) Dir.RIGHT else Dir.LEFT
                        abs(p.x - b.x) < 0.7f -> if (p.y > b.y) Dir.DOWN else Dir.UP
                        else -> null
                    }
                    if (dir != null) {
                        b.dashDir = dir
                        setBossState(b, BossState.CHARGE)
                        events += GameEvent.BOSS_ATTACK
                    }
                }
            }
            BossState.CHARGE -> if (b.stateTime >= 0.6f / b.rage) setBossState(b, BossState.DASH)
            BossState.DASH -> {
                val step = 7.5f * dt
                val (hitX, hitY) = bossMove(b, b.dashDir.dx * step, b.dashDir.dy * step)
                if (hitX || hitY) {
                    setBossState(b, BossState.STUN)
                    events += GameEvent.BOSS_LAND
                    fx += Fx(FxKind.BOSS_LAND, b.x, b.y)
                }
            }
            BossState.STUN -> if (b.stateTime >= 1.5f) {
                setBossState(b, BossState.MOVE)
                b.cooldown = 2.5f / b.rage
            }
            else -> setBossState(b, BossState.MOVE)
        }
    }

    /** Yeti: anda atrás de ti e, de vez em quando, salta para onde estás (a sombra avisa). */
    private fun updateYeti(b: Boss, dt: Float) {
        val p = player
        when (b.state) {
            BossState.JUMP -> {
                val k = (b.stateTime / 1.1f).coerceAtMost(1f)
                b.x = b.jumpFromX + (b.jumpToX - b.jumpFromX) * k
                b.y = b.jumpFromY + (b.jumpToY - b.jumpFromY) * k
                if (k >= 1f) {
                    setBossState(b, BossState.STUN)
                    bossLanded = true
                    events += GameEvent.BOSS_LAND
                    fx += Fx(FxKind.BOSS_LAND, b.x, b.y)
                }
            }
            BossState.STUN -> if (b.stateTime >= 0.9f) {
                setBossState(b, BossState.MOVE)
                b.cooldown = 4.5f / b.rage
            }
            else -> {
                val dx = p.x - b.x
                val dy = p.y - b.y
                val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
                bossMove(b, dx / len * 1.1f * dt, dy / len * 1.1f * dt)
                if (b.cooldown <= 0f) {
                    b.jumpFromX = b.x
                    b.jumpFromY = b.y
                    // Aterra onde o jogador está agora, se lá couber
                    val tx = p.x.coerceIn(1f + BOSS_R, W - 1f - BOSS_R)
                    val ty = p.y.coerceIn(1f + BOSS_R, H - 1f - BOSS_R)
                    val ok = !bossBlocked(tx, ty)
                    b.jumpToX = if (ok) tx else b.x
                    b.jumpToY = if (ok) ty else b.y
                    setBossState(b, BossState.JUMP)
                    events += GameEvent.BOSS_ATTACK
                }
            }
        }
    }

    /** Feiticeiro: teletransporta-se para longe e lança uma bomba mágica aos teus pés. */
    private fun updateSorcerer(b: Boss, dt: Float) {
        val p = player
        when (b.state) {
            BossState.CAST -> if (b.stateTime >= 0.5f) {
                val x = p.cellX
                val y = p.cellY
                if (tiles[y][x] == Tile.FLOOR && bombAt(x, y) == null) {
                    bombs += Bomb(x, y, 3, byBoss = true).apply {
                        ownerInside = true
                        timer = 1.7f
                    }
                    events += GameEvent.BOMB_PLACED
                }
                setBossState(b, BossState.MOVE)
                b.cooldown = 3.4f / b.rage
            }
            else -> {
                if (b.vx == 0f && b.vy == 0f) {
                    b.vx = if (rng.nextBoolean()) 1f else -1f
                    b.vy = if (rng.nextBoolean()) 1f else -1f
                }
                val (hitX, hitY) = bossMove(b, b.vx * 0.9f * dt, b.vy * 0.9f * dt)
                if (hitX) b.vx = -b.vx
                if (hitY) b.vy = -b.vy
                if (b.cooldown <= 0f) {
                    val spots = ArrayList<Pair<Float, Float>>()
                    for (y in 1 until H - 1) for (x in 1 until W - 1) {
                        val cx = x + 0.5f
                        val cy = y + 0.5f
                        if (abs(cx - p.x) + abs(cy - p.y) >= 5f && !bossBlocked(cx, cy)) spots += cx to cy
                    }
                    if (spots.isNotEmpty()) {
                        fx += Fx(FxKind.TELEPORT, b.x, b.y)
                        val (nx, ny) = spots[rng.nextInt(spots.size)]
                        b.x = nx
                        b.y = ny
                        fx += Fx(FxKind.TELEPORT, b.x, b.y)
                        events += GameEvent.TELEPORT
                    }
                    setBossState(b, BossState.CAST)
                    events += GameEvent.BOSS_ATTACK
                }
            }
        }
    }

    // ---------------------------------------------------------------- inimigos

    private fun enemyFree(e: Enemy, x: Int, y: Int): Boolean {
        if (x !in 0 until W || y !in 0 until H) return false
        val t = tiles[y][x]
        if (t == Tile.WALL) return false
        if (t == Tile.BLOCK && (!e.kind.ghost || burn[y][x] > 0f)) return false
        return bombAt(x, y) == null
    }

    private fun updateEnemy(e: Enemy, dt: Float) {
        if (!e.alive) {
            e.dying += dt
            return
        }
        if (e.hurt > 0f) e.hurt = max(0f, e.hurt - dt)
        if (e.kind.teleport) e.teleportIn -= dt
        val travelling = e.toX != e.fromX || e.toY != e.fromY
        if (travelling && !enemyFree(e, e.toX, e.toY)) {
            // Apareceu uma bomba no caminho: dá meia volta.
            val fx = e.fromX
            val fy = e.fromY
            e.fromX = e.toX
            e.fromY = e.toY
            e.toX = fx
            e.toY = fy
            e.dir = e.dir.opposite
        }

        val dist = e.speed * dt
        val tx = e.toX + 0.5f
        val ty = e.toY + 0.5f
        val dx = tx - e.x
        val dy = ty - e.y
        if (abs(dx) + abs(dy) <= dist) {
            e.x = tx
            e.y = ty
            e.fromX = e.toX
            e.fromY = e.toY
            if (e.kind.teleport && e.teleportIn <= 0f) teleport(e)
            chooseNext(e)
        } else {
            e.x += if (dx > 0) min(dist, dx) else max(-dist, dx)
            e.y += if (dy > 0) min(dist, dy) else max(-dist, dy)
        }
    }

    /** O mago desaparece e reaparece numa casa livre, nunca encostado ao jogador. */
    private fun teleport(e: Enemy) {
        e.teleportIn = 3f + rng.nextFloat() * 3f
        val spots = ArrayList<Pair<Int, Int>>()
        for (y in 0 until H) for (x in 0 until W) {
            val far = abs(x - player.cellX) + abs(y - player.cellY) >= 4
            if (far && tiles[y][x] == Tile.FLOOR && flame[y][x] <= 0f && bombAt(x, y) == null) spots += x to y
        }
        if (spots.isEmpty()) return
        fx += Fx(FxKind.TELEPORT, e.x, e.y)
        val (x, y) = spots[rng.nextInt(spots.size)]
        e.x = x + 0.5f
        e.y = y + 0.5f
        e.fromX = x
        e.fromY = y
        e.toX = x
        e.toY = y
        fx += Fx(FxKind.TELEPORT, e.x, e.y)
        events += GameEvent.TELEPORT
    }

    private fun chooseNext(e: Enemy) {
        val options = Dir.entries.filter { enemyFree(e, e.fromX + it.dx, e.fromY + it.dy) }
        if (options.isEmpty()) return
        if (e.kind.straight && e.dir in options) {
            // A cobra segue sempre em frente até bater
            e.toX = e.fromX + e.dir.dx
            e.toY = e.fromY + e.dir.dy
            return
        }
        // Evita andar para trás e para a frente, a não ser que seja um beco.
        val forward = if (options.size > 1) options.filter { it != e.dir.opposite } else options
        val p = player
        val pick = when {
            rng.nextFloat() < e.kind.chase -> forward.minBy { d ->
                abs(e.fromX + d.dx + 0.5f - p.x) + abs(e.fromY + d.dy + 0.5f - p.y) + rng.nextFloat() * 0.5f
            }
            e.dir in forward && rng.nextFloat() < 0.75f -> e.dir
            else -> forward.random(rng)
        }
        e.dir = pick
        e.toX = e.fromX + pick.dx
        e.toY = e.fromY + pick.dy
    }
}
