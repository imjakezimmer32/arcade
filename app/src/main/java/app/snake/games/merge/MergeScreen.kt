package app.snake.games.merge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.snake.Dim
import app.snake.Leaf
import app.snake.Mist
import app.snake.Pad
import app.snake.PadStroke
import app.snake.Pit
import app.snake.Void
import app.snake.core.Dir
import app.snake.core.swipePulse
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.LeafButton
import app.snake.ui.PitFrame
import kotlin.math.abs
import kotlin.math.ln

@Composable
fun MergeGame(arcade: ArcadeViewModel, vm: MergeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("merge", false)?.score ?: 0

    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == MergePhase.Dead) {
            arcade.offerScore("merge", state.score, false, "No moves", "${state.score} pts")
            vm.markScoreOffered()
        }
        if (!state.scoreOffered && state.phase == MergePhase.Won) {
            arcade.offerScore("merge", state.score, false, "2048", "${state.score} pts")
            vm.markScoreOffered()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "2048",
            subtitle = "best $best",
            value = state.score.toString(),
            valueLabel = "score",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(14.dp))
        PitFrame(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .swipePulse(state.phase, slop = 24f, rearm = false, onSwipe = vm::swipe),
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                for (y in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    ) {
                        for (x in 0 until 4) {
                            val value = state.cells[y * 4 + x]
                            Tile(value, Modifier.weight(1f).fillMaxSize())
                        }
                    }
                }
            }
            when (state.phase) {
                MergePhase.Won -> EndOverlay(
                    title = "2048",
                    detail = "${state.score} pts",
                    prompt = prompt,
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onSave = { name ->
                        arcade.saveScore(name)
                        vm.keepGoing()
                    },
                    onSkip = {
                        arcade.skipScore()
                        vm.keepGoing()
                    },
                    onRetry = vm::keepGoing,
                    retryLabel = "KEEP GOING",
                )
                MergePhase.Dead -> EndOverlay(
                    title = "No moves",
                    detail = "${state.score} pts",
                    prompt = prompt,
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onSave = arcade::saveScore,
                    onSkip = arcade::skipScore,
                    onRetry = vm::restart,
                )
                else -> Unit
            }
        }
        Spacer(Modifier.height(12.dp))
        LeafButton("NEW BOARD", onClick = vm::restart, filled = false, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Swipe to slide. Double matching tiles.",
            color = Dim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Tile(value: Int, modifier: Modifier) {
    val bg = tileColor(value)
    val fg = if (value <= 4) Mist else Void
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, PadStroke.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (value != 0) {
            Text(
                text = value.toString(),
                color = fg,
                fontWeight = FontWeight.Black,
                fontSize = if (value >= 1024) 22.sp else 28.sp,
            )
        }
    }
}

private fun tileColor(value: Int): Color {
    if (value == 0) return Pad
    val t = (ln(value.toFloat()) / ln(2f) / 11f).coerceIn(0f, 1f)
    return Color(
        red = Leaf.red + (Bite.red - Leaf.red) * t,
        green = Leaf.green + (0.25f - Leaf.green) * t,
        blue = Leaf.blue + (Bite.blue - Leaf.blue) * t,
    )
}

private val Bite = app.snake.Bite
