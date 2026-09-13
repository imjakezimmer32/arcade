package app.snake.games.breakout

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

enum class BreakPhase { Ready, Running, Paused, Dead }

data class Brick(
    val col: Int,
    val row: Int,
    val alive: Boolean = true,
)

data class BreakState(
    val paddleX: Float = 0.5f,
    val ballX: Float = 0.5f,
    val ballY: Float = 0.84f,
    val vx: Float = 0.28f,
    val vy: Float = -0.58f,
    val bricks: List<Brick> = spawnBricks(1),
    val lives: Int = 3,
    val score: Int = 0,
    val wave: Int = 1,
    val stuck: Boolean = true,
    val phase: BreakPhase = BreakPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val COLS = 8
        const val ROWS = 6
        const val PADDLE_W = 0.28f
        const val PADDLE_H = 0.032f
        const val PADDLE_Y = 0.90f
        const val BALL_RX = 0.018f
        const val BALL_RY = 0.012f
        const val BRICK_TOP = 0.06f
        const val BRICK_H = 0.06f
        const val BRICK_BODY = 0.72f
        fun fresh() = BreakState()
    }
}

fun spawnBricks(wave: Int): List<Brick> = buildList {
    val rows = (BreakState.ROWS + (wave - 1) / 2).coerceAtMost(8)
    for (r in 0 until rows) {
        for (c in 0 until BreakState.COLS) add(Brick(c, r))
    }
}

fun BreakState.pauseToggle(): BreakState = when (phase) {
    BreakPhase.Running -> copy(phase = BreakPhase.Paused)
    BreakPhase.Paused -> copy(phase = BreakPhase.Running)
    else -> this
}

fun BreakState.pauseIfRunning(): BreakState =
    if (phase == BreakPhase.Running) copy(phase = BreakPhase.Paused) else this

fun BreakState.begin(): BreakState = when (phase) {
    BreakPhase.Ready -> copy(phase = BreakPhase.Running, stuck = false)
    BreakPhase.Paused -> copy(phase = BreakPhase.Running)
    BreakPhase.Dead -> BreakState.fresh().copy(phase = BreakPhase.Running, stuck = false)
    BreakPhase.Running -> if (stuck) copy(stuck = false) else this
}

fun BreakState.movePaddle(nx: Float): BreakState {
    val x = nx.coerceIn(BreakState.PADDLE_W / 2f, 1f - BreakState.PADDLE_W / 2f)
    return if (stuck || phase == BreakPhase.Ready) {
        copy(paddleX = x, ballX = x, phase = if (phase == BreakPhase.Ready) BreakPhase.Running else phase)
    } else {
        copy(paddleX = x)
    }
}

fun BreakState.step(dt: Float): BreakState {
    if (phase != BreakPhase.Running) return this
    if (stuck) return copy(ballX = paddleX, ballY = 0.84f)
    var x = ballX + vx * dt
    var y = ballY + vy * dt
    var nvx = vx
    var nvy = vy
    val rx = BreakState.BALL_RX
    val ry = BreakState.BALL_RY
    if (x < rx) {
        x = rx
        nvx = abs(nvx)
    }
    if (x > 1f - rx) {
        x = 1f - rx
        nvx = -abs(nvx)
    }
    if (y < ry) {
        y = ry
        nvy = abs(nvy)
    }

    val paddleTop = BreakState.PADDLE_Y
    val half = BreakState.PADDLE_W / 2f
    if (nvy > 0 && y + ry >= paddleTop && y - ry <= paddleTop + BreakState.PADDLE_H && x in (paddleX - half - rx)..(paddleX + half + rx)) {
        y = paddleTop - ry
        val hit = ((x - paddleX) / half).coerceIn(-1f, 1f)
        nvx = hit * 0.78f
        nvy = -abs(nvy).coerceAtLeast(0.48f)
        val clamped = clampBall(nvx, nvy)
        nvx = clamped.first
        nvy = clamped.second
    }

    val rows = bricks.maxOfOrNull { it.row }?.plus(1) ?: BreakState.ROWS
    val bw = 1f / BreakState.COLS
    val bh = BreakState.BRICK_H
    val top = BreakState.BRICK_TOP
    var nextBricks = bricks
    var nextScore = score
    var consumed = false
    nextBricks = bricks.map { brick ->
        if (consumed || !brick.alive) return@map brick
        val left = brick.col * bw + 0.006f
        val right = (brick.col + 1) * bw - 0.006f
        val bt = top + brick.row * bh
        val bb = bt + bh * BreakState.BRICK_BODY
        val hit = x in (left - rx)..(right + rx) && y in (bt - ry)..(bb + ry)
        if (!hit) brick
        else {
            consumed = true
            val cx = (left + right) / 2f
            val cy = (bt + bb) / 2f
            if (abs(x - cx) * (bb - bt) > abs(y - cy) * (right - left)) {
                nvx = if (x < cx) -abs(nvx) else abs(nvx)
            } else {
                nvy = if (y < cy) -abs(nvy) else abs(nvy)
            }
            val sped = clampBall(nvx * 1.03f, nvy * 1.03f)
            nvx = sped.first
            nvy = sped.second
            nextScore += (rows - brick.row) * 10 * wave
            brick.copy(alive = false)
        }
    }

    if (nextBricks.none { it.alive }) {
        val nxt = wave + 1
        return copy(
            paddleX = paddleX,
            ballX = paddleX,
            ballY = 0.84f,
            vx = 0.28f + nxt * 0.02f,
            vy = -0.58f - nxt * 0.02f,
            bricks = spawnBricks(nxt),
            score = nextScore + 150 * wave,
            wave = nxt,
            stuck = true,
            phase = BreakPhase.Running,
        )
    }

    if (y > 1.04f) {
        val left = lives - 1
        return if (left <= 0) {
            copy(ballX = x, ballY = y, bricks = nextBricks, score = nextScore, lives = 0, phase = BreakPhase.Dead)
        } else {
            copy(
                ballX = paddleX,
                ballY = 0.84f,
                vx = 0.32f,
                vy = -0.55f,
                bricks = nextBricks,
                score = nextScore,
                lives = left,
                stuck = true,
                phase = BreakPhase.Running,
            )
        }
    }

    return copy(ballX = x, ballY = y, vx = nvx, vy = nvy, bricks = nextBricks, score = nextScore)
}

private fun clampBall(vx: Float, vy: Float): Pair<Float, Float> {
    var x = vx
    var y = vy
    if (abs(y) < 0.38f) y = 0.38f * (if (y == 0f) -1f else sign(y))
    val speed = sqrt(x * x + y * y)
    val target = speed.coerceIn(0.52f, 1.15f)
    if (speed > 0.001f) {
        x = x / speed * target
        y = y / speed * target
    }
    return x to y
}
