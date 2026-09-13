package app.snake.games.snake

import app.snake.core.Dir

enum class SnakePhase { Setup, Ready, Running, Paused, Dead, Won }

enum class SnakeEvent { Eat, Die, Win }

enum class SnakeGate { Pick, Seat }

data class SnakeCell(val x: Int, val y: Int) {
    operator fun plus(dir: Dir) = SnakeCell(x + dir.dx, y + dir.dy)
}

data class SnakeState(
    val cols: Int = COLS,
    val rows: Int = ROWS,
    val snake: List<SnakeCell>,
    val dir: Dir,
    val queued: Dir,
    val queued2: Dir? = null,
    val food: SnakeCell,
    val score: Int,
    val phase: SnakePhase,
    val tickMs: Long,
    val scoreOffered: Boolean = false,
    val gate: SnakeGate = SnakeGate.Pick,
    val hotSeat: Boolean = false,
    val leafName: String = "LEAF",
    val headName: String = "HEAD",
    val leafScore: Int = -1,
    val passPending: Boolean = false,
    val recorded: Boolean = false,
) {
    companion object {
        const val COLS = 17
        const val ROWS = 21
        const val START_TICK_MS = 155L
        const val MIN_TICK_MS = 70L

        fun board(
            hotSeat: Boolean = false,
            leafName: String = "LEAF",
            headName: String = "HEAD",
            leafScore: Int = -1,
        ): SnakeState {
            val start = SnakeCell(COLS / 2, ROWS / 2)
            val snake = listOf(
                start,
                SnakeCell(start.x - 1, start.y),
                SnakeCell(start.x - 2, start.y),
            )
            return SnakeState(
                snake = snake,
                dir = Dir.Right,
                queued = Dir.Right,
                food = spawnFood(snake, COLS, ROWS),
                score = 0,
                phase = SnakePhase.Ready,
                tickMs = START_TICK_MS,
                hotSeat = hotSeat,
                leafName = leafName,
                headName = headName,
                leafScore = leafScore,
            )
        }

        fun lobby() = board().copy(phase = SnakePhase.Setup, gate = SnakeGate.Pick)
        fun solo() = board()
        fun seat(leaf: String, head: String) = board(hotSeat = true, leafName = leaf, headName = head)
        fun fresh() = lobby()
    }

    fun currentName() = if (leafScore < 0) leafName else headName
}

fun SnakeState.queueTurn(next: Dir): SnakeState {
    if (phase != SnakePhase.Running && phase != SnakePhase.Ready) return this
    if (passPending) return this
    val against = if (queued != dir) queued else dir
    if (next.isOpposite(against)) return this
    val started = if (phase == SnakePhase.Ready) SnakePhase.Running else phase
    return if (queued == dir) copy(queued = next, phase = started)
    else copy(queued2 = next, phase = started)
}

fun SnakeState.pauseToggle(): SnakeState = when (phase) {
    SnakePhase.Running -> copy(phase = SnakePhase.Paused)
    SnakePhase.Paused -> copy(phase = SnakePhase.Running)
    else -> this
}

fun SnakeState.pauseIfRunning(): SnakeState =
    if (phase == SnakePhase.Running) copy(phase = SnakePhase.Paused) else this

fun SnakeState.begin(): SnakeState = when (phase) {
    SnakePhase.Ready -> if (passPending) this else copy(phase = SnakePhase.Running)
    SnakePhase.Paused -> copy(phase = SnakePhase.Running)
    SnakePhase.Dead, SnakePhase.Won -> if (hotSeat) SnakeState.seat(leafName, headName) else SnakeState.solo()
    SnakePhase.Running, SnakePhase.Setup -> this
}

fun SnakeState.wantSeat() = copy(gate = SnakeGate.Seat)
fun SnakeState.ackPass() = copy(passPending = false)
fun SnakeState.markRecorded() = copy(recorded = true, scoreOffered = true)

data class SnakeStep(val state: SnakeState, val event: SnakeEvent?)

fun SnakeState.step(): SnakeStep {
    if (phase != SnakePhase.Running) return SnakeStep(this, null)

    val facing = if (queued.isOpposite(dir)) dir else queued
    val nextQueued = queued2 ?: facing
    val next = snake.first() + facing
    val outOfBounds = next.x !in 0 until cols || next.y !in 0 until rows
    if (outOfBounds) {
        return SnakeStep(crash(facing).finishLeg(false), SnakeEvent.Die)
    }

    val eating = next == food
    val body = if (eating) snake else snake.dropLast(1)
    if (next in body) {
        return SnakeStep(crash(facing).finishLeg(false), SnakeEvent.Die)
    }

    val grown = listOf(next) + body
    if (eating) {
        val empty = emptyCells(grown, cols, rows)
        if (empty.isEmpty()) {
            return SnakeStep(
                copy(
                    snake = grown,
                    dir = facing,
                    queued = nextQueued,
                    queued2 = null,
                    score = score + 1,
                ).finishLeg(true),
                SnakeEvent.Win,
            )
        }
        val newScore = score + 1
        val faster = maxOf(SnakeState.MIN_TICK_MS, SnakeState.START_TICK_MS - newScore * 4L)
        return SnakeStep(
            copy(
                snake = grown,
                dir = facing,
                queued = nextQueued,
                queued2 = null,
                food = empty.random(),
                score = newScore,
                tickMs = faster,
            ),
            SnakeEvent.Eat,
        )
    }

    return SnakeStep(copy(snake = grown, dir = facing, queued = nextQueued, queued2 = null), null)
}

private fun SnakeState.crash(facing: Dir) = copy(dir = facing, queued = facing, queued2 = null)

private fun SnakeState.finishLeg(won: Boolean): SnakeState {
    if (!hotSeat) return copy(phase = if (won) SnakePhase.Won else SnakePhase.Dead)
    if (leafScore < 0) {
        return SnakeState.board(
            hotSeat = true,
            leafName = leafName,
            headName = headName,
            leafScore = score,
        ).copy(passPending = true, scoreOffered = true)
    }
    return copy(phase = if (won) SnakePhase.Won else SnakePhase.Dead)
}

private fun emptyCells(occupied: List<SnakeCell>, cols: Int, rows: Int): List<SnakeCell> {
    val blocked = occupied.toHashSet()
    val open = ArrayList<SnakeCell>(cols * rows - blocked.size)
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val cell = SnakeCell(x, y)
            if (cell !in blocked) open += cell
        }
    }
    return open
}

private fun spawnFood(snake: List<SnakeCell>, cols: Int, rows: Int): SnakeCell =
    emptyCells(snake, cols, rows).random()
