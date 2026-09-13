package app.snake.games.stacks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StacksViewModel : ViewModel() {
    private val _state = MutableStateFlow(StackState.fresh())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(_state.value.tickMs)
                _state.update { current ->
                    if (current.phase == StackPhase.Running) current.gravity(lock = true) else current
                }
            }
        }
    }

    fun begin() {
        _state.update { it.begin() }
    }

    fun shift(dx: Int) {
        _state.update { it.shift(dx) }
    }

    fun rotate() {
        _state.update { it.rotate() }
    }

    fun softDrop() {
        _state.update { it.softDrop() }
    }

    fun hardDrop() {
        _state.update { it.hardDrop() }
    }

    fun togglePause() {
        _state.update { it.pauseToggle() }
    }

    fun pauseIfRunning() {
        _state.update { it.pauseIfRunning() }
    }

    fun markScoreOffered() {
        _state.update { it.copy(scoreOffered = true) }
    }
}
