package io.github.halilozel1903.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.core.CalendarMath
import io.github.halilozel1903.calendar.core.CalendarSelection
import io.github.halilozel1903.calendar.core.CalendarWeek
import io.github.halilozel1903.calendar.core.EventIndex
import io.github.halilozel1903.calendar.core.GridRows
import io.github.halilozel1903.calendar.core.OutsideDays
import io.github.halilozel1903.calendar.core.RangeCell
import io.github.halilozel1903.calendar.core.RangeHighlight
import io.github.halilozel1903.calendar.core.SelectionResult
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** Event dot colors for a date. [Color.Unspecified] entries are drawn in [CalendarColors.eventDot]. */
public typealias EventDots = (LocalDate) -> List<Color>

/** No event dots. */
public val NoEventDots: EventDots = { emptyList() }

/**
 * Event dots from an [EventIndex]: one dot per event on the date, colored by [color] ([Color.Unspecified] or null for
 * the calendar's default dot color). The result is remembered for [events].
 *
 * ```kotlin
 * val index = remember(events) { EventIndex.ofEvents(events) }
 * val dots = rememberEventDots(index) { event -> event.color?.let(::Color) }
 * ```
 */
@Composable
public fun <T> rememberEventDots(events: EventIndex<T>, color: (T) -> Color? = { null }): EventDots {
    val currentColor by rememberUpdatedState(color)
    return remember(events) {
        { date -> events.eventsOn(date).take(CalendarDefaults.MaxDots).map { currentColor(it) ?: Color.Unspecified } }
    }
}

/**
 * A month calendar that swipes between months.
 *
 * Every month has the same number of rows by default ([GridRows.Six]), so the calendar does not change height while
 * swiping. Taps go through [CalendarState.select], which applies the selection mode and [SelectionRules]
 * (bounds, disabled dates, range length); [onDateClick] then gets the tapped date and what the tap did, for example
 * to show why a date was rejected.
 *
 * @param state the visible month and the selection; see [rememberCalendarState].
 * @param eventDots the dot colors under each date; see [rememberEventDots].
 * @param outsideDays whether days of the previous and next month are drawn in the first and last rows.
 * @param gridRows six rows for every month, or only the rows the month needs.
 * @param showWeekNumbers adds a column with ISO 8601 week numbers.
 * @param header drawn above the weekday labels; [CalendarDefaults.MonthHeader] by default, `{}` for none.
 * @param dayContent replaces the content of a day cell (the day number and dots). The cell still draws the
 * selection circle, range pill and today ring behind it and sets [androidx.compose.material3.LocalContentColor].
 */
@Composable
public fun MonthCalendar(
    state: CalendarState,
    modifier: Modifier = Modifier,
    onDateClick: (date: LocalDate, result: SelectionResult) -> Unit = { _, _ -> },
    eventDots: EventDots = NoEventDots,
    outsideDays: OutsideDays = OutsideDays.Show,
    gridRows: GridRows = GridRows.Six,
    showWeekNumbers: Boolean = false,
    userScrollEnabled: Boolean = true,
    dayHeight: Dp = CalendarDefaults.DayHeight,
    colors: CalendarColors = CalendarDefaults.colors(),
    strings: CalendarStrings = CalendarStrings(),
    locale: Locale = Locale.getDefault(),
    header: @Composable (month: YearMonth) -> Unit = { month ->
        CalendarDefaults.MonthHeader(state = state, month = month, colors = colors, strings = strings, locale = locale)
    },
    dayContent: (@Composable BoxScope.(DayCellState) -> Unit)? = null,
) {
    Column(modifier) {
        header(state.visibleMonth)
        WeekdayRow(
            firstDayOfWeek = state.firstDayOfWeek,
            colors = colors,
            locale = locale,
            leadingWidth = if (showWeekNumbers) CalendarDefaults.WeekNumberWidth else 0.dp,
        )
        HorizontalPager(
            state = state.monthPagerState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = userScrollEnabled,
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            key = { page -> state.months.monthAt(page).toString() },
        ) { page ->
            val month = state.months.monthAt(page)
            val grid = remember(month, state.firstDayOfWeek, gridRows) {
                CalendarMath.monthGrid(month, state.firstDayOfWeek, gridRows)
            }
            Column(Modifier.fillMaxWidth()) {
                for (week in grid.weeks) {
                    WeekRow(
                        week = week,
                        state = state,
                        selection = state.selection,
                        hideOutside = outsideDays == OutsideDays.Hide,
                        showWeekNumber = showWeekNumbers,
                        dayHeight = dayHeight,
                        eventDots = eventDots,
                        colors = colors,
                        strings = strings,
                        locale = locale,
                        onDateClick = onDateClick,
                        dayContent = dayContent,
                    )
                }
            }
        }
    }
}

/**
 * One row of seven day cells, shared by [MonthCalendar] and [WeekCalendar]. Hidden outside days keep an empty cell,
 * so the columns stay aligned, and the range pill is rounded next to them.
 */
@Composable
internal fun WeekRow(
    week: CalendarWeek,
    state: CalendarState,
    selection: CalendarSelection,
    hideOutside: Boolean,
    showWeekNumber: Boolean,
    dayHeight: Dp,
    eventDots: EventDots,
    colors: CalendarColors,
    strings: CalendarStrings,
    locale: Locale,
    onDateClick: (LocalDate, SelectionResult) -> Unit,
    dayContent: (@Composable BoxScope.(DayCellState) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().height(dayHeight), verticalAlignment = Alignment.CenterVertically) {
        if (showWeekNumber) {
            val label = strings.weekNumber(week.weekNumber)
            Text(
                text = week.weekNumber.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.weekNumberContent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .width(CalendarDefaults.WeekNumberWidth)
                    .semantics { contentDescription = label },
            )
        }
        val visible = week.days.map { !hideOutside || it.isInMonth }
        week.days.forEachIndexed { column, day ->
            val cellModifier = Modifier.weight(1f).height(dayHeight)
            if (!visible[column]) {
                Box(cellModifier)
                return@forEachIndexed
            }
            val range: RangeCell = RangeHighlight.cell(
                date = day.date,
                selection = selection,
                isRowStart = column == 0 || !visible[column - 1],
                isRowEnd = column == week.days.lastIndex || !visible[column + 1],
            )
            val dots = eventDots(day.date).map { if (it == Color.Unspecified) colors.eventDot else it }
            DayCell(
                day = dayCellState(day.date, day.position, state, selection, range, dots),
                colors = colors,
                strings = strings,
                locale = locale,
                onClick = { date -> onDateClick(date, state.select(date)) },
                modifier = cellModifier,
                content = dayContent,
            )
        }
    }
}
