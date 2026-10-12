package io.github.halilozel1903.calendar.core

import java.time.LocalDate

/** How taps select dates. */
public enum class SelectionMode {
    /** Taps select nothing. */
    None,

    /** One date; tapping it again clears it. */
    Single,

    /** Any number of dates; a tap toggles a date. */
    Multiple,

    /** A start and an end date: the first tap sets the start, the second the end. */
    Range,
}

/** The selected dates. */
public sealed interface CalendarSelection {

    /** True when [date] is selected (for a range: between start and end, both included). */
    public operator fun contains(date: LocalDate): Boolean

    /** Nothing selected. */
    public data object None : CalendarSelection {
        override fun contains(date: LocalDate): Boolean = false
    }

    /** One selected date. */
    public data class Single(val date: LocalDate) : CalendarSelection {
        override fun contains(date: LocalDate): Boolean = date == this.date
    }

    /** Several selected dates, in no particular order. */
    public data class Multiple(val dates: Set<LocalDate>) : CalendarSelection {
        override fun contains(date: LocalDate): Boolean = date in dates

        /** The dates in calendar order. */
        public val sorted: List<LocalDate> get() = dates.sorted()
    }

    /**
     * A range from [start] to [end], both included. While the user is picking, [end] is null and only the start
     * is selected.
     */
    public data class Range(val start: LocalDate, val end: LocalDate? = null) : CalendarSelection {
        init {
            require(end == null || !end.isBefore(start)) { "end ($end) is before start ($start)" }
        }

        /** True once the end is picked. */
        public val isComplete: Boolean get() = end != null

        /** Days in the range, both ends included; 1 while only the start is picked. */
        public val lengthInDays: Int get() = CalendarMath.daysBetween(start, end ?: start).toInt() + 1

        /** Nights between start and end (days minus one), handy for trips and bookings. */
        public val nights: Int get() = lengthInDays - 1

        /** Every date of the range, start to end. */
        public val dates: List<LocalDate> get() = List(lengthInDays) { start.plusDays(it.toLong()) }

        override fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end ?: start)
    }
}

/** Why a tap did not change the selection. */
public enum class SelectionRejection {
    /** The selection mode is [SelectionMode.None]. */
    SelectionDisabled,

    /** The date is disabled by [SelectionRules]. */
    DateDisabled,

    /** The date is before [SelectionRules.minDate] or after [SelectionRules.maxDate]. */
    OutOfBounds,

    /** The range would be shorter than [SelectionRules.minRangeDays]. */
    RangeTooShort,

    /** The range would be longer than [SelectionRules.maxRangeDays]. */
    RangeTooLong,

    /** The range would include a disabled date and [SelectionRules.allowDisabledInRange] is false. */
    RangeContainsDisabledDate,

    /** [SelectionRules.maxSelectedDates] dates are already selected. */
    TooManyDates,
}

/** What a tap did. */
public sealed interface SelectionResult {
    /** The selection after the tap; the old one when the tap was rejected. */
    public val selection: CalendarSelection

    /** The tap changed the selection to [selection]. */
    public data class Changed(override val selection: CalendarSelection) : SelectionResult

    /** The tap was rejected for [reason]; [selection] is unchanged. */
    public data class Rejected(
        override val selection: CalendarSelection,
        val date: LocalDate,
        val reason: SelectionRejection,
    ) : SelectionResult
}

/**
 * Which dates can be selected and how long a range may be.
 *
 * @property minDate the first selectable date, or null for no limit.
 * @property maxDate the last selectable date, or null for no limit.
 * @property disabledDates dates that can't be selected.
 * @property isDisabled any other rule, for example "weekends" or "fully booked"; checked with [disabledDates].
 * @property minRangeDays the shortest range in days, both ends included (2 means at least one night).
 * @property maxRangeDays the longest range in days, both ends included, or null for no limit.
 * @property allowDisabledInRange whether a range may span disabled dates (true for "closed on Mondays",
 * false for "already booked").
 * @property maxSelectedDates how many dates [SelectionMode.Multiple] may hold, or null for no limit.
 */
