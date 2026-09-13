package app.snake.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.snake.Dim
import app.snake.Leaf
import app.snake.Mist
import app.snake.Pad
import app.snake.PadStroke
import app.snake.Pit
import app.snake.Void
import app.snake.scores.ScorePrompt
import app.snake.scores.ScoreStore

@Composable
fun ArcadeBackdrop(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08110C), Void, Color(0xFF050806)),
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 18.dp),
    ) {
        content()
        Spacer(
            Modifier
                .height(12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        )
    }
}

@Composable
fun GameTopBar(
    title: String,
    subtitle: String,
    value: String,
    valueLabel: String,
    onBack: () -> Unit,
    onPause: (() -> Unit)? = null,
    pauseEnabled: Boolean = false,
    paused: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Back",
                onClick = onBack,
                size = 40.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = Leaf,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                )
                Text(
                    text = subtitle,
                    color = Dim,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 10.dp)) {
                Text(
                    text = value,
                    color = Mist,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                Text(text = valueLabel, color = Dim, fontSize = 11.sp, letterSpacing = 1.sp)
            }
            if (onPause != null) {
                RoundIconButton(
                    icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    label = "Pause",
                    onClick = onPause,
                    enabled = pauseEnabled,
                    size = 40.dp,
                )
            }
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 44.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(size)
            .scale(if (pressed) 0.94f else 1f)
            .clip(CircleShape)
            .background(Pad)
            .border(1.dp, if (pressed && enabled) Leaf else PadStroke, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Leaf else Dim.copy(alpha = 0.4f),
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
fun LeafButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (filled) Leaf else Pad)
            .border(1.dp, if (filled) Leaf else PadStroke, RoundedCornerShape(22.dp))
            .scale(if (pressed) 0.97f else 1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) Void else Leaf,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
fun CenterNote(text: String, dimPit: Boolean = true, onTap: (() -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dimPit) Void.copy(alpha = 0.55f) else Color.Transparent)
            .then(
                if (onTap != null) Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onTap,
                ) else Modifier,
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Mist,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
        )
    }
}

@Composable
fun BoxScope.PitHint(text: String) {
    Text(
        text = text,
        color = Mist,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
    )
}

@Composable
fun PitFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Pit)
            .border(1.5.dp, PadStroke, RoundedCornerShape(22.dp)),
    ) {
        content()
    }
}

@Composable
fun EndOverlay(
    title: String,
    detail: String? = null,
    prompt: ScorePrompt?,
    lastName: String,
    knownNames: List<String> = emptyList(),
    onSave: (String) -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit,
    retryLabel: String = "PLAY AGAIN",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void.copy(alpha = 0.62f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = Mist,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
            )
            if (!detail.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text = detail, color = Dim, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            if (prompt != null) {
                NameEntry(
                    lastName = lastName,
                    knownNames = knownNames,
                    scoreLabel = ScoreStore.formatScore(prompt.score, prompt.lowerBetter),
                    onSave = onSave,
                    onSkip = onSkip,
                )
            } else {
                LeafButton(text = retryLabel, onClick = onRetry)
            }
        }
    }
}

@Composable
fun NameEntry(
    lastName: String,
    knownNames: List<String>,
    scoreLabel: String,
    onSave: (String) -> Unit,
    onSkip: () -> Unit,
) {
    var name by remember(lastName) { mutableStateOf(lastName) }
    Text(
        text = "New high score  $scoreLabel",
        color = Leaf,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    )
    if (knownNames.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            knownNames.forEach { known ->
                val selected = name.equals(known, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Leaf else Pad)
                        .border(1.dp, if (selected) Leaf else PadStroke, RoundedCornerShape(16.dp))
                        .clickable { name = known }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = known,
                        color = if (selected) Void else Leaf,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(ScoreStore.NAME_MAX) },
        singleLine = true,
        label = { Text("Name — reuse or type") },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSave(name) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Leaf,
            unfocusedBorderColor = PadStroke,
            focusedLabelColor = Leaf,
            unfocusedLabelColor = Dim,
            cursorColor = Leaf,
            focusedTextColor = Mist,
            unfocusedTextColor = Mist,
            focusedContainerColor = Pit,
            unfocusedContainerColor = Pit,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        LeafButton(text = "SKIP", onClick = onSkip, filled = false)
        LeafButton(text = "SAVE", onClick = { onSave(name) }, filled = true)
    }
}

fun Modifier.arcadeVerticalScrollbar(state: ScrollState): Modifier = drawWithContent {
    drawContent()
    val viewport = size.height
    val max = state.maxValue.toFloat()
    if (max <= 0f || viewport <= 0f) return@drawWithContent
    val content = max + viewport
    val thumb = (viewport / content * viewport).coerceAtLeast(28f)
    val travel = (viewport - thumb).coerceAtLeast(0f)
    val y = (state.value / max) * travel
    drawRoundRect(
        color = Leaf.copy(alpha = 0.72f),
        topLeft = Offset(size.width - 7f, y),
        size = Size(4f, thumb),
        cornerRadius = CornerRadius(2f, 2f),
    )
}
