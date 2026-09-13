package app.snake.games.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryViewModel : ViewModel() {
    private val _state = MutableStateFlow(MemState.lobby())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.tick() }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(16)
                val cur = _state.value
                if (cur.locked) {
                    delay(520)
                    _state.update { it.resolveMismatch() }
                }
            }
        }
    }

    fun pickSolo() { _state.value = MemState.solo() }
    fun wantSeat() { _state.update { it.wantSeat() } }
    fun sit(leaf: String, head: String) { _state.value = MemState.seat(leaf, head) }
    fun lobby() { _state.value = MemState.lobby() }
    fun flip(index: Int) { _state.update { it.flip(index) } }
    fun restart() { _state.update { it.restart() } }
    fun markScoreOffered() { _state.update { it.markRecorded() } }
}
