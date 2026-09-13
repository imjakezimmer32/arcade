package app.snake.games.echo

enum class EchoPhase { Ready, Watch, Input, Dead }

data class EchoState(
    val seq: List<Int> = emptyList(),
    val at: Int = 0,
    val lit: Int = -1,
    val lives: Int = 3,
    val score: Int = 0,
    val phase: EchoPhase = EchoPhase.Ready,
    val scoreOffered: Boolean = false,
) {
    companion object {
        fun fresh() = EchoState()
    }
}

fun EchoState.begin(): EchoState = when (phase) {
    EchoPhase.Ready -> copy(phase = EchoPhase.Watch)
    EchoPhase.Dead -> EchoState.fresh().copy(phase = EchoPhase.Watch)
    else -> this
}

fun EchoState.tap(pad: Int): EchoState {
    if (phase != EchoPhase.Input) return this
    if (seq.getOrNull(at) != pad) {
        val left = lives - 1
        return if (left <= 0) copy(lives = 0, phase = EchoPhase.Dead, lit = pad)
        else copy(lives = left, at = 0, phase = EchoPhase.Watch, lit = pad)
    }
    val next = at + 1
    return if (next >= seq.size) {
        copy(score = seq.size, at = 0, phase = EchoPhase.Watch, lit = -1)
    } else {
        copy(at = next, lit = pad)
    }
}

fun EchoState.light(pad: Int) = copy(lit = pad)
fun EchoState.grow(pad: Int) = copy(seq = seq + pad, at = 0, phase = EchoPhase.Watch, lit = -1)
fun EchoState.listen() = copy(phase = EchoPhase.Input, lit = -1, at = 0)
fun EchoState.markScoreOffered() = copy(scoreOffered = true)
