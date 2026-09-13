package app.snake.core

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.hypot

const val TAP_SLOP = 22f
const val SWIPE_ARM = 26f

fun boxesOverlap(
    ax: Float, ay: Float, aw: Float, ah: Float,
    bx: Float, by: Float, bw: Float, bh: Float,
): Boolean = abs(ax - bx) * 2f < aw + bw && abs(ay - by) * 2f < ah + bh

fun circleOverlap(ax: Float, ay: Float, ar: Float, bx: Float, by: Float, br: Float, yStretch: Float = 1.55f): Boolean {
    val dx = ax - bx
    val dy = (ay - by) * yStretch
    val rr = ar + br
    return dx * dx + dy * dy < rr * rr
}

fun Modifier.followX(key: Any? = Unit, onX: (Float) -> Unit) = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown()
        onX((down.position.x / size.width).coerceIn(0f, 1f))
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            onX((change.position.x / size.width).coerceIn(0f, 1f))
            change.consume()
            if (!change.pressed) break
        }
    }
}

fun Modifier.followFinger(
    key: Any? = Unit,
    onHold: (x: Float, y: Float, down: Boolean) -> Unit,
    onTap: (() -> Unit)? = null,
) = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val origin = down.position
        var drifted = false
        fun nx(px: Float) = (px / size.width).coerceIn(0f, 1f)
        fun ny(py: Float) = (py / size.height).coerceIn(0f, 1f)
        onHold(nx(origin.x), ny(origin.y), true)
        var last = origin
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            last = change.position
            if (hypot(change.position.x - origin.x, change.position.y - origin.y) > TAP_SLOP) drifted = true
            onHold(nx(last.x), ny(last.y), true)
            change.consume()
            if (!change.pressed) break
        }
        onHold(nx(last.x), ny(last.y), false)
        if (!drifted) onTap?.invoke()
    }
}

fun Modifier.swipePulse(
    key: Any? = Unit,
    slop: Float = SWIPE_ARM,
    rearm: Boolean = true,
    onTap: (() -> Unit)? = null,
    onSwipe: (Dir) -> Unit,
) = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown()
        var accX = 0f
        var accY = 0f
        var tapped = true
        var locked = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            accX += change.position.x - change.previousPosition.x
            accY += change.position.y - change.previousPosition.y
            change.consume()
            if (!locked && (abs(accX) >= slop || abs(accY) >= slop)) {
                tapped = false
                if (abs(accX) > abs(accY)) onSwipe(if (accX > 0) Dir.Right else Dir.Left)
                else onSwipe(if (accY > 0) Dir.Down else Dir.Up)
                accX = 0f
                accY = 0f
                if (!rearm) locked = true
            }
            if (!change.pressed) break
        }
        if (tapped) onTap?.invoke()
    }
}

fun Modifier.onDown(key: Any? = Unit, onDown: () -> Unit) = pointerInput(key) {
    awaitEachGesture {
        awaitFirstDown()
        onDown()
        while (true) {
            val event = awaitPointerEvent()
            if (event.changes.none { it.pressed }) break
        }
    }
}
