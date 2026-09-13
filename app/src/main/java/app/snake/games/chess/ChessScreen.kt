package app.snake.games.chess

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
import app.snake.Mist
import app.snake.Pad
import app.snake.PadStroke
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
fun ChessGame(arcade: ArcadeViewModel, vm: ChessViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val flipped = state.turn == Side.Black
    val wins = arcade.scores.best("chess", false)?.score ?: 0

    LaunchedEffect(state.phase, state.recorded, state.end) {
        if (state.phase == ChessPhase.Over && !state.recorded && state.end != null) {
            when (state.end) {
                ChessEnd.Mate -> {
                    val winner = state.turn.other
                    arcade.recordRun("chess", state.name(winner), state.material(winner).coerceAtLeast(1), true)
                }
                ChessEnd.Stale, ChessEnd.Draw -> {
                    arcade.recordRun("chess", state.whiteName, 0, false)
                    arcade.recordRun("chess", state.blackName, 0, false)
                }
                null -> Unit
            }
            vm.markRecorded()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "CHESS",
            subtitle = when {
                state.phase == ChessPhase.Setup -> "hot seat"
                state.phase == ChessPhase.Over -> endLine(state)
                state.inCheck -> "${state.name(state.turn)}  ·  check"
                else -> "${state.name(state.turn)} to move"
            },
            value = if (state.phase == ChessPhase.Setup) "2P" else state.full.toString(),
            valueLabel = if (state.phase == ChessPhase.Setup) "hot seat" else "move",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ChessBoard(state, flipped, vm::tap)
            }
            when {
                state.phase == ChessPhase.Setup -> HotSeatLobby(
                    title = "CHESS",
                    leafRole = "WHITE",
                    headRole = "BLACK",
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onStart = vm::sit,
                )
                state.passPending -> PassPhoneOverlay(state.name(state.turn), vm::ackPass)
                state.phase == ChessPhase.Promote -> PromoPicker(vm::pickPromo)
                state.phase == ChessPhase.Over -> CenterNote(endLine(state))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeafButton("UNDO", onClick = vm::undo, filled = false, modifier = Modifier.weight(1f))
            if (state.phase == ChessPhase.Over) {
                LeafButton("REMATCH", onClick = vm::rematch, modifier = Modifier.weight(1f))
            } else {
                LeafButton("NEW", onClick = vm::lobby, filled = false, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "White at the bottom after you sit. Board flips each turn. Best win $wins.",
            color = Dim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun endLine(state: ChessState): String = when (state.end) {
    ChessEnd.Mate -> "${state.name(state.turn.other)} mates"
    ChessEnd.Stale -> "Stalemate"
    ChessEnd.Draw -> "Draw"
    null -> "Over"
}

@Composable
private fun PromoPicker(onPick: (Kind) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Void.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PROMOTE", color = Leaf, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Kind.Queen, Kind.Rook, Kind.Bishop, Kind.Knight).forEach { kind ->
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Pad)
                            .border(1.dp, Leaf, RoundedCornerShape(14.dp))
                            .clickable { onPick(kind) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(glyph(Piece(Side.White, kind)), color = Head, fontSize = 28.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChessBoard(state: ChessState, flipped: Boolean, onTap: (Int) -> Unit) {
    val king = kingSq(state.squares, state.turn)
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
                    val file = if (flipped) 7 - col else col
                    val rank = if (flipped) row else 7 - row
                    val square = idx(file, rank)
                    val light = (file + rank) % 2 == 1
                    val piece = state.squares[square]
                    val selected = state.selected == square
                    val target = state.targets.any { it.to == square }
                    val last = state.last?.from == square || state.last?.to == square
                    val check = state.inCheck && square == king
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(
                                when {
                                    check -> Bite.copy(alpha = 0.55f)
                                    selected -> Leaf.copy(alpha = 0.45f)
                                    last -> Head.copy(alpha = 0.18f)
                                    light -> PitAlt
                                    else -> Pad
                                },
                            )
                            .clickable { onTap(square) },
                        contentAlignment = Alignment.Center,
                    ) {
                        piece?.let {
                            Text(
                                text = glyph(it),
                                color = if (it.side == Side.White) Head else Color(0xFF7FE0B0),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (target && piece == null) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Leaf.copy(alpha = 0.85f)),
                            )
                        } else if (target && piece != null) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .border(2.dp, Bite, RoundedCornerShape(0.dp)),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun glyph(piece: Piece): String = when (piece.kind) {
    Kind.King -> if (piece.side == Side.White) "♔" else "♚"
    Kind.Queen -> if (piece.side == Side.White) "♕" else "♛"
    Kind.Rook -> if (piece.side == Side.White) "♖" else "♜"
    Kind.Bishop -> if (piece.side == Side.White) "♗" else "♝"
    Kind.Knight -> if (piece.side == Side.White) "♘" else "♞"
    Kind.Pawn -> if (piece.side == Side.White) "♙" else "♟"
}
