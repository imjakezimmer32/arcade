package app.snake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.snake.Dim
import app.snake.Head
import app.snake.Leaf
import app.snake.Mist
import app.snake.Pad
import app.snake.PadStroke
import app.snake.Pit
import app.snake.Void
import app.snake.scores.ScoreStore

@Composable
fun ModePick(
    title: String,
    soloHint: String,
    seatHint: String,
    onSolo: () -> Unit,
    onSeat: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void.copy(alpha = 0.72f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Mist, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Text("One cabinet. One or two players.", color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            LeafButton("SOLO", onClick = onSolo, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(soloHint, color = Dim, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            LeafButton("HOT SEAT", onClick = onSeat, filled = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(seatHint, color = Dim, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun HotSeatLobby(
    title: String,
    leafRole: String,
    headRole: String,
    lastName: String,
    knownNames: List<String>,
    onStart: (leafName: String, headName: String) -> Unit,
    blurb: String = "Same phone. Pass it after every move.",
) {
    var leaf by remember(lastName) { mutableStateOf(lastName.ifBlank { "LEAF" }) }
    var head by remember { mutableStateOf("HEAD") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void.copy(alpha = 0.72f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Mist, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Spacer(Modifier.height(6.dp))
            Text(blurb, color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            SeatField("Bottom · $leafRole", leaf, Leaf, knownNames) { leaf = it }
            Spacer(Modifier.height(12.dp))
            SeatField("Top · $headRole", head, Head, knownNames) { head = it }
            Spacer(Modifier.height(18.dp))
            LeafButton(
                text = "SIT DOWN",
                onClick = {
                    val a = leaf.trim().ifBlank { "LEAF" }.take(ScoreStore.NAME_MAX)
                    var b = head.trim().ifBlank { "HEAD" }.take(ScoreStore.NAME_MAX)
                    if (b.equals(a, ignoreCase = true)) b = if (a.equals("HEAD", true)) "LEAF" else "HEAD"
                    onStart(a, b)
                },
            )
        }
    }
}

@Composable
private fun SeatField(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    knownNames: List<String>,
    onChange: (String) -> Unit,
) {
    Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    if (knownNames.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            knownNames.forEach { known ->
                val selected = value.equals(known, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (selected) accent else Pad)
                        .border(1.dp, if (selected) accent else PadStroke, RoundedCornerShape(15.dp))
                        .clickable { onChange(known) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(known, color = if (selected) Void else accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(ScoreStore.NAME_MAX)) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent,
            unfocusedBorderColor = PadStroke,
            focusedLabelColor = accent,
            unfocusedLabelColor = Dim,
            cursorColor = accent,
            focusedTextColor = Mist,
            unfocusedTextColor = Mist,
            focusedContainerColor = Pit,
            unfocusedContainerColor = Pit,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun SeatResult(
    title: String,
    detail: String,
    onRematch: () -> Unit,
    onLobby: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void.copy(alpha = 0.72f))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Mist, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(detail, color = Dim, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            LeafButton("REMATCH", onClick = onRematch)
            Spacer(Modifier.height(10.dp))
            LeafButton("NEW", onClick = onLobby, filled = false)
        }
    }
}

@Composable
fun PassPhoneOverlay(name: String, onReady: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void.copy(alpha = 0.78f))
            .clickable(onClick = onReady)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PASS THE PHONE", color = Dim, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(name, color = Leaf, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Tap when you're seated.", color = Mist, fontSize = 15.sp)
        }
    }
}
