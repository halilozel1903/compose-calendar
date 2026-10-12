package io.github.halilozel1903.calendar

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.halilozel1903.calendar.core.CalendarMath
import io.github.halilozel1903.calendar.core.CalendarSelection
import io.github.halilozel1903.calendar.core.CalendarWeek
import io.github.halilozel1903.calendar.core.MonthPaging
import io.github.halilozel1903.calendar.core.SelectionCodec
import io.github.halilozel1903.calendar.core.SelectionMode
import io.github.halilozel1903.calendar.core.SelectionModel
import io.github.halilozel1903.calendar.core.SelectionResult
import io.github.halilozel1903.calendar.core.SelectionRules
import io.github.halilozel1903.calendar.core.WeekPaging
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * State shared by [MonthCalendar] and [WeekCalendar]: the months that can be shown, which month and week are
 * visible, and the selection. Create it with [rememberCalendarState].
 *
 * The month pager and the week pager are independent, so a screen can show both (for example a month on a tablet
 * and a week strip on a phone) from one state; [WeekCalendar] follows a tap by keeping the tapped day's week in view.
 */
@Stable
public class CalendarState(
    /** The months the month pager can swipe through; the week pager covers the same days. */
    public val months: MonthPaging,
    /** The first column of every row. */
    public val firstDayOfWeek: DayOfWeek,
    /** The date marked as today. */
    public val today: LocalDate,
    selectionModel: SelectionModel,
    initialMonth: YearMonth = YearMonth.from(today),
    initialWeekDate: LocalDate = today,
    initialSelection: CalendarSelection = CalendarSelection.None,
) {
    /** How taps change [selection]. [rememberCalendarState] keeps it in sync with its mode and rules. */
    public var selectionModel: SelectionModel by mutableStateOf(selectionModel)

    /** The weeks the week pager can swipe through. */
    public val weeks: WeekPaging = WeekPaging.of(months, firstDayOfWeek)

    /** The pager behind [MonthCalendar]: one page per month of [months]. */
    public val monthPagerState: PagerState =
        PagerState(currentPage = months.indexOf(initialMonth)) { months.pageCount }

    /** The pager behind [WeekCalendar]: one page per week of [weeks]. */
    public val weekPagerState: PagerState =
        PagerState(currentPage = weeks.indexOf(initialWeekDate)) { weeks.pageCount }

    /** The selected dates. Set it to select from code; taps go through [select]. */
    public var selection: CalendarSelection by mutableStateOf(selectionModel.sanitize(initialSelection))

    /** The month the month pager has settled on (or is closest to while swiping). */
    public val visibleMonth: YearMonth by derivedStateOf { months.monthAt(monthPagerState.currentPage) }

    /** The week the week pager has settled on (or is closest to while swiping). */
    public val visibleWeek: CalendarWeek by derivedStateOf { weeks.weekAt(weekPagerState.currentPage) }

    /** The selection mode of [selectionModel]. */
    public val selectionMode: SelectionMode get() = selectionModel.mode

    /** The selection as a range, or null when it is not one. */
    public val selectedRange: CalendarSelection.Range? get() = selection as? CalendarSelection.Range

    /** The selected date in single mode, or null. */
    public val selectedDate: LocalDate? get() = (selection as? CalendarSelection.Single)?.date

    /** Applies a tap on [date] to [selection] under the mode and rules, and returns what happened. */
    public fun select(date: LocalDate): SelectionResult {
        val result = selectionModel.onDateTapped(selection, date)
        if (result is SelectionResult.Changed) selection = result.selection
        return result
    }

    /** Clears [selection]. */
    public fun clearSelection() {
        selection = CalendarSelection.None
    }

    /** True when [date] can be selected under the current rules. */
    public fun isSelectable(date: LocalDate): Boolean = selectionModel.rules.isSelectable(date)

    /** True when the month pager can go to an earlier month. */
    public val canScrollToPreviousMonth: Boolean get() = monthPagerState.currentPage > 0

    /** True when the month pager can go to a later month. */
    public val canScrollToNextMonth: Boolean get() = monthPagerState.currentPage < months.pageCount - 1

    /** Swipes the month pager to [month] (clamped to [months]). */
    public suspend fun animateScrollToMonth(month: YearMonth) {
        monthPagerState.animateScrollToPage(months.indexOf(month))
    }

    /** Jumps the month pager to [month] (clamped to [months]) without an animation. */
    public suspend fun scrollToMonth(month: YearMonth) {
        monthPagerState.scrollToPage(months.indexOf(month))
    }

    /** Swipes the week pager to the week holding [date]. */
    public suspend fun animateScrollToWeek(date: LocalDate) {
        weekPagerState.animateScrollToPage(weeks.indexOf(date))
    }

    /** Jumps the week pager to the week holding [date] without an animation. */
    public suspend fun scrollToWeek(date: LocalDate) {
        weekPagerState.scrollToPage(weeks.indexOf(date))
    }

    public companion object {
        /**
         * Saves the visible month, the visible week and the selection. The months, first day of week, today and
         * selection model are not saved; they come from the arguments again.
         */
        public fun saver(
            months: MonthPaging,
            firstDayOfWeek: DayOfWeek,
            today: LocalDate,
            selectionModel: SelectionModel,
        ): Saver<CalendarState, Any> = listSaver<CalendarState, String>(
            save = { state ->
                listOf(state.visibleMonth.toString(), state.visibleWeek.start.toString()) +
                    SelectionCodec.encode(state.selection)
            },
            restore = { saved ->
                CalendarState(
                    months = months,
                    firstDayOfWeek = firstDayOfWeek,
                    today = today,
                    selectionModel = selectionModel,
                    initialMonth = runCatching { YearMonth.parse(saved[0]) }.getOrDefault(YearMonth.from(today)),
                    initialWeekDate = runCatching { LocalDate.parse(saved[1]) }.getOrDefault(today),
                    initialSelection = SelectionCodec.decode(saved.drop(2)),
                )
            },
        )
    }
}

