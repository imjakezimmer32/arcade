package app.snake.games.flit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.gameClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FlitViewModel : ViewModel() {
    private val _state = MutableStateFlow(FlitState.fresh())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            gameClock { dt -> _state.update { it.step(dt) } }
        }
    }

    fun flap() { _state.update { it.flap() } }
    fun restart() { _state.value = FlitState.fresh() }
    fun togglePause() { _state.update { it.pauseToggle() } }
    fun pauseIfRunning() { _state.update { it.pauseIfRunning() } }
    fun markScoreOffered() { _state.update { it.copy(scoreOffered = true) } }
}
