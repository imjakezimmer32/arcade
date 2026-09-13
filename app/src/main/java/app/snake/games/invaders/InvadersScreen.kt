package app.snake.games.invaders

import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import app.snake.Sky
import app.snake.core.Buzz
import app.snake.core.drawBeetle
import app.snake.core.drawCrab
import app.snake.core.play
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.CenterNote
import app.snake.ui.EndOverlay
import app.snake.ui.GameTopBar
import app.snake.ui.PitFrame
import app.snake.ui.PitHint

@Composable
fun InvadersGame(arcade: ArcadeViewModel, vm: InvadersViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val prompt by arcade.prompt.collectAsStateWithLifecycle()
    val best = arcade.scores.best("invaders", false)?.score ?: 0
    val vibrator = LocalContext.current.getSystemService<Vibrator>()

    LaunchedEffect(arcade) { arcade.pause.collect { vm.pauseIfRunning() } }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            vibrator.play(
                when (event) {
                    InvEvent.Fire -> Buzz.Soft
                    InvEvent.Hit, InvEvent.Wave -> Buzz.Hit
                    InvEvent.Life -> Buzz.Dead
                    InvEvent.Boom, InvEvent.Dead -> Buzz.Dead
                },
            )
        }
    }
    LaunchedEffect(state.phase, state.scoreOffered) {
        if (!state.scoreOffered && state.phase == InvPhase.Dead) {
            arcade.offerScore("invaders", state.score, false, "Overrun", "${state.score} pts")
            vm.markScoreOffered()
        }
    }

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "INVADERS",
            subtitle = "best $best  ·  wave ${state.wave}  ·  ${"♥".repeat(state.lives.coerceAtLeast(0))}",
            value = state.score.toString().padStart(4, '0'),
            valueLabel = "score",
            onBack = arcade::back,
            onPause = vm::togglePause,
            pauseEnabled = state.phase == InvPhase.Running || state.phase == InvPhase.Paused,
            paused = state.phase == InvPhase.Paused,
        )
        Spacer(Modifier.height(12.dp))
        PitFrame(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(state.phase) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val x = (down.position.x / size.width).coerceIn(0f, 1f)
                        vm.aim(x)
                        vm.fire()
                        vm.hold(true)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            vm.aim((change.position.x / size.width).coerceIn(0f, 1f))
                            change.consume()
                            if (!change.pressed) break
                        }
                        vm.hold(false)
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) { drawInvaders(state) }
            when (state.phase) {
                InvPhase.Ready -> PitHint("Hold to move and fire")
                InvPhase.Paused -> CenterNote("Paused")
                InvPhase.Dead -> EndOverlay(
                    "Overrun",
                    "${state.score} pts  ·  wave ${state.wave}",
                    prompt,
                    arcade.scores.lastName,
                    arcade.scores.knownNames(),
                    arcade::saveScore,
                    arcade::skipScore,
                    vm::begin,
                )
                InvPhase.Running -> Unit
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Drag to steer. Hold to keep firing. Clear a wave, they come back faster.",
            color = Dim,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun DrawScope.drawInvaders(state: InvState) {
    val w = size.width
    val h = size.height
    val wiggle = if (state.frame % 2 == 0) 1f else -1f

    state.ufo?.let { saucer ->
        val cx = saucer.x * w
        drawRoundRect(Bite, Offset(cx - w * InvState.UFO_W / 2f, h * 0.03f), Size(w * InvState.UFO_W, h * 0.025f), CornerRadius(8f))
        drawCircle(Head, 4f, Offset(cx - 8f, h * 0.04f))
        drawCircle(Head, 4f, Offset(cx + 8f, h * 0.04f))
    }

    state.aliens.filter { it.alive }.forEach { a ->
        val x = (state.originX + a.col * InvState.CELL_W) * w
        val y = (state.originY + a.row * InvState.CELL_H) * h
        drawCrab(Offset(x, y), w * InvState.ALIEN_RX, if (a.kind == 2) Head else if (a.kind == 1) Leaf else Sky, wiggle)
    }

    state.bunkers.forEach { bunker ->
        val cellW = w * 0.028f
        val cellH = h * 0.038f
        val left = bunker.x * w - cellW * 2f
        val top = h * 0.70f
        bunker.cells.forEachIndexed { i, on ->
            if (!on) return@forEachIndexed
            val c = i % 4
            val r = i / 4
            drawRoundRect(
                Leaf.copy(alpha = 0.72f),
                Offset(left + c * cellW, top + r * cellH),
                Size(cellW - 2f, cellH - 2f),
                CornerRadius(3f),
            )
        }
    }

    state.shots.forEach { s ->
        drawRoundRect(
            if (s.up) Head else Bite,
            Offset(s.x * w - 2f, s.y * h - 9f),
            Size(4f, 16f),
            CornerRadius(2f),
        )
    }

    if (state.invuln <= 0f || ((state.invuln * 12).toInt() % 2 == 0)) {
        val sx = state.ship * w
        val sy = h * InvState.SHIP_Y
        drawBeetle(Offset(sx, sy), w * InvState.SHIP_W / 2f)
    }
}
