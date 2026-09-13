package app.snake.scores

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.snake.ArcadeViewModel
import app.snake.Dim
import app.snake.Leaf
import app.snake.Mist
import app.snake.Pad
import app.snake.PadStroke
import app.snake.Pit
import app.snake.core.GameAppIcon
import app.snake.core.GameId
import app.snake.games.mines.MinesDiff
import app.snake.ui.ArcadeBackdrop
import app.snake.ui.GameTopBar
import app.snake.ui.arcadeVerticalScrollbar

@Composable
fun LeaderboardScreen(arcade: ArcadeViewModel) {
    val revision by arcade.revision.collectAsStateWithLifecycle()
    var game by remember { mutableStateOf(GameId.Snake) }
    var minesDiff by remember { mutableStateOf(MinesDiff.Beginner) }
    var seatBoard by remember { mutableStateOf(false) }
    val lowerBetter = if (seatBoard && game.seatChoice) false else game.lowerBetter
    val key = when {
        game == GameId.Mines -> minesDiff.boardKey
        seatBoard && game.seatChoice -> game.seatKey()
        else -> game.boardKey
    }
    val podium = remember(revision, key, lowerBetter) { arcade.scores.top(key, lowerBetter) }
    val history = remember(revision, key) { arcade.scores.history(key) }
    val scroll = rememberScrollState()

    ArcadeBackdrop {
        Spacer(Modifier.height(10.dp))
        GameTopBar(
            title = "SCORES",
            subtitle = "Top 3  ·  every run kept",
            value = history.size.toString(),
            valueLabel = "runs",
            onBack = arcade::back,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameId.entries.forEach { id ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        game = id
                        seatBoard = false
                    }.padding(horizontal = 2.dp),
                ) {
                    GameAppIcon(id, Modifier.size(52.dp), selected = game == id)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = id.title.lowercase(),
                        color = if (game == id) Leaf else Dim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (game.seatChoice) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Chip(
                    label = "Solo",
                    selected = !seatBoard,
                    modifier = Modifier.weight(1f),
                    onClick = { seatBoard = false },
                )
                Chip(
                    label = "2P",
                    selected = seatBoard,
                    modifier = Modifier.weight(1f),
                    onClick = { seatBoard = true },
                )
            }
        }
        if (game == GameId.Mines) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                MinesDiff.entries.forEach { diff ->
                    Chip(
                        label = diff.label,
                        selected = minesDiff == diff,
                        modifier = Modifier.weight(1f),
                        onClick = { minesDiff = diff },
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Pit)
                .border(1.dp, PadStroke, RoundedCornerShape(18.dp))
                .arcadeVerticalScrollbar(scroll)
                .verticalScroll(scroll)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("TOP 3", color = Leaf, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            if (podium.isEmpty()) {
                Text("No podium yet. Finish a run and write a name.", color = Dim, fontSize = 14.sp)
            } else {
                podium.forEachIndexed { index, row ->
                    ScoreLine(
                        rank = (index + 1).toString(),
                        row = row,
                        lowerBetter = lowerBetter,
                        loud = index == 0,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("HISTORY", color = Dim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            if (history.isEmpty()) {
                Text("Every named run lands here, same name as many times as you want.", color = Dim, fontSize = 14.sp)
            } else {
                history.forEach { row ->
                    HistoryLine(row = row, lowerBetter = lowerBetter)
                }
            }
        }
    }
}

@Composable
private fun ScoreLine(rank: String, row: ScoreRow, lowerBetter: Boolean, loud: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank,
            color = if (loud) Leaf else Dim,
            fontFamily = FontFamily.Monospace,
            fontSize = if (loud) 18.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = row.name,
            color = Mist,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (loud) 18.sp else 15.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ScoreStore.formatScore(row.score, lowerBetter),
            color = Leaf,
            fontFamily = FontFamily.Monospace,
            fontSize = if (loud) 20.sp else 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HistoryLine(row: ScoreRow, lowerBetter: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.name, color = Mist, fontWeight = FontWeight.SemiBold)
            Text(
                text = buildString {
                    append(ScoreStore.formatWhen(row.at))
                    if (!row.win) append("  ·  loss")
                },
                color = Dim,
                fontSize = 11.sp,
            )
        }
        Text(
            text = ScoreStore.formatScore(row.score, lowerBetter),
            color = if (row.win) Leaf else Dim,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) Leaf else Pad)
            .border(1.dp, if (selected) Leaf else PadStroke, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(5),
            color = if (selected) app.snake.Void else Leaf,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}
