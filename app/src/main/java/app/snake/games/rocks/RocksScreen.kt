package app.snake.games.rocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import app.snake.Dim
import app.snake.Head
import app.snake.core.drawCraft
import app.snake.core.drawStone
import app.snake.core.followFinger
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.LeafButton
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

@Composable
fun RocksGame(arcade: ArcadeViewModel, vm: RocksViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("rocks", false)?.score ?: 0
    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == RockPhase.Dead) {
            arcade.offerScore("rocks", state.score, false, "Cracked", "${state.score} pts")
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "ROCKS",
            subtitle = "best $best  ·  wave ${state.wave}  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == RockPhase.Running || state.phase == RockPhase.Paused,
            paused = state.phase == RockPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier.fillMaxWidth().weight(1f).followFinger(state.phase, vm::fly, onTap = vm::fire),
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width
                val h = size.height
                state.rocks.forEach { r ->
                    for (ox in -1..1) for (oy in -1..1) {
                        val nx = r.x + ox
                        val ny = r.y + oy
                        if (ox == 0 && oy == 0 || nx in -0.2f..1.2f && ny in -0.2f..1.2f) {
                            drawStone(Offset(nx * w, ny * h), r.r * w, r.x + r.y)
                        }
                    }
                }
                state.shots.forEach { s ->
                    drawCircle(Head, RocksState.SHOT_R * w, Offset(s.x * w, s.y * h))
                }
                if (state.invuln <= 0f || (state.invuln * 10).toInt() % 2 == 0) {
                    drawCraft(Offset(state.x * w, state.y * h), RocksState.SHIP_R * w, state.ang, state.thrusting)
                }
            }
            when (state.phase) {
                RockPhase.Ready -> PitHint("Hold toward a stone to fly. Tap to fire.")
                RockPhase.Paused -> CenterNote("Paused")
                RockPhase.Dead -> EndOverlay(
                    "Cracked",
                    "${state.score} pts",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                RockPhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeafButton("FIRE", onClick = vm::fire, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text("Hold a finger on the pit to fly toward it. Tap to shoot. Screen wraps.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
