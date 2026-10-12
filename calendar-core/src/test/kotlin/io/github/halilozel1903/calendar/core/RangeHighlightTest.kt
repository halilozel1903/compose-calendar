package io.github.halilozel1903.calendar.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RangeHighlightTest {
    private fun d(day: Int): LocalDate = LocalDate.of(2026, 10, day)
    private val range = CalendarSelection.Range(d(14), d(19))

    @Test
    fun startMiddleAndEnd() {
        val start = RangeHighlight.cell(d(14), range, isRowStart = false, isRowEnd = false)
        assertTrue(start.isStart && start.inRange && start.isEndPoint)
        assertFalse(start.connectsBefore)
        assertTrue(start.connectsAfter)

        val middle = RangeHighlight.cell(d(16), range, isRowStart = false, isRowEnd = false)
        assertTrue(middle.connectsBefore && middle.connectsAfter)
        assertFalse(middle.isEndPoint)

        val end = RangeHighlight.cell(d(19), range, isRowStart = true, isRowEnd = false)
        assertTrue(end.isEnd)
        assertFalse(end.connectsBefore)
        assertFalse(end.connectsAfter)
    }

    @Test
    fun pillIsRoundedWhereItWrapsToTheNextRow() {
        // Monday rows: the 18th is a Sunday (row end), the 19th a Monday (row start).
        val sunday = RangeHighlight.cell(d(18), range, isRowStart = false, isRowEnd = true)
        assertTrue(sunday.connectsBefore)
        assertFalse(sunday.connectsAfter)
    }

    @Test
    fun outsideTheRangeAndOtherSelections() {
        assertEquals(RangeCell.None, RangeHighlight.cell(d(20), range, false, false))
        assertEquals(RangeCell.None, RangeHighlight.cell(d(14), CalendarSelection.Single(d(14)), false, false))
        val pending = RangeHighlight.cell(d(14), CalendarSelection.Range(d(14)), false, false)
        assertTrue(pending.isStart && pending.isEnd)
        assertFalse(pending.connectsBefore || pending.connectsAfter)
    }
}
