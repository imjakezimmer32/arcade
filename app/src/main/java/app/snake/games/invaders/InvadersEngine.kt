package app.snake.games.invaders

enum class InvPhase { Ready, Running, Paused, Dead }

enum class InvEvent { Fire, Hit, Boom, Life, Wave, Dead }

data class Alien(
    val col: Int,
    val row: Int,
    val kind: Int,
    val alive: Boolean = true,
)

data class Shot(val x: Float, val y: Float, val up: Boolean)

data class Ufo(val x: Float, val vx: Float, val prize: Int)

data class Bunker(val x: Float, val cells: BooleanArray) {
    override fun equals(other: Any?) = other is Bunker && x == other.x && cells.contentEquals(other.cells)
    override fun hashCode() = 31 * x.hashCode() + cells.contentHashCode()
}

data class InvState(
    val ship: Float = 0.5f,
    val aliens: List<Alien> = swarm(),
    val shots: List<Shot> = emptyList(),
    val bunkers: List<Bunker> = bunkers(),
    val ufo: Ufo? = null,
    val dir: Float = 1f,
    val originX: Float = 0.08f,
    val originY: Float = 0.10f,
    val march: Float = 0f,
    val shootIn: Float = 0.9f,
    val ufoIn: Float = 12f,
    val cooldown: Float = 0f,
    val holding: Boolean = false,
    val score: Int = 0,
    val lives: Int = 3,
    val wave: Int = 1,
    val frame: Int = 0,
    val invuln: Float = 0f,
    val phase: InvPhase = InvPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val COLS = 9
        const val ROWS = 5
        const val CELL_W = 0.092f
        const val CELL_H = 0.062f
        const val SHIP_Y = 0.90f
        const val ALIEN_RX = 0.034f
        const val ALIEN_RY = 0.024f
        const val SHIP_W = 0.10f
        const val SHIP_H = 0.055f
        const val SHOT_W = 0.012f
        const val SHOT_H = 0.028f
        const val UFO_W = 0.10f
        fun fresh() = InvState()
    }
}

fun swarm() = buildList {
    for (row in 0 until InvState.ROWS) {
        val kind = when (row) {
            0 -> 2
            1, 2 -> 1
            else -> 0
        }
        for (col in 0 until InvState.COLS) add(Alien(col, row, kind))
    }
}

fun bunkers(): List<Bunker> = listOf(0.18f, 0.40f, 0.60f, 0.82f).map { x ->
    Bunker(x, BooleanArray(4 * 3) { true })
}

fun InvState.pauseToggle() = when (phase) {
    InvPhase.Running -> copy(phase = InvPhase.Paused)
    InvPhase.Paused -> copy(phase = InvPhase.Running)
    else -> this
}

fun InvState.pauseIfRunning() = if (phase == InvPhase.Running) copy(phase = InvPhase.Paused) else this

fun InvState.begin() = when (phase) {
    InvPhase.Ready -> copy(phase = InvPhase.Running)
    InvPhase.Paused -> copy(phase = InvPhase.Running)
    InvPhase.Dead -> InvState.fresh()
    InvPhase.Running -> this
}

fun InvState.aim(x: Float) = copy(
    ship = x.coerceIn(0.07f, 0.93f),
    phase = if (phase == InvPhase.Ready) InvPhase.Running else phase,
)

fun InvState.hold(on: Boolean) = copy(holding = on)

fun InvState.fire(): Pair<InvState, InvEvent?> {
    if (phase == InvPhase.Dead) return this to null
    val running = if (phase == InvPhase.Ready || phase == InvPhase.Paused) copy(phase = InvPhase.Running) else this
    if (running.cooldown > 0f) return running to null
    if (running.shots.any { it.up }) return running to null
    return running.copy(
        shots = running.shots + Shot(running.ship, InvState.SHIP_Y - 0.04f, true),
        cooldown = 0.22f,
        phase = InvPhase.Running,
    ) to InvEvent.Fire
}

