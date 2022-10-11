package mil.nga.giat.mage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorPalette = lightColors(
  primary = Blue600,
  primaryVariant = Blue800,
  secondary = OrangeA700,
  error = Red800
)

private val DarkColorPalette = darkColors(
  primary = Grey800,
  primaryVariant = Grey800,
  secondary = BlueA200,
  error = Red300,
  onPrimary = Color.White
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

val Colors.warning: Color @Composable get() = Amber700
val Colors.topAppBarBackground: Color @Composable get() = primary
val Colors.importantBackground: Color @Composable get() = OrangeA400
val Colors.linkColor: Color @Composable get() {
 return if (isSystemInDarkTheme()) MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium) else primary
}