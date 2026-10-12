package io.github.halilozel1903.calendar

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.core.CalendarSelection
import io.github.halilozel1903.calendar.core.CalendarWeek
import io.github.halilozel1903.calendar.core.SelectionResult
import java.time.LocalDate
import java.util.Locale

/**
 * A one-week strip that swipes between weeks, for compact screens and above a day's agenda.
 *
 * It uses the same [CalendarState] as [MonthCalendar] (selection, rules, today), with its own pager over
 * [CalendarState.weeks]. When the selection changes to a date outside the visible week (for example from a
 * [MonthCalendar] on the same screen, or from code), the strip swipes to that date's week.
 *
 * @param header drawn above the weekday labels; by default the month (or months) of the visible week, with arrows
 * to the previous and next week. Pass `{}` for none.
 */
@Composable
public fun WeekCalendar(
    state: CalendarState,
    modifier: Modifier = Modifier,
    onDateClick: (date: LocalDate, result: SelectionResult) -> Unit = { _, _ -> },
    eventDots: EventDots = NoEventDots,
    showWeekNumbers: Boolean = false,
    userScrollEnabled: Boolean = true,
    dayHeight: Dp = CalendarDefaults.WeekDayHeight,
    followSelection: Boolean = true,
    colors: CalendarColors = CalendarDefaults.colors(),
    strings: CalendarStrings = CalendarStrings(),
    locale: Locale = Locale.getDefault(),
    header: @Composable (week: CalendarWeek) -> Unit = { week ->
        CalendarDefaults.WeekHeader(state = state, week = week, colors = colors, strings = strings, locale = locale)
    },
    dayContent: (@Composable BoxScope.(DayCellState) -> Unit)? = null,
) {
    if (followSelection) {
        val anchor = state.selection.anchorDate()
        LaunchedEffect(state, anchor) {
            if (anchor != null && anchor !in state.visibleWeek) state.animateScrollToWeek(anchor)
        }
    }
    Column(modifier) {
        header(state.visibleWeek)
        WeekdayRow(
            firstDayOfWeek = state.firstDayOfWeek,
            colors = colors,
            locale = locale,
            leadingWidth = if (showWeekNumbers) CalendarDefaults.WeekNumberWidth else 0.dp,
        )
        HorizontalPager(
            state = state.weekPagerState,
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = userScrollEnabled,
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            key = { page -> state.weeks.weekStartAt(page).toString() },
        ) { page ->
            val week = remember(page, state.weeks) { state.weeks.weekAt(page) }
            WeekRow(
                week = week,
                state = state,
                selection = state.selection,
                hideOutside = false,
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

/** The date a week strip or agenda should show for [this] selection: the single date, the first date, the start. */
internal fun CalendarSelection.anchorDate(): LocalDate? = when (this) {
    CalendarSelection.None -> null
    is CalendarSelection.Single -> date
    is CalendarSelection.Multiple -> sorted.firstOrNull()
    is CalendarSelection.Range -> start
}
