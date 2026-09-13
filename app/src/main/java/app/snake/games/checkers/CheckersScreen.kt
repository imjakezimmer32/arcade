package app.snake.games.checkers

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import app.snake.Mist
import app.snake.Pad
import app.snake.PitAlt
import app.snake.Void
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.GameTopBar
import app.snake.ui.HotSeatLobby
import app.snake.ui.LeafButton
import app.snake.ui.PassPhoneOverlay
import app.snake.ui.PitFrame

@Composable
fun CheckersGame(arcade: ArcadeViewModel, vm: CheckersViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val flipped = !state.leafTurn
    val mustJump = state.mustCapture()

    LaunchedEffect(state.phase, state.recorded, state.winnerLeaf) {
        if (state.phase == CkPhase.Over && !state.recorded) {
            val winner = state.winnerLeaf
            if (winner != null) {
                arcade.recordRun("checkers", state.name(winner), state.pieceScore(winner).coerceAtLeast(1), true)
            } else {
                arcade.recordRun("checkers", state.leafName, 0, false)
                arcade.recordRun("checkers", state.headName, 0, false)
            }
            vm.markRecorded()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "CHECKERS",
            subtitle = when {
                state.phase == CkPhase.Setup -> "hot seat"
                state.phase == CkPhase.Over -> "${state.name(state.winnerLeaf ?: state.leafTurn)} wins"
                state.chain != null -> "${state.currentName()}  ·  keep jumping"
                mustJump && state.selected != null -> "${state.currentName()}  ·  jump"
                else -> "${state.currentName()} to move"
            },
            value = "${state.pieceScore(true)}–${state.pieceScore(false)}",
            valueLabel = "men",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CheckersBoard(state, flipped, vm::tap)
            }
            when {
                state.phase == CkPhase.Setup -> HotSeatLobby(
                    title = "CHECKERS",
                    leafRole = "GREEN",
                    headRole = "LIME",
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onStart = vm::sit,
                )
                state.passPending -> PassPhoneOverlay(state.currentName(), vm::ackPass)
                state.phase == CkPhase.Over -> CenterNote("${state.name(state.winnerLeaf ?: true)} takes the board")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeafButton("UNDO", onClick = vm::undo, filled = false, modifier = Modifier.weight(1f))
            if (state.phase == CkPhase.Over) {
                LeafButton("REMATCH", onClick = vm::rematch, modifier = Modifier.weight(1f))
            } else {
                LeafButton("NEW", onClick = vm::lobby, filled = false, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Jumps are forced. Kings move both ways. Board flips each turn.",
            color = Dim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CheckersBoard(state: CkState, flipped: Boolean, onTap: (Int) -> Unit) {
    Column(
        Modifier
            .padding(10.dp)
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
    ) {
        for (row in 0..7) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0..7) {
                    val x = if (flipped) 7 - col else col
                    val y = if (flipped) row else 7 - row
                    val square = sq(x, y)
                    val dark = playable(x, y)
                    val man = state.board[square]
                    val selected = state.selected == square
                    val target = state.targets.any { it.to == square }
                    val last = state.last?.from == square || state.last?.to == square || state.last?.over == square
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                when {
                                    selected -> Leaf.copy(alpha = 0.5f)
                                    last -> Head.copy(alpha = 0.16f)
                                    dark -> Pad
                                    else -> PitAlt
                                },
                            )
                            .clickable(enabled = dark) { onTap(square) },
                        contentAlignment = Alignment.Center,
                    ) {
                        man?.let { piece ->
                            Box(
                                Modifier
                                    .fillMaxSize(0.72f)
                                    .clip(CircleShape)
                                    .background(if (piece.leaf) Leaf else Head)
                                    .then(
                                        if (piece.king) Modifier.border(2.dp, Void, CircleShape)
                                        else Modifier,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (piece.king) {
                                    Text("K", color = Void, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        if (target) {
                            Box(
                                Modifier
                                    .size(if (man == null) 12.dp else 0.dp)
                                    .clip(CircleShape)
                                    .background(if (state.targets.any { it.to == square && it.over >= 0 }) Bite else Mist.copy(alpha = 0.9f)),
                            )
                            if (man != null) {
                                Box(Modifier.matchParentSize().border(2.dp, Bite, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
