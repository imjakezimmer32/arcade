package app.snake.games.peck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Pad
import app.snake.PitAlt
import app.snake.core.drawSprout
import app.snake.core.onDown
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

@Composable
fun PeckGame(arcade: ArcadeViewModel, vm: PeckViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("peck", false)?.score ?: 0
    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == PeckPhase.Dead) {
            arcade.offerScore("peck", state.score, false, "They got away", "${state.score} pts")
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "PECK",
            subtitle = "best $best  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == PeckPhase.Running || state.phase == PeckPhase.Paused,
            paused = state.phase == PeckPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (r in 0..2) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (c in 0..2) {
                            val i = r * 3 + c
                            val hole = state.holes[i]
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(18.dp))
                                    .background(PitAlt)
                                    .onDown(i) { vm.peck(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(Modifier.fillMaxSize()) {
                                    val holeR = size.minDimension * 0.38f
                                    drawCircle(Pad, holeR, Offset(size.width / 2f, size.height * 0.62f))
                                    if (hole.up > 0f || hole.hit > 0f) {
                                        val peek = if (hole.hit > 0f) 0.45f else (hole.up / 0.8f).coerceIn(0.4f, 1f)
                                        drawSprout(Offset(size.width / 2f, size.height * 0.52f), holeR, peek)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            when (state.phase) {
                PeckPhase.Ready -> PitHint("Tap sprouts. Empty soil is safe.")
                PeckPhase.Paused -> CenterNote("Paused")
                PeckPhase.Dead -> EndOverlay(
                    "They got away",
                    "${state.score} pts",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                PeckPhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Peck sprouts before they duck. Missing them costs a life. Empty soil just breaks combo.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
