package app.snake.games.catcher

enum class CatchPhase { Ready, Running, Paused, Dead }

data class Berry(val x: Float, val y: Float, val vy: Float, val kind: Int)

data class CatchState(
    val x: Float = 0.5f,
    val berries: List<Berry> = emptyList(),
    val spawn: Float = 0.3f,
    val lives: Int = 3,
    val score: Int = 0,
    val acc: Float = 0f,
    val phase: CatchPhase = CatchPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val BASKET_Y = 0.88f
        const val BASKET_W = 0.32f
        const val BASKET_H = 0.08f
        const val BERRY = 0.055f
        fun fresh() = CatchState()
    }
}

fun CatchState.pauseToggle() = when (phase) {
    CatchPhase.Running -> copy(phase = CatchPhase.Paused)
    CatchPhase.Paused -> copy(phase = CatchPhase.Running)
    else -> this
}

fun CatchState.pauseIfRunning() = if (phase == CatchPhase.Running) copy(phase = CatchPhase.Paused) else this

fun CatchState.begin() = when (phase) {
    CatchPhase.Ready -> copy(phase = CatchPhase.Running)
    CatchPhase.Paused -> copy(phase = CatchPhase.Running)
    CatchPhase.Dead -> CatchState.fresh().copy(phase = CatchPhase.Running)
    CatchPhase.Running -> this
}

fun CatchState.move(nx: Float) = copy(
    x = nx.coerceIn(0.14f, 0.86f),
    phase = if (phase == CatchPhase.Ready) CatchPhase.Running else phase,
)

fun CatchState.step(dt: Float): CatchState {
    if (phase != CatchPhase.Running) return this
    val fall = (0.32f + score * 0.004f).coerceAtMost(0.72f)
    var nextScore = score
    var nextLives = lives
    val moved = ArrayList<Berry>()
    for (b in berries) {
        val ny = b.y + (b.vy + fall) * 0.5f * dt
        val caught = app.snake.core.boxesOverlap(
            b.x, ny, CatchState.BERRY, CatchState.BERRY,
            x, CatchState.BASKET_Y, CatchState.BASKET_W, CatchState.BASKET_H,
        )
        when {
            caught && b.kind == 2 -> nextLives -= 1
            caught -> nextScore += if (b.kind == 0) 10 else 25
            ny > 1.08f -> if (b.kind == 0) nextLives -= 1
            else -> moved += b.copy(y = ny)
        }
    }
    var spawnIn = spawn - dt
    if (spawnIn <= 0f) {
        val roll = kotlin.random.Random.nextFloat()
        val kind = when {
            roll < 0.12f -> 2
            roll < 0.30f -> 1
            else -> 0
        }
        moved += Berry(
            x = 0.12f + kotlin.random.Random.nextFloat() * 0.76f,
            y = -0.06f,
            vy = fall,
            kind = kind,
        )
        spawnIn = (0.55f - score * 0.003f).coerceAtLeast(0.18f)
    }
    if (nextLives <= 0) return copy(berries = moved, spawn = spawnIn, score = nextScore, lives = 0, phase = CatchPhase.Dead)
    return copy(berries = moved, spawn = spawnIn, lives = nextLives, score = nextScore)
}

fun CatchState.markScoreOffered() = copy(scoreOffered = true)
