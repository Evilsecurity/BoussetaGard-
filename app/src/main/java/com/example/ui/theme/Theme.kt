package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color(0xFF002200),
    primaryContainer = Color(0xFF005121),
    onPrimaryContainer = Color(0xFFB1FFC1),
    secondary = Color(0xFF26A69A),
    onSecondary = Color(0xFF002521),
    tertiary = Color(0xFF80DEEA),
    onTertiary = Color(0xFF002B33),
    background = Color(0xFF0C130E),
    onBackground = Color(0xFFD8E4DC),
    surface = Color(0xFF131D16),
    onSurface = Color(0xFFD8E4DC),
    surfaceVariant = Color(0xFF1E2F24),
    onSurfaceVariant = Color(0xFFCCE6D2)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF00833F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDF9CD),
    onPrimaryContainer = Color(0xFF002B11),
    secondary = Color(0xFF00695C),
    onSecondary = Color.White,
    tertiary = Color(0xFF00838F),
    onTertiary = Color.White,
    background = Color(0xFFF7FBF8),
    onBackground = Color(0xFF111E15),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111E15),
    surfaceVariant = Color(0xFFDFE9E1),
    onSurfaceVariant = Color(0xFF3F4B41)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
