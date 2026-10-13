package pt.tiago.kaboom.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
        private const val EPS = 0.001f
    }

    val tiles = Array(H) { Array(W) { Tile.FLOOR } }

    /** > 0: o bloco está a arder e desaparece quando chegar a 0. */
    val burn = Array(H) { FloatArray(W) }

    /** > 0: há fogo nesta casa. [flameH]/[flameV] dizem em que sentido, para o desenho. */
    val flame = Array(H) { FloatArray(W) }
    val flameH = Array(H) { BooleanArray(W) }
    val flameV = Array(H) { BooleanArray(W) }

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
    var lives = START_LIVES; private set
    var score = 0; private set
    var phase = Phase.START; private set
    var phaseTime = 0f; private set
    var time = 0f; private set

    val events = ArrayList<GameEvent>()

    /** Direcção que o jogador está a carregar (null = parado). Escrita pela UI. */
    var input: Dir? = null
    private var bombRequested = false

    val doorOpen get() = enemies.none { it.alive }

    fun newGame() {
        level = 1
        lives = START_LIVES
        score = 0
        player = Player()
        loadLevel(1)
    }

    fun requestBomb() {
        bombRequested = true
    }

    fun update(dt: Float) {
        time += dt
        phaseTime += dt
        when (phase) {
            Phase.START -> {
                bombRequested = false
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
        bombs.clear()
        enemies.clear()
        doorX = -1
        doorY = -1
        doorRevealed = false
        doorOpenedAt = -100f
        bombRequested = false
        // O input não se limpa: é a UI que o controla, e quem segura o joystick
        // durante a mudança de nível deve continuar a andar.
        // Os poderes ganhos mantêm-se; só a posição e o estado de morte voltam ao início.
        player.x = 1.5f
        player.y = 1.5f
        player.facing = Dir.DOWN
        player.dying = -1f
        player.moving = false
    }

    private fun loadLevel(n: Int) {
        level = n
        resetBoard()
        generate(n)
        doorWasOpen = doorOpen
        setPhase(Phase.START)
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
            val kind = when {
                n >= 4 && i % 3 == 2 -> EnemyKind.GHOST
                n >= 2 && i % 2 == 1 -> EnemyKind.EYE
                else -> EnemyKind.BLOB
            }
            enemies += Enemy(kind, spots[i].first, spots[i].second)
        }
    }

    private fun randomItem(): ItemType = when (rng.nextInt(10)) {
        in 0..3 -> ItemType.BOMB
        in 4..7 -> ItemType.FIRE
        else -> ItemType.SPEED
    }

    /**
     * Carrega um mapa desenhado em texto (para testes): '#' parede, 'B' bloco, '.' chão,
     * 'P' jogador, 'E' inimigo, 'D' porta já à vista. O que faltar fica parede.
     */
    fun loadMap(rows: List<String>) {
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
        p.moving = false
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
            }
            score += 50
            events += GameEvent.ITEM
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
        if (bombs.size >= p.maxBombs || bombAt(x, y) != null || tiles[y][x] != Tile.FLOOR) return
        bombs += Bomb(x, y, p.range)
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
            }
            p.x = nx
        } else {
            var ny = p.y + d.dy * dist
            if (!playerFree(ncx, ncy + d.dy)) {
                val limit = ncy + 0.5f
                ny = if (d.dy > 0) min(ny, max(p.y, limit)) else max(ny, min(p.y, limit))
            }
            p.y = ny
        }
    }

    private fun checkPlayerDeath() {
        val p = player
        val hitByEnemy = enemies.any { it.alive && abs(it.x - p.x) < 0.7f && abs(it.y - p.y) < 0.7f }
        if (flame[p.cellY][p.cellX] > 0f || hitByEnemy) {
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
        }
    }

    // ---------------------------------------------------------------- mundo

    private fun updateWorld(dt: Float) {
        for (b in bombs.toList()) {
            if (b.exploded) continue
            b.timer -= dt
            if (b.timer <= 0f) explode(b)
        }

        for (y in 0 until H) for (x in 0 until W) {
            if (flame[y][x] > 0f) {
                flame[y][x] -= dt
                if (flame[y][x] <= 0f) {
                    flame[y][x] = 0f
                    flameH[y][x] = false
                    flameV[y][x] = false
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
            if (e.alive && flame[e.cellY][e.cellX] > 0f) {
                e.dying = 0f
                score += e.kind.points
                events += GameEvent.ENEMY_DIED
            }
        }
        enemies.removeAll { it.dying > 1f }

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
            ignite(b.x, b.y, h = true, v = true)
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
                        }
                        break
                    }
                    val other = bombAt(x, y)
                    if (other != null) {
                        if (!other.exploded) {
                            other.exploded = true
                            queue += other
                        }
                        break
                    }
                    ignite(x, y, h = d.dx != 0, v = d.dy != 0)
                    // O fogo destrói os itens à vista e pára aí.
                    if (items.remove(y * W + x) != null) break
                }
            }
        }
        events += GameEvent.EXPLOSION
    }

    private fun ignite(x: Int, y: Int, h: Boolean, v: Boolean) {
        flame[y][x] = FLAME
        if (h) flameH[y][x] = true
        if (v) flameV[y][x] = true
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

        val dist = e.kind.speed * dt
        val tx = e.toX + 0.5f
        val ty = e.toY + 0.5f
        val dx = tx - e.x
        val dy = ty - e.y
        if (abs(dx) + abs(dy) <= dist) {
            e.x = tx
            e.y = ty
            e.fromX = e.toX
            e.fromY = e.toY
            chooseNext(e)
        } else {
            e.x += if (dx > 0) min(dist, dx) else max(-dist, dx)
            e.y += if (dy > 0) min(dist, dy) else max(-dist, dy)
        }
    }

    private fun chooseNext(e: Enemy) {
        val options = Dir.entries.filter { enemyFree(e, e.fromX + it.dx, e.fromY + it.dy) }
        if (options.isEmpty()) return
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
