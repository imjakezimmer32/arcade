package app.snake.games.pong

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

enum class PongPhase { Setup, Ready, Running, Paused, Dead }

enum class PongEvent { Paddle, Score, Miss, Dead }

enum class PongGate { Pick, Seat }

data class PongState(
    val px: Float = 0.5f,
    val cpu: Float = 0.5f,
    val bx: Float = 0.5f,
    val by: Float = 0.72f,
    val vx: Float = 0.22f,
    val vy: Float = -0.48f,
    val score: Int = 0,
    val lives: Int = 3,
    val serve: Float = 0f,
    val rally: Int = 0,
    val phase: PongPhase = PongPhase.Setup,
    val gate: PongGate = PongGate.Pick,
    val hotSeat: Boolean = false,
    val leafName: String = "LEAF",
    val headName: String = "HEAD",
    val leafPts: Int = 0,
    val headPts: Int = 0,
    val serveDown: Boolean = true,
    val recorded: Boolean = false,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val PAD = 0.28f
        const val PAD_H = 0.028f
        const val LEAF_Y = 0.88f
        const val HEAD_Y = 0.08f
        const val BALL = 0.018f
        const val TO_WIN = 7
        fun lobby() = PongState()
        fun solo() = PongState(phase = PongPhase.Ready, gate = PongGate.Pick)
        fun seat(leaf: String, head: String) = PongState(
            phase = PongPhase.Ready,
            gate = PongGate.Pick,
            hotSeat = true,
            leafName = leaf,
            headName = head,
            bx = 0.5f,
            by = 0.5f,
            serve = 0.7f,
            serveDown = true,
        )
    }
}

fun PongState.pauseToggle() = when (phase) {
    PongPhase.Running -> copy(phase = PongPhase.Paused)
    PongPhase.Paused -> copy(phase = PongPhase.Running)
    else -> this
}

fun PongState.pauseIfRunning() = if (phase == PongPhase.Running) copy(phase = PongPhase.Paused) else this

fun PongState.wantSeat() = copy(gate = PongGate.Seat)

fun PongState.lobby() = PongState.lobby()

fun PongState.begin() = when (phase) {
    PongPhase.Ready -> copy(
        phase = PongPhase.Running,
        serve = if (hotSeat) 0.55f else 0.55f,
        bx = if (hotSeat) 0.5f else px,
        by = if (hotSeat) 0.5f else 0.78f,
        vx = 0.18f,
        vy = if (hotSeat) (if (serveDown) 0.5f else -0.5f) else -0.5f,
    )
    PongPhase.Paused -> copy(phase = PongPhase.Running)
    PongPhase.Dead -> if (hotSeat) PongState.seat(leafName, headName) else PongState.solo()
    PongPhase.Running -> this
    PongPhase.Setup -> this
}

fun PongState.moveLeaf(x: Float): PongState {
    val nx = x.coerceIn(PongState.PAD / 2f, 1f - PongState.PAD / 2f)
    return if (!hotSeat && (phase == PongPhase.Ready || phase == PongPhase.Setup)) {
        copy(px = nx, bx = nx, phase = PongPhase.Running, serve = 0.35f)
    } else if (phase == PongPhase.Ready && hotSeat) {
        copy(px = nx, phase = PongPhase.Running, serve = 0.4f)
    } else {
        copy(px = nx, phase = if (phase == PongPhase.Ready) PongPhase.Running else phase)
    }
}

fun PongState.moveHead(x: Float): PongState {
    if (!hotSeat) return this
    val nx = x.coerceIn(PongState.PAD / 2f, 1f - PongState.PAD / 2f)
    return copy(cpu = nx, phase = if (phase == PongPhase.Ready) PongPhase.Running else phase)
}

fun PongState.markScoreOffered() = copy(scoreOffered = true, recorded = true)

