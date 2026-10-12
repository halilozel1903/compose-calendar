package io.github.halilozel1903.calendar.sample

import io.github.halilozel1903.calendar.core.CalendarEvent
import java.time.LocalDate
import java.time.LocalDateTime

/** The Planner is fixed to this day, so the app and the README screenshots always look the same. */
val Today: LocalDate = LocalDate.of(2026, 10, 14)

/** Event colors, as ARGB like [CalendarEvent.color]. */
object EventColors {
    const val Work = 0xFF3D6FE0
    const val Personal = 0xFF2E9E6A
    const val Fitness = 0xFFE07A2E
    const val Travel = 0xFF8E5BD8
    const val Family = 0xFFD64F7A
}

/** Made-up events from September to November 2026. */
object PlannerData {

    private fun at(month: Int, day: Int, hour: Int, minute: Int = 0): LocalDateTime =
        LocalDateTime.of(2026, month, day, hour, minute)

    private fun timed(
        id: String,
        title: String,
        month: Int,
        day: Int,
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        color: Long,
        location: String? = null,
    ) = CalendarEvent(id, title, at(month, day, from.first, from.second), at(month, day, to.first, to.second), location = location, color = color)

    private fun allDay(id: String, title: String, month: Int, day: Int, lastDay: Int = day, color: Long, location: String? = null) =
        CalendarEvent.allDay(id, title, LocalDate.of(2026, month, day), LocalDate.of(2026, month, lastDay), location, color)

    val events: List<CalendarEvent> = buildList<CalendarEvent> {
        // Weekly standups on Mondays, Wednesdays and Fridays.
        var day = LocalDate.of(2026, 9, 28)
        var n = 0
        while (day.isBefore(LocalDate.of(2026, 11, 28))) {
            if (day.dayOfWeek.value in setOf(1, 3, 5)) {
                add(timed("standup-${n++}", "Standup", day.monthValue, day.dayOfMonth, 9 to 30, 9 to 45, EventColors.Work, "Room Atlas"))
            }
            day = day.plusDays(1)
        }
        add(timed("yoga-1", "Morning yoga", 10, 6, 7 to 0, 8 to 0, EventColors.Fitness, "Riverside studio"))
        add(timed("yoga-2", "Morning yoga", 10, 13, 7 to 0, 8 to 0, EventColors.Fitness, "Riverside studio"))
        add(timed("yoga-3", "Morning yoga", 10, 20, 7 to 0, 8 to 0, EventColors.Fitness, "Riverside studio"))
        add(timed("yoga-4", "Morning yoga", 10, 27, 7 to 0, 8 to 0, EventColors.Fitness, "Riverside studio"))
        add(timed("planning", "Sprint planning", 10, 12, 10 to 0, 11 to 30, EventColors.Work, "Room Atlas"))
        add(timed("dentist", "Dentist", 10, 13, 16 to 30, 17 to 15, EventColors.Personal, "Harbor Dental"))
        add(timed("design", "Design review", 10, 14, 11 to 0, 12 to 0, EventColors.Work, "Studio 3"))
        add(timed("lunch", "Lunch with Mia", 10, 14, 12 to 30, 13 to 30, EventColors.Personal, "Olive & Fig"))
        add(timed("climbing", "Climbing", 10, 14, 18 to 30, 20 to 0, EventColors.Fitness, "Boulder Hall"))
        add(timed("one-on-one", "1:1 with Sam", 10, 15, 14 to 0, 14 to 30, EventColors.Work))
        add(timed("book-club", "Book club", 10, 15, 19 to 0, 21 to 0, EventColors.Personal, "Corner Books"))
        add(allDay("offsite", "Team offsite", 10, 19, 21, EventColors.Work, "Lakeview Lodge"))
        add(timed("market", "Farmers market", 10, 17, 9 to 0, 11 to 0, EventColors.Personal, "Market Square"))
        add(allDay("mom", "Mom's birthday", 10, 18, color = EventColors.Family))
        add(timed("dinner", "Family dinner", 10, 18, 18 to 0, 21 to 0, EventColors.Family, "Mom's place"))
        add(timed("release", "Release 2.4", 10, 22, 15 to 0, 16 to 0, EventColors.Work))
        add(timed("teacher", "Parent-teacher meeting", 10, 23, 17 to 0, 17 to 30, EventColors.Family, "Elm Street School"))
        add(timed("flight-out", "Flight to Lisbon", 10, 29, 8 to 10, 11 to 25, EventColors.Travel, "Gate B12"))
        add(allDay("lisbon", "Lisbon long weekend", 10, 29, 31, EventColors.Travel, "Lisbon"))
        add(timed("flight-back", "Flight home", 11, 1, 17 to 40, 20 to 55, EventColors.Travel, "Lisbon airport"))
        add(timed("retro", "Sprint retro", 10, 9, 15 to 0, 16 to 0, EventColors.Work, "Room Atlas"))
        add(timed("run", "10K run", 10, 4, 9 to 0, 10 to 30, EventColors.Fitness, "City park"))
        add(timed("haircut", "Haircut", 10, 7, 18 to 0, 18 to 45, EventColors.Personal))
        add(timed("concert", "Concert", 10, 10, 20 to 0, 23 to 0, EventColors.Personal, "Blue Note"))
        add(timed("quarterly", "Quarterly review", 11, 5, 10 to 0, 12 to 0, EventColors.Work, "Main hall"))
        add(timed("vet", "Vet appointment", 11, 9, 9 to 30, 10 to 0, EventColors.Family))
        add(allDay("conference", "Compose Conf", 11, 12, 13, EventColors.Work, "Berlin"))
    }

    /** Dates of the cabin that are already booked; the trip planner can't select them or span them. */
    val bookedDates: Set<LocalDate> = setOf(
        LocalDate.of(2026, 10, 23),
        LocalDate.of(2026, 10, 24),
        LocalDate.of(2026, 10, 25),
        LocalDate.of(2026, 11, 6),
        LocalDate.of(2026, 11, 7),
        LocalDate.of(2026, 11, 20),
        LocalDate.of(2026, 11, 21),
        LocalDate.of(2026, 11, 22),
    )
}
