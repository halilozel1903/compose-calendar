package io.github.halilozel1903.calendar.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.MonthCalendar
import io.github.halilozel1903.calendar.core.CalendarSelection
import io.github.halilozel1903.calendar.core.OutsideDays
import io.github.halilozel1903.calendar.core.SelectionMode
import io.github.halilozel1903.calendar.core.SelectionRejection
import io.github.halilozel1903.calendar.core.SelectionResult
import io.github.halilozel1903.calendar.core.SelectionRules
import io.github.halilozel1903.calendar.rememberCalendarState
import java.time.LocalDate
import java.time.YearMonth

/** Nightly price of the cabin, in dollars. */
private const val NightlyPrice = 120

/** At least one night, at most two weeks. */
private val TripRules = SelectionRules(
    minDate = Today,
    maxDate = Today.plusMonths(6),
    disabledDates = PlannerData.bookedDates,
    minRangeDays = 2,
    maxRangeDays = 15,
    allowDisabledInRange = false,
)

/**
 * Booking a cabin with range selection: check-in and check-out are the ends of the range, booked nights are
 * disabled and can't be inside a stay, past days can't be picked, and a stay is one to fourteen nights.
 */
@Composable
fun TripScreen(wide: Boolean) {
    val trip = rememberCalendarState(
        today = Today,
        firstMonth = YearMonth.from(Today),
        lastMonth = YearMonth.from(Today.plusMonths(6)),
        firstDayOfWeek = java.time.DayOfWeek.MONDAY,
        selectionMode = SelectionMode.Range,
        selectionRules = TripRules,
        initialSelection = CalendarSelection.Range(LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 22)),
    )
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = if (wide) 560.dp else 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = "Lake cabin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
            Text(
                text = "Pick check-in and check-out. Struck-through nights are booked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp),
            )
            MonthCalendar(
                state = trip,
                outsideDays = OutsideDays.Hide,
                onDateClick = { date, result ->
                    message = (result as? SelectionResult.Rejected)?.let { rejectionMessage(date, it.reason) }
                },
                dayContent = { day ->
                    val booked = day.date in PlannerData.bookedDates
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (day.range.isEndPoint) FontWeight.SemiBold else FontWeight.Normal,
                        textDecoration = if (booked) TextDecoration.LineThrough else null,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                },
            )
            TripSummary(trip.selectedRange, message, onClear = {
                trip.clearSelection()
                message = null
            })
        }
    }
}

@Composable
private fun TripSummary(range: CalendarSelection.Range?, message: String?, onClear: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val end = range?.end
            when {
                range == null -> SummaryTitle("Pick a check-in date")
                end == null -> {
                    SummaryTitle("Check-in ${shortDate(range.start)}")
                    Text("Now pick a check-out date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${shortDate(range.start)} → ${shortDate(end)}",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${range.nights} × $$NightlyPrice per night",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (range.nights == 1) "1 night" else "${range.nights} nights",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "$${range.nights * NightlyPrice}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (message != null) {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear, enabled = range != null) { Text("Clear") }
                Button(onClick = {}, enabled = range?.isComplete == true) { Text("Reserve") }
            }
        }
    }
}

@Composable
private fun SummaryTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

private fun rejectionMessage(date: LocalDate, reason: SelectionRejection): String {
    val day = shortDate(date)
    return when (reason) {
        SelectionRejection.DateDisabled -> "$day is already booked."
        SelectionRejection.OutOfBounds -> "$day can't be booked."
        SelectionRejection.RangeTooShort -> "Stay at least one night."
        SelectionRejection.RangeTooLong -> "Stays are at most ${TripRules.maxRangeDays?.minus(1)} nights."
        SelectionRejection.RangeContainsDisabledDate -> "Booked nights are in between. Pick an earlier check-out."
        SelectionRejection.TooManyDates, SelectionRejection.SelectionDisabled -> "$day can't be picked."
    }
}
