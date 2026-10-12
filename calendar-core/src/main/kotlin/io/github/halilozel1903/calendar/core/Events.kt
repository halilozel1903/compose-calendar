package io.github.halilozel1903.calendar.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.TreeMap

/**
 * A ready-made event type. Any type works with [EventIndex]; this one covers the common case.
 *
 * [end] is exclusive: an event from 22:00 to 00:00 the next day belongs to one day only. An all-day event runs from
 * the start of its first day to the start of the day after its last day; use [allDay] to make one.
 *
 * @property color an ARGB color such as `0xFF2B5BD7`, or null for the calendar's default dot color.
 */
public data class CalendarEvent(
    val id: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val isAllDay: Boolean = false,
    val location: String? = null,
    val color: Long? = null,
) {
    init {
        require(!end.isBefore(start)) { "end ($end) is before start ($start)" }
    }

    /** The first day the event is on. */
    val firstDay: LocalDate get() = start.toLocalDate()

    /** The last day the event is on; an end at midnight does not count the day it ends on. */
    val lastDay: LocalDate
        get() = if (end.toLocalTime() == LocalTime.MIDNIGHT && end.isAfter(start)) {
            end.toLocalDate().minusDays(1)
        } else {
            end.toLocalDate()
        }

    public companion object {
        /** An all-day event from [firstDay] to [lastDay], both included. */
        public fun allDay(
            id: String,
            title: String,
            firstDay: LocalDate,
            lastDay: LocalDate = firstDay,
            location: String? = null,
            color: Long? = null,
        ): CalendarEvent = CalendarEvent(
            id = id,
            title = title,
            start = firstDay.atStartOfDay(),
            end = lastDay.plusDays(1).atStartOfDay(),
            isAllDay = true,
            location = location,
            color = color,
        )

        /** All-day events first, then by start time, then by title. */
        public val DayOrder: Comparator<CalendarEvent> =
            compareByDescending<CalendarEvent> { it.isAllDay }.thenBy { it.start }.thenBy { it.title }
    }
}

/**
 * One day of an event. A three-day event has three occurrences with [dayNumber] 1, 2 and 3 of [totalDays] 3.
 */
public data class EventOccurrence<out T>(
    val event: T,
    val date: LocalDate,
    val dayNumber: Int,
    val totalDays: Int,
) {
    /** The event starts on [date]. */
    val isFirstDay: Boolean get() = dayNumber == 1

    /** The event ends on [date]. */
    val isLastDay: Boolean get() = dayNumber == totalDays

    /** The event spans more than one day. */
    val isMultiDay: Boolean get() = totalDays > 1
}

/**
 * Events grouped by day: what to show as dots under a date and in an agenda. Multi-day events appear on each of
 * their days. Build one with [of] (any event type) or [ofEvents] ([CalendarEvent]); it is immutable, so build a new
 * one when the events change (and `remember` it in Compose).
 */
public class EventIndex<T> private constructor(
    private val byDay: TreeMap<LocalDate, List<EventOccurrence<T>>>,
) {
    /** Days that have at least one event, in order. */
    public val dates: Set<LocalDate> get() = byDay.keys

    /** True when no day has an event. */
    public val isEmpty: Boolean get() = byDay.isEmpty()

    /** The events on [date], in the index's order. */
    public fun eventsOn(date: LocalDate): List<T> = byDay[date]?.map { it.event } ?: emptyList()

    /** The occurrences on [date], with their day number for multi-day events. */
    public fun occurrencesOn(date: LocalDate): List<EventOccurrence<T>> = byDay[date] ?: emptyList()

    /** How many events are on [date]. */
    public fun countOn(date: LocalDate): Int = byDay[date]?.size ?: 0

    /** True when [date] has at least one event. */
    public fun hasEvents(date: LocalDate): Boolean = byDay.containsKey(date)

    /** The days from [from] to [to] (both included) that have events, with their occurrences. */
    public fun between(from: LocalDate, to: LocalDate): Map<LocalDate, List<EventOccurrence<T>>> =
        if (to.isBefore(from)) emptyMap() else byDay.subMap(from, true, to, true)

    /** The first day on or after [date] that has events, or null. */
    public fun nextDateWithEvents(date: LocalDate): LocalDate? = byDay.ceilingKey(date)

    public companion object {
        /**
         * Groups [events] by day. [firstDay] and [lastDay] give the days an event is on (both included; a [lastDay]
         * before [firstDay] counts as [firstDay]). Events on the same day keep the order of [comparator], or the
         * order of [events] when it is null. [maxDaysPerEvent] caps how many days one event can fill, so a
         * mistaken end date years away does not create a huge index.
         */
        public fun <T> of(
            events: Iterable<T>,
            firstDay: (T) -> LocalDate,
            lastDay: (T) -> LocalDate = firstDay,
            comparator: Comparator<in T>? = null,
            maxDaysPerEvent: Int = 366,
        ): EventIndex<T> {
            require(maxDaysPerEvent >= 1) { "maxDaysPerEvent must be at least 1, got $maxDaysPerEvent" }
            val sorted = if (comparator != null) events.sortedWith(comparator) else events.toList()
            val map = TreeMap<LocalDate, MutableList<EventOccurrence<T>>>()
            for (event in sorted) {
                val first = firstDay(event)
                val last = lastDay(event).let { if (it.isBefore(first)) first else it }
                val total = (CalendarMath.daysBetween(first, last) + 1).coerceAtMost(maxDaysPerEvent.toLong()).toInt()
                for (day in 0 until total) {
                    val date = first.plusDays(day.toLong())
                    map.getOrPut(date) { mutableListOf() }.add(EventOccurrence(event, date, day + 1, total))
                }
            }
            val result = TreeMap<LocalDate, List<EventOccurrence<T>>>()
            for ((date, list) in map) result[date] = list.toList()
            return EventIndex(result)
        }

        /** Groups [CalendarEvent]s by day: all-day events first, then by start time. */
        public fun ofEvents(events: Iterable<CalendarEvent>): EventIndex<CalendarEvent> =
            of(events, CalendarEvent::firstDay, CalendarEvent::lastDay, CalendarEvent.DayOrder)

        /** An index without events. */
        public fun <T> empty(): EventIndex<T> = EventIndex(TreeMap<LocalDate, List<EventOccurrence<T>>>())
    }
}
