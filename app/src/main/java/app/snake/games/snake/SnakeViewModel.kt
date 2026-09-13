package app.snake.games.snake

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.Dir
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SnakeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(SnakeState.lobby())
    val state = _state.asStateFlow()

    private val _events = Channel<SnakeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(_state.value.tickMs)
                var event: SnakeEvent? = null
                _state.update { current ->
                    val result = current.step()
                    event = result.event
                    result.state
                }
                event?.let { _events.send(it) }
            }
        }
    }

    fun turn(dir: Dir) {
        _state.update { it.queueTurn(dir) }
    }

    fun pickSolo() { _state.value = SnakeState.solo() }
    fun wantSeat() { _state.update { it.wantSeat() } }
    fun sit(leaf: String, head: String) { _state.value = SnakeState.seat(leaf, head) }
    fun lobby() { _state.value = SnakeState.lobby() }
    fun ackPass() { _state.update { it.ackPass() } }

    fun primaryAction() {
        _state.update { it.begin() }
    }

    fun togglePause() {
        _state.update { it.pauseToggle() }
    }

    fun pauseIfRunning() {
        _state.update { it.pauseIfRunning() }
    }

    fun markScoreOffered() {
        _state.update { it.markRecorded() }
    }
}
