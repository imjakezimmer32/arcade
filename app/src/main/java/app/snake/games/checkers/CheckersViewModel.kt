package app.snake.games.checkers

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CheckersViewModel : ViewModel() {
    private val _state = MutableStateFlow(CkState.lobby())
    val state = _state.asStateFlow()

    fun sit(leaf: String, head: String) {
        _state.value = CkState.start(leaf, head)
    }

    fun tap(square: Int) {
        _state.update { it.tap(square) }
    }

    fun ackPass() {
        _state.update { it.ackPass() }
    }

    fun undo() {
        _state.update { it.undo() }
    }

    fun rematch() {
        val cur = _state.value
        _state.value = CkState.start(cur.leafName, cur.headName)
    }

    fun lobby() {
        _state.value = CkState.lobby()
    }

    fun markRecorded() {
        _state.update { it.markRecorded() }
    }
}
