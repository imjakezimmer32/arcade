package app.snake.games.memory

enum class MemPhase { Setup, Ready, Playing, Won }

enum class MemGate { Pick, Seat }

data class MemTile(val pair: Int, val open: Boolean = false, val matched: Boolean = false)

data class MemState(
    val tiles: List<MemTile> = deal(),
    val first: Int? = null,
    val locked: Boolean = false,
    val misses: Int = 0,
    val elapsed: Int = 0,
    val phase: MemPhase = MemPhase.Setup,
    val gate: MemGate = MemGate.Pick,
    val hotSeat: Boolean = false,
    val leafName: String = "LEAF",
    val headName: String = "HEAD",
    val leafPairs: Int = 0,
    val headPairs: Int = 0,
    val leafTurn: Boolean = true,
    val recorded: Boolean = false,
    val scoreOffered: Boolean = false,
) {
    companion object {
        fun lobby() = MemState()
        fun solo() = MemState(phase = MemPhase.Ready)
        fun seat(leaf: String, head: String) = MemState(
            phase = MemPhase.Ready,
            hotSeat = true,
            leafName = leaf,
            headName = head,
        )
    }

    fun currentName() = if (leafTurn) leafName else headName
}

fun deal(): List<MemTile> {
    val ids = (0 until 8).flatMap { listOf(it, it) }.shuffled()
    return ids.map { MemTile(it) }
}

fun MemState.tick(): MemState =
    if (phase == MemPhase.Playing && !hotSeat) copy(elapsed = elapsed + 1) else this

fun MemState.wantSeat() = copy(gate = MemGate.Seat)

fun MemState.lobby() = MemState.lobby()

fun MemState.restart(): MemState = when {
    hotSeat -> MemState.seat(leafName, headName)
    phase == MemPhase.Setup -> this
    else -> MemState.solo()
}

fun MemState.flip(index: Int): MemState {
    if (locked || phase == MemPhase.Won || phase == MemPhase.Setup) return this
    val tile = tiles.getOrNull(index) ?: return this
    if (tile.open || tile.matched) return this
    val opened = tiles.toMutableList().also { it[index] = tile.copy(open = true) }
    val started = if (phase == MemPhase.Ready) MemPhase.Playing else phase
    val pick = first
    if (pick == null) return copy(tiles = opened, first = index, phase = started)
    val a = opened[pick]
    val b = opened[index]
    return if (a.pair == b.pair) {
        opened[pick] = a.copy(matched = true, open = true)
        opened[index] = b.copy(matched = true, open = true)
        val won = opened.all { it.matched }
        val nextLeaf = leafPairs + if (hotSeat && leafTurn) 1 else 0
        val nextHead = headPairs + if (hotSeat && !leafTurn) 1 else 0
        copy(
            tiles = opened,
            first = null,
            phase = if (won) MemPhase.Won else started,
            leafPairs = nextLeaf,
            headPairs = nextHead,
        )
    } else {
        copy(tiles = opened, first = pick, locked = true, misses = misses + 1, phase = started)
    }
}

fun MemState.resolveMismatch(): MemState {
    if (!locked) return this
    val next = tiles.map { if (it.matched) it else it.copy(open = false) }
    return copy(
        tiles = next,
        first = null,
        locked = false,
        leafTurn = if (hotSeat) !leafTurn else leafTurn,
    )
}

fun MemState.markRecorded() = copy(recorded = true, scoreOffered = true)
