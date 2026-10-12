package io.github.halilozel1903.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.calendar.core.CalendarMath
import io.github.halilozel1903.calendar.core.CalendarWeek
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Colors of the calendar views. Get the Material 3 defaults from [CalendarDefaults.colors]. */
@Immutable
public data class CalendarColors(
    /** Day numbers of the month. */
    val content: Color,
    /** Day numbers of the previous and next month. */
    val outsideContent: Color,
    /** Day numbers that can't be selected. */
    val disabledContent: Color,
    /** The weekday labels above the grid. */
    val weekdayContent: Color,
    /** Week numbers. */
    val weekNumberContent: Color,
    /** The month title and arrows. */
    val headerContent: Color,
    /** The circle behind a selected date and behind range end points. */
    val selectedContainer: Color,
    /** Day numbers on [selectedContainer]. */
    val selectedContent: Color,
    /** The pill between the ends of a range. */
    val rangeContainer: Color,
    /** Day numbers on [rangeContainer]. */
    val rangeContent: Color,
    /** The ring around today. */
    val todayOutline: Color,
    /** Today's day number when it is not selected. */
    val todayContent: Color,
    /** Event dots when an event has no color of its own. */
    val eventDot: Color,
    /** Background of agenda day headers, so events scroll under them. */
    val agendaHeaderContainer: Color,
)

/** Texts the calendar shows or reads out to accessibility services. Override them to translate. */
@Immutable
public class CalendarStrings(
    public val previousMonth: String = "Previous month",
    public val nextMonth: String = "Next month",
    public val previousWeek: String = "Previous week",
    public val nextWeek: String = "Next week",
    public val today: String = "Today",
    public val rangeStart: String = "Start of range",
    public val rangeEnd: String = "End of range",
    public val unavailable: String = "Unavailable",
    public val noEvents: String = "No events",
    public val allDay: String = "All day",
    public val weekNumber: (Int) -> String = { "Week $it" },
    public val eventCount: (Int) -> String = { if (it == 1) "1 event" else "$it events" },
    /** "Day 2 of 3", for the days of an event that spans several days. */
    public val dayOfEvent: (day: Int, total: Int) -> String = { day, total -> "Day $day of $total" },
)

/** Defaults for [MonthCalendar], [WeekCalendar] and [AgendaList]. */
public object CalendarDefaults {

    /** Height of a day cell in [MonthCalendar]. */
    public val DayHeight: Dp = 48.dp

    /** Height of a day cell in [WeekCalendar]. */
    public val WeekDayHeight: Dp = 52.dp

    /** Width of the week number column. */
    public val WeekNumberWidth: Dp = 32.dp

    /** At most this many event dots are drawn under a date. */
    public const val MaxDots: Int = 3

    /** Material 3 colors from the current [MaterialTheme]. */
    @Composable
    public fun colors(
        content: Color = MaterialTheme.colorScheme.onSurface,
        outsideContent: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        disabledContent: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
        weekdayContent: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        weekNumberContent: Color = MaterialTheme.colorScheme.outline,
        headerContent: Color = MaterialTheme.colorScheme.onSurface,
        selectedContainer: Color = MaterialTheme.colorScheme.primary,
        selectedContent: Color = MaterialTheme.colorScheme.onPrimary,
        rangeContainer: Color = MaterialTheme.colorScheme.primaryContainer,
        rangeContent: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        todayOutline: Color = MaterialTheme.colorScheme.primary,
        todayContent: Color = MaterialTheme.colorScheme.primary,
        eventDot: Color = MaterialTheme.colorScheme.tertiary,
        agendaHeaderContainer: Color = MaterialTheme.colorScheme.surface,
    ): CalendarColors = CalendarColors(
        content = content,
        outsideContent = outsideContent,
        disabledContent = disabledContent,
        weekdayContent = weekdayContent,
        weekNumberContent = weekNumberContent,
        headerContent = headerContent,
        selectedContainer = selectedContainer,
        selectedContent = selectedContent,
        rangeContainer = rangeContainer,
        rangeContent = rangeContent,
        todayOutline = todayOutline,
        todayContent = todayContent,
        eventDot = eventDot,
        agendaHeaderContainer = agendaHeaderContainer,
    )

