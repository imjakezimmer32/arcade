package app.snake.core

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.snake.Bite
import app.snake.Dew
import app.snake.Head
import app.snake.Leaf
import app.snake.Moss
import app.snake.Pond
import app.snake.Shell
import app.snake.Sky
import app.snake.Vein
import app.snake.Void
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawEyes(center: Offset, radius: Float, look: Offset = Offset(radius * 0.18f, -radius * 0.08f)) {
    val spread = Offset(radius * 0.32f, 0f)
    val r = radius * 0.16f
    drawCircle(Void, r, center - spread + look)
    drawCircle(Void, r, center + spread + look)
    drawCircle(Shell, r * 0.35f, center - spread + look + Offset(-r * 0.2f, -r * 0.2f))
    drawCircle(Shell, r * 0.35f, center + spread + look + Offset(-r * 0.2f, -r * 0.2f))
}

fun DrawScope.drawBerry(center: Offset, radius: Float) {
    drawCircle(Bite.copy(alpha = 0.22f), radius * 1.55f, center)
    drawCircle(Bite, radius, center)
    drawCircle(Color(0xFFFF8A96), radius * 0.28f, center + Offset(-radius * 0.28f, -radius * 0.28f))
    drawRoundRect(Leaf, Offset(center.x - radius * 0.12f, center.y - radius * 1.15f), Size(radius * 0.24f, radius * 0.4f), CornerRadius(2f))
}

fun DrawScope.drawFrog(center: Offset, radius: Float) {
    drawRoundRect(Leaf, Offset(center.x - radius * 0.85f, center.y - radius * 0.15f), Size(radius * 1.7f, radius * 1.1f), CornerRadius(radius * 0.5f))
    drawCircle(Dew, radius * 0.72f, center + Offset(0f, -radius * 0.35f))
    drawCircle(Leaf, radius * 0.38f, center + Offset(-radius * 0.38f, -radius * 0.62f))
    drawCircle(Leaf, radius * 0.38f, center + Offset(radius * 0.38f, -radius * 0.62f))
    drawEyes(center + Offset(0f, -radius * 0.42f), radius * 0.9f, Offset(0f, -radius * 0.06f))
    drawCircle(Vein, radius * 0.12f, center + Offset(-radius * 0.18f, radius * 0.12f))
    drawCircle(Vein, radius * 0.12f, center + Offset(radius * 0.18f, radius * 0.12f))
}

fun DrawScope.drawBird(center: Offset, radius: Float, flap: Float = 0f) {
    val wing = radius * (0.7f + flap * 0.25f)
    drawCircle(Head, radius, center)
    drawRoundRect(Head, Offset(center.x - radius * 1.15f, center.y - wing * 0.35f), Size(radius * 0.7f, wing * 0.7f), CornerRadius(radius * 0.3f))
    drawCircle(Bite, radius * 0.22f, center + Offset(radius * 0.55f, 0f))
    drawEyes(center + Offset(radius * 0.12f, -radius * 0.08f), radius * 0.85f)
}

fun DrawScope.drawCrab(center: Offset, radius: Float, color: Color, wiggle: Float = 1f) {
    drawRoundRect(color, Offset(center.x - radius, center.y - radius * 0.55f), Size(radius * 2f, radius * 1.05f), CornerRadius(radius * 0.25f))
    drawEyes(center + Offset(0f, -radius * 0.12f), radius * 1.1f)
    drawRoundRect(color, Offset(center.x - radius * 1.15f, center.y + radius * 0.2f), Size(radius * 0.35f, radius * 0.7f * wiggle), CornerRadius(3f))
    drawRoundRect(color, Offset(center.x + radius * 0.8f, center.y + radius * 0.2f), Size(radius * 0.35f, radius * 0.7f * wiggle), CornerRadius(3f))
}

fun DrawScope.drawBeetle(center: Offset, radius: Float, color: Color = Leaf) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x - radius * 0.85f, center.y + radius * 0.7f)
        lineTo(center.x - radius * 0.28f, center.y + radius * 0.35f)
        lineTo(center.x + radius * 0.28f, center.y + radius * 0.35f)
        lineTo(center.x + radius * 0.85f, center.y + radius * 0.7f)
        close()
    }
    drawPath(path, color)
    drawCircle(Vein, radius * 0.12f, center + Offset(-radius * 0.18f, -radius * 0.1f))
    drawCircle(Vein, radius * 0.12f, center + Offset(radius * 0.18f, -radius * 0.1f))
}

