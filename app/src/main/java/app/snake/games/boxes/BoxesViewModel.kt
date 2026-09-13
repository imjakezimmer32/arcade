package app.snake.games.boxes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BoxesViewModel : ViewModel() {
    private val _state = MutableStateFlow(BoxesState.lobby())
    val state = _state.asStateFlow()

    fun sit(leaf: String, head: String) { _state.value = BoxesState.start(leaf, head) }
    fun claimH(i: Int) { _state.update { it.claimH(i) } }
    fun claimV(i: Int) { _state.update { it.claimV(i) } }
    fun ackPass() { _state.update { it.ackPass() } }
    fun rematch() {
        val cur = _state.value
        _state.value = BoxesState.start(cur.leafName, cur.headName)
    }
    fun lobby() { _state.value = BoxesState.lobby() }
    fun markRecorded() { _state.update { it.markRecorded() } }
}
