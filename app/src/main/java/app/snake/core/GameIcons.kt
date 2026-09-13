package app.snake.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.snake.Bite
import app.snake.Head
import app.snake.Leaf
import app.snake.Pad
import app.snake.PadStroke
import app.snake.Sky
import app.snake.core.drawBeetle
import app.snake.core.drawBerry
import app.snake.core.drawBird
import app.snake.core.drawCraft
import app.snake.core.drawCrab
import app.snake.core.drawFrog
import app.snake.core.drawSprout
import app.snake.core.drawStone
import app.snake.Void

@Composable
fun GameAppIcon(
    game: GameId,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Pad)
            .border(1.5.dp, if (selected) Leaf else PadStroke, RoundedCornerShape(22.dp))
            .padding(10.dp)
            .fillMaxSize(),
    ) {
        drawGameGlyph(game)
    }
}

@Composable
fun ScoresAppIcon(modifier: Modifier = Modifier, selected: Boolean = false) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Pad)
            .border(1.5.dp, if (selected) Leaf else PadStroke, RoundedCornerShape(22.dp))
            .padding(10.dp)
            .fillMaxSize(),
    ) {
        drawTrophy()
    }
}

private fun DrawScope.drawGameGlyph(game: GameId) {
    when (game) {
        GameId.Snake -> drawSnakeIcon()
        GameId.Mines -> drawMinesIcon()
        GameId.Breakout -> drawBreakoutIcon()
        GameId.Stacks -> drawStacksIcon()
        GameId.Merge -> drawMergeIcon()
        GameId.Pong -> drawPongIcon()
        GameId.Invaders -> drawInvadersIcon()
        GameId.Flit -> drawFlitIcon()
        GameId.Memory -> drawMemoryIcon()
        GameId.Dodge -> drawDodgeIcon()
        GameId.Checkers -> drawCheckersIcon()
        GameId.Chess -> drawChessIcon()
        GameId.Four -> drawFourIcon()
        GameId.Flip -> drawFlipIcon()
        GameId.Hop -> drawHopIcon()
        GameId.Echo -> drawEchoIcon()
        GameId.Rocks -> drawRocksIcon()
        GameId.Peck -> drawPeckIcon()
        GameId.Catch -> drawCatchIcon()
        GameId.Slide -> drawSlideIcon()
        GameId.Boxes -> drawBoxesIcon()
    }
}

private fun DrawScope.drawSnakeIcon() {
    val s = size.minDimension
    drawBeadish(Offset(s * 0.28f, s * 0.62f), s * 0.14f, Leaf)
    drawBeadish(Offset(s * 0.48f, s * 0.58f), s * 0.15f, Leaf)
    drawBeadish(Offset(s * 0.66f, s * 0.42f), s * 0.16f, Head)
    drawBerry(Offset(s * 0.72f, s * 0.22f), s * 0.1f)
}

private fun DrawScope.drawBeadish(center: Offset, radius: Float, color: Color) {
    drawCircle(color, radius, center)
}

private fun DrawScope.drawMinesIcon() {
    val s = size.minDimension
    drawRoundRect(Color(0xFF1C3328), Offset(s * 0.12f, s * 0.12f), Size(s * 0.76f, s * 0.76f), CornerRadius(8f))
    drawCircle(Bite, s * 0.16f, Offset(s * 0.5f, s * 0.5f))
    drawLine(Head, Offset(s * 0.5f, s * 0.22f), Offset(s * 0.5f, s * 0.38f), strokeWidth = 5f)
    drawLine(Head, Offset(s * 0.5f, s * 0.62f), Offset(s * 0.5f, s * 0.78f), strokeWidth = 5f)
    drawLine(Head, Offset(s * 0.22f, s * 0.5f), Offset(s * 0.38f, s * 0.5f), strokeWidth = 5f)
    drawLine(Head, Offset(s * 0.62f, s * 0.5f), Offset(s * 0.78f, s * 0.5f), strokeWidth = 5f)
}

