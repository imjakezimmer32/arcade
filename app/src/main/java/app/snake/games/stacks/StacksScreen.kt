package app.snake.games.stacks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Pad
import app.snake.PadStroke
import app.snake.PitAlt
import app.snake.core.Dir
import app.snake.core.swipePulse
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.LeafButton
import app.snake.ui.PitFrame
import app.snake.ui.PitHint
import kotlin.math.min

private val PieceColors = listOf(
    Color.Transparent,
    Color(0xFF7FDBFF),
    Color(0xFFC8FF7A),
    Color(0xFFCE93D8),
    Color(0xFF3DDC84),
    Color(0xFFFF5A6A),
    Color(0xFF7C9CFF),
    Color(0xFFE6A15C),
)

@Composable
fun StacksGame(arcade: ArcadeViewModel, vm: StacksViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("stacks", false)?.score ?: 0

    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == StackPhase.Dead) {
            arcade.offerScore("stacks", state.score, false, "Stacked out", "${state.score} pts")
            vm.markScoreOffered()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "STACKS",
            subtitle = "best $best  ·  ${state.lines} lines",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == StackPhase.Running || state.phase == StackPhase.Paused,
            paused = state.phase == StackPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .swipePulse(
                    state.phase,
                    slop = 28f,
                    rearm = false,
                    onTap = { if (state.phase == StackPhase.Ready) vm.begin() else vm.rotate() },
                    onSwipe = { dir ->
                        when (dir) {
                            Dir.Left -> vm.shift(-1)
                            Dir.Right -> vm.shift(1)
                            Dir.Down -> vm.hardDrop()
                            Dir.Up -> vm.rotate()
                        }
                    },
                ),
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) { drawStacks(state) }
            when (state.phase) {
                StackPhase.Ready -> PitHint("Tap to rotate. Swipe to shift. Swipe down to drop.")
                StackPhase.Paused -> CenterNote("Paused")
                StackPhase.Dead -> EndOverlay(
                    title = "Stacked out",
                    detail = "${state.score} pts",
                    prompt = prompt,
                    lastName = arcade.scores.lastName,
                    knownNames = arcade.scores.knownNames(),
                    onSave = arcade::saveScore,
                    onSkip = arcade::skipScore,
                    onRetry = vm::begin,
                )
                StackPhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NEXT", color = Dim, fontSize = 10.sp, letterSpacing = 1.sp)
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .width(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Pad)
                        .border(1.dp, PadStroke, RoundedCornerShape(12.dp)),
                ) {
                    Canvas(Modifier.fillMaxSize().padding(6.dp)) { drawNext(state.nextType) }
                }
            }
            LeafButton("ROTATE", onClick = vm::rotate, filled = true, modifier = Modifier.weight(1f))
            LeafButton("DROP", onClick = vm::hardDrop, filled = false, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the pit to rotate. Swipe to shift. Swipe down to drop.",
            color = Dim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStacks(state: StackState) {
    val cell = min(size.width / StackState.WIDTH, size.height / StackState.HEIGHT)
    val origin = Offset((size.width - cell * StackState.WIDTH) / 2f, (size.height - cell * StackState.HEIGHT) / 2f)
    val inset = cell * 0.08f
    val radius = CornerRadius(cell * 0.22f, cell * 0.22f)
    fun drawCell(x: Int, y: Int, color: Color, alpha: Float = 1f) {
        if (y < 0) return
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(origin.x + x * cell + inset, origin.y + y * cell + inset),
            size = Size(cell - inset * 2, cell - inset * 2),
            cornerRadius = radius,
        )
    }
    for (y in 0 until StackState.HEIGHT) {
        for (x in 0 until StackState.WIDTH) {
            val id = state.cells[y * StackState.WIDTH + x]
            if (id == 0) drawCell(x, y, PitAlt, 0.55f)
            else drawCell(x, y, PieceColors[id])
        }
    }
    state.active?.let { piece ->
        val gy = ghostY(state)
        val tet = TETROMINOES[piece.type - 1]
        val cells = tet.rotations[piece.rot % tet.rotations.size]
        cells.forEach { (cx, cy) ->
            drawCell(piece.x + cx, gy + cy, PieceColors[piece.type], 0.22f)
            drawCell(piece.x + cx, piece.y + cy, PieceColors[piece.type])
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNext(type: Int) {
    val tet = TETROMINOES[type - 1]
    val cells = tet.rotations.first()
    val minX = cells.minOf { it.first }
    val minY = cells.minOf { it.second }
    val w = cells.maxOf { it.first } - minX + 1
    val h = cells.maxOf { it.second } - minY + 1
    val cell = min(size.width / 4f, size.height / 4f)
    val ox = (size.width - w * cell) / 2f
    val oy = (size.height - h * cell) / 2f
    cells.forEach { (x, y) ->
        drawRoundRect(
            color = PieceColors[type],
            topLeft = Offset(ox + (x - minX) * cell + 3f, oy + (y - minY) * cell + 3f),
            size = Size(cell - 6f, cell - 6f),
            cornerRadius = CornerRadius(8f, 8f),
        )
    }
}
