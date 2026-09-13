package app.snake.games.flit

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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Leaf
import app.snake.Pad
import app.snake.core.drawBird
import app.snake.core.onDown
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

@Composable
fun FlitGame(arcade: ArcadeViewModel, vm: FlitViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("flit", false)?.score ?: 0
    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == FlitPhase.Dead) {
            arcade.offerScore("flit", state.score, false, "Clipped a pipe", "${state.score} gates")
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "FLIT",
            subtitle = "best $best",
            value = state.score.toString().padStart(3, '0'),
            valueLabel = "gates",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == FlitPhase.Running || state.phase == FlitPhase.Paused,
            paused = state.phase == FlitPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier.fillMaxWidth().weight(1f).onDown(state.phase) {
                when (state.phase) {
                    FlitPhase.Dead -> vm.restart()
                    else -> vm.flap()
                }
            },
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(Pad, Offset(0f, h * 0.94f), Size(w, h * 0.08f), CornerRadius(0f))
                state.pipes.forEach { p ->
                    val gap = p.gap * h
                    val x = p.x * w
                    val pw = w * FlitState.PIPE_W
                    val halfGap = h * FlitState.GAP
                    drawRoundRect(Leaf, Offset(x - pw / 2, 0f), Size(pw, (gap - halfGap).coerceAtLeast(0f)), CornerRadius(8f))
                    drawRoundRect(Leaf, Offset(x - pw / 2 - 6f, gap - halfGap - 12f), Size(pw + 12f, 14f), CornerRadius(4f))
                    val bottom = gap + halfGap
                    drawRoundRect(Leaf, Offset(x - pw / 2, bottom), Size(pw, h * 0.94f - bottom), CornerRadius(8f))
                    drawRoundRect(Leaf, Offset(x - pw / 2 - 6f, bottom), Size(pw + 12f, 14f), CornerRadius(4f))
                }
                val by = state.y * h
                val bx = w * FlitState.BIRD_X
                val flap = (state.v * -1.4f).coerceIn(-1f, 1f)
                drawBird(Offset(bx, by), FlitState.RADIUS * w, flap)
            }
            when (state.phase) {
                FlitPhase.Ready -> PitHint("Tap to flap")
                FlitPhase.Paused -> CenterNote("Paused")
                FlitPhase.Dead -> EndOverlay(
                    "Clipped a pipe",
                    "${state.score} gates",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::restart,
                )
                FlitPhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Tap to rise. Thread the garden posts.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
