package io.github.halilozel1903.calendar.core

import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventsAndAgendaTest {
    private fun d(day: Int): LocalDate = LocalDate.of(2026, 10, day)
    private fun t(day: Int, hour: Int, minute: Int = 0): LocalDateTime = LocalDateTime.of(2026, 10, day, hour, minute)

    private val standup = CalendarEvent("1", "Standup", t(14, 9, 30), t(14, 9, 45))
    private val lunch = CalendarEvent("2", "Lunch with Mia", t(14, 12), t(14, 13))
    private val offsite = CalendarEvent.allDay("3", "Team offsite", d(15), d(17))
    private val lateShow = CalendarEvent("4", "Late show", t(16, 22), t(17, 0))
    private val overnight = CalendarEvent("5", "Night train", t(20, 23), t(21, 7))
    private val index = EventIndex.ofEvents(listOf(lunch, offsite, standup, lateShow, overnight))

    @Test
    fun eventsAreGroupedByDayInDayOrder() {
        assertEquals(listOf(standup, lunch), index.eventsOn(d(14)))
        assertEquals(2, index.countOn(d(14)))
        assertTrue(index.hasEvents(d(15)))
        assertFalse(index.hasEvents(d(18)))
        assertEquals(emptyList(), index.eventsOn(d(18)))
    }

    @Test
    fun multiDayEventsAppearOnEachDay() {
        assertEquals(listOf(d(14), d(15), d(16), d(17), d(20), d(21)), index.dates.toList())
        val second = index.occurrencesOn(d(16)).first { it.event == offsite }
        assertEquals(2, second.dayNumber)
        assertEquals(3, second.totalDays)
        assertTrue(second.isMultiDay)
        assertFalse(second.isFirstDay || second.isLastDay)
        // All-day events come before timed ones.
        assertEquals(listOf(offsite, lateShow), index.eventsOn(d(16)))
    }

    @Test
    fun anEndAtMidnightDoesNotCountTheNextDay() {
        assertEquals(d(16), lateShow.lastDay)
        assertEquals(listOf(offsite), index.eventsOn(d(17)))
        assertEquals(d(21), overnight.lastDay)
        assertEquals(d(17), offsite.lastDay)
        assertTrue(offsite.isAllDay)
    }

    @Test
    fun genericEventsAndCaps() {
        data class Trip(val name: String, val from: LocalDate, val to: LocalDate)
        val trips = EventIndex.of(
            listOf(Trip("Lisbon", d(1), d(5)), Trip("Backwards", d(10), d(8)), Trip("Forever", d(20), d(20).plusYears(5))),
            firstDay = { it.from },
            lastDay = { it.to },
            maxDaysPerEvent = 10,
        )
        assertEquals(5, trips.occurrencesOn(d(3)).single().totalDays)
        assertEquals(1, trips.occurrencesOn(d(10)).single().totalDays)
        assertEquals(10, trips.occurrencesOn(d(20)).single().totalDays)
        assertFalse(trips.hasEvents(d(30)))
        assertTrue(EventIndex.empty<Trip>().isEmpty)
    }

    @Test
    fun betweenAndNext() {
        assertEquals(listOf(d(15), d(16), d(17)), index.between(d(15), d(17)).keys.toList())
        assertTrue(index.between(d(17), d(15)).isEmpty())
        assertEquals(d(20), index.nextDateWithEvents(d(18)))
        assertEquals(null, index.nextDateWithEvents(d(22)))
    }

    @Test
    fun agendaWithAndWithoutEmptyDays() {
        val busy = Agenda.build(index, d(13), d(18))
        assertEquals(listOf(d(14), d(15), d(16), d(17)), busy.map { it.date })

        val withToday = Agenda.build(index, d(13), d(18), alwaysInclude = setOf(d(13), d(30)))
        assertEquals(d(13), withToday.first().date)
        assertTrue(withToday.first().isEmpty)
        assertEquals(5, withToday.size)

        val all = Agenda.build(index, d(13), d(18), includeEmptyDays = true)
        assertEquals(6, all.size)
        assertTrue(all.last().isEmpty)
        assertTrue(Agenda.build(index, d(18), d(13)).isEmpty())
    }

    @Test
    fun headerIndexMatchesTheListItems() {
        val days = Agenda.build(index, d(13), d(18), includeEmptyDays = true)
        // 13: header + empty, 14: header + 2, 15: header + 1, 16: header + 2.
        assertEquals(0, Agenda.headerIndexOf(days, d(13)))
        assertEquals(2, Agenda.headerIndexOf(days, d(14)))
        assertEquals(5, Agenda.headerIndexOf(days, d(15)))
        assertEquals(7, Agenda.headerIndexOf(days, d(16)))
        assertEquals(days.sumOf { Agenda.itemCount(it) }, Agenda.headerIndexOf(days, d(30)))
        // A day that is not listed points at the next listed day.
        val busy = Agenda.build(index, d(13), d(21))
        assertEquals(Agenda.headerIndexOf(busy, d(20)), Agenda.headerIndexOf(busy, d(18)))
    }
}
