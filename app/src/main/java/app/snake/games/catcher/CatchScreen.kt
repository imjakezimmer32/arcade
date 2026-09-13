package app.snake.games.catcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Bite
import app.snake.Dim
import app.snake.Head
import app.snake.Leaf
import app.snake.core.drawBasket
import app.snake.core.drawBerry
import app.snake.core.followX
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

@Composable
fun CatchGame(arcade: ArcadeViewModel, vm: CatchViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("catch", false)?.score ?: 0
    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == CatchPhase.Dead) {
            arcade.offerScore("catch", state.score, false, "Missed the crop", "${state.score} pts")
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "CATCH",
            subtitle = "best $best  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == CatchPhase.Running || state.phase == CatchPhase.Paused,
            paused = state.phase == CatchPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier.fillMaxWidth().weight(1f).followX(state.phase, vm::move),
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width
                val h = size.height
                state.berries.forEach { b ->
                    val c = Offset(b.x * w, b.y * h)
                    when (b.kind) {
                        2 -> drawCircle(Bite, w * CatchState.BERRY / 2f, c)
                        1 -> drawCircle(Head, w * CatchState.BERRY / 2f, c)
                        else -> drawBerry(c, w * CatchState.BERRY / 2f)
                    }
                }
                drawBasket(Offset(state.x * w, h * CatchState.BASKET_Y), w * CatchState.BASKET_W, h * CatchState.BASKET_H)
            }
            when (state.phase) {
                CatchPhase.Ready -> PitHint("Hold and slide the basket")
                CatchPhase.Paused -> CenterNote("Paused")
                CatchPhase.Dead -> EndOverlay(
                    "Missed the crop",
                    "${state.score} pts",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                CatchPhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Catch greens. Lime is bonus. Bite berries cost a life if you catch them.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
