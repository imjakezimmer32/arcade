package app.snake.games.breakout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.gameClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BreakoutViewModel : ViewModel() {
    private val _state = MutableStateFlow(BreakState.fresh())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            gameClock { dt -> _state.update { it.step(dt) } }
        }
    }

    fun begin() {
        _state.update { it.begin() }
    }

    fun movePaddle(x: Float) {
        _state.update { it.movePaddle(x) }
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
