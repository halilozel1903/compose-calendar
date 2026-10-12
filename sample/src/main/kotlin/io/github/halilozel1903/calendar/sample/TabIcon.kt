package io.github.halilozel1903.calendar.sample

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Outline icons for the navigation, drawn as vectors so the sample needs no icon library. */
object TabIcon {
    fun of(tab: PlannerTab): ImageVector = when (tab) {
        PlannerTab.Month -> Month
        PlannerTab.Week -> Week
        PlannerTab.Trip -> Trip
    }

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
            .build()

    /** A calendar page with a grid of days. */
    val Month: ImageVector = icon("month") {
        moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(5f, 9.5f); lineTo(19f, 9.5f)
        moveTo(9f, 3f); lineTo(9f, 6.5f)
        moveTo(15f, 3f); lineTo(15f, 6.5f)
        moveTo(8.5f, 13f); lineTo(8.6f, 13f)
        moveTo(12f, 13f); lineTo(12.1f, 13f)
        moveTo(15.5f, 13f); lineTo(15.6f, 13f)
        moveTo(8.5f, 16.5f); lineTo(8.6f, 16.5f)
        moveTo(12f, 16.5f); lineTo(12.1f, 16.5f)
    }

    /** A calendar page with one highlighted row. */
    val Week: ImageVector = icon("week") {
        moveTo(5f, 5f); lineTo(19f, 5f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(9f, 3f); lineTo(9f, 6.5f)
        moveTo(15f, 3f); lineTo(15f, 6.5f)
        moveTo(8f, 12.5f); lineTo(16f, 12.5f)
        moveTo(8f, 16f); lineTo(13f, 16f)
    }

    /** A suitcase. */
    val Trip: ImageVector = icon("trip") {
        moveTo(4f, 8f); lineTo(20f, 8f); lineTo(20f, 19f); lineTo(4f, 19f); close()
        moveTo(9f, 8f); lineTo(9f, 5f); lineTo(15f, 5f); lineTo(15f, 8f)
        moveTo(9f, 11.5f); lineTo(9f, 15.5f)
        moveTo(15f, 11.5f); lineTo(15f, 15.5f)
    }
}
