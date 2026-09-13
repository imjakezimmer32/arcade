package app.snake.games.breakout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import app.snake.Mist
import app.snake.Pad
import app.snake.core.followX
import app.snake.core.onDown
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

private val BrickColors = listOf(
    Color(0xFFFF5A6A),
    Color(0xFFE6A15C),
    Color(0xFFC8FF7A),
    Color(0xFF3DDC84),
    Color(0xFF7FDBFF),
    Color(0xFF9AA8FF),
)

@Composable
fun BreakoutGame(arcade: ArcadeViewModel, vm: BreakoutViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("breakout", false)?.score ?: 0

    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == BreakPhase.Dead) {
            arcade.offerScore(
                boardKey = "breakout",
                score = state.score,
                lowerBetter = false,
                headline = "Ball down",
                detail = "${state.score} pts",
            )
            vm.markScoreOffered()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "BREAKOUT",
            subtitle = "best $best  ·  wave ${state.wave}  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == BreakPhase.Running || state.phase == BreakPhase.Paused,
            paused = state.phase == BreakPhase.Paused,
        )
        Spacer(Modifier.height(14.dp))
        PitFrame(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .followX(state.phase) { vm.movePaddle(it) }
                .onDown(state.phase) { vm.begin() },
        ) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val w = size.width
                val h = size.height
                val bw = w / BreakState.COLS
                val bh = h * BreakState.BRICK_H
                val top = h * BreakState.BRICK_TOP
                state.bricks.forEach { brick ->
                    if (!brick.alive) return@forEach
                    drawRoundRect(
                        color = BrickColors[brick.row % BrickColors.size],
                        topLeft = Offset(brick.col * bw + 3f, top + brick.row * bh),
                        size = Size(bw - 6f, bh * BreakState.BRICK_BODY),
                        cornerRadius = CornerRadius(6f, 6f),
                    )
                }
                val paddleW = w * BreakState.PADDLE_W
                val paddleX = state.paddleX * w - paddleW / 2f
                drawRoundRect(
                    color = Leaf,
                    topLeft = Offset(paddleX, h * BreakState.PADDLE_Y),
                    size = Size(paddleW, h * BreakState.PADDLE_H),
                    cornerRadius = CornerRadius(12f, 12f),
                )
                drawOval(
                    color = Head,
                    topLeft = Offset(state.ballX * w - w * BreakState.BALL_RX, state.ballY * h - h * BreakState.BALL_RY),
                    size = Size(w * BreakState.BALL_RX * 2f, h * BreakState.BALL_RY * 2f),
                )
            }
            when (state.phase) {
                BreakPhase.Ready -> PitHint("Slide the paddle. Tap to serve.")
                BreakPhase.Paused -> CenterNote("Paused")
                BreakPhase.Dead -> EndOverlay(
                    title = "Ball down",
                    detail = "${state.score} pts",
                    prompt = prompt,
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onSave = arcade::saveScore,
                    onSkip = arcade::skipScore,
                    onRetry = vm::begin,
                )
                BreakPhase.Running -> if (state.stuck) PitHint("Tap to launch")
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Slide the paddle. Waves keep coming after a clear.",
            color = Dim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