private fun DrawScope.drawBreakoutIcon() {
    val w = size.width
    val h = size.height
    val bw = w / 4f
    for (r in 0..2) for (c in 0..3) {
        val colors = listOf(Bite, Head, Leaf)
        drawRoundRect(
            colors[r],
            Offset(c * bw + 3f, r * 14f + 6f),
            Size(bw - 6f, 10f),
            CornerRadius(3f),
        )
    }
    drawRoundRect(Leaf, Offset(w * 0.28f, h * 0.78f), Size(w * 0.44f, 8f), CornerRadius(4f))
    drawCircle(Head, 7f, Offset(w * 0.55f, h * 0.62f))
}

private fun DrawScope.drawStacksIcon() {
    val cell = size.minDimension / 5.2f
    val blocks = listOf(1 to 3, 2 to 3, 2 to 2, 3 to 3, 1 to 4, 2 to 4, 3 to 4, 3 to 2)
    blocks.forEach { (x, y) ->
        drawRoundRect(Leaf, Offset(x * cell, y * cell * 0.85f), Size(cell * 0.9f, cell * 0.9f), CornerRadius(4f))
    }
}

private fun DrawScope.drawMergeIcon() {
    val s = size.minDimension
    val tile = s * 0.38f
    drawRoundRect(PadStroke, Offset(s * 0.08f, s * 0.08f), Size(tile, tile), CornerRadius(8f))
    drawRoundRect(Leaf, Offset(s * 0.52f, s * 0.08f), Size(tile, tile), CornerRadius(8f))
    drawRoundRect(Head, Offset(s * 0.08f, s * 0.52f), Size(tile, tile), CornerRadius(8f))
    drawRoundRect(Bite, Offset(s * 0.52f, s * 0.52f), Size(tile, tile), CornerRadius(8f))
}

private fun DrawScope.drawPongIcon() {
    val w = size.width
    val h = size.height
    drawRoundRect(Leaf, Offset(8f, h * 0.72f), Size(w * 0.42f, 8f), CornerRadius(4f))
    drawRoundRect(Head, Offset(w * 0.48f, h * 0.18f), Size(w * 0.42f, 8f), CornerRadius(4f))
    drawCircle(Mist, 7f, Offset(w * 0.55f, h * 0.48f))
}

private val Mist = app.snake.Mist

private fun DrawScope.drawInvadersIcon() {
    val s = size.minDimension
    drawCrab(Offset(s * 0.32f, s * 0.32f), s * 0.14f, Leaf)
    drawCrab(Offset(s * 0.68f, s * 0.32f), s * 0.14f, Head)
    drawCrab(Offset(s * 0.5f, s * 0.58f), s * 0.16f, Sky)
    drawBeetle(Offset(s * 0.5f, s * 0.86f), s * 0.16f)
}

private fun DrawScope.drawFlitIcon() {
    val s = size.minDimension
    drawRoundRect(Leaf, Offset(s * 0.08f, 4f), Size(s * 0.22f, s * 0.42f), CornerRadius(4f))
    drawRoundRect(Leaf, Offset(s * 0.08f, s * 0.68f), Size(s * 0.22f, s * 0.28f), CornerRadius(4f))
    drawRoundRect(Leaf, Offset(s * 0.7f, 4f), Size(s * 0.22f, s * 0.28f), CornerRadius(4f))
    drawRoundRect(Leaf, Offset(s * 0.7f, s * 0.52f), Size(s * 0.22f, s * 0.44f), CornerRadius(4f))
    drawBird(Offset(s * 0.5f, s * 0.48f), s * 0.16f)
}

private fun DrawScope.drawMemoryIcon() {
    val s = size.minDimension
    val gap = s * 0.08f
    val tile = (s - gap * 3) / 2f
    drawRoundRect(Leaf, Offset(gap, gap), Size(tile, tile), CornerRadius(8f))
    drawRoundRect(PadStroke, Offset(gap * 2 + tile, gap), Size(tile, tile), CornerRadius(8f))
    drawRoundRect(PadStroke, Offset(gap, gap * 2 + tile), Size(tile, tile), CornerRadius(8f))
    drawRoundRect(Head, Offset(gap * 2 + tile, gap * 2 + tile), Size(tile, tile), CornerRadius(8f))
}

