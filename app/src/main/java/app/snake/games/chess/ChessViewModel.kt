package app.snake.games.chess

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChessViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChessState.lobby())
    val state = _state.asStateFlow()

    fun sit(white: String, black: String) {
        _state.value = ChessState.start(white, black)
    }

    fun tap(square: Int) {
        _state.update { it.tap(square) }
    }

    fun pickPromo(kind: Kind) {
        _state.update { it.pickPromo(kind) }
    }

    fun ackPass() {
        _state.update { it.ackPass() }
    }

    fun undo() {
        _state.update { it.undo() }
    }

    fun rematch() {
        val cur = _state.value
        _state.value = ChessState.start(cur.whiteName, cur.blackName)
    }

    fun lobby() {
        _state.value = ChessState.lobby()
    }

    fun markRecorded() {
        _state.update { it.markRecorded() }
    }
}
