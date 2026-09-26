package ro.buroot.laqta

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF2B4174)
val NavyDark = Color(0xFF3A5391)
val Mauve = Color(0xFF8E6A8C)
val MauveDark = Color(0xFF7A5878)
val OkGreen = Color(0xFF2E7D4F)
val BadRed = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Mauve,
    onSecondary = Color.White,
    background = Color(0xFFF8F7FC),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFF8F7FC),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFEDECF3),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF77767D),
    secondaryContainer = Color(0xFFDCE1FF),
    onSecondaryContainer = Color(0xFF142B5C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB2C5FF),
    onPrimary = Color(0xFF102C5F),
    secondary = Color(0xFFE3BADF),
    onSecondary = Color(0xFF432741),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF1F2027),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF8F909A),
    secondaryContainer = Color(0xFF34457A),
    onSecondaryContainer = Color(0xFFDCE1FF)
)

@Composable
fun LaqtaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
