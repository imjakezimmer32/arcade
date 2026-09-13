package app.snake.games.dodge

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
import app.snake.Bite
import app.snake.Dim
import app.snake.Head
import app.snake.core.drawBeetle
import app.snake.core.followX
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

@Composable
fun DodgeGame(arcade: ArcadeViewModel, vm: DodgeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("dodge", false)?.score ?: 0
    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == DodgePhase.Dead) {
            arcade.offerScore("dodge", state.score, false, "Hit", "${state.score} pts")
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "DODGE",
            subtitle = "best $best  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == DodgePhase.Running || state.phase == DodgePhase.Paused,
            paused = state.phase == DodgePhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier.fillMaxWidth().weight(1f).followX(state.phase, vm::move),
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width
                val h = size.height
                state.hazards.forEach { hz ->
                    drawRoundRect(
                        Bite,
                        Offset((hz.x - hz.w / 2) * w, hz.y * h - h * DodgeState.HAZ_H / 2f),
                        Size(hz.w * w, h * DodgeState.HAZ_H),
                        CornerRadius(10f),
                    )
                    drawCircle(Head.copy(alpha = 0.45f), 5f, Offset(hz.x * w, hz.y * h))
                }
                if (state.invuln <= 0f || (state.invuln * 10).toInt() % 2 == 0) {
                    drawBeetle(Offset(state.x * w, h * DodgeState.SHIP_Y), w * DodgeState.SHIP_W / 2f)
                }
            }
            when (state.phase) {
                DodgePhase.Ready -> PitHint("Hold and slide. Finger is the beetle.")
                DodgePhase.Paused -> CenterNote("Paused")
                DodgePhase.Dead -> EndOverlay(
                    "Hit",
                    "${state.score} pts",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                DodgePhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Your finger is the beetle. Bites drift later. Three lives.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
