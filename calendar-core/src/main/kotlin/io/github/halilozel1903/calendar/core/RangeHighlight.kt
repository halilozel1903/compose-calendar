package io.github.halilozel1903.calendar.core

import java.time.LocalDate

/**
 * How one cell takes part in a range highlight drawn as a continuous pill. Cells in a row touch, so each in-range
 * cell paints a band across its full width and the pill looks like one shape; it is rounded where the range starts
 * or ends and where it wraps to the next row.
 */
public data class RangeCell(
    /** The cell is part of the range (start and end included). */
    val inRange: Boolean,
    /** The range starts on this cell. */
    val isStart: Boolean,
    /** The range ends on this cell. */
    val isEnd: Boolean,
    /** The band connects to the cell on the left (in layout direction: the previous day). */
    val connectsBefore: Boolean,
    /** The band connects to the cell on the right (in layout direction: the next day). */
    val connectsAfter: Boolean,
) {
    /** The cell is a range end point (start or end), usually drawn as a filled circle on top of the band. */
    val isEndPoint: Boolean get() = isStart || isEnd

    public companion object {
        /** A cell outside any range. */
        public val None: RangeCell = RangeCell(false, false, false, false, false)
    }
}

/** Works out the pill shape of a range across the cells of a grid. */
public object RangeHighlight {

    /**
     * The [RangeCell] for [date] under [selection]. [isRowStart] and [isRowEnd] say whether the cell is the first
     * or last visible cell of its row (or the neighbour is hidden, for example an outside day), so the pill is
     * rounded there. A range with only its start picked is a single end point.
     */
    public fun cell(
        date: LocalDate,
        selection: CalendarSelection,
        isRowStart: Boolean,
        isRowEnd: Boolean,
    ): RangeCell {
        val range = selection as? CalendarSelection.Range ?: return RangeCell.None
        if (date !in range) return RangeCell.None
        val end = range.end ?: range.start
        val isStart = date == range.start
        val isEnd = date == end
        return RangeCell(
            inRange = true,
            isStart = isStart,
            isEnd = isEnd,
            connectsBefore = !isStart && !isRowStart,
            connectsAfter = !isEnd && !isRowEnd,
        )
    }
}
