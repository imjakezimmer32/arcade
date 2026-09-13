package app.snake.games.hop

import app.snake.core.Dir

enum class HopPhase { Ready, Running, Paused, Dead }

data class Hopper(val row: Int, val x: Float, val w: Float, val vx: Float, val water: Boolean)

data class HopState(
    val col: Int = 4,
    val row: Int = ROWS - 1,
    val ride: Float = 0.5f,
    val homes: BooleanArray = BooleanArray(5),
    val traffic: List<Hopper> = spawn(1),
    val lives: Int = 3,
    val score: Int = 0,
    val wave: Int = 1,
    val hopLock: Float = 0f,
    val invuln: Float = 0f,
    val phase: HopPhase = HopPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val COLS = 9
        const val ROWS = 10
        const val FROG_W = 0.09f
        fun fresh() = HopState()
    }

    override fun equals(other: Any?) = other is HopState &&
        col == other.col && row == other.row && ride == other.ride &&
        homes.contentEquals(other.homes) && traffic == other.traffic &&
        lives == other.lives && score == other.score && wave == other.wave &&
        hopLock == other.hopLock && invuln == other.invuln &&
        phase == other.phase && scoreOffered == other.scoreOffered
    override fun hashCode() = col * 31 + row
}

fun spawn(wave: Int): List<Hopper> {
    val mul = 0.82f + wave * 0.08f
    fun lane(row: Int, water: Boolean, vx: Float, count: Int, w: Float) = List(count) { i ->
        Hopper(row, x = i * (1f / count) + 0.02f, w = w, vx = vx * mul, water = water)
    }
    return lane(1, true, 0.18f, 3, 0.28f) +
        lane(2, true, -0.22f, 3, 0.32f) +
        lane(3, true, 0.16f, 2, 0.38f) +
        lane(5, false, -0.26f, 3, 0.18f) +
        lane(6, false, 0.30f, 3, 0.16f) +
        lane(7, false, -0.22f, 4, 0.14f) +
        lane(8, false, 0.34f, 3, 0.18f)
}

private val WATER = setOf(1, 2, 3)
private val ROAD = setOf(5, 6, 7, 8)
private val HOME_COLS = intArrayOf(0, 2, 4, 6, 8)

fun HopState.pauseToggle() = when (phase) {
    HopPhase.Running -> copy(phase = HopPhase.Paused)
    HopPhase.Paused -> copy(phase = HopPhase.Running)
    else -> this
}

fun HopState.pauseIfRunning() = if (phase == HopPhase.Running) copy(phase = HopPhase.Paused) else this

fun HopState.begin() = when (phase) {
    HopPhase.Ready -> copy(phase = HopPhase.Running)
    HopPhase.Paused -> copy(phase = HopPhase.Running)
    HopPhase.Dead -> HopState.fresh().copy(phase = HopPhase.Running)
    HopPhase.Running -> this
}

fun HopState.hop(dir: Dir): HopState {
    if (phase == HopPhase.Dead || phase == HopPhase.Paused) return this
    if (hopLock > 0f) return this
    val running = if (phase == HopPhase.Ready) copy(phase = HopPhase.Running) else this
    val nc = (running.col + dir.dx).coerceIn(0, HopState.COLS - 1)
    val nr = (running.row + dir.dy).coerceIn(0, HopState.ROWS - 1)
    val cellMid = (nc + 0.5f) / HopState.COLS
    val rideX = if (nr == running.row) running.ride + dir.dx / HopState.COLS.toFloat() else cellMid
    val landed = running.copy(col = nc, row = nr, ride = rideX.coerceIn(0.05f, 0.95f), hopLock = 0.12f, phase = HopPhase.Running)
    if (nr in WATER) {
        val log = running.traffic.firstOrNull { it.water && it.row == nr && rideOn(it, landed.ride) }
        return if (log == null) landed.hurt()
        else landed.copy(ride = landed.ride.coerceIn(log.x + 0.02f, log.x + log.w - 0.02f))
    }
    if (nr in ROAD && running.invuln <= 0f && running.traffic.any { !it.water && it.row == nr && rideOn(it, landed.ride) }) {
        return landed.hurt()
    }
    return landed
}

fun HopState.step(dt: Float): HopState {
    if (phase != HopPhase.Running) return this
    val moved = traffic.map { h ->
        var x = h.x + h.vx * dt
        if (x > 1.25f) x -= 1.5f
        if (x < -0.45f) x += 1.5f
        h.copy(x = x)
    }
    var x = ride
    var c = col
    if (row in WATER) {
        val log = moved.firstOrNull { it.water && it.row == row && rideOn(it, x) }
        if (log != null) {
            x = (x + log.vx * dt)
            if (x !in 0.04f..0.96f) return copy(traffic = moved, ride = x, col = c).hurt()
            c = (x * HopState.COLS).toInt().coerceIn(0, HopState.COLS - 1)
        }
    }
    var next = copy(traffic = moved, ride = x, col = c, hopLock = (hopLock - dt).coerceAtLeast(0f), invuln = (invuln - dt).coerceAtLeast(0f))
    if (next.row == 0) {
        val hi = HOME_COLS.indexOf(next.col)
        return if (hi >= 0 && !next.homes[hi]) {
            val homes = next.homes.copyOf().also { it[hi] = true }
            val cleared = homes.all { it }
            next.copy(
                homes = if (cleared) BooleanArray(5) else homes,
                col = 4, row = HopState.ROWS - 1, ride = 0.5f,
                score = score + if (cleared) 200 * wave else 100,
                wave = if (cleared) wave + 1 else wave,
                traffic = if (cleared) spawn(wave + 1) else moved,
                invuln = 0.5f,
            )
        } else {
            next.hurt()
        }
    }
    if (next.invuln > 0f) return next
    if (next.row in WATER && moved.none { it.water && it.row == next.row && rideOn(it, next.ride) }) return next.hurt()
    if (next.row in ROAD && moved.any { !it.water && it.row == next.row && rideOn(it, next.ride) }) return next.hurt()
    return next
}

private fun rideOn(h: Hopper, frogX: Float): Boolean {
    val half = HopState.FROG_W / 2f
    fun hit(lx: Float) = frogX + half > lx && frogX - half < lx + h.w
    return hit(h.x) || hit(h.x - 1.5f) || hit(h.x + 1.5f)
}

private fun HopState.hurt(): HopState {
    val left = lives - 1
    return if (left <= 0) copy(lives = 0, phase = HopPhase.Dead)
    else copy(lives = left, col = 4, row = HopState.ROWS - 1, ride = 0.5f, invuln = 1.2f)
}

fun HopState.markScoreOffered() = copy(scoreOffered = true)
