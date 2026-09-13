package app.snake

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Void = Color(0xFF070A08)
val Pit = Color(0xFF0C1410)
val PitAlt = Color(0xFF101A14)
val Leaf = Color(0xFF3DDC84)
val Head = Color(0xFFC8FF7A)
val Bite = Color(0xFFFF5A6A)
val Mist = Color(0xFFD7F5E3)
val Dim = Color(0xFF7FA88F)
val Pad = Color(0xFF15241C)
val PadStroke = Color(0xFF2A4A38)
val Moss = Color(0xFF1A3A28)
val Dew = Color(0xFF8BE8B0)
val Shell = Color(0xFFE8F5C8)
val Vein = Color(0xFF0A1F14)
val Pond = Color(0xFF143D4A)
val Sky = Color(0xFF7FDBFF)


private val SnakeColors = darkColorScheme(
    primary = Leaf,
    onPrimary = Void,
    secondary = Head,
    background = Void,
    surface = Pit,
    onBackground = Mist,
    onSurface = Mist,
    error = Bite,
)

@Composable
fun SnakeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SnakeColors,
        content = content,
    )
}
