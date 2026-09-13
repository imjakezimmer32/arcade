package app.snake.games.hop

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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.PitAlt
import app.snake.Pond
import app.snake.Void
import app.snake.core.Dir
import app.snake.core.drawCar
import app.snake.core.drawFrog
import app.snake.core.drawLilyPad
import app.snake.core.drawLog
import app.snake.core.swipePulse
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.LeafButton
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

private val HOME_COLS = intArrayOf(0, 2, 4, 6, 8)

@Composable
fun HopGame(arcade: ArcadeViewModel, vm: HopViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("hop", false)?.score ?: 0
    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == HopPhase.Dead) {
            arcade.offerScore("hop", state.score, false, "Splat", "${state.score} pts")
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "HOP",
            subtitle = "best $best  ·  wave ${state.wave}  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == HopPhase.Running || state.phase == HopPhase.Paused,
            paused = state.phase == HopPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier.fillMaxWidth().weight(1f).swipePulse(state.phase, onTap = { vm.hop(Dir.Up) }, onSwipe = vm::hop),
        ) {
            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                val cw = size.width / HopState.COLS
                val rh = size.height / HopState.ROWS
                for (r in 0 until HopState.ROWS) {
                    val color = when (r) {
                        0 -> Pond
                        in 1..3 -> Pond.copy(alpha = 0.55f)
                        4, 9 -> PitAlt
                        else -> Void
                    }
                    drawRect(color, Offset(0f, r * rh), Size(size.width, rh))
                }
                HOME_COLS.forEachIndexed { i, c ->
                    drawLilyPad(
                        Offset(c * cw + cw * 0.5f, rh * 0.5f),
                        minOf(cw, rh) * 0.38f,
                        state.homes.getOrElse(i) { false },
                    )
                }
                state.traffic.forEach { h ->
                    fun blob(x: Float) {
                        val left = Offset(x * size.width, h.row * rh + 5f)
                        val ww = h.w * size.width
                        val hh = rh - 10f
                        if (h.water) drawLog(left, ww, hh) else drawCar(left, ww, hh)
                    }
                    blob(h.x)
                    if (h.x + h.w > 1f) blob(h.x - 1.5f)
                    if (h.x < 0f) blob(h.x + 1.5f)
                }
                if (state.invuln <= 0f || (state.invuln * 10).toInt() % 2 == 0) {
                    drawFrog(Offset(state.ride * size.width, (state.row + 0.5f) * rh), size.width * HopState.FROG_W / 2f)
                }
            }
            when (state.phase) {
                HopPhase.Ready -> PitHint("Tap to hop up. Swipe to sidestep.")
                HopPhase.Paused -> CenterNote("Paused")
                HopPhase.Dead -> EndOverlay(
                    "Splat",
                    "${state.score} pts",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                HopPhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeafButton("◀", onClick = { vm.hop(Dir.Left) }, filled = false, modifier = Modifier.weight(1f))
            LeafButton("▲", onClick = { vm.hop(Dir.Up) }, modifier = Modifier.weight(1f))
            LeafButton("▼", onClick = { vm.hop(Dir.Down) }, filled = false, modifier = Modifier.weight(1f))
            LeafButton("▶", onClick = { vm.hop(Dir.Right) }, filled = false, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text("Ride the logs. Fill every lily. Tap hops forward.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
