package io.github.halilozel1903.calendar.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.CalendarState
import io.github.halilozel1903.calendar.EventDots
import io.github.halilozel1903.calendar.core.CalendarEvent
import io.github.halilozel1903.calendar.core.CalendarSelection
import io.github.halilozel1903.calendar.core.EventIndex
import io.github.halilozel1903.calendar.core.SelectionMode
import io.github.halilozel1903.calendar.rememberCalendarState
import io.github.halilozel1903.calendar.rememberEventDots
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.YearMonth

/** The months the Planner can show. */
val FirstMonth: YearMonth = YearMonth.of(2026, 1)
val LastMonth: YearMonth = YearMonth.of(2027, 12)

/**
 * The Planner: month, week and trip screens. Phones get a bottom navigation bar; wide windows (840 dp and more, or the
 * `tablet` scene) get a navigation rail, and the month screen shows the month and the agenda side by side.
 */
@Composable
fun PlannerApp(scene: Scene?) {
    var tab by rememberSaveable { mutableStateOf(scene?.tab ?: PlannerTab.Month) }
    val index = remember { EventIndex.ofEvents(PlannerData.events) }
    val dots = rememberEventDots(index) { event: CalendarEvent -> event.color?.let { Color(it) } }
    // One state for the month and week screens, so a day picked on one is selected on the other.
    val calendar = rememberCalendarState(
        today = Today,
        firstMonth = FirstMonth,
        lastMonth = LastMonth,
        firstDayOfWeek = DayOfWeek.MONDAY,
        selectionMode = SelectionMode.Single,
        initialSelection = CalendarSelection.Single(Today),
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = scene?.twoPane ?: (maxWidth >= 840.dp)
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    Spacer(Modifier.height(8.dp))
                    for (item in PlannerTab.entries) {
                        NavigationRailItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(TabIcon.of(item), contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)),
                ) {
                    PlannerTopBar(calendar)
                    Box(Modifier.weight(1f)) { Screen(tab, calendar, index, dots, wide = true) }
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                    PlannerTopBar(calendar)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
                ) {
                    Screen(tab, calendar, index, dots, wide = false)
                }
                NavigationBar {
                    for (item in PlannerTab.entries) {
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(TabIcon.of(item), contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Screen(tab: PlannerTab, calendar: CalendarState, index: EventIndex<CalendarEvent>, dots: EventDots, wide: Boolean) {
    when (tab) {
        PlannerTab.Month -> if (wide) TwoPaneScreen(calendar, index, dots) else MonthScreen(calendar, index, dots)
        PlannerTab.Week -> WeekScreen(calendar, index, dots)
        PlannerTab.Trip -> TripScreen(wide = wide)
    }
}

/** The app title and a "Today" button that brings the month and week back to today and selects it. */
@Composable
private fun PlannerTopBar(calendar: CalendarState) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Planner",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = {
                calendar.selection = CalendarSelection.Single(calendar.today)
                scope.launch {
                    calendar.animateScrollToMonth(YearMonth.from(calendar.today))
                }
                scope.launch {
                    calendar.animateScrollToWeek(calendar.today)
                }
            },
        ) {
            Text("Today")
        }
    }
}
