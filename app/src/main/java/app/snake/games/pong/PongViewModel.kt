package app.snake.games.pong

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.gameClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PongViewModel : ViewModel() {
    private val _state = MutableStateFlow(PongState.lobby())
    val state = _state.asStateFlow()

    private val _events = Channel<PongEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            gameClock { dt ->
                var event: PongEvent? = null
                _state.update {
                    val result = it.step(dt)
                    event = result.second
                    result.first
                }
                event?.let { _events.trySend(it) }
            }
        }
    }

    fun pickSolo() { _state.value = PongState.solo() }
    fun wantSeat() { _state.update { it.wantSeat() } }
    fun sit(leaf: String, head: String) { _state.value = PongState.seat(leaf, head) }
    fun lobby() { _state.value = PongState.lobby() }
    fun begin() { _state.update { it.begin() } }
    fun moveLeaf(x: Float) { _state.update { it.moveLeaf(x) } }
    fun moveHead(x: Float) { _state.update { it.moveHead(x) } }
    fun togglePause() { _state.update { it.pauseToggle() } }
    fun pauseIfRunning() { _state.update { it.pauseIfRunning() } }
    fun markScoreOffered() { _state.update { it.markScoreOffered() } }
}