fun DrawScope.drawSprout(center: Offset, radius: Float, peek: Float = 1f) {
    val h = radius * (0.4f + peek * 0.9f)
    drawCircle(Moss, radius * 1.05f, center + Offset(0f, radius * 0.35f))
    drawCircle(Leaf, radius * 0.7f * peek, center + Offset(0f, -h * 0.15f))
    drawCircle(Dew, radius * 0.28f * peek, center + Offset(-radius * 0.18f, -h * 0.28f))
    drawCircle(Dew, radius * 0.28f * peek, center + Offset(radius * 0.18f, -h * 0.28f))
    if (peek > 0.4f) drawEyes(center + Offset(0f, -h * 0.2f), radius * peek)
}

fun DrawScope.drawBasket(center: Offset, width: Float, height: Float) {
    drawRoundRect(Leaf, Offset(center.x - width / 2f, center.y - height / 2f), Size(width, height), CornerRadius(height * 0.4f))
    drawRoundRect(Moss, Offset(center.x - width / 2f + 6f, center.y - height * 0.15f), Size(width - 12f, height * 0.45f), CornerRadius(8f))
}

fun DrawScope.drawCraft(center: Offset, radius: Float, ang: Float, thrusting: Boolean) {
    val nose = Offset(center.x + cos(ang) * radius, center.y + sin(ang) * radius)
    val left = Offset(center.x + cos(ang + 2.45f) * radius * 0.78f, center.y + sin(ang + 2.45f) * radius * 0.78f)
    val right = Offset(center.x + cos(ang - 2.45f) * radius * 0.78f, center.y + sin(ang - 2.45f) * radius * 0.78f)
    val path = Path().apply {
        moveTo(nose.x, nose.y)
        lineTo(left.x, left.y)
        lineTo(center.x + cos(ang + 3.14f) * radius * 0.28f, center.y + sin(ang + 3.14f) * radius * 0.28f)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, Leaf)
    if (thrusting) {
        val flame = Path().apply {
            val back = Offset(center.x + cos(ang + 3.14f) * radius * 0.55f, center.y + sin(ang + 3.14f) * radius * 0.55f)
            moveTo(back.x, back.y)
            lineTo(center.x + cos(ang + 2.6f) * radius * 0.4f, center.y + sin(ang + 2.6f) * radius * 0.4f)
            lineTo(center.x + cos(ang - 2.6f) * radius * 0.4f, center.y + sin(ang - 2.6f) * radius * 0.4f)
            close()
        }
        drawPath(flame, Head)
    }
}

fun DrawScope.drawStone(center: Offset, radius: Float, seed: Float = 0f) {
    val path = Path().apply {
        val n = 7
        for (i in 0 until n) {
            val a = seed + i * 6.283f / n
            val rr = radius * (0.78f + 0.22f * sin(a * 3f + seed))
            val p = Offset(center.x + cos(a) * rr, center.y + sin(a) * rr)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(path, Moss)
    drawPath(path, Head.copy(alpha = 0.85f), style = Stroke(width = 3f))
}

fun DrawScope.drawLog(left: Offset, width: Float, height: Float) {
    drawRoundRect(Leaf, left, Size(width, height), CornerRadius(height * 0.4f))
    drawRoundRect(Moss, Offset(left.x + width * 0.12f, left.y + height * 0.28f), Size(width * 0.18f, height * 0.44f), CornerRadius(4f))
    drawRoundRect(Moss, Offset(left.x + width * 0.55f, left.y + height * 0.28f), Size(width * 0.22f, height * 0.44f), CornerRadius(4f))
}

fun DrawScope.drawCar(left: Offset, width: Float, height: Float) {
    drawRoundRect(Bite, left, Size(width, height), CornerRadius(height * 0.28f))
    drawRoundRect(Shell.copy(alpha = 0.35f), Offset(left.x + width * 0.12f, left.y + 4f), Size(width * 0.28f, height * 0.42f), CornerRadius(3f))
    drawRoundRect(Shell.copy(alpha = 0.35f), Offset(left.x + width * 0.55f, left.y + 4f), Size(width * 0.28f, height * 0.42f), CornerRadius(3f))
}

fun DrawScope.drawBead(center: Offset, radius: Float, color: Color) {
    drawCircle(color, radius, center)
    drawCircle(Shell.copy(alpha = 0.35f), radius * 0.28f, center + Offset(-radius * 0.28f, -radius * 0.28f))
}

fun DrawScope.drawLilyPad(center: Offset, radius: Float, filled: Boolean) {
    drawCircle(if (filled) Head else Leaf.copy(alpha = 0.4f), radius, center)
    drawCircle(Pond.copy(alpha = 0.35f), radius * 0.45f, center)
}
