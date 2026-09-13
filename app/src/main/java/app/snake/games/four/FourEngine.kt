package app.snake.games.four

enum class FourPhase { Setup, Playing, Over }

data class FourState(
    val cells: IntArray = IntArray(COLS * ROWS),
    val leafTurn: Boolean = true,
    val phase: FourPhase = FourPhase.Setup,
    val leafName: String = "LEAF",
    val headName: String = "HEAD",
    val passPending: Boolean = false,
    val winner: Int = 0,
    val last: Int = -1,
    val winLine: IntArray = intArrayOf(),
    val recorded: Boolean = false,
) {
    companion object {
        const val COLS = 7
        const val ROWS = 6
        fun lobby() = FourState()
        fun start(leaf: String, head: String) = FourState(
            phase = FourPhase.Playing,
            leafName = leaf,
            headName = head,
        )
    }

    fun name(leaf: Boolean) = if (leaf) leafName else headName
    fun currentName() = name(leafTurn)
    override fun equals(other: Any?) = other is FourState && cells.contentEquals(other.cells) &&
        leafTurn == other.leafTurn && phase == other.phase && passPending == other.passPending &&
        winner == other.winner && last == other.last && recorded == other.recorded &&
        leafName == other.leafName && headName == other.headName &&
        winLine.contentEquals(other.winLine)
    override fun hashCode() = cells.contentHashCode()
}

fun FourState.ackPass() = copy(passPending = false)
fun FourState.markRecorded() = copy(recorded = true)

fun FourState.drop(col: Int): FourState {
    if (phase != FourPhase.Playing || passPending) return this
    if (col !in 0 until FourState.COLS) return this
    var row = FourState.ROWS - 1
    while (row >= 0 && cells[row * FourState.COLS + col] != 0) row--
    if (row < 0) return this
    val piece = if (leafTurn) 1 else 2
    val next = cells.copyOf()
    val i = row * FourState.COLS + col
    next[i] = piece
    val line = fourLine(next, col, row, piece)
    val full = next.none { it == 0 }
    return copy(
        cells = next,
        leafTurn = !leafTurn,
        last = i,
        winLine = line,
        phase = if (line.isNotEmpty() || full) FourPhase.Over else FourPhase.Playing,
        winner = when {
            line.isNotEmpty() -> piece
            else -> 0
        },
        passPending = line.isEmpty() && !full,
    )
}

private fun fourLine(board: IntArray, col: Int, row: Int, piece: Int): IntArray {
    val dirs = arrayOf(1 to 0, 0 to 1, 1 to 1, 1 to -1)
    for ((dx, dy) in dirs) {
        val cells = ArrayList<Int>(8)
        cells += row * FourState.COLS + col
        var c = col + dx
        var r = row + dy
        while (c in 0 until FourState.COLS && r in 0 until FourState.ROWS && board[r * FourState.COLS + c] == piece) {
            cells += r * FourState.COLS + c
            c += dx
            r += dy
        }
        c = col - dx
        r = row - dy
        while (c in 0 until FourState.COLS && r in 0 until FourState.ROWS && board[r * FourState.COLS + c] == piece) {
            cells += r * FourState.COLS + c
            c -= dx
            r -= dy
        }
        if (cells.size >= 4) return cells.toIntArray()
    }
    return intArrayOf()
}
