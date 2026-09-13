package app.snake.games.echo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EchoViewModel : ViewModel() {
    private val _state = MutableStateFlow(EchoState.fresh())
    val state = _state.asStateFlow()
    private var show: Job? = null

    fun begin() {
        val wasDead = _state.value.phase == EchoPhase.Dead
        _state.update { it.begin() }
        if (wasDead || _state.value.seq.isEmpty()) {
            growAndPlay()
        } else {
            playSeq()
        }
    }

    fun tap(pad: Int) {
        val before = _state.value
        if (before.phase != EchoPhase.Input) {
            if (before.phase == EchoPhase.Ready || before.phase == EchoPhase.Dead) begin()
            return
        }
        _state.update { it.tap(pad) }
        val after = _state.value
        if (after.phase == EchoPhase.Watch && after.score > before.score) {
            growAndPlay()
        } else if (after.phase == EchoPhase.Watch) {
            show?.cancel()
            show = viewModelScope.launch {
                delay(420)
                playSeq()
            }
        }
    }

    private fun growAndPlay() {
        _state.update { it.grow((0..3).random()) }
        playSeq()
    }

    private fun playSeq() {
        show?.cancel()
        show = viewModelScope.launch {
            delay(380)
            val seq = _state.value.seq
            for (pad in seq) {
                if (_state.value.phase != EchoPhase.Watch) return@launch
                _state.update { it.light(pad) }
                delay((380 - seq.size * 14).coerceAtLeast(160).toLong())
                _state.update { it.light(-1) }
                delay((110 - seq.size * 4).coerceAtLeast(60).toLong())
            }
            _state.update { it.listen() }
        }
    }

    fun markScoreOffered() { _state.update { it.markScoreOffered() } }
}
