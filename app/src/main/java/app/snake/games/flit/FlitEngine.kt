package app.snake.games.flit

enum class FlitPhase { Ready, Running, Paused, Dead }

data class Pipe(val x: Float, val gap: Float)

data class FlitState(
    val y: Float = 0.5f,
    val v: Float = 0f,
    val pipes: List<Pipe> = listOf(Pipe(1.15f, 0.48f), Pipe(1.72f, 0.52f)),
    val score: Int = 0,
    val passed: Int = 0,
    val phase: FlitPhase = FlitPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val BIRD_X = 0.26f
        const val RADIUS = 0.034f
        const val GAP = 0.205f
        const val PIPE_W = 0.10f
        fun fresh() = FlitState()
    }
}

fun FlitState.pauseToggle() = when (phase) {
    FlitPhase.Running -> copy(phase = FlitPhase.Paused)
    FlitPhase.Paused -> copy(phase = FlitPhase.Running)
    else -> this
}

fun FlitState.pauseIfRunning() = if (phase == FlitPhase.Running) copy(phase = FlitPhase.Paused) else this

fun FlitState.flap(): FlitState = when (phase) {
    FlitPhase.Dead -> this
    FlitPhase.Paused -> copy(phase = FlitPhase.Running, v = -0.58f)
    else -> copy(v = -0.58f, phase = FlitPhase.Running)
}

fun FlitState.restart() = FlitState.fresh()

fun FlitState.step(dt: Float): FlitState {
    if (phase != FlitPhase.Running) return this
    val nv = (v + 1.85f * dt).coerceIn(-0.9f, 0.95f)
    val ny = y + nv * dt
    if (ny - FlitState.RADIUS < 0.02f || ny + FlitState.RADIUS > 0.93f) {
        return copy(y = ny.coerceIn(0.04f, 0.93f), v = nv, phase = FlitPhase.Dead)
    }
    var nextScore = score
    var nextPassed = passed
    val speed = (0.28f + score * 0.008f).coerceAtMost(0.42f)
    val moved = pipes.map { p ->
        val nx = p.x - speed * dt
        if (p.x > FlitState.BIRD_X && nx <= FlitState.BIRD_X) {
            nextScore += 1
            nextPassed += 1
        }
        if (nx < -0.18f) Pipe(1.22f, (0.30f..0.70f).random()) else p.copy(x = nx)
    }
    val hit = moved.any { p ->
        val inX = kotlin.math.abs(p.x - FlitState.BIRD_X) < FlitState.PIPE_W / 2f + FlitState.RADIUS
        val inGap = ny > p.gap - FlitState.GAP + 0.018f && ny < p.gap + FlitState.GAP - 0.018f
        inX && !inGap
    }
    if (hit) return copy(y = ny, v = nv, pipes = moved, score = nextScore, passed = nextPassed, phase = FlitPhase.Dead)
    return copy(y = ny, v = nv, pipes = moved, score = nextScore, passed = nextPassed)
}

private fun ClosedFloatingPointRange<Float>.random(): Float =
    start + kotlin.random.Random.nextFloat() * (endInclusive - start)
