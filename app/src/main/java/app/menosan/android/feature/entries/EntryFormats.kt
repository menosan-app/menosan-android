package app.menosan.android.feature.entries

import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.model.Entry
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.time.WeekCalc
import app.menosan.android.feature.reports.formatWeekRange
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object EntryFormats {
    private val dayMonth = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
    private val fullDate = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val weekday = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)

    fun weekRange(weekStart: LocalDate): String = formatWeekRange(weekStart, WeekCalc.weekEnd(weekStart))

    fun date(instant: Instant): String = fullDate.format(instant.atZone(WeekCalc.ZONE))

    fun time(instant: Instant): String = time.format(instant.atZone(WeekCalc.ZONE)).lowercase(Locale.ENGLISH)

    fun shortDateTime(instant: Instant): String =
        "${dayMonth.format(instant.atZone(WeekCalc.ZONE))} · ${time(instant)}"

    fun editableUntil(weekStart: LocalDate): String = "${weekday.format(WeekCalc.weekEnd(weekStart))}, 11:59 pm"

    fun dayIndex(instant: Instant): Int = instant.atZone(WeekCalc.ZONE).dayOfWeek.let { if (it == DayOfWeek.SUNDAY) 0 else it.value }

    fun dayIndex(date: LocalDate): Int = if (date.dayOfWeek == DayOfWeek.SUNDAY) 0 else date.dayOfWeek.value
}

data class CategoryTotal(val entries: Int = 0, val pieces: Int = 0, val grams: Int = 0)

data class WeekSummary(
    val entries: Int,
    val pieces: Int,
    val grams: Int,
    val entriesPerDay: List<Int>,
    val byCategory: Map<WasteCategory, CategoryTotal>,
) {
    companion object {
        fun of(entries: List<Entry>): WeekSummary {
            val perDay = IntArray(7)
            entries.forEach { perDay[EntryFormats.dayIndex(it.createdAt)]++ }
            return WeekSummary(
                entries = entries.size,
                pieces = entries.sumIn(QuantityUnit.PIECES),
                grams = entries.sumIn(QuantityUnit.GRAMS),
                entriesPerDay = perDay.toList(),
                byCategory = WasteCategory.entries.associateWith { category ->
                    entries.filter { it.category == category }.let {
                        CategoryTotal(it.size, it.sumIn(QuantityUnit.PIECES), it.sumIn(QuantityUnit.GRAMS))
                    }
                },
            )
        }
    }
}

private fun List<Entry>.sumIn(unit: QuantityUnit): Int = filter { it.unit == unit }.sumOf { it.quantity }
