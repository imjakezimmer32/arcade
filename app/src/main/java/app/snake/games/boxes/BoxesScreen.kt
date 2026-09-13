package app.snake.games.boxes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import app.snake.Pad
import app.snake.PadStroke
import app.snake.core.GameId
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.GameTopBar
import app.snake.ui.HotSeatLobby
import app.snake.ui.LeafButton
import app.snake.ui.PassPhoneOverlay
import app.snake.ui.PitFrame
import app.snake.ui.SeatResult

@Composable
fun BoxesGame(arcade: ArcadeViewModel, vm: BoxesViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.phase, state.recorded) {
        if (state.phase == BoxesPhase.Over && !state.recorded) {
            val leaf = state.count(true)
            val head = state.count(false)
            when {
                leaf > head -> {
                    arcade.recordRun(GameId.Boxes.boardKey, state.leafName, leaf, true)
                    arcade.recordRun(GameId.Boxes.boardKey, state.headName, head, false)
                }
                head > leaf -> {
                    arcade.recordRun(GameId.Boxes.boardKey, state.headName, head, true)
                    arcade.recordRun(GameId.Boxes.boardKey, state.leafName, leaf, false)
                }
                else -> {
                    arcade.recordRun(GameId.Boxes.boardKey, state.leafName, leaf, false)
                    arcade.recordRun(GameId.Boxes.boardKey, state.headName, head, false)
                }
            }
            vm.markRecorded()
        }
    }
    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "BOXES",
            subtitle = when (state.phase) {
                BoxesPhase.Setup -> "hot seat"
                BoxesPhase.Over -> "grid full"
                else -> "${state.currentName()} draws a line"
            },
            value = "${state.count(true)}–${state.count(false)}",
            valueLabel = "boxes",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(10.dp).aspectRatio(1f).fillMaxWidth()) {
                    for (r in 0..4) {
                        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                            Dot()
                            for (c in 0..3) {
                                val i = r * 4 + c
                                val on = state.h[i]
                                Box(
                                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                i == state.lastH -> Head
                                                on -> Leaf
                                                else -> PadStroke
                                            },
                                        )
                                        .clickable(enabled = !on && state.phase == BoxesPhase.Playing) { vm.claimH(i) },
                                )
                                Dot()
                            }
                        }
                        if (r < 4) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                for (c in 0..4) {
                                    val i = r * 5 + c
                                    val on = state.v[i]
                                    Box(
                                        Modifier.width(36.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    i == state.lastV -> Head
                                                    on -> Leaf
                                                    else -> PadStroke
                                                },
                                            )
                                            .clickable(enabled = !on && state.phase == BoxesPhase.Playing) { vm.claimV(i) },
                                    )
                                    if (c < 4) {
                                        val own = state.owner[r * 4 + c]
                                        Box(
                                            Modifier.weight(1f).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when (own) {
                                                        1 -> Leaf.copy(alpha = 0.45f)
                                                        2 -> Head.copy(alpha = 0.45f)
                                                        else -> Pad
                                                    },
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                when {
                    state.phase == BoxesPhase.Setup -> HotSeatLobby(
                        title = "BOXES",
                        leafRole = "GREEN",
                        headRole = "LIME",
                        lastName = arcade.scores.lastName,
                        knownNames = arcade.scores.knownNames(),
                        onStart = vm::sit,
                        blurb = "Draw a line. Close a box, go again. Most boxes wins.",
                    )
                    state.passPending -> PassPhoneOverlay(state.currentName(), vm::ackPass)
                    state.phase == BoxesPhase.Over -> SeatResult(
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
        Text("Close a square to keep the turn.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Dot() {
    Box(Modifier.size(18.dp).clip(CircleShape).background(Leaf))
}
