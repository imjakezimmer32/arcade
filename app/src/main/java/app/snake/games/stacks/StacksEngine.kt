package app.snake.games.stacks

enum class StackPhase { Ready, Running, Paused, Dead }

data class Tetromino(
    val id: Int,
    val rotations: List<List<Pair<Int, Int>>>,
)

private fun piece(id: Int, vararg rots: List<Pair<Int, Int>>) = Tetromino(id, rots.toList())

val TETROMINOES = listOf(
    piece(
        1,
        listOf(0 to 1, 1 to 1, 2 to 1, 3 to 1),
        listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3),
        listOf(0 to 2, 1 to 2, 2 to 2, 3 to 2),
        listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3),
    ),
    piece(2, listOf(1 to 0, 2 to 0, 1 to 1, 2 to 1)),
    piece(
        3,
        listOf(1 to 0, 0 to 1, 1 to 1, 2 to 1),
        listOf(1 to 0, 1 to 1, 2 to 1, 1 to 2),
        listOf(0 to 1, 1 to 1, 2 to 1, 1 to 2),
        listOf(1 to 0, 0 to 1, 1 to 1, 1 to 2),
    ),
    piece(
        4,
        listOf(1 to 0, 2 to 0, 0 to 1, 1 to 1),
        listOf(1 to 0, 1 to 1, 2 to 1, 2 to 2),
    ),
    piece(
        5,
        listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1),
        listOf(2 to 0, 1 to 1, 2 to 1, 1 to 2),
    ),
    piece(
        6,
        listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1),
        listOf(1 to 0, 2 to 0, 1 to 1, 1 to 2),
        listOf(0 to 1, 1 to 1, 2 to 1, 2 to 2),
        listOf(1 to 0, 1 to 1, 0 to 2, 1 to 2),
    ),
    piece(
        7,
        listOf(2 to 0, 0 to 1, 1 to 1, 2 to 1),
        listOf(1 to 0, 1 to 1, 1 to 2, 2 to 2),
        listOf(0 to 1, 1 to 1, 2 to 1, 0 to 2),
        listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2),
    ),
)

data class ActivePiece(
    val type: Int,
    val rot: Int,
    val x: Int,
    val y: Int,
)

data class StackState(
    val cells: IntArray = IntArray(WIDTH * HEIGHT),
    val active: ActivePiece? = null,
    val nextType: Int = 1,
    val bag: List<Int> = emptyList(),
    val score: Int = 0,
    val lines: Int = 0,
    val phase: StackPhase = StackPhase.Ready,
    val tickMs: Long = 700L,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val WIDTH = 10
        const val HEIGHT = 20
        fun fresh() = StackState(bag = refillBag() + refillBag()).spawn()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StackState) return false
        return cells.contentEquals(other.cells) &&
            active == other.active &&
            nextType == other.nextType &&
            bag == other.bag &&
            score == other.score &&
            lines == other.lines &&
            phase == other.phase &&
            tickMs == other.tickMs &&
            scoreOffered == other.scoreOffered
    }

    override fun hashCode(): Int {
        var r = cells.contentHashCode()
        r = 31 * r + (active?.hashCode() ?: 0)
        r = 31 * r + nextType
        r = 31 * r + bag.hashCode()
        r = 31 * r + score
        r = 31 * r + lines
        r = 31 * r + phase.hashCode()
        r = 31 * r + tickMs.hashCode()
        r = 31 * r + scoreOffered.hashCode()
        return r
    }
}

private fun refillBag() = (1..7).shuffled()

fun StackState.pauseToggle(): StackState = when (phase) {
    StackPhase.Running -> copy(phase = StackPhase.Paused)
    StackPhase.Paused -> copy(phase = StackPhase.Running)
    else -> this
}

fun StackState.pauseIfRunning(): StackState =
    if (phase == StackPhase.Running) copy(phase = StackPhase.Paused) else this

fun StackState.begin(): StackState = when (phase) {
    StackPhase.Ready -> copy(phase = StackPhase.Running)
    StackPhase.Paused -> copy(phase = StackPhase.Running)
    StackPhase.Dead -> StackState.fresh().copy(phase = StackPhase.Running)
    StackPhase.Running -> this
}

