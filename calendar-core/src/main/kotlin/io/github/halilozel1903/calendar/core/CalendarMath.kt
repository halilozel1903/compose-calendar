package io.github.halilozel1903.calendar.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.WeekFields
import java.util.Locale

/** Month grids, week rows and week numbers. */
public object CalendarMath {

    /** The seven days of the week in display order, starting at [firstDayOfWeek]. */
    public fun weekdays(firstDayOfWeek: DayOfWeek): List<DayOfWeek> = List(7) { firstDayOfWeek.plus(it.toLong()) }

    /** The first day of the week for [locale] (Sunday in the US, Monday in most of Europe). */
    public fun firstDayOfWeek(locale: Locale): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

    /** The first day of the week that holds [date]. */
    public fun startOfWeek(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate {
        val offset = (date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return date.minusDays(offset.toLong())
    }

    /** The last day of the week that holds [date]. */
    public fun endOfWeek(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
        startOfWeek(date, firstDayOfWeek).plusDays(6)

    /** The ISO 8601 week number of [date] (1 to 53; weeks start on Monday and week 1 holds the first Thursday). */
    public fun isoWeekNumber(date: LocalDate): Int = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    /** The ISO 8601 week-based year of [date], which differs from the calendar year around New Year. */
    public fun isoWeekYear(date: LocalDate): Int = date.get(IsoFields.WEEK_BASED_YEAR)

    /** The row of seven days that holds [date]. Every day counts as in month. */
    public fun weekOf(date: LocalDate, firstDayOfWeek: DayOfWeek): CalendarWeek {
        val start = startOfWeek(date, firstDayOfWeek)
        return CalendarWeek(List(7) { CalendarDay(start.plusDays(it.toLong()), DayPosition.InMonth) })
    }

    /**
     * The grid of [month]: rows of seven days starting on [firstDayOfWeek], the first row holding the 1st.
     * With [GridRows.Six] the grid always has six rows; with [GridRows.Fit] it ends with the row holding the
     * last day. Days of the neighbouring months are marked [DayPosition.PreviousMonth] or [DayPosition.NextMonth];
     * whether they are drawn ([OutsideDays]) is up to the UI, so the grid always has full rows.
     */
    public fun monthGrid(
        month: YearMonth,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
        rows: GridRows = GridRows.Six,
    ): MonthGrid {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        val gridStart = startOfWeek(first, firstDayOfWeek)
        val neededRows = (ChronoUnit.DAYS.between(gridStart, last).toInt() / 7) + 1
        val rowCount = if (rows == GridRows.Six) 6 else neededRows
        val weeks = List(rowCount) { row ->
            CalendarWeek(
                List(7) { column ->
                    val date = gridStart.plusDays((row * 7 + column).toLong())
                    val position = when {
                        date.isBefore(first) -> DayPosition.PreviousMonth
                        date.isAfter(last) -> DayPosition.NextMonth
                        else -> DayPosition.InMonth
                    }
                    CalendarDay(date, position)
                },
            )
        }
        return MonthGrid(month, firstDayOfWeek, weeks)
    }

    /** Whole days from [from] to [to]; negative when [to] is earlier. */
    public fun daysBetween(from: LocalDate, to: LocalDate): Long = ChronoUnit.DAYS.between(from, to)

    /** Whole months from [from] to [to]; negative when [to] is earlier. */
    public fun monthsBetween(from: YearMonth, to: YearMonth): Int = ChronoUnit.MONTHS.between(from, to).toInt()
}
