package app.snake.games.snake

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import android.os.Vibrator
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Bite
import app.snake.Dim
import app.snake.Head
import app.snake.Leaf
import app.snake.Pad
import app.snake.PadStroke
import app.snake.PitAlt
import app.snake.core.Buzz
import app.snake.core.Dir
import app.snake.core.GameId
import app.snake.core.play
import app.snake.core.swipePulse
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.HotSeatLobby
import app.snake.ui.ModePick
import app.snake.ui.PassPhoneOverlay
import app.snake.ui.PitFrame
import app.snake.ui.PitHint
import app.snake.ui.SeatResult
import kotlin.math.min

@Composable
fun SnakeGame(arcade: ArcadeViewModel, vm: SnakeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val vibrator = LocalContext.current.getSystemService<Vibrator>()

    LaunchedEffect(arcade) {
        arcade.pause.collect { vm.pauseIfRunning() }
    }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            vibrator.play(
                when (event) {
                    SnakeEvent.Eat -> Buzz.Soft
                    SnakeEvent.Die -> Buzz.Dead
                    SnakeEvent.Win -> Buzz.Win
                },
            )
        }
    }
    LaunchedEffect(state.phase, state.scoreOffered, state.hotSeat, state.recorded, state.leafScore) {
        if (state.hotSeat) {
            if (!state.recorded && state.leafScore >= 0 && (state.phase == SnakePhase.Dead || state.phase == SnakePhase.Won)) {
                val leaf = state.leafScore
                val head = state.score
                when {
                    leaf > head -> {
                        arcade.recordRun(GameId.Snake.seatKey(), state.leafName, leaf, true)
                        arcade.recordRun(GameId.Snake.seatKey(), state.headName, head, false)
                    }
                    head > leaf -> {
                        arcade.recordRun(GameId.Snake.seatKey(), state.headName, head, true)
                        arcade.recordRun(GameId.Snake.seatKey(), state.leafName, leaf, false)
                    }
                    else -> {
                        arcade.recordRun(GameId.Snake.seatKey(), state.leafName, leaf, false)
                        arcade.recordRun(GameId.Snake.seatKey(), state.headName, head, false)
                    }
                }
                vm.markScoreOffered()
            }
        } else if (!state.scoreOffered && (state.phase == SnakePhase.Dead || state.phase == SnakePhase.Won)) {
            arcade.offerScore(
                boardKey = "snake",
                score = state.score,
                lowerBetter = false,
                headline = if (state.phase == SnakePhase.Won) "Board cleared" else "You crashed",
                detail = "${state.score} berries",
            )
            vm.markScoreOffered()
        }
    }

    val pulse by rememberInfiniteTransition(label = "food").animateFloat(
        initialValue = 0.82f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(620), RepeatMode.Reverse),
        label = "foodPulse",
    )
    val best = arcade.scores.best("snake", false)?.score ?: 0

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "SNAKE",
            subtitle = when {
                state.phase == SnakePhase.Setup -> "solo or hot seat"
                state.hotSeat && state.passPending -> "pass to ${state.headName}"
                state.hotSeat -> "${state.currentName()}  ·  ${if (state.leafScore < 0) "first run" else "beat ${state.leafScore}"}"
                else -> "best $best"
            },
            value = state.score.toString().padStart(3, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == SnakePhase.Running || state.phase == SnakePhase.Paused,
            paused = state.phase == SnakePhase.Paused,
        )
        Spacer(Modifier.height(14.dp))
        val ratio = state.cols.toFloat() / state.rows.toFloat()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            PitFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
                    .swipePulse(state.phase, slop = 22f, onTap = vm::primaryAction, onSwipe = vm::turn),
            ) {
                Canvas(Modifier.fillMaxSize().padding(8.dp)) { drawSnake(state, pulse) }
                when {
                    state.phase == SnakePhase.Setup && state.gate == SnakeGate.Pick -> ModePick(
                        title = "SNAKE",
                        soloHint = "Hunt berries. Don't crash.",
                        seatHint = "Play a run, then pass. Highest berries wins.",
                        onSolo = vm::pickSolo,
                        onSeat = vm::wantSeat,
                    )
                    state.phase == SnakePhase.Setup && state.gate == SnakeGate.Seat -> HotSeatLobby(
                        title = "SNAKE",
                        leafRole = "FIRST",
                        headRole = "SECOND",
                        lastName = arcade.scores.lastName,
                        knownNames = arcade.scores.knownNames(),
                        onStart = vm::sit,
                        blurb = "Same phone. First player hunts, then you pass.",
                    )
                    state.passPending -> PassPhoneOverlay(state.headName, vm::ackPass)
                    state.phase == SnakePhase.Ready -> PitHint(if (state.hotSeat) "${state.currentName()} — swipe or tap" else "Swipe or tap to hunt")
                    state.phase == SnakePhase.Paused -> CenterNote("Paused")
                    state.hotSeat && (state.phase == SnakePhase.Dead || state.phase == SnakePhase.Won) -> SeatResult(
                        title = when {
                            state.leafScore > state.score -> "${state.leafName} takes it"
                            state.score > state.leafScore -> "${state.headName} takes it"
                            else -> "Draw"
                        },
                        detail = "${state.leafName} ${state.leafScore}  ·  ${state.headName} ${state.score}",
                        onRematch = { vm.sit(state.leafName, state.headName) },
                        onLobby = vm::lobby,
                    )
                    state.phase == SnakePhase.Dead || state.phase == SnakePhase.Won -> EndOverlay(
                        title = if (state.phase == SnakePhase.Won) "Board cleared" else "You crashed",
                        detail = "${state.score} berries",
                        prompt = prompt,
                        lastName = arcade.scores.lastName,
                        knownNames = arcade.scores.knownNames(),
                        onSave = arcade::saveScore,
                        onSkip = arcade::skipScore,
                        onRetry = vm::primaryAction,
                    )
                    state.phase == SnakePhase.Running -> Unit
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        DPad(onTurn = vm::turn)
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (state.hotSeat) "Pass after the first crash. Swipe or use the pad." else "Swipe the pit or use the pad",
            color = Dim,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DPad(onTurn: (Dir) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PadKey(Icons.Filled.KeyboardArrowUp, "Up") { onTurn(Dir.Up) }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PadKey(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left") { onTurn(Dir.Left) }
            Spacer(Modifier.size(88.dp))
            PadKey(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right") { onTurn(Dir.Right) }
        }
        PadKey(Icons.Filled.KeyboardArrowDown, "Down") { onTurn(Dir.Down) }
    }
}

@Composable
private fun PadKey(icon: ImageVector, label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(if (pressed) 0.94f else 1f)
            .clip(CircleShape)
            .background(Pad)
            .border(1.dp, if (pressed) Leaf else PadStroke, CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Leaf, modifier = Modifier.size(36.dp))
    }
}

private fun DrawScope.drawSnake(state: SnakeState, foodPulse: Float) {
    val cell = min(size.width / state.cols, size.height / state.rows)
    val gridW = cell * state.cols
    val gridH = cell * state.rows
    val origin = Offset((size.width - gridW) / 2f, (size.height - gridH) / 2f)
    val inset = cell * 0.12f
    val radius = CornerRadius(cell * 0.32f, cell * 0.32f)

    for (y in 0 until state.rows) {
        for (x in 0 until state.cols) {
            if ((x + y) % 2 == 0) {
                drawRoundRect(
                    color = PitAlt,
                    topLeft = Offset(origin.x + x * cell, origin.y + y * cell),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
        }
    }

    val foodCenter = Offset(origin.x + (state.food.x + 0.5f) * cell, origin.y + (state.food.y + 0.5f) * cell)
    drawCircle(Bite.copy(alpha = 0.22f), cell * 0.62f * foodPulse, foodCenter)
    drawCircle(Bite, cell * 0.28f * foodPulse, foodCenter)
    drawCircle(Color(0xFFFFC4C8), cell * 0.08f, foodCenter + Offset(-cell * 0.08f, -cell * 0.08f))

    state.snake.asReversed().forEachIndexed { reverseIndex, cellPos ->
        val indexFromHead = state.snake.lastIndex - reverseIndex
        val t = if (state.snake.size == 1) 1f else 1f - indexFromHead / state.snake.lastIndex.toFloat()
        val color = Color(
            Leaf.red + (Head.red - Leaf.red) * t,
            Leaf.green + (Head.green - Leaf.green) * t,
            Leaf.blue + (Head.blue - Leaf.blue) * t,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(origin.x + cellPos.x * cell + inset, origin.y + cellPos.y * cell + inset),
            size = Size(cell - inset * 2, cell - inset * 2),
            cornerRadius = radius,
        )
    }

    val head = state.snake.first()
    val headCenter = Offset(origin.x + (head.x + 0.5f) * cell, origin.y + (head.y + 0.5f) * cell)
    val eyeOffset = cell * 0.16f
    val eyeShift = when (state.dir) {
        Dir.Up -> Offset(0f, -eyeOffset)
        Dir.Down -> Offset(0f, eyeOffset)
        Dir.Left -> Offset(-eyeOffset, 0f)
        Dir.Right -> Offset(eyeOffset, 0f)
    }
    val perp = when (state.dir) {
        Dir.Up, Dir.Down -> Offset(cell * 0.13f, 0f)
        Dir.Left, Dir.Right -> Offset(0f, cell * 0.13f)
    }
    drawCircle(Color(0xFF07140C), cell * 0.07f, headCenter + eyeShift + perp)
    drawCircle(Color(0xFF07140C), cell * 0.07f, headCenter + eyeShift - perp)
}
