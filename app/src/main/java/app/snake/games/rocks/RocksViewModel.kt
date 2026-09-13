package app.snake.games.rocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.gameClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RocksViewModel : ViewModel() {
    private val _state = MutableStateFlow(RocksState.fresh())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { gameClock { dt -> _state.update { it.step(dt) } } }
    }

    fun begin() { _state.update { it.begin() } }
    fun fly(x: Float, y: Float, down: Boolean) { _state.update { it.fly(x, y, down) } }
    fun steer(delta: Float) { _state.update { it.steer(delta) } }
    fun thrust(on: Boolean) { _state.update { it.thrust(on) } }
    fun fire() { _state.update { it.fire() } }
    fun togglePause() { _state.update { it.pauseToggle() } }
    fun pauseIfRunning() { _state.update { it.pauseIfRunning() } }
    fun markScoreOffered() { _state.update { it.markScoreOffered() } }
}
