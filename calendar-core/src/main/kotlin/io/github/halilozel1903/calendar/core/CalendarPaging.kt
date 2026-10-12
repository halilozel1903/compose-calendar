package io.github.halilozel1903.calendar.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The months a pager can show, from [start] to [endInclusive], and the page index of each.
 */
public data class MonthPaging(
    val start: YearMonth,
    val endInclusive: YearMonth,
) {
    init {
        require(!endInclusive.isBefore(start)) { "endInclusive ($endInclusive) is before start ($start)" }
    }

    /** Number of pages. */
    val pageCount: Int get() = CalendarMath.monthsBetween(start, endInclusive) + 1

    /** The month on page [index], clamped to the range. */
    public fun monthAt(index: Int): YearMonth = start.plusMonths(index.coerceIn(0, pageCount - 1).toLong())

    /** The page of [month], clamped to the range. */
    public fun indexOf(month: YearMonth): Int = CalendarMath.monthsBetween(start, month).coerceIn(0, pageCount - 1)

    /** True when [month] has a page. */
    public operator fun contains(month: YearMonth): Boolean = !month.isBefore(start) && !month.isAfter(endInclusive)

    public companion object {
        /** [around] with [before] months before it and [after] months after it. */
        public fun around(around: YearMonth, before: Int = 120, after: Int = 120): MonthPaging =
            MonthPaging(around.minusMonths(before.toLong()), around.plusMonths(after.toLong()))
    }
}

/**
 * The weeks a pager can show: every week that holds a day from [start] to [endInclusive], each starting on
 * [firstDayOfWeek].
 */
public data class WeekPaging(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val firstDayOfWeek: DayOfWeek,
) {
    init {
        require(!endInclusive.isBefore(start)) { "endInclusive ($endInclusive) is before start ($start)" }
    }

    /** The first day of the first page. */
    val firstWeekStart: LocalDate get() = CalendarMath.startOfWeek(start, firstDayOfWeek)

    /** Number of pages. */
    val pageCount: Int
        get() = (CalendarMath.daysBetween(firstWeekStart, CalendarMath.startOfWeek(endInclusive, firstDayOfWeek)) / 7)
            .toInt() + 1

    /** The first day of the week on page [index], clamped to the range. */
    public fun weekStartAt(index: Int): LocalDate = firstWeekStart.plusWeeks(index.coerceIn(0, pageCount - 1).toLong())

    /** The week on page [index]. */
    public fun weekAt(index: Int): CalendarWeek = CalendarMath.weekOf(weekStartAt(index), firstDayOfWeek)

    /** The page of the week holding [date], clamped to the range. */
    public fun indexOf(date: LocalDate): Int =
        (CalendarMath.daysBetween(firstWeekStart, CalendarMath.startOfWeek(date, firstDayOfWeek)) / 7)
            .toInt()
            .coerceIn(0, pageCount - 1)

    public companion object {
        /** Every week of the months in [months]. */
        public fun of(months: MonthPaging, firstDayOfWeek: DayOfWeek): WeekPaging =
            WeekPaging(months.start.atDay(1), months.endInclusive.atEndOfMonth(), firstDayOfWeek)
    }
}