fun InvState.step(dt: Float): Pair<InvState, InvEvent?> {
    if (phase != InvPhase.Running) return this to null
    var event: InvEvent? = null
    var next = copy(
        cooldown = (cooldown - dt).coerceAtLeast(0f),
        invuln = (invuln - dt).coerceAtLeast(0f),
        ufoIn = ufoIn - dt,
    )
    if (next.holding && next.cooldown <= 0f && next.shots.none { it.up }) {
        val fired = next.fire()
        next = fired.first
        event = fired.second
    }

    val living = next.aliens.filter { it.alive }
    if (living.isEmpty()) {
        return next.advanceWave() to InvEvent.Wave
    }

    val marched = next.marchAliens(dt)
    next = marched.first
    if (marched.second != null) event = marched.second

    next = next.moveShots(dt)
    val hits = next.collide()
    next = hits.first
    if (hits.second != null) event = hits.second

    next = next.maybeUfo(dt)
    next = next.maybeAlienShot(dt)

    val grounded = next.aliens.any { it.alive && next.originY + it.row * InvState.CELL_H > 0.78f }
    if (grounded) return next.copy(phase = InvPhase.Dead, lives = 0) to InvEvent.Dead

    if (next.lives <= 0) return next.copy(phase = InvPhase.Dead) to InvEvent.Dead
    return next to event
}

private fun InvState.advanceWave(): InvState {
    val bonus = 50 * wave
    return copy(
        aliens = swarm(),
        shots = emptyList(),
        bunkers = bunkers(),
        ufo = null,
        dir = 1f,
        originX = 0.08f,
        originY = (0.10f + wave * 0.02f).coerceAtMost(0.22f),
        march = 0f,
        shootIn = (0.85f - wave * 0.06f).coerceAtLeast(0.28f),
        ufoIn = 10f,
        cooldown = 0.4f,
        score = score + bonus,
        wave = wave + 1,
        frame = 0,
        invuln = 1.2f,
    )
}

private fun InvState.marchAliens(dt: Float): Pair<InvState, InvEvent?> {
    val living = aliens.filter { it.alive }
    val left = living.minOf { originX + it.col * InvState.CELL_W }
    val right = living.maxOf { originX + it.col * InvState.CELL_W }
    val remain = living.size / aliens.size.toFloat()
    val interval = (0.12f + remain * 0.42f) / (1f + (wave - 1) * 0.12f)
    var t = march + dt
    var ox = originX
    var oy = originY
    var nd = dir
    var fr = frame
    if (t >= interval) {
        t = 0f
        fr++
        val step = 0.014f
        ox += nd * step
        val nextLeft = left + nd * step
        val nextRight = right + nd * step
        if (nextRight > 0.94f || nextLeft < 0.04f) {
            nd = -nd
            oy += 0.026f
            ox = originX
        }
    }
    return copy(originX = ox, originY = oy, dir = nd, march = t, frame = fr) to null
}

private fun InvState.moveShots(dt: Float): InvState {
    val moved = shots.map { shot ->
        shot.copy(y = shot.y + if (shot.up) -0.95f * dt else 0.42f * dt)
    }.filter { it.y in -0.08f..1.08f }
    val ufoMoved = ufo?.let { it.copy(x = it.x + it.vx * dt) }?.takeIf { it.x in -0.12f..1.12f }
    return copy(shots = moved, ufo = ufoMoved)
}

