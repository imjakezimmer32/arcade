package app.snake.games.invaders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.gameClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InvadersViewModel : ViewModel() {
    private val _state = MutableStateFlow(InvState.fresh())
    val state = _state.asStateFlow()

    private val _events = Channel<InvEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            gameClock { dt ->
                var event: InvEvent? = null
                _state.update {
                    val result = it.step(dt)
                    event = result.second
                    result.first
                }
                event?.let { _events.trySend(it) }
            }
        }
    }

    fun begin() { _state.update { it.begin() } }
    fun aim(x: Float) { _state.update { it.aim(x) } }
    fun hold(on: Boolean) { _state.update { it.hold(on) } }
    fun fire() {
        _state.update {
            val result = it.fire()
            result.second?.let { ev -> _events.trySend(ev) }
            result.first
        }
    }
    fun togglePause() { _state.update { it.pauseToggle() } }
    fun pauseIfRunning() { _state.update { it.pauseIfRunning() } }
    fun markScoreOffered() { _state.update { it.copy(scoreOffered = true) } }
}