fun PongState.step(dt: Float): Pair<PongState, PongEvent?> {
    if (phase != PongPhase.Running) return this to null
    if (serve > 0f) {
        val left = serve - dt
        return if (hotSeat) {
            copy(serve = left, bx = 0.5f, by = 0.5f) to null
        } else {
            copy(serve = left, bx = px, by = 0.78f) to null
        }
    }
    var x = bx + vx * dt
    var y = by + vy * dt
    var nvx = vx
    var nvy = vy
    val r = PongState.BALL
    if (x < r) {
        x = r
        nvx = abs(nvx)
    }
    if (x > 1f - r) {
        x = 1f - r
        nvx = -abs(nvx)
    }
    val half = PongState.PAD / 2f
    val ncpu = if (hotSeat) {
        cpu
    } else {
        val cpuSpeed = (0.42f + score * 0.012f).coerceAtMost(0.72f)
        val aim = x + (if (nvy < 0) (px - 0.5f) * 0.04f else 0f)
        (cpu + (aim - cpu).coerceIn(-cpuSpeed * dt, cpuSpeed * dt)).coerceIn(half, 1f - half)
    }
    val ballR = PongState.BALL
    val padH = PongState.PAD_H

    if (nvy < 0 && y - ballR <= PongState.HEAD_Y + padH && y + ballR >= PongState.HEAD_Y && x in (ncpu - half - ballR)..(ncpu + half + ballR)) {
        y = PongState.HEAD_Y + padH + ballR
        val hit = ((x - ncpu) / half).coerceIn(-1f, 1f)
        nvx = hit * 0.62f
        nvy = abs(nvy).coerceAtLeast(0.42f) * 1.03f
        val sped = clampVel(nvx, nvy)
        return copy(bx = x, by = y, vx = sped.first, vy = sped.second, cpu = ncpu, rally = rally + 1) to PongEvent.Paddle
    }
    if (nvy > 0 && y + ballR >= PongState.LEAF_Y && y - ballR <= PongState.LEAF_Y + padH && x in (px - half - ballR)..(px + half + ballR)) {
        y = PongState.LEAF_Y - ballR
        val hit = ((x - px) / half).coerceIn(-1f, 1f)
        nvx = hit * 0.72f
        nvy = -abs(nvy).coerceAtLeast(0.42f) * 1.035f
        val sped = clampVel(nvx, nvy)
        return copy(bx = x, by = y, vx = sped.first, vy = sped.second, cpu = ncpu, rally = rally + 1) to PongEvent.Paddle
    }
    if (y < -0.02f) {
        return if (hotSeat) {
            point(leaf = true, ncpu)
        } else {
            copy(
                bx = px, by = 0.78f, vx = 0.2f, vy = -0.52f, cpu = ncpu,
                score = score + 1, serve = 0.6f, rally = 0,
            ) to PongEvent.Score
        }
    }
    if (y > 1.04f) {
        return if (hotSeat) {
            point(leaf = false, ncpu)
        } else {
            val left = lives - 1
            if (left <= 0) {
                copy(by = y, lives = 0, phase = PongPhase.Dead) to PongEvent.Dead
            } else {
                copy(
                    bx = px, by = 0.78f, vx = 0.2f, vy = -0.52f, cpu = ncpu,
                    lives = left, serve = 0.7f, rally = 0,
                ) to PongEvent.Miss
            }
        }
    }
    return copy(bx = x, by = y, vx = nvx, vy = nvy, cpu = ncpu) to null
}

private fun PongState.point(leaf: Boolean, ncpu: Float): Pair<PongState, PongEvent?> {
    val nextLeaf = leafPts + if (leaf) 1 else 0
    val nextHead = headPts + if (leaf) 0 else 1
    val over = nextLeaf >= PongState.TO_WIN || nextHead >= PongState.TO_WIN
    return copy(
        bx = 0.5f,
        by = 0.5f,
        vx = 0.2f,
        vy = if (leaf) -0.52f else 0.52f,
        cpu = ncpu,
        leafPts = nextLeaf,
        headPts = nextHead,
        serve = 0.65f,
        serveDown = !leaf,
        rally = 0,
        phase = if (over) PongPhase.Dead else phase,
        score = if (leaf) nextLeaf else nextHead,
    ) to if (over) PongEvent.Dead else PongEvent.Score
}

private fun clampVel(vx: Float, vy: Float): Pair<Float, Float> {
    var x = vx
    var y = vy
    if (abs(y) < 0.36f) y = 0.36f * sign(y).let { if (it == 0f) -1f else it }
    val speed = sqrt(x * x + y * y)
    val min = 0.48f
    val max = 1.05f
    val target = speed.coerceIn(min, max)
    if (speed > 0.001f) {
        x = x / speed * target
        y = y / speed * target
    }
    return x to y
}