/**
 * Creates and remembers a [CalendarState] that survives configuration changes and process death.
 *
 * @param today the date marked as today. Pass a fixed date in tests and screenshots.
 * @param initialMonth the month shown first.
 * @param firstMonth the earliest month the pager can reach.
 * @param lastMonth the latest month the pager can reach.
 * @param firstDayOfWeek the first column; by default the one of the device's locale.
 * @param selectionMode how taps select dates.
 * @param selectionRules bounds, disabled dates and range lengths. Remember it (or keep it in a ViewModel) so it is
 * not a new object on every recomposition.
 * @param initialSelection the selection before any tap.
 */
@Composable
public fun rememberCalendarState(
    today: LocalDate = LocalDate.now(),
    initialMonth: YearMonth = YearMonth.from(today),
    firstMonth: YearMonth = initialMonth.minusMonths(DefaultMonthsAround),
    lastMonth: YearMonth = initialMonth.plusMonths(DefaultMonthsAround),
    firstDayOfWeek: DayOfWeek = remember { CalendarMath.firstDayOfWeek(Locale.getDefault()) },
    selectionMode: SelectionMode = SelectionMode.Single,
    selectionRules: SelectionRules = SelectionRules.Default,
    initialSelection: CalendarSelection = CalendarSelection.None,
): CalendarState {
    val months = remember(firstMonth, lastMonth) { MonthPaging(firstMonth, lastMonth) }
    val model = remember(selectionMode, selectionRules) { SelectionModel(selectionMode, selectionRules) }
    val state = rememberSaveable(
        months,
        firstDayOfWeek,
        saver = CalendarState.saver(months, firstDayOfWeek, today, model),
    ) {
        CalendarState(
            months = months,
            firstDayOfWeek = firstDayOfWeek,
            today = today,
            selectionModel = model,
            initialMonth = initialMonth,
            initialWeekDate = initialSelection.anchorDate() ?: today,
            initialSelection = initialSelection,
        )
    }
    SideEffect {
        if (state.selectionModel !== model) {
            state.selectionModel = model
            state.selection = model.sanitize(state.selection)
        }
    }
    return state
}

private const val DefaultMonthsAround = 120L
