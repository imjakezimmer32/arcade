package app.snake.games.boxes

enum class BoxesPhase { Setup, Playing, Over }

data class BoxesState(
    val h: BooleanArray = BooleanArray(5 * 4),
    val v: BooleanArray = BooleanArray(4 * 5),
    val owner: IntArray = IntArray(16),
    val leafTurn: Boolean = true,
    val phase: BoxesPhase = BoxesPhase.Setup,
    val leafName: String = "LEAF",
    val headName: String = "HEAD",
    val passPending: Boolean = false,
    val recorded: Boolean = false,
    val lastH: Int = -1,
    val lastV: Int = -1,
) {
    companion object {
        fun lobby() = BoxesState()
        fun start(leaf: String, head: String) = BoxesState(
            phase = BoxesPhase.Playing,
            leafName = leaf,
            headName = head,
        )
    }

    fun currentName() = if (leafTurn) leafName else headName
    fun count(leaf: Boolean) = owner.count { it == if (leaf) 1 else 2 }
    override fun equals(other: Any?) = other is BoxesState && h.contentEquals(other.h) &&
        v.contentEquals(other.v) && owner.contentEquals(other.owner) &&
        leafTurn == other.leafTurn && phase == other.phase && passPending == other.passPending &&
        recorded == other.recorded && leafName == other.leafName && headName == other.headName &&
        lastH == other.lastH && lastV == other.lastV
    override fun hashCode() = h.contentHashCode() * 31 + v.contentHashCode()
}

fun BoxesState.ackPass() = copy(passPending = false)
fun BoxesState.markRecorded() = copy(recorded = true)

fun BoxesState.claimH(i: Int): BoxesState {
    if (phase != BoxesPhase.Playing || passPending || i !in h.indices || h[i]) return this
    val nextH = h.copyOf().also { it[i] = true }
    return settle(nextH, v.copyOf(), lastH = i, lastV = -1)
}

fun BoxesState.claimV(i: Int): BoxesState {
    if (phase != BoxesPhase.Playing || passPending || i !in v.indices || v[i]) return this
    val nextV = v.copyOf().also { it[i] = true }
    return settle(h.copyOf(), nextV, lastH = -1, lastV = i)
}

private fun BoxesState.settle(nextH: BooleanArray, nextV: BooleanArray, lastH: Int, lastV: Int): BoxesState {
    val piece = if (leafTurn) 1 else 2
    val nextOwner = owner.copyOf()
    var gained = 0
    for (r in 0..3) for (c in 0..3) {
        val idx = r * 4 + c
        if (nextOwner[idx] != 0) continue
        val top = nextH[r * 4 + c]
        val bot = nextH[(r + 1) * 4 + c]
        val left = nextV[r * 5 + c]
        val right = nextV[r * 5 + c + 1]
        if (top && bot && left && right) {
            nextOwner[idx] = piece
            gained++
        }
    }
    val full = nextOwner.none { it == 0 }
    return copy(
        h = nextH,
        v = nextV,
        owner = nextOwner,
        leafTurn = if (gained > 0) leafTurn else !leafTurn,
        lastH = lastH,
        lastV = lastV,
        phase = if (full) BoxesPhase.Over else BoxesPhase.Playing,
        passPending = !full && gained == 0,
    )
}
