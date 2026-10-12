package io.github.halilozel1903.calendar.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Where a day of a month grid sits relative to the month the grid shows. */
public enum class DayPosition {
    /** A day of the month itself. */
    InMonth,

    /** A trailing day of the previous month that fills the first row. */
    PreviousMonth,

    /** A leading day of the next month that fills the last rows. */
    NextMonth,
}

/** What happens to the days of the previous and next month that share a row with the month's own days. */
public enum class OutsideDays {
    /** Show them (usually dimmed); they can be tapped and selected. */
    Show,

    /** Keep their cells empty, so only the month's own days are visible. The grid keeps its shape. */
    Hide,
}

/** How many rows a month grid has. */
public enum class GridRows {
    /** Always six rows, so every month has the same height and a pager does not jump while swiping. */
    Six,

    /** Only as many rows as the month needs (four to six). */
    Fit,
}

/** One cell of a month grid or a week row. */
public data class CalendarDay(
    val date: LocalDate,
    val position: DayPosition,
) {
    /** True for the month's own days. Week rows mark every day as [DayPosition.InMonth]. */
    val isInMonth: Boolean get() = position == DayPosition.InMonth
}

/** Seven consecutive days, starting on the grid's first day of the week. */
public data class CalendarWeek(
    val days: List<CalendarDay>,
) {
    init {
        require(days.size == 7) { "A week has 7 days, got ${days.size}" }
    }

    /** The first day of the row. */
    val start: LocalDate get() = days.first().date

    /** The last day of the row. */
    val end: LocalDate get() = days.last().date

    /**
     * The ISO 8601 week number of the row. A row always holds exactly one Thursday, and the ISO week is the week of
     * that Thursday, so the number is exact for rows starting on Monday and the usual convention for other starts.
     */
    val weekNumber: Int get() = CalendarMath.isoWeekNumber(days.first { it.date.dayOfWeek == DayOfWeek.THURSDAY }.date)

    /** True when [date] is one of the row's days. */
    public operator fun contains(date: LocalDate): Boolean = date in start..end
}

/** The weeks of one month as a calendar shows them. */
public data class MonthGrid(
    val month: YearMonth,
    val firstDayOfWeek: DayOfWeek,
    val weeks: List<CalendarWeek>,
) {
    /** All cells, row by row. */
    val days: List<CalendarDay> get() = weeks.flatMap { it.days }

    /** The first visible date, which may belong to the previous month. */
    val firstVisibleDate: LocalDate get() = weeks.first().start

    /** The last visible date, which may belong to the next month. */
    val lastVisibleDate: LocalDate get() = weeks.last().end

    /** The row that holds [date], or null when the grid does not show it. */
    public fun weekOf(date: LocalDate): CalendarWeek? = weeks.firstOrNull { date in it }
}
