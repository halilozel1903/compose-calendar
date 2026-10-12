package io.github.halilozel1903.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.core.CalendarSelection
import io.github.halilozel1903.calendar.core.DayPosition
import io.github.halilozel1903.calendar.core.RangeCell
import java.time.LocalDate
import java.util.Locale

/** Everything a day cell needs to draw itself; passed to the `dayContent` slot. */
@Immutable
public data class DayCellState(
    val date: LocalDate,
    /** In the shown month, or a day of the previous or next month. Week rows are always in month. */
    val position: DayPosition,
    val isToday: Boolean,
    /** The date is part of the selection (for a range: anywhere between start and end). */
    val isSelected: Boolean,
    /** The date can be tapped and selected. */
    val isEnabled: Boolean,
    /** How the date takes part in a range pill. */
    val range: RangeCell,
    /** Event dot colors, at most [CalendarDefaults.MaxDots] are drawn by the default content. */
    val dots: List<Color>,
) {
    /** True for days of the shown month. */
    val isInMonth: Boolean get() = position == DayPosition.InMonth
}

/**
 * One day cell: the selection circle, range pill and today ring behind, the content in front, and the semantics
 * (full date, today, selection, range end, event count, disabled) for TalkBack.
 */
@Composable
internal fun DayCell(
    day: DayCellState,
    colors: CalendarColors,
    strings: CalendarStrings,
    locale: Locale,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable BoxScope.(DayCellState) -> Unit)?,
) {
    val filled = day.range.isEndPoint || (day.isSelected && !day.range.inRange)
    val contentColor = when {
        filled -> colors.selectedContent
        !day.isEnabled -> colors.disabledContent
        day.range.inRange -> colors.rangeContent
        !day.isInMonth -> colors.outsideContent
        day.isToday -> colors.todayContent
        else -> colors.content
    }
    val description = buildString {
        append(CalendarDefaults.fullDate(day.date, locale))
        if (day.isToday) append(", ").append(strings.today)
        if (day.range.inRange && day.range.isStart) append(", ").append(strings.rangeStart)
        if (day.range.inRange && day.range.isEnd) append(", ").append(strings.rangeEnd)
        if (day.dots.isNotEmpty()) append(", ").append(strings.eventCount(day.dots.size))
        if (!day.isEnabled) append(", ").append(strings.unavailable)
    }
    Box(
        modifier = modifier
            .drawBehind { drawDayBackground(day, filled, colors) }
            .selectable(
                selected = day.isSelected,
                enabled = day.isEnabled,
                role = Role.Button,
                onClick = { onClick(day.date) },
            )
            .semantics { contentDescription = description },
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                if (content != null) content(day) else CalendarDefaults.DayContent(day, Modifier.fillMaxSize())
            }
        }
    }
}

private fun DrawScope.drawDayBackground(day: DayCellState, filled: Boolean, colors: CalendarColors) {
    val diameter = minOf(size.width, size.height) - 4.dp.toPx()
    if (diameter <= 0f) return
    val radius = diameter / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val range = day.range
    if (range.inRange && !(range.isStart && range.isEnd)) {
        // In right-to-left layouts the previous day is on the right.
        val ltr = layoutDirection == LayoutDirection.Ltr
        val joinsLeft = if (ltr) range.connectsBefore else range.connectsAfter
        val joinsRight = if (ltr) range.connectsAfter else range.connectsBefore
        val left = if (joinsLeft) 0f else center.x - radius
        val right = if (joinsRight) size.width else center.x + radius
        val top = center.y - radius
        val leftCorner = if (joinsLeft) CornerRadius.Zero else CornerRadius(radius)
        val rightCorner = if (joinsRight) CornerRadius.Zero else CornerRadius(radius)
        val pill = RoundRect(
            left = left,
            top = top,
            right = right,
            bottom = top + diameter,
            topLeftCornerRadius = leftCorner,
            topRightCornerRadius = rightCorner,
            bottomRightCornerRadius = rightCorner,
            bottomLeftCornerRadius = leftCorner,
        )
        drawPath(Path().apply { addRoundRect(pill) }, colors.rangeContainer)
    }
    when {
        filled -> drawCircle(colors.selectedContainer, radius = radius, center = center)
        day.isToday -> drawCircle(
            color = colors.todayOutline,
            radius = radius - 0.75.dp.toPx(),
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

/** Builds the [DayCellState] of [date]. */
internal fun dayCellState(
    date: LocalDate,
    position: DayPosition,
    state: CalendarState,
    selection: CalendarSelection,
    range: RangeCell,
    dots: List<Color>,
): DayCellState = DayCellState(
    date = date,
    position = position,
    isToday = date == state.today,
    isSelected = date in selection,
    isEnabled = state.isSelectable(date),
    range = range,
    dots = dots,
)
