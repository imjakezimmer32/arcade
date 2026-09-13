package app.snake.games.mines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MinesViewModel : ViewModel() {
    private val _state = MutableStateFlow(MinesState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.tickTimer() }
            }
        }
    }

    fun setDiff(diff: MinesDiff) {
        _state.value = MinesState(diff = diff)
    }

    fun press(x: Int, y: Int) {
        _state.update { it.press(x, y) }
    }

    fun longPress(x: Int, y: Int) {
        _state.update { it.longPress(x, y) }
    }

    fun toggleFlagMode() {
        _state.update { it.toggleFlagMode() }
    }

    fun restart() {
        _state.update { MinesState(diff = it.diff, flagMode = it.flagMode, questions = it.questions) }
    }

    fun markScoreOffered() {
        _state.update { it.copy(scoreOffered = true) }
    }
}
