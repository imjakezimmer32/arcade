package app.snake.core

import kotlinx.coroutines.delay

suspend fun gameClock(onTick: (Float) -> Unit) {
    var last = System.nanoTime()
    while (true) {
        delay(16)
        val now = System.nanoTime()
        val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.033f)
        last = now
        onTick(dt)
    }
}