public class SelectionRules(
    public val minDate: LocalDate? = null,
    public val maxDate: LocalDate? = null,
    public val disabledDates: Set<LocalDate> = emptySet(),
    public val isDisabled: (LocalDate) -> Boolean = { false },
    public val minRangeDays: Int = 1,
    public val maxRangeDays: Int? = null,
    public val allowDisabledInRange: Boolean = false,
    public val maxSelectedDates: Int? = null,
) {
    init {
        require(minDate == null || maxDate == null || !maxDate.isBefore(minDate)) {
            "maxDate ($maxDate) is before minDate ($minDate)"
        }
        require(minRangeDays >= 1) { "minRangeDays must be at least 1, got $minRangeDays" }
        require(maxRangeDays == null || maxRangeDays >= minRangeDays) {
            "maxRangeDays ($maxRangeDays) is smaller than minRangeDays ($minRangeDays)"
        }
        require(maxSelectedDates == null || maxSelectedDates >= 1) {
            "maxSelectedDates must be at least 1, got $maxSelectedDates"
        }
    }

    /** True when [date] is between [minDate] and [maxDate]. */
    public fun isInBounds(date: LocalDate): Boolean =
        (minDate == null || !date.isBefore(minDate)) && (maxDate == null || !date.isAfter(maxDate))

    /** True when [date] can be selected: in bounds and not disabled. */
    public fun isSelectable(date: LocalDate): Boolean = isInBounds(date) && !isDateDisabled(date)

    /** True when [date] is disabled by [disabledDates] or [isDisabled] (bounds are not checked). */
    public fun isDateDisabled(date: LocalDate): Boolean = date in disabledDates || isDisabled(date)

    /** Why [date] can't be selected, or null when it can. */
    public fun rejectionFor(date: LocalDate): SelectionRejection? = when {
        !isInBounds(date) -> SelectionRejection.OutOfBounds
        isDateDisabled(date) -> SelectionRejection.DateDisabled
        else -> null
    }

    /** Why [start]..[end] can't be a range, or null when it can. Both ends must already be selectable. */
    public fun rangeRejectionFor(start: LocalDate, end: LocalDate): SelectionRejection? {
        val days = CalendarMath.daysBetween(start, end) + 1
        return when {
            days < minRangeDays -> SelectionRejection.RangeTooShort
            maxRangeDays != null && days > maxRangeDays -> SelectionRejection.RangeTooLong
            !allowDisabledInRange && firstDisabledBetween(start, end) != null ->
                SelectionRejection.RangeContainsDisabledDate
            else -> null
        }
    }

    /** The first disabled date strictly between [start] and [end], or null. */
    public fun firstDisabledBetween(start: LocalDate, end: LocalDate): LocalDate? {
        var date = start.plusDays(1)
        while (date.isBefore(end)) {
            if (isDateDisabled(date)) return date
            date = date.plusDays(1)
        }
        return null
    }

    public companion object {
        /** No limits. */
        public val Default: SelectionRules = SelectionRules()
    }
}

/**
 * Turns taps into selections for a [mode] under [rules]. Pure and stateless: give it the current selection and the
 * tapped date, get the new selection back.
 *
 * Range picking: the first tap sets the start. The second tap sets the end when it is on or after the start and the
 * range passes the rules; a tap before the start moves the start there instead. A tap after a complete range starts a
 * new one. When a second tap would make a range that is too short, too long or crosses a disabled date it is
 * rejected and the start stays, so the user can pick another end.
 */
