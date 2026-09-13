package app.snake.games.four

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FourViewModel : ViewModel() {
    private val _state = MutableStateFlow(FourState.lobby())
    val state = _state.asStateFlow()

    fun sit(leaf: String, head: String) { _state.value = FourState.start(leaf, head) }
    fun drop(col: Int) { _state.update { it.drop(col) } }
    fun ackPass() { _state.update { it.ackPass() } }
    fun rematch() {
        val cur = _state.value
        _state.value = FourState.start(cur.leafName, cur.headName)
    }
    fun lobby() { _state.value = FourState.lobby() }
    fun markRecorded() { _state.update { it.markRecorded() } }
}
