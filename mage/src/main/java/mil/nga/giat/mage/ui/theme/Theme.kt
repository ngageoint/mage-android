package mil.nga.giat.mage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColors(
  primary = Blue600,
  primaryVariant = Blue800,
  secondary = OrangeA700,
  error = Red800
)

private val DarkColorPalette = darkColors(
  primary = Grey900,
  primaryVariant = Grey900,
  secondary = BlueA200,
  error = Red300
)

@Composable
fun MageTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colors = if (darkTheme) DarkColorPalette else LightColorPalette

  MaterialTheme(
    colors = colors,
    content = content
  )
}

val Colors.topAppBarBackground: Color @Composable get() = primary
val Colors.importantBackground: Color @Composable get() = OrangeA400