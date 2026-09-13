package app.snake.games.mines

enum class MinesDiff(
    val cols: Int,
    val rows: Int,
    val mines: Int,
    val label: String,
    val boardKey: String,
) {
    Beginner(9, 9, 10, "Easy", "mines_beginner"),
    Intermediate(16, 16, 40, "Med", "mines_intermediate"),
    Expert(16, 30, 99, "Hard", "mines_expert"),
}

enum class MinesMark { None, Flag, Question }

enum class MinesStatus { Ready, Playing, Won, Lost }

data class MinesTile(
    val mine: Boolean = false,
    val adj: Int = 0,
    val open: Boolean = false,
    val mark: MinesMark = MinesMark.None,
)

data class MinesState(
    val diff: MinesDiff = MinesDiff.Beginner,
    val tiles: List<MinesTile> = List(MinesDiff.Beginner.cols * MinesDiff.Beginner.rows) { MinesTile() },
    val status: MinesStatus = MinesStatus.Ready,
    val elapsedSec: Int = 0,
    val flagMode: Boolean = false,
    val questions: Boolean = true,
    val scoreOffered: Boolean = false,
) {
    val cols: Int get() = diff.cols
    val rows: Int get() = diff.rows
    val flags: Int get() = tiles.count { it.mark == MinesMark.Flag }
    val remaining: Int get() = diff.mines - flags

    fun at(x: Int, y: Int) = tiles[y * cols + x]
}

fun MinesState.withDiff(diff: MinesDiff) = MinesState(diff = diff)

fun MinesState.toggleFlagMode() = copy(flagMode = !flagMode)

fun MinesState.toggleQuestions() = copy(questions = !questions)

fun MinesState.tickTimer(): MinesState =
    if (status == MinesStatus.Playing) copy(elapsedSec = elapsedSec + 1) else this

fun MinesState.openAt(x: Int, y: Int): MinesState {
    if (status == MinesStatus.Won || status == MinesStatus.Lost) return this
    if (x !in 0 until cols || y !in 0 until rows) return this
    val i = y * cols + x
    val tile = tiles[i]
    if (tile.mark == MinesMark.Flag) return this
    if (tile.open) return chord(x, y)

    var next = this
    if (status == MinesStatus.Ready) {
        next = next.placeMines(safeX = x, safeY = y).copy(status = MinesStatus.Playing)
    }
    return next.reveal(x, y)
}

fun MinesState.cycleMark(x: Int, y: Int): MinesState {
    if (status == MinesStatus.Won || status == MinesStatus.Lost) return this
    if (x !in 0 until cols || y !in 0 until rows) return this
    val i = y * cols + x
    val tile = tiles[i]
    if (tile.open) return this
    val nextMark = when (tile.mark) {
        MinesMark.None -> MinesMark.Flag
        MinesMark.Flag -> if (questions) MinesMark.Question else MinesMark.None
        MinesMark.Question -> MinesMark.None
    }
    return copy(tiles = tiles.toMutableList().also { it[i] = tile.copy(mark = nextMark) })
}

fun MinesState.press(x: Int, y: Int): MinesState =
    if (flagMode) cycleMark(x, y) else openAt(x, y)

fun MinesState.longPress(x: Int, y: Int): MinesState =
    if (flagMode) openAt(x, y) else cycleMark(x, y)

private fun MinesState.placeMines(safeX: Int, safeY: Int): MinesState {
    val forbidden = HashSet<Int>()
    for (dy in -1..1) {
        for (dx in -1..1) {
            val nx = safeX + dx
            val ny = safeY + dy
            if (nx in 0 until cols && ny in 0 until rows) forbidden += ny * cols + nx
        }
    }
    val pool = (0 until cols * rows).filter { it !in forbidden }.shuffled()
    val mineSet = pool.take(diff.mines).toHashSet()
    val next = MutableList(cols * rows) { MinesTile() }
    for (i in next.indices) {
        val x = i % cols
        val y = i / cols
        var adj = 0
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until cols && ny in 0 until rows && (ny * cols + nx) in mineSet) adj++
        }
        next[i] = MinesTile(mine = i in mineSet, adj = adj)
    }
    return copy(tiles = next)
}

private fun MinesState.reveal(startX: Int, startY: Int): MinesState {
    val next = tiles.toMutableList()
    val stack = ArrayDeque<Pair<Int, Int>>()
    stack.add(startX to startY)
    while (stack.isNotEmpty()) {
        val (x, y) = stack.removeFirst()
        val i = y * cols + x
        val tile = next[i]
        if (tile.open || tile.mark == MinesMark.Flag) continue
        if (tile.mine) {
            for (idx in next.indices) {
                if (next[idx].mine) next[idx] = next[idx].copy(open = true, mark = MinesMark.None)
            }
            return copy(tiles = next, status = MinesStatus.Lost)
        }
        next[i] = tile.copy(open = true, mark = MinesMark.None)
        if (tile.adj == 0) {
            for (dy in -1..1) for (dx in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until cols && ny in 0 until rows) stack.add(nx to ny)
            }
        }
    }
    return copy(tiles = next).checkWin()
}

private fun MinesState.chord(x: Int, y: Int): MinesState {
    val tile = at(x, y)
    if (!tile.open || tile.adj == 0) return this
    var flags = 0
    val hidden = ArrayList<Pair<Int, Int>>()
    for (dy in -1..1) for (dx in -1..1) {
        if (dx == 0 && dy == 0) continue
        val nx = x + dx
        val ny = y + dy
        if (nx !in 0 until cols || ny !in 0 until rows) continue
        val n = at(nx, ny)
        if (n.mark == MinesMark.Flag) flags++
        else if (!n.open) hidden += nx to ny
    }
    if (flags != tile.adj) return this
    var current = this
    for ((nx, ny) in hidden) {
        current = current.reveal(nx, ny)
        if (current.status == MinesStatus.Lost) return current
    }
    return current
}

private fun MinesState.checkWin(): MinesState {
    val hiddenSafe = tiles.any { !it.mine && !it.open }
    if (hiddenSafe) return this
    val flagged = tiles.map {
        if (it.mine) it.copy(mark = MinesMark.Flag, open = it.open) else it
    }
    return copy(tiles = flagged, status = MinesStatus.Won)
}
