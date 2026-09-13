package app.snake.games.flip

enum class FlipPhase { Setup, Playing, Over }

data class FlipState(
    val cells: IntArray = startBoard(),
    val leafTurn: Boolean = true,
    val phase: FlipPhase = FlipPhase.Setup,
    val leafName: String = "LEAF",
    val headName: String = "HEAD",
    val passPending: Boolean = false,
    val recorded: Boolean = false,
    val skipped: Boolean = false,
    val last: Int = -1,
    val flipped: IntArray = intArrayOf(),
) {
    companion object {
        const val N = 8
        fun lobby() = FlipState()
        fun start(leaf: String, head: String) = FlipState(
            phase = FlipPhase.Playing,
            leafName = leaf,
            headName = head,
        )
    }

    fun name(leaf: Boolean) = if (leaf) leafName else headName
    fun currentName() = name(leafTurn)
    fun count(leaf: Boolean) = cells.count { it == if (leaf) 1 else 2 }
    override fun equals(other: Any?) = other is FlipState && cells.contentEquals(other.cells) &&
        leafTurn == other.leafTurn && phase == other.phase && passPending == other.passPending &&
        recorded == other.recorded && skipped == other.skipped && leafName == other.leafName &&
        headName == other.headName && last == other.last && flipped.contentEquals(other.flipped)
    override fun hashCode() = cells.contentHashCode()
}

fun startBoard(): IntArray {
    val b = IntArray(64)
    b[3 * 8 + 3] = 2
    b[3 * 8 + 4] = 1
    b[4 * 8 + 3] = 1
    b[4 * 8 + 4] = 2
    return b
}

fun FlipState.ackPass() = copy(passPending = false)
fun FlipState.markRecorded() = copy(recorded = true)

fun FlipState.legal(leaf: Boolean): List<Int> {
    val side = if (leaf) 1 else 2
    return (0 until 64).filter { cells[it] == 0 && flips(cells, it, side).isNotEmpty() }
}

fun FlipState.play(i: Int): FlipState {
    if (phase != FlipPhase.Playing || passPending) return this
    val side = if (leafTurn) 1 else 2
    val flipped = flips(cells, i, side)
    if (cells[i] != 0 || flipped.isEmpty()) return this
    val next = cells.copyOf()
    next[i] = side
    flipped.forEach { next[it] = side }
    val opp = !leafTurn
    val oppMoves = legalOn(next, opp)
    val selfMoves = legalOn(next, leafTurn)
    val mark = flipped.toIntArray()
    return when {
        oppMoves.isNotEmpty() -> copy(cells = next, leafTurn = opp, passPending = true, skipped = false, last = i, flipped = mark)
        selfMoves.isNotEmpty() -> copy(cells = next, skipped = true, passPending = false, last = i, flipped = mark)
        else -> copy(cells = next, phase = FlipPhase.Over, skipped = false, last = i, flipped = mark)
    }
}

private fun legalOn(board: IntArray, leaf: Boolean): List<Int> {
    val side = if (leaf) 1 else 2
    return (0 until 64).filter { board[it] == 0 && flips(board, it, side).isNotEmpty() }
}

private fun flips(board: IntArray, at: Int, side: Int): List<Int> {
    val opp = if (side == 1) 2 else 1
    val c0 = at % 8
    val r0 = at / 8
    val out = ArrayList<Int>(12)
    val dirs = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
    for ((dx, dy) in dirs) {
        val run = ArrayList<Int>(6)
        var c = c0 + dx
        var r = r0 + dy
        while (c in 0..7 && r in 0..7) {
            val i = r * 8 + c
            val p = board[i]
            if (p == opp) run += i
            else if (p == side && run.isNotEmpty()) {
                out += run
                break
            } else break
            c += dx
            r += dy
        }
    }
    return out
}