    /** "October 2026" for [locale]. */
    public fun monthTitle(month: YearMonth, locale: Locale): String =
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }

    /** The full spoken date, such as "Wednesday, October 14, 2026". */
    public fun fullDate(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))

    /**
     * The default day content: the day number, with up to [MaxDots] event dots under it. The number takes
     * [LocalContentColor], which the calendar sets for the cell's state.
     */
    @Composable
    public fun DayContent(day: DayCellState, modifier: Modifier = Modifier) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (day.isToday || day.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (day.dots.isNotEmpty()) {
                EventDots(
                    colors = day.dots,
                    // On a filled circle the dots take the number's color so they stay visible.
                    override = if (day.range.isEndPoint || (day.isSelected && !day.range.inRange)) LocalContentColor.current else null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                )
            }
        }
    }

    /** A row of up to [MaxDots] dots. */
    @Composable
    public fun EventDots(colors: List<Color>, modifier: Modifier = Modifier, override: Color? = null, size: Dp = 4.dp) {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            colors.take(MaxDots).forEach { color ->
                Canvas(Modifier.size(size)) { drawCircle(override ?: color) }
            }
        }
    }

    /**
     * The default month header: the month title with previous and next arrows. The title is a heading and a polite
     * live region, so TalkBack announces the new month after a swipe.
     */
    @Composable
    public fun MonthHeader(
        title: String,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        modifier: Modifier = Modifier,
        canGoPrevious: Boolean = true,
        canGoNext: Boolean = true,
        colors: CalendarColors = colors(),
        strings: CalendarStrings = CalendarStrings(),
    ) {
        PagerHeader(
            title = title,
            onPrevious = onPrevious,
            onNext = onNext,
            previousDescription = strings.previousMonth,
            nextDescription = strings.nextMonth,
            canGoPrevious = canGoPrevious,
            canGoNext = canGoNext,
            colors = colors,
            modifier = modifier,
        )
    }

    /**
     * The default [MonthCalendar] header: [monthTitle] of [month] with arrows that swipe [state]'s month pager to
     * the previous and next month.
     */
    @Composable
    public fun MonthHeader(
        state: CalendarState,
        month: YearMonth,
        modifier: Modifier = Modifier,
        colors: CalendarColors = colors(),
        strings: CalendarStrings = CalendarStrings(),
        locale: Locale = Locale.getDefault(),
    ) {
        val scope = rememberCoroutineScope()
        MonthHeader(
            title = monthTitle(month, locale),
            onPrevious = { scope.launch { state.animateScrollToMonth(state.visibleMonth.minusMonths(1)) } },
            onNext = { scope.launch { state.animateScrollToMonth(state.visibleMonth.plusMonths(1)) } },
            modifier = modifier,
            canGoPrevious = state.canScrollToPreviousMonth,
            canGoNext = state.canScrollToNextMonth,
            colors = colors,
            strings = strings,
        )
    }

    /** "October 2026", or "September – October 2026" and "December 2026 – January 2027" for weeks across months. */
    public fun weekTitle(week: CalendarWeek, locale: Locale): String {
        val first = YearMonth.from(week.start)
        val last = YearMonth.from(week.end)
        if (first == last) return monthTitle(first, locale)
        return if (first.year == last.year) {
            val monthOnly = DateTimeFormatter.ofPattern("LLLL", locale)
            first.format(monthOnly).replaceFirstChar { it.titlecase(locale) } + " – " + monthTitle(last, locale)
        } else {
            monthTitle(first, locale) + " – " + monthTitle(last, locale)
        }
    }

    /** The default [WeekCalendar] header: [weekTitle] with arrows that swipe to the previous and next week. */
    @Composable
    public fun WeekHeader(
        state: CalendarState,
        week: CalendarWeek,
        modifier: Modifier = Modifier,
        colors: CalendarColors = colors(),
        strings: CalendarStrings = CalendarStrings(),
        locale: Locale = Locale.getDefault(),
    ) {
        val scope = rememberCoroutineScope()
        val pager = state.weekPagerState
        PagerHeader(
            title = weekTitle(week, locale),
            onPrevious = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            onNext = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            previousDescription = strings.previousWeek,
            nextDescription = strings.nextWeek,
            canGoPrevious = pager.currentPage > 0,
            canGoNext = pager.currentPage < state.weeks.pageCount - 1,
            colors = colors,
            modifier = modifier,
        )
    }

    /** The narrow weekday label ("M", "T", …) for [day]. */
    public fun weekdayLabel(day: DayOfWeek, locale: Locale): String = day.getDisplayName(TextStyle.NARROW_STANDALONE, locale)

    /** The short weekday label ("Mon", "Tue", …) for [day]. */
    public fun weekdayShortLabel(day: DayOfWeek, locale: Locale): String = day.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
}

/** A chevron that points to the next item in the layout direction, or the previous one. */
@Composable
internal fun Chevron(forward: Boolean, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier.size(24.dp)) {
        val pointsRight = forward == (layoutDirection == LayoutDirection.Ltr)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (pointsRight) {
                moveTo(w * 0.40f, h * 0.27f)
                lineTo(w * 0.63f, h * 0.50f)
                lineTo(w * 0.40f, h * 0.73f)
            } else {
                moveTo(w * 0.60f, h * 0.27f)
                lineTo(w * 0.37f, h * 0.50f)
                lineTo(w * 0.60f, h * 0.73f)
            }
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The weekday labels above a grid; hidden from accessibility services because each day reads its weekday. */
@Composable
internal fun WeekdayRow(
    firstDayOfWeek: DayOfWeek,
    colors: CalendarColors,
    locale: Locale,
    leadingWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingWidth > 0.dp) Box(Modifier.size(width = leadingWidth, height = 1.dp))
        for (day in CalendarMath.weekdays(firstDayOfWeek)) {
            Text(
                text = CalendarDefaults.weekdayLabel(day, locale),
                style = MaterialTheme.typography.labelMedium,
                color = colors.weekdayContent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** A title with previous and next arrows; the title is a heading and a polite live region. */
@Composable
internal fun PagerHeader(
    title: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousDescription: String,
    nextDescription: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    colors: CalendarColors,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalContentColor provides colors.headerContent) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    },
            )
            IconButton(
                onClick = onPrevious,
                enabled = canGoPrevious,
                modifier = Modifier.semantics { contentDescription = previousDescription },
            ) {
                Chevron(forward = false)
            }
            IconButton(
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier.semantics { contentDescription = nextDescription },
            ) {
                Chevron(forward = true)
            }
        }
    }
}