private fun InvState.collide(): Pair<InvState, InvEvent?> {
    var nextAliens = aliens
    var nextShots = shots.toMutableList()
    var nextBunkers = bunkers
    var nextUfo = ufo
    var nextScore = score
    var nextLives = lives
    var nextInvuln = invuln
    var event: InvEvent? = null

    fun bunkerHit(x: Float, y: Float): Boolean {
        if (y !in 0.70f..0.82f) return false
        var hit = false
        nextBunkers = nextBunkers.map { bunker ->
            val localX = x - bunker.x + 0.055f
            val localY = y - 0.70f
            val c = (localX / 0.028f).toInt()
            val r = (localY / 0.04f).toInt()
            if (c !in 0..3 || r !in 0..2) return@map bunker
            val i = r * 4 + c
            if (!bunker.cells[i]) return@map bunker
            hit = true
            Bunker(bunker.x, bunker.cells.copyOf().also { it[i] = false })
        }
        return hit
    }

    nextShots = nextShots.filter { shot ->
        if (bunkerHit(shot.x, shot.y)) return@filter false
        if (shot.up) {
            nextUfo?.let { saucer ->
                if (kotlin.math.abs(shot.x - saucer.x) < InvState.UFO_W / 2f && shot.y < 0.08f) {
                    nextScore += saucer.prize
                    nextUfo = null
                    event = InvEvent.Hit
                    return@filter false
                }
            }
            val hit = nextAliens.indexOfFirst { a ->
                if (!a.alive) return@indexOfFirst false
                val ax = originX + a.col * InvState.CELL_W
                val ay = originY + a.row * InvState.CELL_H
                app.snake.core.boxesOverlap(
                    ax, ay, InvState.ALIEN_RX * 2f, InvState.ALIEN_RY * 2f,
                    shot.x, shot.y, InvState.SHOT_W, InvState.SHOT_H,
                )
            }
            if (hit >= 0) {
                val alien = nextAliens[hit]
                nextAliens = nextAliens.toMutableList().also { it[hit] = alien.copy(alive = false) }
                nextScore += when (alien.kind) {
                    2 -> 30
                    1 -> 20
                    else -> 10
                }
                event = InvEvent.Hit
                false
            } else true
        } else {
            if (invuln <= 0f &&
                app.snake.core.boxesOverlap(
                    shot.x, shot.y, InvState.SHOT_W, InvState.SHOT_H,
                    ship, InvState.SHIP_Y, InvState.SHIP_W, InvState.SHIP_H,
                )
            ) {
                nextLives -= 1
                nextInvuln = 2f
                event = if (nextLives <= 0) InvEvent.Dead else InvEvent.Life
                false
            } else true
        }
    }.toMutableList()

    if (event == InvEvent.Life || event == InvEvent.Dead) {
        nextShots = nextShots.filter { it.up }.toMutableList()
    }

    return copy(
        aliens = nextAliens,
        shots = nextShots,
        bunkers = nextBunkers,
        ufo = nextUfo,
        score = nextScore,
        lives = nextLives.coerceAtLeast(0),
        invuln = nextInvuln,
        phase = if (nextLives <= 0) InvPhase.Dead else phase,
    ) to event
}

private fun InvState.maybeUfo(dt: Float): InvState {
    if (ufo != null) return this
    if (ufoIn > 0f) return this
    val goingRight = wave % 2 == 0
    return copy(
        ufo = Ufo(if (goingRight) -0.08f else 1.08f, if (goingRight) 0.22f else -0.22f, listOf(50, 100, 150, 300).random()),
        ufoIn = 16f + wave * 2f,
    )
}

private fun InvState.maybeAlienShot(dt: Float): InvState {
    val nextIn = shootIn - dt
    if (nextIn > 0f) return copy(shootIn = nextIn)
    if (shots.count { !it.up } >= 3) return copy(shootIn = 0.2f)
    val living = aliens.filter { it.alive }
    if (living.isEmpty()) return copy(shootIn = 0.4f)
    val bottoms = living.groupBy { it.col }.values.map { col -> col.maxBy { it.row } }
    val shooter = bottoms.random()
    val x = originX + shooter.col * InvState.CELL_W
    val y = originY + shooter.row * InvState.CELL_H + 0.03f
    val wait = (0.72f - wave * 0.05f).coerceAtLeast(0.26f)
    return copy(shots = shots + Shot(x, y, false), shootIn = wait)
}
