package app.snake.games.four

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Head
import app.snake.Leaf
import app.snake.Mist
import app.snake.PitAlt
import app.snake.core.GameId
import app.snake.core.onDown
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.GameTopBar
import app.snake.ui.HotSeatLobby
import app.snake.ui.LeafButton
import app.snake.ui.PassPhoneOverlay
import app.snake.ui.PitFrame
import app.snake.ui.SeatResult

@Composable
fun FourGame(arcade: ArcadeViewModel, vm: FourViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.phase, state.recorded) {
        if (state.phase == FourPhase.Over && !state.recorded) {
            when (state.winner) {
                1 -> arcade.recordRun(GameId.Four.boardKey, state.leafName, 1, true)
                2 -> arcade.recordRun(GameId.Four.boardKey, state.headName, 1, true)
                else -> {
                    arcade.recordRun(GameId.Four.boardKey, state.leafName, 0, false)
                    arcade.recordRun(GameId.Four.boardKey, state.headName, 0, false)
                }
            }
            vm.markRecorded()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "FOUR",
            subtitle = when (state.phase) {
                FourPhase.Setup -> "hot seat"
                FourPhase.Over -> when (state.winner) {
                    1 -> "${state.leafName} connects"
                    2 -> "${state.headName} connects"
                    else -> "Draw"
                }
                else -> "${state.currentName()} to drop"
            },
            value = if (state.phase == FourPhase.Setup) "2P" else if (state.leafTurn) "L" else "H",
            valueLabel = "turn",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until FourState.ROWS) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until FourState.COLS) {
                            val i = r * FourState.COLS + c
                            val v = state.cells[i]
                            val win = i in state.winLine
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    .background(if (win) Leaf.copy(alpha = 0.22f) else PitAlt)
                                    .onDown(c) { vm.drop(c) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (v != 0) {
                                    Box(
                                        Modifier.fillMaxSize(0.82f).clip(CircleShape)
                                            .background(if (v == 1) Leaf else Head)
                                            .then(
                                                when {
                                                    win -> Modifier.border(3.dp, Mist, CircleShape)
                                                    state.last == i -> Modifier.border(2.dp, Mist, CircleShape)
                                                    else -> Modifier
                                                },
                                            ),
                                    )
                                } else if (r == 0) {
                                    Box(
                                        Modifier.fillMaxSize(0.28f).clip(CircleShape)
                                            .background((if (state.leafTurn) Leaf else Head).copy(alpha = 0.18f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            when {
                state.phase == FourPhase.Setup -> HotSeatLobby(
                    title = "FOUR",
                    leafRole = "GREEN",
                    headRole = "LIME",
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onStart = vm::sit,
                    blurb = "Drop a disc. Four in a row wins. Pass the phone.",
                )
                state.passPending -> PassPhoneOverlay(state.currentName(), vm::ackPass)
                state.phase == FourPhase.Over -> SeatResult(
                    title = when (state.winner) {
                        1 -> "${state.leafName} connects"
                        2 -> "${state.headName} connects"
                        else -> "Draw"
                    },
                    detail = "Four in a row",
                    onRematch = vm::rematch,
                    onLobby = vm::lobby,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeafButton("NEW", onClick = vm::lobby, filled = false, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Text("Tap a column. Gravity does the rest.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
