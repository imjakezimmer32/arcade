package app.snake.games.mines

import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
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
import app.snake.core.Buzz
import app.snake.core.play
import app.snake.scores.ScoreStore
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.LeafButton
import app.snake.ui.PitFrame
import app.snake.ui.arcadeVerticalScrollbar
import kotlin.math.min

private val NumberInk = listOf(
    Color(0xFF7FDBFF),
    Color(0xFF3DDC84),
    Color(0xFFFF5A6A),
    Color(0xFF9AA8FF),
    Color(0xFFE6A15C),
    Color(0xFF64D8CB),
    Color(0xFFE8F5E9),
    Color(0xFF90A4AE),
)

@Composable
fun MinesGame(arcade: ArcadeViewModel, vm: MinesViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val vibrator = LocalContext.current.getSystemService<Vibrator>()
    val scroll = rememberScrollState()

    LaunchedEffect(state.status, state.scoreOffered, state.elapsedSec) {
        if (state.status == MinesStatus.Won && !state.scoreOffered) {
            vibrator.play(Buzz.Win)
            arcade.offerScore(
                boardKey = state.diff.boardKey,
                score = state.elapsedSec.coerceAtLeast(1),
                lowerBetter = true,
                headline = "Cleared",
                detail = ScoreStore.formatScore(state.elapsedSec.coerceAtLeast(1), true),
            )
            vm.markScoreOffered()
        }
        if (state.status == MinesStatus.Lost && !state.scoreOffered) {
            vibrator.play(Buzz.Dead)
            arcade.offerScore(
                boardKey = state.diff.boardKey,
                score = state.elapsedSec,
                lowerBetter = true,
                headline = "Boom",
                detail = ScoreStore.formatScore(state.elapsedSec, true),
                win = false,
            )
            vm.markScoreOffered()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "MINES",
            subtitle = "${state.diff.mines} buried",
            value = ScoreStore.formatScore(state.elapsedSec, true),
            valueLabel = if (state.remaining >= 0) "${state.remaining} left" else "${-state.remaining} over",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MinesDiff.entries.forEach { diff ->
                LeafButton(
                    text = diff.label.uppercase(),
                    onClick = { vm.setDiff(diff) },
                    filled = state.diff == diff,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LeafButton(
                text = if (state.flagMode) "FLAG" else "DIG",
                onClick = vm::toggleFlagMode,
                filled = state.flagMode,
                modifier = Modifier.weight(1f),
            )
            LeafButton(
                text = "NEW",
                onClick = vm::restart,
                filled = false,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .arcadeVerticalScrollbar(scroll)
                .verticalScroll(scroll),
            contentAlignment = Alignment.TopCenter,
        ) {
            val ratio = state.cols.toFloat() / state.rows.toFloat()
            PitFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio),
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .pointerInput(state.diff, state.flagMode, state.status) {
                            detectTapGestures(
                                onTap = { offset ->
                                    cellAt(offset, state)?.let { (x, y) -> vm.press(x, y) }
                                },
                                onLongPress = { offset ->
                                    cellAt(offset, state)?.let { (x, y) -> vm.longPress(x, y) }
                                },
                            )
                        },
                ) {
                    drawMines(state)
                }
                when (state.status) {
                    MinesStatus.Lost -> EndOverlay(
                        title = "Boom",
                        detail = "Long-press flags. Chord a number.",
                        prompt = prompt,
                        lastName = arcade.scores.lastName,
                        knownNames = arcade.scores.knownNames(),
                        onSave = arcade::saveScore,
                        onSkip = arcade::skipScore,
                        onRetry = vm::restart,
                    )
                    MinesStatus.Won -> EndOverlay(
                        title = "Cleared",
                        detail = ScoreStore.formatScore(state.elapsedSec.coerceAtLeast(1), true),
                        prompt = prompt,
                        lastName = arcade.scores.lastName,
                        knownNames = arcade.scores.knownNames(),
                        onSave = arcade::saveScore,
                        onSkip = arcade::skipScore,
                        onRetry = vm::restart,
                    )
                    else -> Unit
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap to open. Long-press to flag. Tap a number to chord.",
            color = Dim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun androidx.compose.ui.input.pointer.PointerInputScope.cellAt(
    offset: Offset,
    state: MinesState,
): Pair<Int, Int>? {
    val cell = min(size.width.toFloat() / state.cols, size.height.toFloat() / state.rows)
    val gridW = cell * state.cols
    val gridH = cell * state.rows
    val ox = (size.width - gridW) / 2f
    val oy = (size.height - gridH) / 2f
    val x = ((offset.x - ox) / cell).toInt()
    val y = ((offset.y - oy) / cell).toInt()
    if (x !in 0 until state.cols || y !in 0 until state.rows) return null
    return x to y
}

private fun DrawScope.drawMines(state: MinesState) {
    val cell = min(size.width / state.cols, size.height / state.rows)
    val origin = Offset((size.width - cell * state.cols) / 2f, (size.height - cell * state.rows) / 2f)
    val radius = CornerRadius(cell * 0.18f, cell * 0.18f)
    val inset = cell * 0.08f
    for (y in 0 until state.rows) {
        for (x in 0 until state.cols) {
            val tile = state.at(x, y)
            val tl = Offset(origin.x + x * cell + inset, origin.y + y * cell + inset)
            val sz = Size(cell - inset * 2, cell - inset * 2)
            when {
                tile.open && tile.mine -> {
                    drawRoundRect(Bite, tl, sz, radius)
                    drawCircle(Void, cell * 0.16f, tl + Offset(sz.width / 2, sz.height / 2))
                }
                tile.open -> {
                    drawRoundRect(PitAlt, tl, sz, radius)
                    if (tile.adj > 0) {
                        val paint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = NumberInk[(tile.adj - 1).coerceIn(0, 7)].toArgb()
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize = cell * 0.55f
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            tile.adj.toString(),
                            tl.x + sz.width / 2f,
                            tl.y + sz.height * 0.72f,
                            paint,
                        )
                    }
                }
                else -> {
                    drawRoundRect(Pad, tl, sz, radius)
                    drawRoundRect(PadStroke, tl, sz, radius, style = Stroke(width = 2f))
                    if (tile.mark == MinesMark.Flag) {
                        drawRoundRect(Bite, tl + Offset(sz.width * 0.28f, sz.height * 0.22f), Size(sz.width * 0.42f, sz.height * 0.38f), CornerRadius(4f))
                    } else if (tile.mark == MinesMark.Question) {
                        drawCircle(Head, cell * 0.12f, tl + Offset(sz.width / 2, sz.height / 2))
                    }
                }
            }
        }
    }
}
