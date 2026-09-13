package app.snake.games.merge

import app.snake.core.Dir

enum class MergePhase { Ready, Playing, Won, Dead }

data class MergeState(
    val cells: IntArray = IntArray(16),
    val score: Int = 0,
    val phase: MergePhase = MergePhase.Ready,
    val wonKeptGoing: Boolean = false,
    val scoreOffered: Boolean = false,
) {
    companion object {
        fun fresh(): MergeState = MergeState().spawn().spawn().copy(phase = MergePhase.Ready)

        fun at(cells: IntArray, x: Int, y: Int) = cells[y * 4 + x]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MergeState) return false
        return cells.contentEquals(other.cells) &&
            score == other.score &&
            phase == other.phase &&
            wonKeptGoing == other.wonKeptGoing &&
            scoreOffered == other.scoreOffered
    }

    override fun hashCode() =
        31 * cells.contentHashCode() + 31 * score + phase.hashCode() + wonKeptGoing.hashCode() + scoreOffered.hashCode()
}

fun MergeState.spawn(): MergeState {
    val empty = cells.indices.filter { cells[it] == 0 }
    if (empty.isEmpty()) return this
    val next = cells.copyOf()
    next[empty.random()] = if ((0..9).random() == 0) 4 else 2
    return copy(cells = next)
}

fun MergeState.swipe(dir: Dir): MergeState {
    if (phase == MergePhase.Dead) return this
    val (slid, gained) = slide(cells, dir)
    if (slid.contentEquals(cells)) return this
    var next = copy(cells = slid, score = score + gained, phase = MergePhase.Playing).spawn()
    if (!next.wonKeptGoing && next.cells.any { it >= 2048 }) {
        next = next.copy(phase = MergePhase.Won, wonKeptGoing = true)
    }
    if (!hasMove(next.cells)) {
        next = next.copy(phase = MergePhase.Dead)
    }
    return next
}

fun MergeState.keepGoing(): MergeState =
    if (phase == MergePhase.Won) copy(phase = MergePhase.Playing, scoreOffered = false) else this

fun MergeState.restart(): MergeState = MergeState.fresh()

private fun slide(cells: IntArray, dir: Dir): Pair<IntArray, Int> {
    val next = IntArray(16)
    var gained = 0
    val vertical = dir == Dir.Up || dir == Dir.Down
    val reverse = dir == Dir.Right || dir == Dir.Down
    for (line in 0 until 4) {
        val vals = IntArray(4) { i ->
            val idx = if (vertical) {
                if (reverse) (3 - i) * 4 + line else i * 4 + line
            } else {
                if (reverse) line * 4 + (3 - i) else line * 4 + i
            }
            cells[idx]
        }
        val compact = vals.filter { it != 0 }.toMutableList()
        val merged = ArrayList<Int>(4)
        var i = 0
        while (i < compact.size) {
            if (i + 1 < compact.size && compact[i] == compact[i + 1]) {
                val v = compact[i] * 2
                merged += v
                gained += v
                i += 2
            } else {
                merged += compact[i]
                i += 1
            }
        }
        while (merged.size < 4) merged += 0
        for (j in 0 until 4) {
            val idx = if (vertical) {
                if (reverse) (3 - j) * 4 + line else j * 4 + line
            } else {
                if (reverse) line * 4 + (3 - j) else line * 4 + j
            }
            next[idx] = merged[j]
        }
    }
    return next to gained
}

private fun hasMove(cells: IntArray): Boolean {
    if (cells.any { it == 0 }) return true
    for (y in 0 until 4) for (x in 0 until 4) {
        val v = cells[y * 4 + x]
        if (x < 3 && cells[y * 4 + x + 1] == v) return true
        if (y < 3 && cells[(y + 1) * 4 + x] == v) return true
    }
    return false
}
