package app.snake.games.rocks

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class RockPhase { Ready, Running, Paused, Dead }

data class Rock(val x: Float, val y: Float, val vx: Float, val vy: Float, val r: Float, val stage: Int)
data class Shot(val x: Float, val y: Float, val vx: Float, val vy: Float, val life: Float)

data class RocksState(
    val x: Float = 0.5f,
    val y: Float = 0.55f,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val ang: Float = -1.57f,
    val thrusting: Boolean = false,
    val aimX: Float = 0.5f,
    val aimY: Float = 0.35f,
    val cooldown: Float = 0f,
    val rocks: List<Rock> = field(1),
    val shots: List<Shot> = emptyList(),
    val lives: Int = 3,
    val score: Int = 0,
    val wave: Int = 1,
    val invuln: Float = 1.2f,
    val phase: RockPhase = RockPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val SHIP_R = 0.042f
        const val SHOT_R = 0.014f
        fun fresh() = RocksState()
    }
}

fun field(wave: Int): List<Rock> = List(3 + wave) {
    val ang = Random.nextFloat() * 6.28f
    Rock(
        x = if (Random.nextBoolean()) 0.08f else 0.92f,
        y = Random.nextFloat(),
        vx = cos(ang) * (0.08f + wave * 0.02f),
        vy = sin(ang) * (0.08f + wave * 0.02f),
        r = 0.08f,
        stage = 3,
    )
}

fun RocksState.pauseToggle() = when (phase) {
    RockPhase.Running -> copy(phase = RockPhase.Paused)
    RockPhase.Paused -> copy(phase = RockPhase.Running)
    else -> this
}

fun RocksState.pauseIfRunning() = if (phase == RockPhase.Running) copy(phase = RockPhase.Paused) else this

fun RocksState.begin() = when (phase) {
    RockPhase.Ready -> copy(phase = RockPhase.Running)
    RockPhase.Paused -> copy(phase = RockPhase.Running)
    RockPhase.Dead -> RocksState.fresh().copy(phase = RockPhase.Running)
    RockPhase.Running -> this
}

fun RocksState.fly(nx: Float, ny: Float, down: Boolean) = copy(
    aimX = nx.coerceIn(0f, 1f),
    aimY = ny.coerceIn(0f, 1f),
    thrusting = down,
    phase = if (phase == RockPhase.Ready && down) RockPhase.Running else phase,
)

fun RocksState.thrust(on: Boolean) = copy(
    thrusting = on,
    phase = if (phase == RockPhase.Ready && on) RockPhase.Running else phase,
)

fun RocksState.steer(delta: Float) = copy(
    ang = ang + delta,
    phase = if (phase == RockPhase.Ready) RockPhase.Running else phase,
)

fun RocksState.fire(): RocksState {
    if (phase == RockPhase.Dead) return this
    val running = if (phase == RockPhase.Ready) copy(phase = RockPhase.Running) else this
    if (running.cooldown > 0f) return running
    val sp = 0.62f
    return running.copy(
        shots = shots + Shot(x, y, cos(ang) * sp, sin(ang) * sp, 0.7f),
        cooldown = 0.22f,
        phase = RockPhase.Running,
    )
}

fun RocksState.step(dt: Float): RocksState {
    if (phase != RockPhase.Running) return this
    val want = kotlin.math.atan2(aimY - y, aimX - x)
    var d = want - ang
    while (d > 3.1416f) d -= 6.2832f
    while (d < -3.1416f) d += 6.2832f
    val turn = (8f * dt).coerceAtMost(kotlin.math.abs(d)) * if (d >= 0f) 1f else -1f
    val nang = if (thrusting) ang + turn else ang
    val acc = if (thrusting) 0.62f else 0f
    var nvx = (vx + cos(nang) * acc * dt) * 0.988f
    var nvy = (vy + sin(nang) * acc * dt) * 0.988f
    val speed = kotlin.math.sqrt(nvx * nvx + nvy * nvy)
    if (speed > 0.85f) {
        nvx = nvx / speed * 0.85f
        nvy = nvy / speed * 0.85f
    }
    var nx = wrap(x + nvx * dt)
    var ny = wrap(y + nvy * dt)
    val shots2 = shots.map { it.copy(x = wrap(it.x + it.vx * dt), y = wrap(it.y + it.vy * dt), life = it.life - dt) }
        .filter { it.life > 0f }
    val rocks2 = rocks.map { r ->
        r.copy(x = wrap(r.x + r.vx * dt), y = wrap(r.y + r.vy * dt))
    }.toMutableList()
    val keptShots = ArrayList<Shot>()
    var nextScore = score
    val spawned = ArrayList<Rock>()
    for (s in shots2) {
        val hit = rocks2.indexOfFirst { dist2(s.x, s.y, it.x, it.y) < (it.r + RocksState.SHOT_R) * (it.r + RocksState.SHOT_R) }
        if (hit < 0) keptShots += s
        else {
            val r = rocks2.removeAt(hit)
            nextScore += when (r.stage) {
                3 -> 20
                2 -> 50
                else -> 100
            }
            if (r.stage > 1) {
                val nr = r.r * 0.62f
                val st = r.stage - 1
                val a = Random.nextFloat() * 6.28f
                spawned += Rock(r.x, r.y, cos(a) * 0.16f, sin(a) * 0.16f, nr, st)
                spawned += Rock(r.x, r.y, -cos(a) * 0.16f, -sin(a) * 0.16f, nr, st)
            }
        }
    }
    rocks2 += spawned
    var lives2 = lives
    var inv = (invuln - dt).coerceAtLeast(0f)
    var dead = false
    if (inv <= 0f) {
        val crash = rocks2.any {
            val body = it.r * 0.88f + RocksState.SHIP_R
            dist2(nx, ny, it.x, it.y) < body * body
        }
        if (crash) {
            lives2 -= 1
            if (lives2 <= 0) dead = true
            else {
                nx = 0.5f
                ny = 0.55f
                nvx = 0f
                nvy = 0f
                inv = 2f
            }
        }
    }
    val waveClear = rocks2.isEmpty()
    val nextRocks = if (waveClear) field(wave + 1) else rocks2
    return copy(
        x = nx, y = ny, vx = nvx, vy = nvy, ang = nang,
        cooldown = (cooldown - dt).coerceAtLeast(0f),
        rocks = nextRocks,
        shots = keptShots,
        lives = lives2.coerceAtLeast(0),
        score = nextScore + if (waveClear) 150 * wave else 0,
        wave = if (waveClear) wave + 1 else wave,
        invuln = if (waveClear) 1.2f else inv,
        phase = if (dead) RockPhase.Dead else phase,
    )
}

private fun wrap(v: Float) = when {
    v < 0f -> v + 1f
    v > 1f -> v - 1f
    else -> v
}

private fun dist2(ax: Float, ay: Float, bx: Float, by: Float): Float {
    var dx = ax - bx
    var dy = ay - by
    if (dx > 0.5f) dx -= 1f
    if (dx < -0.5f) dx += 1f
    if (dy > 0.5f) dy -= 1f
    if (dy < -0.5f) dy += 1f
    return dx * dx + dy * dy
}

fun RocksState.markScoreOffered() = copy(scoreOffered = true)
