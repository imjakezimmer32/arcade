package app.snake.games.echo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import app.snake.Pad
import app.snake.PadStroke
import app.snake.Void
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.PitFrame
import app.snake.ui.PitHint
import app.snake.core.onDown

private val Pads = listOf(Leaf, Head, Bite, Color(0xFF7FDBFF))
private val Glyphs = listOf("✿", "★", "●", "◆")

@Composable
fun EchoGame(arcade: ArcadeViewModel, vm: EchoViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("echo", false)?.score ?: 0
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == EchoPhase.Dead) {
            arcade.offerScore("echo", state.score, false, "Broke the chain", "${state.score} rounds")
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "ECHO",
            subtitle = when (state.phase) {
                EchoPhase.Watch -> "watch"
                EchoPhase.Input -> "your turn  ·  best $best  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}"
                else -> "best $best  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}"
            },
            value = state.score.toString().padStart(2, '0'),
            valueLabel = "round",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0..1) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (col in 0..1) {
                            val i = row * 2 + col
                            val on = state.lit == i
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(18.dp))
                                    .background(if (on) Pads[i] else Pad)
                                    .border(2.dp, if (on) Pads[i] else PadStroke, RoundedCornerShape(18.dp))
                                    .onDown(i) { vm.tap(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    Glyphs[i],
                                    color = if (on) Void else Pads[i].copy(alpha = 0.55f),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
            when (state.phase) {
                EchoPhase.Ready -> PitHint("Tap a pad to listen")
                EchoPhase.Watch, EchoPhase.Input -> Unit
                EchoPhase.Dead -> EndOverlay(
                    "Broke the chain",
                    "${state.score} rounds",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Repeat the garden. It grows each round.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
