package io.github.halilozel1903.calendar.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** A calm teal theme for the Planner. */
@Composable
fun PlannerTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFF7DD5C7),
            onPrimary = Color(0xFF003731),
            primaryContainer = Color(0xFF1F5049),
            onPrimaryContainer = Color(0xFF9EF2E3),
            secondary = Color(0xFFB1CCC6),
            secondaryContainer = Color(0xFF334B47),
            onSecondaryContainer = Color(0xFFCCE8E2),
            tertiary = Color(0xFFF2B880),
            tertiaryContainer = Color(0xFF65401B),
            onTertiaryContainer = Color(0xFFFFDCC0),
            background = Color(0xFF0F1513),
            onBackground = Color(0xFFDEE4E1),
            surface = Color(0xFF0F1513),
            onSurface = Color(0xFFDEE4E1),
            surfaceVariant = Color(0xFF3F4946),
            onSurfaceVariant = Color(0xFFBEC9C5),
            surfaceContainerLowest = Color(0xFF0A0F0E),
            surfaceContainerLow = Color(0xFF171D1B),
            surfaceContainer = Color(0xFF1B211F),
            surfaceContainerHigh = Color(0xFF252B2A),
            surfaceContainerHighest = Color(0xFF303634),
            outline = Color(0xFF899390),
            outlineVariant = Color(0xFF3F4946),
            error = Color(0xFFFFB4AB),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0E6B60),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFC4EDE6),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A635F),
            secondaryContainer = Color(0xFFCCE8E2),
            onSecondaryContainer = Color(0xFF051F1C),
            tertiary = Color(0xFF9A5B22),
            tertiaryContainer = Color(0xFFFFDCC0),
            onTertiaryContainer = Color(0xFF331200),
            background = Color(0xFFF6FBF9),
            onBackground = Color(0xFF171D1B),
            surface = Color(0xFFF6FBF9),
            onSurface = Color(0xFF171D1B),
            surfaceVariant = Color(0xFFDAE5E1),
            onSurfaceVariant = Color(0xFF3F4946),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF0F5F3),
            surfaceContainer = Color(0xFFEAEFED),
            surfaceContainerHigh = Color(0xFFE4EAE7),
            surfaceContainerHighest = Color(0xFFDEE4E1),
            outline = Color(0xFF6F7976),
            outlineVariant = Color(0xFFBEC9C5),
            error = Color(0xFFBA1A1A),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
