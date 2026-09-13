package app.snake.games.checkers

data class Man(val leaf: Boolean, val king: Boolean)

data class CkMove(val from: Int, val to: Int, val over: Int = -1)

enum class CkPhase { Setup, Playing, Over }

data class CkSnap(
    val board: List<Man?>,
    val leafTurn: Boolean,
)

data class CkState(
    val board: List<Man?>,
    val leafTurn: Boolean,
    val selected: Int? = null,
    val targets: List<CkMove> = emptyList(),
    val chain: Int? = null,
    val last: CkMove? = null,
    val phase: CkPhase = CkPhase.Setup,
    val leafName: String = "LEAF",
    val headName: String = "HEAD",
    val history: List<CkSnap> = emptyList(),
    val passPending: Boolean = false,
    val winnerLeaf: Boolean? = null,
    val recorded: Boolean = false,
) {
    fun name(leaf: Boolean) = if (leaf) leafName else headName

    fun currentName() = name(leafTurn)

    fun pieceScore(leaf: Boolean): Int = board.fold(0) { acc, m ->
        acc + when {
            m == null || m.leaf != leaf -> 0
            m.king -> 2
            else -> 1
        }
    }

    fun mustCapture() = chain != null || allJumps().isNotEmpty()

    companion object {
        fun lobby() = start("LEAF", "HEAD").copy(phase = CkPhase.Setup)

        fun start(leafName: String, headName: String): CkState {
            val board = MutableList<Man?>(64) { null }
            for (y in 0..2) for (x in 0..7) {
                if (playable(x, y)) board[sq(x, y)] = Man(leaf = true, king = false)
            }
            for (y in 5..7) for (x in 0..7) {
                if (playable(x, y)) board[sq(x, y)] = Man(leaf = false, king = false)
            }
            return CkState(
                board = board,
                leafTurn = true,
                phase = CkPhase.Playing,
                leafName = leafName,
                headName = headName,
            )
        }
    }
}

fun playable(x: Int, y: Int) = (x + y) % 2 == 0
fun sq(x: Int, y: Int) = y * 8 + x
fun xOf(i: Int) = i % 8
fun yOf(i: Int) = i / 8

fun CkState.sit(leaf: String, head: String) = CkState.start(leaf, head)

fun CkState.ackPass() = copy(passPending = false)

fun CkState.markRecorded() = copy(recorded = true)

fun CkState.undo(): CkState {
    val snap = history.lastOrNull() ?: return this
    return copy(
        board = snap.board,
        leafTurn = snap.leafTurn,
        selected = null,
        targets = emptyList(),
        chain = null,
        last = null,
        phase = CkPhase.Playing,
        history = history.dropLast(1),
        passPending = false,
        winnerLeaf = null,
        recorded = false,
    )
}

fun CkState.tap(square: Int): CkState {
    if (phase != CkPhase.Playing || passPending) return this
    if (chain != null) {
        val hit = targets.firstOrNull { it.to == square } ?: return this
        return play(hit)
    }
    val hit = targets.firstOrNull { it.to == square }
    if (selected != null && hit != null) return play(hit)
    val man = board.getOrNull(square) ?: return copy(selected = null, targets = emptyList())
    if (man.leaf != leafTurn) return copy(selected = null, targets = emptyList())
    val jumps = allJumps()
    val moves = if (jumps.isNotEmpty()) {
        val mine = jumps.filter { it.from == square }
        if (mine.isEmpty()) return copy(selected = null, targets = emptyList())
        mine
    } else {
        quietFrom(square)
    }
    return copy(selected = square, targets = moves)
}

private fun CkState.play(move: CkMove): CkState {
    val snap = CkSnap(board, leafTurn)
    val next = board.toMutableList()
    var man = next[move.from] ?: return this
    next[move.from] = null
    if (move.over >= 0) next[move.over] = null
    val y = yOf(move.to)
    val crowned = !man.king && ((man.leaf && y == 7) || (!man.leaf && y == 0))
    if (crowned) man = man.copy(king = true)
    next[move.to] = man
    val more = if (!crowned && move.over >= 0) {
        jumpsFrom(next, move.to, man)
    } else {
        emptyList()
    }
    if (more.isNotEmpty()) {
        return copy(
            board = next,
            selected = move.to,
            targets = more,
            chain = move.to,
            last = move,
            history = history + snap,
        )
    }
    val nextTurn = !leafTurn
    val opponentMoves = legalAny(next, nextTurn)
    val opponentLeft = next.any { it != null && it.leaf == nextTurn }
    val over = !opponentLeft || opponentMoves.isEmpty()
    return copy(
        board = next,
        leafTurn = nextTurn,
        selected = null,
        targets = emptyList(),
        chain = null,
        last = move,
        phase = if (over) CkPhase.Over else CkPhase.Playing,
        history = history + snap,
        passPending = !over,
        winnerLeaf = if (over) leafTurn else null,
    )
}

private fun CkState.allJumps(): List<CkMove> {
    val out = ArrayList<CkMove>()
    for (i in board.indices) {
        val man = board[i] ?: continue
        if (man.leaf != leafTurn) continue
        out += jumpsFrom(board, i, man)
    }
    return out
}

private fun CkState.quietFrom(from: Int): List<CkMove> {
    val man = board[from] ?: return emptyList()
    val out = ArrayList<CkMove>(4)
    for (d in dirs(man)) {
        val x = xOf(from) + d[0]
        val y = yOf(from) + d[1]
        if (x !in 0..7 || y !in 0..7) continue
        val to = sq(x, y)
        if (board[to] == null) out += CkMove(from, to)
    }
    return out
}

private fun jumpsFrom(board: List<Man?>, from: Int, man: Man): List<CkMove> {
    val out = ArrayList<CkMove>(4)
    for (d in dirs(man)) {
        val mx = xOf(from) + d[0]
        val my = yOf(from) + d[1]
        val tx = xOf(from) + d[0] * 2
        val ty = yOf(from) + d[1] * 2
        if (tx !in 0..7 || ty !in 0..7) continue
        val mid = sq(mx, my)
        val to = sq(tx, ty)
        val jumped = board[mid] ?: continue
        if (jumped.leaf == man.leaf) continue
        if (board[to] != null) continue
        out += CkMove(from, to, over = mid)
    }
    return out
}

private fun legalAny(board: List<Man?>, leafTurn: Boolean): List<CkMove> {
    val jumps = ArrayList<CkMove>()
    val quiet = ArrayList<CkMove>()
    for (i in board.indices) {
        val man = board[i] ?: continue
        if (man.leaf != leafTurn) continue
        jumps += jumpsFrom(board, i, man)
        for (d in dirs(man)) {
            val x = xOf(i) + d[0]
            val y = yOf(i) + d[1]
            if (x !in 0..7 || y !in 0..7) continue
            val to = sq(x, y)
            if (board[to] == null) quiet += CkMove(i, to)
        }
    }
    return if (jumps.isNotEmpty()) jumps else quiet
}

private fun dirs(man: Man): Array<IntArray> {
    if (man.king) {
        return arrayOf(intArrayOf(1, 1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(-1, -1))
    }
    return if (man.leaf) {
        arrayOf(intArrayOf(1, 1), intArrayOf(-1, 1))
    } else {
        arrayOf(intArrayOf(1, -1), intArrayOf(-1, -1))
    }
}
