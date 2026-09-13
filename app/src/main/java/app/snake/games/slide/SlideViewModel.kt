package app.snake.games.slide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.snake.core.Dir
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SlideViewModel : ViewModel() {
    private val _state = MutableStateFlow(SlideState.fresh())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { it.tick() }
            }
        }
    }

    fun begin() { _state.update { it.begin() } }
    fun tap(i: Int) { _state.update { it.tap(i) } }
    fun swipe(dir: Dir) { _state.update { it.swipe(dir) } }
    fun shuffle() { _state.value = SlideState.fresh() }
    fun markScoreOffered() { _state.update { it.markScoreOffered() } }
}
