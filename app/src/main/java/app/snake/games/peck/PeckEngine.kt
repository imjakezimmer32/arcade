package app.snake.games.peck

enum class PeckPhase { Ready, Running, Paused, Dead }

data class Hole(val up: Float = 0f, val hit: Float = 0f)

data class PeckState(
    val holes: List<Hole> = List(9) { Hole() },
    val spawn: Float = 0.4f,
    val lives: Int = 3,
    val score: Int = 0,
    val combo: Int = 0,
    val phase: PeckPhase = PeckPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        fun fresh() = PeckState()
    }
}

fun PeckState.pauseToggle() = when (phase) {
    PeckPhase.Running -> copy(phase = PeckPhase.Paused)
    PeckPhase.Paused -> copy(phase = PeckPhase.Running)
    else -> this
}

fun PeckState.pauseIfRunning() = if (phase == PeckPhase.Running) copy(phase = PeckPhase.Paused) else this

fun PeckState.begin() = when (phase) {
    PeckPhase.Ready -> copy(phase = PeckPhase.Running)
    PeckPhase.Paused -> copy(phase = PeckPhase.Running)
    PeckPhase.Dead -> PeckState.fresh().copy(phase = PeckPhase.Running)
    PeckPhase.Running -> this
}

fun PeckState.peck(i: Int): PeckState {
    if (phase == PeckPhase.Dead || i !in holes.indices) return this
    val running = if (phase == PeckPhase.Ready) copy(phase = PeckPhase.Running) else this
    val hole = running.holes[i]
    return if (hole.up > 0f) {
        val next = running.holes.toMutableList()
        next[i] = Hole(up = 0f, hit = 0.22f)
        running.copy(holes = next, score = score + 10 + combo * 2, combo = combo + 1)
    } else {
        running.copy(combo = 0)
    }
}

fun PeckState.step(dt: Float): PeckState {
    if (phase != PeckPhase.Running) return this
    var nextLives = lives
    val next = holes.map { h ->
        val hit = (h.hit - dt).coerceAtLeast(0f)
        if (h.up > 0f) {
            val left = h.up - dt
            if (left <= 0f) {
                nextLives -= 1
                Hole(up = 0f, hit = 0f)
            } else Hole(up = left, hit = hit)
        } else Hole(up = 0f, hit = hit)
    }
    if (nextLives <= 0) return copy(holes = next, lives = 0, phase = PeckPhase.Dead)
    var spawnIn = spawn - dt
    var holes2 = next
    if (spawnIn <= 0f && next.count { it.up > 0f } < 3) {
        val open = next.indices.filter { next[it].up <= 0f && next[it].hit <= 0f }
        if (open.isNotEmpty()) {
            val i = open.random()
            holes2 = next.toMutableList().also {
                it[i] = Hole(up = (0.72f - score * 0.002f).coerceAtLeast(0.38f))
            }
        }
        spawnIn = (0.55f - score * 0.0025f).coerceAtLeast(0.18f)
    }
    return copy(holes = holes2, spawn = spawnIn, lives = nextLives)
}

fun PeckState.markScoreOffered() = copy(scoreOffered = true)
