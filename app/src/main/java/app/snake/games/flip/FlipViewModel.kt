package app.snake.games.flip

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FlipViewModel : ViewModel() {
    private val _state = MutableStateFlow(FlipState.lobby())
    val state = _state.asStateFlow()

    fun sit(leaf: String, head: String) { _state.value = FlipState.start(leaf, head) }
    fun play(i: Int) { _state.update { it.play(i) } }
    fun ackPass() { _state.update { it.ackPass() } }
    fun rematch() {
        val cur = _state.value
        _state.value = FlipState.start(cur.leafName, cur.headName)
    }
    fun lobby() { _state.value = FlipState.lobby() }
    fun markRecorded() { _state.update { it.markRecorded() } }
}
