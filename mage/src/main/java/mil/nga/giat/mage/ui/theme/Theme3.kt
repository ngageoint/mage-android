package mil.nga.giat.mage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColorScheme(
  primary = Blue600,
  secondary = OrangeA700,
  tertiary = Blue800,
  surfaceVariant = Color(red = 231, green = 231, blue = 231),
  error = Red800
)

private val DarkColorPalette = darkColorScheme(
  primary = Grey600,
  secondary = BlueA200,
  tertiary = Color(0xDDFFFFFF),
  error = Red300,
  onPrimary = Color.White,
  surfaceVariant = Color(red = 42, green = 41, blue = 45)
)

@Composable
fun MageTheme3(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) {
    DarkColorPalette
  } else {
    LightColorPalette
  }
  MaterialTheme(
    colorScheme = colorScheme,
    content = content
  )
}

val ColorScheme.onSurfaceDisabled: Color @Composable
  get() = onSurface.copy(alpha = 0.40f)