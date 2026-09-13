package app.snake.games.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import app.snake.Head
import app.snake.Leaf
import app.snake.Pad
import app.snake.PadStroke
import app.snake.core.GameId
import app.snake.core.onDown
import app.snake.scores.ScoreStore
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.HotSeatLobby
import app.snake.ui.ModePick
import app.snake.ui.PitFrame
import app.snake.ui.SeatResult

private val Faces = listOf(
    Color(0xFF3DDC84), Color(0xFFC8FF7A), Color(0xFFFF5A6A), Color(0xFF7FDBFF),
    Color(0xFFE6A15C), Color(0xFFCE93D8), Color(0xFF9AA8FF), Color(0xFFFFC4C8),
)
private val Glyphs = listOf("●", "★", "✿", "☽", "◆", "♠", "♥", "☀")

@Composable
fun MemoryGame(arcade: ArcadeViewModel, vm: MemoryViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("memory", true)

    LaunchedEffect(state.phase, state.recorded, state.hotSeat) {
        if (state.phase != MemPhase.Won || state.recorded) return@LaunchedEffect
        if (state.hotSeat) {
            when {
                state.leafPairs > state.headPairs -> {
                    arcade.recordRun(GameId.Memory.seatKey(), state.leafName, state.leafPairs, true)
                    arcade.recordRun(GameId.Memory.seatKey(), state.headName, state.headPairs, false)
                }
                state.headPairs > state.leafPairs -> {
                    arcade.recordRun(GameId.Memory.seatKey(), state.headName, state.headPairs, true)
                    arcade.recordRun(GameId.Memory.seatKey(), state.leafName, state.leafPairs, false)
                }
                else -> {
                    arcade.recordRun(GameId.Memory.seatKey(), state.leafName, state.leafPairs, false)
                    arcade.recordRun(GameId.Memory.seatKey(), state.headName, state.headPairs, false)
                }
            }
            vm.markScoreOffered()
        } else if (!state.scoreOffered) {
            arcade.offerScore("memory", state.elapsed.coerceAtLeast(1), true, "Matched", ScoreStore.formatScore(state.elapsed.coerceAtLeast(1), true))
            vm.markScoreOffered()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "MEMORY",
            subtitle = when {
                state.phase == MemPhase.Setup -> "solo or hot seat"
                state.hotSeat && state.phase != MemPhase.Won -> "${state.currentName()}  ·  match to keep the turn"
                state.hotSeat -> "${state.leafName} ${state.leafPairs}  ·  ${state.headName} ${state.headPairs}"
                else -> "best ${best?.let { ScoreStore.formatScore(it.score, true) } ?: "—"}"
            },
            value = if (state.hotSeat) "${state.leafPairs}–${state.headPairs}" else ScoreStore.formatScore(state.elapsed, true),
            valueLabel = if (state.hotSeat) "pairs" else "${state.misses} misses",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in 0 until 4) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0 until 4) {
                            val i = row * 4 + col
                            val tile = state.tiles[i]
                            val face = tile.open || tile.matched
                            val turnColor = if (state.hotSeat && state.leafTurn) Leaf else Head
                            Box(
                                Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(12.dp))
                                    .background(if (face) Faces[tile.pair] else Pad)
                                    .border(1.dp, if (state.hotSeat && !face) turnColor.copy(alpha = 0.45f) else PadStroke, RoundedCornerShape(12.dp))
                                    .onDown(i) { vm.flip(i) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (face) Text(Glyphs[tile.pair], color = app.snake.Void, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
            when {
                state.phase == MemPhase.Setup && state.gate == MemGate.Pick -> ModePick(
                    title = "MEMORY",
                    soloHint = "Beat the clock. Faster time is better.",
                    seatHint = "Take turns. A match keeps the turn.",
                    onSolo = vm::pickSolo,
                    onSeat = vm::wantSeat,
                )
                state.phase == MemPhase.Setup && state.gate == MemGate.Seat -> HotSeatLobby(
                    title = "MEMORY",
                    leafRole = "FIRST",
                    headRole = "SECOND",
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onStart = vm::sit,
                    blurb = "Same phone. Match a pair and you go again.",
                )
                state.phase == MemPhase.Won && state.hotSeat -> SeatResult(
                    title = when {
                        state.leafPairs > state.headPairs -> "${state.leafName} takes it"
                        state.headPairs > state.leafPairs -> "${state.headName} takes it"
                        else -> "Draw"
                    },
                    detail = "${state.leafName} ${state.leafPairs}  ·  ${state.headName} ${state.headPairs}",
                    onRematch = vm::restart,
                    onLobby = vm::lobby,
                )
                state.phase == MemPhase.Won -> EndOverlay(
                    "Matched",
                    ScoreStore.formatScore(state.elapsed.coerceAtLeast(1), true),
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::restart,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.hotSeat) "Flip two. A match keeps your turn." else "Flip two. Match every pair.",
            color = Dim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
