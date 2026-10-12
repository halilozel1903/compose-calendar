package io.github.halilozel1903.calendar.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MonthGridTest {
    private val october = YearMonth.of(2026, 10)

    @Test
    fun mondayGridStartsOnTheMondayBeforeTheFirst() {
        // 1 October 2026 is a Thursday.
        val grid = CalendarMath.monthGrid(october, DayOfWeek.MONDAY)
        assertEquals(6, grid.weeks.size)
        assertEquals(LocalDate.of(2026, 9, 28), grid.firstVisibleDate)
        assertEquals(DayPosition.PreviousMonth, grid.days[0].position)
        assertEquals(DayPosition.InMonth, grid.days[3].position)
        assertEquals(LocalDate.of(2026, 10, 1), grid.days[3].date)
        assertEquals(LocalDate.of(2026, 11, 8), grid.lastVisibleDate)
        assertEquals(DayPosition.NextMonth, grid.days.last().position)
        assertTrue(grid.weeks.all { it.days.first().date.dayOfWeek == DayOfWeek.MONDAY })
    }

    @Test
    fun sundayGridStartsOnSunday() {
        val grid = CalendarMath.monthGrid(october, DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 9, 27), grid.firstVisibleDate)
        assertEquals(DayOfWeek.SUNDAY, grid.firstVisibleDate.dayOfWeek)
        assertEquals(31, grid.days.count { it.isInMonth })
    }

    @Test
    fun saturdayStartAndMonthStartingOnTheFirstDayHasNoLeadingDays() {
        // 1 August 2026 is a Saturday.
        val grid = CalendarMath.monthGrid(YearMonth.of(2026, 8), DayOfWeek.SATURDAY, GridRows.Fit)
        assertEquals(LocalDate.of(2026, 8, 1), grid.firstVisibleDate)
        assertEquals(5, grid.weeks.size)
    }

    @Test
    fun fitRowsUsesOnlyTheRowsTheMonthNeeds() {
        // February 2027 starts on a Monday and has 28 days: exactly four Monday rows.
        val february = CalendarMath.monthGrid(YearMonth.of(2027, 2), DayOfWeek.MONDAY, GridRows.Fit)
        assertEquals(4, february.weeks.size)
        assertTrue(february.days.all { it.isInMonth })
        // August 2026 with Monday start needs six rows (Sat 1st, 31 days).
        assertEquals(6, CalendarMath.monthGrid(YearMonth.of(2026, 8), DayOfWeek.MONDAY, GridRows.Fit).weeks.size)
        // Six rows are kept for a pager even when four would do.
        assertEquals(6, CalendarMath.monthGrid(YearMonth.of(2027, 2), DayOfWeek.MONDAY, GridRows.Six).weeks.size)
    }

    @Test
    fun everyCellIsConsecutive() {
        val grid = CalendarMath.monthGrid(YearMonth.of(2024, 2), DayOfWeek.WEDNESDAY)
        grid.days.zipWithNext().forEach { (a, b) -> assertEquals(a.date.plusDays(1), b.date) }
        assertEquals(29, grid.days.count { it.isInMonth })
    }

    @Test
    fun weekdaysRotateFromTheFirstDay() {
        assertEquals(
            listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            CalendarMath.weekdays(DayOfWeek.SUNDAY),
        )
        assertEquals(DayOfWeek.SUNDAY, CalendarMath.firstDayOfWeek(Locale.US))
        assertEquals(DayOfWeek.MONDAY, CalendarMath.firstDayOfWeek(Locale.GERMANY))
    }

    @Test
    fun startAndEndOfWeek() {
        val wednesday = LocalDate.of(2026, 10, 14)
        assertEquals(LocalDate.of(2026, 10, 12), CalendarMath.startOfWeek(wednesday, DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 11), CalendarMath.startOfWeek(wednesday, DayOfWeek.SUNDAY))
        assertEquals(LocalDate.of(2026, 10, 18), CalendarMath.endOfWeek(wednesday, DayOfWeek.MONDAY))
        assertEquals(wednesday, CalendarMath.startOfWeek(wednesday, DayOfWeek.WEDNESDAY))
        val row = CalendarMath.weekOf(wednesday, DayOfWeek.MONDAY)
        assertTrue(wednesday in row)
        assertFalse(LocalDate.of(2026, 10, 19) in row)
    }

    @Test
    fun isoWeekNumbersAroundNewYear() {
        assertEquals(42, CalendarMath.isoWeekNumber(LocalDate.of(2026, 10, 14)))
        // 1 January 2027 is a Friday: it belongs to week 53 of 2026.
        assertEquals(53, CalendarMath.isoWeekNumber(LocalDate.of(2027, 1, 1)))
        assertEquals(2026, CalendarMath.isoWeekYear(LocalDate.of(2027, 1, 1)))
        // 29 December 2025 is a Monday in week 1 of 2026.
        assertEquals(1, CalendarMath.isoWeekNumber(LocalDate.of(2025, 12, 29)))
    }

    @Test
    fun rowWeekNumberUsesTheThursdayOfTheRow() {
        val monday = CalendarMath.monthGrid(october, DayOfWeek.MONDAY)
        assertEquals(listOf(40, 41, 42, 43, 44, 45), monday.weeks.map { it.weekNumber })
        // A Sunday row from 11 to 17 October holds Thursday the 15th: week 42.
        val sunday = CalendarMath.monthGrid(october, DayOfWeek.SUNDAY)
        assertEquals(42, sunday.weekOf(LocalDate.of(2026, 10, 11))!!.weekNumber)
        assertNull(sunday.weekOf(LocalDate.of(2026, 12, 1)))
    }

    @Test
    fun monthPagingMapsPagesAndMonths() {
        val paging = MonthPaging(YearMonth.of(2026, 1), YearMonth.of(2027, 12))
        assertEquals(24, paging.pageCount)
        assertEquals(9, paging.indexOf(october))
        assertEquals(october, paging.monthAt(9))
        assertEquals(YearMonth.of(2026, 1), paging.monthAt(-4))
        assertEquals(23, paging.indexOf(YearMonth.of(2030, 1)))
        assertTrue(october in paging)
        assertFalse(YearMonth.of(2025, 12) in paging)
        assertEquals(241, MonthPaging.around(october).pageCount)
    }

    @Test
    fun weekPagingCoversEveryWeekOfTheMonths() {
        val paging = WeekPaging.of(MonthPaging(october, october), DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 9, 28), paging.firstWeekStart)
        assertEquals(5, paging.pageCount)
        assertEquals(2, paging.indexOf(LocalDate.of(2026, 10, 14)))
        assertEquals(LocalDate.of(2026, 10, 12), paging.weekStartAt(2))
        assertEquals(LocalDate.of(2026, 10, 18), paging.weekAt(2).end)
        assertEquals(4, paging.indexOf(LocalDate.of(2027, 5, 1)))
    }
}