private fun DrawScope.drawDodgeIcon() {
    val s = size.minDimension
    drawCircle(Bite, s * 0.1f, Offset(s * 0.28f, s * 0.22f))
    drawCircle(Bite, s * 0.12f, Offset(s * 0.7f, s * 0.34f))
    drawBeetle(Offset(s * 0.5f, s * 0.72f), s * 0.2f)
}

private fun DrawScope.drawCheckersIcon() {
    val s = size.minDimension
    val cell = s / 4f
    for (y in 0..3) for (x in 0..3) {
        if ((x + y) % 2 == 0) {
            drawRect(PadStroke, Offset(x * cell, y * cell), Size(cell, cell))
        }
    }
    drawCircle(Leaf, cell * 0.32f, Offset(cell * 0.5f, cell * 2.5f))
    drawCircle(Leaf, cell * 0.32f, Offset(cell * 2.5f, cell * 3.5f))
    drawCircle(Head, cell * 0.32f, Offset(cell * 1.5f, cell * 0.5f))
    drawCircle(Head, cell * 0.32f, Offset(cell * 3.5f, cell * 1.5f))
}

private fun DrawScope.drawChessIcon() {
    val s = size.minDimension
    val stem = Path().apply {
        moveTo(s * 0.5f, s * 0.12f)
        lineTo(s * 0.38f, s * 0.42f)
        lineTo(s * 0.42f, s * 0.42f)
        lineTo(s * 0.36f, s * 0.78f)
        lineTo(s * 0.64f, s * 0.78f)
        lineTo(s * 0.58f, s * 0.42f)
        lineTo(s * 0.62f, s * 0.42f)
        close()
    }
    drawPath(stem, Head)
    drawCircle(Head, s * 0.09f, Offset(s * 0.5f, s * 0.16f))
    drawRoundRect(Leaf, Offset(s * 0.28f, s * 0.78f), Size(s * 0.44f, s * 0.1f), CornerRadius(4f))
}

private fun DrawScope.drawFourIcon() {
    val s = size.minDimension
    val cell = s / 4f
    for (y in 0..3) for (x in 0..3) {
        drawCircle(PadStroke, cell * 0.28f, Offset(x * cell + cell * 0.5f, y * cell + cell * 0.5f))
    }
    drawCircle(Leaf, cell * 0.28f, Offset(cell * 1.5f, cell * 2.5f))
    drawCircle(Leaf, cell * 0.28f, Offset(cell * 1.5f, cell * 3.5f))
    drawCircle(Head, cell * 0.28f, Offset(cell * 2.5f, cell * 3.5f))
}

private fun DrawScope.drawFlipIcon() {
    val s = size.minDimension
    drawCircle(Leaf, s * 0.22f, Offset(s * 0.38f, s * 0.38f))
    drawCircle(Head, s * 0.22f, Offset(s * 0.62f, s * 0.62f))
    drawCircle(Leaf, s * 0.12f, Offset(s * 0.68f, s * 0.32f))
    drawCircle(Head, s * 0.12f, Offset(s * 0.32f, s * 0.68f))
}

private fun DrawScope.drawHopIcon() {
    val s = size.minDimension
    drawFrog(Offset(s * 0.5f, s * 0.58f), s * 0.22f)
    drawRoundRect(Bite, Offset(s * 0.08f, s * 0.82f), Size(s * 0.28f, s * 0.1f), CornerRadius(4f))
    drawRoundRect(Leaf, Offset(s * 0.58f, s * 0.18f), Size(s * 0.3f, s * 0.1f), CornerRadius(4f))
}

