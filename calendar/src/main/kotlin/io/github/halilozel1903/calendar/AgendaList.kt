package io.github.halilozel1903.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.core.Agenda
import io.github.halilozel1903.calendar.core.AgendaDay
import io.github.halilozel1903.calendar.core.CalendarEvent
import io.github.halilozel1903.calendar.core.EventOccurrence
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * A scrolling agenda: a sticky header per day and the day's events under it. Events scroll under the header of their
 * day, and the next day's header pushes it away. Build [days] with [Agenda.build]; scroll to a date with
 * [scrollToAgendaDate].
 *
 * The list has exactly [Agenda.itemCount] items per day (the header, then one item per event, or one [emptyDay] item
 * for a day without events), so [Agenda.headerIndexOf] gives the list index of a day.
 *
 * @param eventContent one event of a day; [EventOccurrence.dayNumber] tells which day of a multi-day event it is.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun <T> AgendaList(
    days: List<AgendaDay<T>>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    today: LocalDate? = null,
    selectedDate: LocalDate? = null,
    onDayClick: ((LocalDate) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
    colors: CalendarColors = CalendarDefaults.colors(),
    strings: CalendarStrings = CalendarStrings(),
    locale: Locale = Locale.getDefault(),
    dayHeader: @Composable (day: AgendaDay<T>) -> Unit = { day ->
        AgendaDefaults.DayHeader(
            date = day.date,
            isToday = day.date == today,
            isSelected = day.date == selectedDate,
            onClick = onDayClick?.let { click -> { click(day.date) } },
            colors = colors,
            strings = strings,
            locale = locale,
        )
    },
    emptyDay: @Composable (date: LocalDate) -> Unit = { AgendaDefaults.EmptyDay(strings = strings) },
    eventContent: @Composable (occurrence: EventOccurrence<T>) -> Unit,
) {
    LazyColumn(modifier = modifier, state = state, contentPadding = contentPadding) {
        for (day in days) {
            val date = day.date
            stickyHeader(key = "header-$date", contentType = "header") {
                dayHeader(day)
            }
            if (day.occurrences.isEmpty()) {
                item(key = "empty-$date", contentType = "empty") { emptyDay(date) }
            } else {
                day.occurrences.forEachIndexed { index, occurrence ->
                    item(key = "event-$date-$index", contentType = "event") { eventContent(occurrence) }
                }
            }
        }
    }
}

/**
 * An [AgendaList] of [CalendarEvent]s with the default event rows: a color bar, the title, the time (or "All day",
 * or "Day 2 of 3") and the location.
 */
@Composable
public fun AgendaList(
    days: List<AgendaDay<CalendarEvent>>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    today: LocalDate? = null,
    selectedDate: LocalDate? = null,
    onDayClick: ((LocalDate) -> Unit)? = null,
    onEventClick: ((CalendarEvent) -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
    colors: CalendarColors = CalendarDefaults.colors(),
    strings: CalendarStrings = CalendarStrings(),
    locale: Locale = Locale.getDefault(),
) {
    AgendaList<CalendarEvent>(
        days = days,
        modifier = modifier,
        state = state,
        today = today,
        selectedDate = selectedDate,
        onDayClick = onDayClick,
        contentPadding = contentPadding,
        colors = colors,
        strings = strings,
        locale = locale,
    ) { occurrence ->
        AgendaDefaults.EventRow(
            occurrence = occurrence,
            onClick = onEventClick?.let { click -> { click(occurrence.event) } },
            colors = colors,
            strings = strings,
            locale = locale,
        )
    }
}

/**
 * Scrolls an [AgendaList] showing [days] so the header of [date] (or of the first listed day after it) is at the
 * top.
 */
public suspend fun LazyListState.scrollToAgendaDate(days: List<AgendaDay<*>>, date: LocalDate, animate: Boolean = true) {
    val total = days.sumOf { Agenda.itemCount(it) }
    if (total == 0) return
    val index = Agenda.headerIndexOf(days, date).coerceAtMost(total - 1)
    if (animate) animateScrollToItem(index) else scrollToItem(index)
}

/** The building blocks of [AgendaList]. */
public object AgendaDefaults {

    /** "Wednesday, October 14" for [locale]. */
    public fun dayTitle(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))

    /** "09:30 – 10:15" (or "9:30 AM – 10:15 AM") for [locale]. */
    public fun timeRange(event: CalendarEvent, locale: Locale): String {
        val format = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        return "${event.start.toLocalTime().format(format)} – ${event.end.toLocalTime().format(format)}"
    }

    /** "All day", "Day 2 of 3" or the time range of [occurrence]. */
    public fun timeLabel(occurrence: EventOccurrence<CalendarEvent>, strings: CalendarStrings, locale: Locale): String {
        val event = occurrence.event
        return when {
            occurrence.isMultiDay -> strings.dayOfEvent(occurrence.dayNumber, occurrence.totalDays) +
                if (event.isAllDay) "" else " · " + timeRange(event, locale)
            event.isAllDay -> strings.allDay
            else -> timeRange(event, locale)
        }
    }

    /**
     * The default sticky day header: the day's name and date on an opaque background, so events scroll under it.
     * Today is marked with a filled circle around the day number, the selected day in the header color.
     */
    @Composable
    public fun DayHeader(
        date: LocalDate,
        modifier: Modifier = Modifier,
        isToday: Boolean = false,
        isSelected: Boolean = false,
        onClick: (() -> Unit)? = null,
        colors: CalendarColors = CalendarDefaults.colors(),
        strings: CalendarStrings = CalendarStrings(),
        locale: Locale = Locale.getDefault(),
    ) {
        val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.agendaHeaderContainer)
                .then(clickModifier)
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isToday) colors.selectedContainer else Color.Transparent)
                    .width(36.dp)
                    .height(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isToday -> colors.selectedContent
                        isSelected -> colors.todayContent
                        else -> colors.content
                    },
                    maxLines = 1,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = dayTitle(date, locale),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) colors.todayContent else colors.headerContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isToday) {
                    Text(
                        text = strings.today,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.todayContent,
                        maxLines = 1,
                    )
                }
            }
        }
    }

    /** The default item of a day without events. */
    @Composable
    public fun EmptyDay(modifier: Modifier = Modifier, strings: CalendarStrings = CalendarStrings()) {
        Text(
            text = strings.noEvents,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 64.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        )
    }

    /** The default event row: a color bar, the title, [timeLabel] and the location. */
    @Composable
    public fun EventRow(
        occurrence: EventOccurrence<CalendarEvent>,
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        colors: CalendarColors = CalendarDefaults.colors(),
        strings: CalendarStrings = CalendarStrings(),
        locale: Locale = Locale.getDefault(),
    ) {
        val event = occurrence.event
        val accent = event.color?.let { Color(it) } ?: colors.eventDot
        val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 64.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.12f))
                .then(clickModifier)
                .semantics(mergeDescendants = true) {}
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val details = listOfNotNull(timeLabel(occurrence, strings, locale), event.location).joinToString(" · ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