public class SelectionModel(
    public val mode: SelectionMode,
    public val rules: SelectionRules = SelectionRules.Default,
) {

    /** The selection after tapping [date]. */
    public fun onDateTapped(current: CalendarSelection, date: LocalDate): SelectionResult {
        if (mode == SelectionMode.None) return SelectionResult.Rejected(current, date, SelectionRejection.SelectionDisabled)
        rules.rejectionFor(date)?.let { return SelectionResult.Rejected(current, date, it) }
        return when (mode) {
            SelectionMode.None -> SelectionResult.Rejected(current, date, SelectionRejection.SelectionDisabled)
            SelectionMode.Single -> SelectionResult.Changed(
                if (current is CalendarSelection.Single && current.date == date) {
                    CalendarSelection.None
                } else {
                    CalendarSelection.Single(date)
                },
            )
            SelectionMode.Multiple -> tapMultiple(current, date)
            SelectionMode.Range -> tapRange(current, date)
        }
    }

    private fun tapMultiple(current: CalendarSelection, date: LocalDate): SelectionResult {
        val dates = when (current) {
            is CalendarSelection.Multiple -> current.dates
            is CalendarSelection.Single -> setOf(current.date)
            is CalendarSelection.Range -> current.dates.toSet()
            CalendarSelection.None -> emptySet()
        }
        if (date in dates) {
            val remaining = dates - date
            return SelectionResult.Changed(if (remaining.isEmpty()) CalendarSelection.None else CalendarSelection.Multiple(remaining))
        }
        val max = rules.maxSelectedDates
        if (max != null && dates.size >= max) {
            return SelectionResult.Rejected(current, date, SelectionRejection.TooManyDates)
        }
        return SelectionResult.Changed(CalendarSelection.Multiple(dates + date))
    }

    private fun tapRange(current: CalendarSelection, date: LocalDate): SelectionResult {
        val pending = current as? CalendarSelection.Range
        if (pending == null || pending.isComplete || date.isBefore(pending.start)) {
            return SelectionResult.Changed(startRange(date))
        }
        rules.rangeRejectionFor(pending.start, date)?.let { return SelectionResult.Rejected(current, date, it) }
        return SelectionResult.Changed(CalendarSelection.Range(pending.start, date))
    }

    // A one-day range is complete as soon as its start is picked when ranges may be one day long only.
    private fun startRange(date: LocalDate): CalendarSelection.Range =
        if (rules.maxRangeDays == 1) CalendarSelection.Range(date, date) else CalendarSelection.Range(date)

    /**
     * Checks a selection set from code against the mode and the rules, for example one restored after a rules
     * change. Returns [CalendarSelection.None] for anything that breaks them.
     */
    public fun sanitize(selection: CalendarSelection): CalendarSelection = when (selection) {
        CalendarSelection.None -> selection
        is CalendarSelection.Single ->
            if (mode == SelectionMode.Single && rules.isSelectable(selection.date)) selection else CalendarSelection.None
        is CalendarSelection.Multiple -> {
            val valid = selection.dates.filter(rules::isSelectable).toSet()
            val max = rules.maxSelectedDates
            when {
                mode != SelectionMode.Multiple || valid.isEmpty() -> CalendarSelection.None
                max != null && valid.size > max -> CalendarSelection.Multiple(valid.sorted().take(max).toSet())
                else -> CalendarSelection.Multiple(valid)
            }
        }
        is CalendarSelection.Range -> {
            val end = selection.end
            when {
                mode != SelectionMode.Range || !rules.isSelectable(selection.start) -> CalendarSelection.None
                end == null -> selection
                !rules.isSelectable(end) || rules.rangeRejectionFor(selection.start, end) != null -> CalendarSelection.None
                else -> selection
            }
        }
    }
}

/**
 * Encodes a selection as a list of ISO dates and back, for saved instance state.
 * The first entry names the kind: `none`, `single`, `multiple`, `range`.
 */
public object SelectionCodec {

    /** [selection] as strings. */
    public fun encode(selection: CalendarSelection): List<String> = when (selection) {
        CalendarSelection.None -> listOf("none")
        is CalendarSelection.Single -> listOf("single", selection.date.toString())
        is CalendarSelection.Multiple -> listOf("multiple") + selection.sorted.map(LocalDate::toString)
        is CalendarSelection.Range -> listOfNotNull("range", selection.start.toString(), selection.end?.toString())
    }

    /** The selection [encoded] by [encode]; [CalendarSelection.None] for anything it can't read. */
    public fun decode(encoded: List<String>): CalendarSelection = runCatching {
        val dates = encoded.drop(1).map(LocalDate::parse)
        when (encoded.firstOrNull()) {
            "single" -> CalendarSelection.Single(dates.single())
            "multiple" -> if (dates.isEmpty()) CalendarSelection.None else CalendarSelection.Multiple(dates.toSet())
            "range" -> CalendarSelection.Range(dates[0], dates.getOrNull(1))
            else -> CalendarSelection.None
        }
    }.getOrDefault(CalendarSelection.None)
}
