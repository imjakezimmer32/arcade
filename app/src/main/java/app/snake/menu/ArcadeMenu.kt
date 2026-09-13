package app.snake.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Leaf
import app.snake.Mist
import app.snake.Route
import app.snake.core.GameAppIcon
import app.snake.core.GameId
import app.snake.core.ScoresAppIcon
import app.snake.scores.ScoreStore
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.arcadeVerticalScrollbar

private sealed class Launcher {
    data class Game(val id: GameId) : Launcher()
    data object Scores : Launcher()
}

@Composable
fun ArcadeMenu(arcade: ArcadeViewModel) {
    val revision by arcade.revision.collectAsStateWithLifecycle()
    val scroll = rememberScrollState()
    val tiles = remember { GameId.entries.map { Launcher.Game(it) } + Launcher.Scores }
    ArcadeBackdrop {
        Spacer(Modifier.height(12.dp))
        Text("ARCADE", color = Leaf, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 8.sp)
        Text("Apps on the cabinet", color = Dim, fontSize = 14.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(18.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .arcadeVerticalScrollbar(scroll)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            tiles.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { item ->
                        when (item) {
                            is Launcher.Game -> {
                                val best = remember(revision, item.id) {
                                    item.id.scoreKeys()
                                        .mapNotNull { key -> arcade.scores.best(key, item.id.lowerBetter) }
                                        .let { rows ->
                                            if (item.id.lowerBetter) rows.minByOrNull { it.score }
                                            else rows.maxByOrNull { it.score }
                                        }
                                }
                                val label = when {
                                    item.id.hotSeat -> "hot seat"
                                    item.id.seatChoice && best == null -> "solo · 2P"
                                    best == null -> "play"
                                    item.id.lowerBetter -> ScoreStore.formatScore(best.score, true)
                                    else -> best.score.toString()
                                }
                                AppTile(
                                    title = item.id.title,
                                    caption = label.ifBlank { "play" },
                                    onClick = { arcade.go(Route.Play(item.id)) },
                                ) {
                                    GameAppIcon(item.id, Modifier.size(72.dp))
                                }
                            }
                            Launcher.Scores -> AppTile(
                                title = "SCORES",
                                caption = "hall",
                                onClick = { arcade.go(Route.Scores) },
                            ) {
                                ScoresAppIcon(Modifier.size(72.dp))
                            }
                        }
                    }
                    repeat(3 - row.size) {
                        Spacer(Modifier.size(88.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AppTile(
    title: String,
    caption: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .scale(if (pressed) 0.94f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = Mist,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(text = caption, color = Dim, fontSize = 10.sp, maxLines = 1)
    }
}
