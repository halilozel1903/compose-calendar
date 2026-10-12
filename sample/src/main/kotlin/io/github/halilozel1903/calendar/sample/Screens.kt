package io.github.halilozel1903.calendar.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.AgendaDefaults
import io.github.halilozel1903.calendar.AgendaList
import io.github.halilozel1903.calendar.CalendarState
import io.github.halilozel1903.calendar.EventDots
import io.github.halilozel1903.calendar.MonthCalendar
import io.github.halilozel1903.calendar.WeekCalendar
import io.github.halilozel1903.calendar.core.Agenda
import io.github.halilozel1903.calendar.core.CalendarEvent
import io.github.halilozel1903.calendar.core.CalendarSelection
import io.github.halilozel1903.calendar.core.EventIndex
import io.github.halilozel1903.calendar.scrollToAgendaDate
import java.time.LocalDate
import java.util.Locale

/** The month with event dots, and the selected day's events below it. */
@Composable
fun MonthScreen(calendar: CalendarState, index: EventIndex<CalendarEvent>, dots: EventDots) {
    Column(Modifier.fillMaxSize()) {
        MonthCalendar(
            state = calendar,
            eventDots = dots,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
        val date = calendar.selectedDate ?: calendar.today
        val occurrences = index.occurrencesOn(date)
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "header") {
                AgendaDefaults.DayHeader(date = date, isToday = date == calendar.today)
            }
            if (occurrences.isEmpty()) {
                item(key = "empty") { AgendaDefaults.EmptyDay() }
            } else {
                items(occurrences, key = { it.event.id }) { occurrence ->
                    AgendaDefaults.EventRow(occurrence)
                }
            }
        }
    }
}

/**
 * The week strip above an agenda with sticky day headers. Tapping a day in the strip (or a day header) selects it
 * and scrolls the agenda to it.
 */
@Composable
fun WeekScreen(calendar: CalendarState, index: EventIndex<CalendarEvent>, dots: EventDots) {
    val days = remember(index) {
        Agenda.build(index, from = FirstMonth.atDay(1), to = LastMonth.atEndOfMonth(), includeEmptyDays = true)
    }
    val selected = calendar.selectedDate ?: calendar.today
    // Start at the selected day without scrolling, so the first frame is already in place.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = Agenda.headerIndexOf(days, selected))
    LaunchedEffect(selected) {
        if (listState.firstVisibleItemIndex != Agenda.headerIndexOf(days, selected)) {
            listState.scrollToAgendaDate(days, selected)
        }
    }
    Column(Modifier.fillMaxSize()) {
        WeekCalendar(
            state = calendar,
            eventDots = dots,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
        AgendaList(
            days = days,
            state = listState,
            today = calendar.today,
            selectedDate = calendar.selectedDate,
            onDayClick = { date -> calendar.selection = CalendarSelection.Single(date) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Tablets: the month with week numbers and a legend on the left, the upcoming agenda on the right. */
@Composable
fun TwoPaneScreen(calendar: CalendarState, index: EventIndex<CalendarEvent>, dots: EventDots) {
    val days = remember(index) {
        Agenda.build(index, from = FirstMonth.atDay(1), to = LastMonth.atEndOfMonth(), alwaysInclude = setOf(Today))
    }
    val selected = calendar.selectedDate ?: calendar.today
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = Agenda.headerIndexOf(days, selected))
    LaunchedEffect(selected) {
        if (listState.firstVisibleItemIndex != Agenda.headerIndexOf(days, selected)) {
            listState.scrollToAgendaDate(days, selected)
        }
    }
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(0.46f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
        ) {
            MonthCalendar(
                state = calendar,
                eventDots = dots,
                showWeekNumbers = true,
                dayHeight = 56.dp,
            )
            Spacer(Modifier.height(16.dp))
            Legend()
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier
                .weight(0.54f)
                .fillMaxHeight(),
        ) {
            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
            )
            AgendaList(
                days = days,
                state = listState,
                today = calendar.today,
                selectedDate = calendar.selectedDate,
                onDayClick = { date -> calendar.selection = CalendarSelection.Single(date) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** The event colors and what they mean. */
@Composable
private fun Legend() {
    val entries = listOf(
        "Work" to EventColors.Work,
        "Personal" to EventColors.Personal,
        "Fitness" to EventColors.Fitness,
        "Travel" to EventColors.Travel,
        "Family" to EventColors.Family,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        for ((label, color) in entries) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(color)),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** "Fri, Oct 16" for [locale]. */
fun shortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", locale))
