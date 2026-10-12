package io.github.halilozel1903.calendar.core

import java.time.LocalDate

/** One day of an agenda: a header and the day's events, which may be empty. */
public data class AgendaDay<out T>(
    val date: LocalDate,
    val occurrences: List<EventOccurrence<T>>,
) {
    /** True when the day has no events. */
    val isEmpty: Boolean get() = occurrences.isEmpty()
}

/** Builds agendas: days with their events, ready for a list with sticky day headers. */
public object Agenda {

    /**
     * The days from [from] to [to] (both included) with their events from [index]. With [includeEmptyDays] every
     * day is listed (an empty day can show "No events"); otherwise only days with events. Days in [alwaysInclude]
     * (usually today) are listed even when empty, if they are in the range.
     */
    public fun <T> build(
        index: EventIndex<T>,
        from: LocalDate,
        to: LocalDate,
        includeEmptyDays: Boolean = false,
        alwaysInclude: Set<LocalDate> = emptySet(),
    ): List<AgendaDay<T>> {
        if (to.isBefore(from)) return emptyList()
        if (!includeEmptyDays) {
            val withEvents = index.between(from, to)
            val extra = alwaysInclude.filter { it !in withEvents && !it.isBefore(from) && !it.isAfter(to) }
            return (withEvents.keys + extra).sorted().map { AgendaDay(it, index.occurrencesOn(it)) }
        }
        val days = CalendarMath.daysBetween(from, to).toInt() + 1
        return List(days) { offset ->
            val date = from.plusDays(offset.toLong())
            AgendaDay(date, index.occurrencesOn(date))
        }
    }

    /**
     * The number of list items [day] takes in an agenda list: its header plus one item per event, or plus one
     * "no events" item when it is empty.
     */
    public fun itemCount(day: AgendaDay<*>): Int = 1 + maxOf(1, day.occurrences.size)

    /**
     * The list index of the header of [date], or of the first day after it when [date] is not listed. Returns the
     * index after the last item when every day is before [date]. Matches the items of `AgendaList`.
     */
    public fun headerIndexOf(days: List<AgendaDay<*>>, date: LocalDate): Int {
        var index = 0
        for (day in days) {
            if (!day.date.isBefore(date)) return index
            index += itemCount(day)
        }
        return index
    }
}
