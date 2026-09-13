package app.snake

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import app.snake.core.GameId
import app.snake.scores.ScorePrompt
import app.snake.scores.ScoreStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class Route {
    data object Menu : Route()
    data object Scores : Route()
    data class Play(val game: GameId) : Route()
}

class ArcadeViewModel(app: Application) : AndroidViewModel(app) {
    val scores = ScoreStore(app)

    private val _route = MutableStateFlow<Route>(Route.Menu)
    val route = _route.asStateFlow()

    private val _pause = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pause = _pause.asSharedFlow()

    private val _revision = MutableStateFlow(0)
    val revision = _revision.asStateFlow()

    private val _prompt = MutableStateFlow<ScorePrompt?>(null)
    val prompt = _prompt.asStateFlow()

    init {
        val legacy = app.getSharedPreferences("snake", Context.MODE_PRIVATE).getInt("high", 0)
        scores.migrateAnonymous("snake", legacy)
    }

    fun go(route: Route) {
        _prompt.value = null
        _route.value = route
    }

    fun back() {
        _prompt.value = null
        _route.value = Route.Menu
    }

    fun requestPause() {
        _pause.tryEmit(Unit)
    }

    fun offerScore(
        boardKey: String,
        score: Int,
        lowerBetter: Boolean,
        headline: String,
        detail: String,
        win: Boolean = true,
    ) {
        if (_prompt.value != null) return
        if (scores.beatsBest(boardKey, score, lowerBetter, win)) {
            _prompt.value = ScorePrompt(boardKey, score, lowerBetter, headline, detail, win)
        } else {
            scores.submit(boardKey, scores.lastName.ifBlank { "PLAYER" }, score, win)
            _revision.value += 1
        }
    }

    fun saveScore(name: String) {
        val prompt = _prompt.value ?: return
        scores.submit(prompt.boardKey, name, prompt.score, prompt.win)
        _revision.value += 1
        _prompt.value = null
    }

    fun skipScore() {
        val prompt = _prompt.value ?: return
        scores.submit(prompt.boardKey, scores.lastName.ifBlank { "PLAYER" }, prompt.score, prompt.win)
        _revision.value += 1
        _prompt.value = null
    }

    fun recordRun(boardKey: String, name: String, score: Int, win: Boolean) {
        scores.submit(boardKey, name, score, win)
        _revision.value += 1
    }
}
