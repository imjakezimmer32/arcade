package app.snake.games.slide

import app.snake.core.Dir

enum class SlidePhase { Ready, Playing, Won }

data class SlideState(
    val cells: IntArray = solved(),
    val elapsed: Int = 0,
    val moves: Int = 0,
    val phase: SlidePhase = SlidePhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        const val N = 4
        fun fresh() = SlideState(cells = scramble())
    }

    override fun equals(other: Any?) = other is SlideState && cells.contentEquals(other.cells) &&
        elapsed == other.elapsed && moves == other.moves && phase == other.phase && scoreOffered == other.scoreOffered
    override fun hashCode() = cells.contentHashCode()
}

fun solved(): IntArray = IntArray(16) { if (it == 15) 0 else it + 1 }

fun scramble(): IntArray {
    val cells = solved()
    var empty = 15
    repeat(90) {
        val r = empty / 4
        val c = empty % 4
        val opts = buildList {
            if (r > 0) add(empty - 4)
            if (r < 3) add(empty + 4)
            if (c > 0) add(empty - 1)
            if (c < 3) add(empty + 1)
        }
        val pick = opts.random()
        cells[empty] = cells[pick]
        cells[pick] = 0
        empty = pick
    }
    return cells
}

fun SlideState.begin() = when (phase) {
    SlidePhase.Ready -> copy(phase = SlidePhase.Playing)
    SlidePhase.Won -> SlideState.fresh().copy(phase = SlidePhase.Playing)
    SlidePhase.Playing -> this
}

fun SlideState.tick() = if (phase == SlidePhase.Playing) copy(elapsed = elapsed + 1) else this

fun SlideState.tap(i: Int): SlideState {
    if (phase == SlidePhase.Won || i !in 0..15) return this
    val empty = cells.indexOf(0)
    val er = empty / 4
    val ec = empty % 4
    val r = i / 4
    val c = i % 4
    val adj = (r == er && kotlin.math.abs(c - ec) == 1) || (c == ec && kotlin.math.abs(r - er) == 1)
    if (!adj) return if (phase == SlidePhase.Ready) copy(phase = SlidePhase.Playing) else this
    val next = cells.copyOf()
    next[empty] = cells[i]
    next[i] = 0
    val playing = if (phase == SlidePhase.Ready) SlidePhase.Playing else phase
    val won = next.contentEquals(solved())
    return copy(
        cells = next,
        moves = moves + 1,
        phase = if (won) SlidePhase.Won else playing,
    )
}

fun SlideState.swipe(dir: Dir): SlideState {
    val empty = cells.indexOf(0)
    val r = empty / 4
    val c = empty % 4
    val from = when (dir) {
        Dir.Left -> if (c < 3) empty + 1 else return this
        Dir.Right -> if (c > 0) empty - 1 else return this
        Dir.Up -> if (r < 3) empty + 4 else return this
        Dir.Down -> if (r > 0) empty - 4 else return this
    }
    return tap(from)
}

fun SlideState.markScoreOffered() = copy(scoreOffered = true)
