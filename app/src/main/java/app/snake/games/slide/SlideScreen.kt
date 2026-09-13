package app.snake.games.slide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Leaf
import app.snake.PitAlt
import app.snake.Void
import app.snake.core.Dir
import app.snake.core.onDown
import app.snake.core.swipePulse
import app.snake.scores.ScoreStore
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.LeafButton
import app.snake.ui.PitFrame
import app.snake.ui.PitHint
import kotlin.math.abs

@Composable
fun SlideGame(arcade: ArcadeViewModel, vm: SlideViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("slide", true)
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == SlidePhase.Won) {
            arcade.offerScore("slide", state.elapsed.coerceAtLeast(1), true, "Solved", ScoreStore.formatScore(state.elapsed.coerceAtLeast(1), true))
            vm.markScoreOffered()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "SLIDE",
            subtitle = "best ${best?.let { ScoreStore.formatScore(it.score, true) } ?: "—"}  ·  ${state.moves} moves",
            value = ScoreStore.formatScore(state.elapsed, true),
            valueLabel = "time",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier.fillMaxWidth().weight(1f).swipePulse(state.phase, slop = 24f, rearm = false, onSwipe = vm::swipe),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(10.dp).aspectRatio(1f).fillMaxWidth(),
            ) {
                for (r in 0..3) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0..3) {
                            val i = r * 4 + c
                            val v = state.cells[i]
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(12.dp))
                                    .background(if (v == 0) PitAlt else Leaf)
                                    .onDown(i) { vm.tap(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (v != 0) {
                                    Text(
                                        v.toString(),
                                        color = Void,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }
                    }
                    if (r < 3) Spacer(Modifier.height(6.dp))
                }
            }
            when (state.phase) {
                SlidePhase.Ready -> PitHint("Swipe or tap a tile beside the gap")
                SlidePhase.Won -> EndOverlay(
                    "Solved",
                    ScoreStore.formatScore(state.elapsed.coerceAtLeast(1), true),
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                SlidePhase.Playing -> Unit
            }
            }
        }
        Spacer(Modifier.height(10.dp))
        LeafButton("SHUFFLE", onClick = vm::shuffle, filled = false, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Swipe the board or tap a tile next to the gap. Faster time wins.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
