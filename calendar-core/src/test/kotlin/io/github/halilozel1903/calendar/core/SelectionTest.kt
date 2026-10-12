package io.github.halilozel1903.calendar.core

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SelectionTest {
    private fun d(day: Int, month: Int = 10): LocalDate = LocalDate.of(2026, month, day)

    private fun SelectionModel.tap(vararg days: Int): CalendarSelection =
        days.fold<CalendarSelection>(CalendarSelection.None) { current, day -> onDateTapped(current, d(day)).selection }

    @Test
    fun singleSelectsAndTogglesOff() {
        val model = SelectionModel(SelectionMode.Single)
        assertEquals(CalendarSelection.Single(d(14)), model.tap(14))
        assertEquals(CalendarSelection.Single(d(15)), model.tap(14, 15))
        assertEquals(CalendarSelection.None, model.tap(14, 14))
    }

    @Test
    fun noneRejectsEveryTap() {
        val result = SelectionModel(SelectionMode.None).onDateTapped(CalendarSelection.None, d(1))
        assertEquals(SelectionRejection.SelectionDisabled, assertIs<SelectionResult.Rejected>(result).reason)
    }

    @Test
    fun multipleTogglesAndHonoursTheLimit() {
        val model = SelectionModel(SelectionMode.Multiple, SelectionRules(maxSelectedDates = 2))
        assertEquals(CalendarSelection.Multiple(setOf(d(1), d(3))), model.tap(1, 3))
        assertEquals(CalendarSelection.Multiple(setOf(d(3))), model.tap(1, 3, 1))
        assertEquals(CalendarSelection.None, model.tap(1, 1))
        val full = model.tap(1, 3)
        val rejected = model.onDateTapped(full, d(5))
        assertEquals(SelectionRejection.TooManyDates, assertIs<SelectionResult.Rejected>(rejected).reason)
        assertEquals(full, rejected.selection)
        assertEquals(listOf(d(1), d(3)), (full as CalendarSelection.Multiple).sorted)
    }

    @Test
    fun rangeIsPickedWithTwoTaps() {
        val model = SelectionModel(SelectionMode.Range)
        val pending = model.tap(14)
        assertEquals(CalendarSelection.Range(d(14), null), pending)
        assertFalse((pending as CalendarSelection.Range).isComplete)
        val range = model.tap(14, 19) as CalendarSelection.Range
        assertEquals(CalendarSelection.Range(d(14), d(19)), range)
        assertEquals(6, range.lengthInDays)
        assertEquals(5, range.nights)
        assertTrue(d(16) in range)
        assertFalse(d(20) in range)
        assertEquals(6, range.dates.size)
    }

    @Test
    fun tapBeforeTheStartMovesTheStartAndATapAfterACompleteRangeStartsOver() {
        val model = SelectionModel(SelectionMode.Range)
        assertEquals(CalendarSelection.Range(d(10)), model.tap(14, 10))
        assertEquals(CalendarSelection.Range(d(25)), model.tap(14, 19, 25))
    }

    @Test
    fun rangeAcrossMonths() {
        val model = SelectionModel(SelectionMode.Range)
        val range = model.onDateTapped(CalendarSelection.Range(d(29)), d(2, 11)).selection as CalendarSelection.Range
        assertEquals(5, range.lengthInDays)
    }

    @Test
    fun minAndMaxLength() {
        val model = SelectionModel(SelectionMode.Range, SelectionRules(minRangeDays = 3, maxRangeDays = 8))
        val tooShort = model.onDateTapped(CalendarSelection.Range(d(14)), d(15))
        assertEquals(SelectionRejection.RangeTooShort, assertIs<SelectionResult.Rejected>(tooShort).reason)
        assertEquals(CalendarSelection.Range(d(14)), tooShort.selection)
        val tooLong = model.onDateTapped(CalendarSelection.Range(d(14)), d(22))
        assertEquals(SelectionRejection.RangeTooLong, assertIs<SelectionResult.Rejected>(tooLong).reason)
        assertIs<SelectionResult.Changed>(model.onDateTapped(CalendarSelection.Range(d(14)), d(21)))
        assertIs<SelectionResult.Changed>(model.onDateTapped(CalendarSelection.Range(d(14)), d(16)))
    }

    @Test
    fun oneDayRangeCompletesOnTheFirstTapWhenThatIsTheMaximum() {
        val model = SelectionModel(SelectionMode.Range, SelectionRules(maxRangeDays = 1))
        assertEquals(CalendarSelection.Range(d(14), d(14)), model.tap(14))
        // With the default rules tapping the start again makes a one-day range.
        assertEquals(CalendarSelection.Range(d(14), d(14)), SelectionModel(SelectionMode.Range).tap(14, 14))
    }

    @Test
    fun disabledDatesAndBounds() {
        val rules = SelectionRules(
            minDate = d(5),
            maxDate = d(30),
            disabledDates = setOf(d(17)),
            isDisabled = { it.dayOfWeek == DayOfWeek.SUNDAY },
        )
        val model = SelectionModel(SelectionMode.Single, rules)
        assertEquals(SelectionRejection.OutOfBounds, (model.onDateTapped(CalendarSelection.None, d(4)) as SelectionResult.Rejected).reason)
        assertEquals(SelectionRejection.OutOfBounds, (model.onDateTapped(CalendarSelection.None, d(31)) as SelectionResult.Rejected).reason)
        assertEquals(SelectionRejection.DateDisabled, (model.onDateTapped(CalendarSelection.None, d(17)) as SelectionResult.Rejected).reason)
        // 18 October 2026 is a Sunday.
        assertEquals(SelectionRejection.DateDisabled, (model.onDateTapped(CalendarSelection.None, d(18)) as SelectionResult.Rejected).reason)
        assertTrue(rules.isSelectable(d(14)))
        assertFalse(rules.isSelectable(d(18)))
    }

    @Test
    fun rangeOverADisabledDate() {
        val booked = SelectionRules(disabledDates = setOf(d(17)))
        val strict = SelectionModel(SelectionMode.Range, booked)
        val result = strict.onDateTapped(CalendarSelection.Range(d(14)), d(20))
        assertEquals(SelectionRejection.RangeContainsDisabledDate, assertIs<SelectionResult.Rejected>(result).reason)
        assertEquals(d(17), booked.firstDisabledBetween(d(14), d(20)))

        val lenient = SelectionModel(SelectionMode.Range, SelectionRules(disabledDates = setOf(d(17)), allowDisabledInRange = true))
        assertEquals(CalendarSelection.Range(d(14), d(20)), lenient.onDateTapped(CalendarSelection.Range(d(14)), d(20)).selection)
    }

    @Test
    fun sanitizeDropsSelectionsThatBreakTheRules() {
        val rules = SelectionRules(disabledDates = setOf(d(17)), maxRangeDays = 5)
        val range = SelectionModel(SelectionMode.Range, rules)
        assertEquals(CalendarSelection.None, range.sanitize(CalendarSelection.Range(d(14), d(20))))
        assertEquals(CalendarSelection.Range(d(18), d(20)), range.sanitize(CalendarSelection.Range(d(18), d(20))))
        assertEquals(CalendarSelection.None, range.sanitize(CalendarSelection.Single(d(1))))
        val multiple = SelectionModel(SelectionMode.Multiple, SelectionRules(disabledDates = setOf(d(17)), maxSelectedDates = 2))
        assertEquals(
            CalendarSelection.Multiple(setOf(d(1), d(2))),
            multiple.sanitize(CalendarSelection.Multiple(setOf(d(3), d(17), d(2), d(1)))),
        )
    }

    @Test
    fun codecRoundTrips() {
        val selections = listOf(
            CalendarSelection.None,
            CalendarSelection.Single(d(14)),
            CalendarSelection.Multiple(setOf(d(2), d(1))),
            CalendarSelection.Range(d(14)),
            CalendarSelection.Range(d(14), d(19)),
        )
        for (selection in selections) {
            assertEquals(selection, SelectionCodec.decode(SelectionCodec.encode(selection)))
        }
        assertEquals(CalendarSelection.None, SelectionCodec.decode(listOf("range", "not a date")))
        assertEquals(CalendarSelection.None, SelectionCodec.decode(emptyList()))
    }

    @Test
    fun invalidRulesAreRejected() {
        assertFailsWithIllegalArgument { SelectionRules(minRangeDays = 0) }
        assertFailsWithIllegalArgument { SelectionRules(minRangeDays = 4, maxRangeDays = 3) }
        assertFailsWithIllegalArgument { SelectionRules(minDate = d(10), maxDate = d(9)) }
        assertFailsWithIllegalArgument { CalendarSelection.Range(d(10), d(9)) }
    }

    private fun assertFailsWithIllegalArgument(block: () -> Unit) {
        kotlin.test.assertFailsWith<IllegalArgumentException> { block() }
    }
}
