package nl.hoefvinyl.stereoinstallatie.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Colors = darkColorScheme(
    primary = Gold,
    secondary = ActivePurple,
    background = AppBackground,
    surface = CardBackground,
    onPrimary = PurpleText,
    onBackground = PrimaryText,
    onSurface = PrimaryText
)

@Composable
fun StereoInstallatieTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