private fun DrawScope.drawEchoIcon() {
    val s = size.minDimension
    drawRoundRect(Leaf, Offset(s * 0.1f, s * 0.1f), Size(s * 0.36f, s * 0.36f), CornerRadius(10f))
    drawRoundRect(Head, Offset(s * 0.54f, s * 0.1f), Size(s * 0.36f, s * 0.36f), CornerRadius(10f))
    drawRoundRect(Bite, Offset(s * 0.1f, s * 0.54f), Size(s * 0.36f, s * 0.36f), CornerRadius(10f))
    drawRoundRect(PadStroke, Offset(s * 0.54f, s * 0.54f), Size(s * 0.36f, s * 0.36f), CornerRadius(10f))
}

private fun DrawScope.drawRocksIcon() {
    val s = size.minDimension
    drawStone(Offset(s * 0.42f, s * 0.42f), s * 0.28f, 0.4f)
    drawCraft(Offset(s * 0.58f, s * 0.68f), s * 0.16f, -1.2f, true)
}

private fun DrawScope.drawPeckIcon() {
    val s = size.minDimension
    drawCircle(PadStroke, s * 0.16f, Offset(s * 0.28f, s * 0.32f))
    drawCircle(PadStroke, s * 0.16f, Offset(s * 0.72f, s * 0.32f))
    drawCircle(PadStroke, s * 0.16f, Offset(s * 0.28f, s * 0.72f))
    drawSprout(Offset(s * 0.72f, s * 0.68f), s * 0.16f, 1f)
}

private fun DrawScope.drawCatchIcon() {
    val s = size.minDimension
    drawBerry(Offset(s * 0.32f, s * 0.28f), s * 0.1f)
    drawCircle(Head, s * 0.1f, Offset(s * 0.68f, s * 0.22f))
    drawRoundRect(Leaf, Offset(s * 0.22f, s * 0.72f), Size(s * 0.56f, s * 0.14f), CornerRadius(10f))
}

private fun DrawScope.drawSlideIcon() {
    val s = size.minDimension
    val g = s * 0.08f
    val t = (s - g * 3) / 2f
    drawRoundRect(Leaf, Offset(g, g), Size(t, t), CornerRadius(8f))
    drawRoundRect(Head, Offset(g * 2 + t, g), Size(t, t), CornerRadius(8f))
    drawRoundRect(Leaf, Offset(g, g * 2 + t), Size(t, t), CornerRadius(8f))
}

private fun DrawScope.drawBoxesIcon() {
    val s = size.minDimension
    drawRect(Leaf, Offset(s * 0.18f, s * 0.18f), Size(s * 0.64f, 6f))
    drawRect(Leaf, Offset(s * 0.18f, s * 0.5f), Size(s * 0.64f, 6f))
    drawRect(Head, Offset(s * 0.18f, s * 0.82f), Size(s * 0.64f, 6f))
    drawRect(Leaf, Offset(s * 0.18f, s * 0.18f), Size(6f, s * 0.64f))
    drawRect(Head, Offset(s * 0.5f, s * 0.18f), Size(6f, s * 0.64f))
    drawRect(Leaf, Offset(s * 0.82f, s * 0.18f), Size(6f, s * 0.64f))
}

private fun DrawScope.drawTrophy() {
    val s = size.minDimension
    drawRoundRect(Head, Offset(s * 0.28f, s * 0.18f), Size(s * 0.44f, s * 0.42f), CornerRadius(10f))
    drawCircle(Leaf, s * 0.08f, Offset(s * 0.18f, s * 0.36f), style = Stroke(4f))
    drawCircle(Leaf, s * 0.08f, Offset(s * 0.82f, s * 0.36f), style = Stroke(4f))
    drawRoundRect(Leaf, Offset(s * 0.42f, s * 0.58f), Size(s * 0.16f, s * 0.16f), CornerRadius(2f))
    drawRoundRect(Head, Offset(s * 0.3f, s * 0.74f), Size(s * 0.4f, s * 0.12f), CornerRadius(4f))
}
