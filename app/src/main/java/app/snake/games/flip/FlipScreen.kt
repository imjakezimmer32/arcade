package app.snake.games.flip

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
import app.snake.Pad
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
fun FlipGame(arcade: ArcadeViewModel, vm: FlipViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val legal = if (state.phase == FlipPhase.Playing) state.legal(state.leafTurn) else emptyList()
    LaunchedEffect(state.phase, state.recorded) {
        if (state.phase == FlipPhase.Over && !state.recorded) {
            val leaf = state.count(true)
            val head = state.count(false)
            when {
                leaf > head -> {
                    arcade.recordRun(GameId.Flip.boardKey, state.leafName, leaf, true)
                    arcade.recordRun(GameId.Flip.boardKey, state.headName, head, false)
                }
                head > leaf -> {
                    arcade.recordRun(GameId.Flip.boardKey, state.headName, head, true)
                    arcade.recordRun(GameId.Flip.boardKey, state.leafName, leaf, false)
                }
                else -> {
                    arcade.recordRun(GameId.Flip.boardKey, state.leafName, leaf, false)
                    arcade.recordRun(GameId.Flip.boardKey, state.headName, head, false)
                }
            }
            vm.markRecorded()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "FLIP",
            subtitle = when {
                state.phase == FlipPhase.Setup -> "hot seat"
                state.phase == FlipPhase.Over -> "board full"
                state.skipped -> "${state.currentName()} plays again — no move"
                else -> "${state.currentName()} to flip"
            },
            value = "${state.count(true)}–${state.count(false)}",
            valueLabel = "discs",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(8.dp).aspectRatio(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                for (r in 0..7) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0..7) {
                            val i = r * 8 + c
                            val v = state.cells[i]
                            val can = i in legal
                            Box(
                                Modifier.weight(1f).fillMaxSize()
                                    .background(if ((c + r) % 2 == 0) Pad else PitAlt)
                                    .onDown(i) { if (can) vm.play(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    v != 0 -> Box(
                                        Modifier.fillMaxSize(0.72f).clip(CircleShape)
                                            .background(if (v == 1) Leaf else Head)
                                            .then(
                                                when {
                                                    i == state.last -> Modifier.border(2.dp, Mist, CircleShape)
                                                    i in state.flipped -> Modifier.border(1.5.dp, Mist.copy(alpha = 0.7f), CircleShape)
                                                    else -> Modifier
                                                },
                                            ),
                                    )
                                    can -> Box(
                                        Modifier.fillMaxSize(0.22f).clip(CircleShape)
                                            .background((if (state.leafTurn) Leaf else Head).copy(alpha = 0.55f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            when {
                state.phase == FlipPhase.Setup -> HotSeatLobby(
                    title = "FLIP",
                    leafRole = "GREEN",
                    headRole = "LIME",
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onStart = vm::sit,
                    blurb = "Reversi. Trap a line, flip them. Most discs wins.",
                )
                state.passPending -> PassPhoneOverlay(state.currentName(), vm::ackPass)
                state.phase == FlipPhase.Over -> SeatResult(
                    title = when {
                        state.count(true) > state.count(false) -> "${state.leafName} takes it"
                        state.count(false) > state.count(true) -> "${state.headName} takes it"
                        else -> "Draw"
                    },
                    detail = "${state.leafName} ${state.count(true)}  ·  ${state.headName} ${state.count(false)}",
                    onRematch = vm::rematch,
                    onLobby = vm::lobby,
                )
            }
            }
        }
        Spacer(Modifier.height(10.dp))
        LeafButton("NEW", onClick = vm::lobby, filled = false, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("Dots mark legal drops.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
