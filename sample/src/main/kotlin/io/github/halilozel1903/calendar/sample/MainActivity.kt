package io.github.halilozel1903.calendar.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Planner, a small calendar app built with compose-calendar. It is fixed to Wednesday, 14 October 2026 and shows
 * made-up events, so every run looks the same.
 *
 * `scripts/screenshots.sh` starts it with `--es scene <scene>` to open a fixed screen for the README:
 *
 * - `month`: October 2026 with event dots and the selected day's events
 * - `range`: the trip planner with 16 to 22 October picked (6 nights) and booked dates disabled
 * - `week`: the week strip above the agenda with sticky day headers
 * - `tablet`: the month and the agenda side by side (tablets in landscape)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            PlannerTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    PlannerApp(scene = scene)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