fun StackState.spawn(): StackState {
    var queue = if (bag.size < 8) bag + refillBag() else bag
    val type = queue.first()
    queue = queue.drop(1)
    val piece = ActivePiece(type, 0, 3, 0)
    val coming = queue.first()
    return if (collides(cells, piece)) copy(active = piece, nextType = coming, bag = queue, phase = StackPhase.Dead)
    else copy(active = piece, nextType = coming, bag = queue, phase = if (phase == StackPhase.Dead) StackPhase.Dead else phase)
}

fun StackState.shift(dx: Int): StackState {
    val piece = active ?: return this
    val moved = piece.copy(x = piece.x + dx)
    return if (collides(cells, moved)) this else copy(active = moved, phase = startIfReady())
}

fun StackState.rotate(): StackState {
    val piece = active ?: return this
    val tet = TETROMINOES[piece.type - 1]
    val nextRot = (piece.rot + 1) % tet.rotations.size
    val kicks = listOf(0, -1, 1, -2, 2)
    for (k in kicks) {
        val moved = piece.copy(rot = nextRot, x = piece.x + k)
        if (!collides(cells, moved)) return copy(active = moved, phase = startIfReady())
    }
    return this
}

fun StackState.softDrop(): StackState = gravity(lock = false)

fun StackState.hardDrop(): StackState {
    val piece = active ?: return this
    var y = piece.y
    while (!collides(cells, piece.copy(y = y + 1))) y++
    return copy(active = piece.copy(y = y), phase = startIfReady()).gravity(lock = true)
}

fun StackState.gravity(lock: Boolean = true): StackState {
    if (phase == StackPhase.Dead) return this
    val piece = active ?: return spawn()
    val down = piece.copy(y = piece.y + 1)
    val running = copy(phase = startIfReady())
    if (!collides(cells, down)) return running.copy(active = down)
    if (!lock) return running
    val locked = lockPiece(cells, piece)
    val (cleared, count) = clearLines(locked)
    val add = when (count) {
        1 -> 100
        2 -> 300
        3 -> 500
        4 -> 800
        else -> 0
    }
    val faster = maxOf(120L, 700L - (lines + count) * 18L)
    return running.copy(
        cells = cleared,
        active = null,
        score = score + add + 1,
        lines = lines + count,
        tickMs = faster,
    ).spawn()
}

private fun StackState.startIfReady() = if (phase == StackPhase.Ready) StackPhase.Running else phase

private fun cellsOf(piece: ActivePiece): List<Pair<Int, Int>> {
    val tet = TETROMINOES[piece.type - 1]
    val rot = tet.rotations[piece.rot % tet.rotations.size]
    return rot.map { (x, y) -> piece.x + x to piece.y + y }
}

private fun collides(board: IntArray, piece: ActivePiece): Boolean {
    for ((x, y) in cellsOf(piece)) {
        if (x !in 0 until StackState.WIDTH || y >= StackState.HEIGHT) return true
        if (y < 0) continue
        if (board[y * StackState.WIDTH + x] != 0) return true
    }
    return false
}

private fun lockPiece(board: IntArray, piece: ActivePiece): IntArray {
    val next = board.copyOf()
    for ((x, y) in cellsOf(piece)) {
        if (y in 0 until StackState.HEIGHT && x in 0 until StackState.WIDTH) {
            next[y * StackState.WIDTH + x] = piece.type
        }
    }
    return next
}

private fun clearLines(board: IntArray): Pair<IntArray, Int> {
    val keep = ArrayList<IntArray>(StackState.HEIGHT)
    for (y in 0 until StackState.HEIGHT) {
        val row = IntArray(StackState.WIDTH) { board[y * StackState.WIDTH + it] }
        if (row.any { it == 0 }) keep += row
    }
    val cleared = StackState.HEIGHT - keep.size
    val next = IntArray(StackState.WIDTH * StackState.HEIGHT)
    val start = cleared
    keep.forEachIndexed { i, row ->
        row.copyInto(next, (start + i) * StackState.WIDTH)
    }
    return next to cleared
}

fun ghostY(state: StackState): Int {
    val piece = state.active ?: return 0
    var y = piece.y
    while (!collides(state.cells, piece.copy(y = y + 1))) y++
    return y
}
