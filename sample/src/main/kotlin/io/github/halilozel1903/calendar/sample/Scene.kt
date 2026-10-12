package io.github.halilozel1903.calendar.sample

/** Screenshot scenes, picked with the `scene` intent extra. */
enum class Scene(val key: String, val tab: PlannerTab, val twoPane: Boolean? = null) {
    Month("month", PlannerTab.Month, twoPane = false),
    Range("range", PlannerTab.Trip, twoPane = false),
    Week("week", PlannerTab.Week, twoPane = false),
    Tablet("tablet", PlannerTab.Month, twoPane = true),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}

/** The Planner's three screens. */
enum class PlannerTab(val label: String) {
    Month("Month"),
    Week("Week"),
    Trip("Trip"),
}
