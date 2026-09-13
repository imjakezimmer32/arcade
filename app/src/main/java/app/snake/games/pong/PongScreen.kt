package app.snake.games.pong

import android.os.Vibrator
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Head
import app.snake.Leaf
import app.snake.Mist
import app.snake.core.Buzz
import app.snake.core.GameId
import app.snake.core.followX
import app.snake.core.play
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.HotSeatLobby
import app.snake.ui.ModePick
import app.snake.ui.PitFrame
import app.snake.ui.PitHint
import app.snake.ui.SeatResult

@Composable
fun PongGame(arcade: ArcadeViewModel, vm: PongViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("pong", false)?.score ?: 0
    val vibrator = LocalContext.current.getSystemService<Vibrator>()

    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            vibrator.play(
                when (event) {
                    PongEvent.Paddle -> Buzz.Soft
                    PongEvent.Score -> Buzz.Hit
                    PongEvent.Miss -> Buzz.Dead
                    PongEvent.Dead -> Buzz.Dead
                },
            )
        }
    }
    LaunchedEffect(state.phase, state.recorded, state.hotSeat) {
        if (state.phase != PongPhase.Dead || state.recorded) return@LaunchedEffect
        if (state.hotSeat) {
            val leafWins = state.leafPts > state.headPts
            val winner = if (leafWins) state.leafName else state.headName
            val pts = maxOf(state.leafPts, state.headPts)
            arcade.recordRun(GameId.Pong.seatKey(), winner, pts, true)
            val loser = if (leafWins) state.headName else state.leafName
            val losePts = minOf(state.leafPts, state.headPts)
            arcade.recordRun(GameId.Pong.seatKey(), loser, losePts, false)
            vm.markScoreOffered()
        } else if (!state.scoreOffered) {
            arcade.offerScore("pong", state.score, false, "Rally over", "${state.score} pts")
            vm.markScoreOffered()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "PONG",
            subtitle = when {
                state.phase == PongPhase.Setup -> "solo or hot seat"
                state.hotSeat -> "${state.leafName}  ${state.leafPts}  ·  ${state.headName}  ${state.headPts}  ·  first to ${PongState.TO_WIN}"
                else -> "best $best  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}"
            },
            value = if (state.hotSeat) "${state.leafPts}–${state.headPts}" else state.score.toString().padStart(3, '0'),
            valueLabel = if (state.hotSeat) "match" else "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == PongPhase.Running || state.phase == PongPhase.Paused,
            paused = state.phase == PongPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(
                    if (state.hotSeat && state.phase != PongPhase.Setup) {
                        Modifier.pointerInput(state.phase) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        if (!change.pressed) return@forEach
                                        val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                        if (change.position.y < size.height * 0.5f) vm.moveHead(nx) else vm.moveLeaf(nx)
                                        change.consume()
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier.followX(state.phase, vm::moveLeaf)
                    },
                ),
        ) {
            Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                val w = size.width
                val h = size.height
                val pw = w * PongState.PAD
                val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 12f), 0f)
                drawLine(Mist.copy(alpha = 0.18f), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 2f, pathEffect = dash)
                drawRoundRect(Head, Offset(state.cpu * w - pw / 2, h * PongState.HEAD_Y), Size(pw, h * PongState.PAD_H), CornerRadius(7f))
                drawRoundRect(Leaf, Offset(state.px * w - pw / 2, h * PongState.LEAF_Y), Size(pw, h * PongState.PAD_H), CornerRadius(8f))
                val glow = if (state.serve > 0f) 0.45f else 1f
                drawCircle(Mist.copy(alpha = 0.25f * glow), w * 0.038f, Offset(state.bx * w, state.by * h))
                drawCircle(Mist, w * PongState.BALL, Offset(state.bx * w, state.by * h))
            }
            when {
                state.phase == PongPhase.Setup && state.gate == PongGate.Pick -> ModePick(
                    title = "PONG",
                    soloHint = "You on the leaf paddle. CPU up top.",
                    seatHint = "Two paddles. First to ${PongState.TO_WIN}.",
                    onSolo = vm::pickSolo,
                    onSeat = vm::wantSeat,
                )
                state.phase == PongPhase.Setup && state.gate == PongGate.Seat -> HotSeatLobby(
                    title = "PONG",
                    leafRole = "BOTTOM",
                    headRole = "TOP",
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onStart = vm::sit,
                    blurb = "Same phone. Leaf sits bottom, Head sits top. Both play at once.",
                )
                state.phase == PongPhase.Ready -> PitHint(if (state.hotSeat) "Slide your half" else "Slide to serve")
                state.phase == PongPhase.Paused -> CenterNote("Paused")
                state.phase == PongPhase.Dead && state.hotSeat -> SeatResult(
                    title = if (state.leafPts > state.headPts) "${state.leafName} takes it" else "${state.headName} takes it",
                    detail = "${state.leafName} ${state.leafPts}  ·  ${state.headName} ${state.headPts}",
                    onRematch = { vm.sit(state.leafName, state.headName) },
                    onLobby = vm::lobby,
                )
                state.phase == PongPhase.Dead -> EndOverlay(
                    "Rally over",
                    "${state.score} pts",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                state.phase == PongPhase.Running && state.serve > 0.15f -> PitHint("Serve")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                state.hotSeat -> "Top half is Head. Bottom half is Leaf."
                else -> "Your paddle is the leaf one. CPU sits up top."
            },
            color = Dim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
