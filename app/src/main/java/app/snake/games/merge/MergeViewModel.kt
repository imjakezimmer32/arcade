package app.snake.games.merge

import androidx.lifecycle.ViewModel
import app.snake.core.Dir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MergeViewModel : ViewModel() {
    private val _state = MutableStateFlow(MergeState.fresh())
    val state = _state.asStateFlow()

    fun swipe(dir: Dir) {
        _state.update { it.swipe(dir) }
    }

    fun keepGoing() {
        _state.update { it.keepGoing() }
    }

    fun restart() {
        _state.value = MergeState.fresh()
    }

    fun markScoreOffered() {
        _state.update { it.copy(scoreOffered = true) }
    }
}
