package app.snake.games.dodge

enum class DodgePhase { Ready, Running, Paused, Dead }

data class Hazard(val x: Float, val y: Float, val w: Float, val vx: Float = 0f)

data class DodgeState(
    val x: Float = 0.5f,
    val hazards: List<Hazard> = emptyList(),
    val spawn: Float = 0f,
    val score: Int = 0,
    val acc: Float = 0f,
    val lives: Int = 3,
    val invuln: Float = 0f,
    val phase: DodgePhase = DodgePhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val SHIP_Y = 0.88f
        const val SHIP_W = 0.10f
        const val SHIP_H = 0.07f
        const val HAZ_H = 0.05f
        fun fresh() = DodgeState()
    }
}

fun DodgeState.pauseToggle() = when (phase) {
    DodgePhase.Running -> copy(phase = DodgePhase.Paused)
    DodgePhase.Paused -> copy(phase = DodgePhase.Running)
    else -> this
}

fun DodgeState.pauseIfRunning() = if (phase == DodgePhase.Running) copy(phase = DodgePhase.Paused) else this

fun DodgeState.begin() = when (phase) {
    DodgePhase.Ready -> copy(phase = DodgePhase.Running)
    DodgePhase.Paused -> copy(phase = DodgePhase.Running)
    DodgePhase.Dead -> DodgeState.fresh().copy(phase = DodgePhase.Running)
    DodgePhase.Running -> this
}

fun DodgeState.move(nx: Float) = copy(
    x = nx.coerceIn(0.08f, 0.92f),
    phase = if (phase == DodgePhase.Ready) DodgePhase.Running else phase,
)

fun DodgeState.step(dt: Float): DodgeState {
    if (phase != DodgePhase.Running) return this
    val fall = (0.38f + score * 0.004f).coerceAtMost(0.85f)
    var nextSpawn = spawn - dt
    var next = hazards.map {
        val nx = (it.x + it.vx * dt).let { v -> if (v < 0.08f || v > 0.92f) it.copy(x = v.coerceIn(0.08f, 0.92f), vx = -it.vx) else it.copy(x = v) }
        nx.copy(y = nx.y + fall * dt)
    }.filter { it.y < 1.12f }
    if (nextSpawn <= 0f) {
        val drift = if (score > 40 && kotlin.random.Random.nextFloat() < 0.35f) {
            (kotlin.random.Random.nextFloat() - 0.5f) * 0.22f
        } else 0f
        next = next + Hazard(
            x = 0.08f + kotlin.random.Random.nextFloat() * 0.84f,
            y = -0.08f,
            w = 0.10f + kotlin.random.Random.nextFloat() * 0.12f,
            vx = drift,
        )
        nextSpawn = (0.46f - score * 0.0035f).coerceAtLeast(0.16f)
    }
    val hit = next.any { h ->
        app.snake.core.boxesOverlap(h.x, h.y, h.w, DodgeState.HAZ_H, x, DodgeState.SHIP_Y, DodgeState.SHIP_W, DodgeState.SHIP_H)
    }
    val nextAcc = acc + dt * 10f
    val add = nextAcc.toInt()
    val inv = (invuln - dt).coerceAtLeast(0f)
    if (hit && inv <= 0f) {
        val left = lives - 1
        return if (left <= 0) {
            copy(hazards = next, spawn = nextSpawn, acc = nextAcc - add, score = score + add, lives = 0, invuln = 0f, phase = DodgePhase.Dead)
        } else {
            copy(
                hazards = next.filter { it.y < 0.62f },
                spawn = 0.35f,
                acc = nextAcc - add,
                score = score + add,
                lives = left,
                invuln = 1.5f,
            )
        }
    }
    return copy(hazards = next, spawn = nextSpawn, acc = nextAcc - add, score = score + add, invuln = inv)
}
