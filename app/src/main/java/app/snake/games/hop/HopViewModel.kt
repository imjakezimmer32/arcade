package app.snake.games.hop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.Dir
import app.snake.core.gameClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HopViewModel : ViewModel() {
    private val _state = MutableStateFlow(HopState.fresh())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { gameClock { dt -> _state.update { it.step(dt) } } }
    }

    fun begin() { _state.update { it.begin() } }
    fun hop(dir: Dir) { _state.update { it.hop(dir) } }
    fun togglePause() { _state.update { it.pauseToggle() } }
    fun pauseIfRunning() { _state.update { it.pauseIfRunning() } }
    fun markScoreOffered() { _state.update { it.markScoreOffered() } }
}
