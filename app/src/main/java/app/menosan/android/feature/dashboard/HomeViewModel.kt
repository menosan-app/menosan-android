package app.menosan.android.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.menosan.android.core.auth.AuthService
import app.menosan.android.core.model.Entry
import app.menosan.android.core.model.EntrySyncStatus
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.time.WeekCalc
import app.menosan.android.data.remote.dto.HotspotDto
import app.menosan.android.data.remote.dto.ImpactDto
import app.menosan.android.data.repo.EntryRepository
import app.menosan.android.data.repo.ReportRepository
import app.menosan.android.data.repo.ReportView
import app.menosan.android.data.repo.TaxonomyRepository
import app.menosan.android.data.repo.currentWeekStartFlow
import app.menosan.android.feature.entries.CategoryTotal
import app.menosan.android.feature.entries.EntryFormats
import app.menosan.android.feature.entries.WeekSummary
import app.menosan.android.feature.entries.deleteMessage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class LatestReportCard(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val isProvisional: Boolean,
    val isLastWeek: Boolean,
    val analyzedEntries: Int,
    val analyzedPieces: Int,
    val analyzedGrams: Int,
    val topHotspot: HotspotDto?,
    val trying: List<String>,
    val hasIdeasToPick: Boolean,
    val impacts: List<ImpactDto>,
) {
    companion object {
        fun from(view: ReportView, currentWeekStart: LocalDate): LatestReportCard {
            val report = view.report
            val trying = if (view.canAdopt) view.adopted.distinctBy { it.interventionId }.map { it.title } else emptyList()
            return LatestReportCard(
                weekStart = view.weekStart,
                weekEnd = view.weekEnd,
                isProvisional = view.isProvisional,
                isLastWeek = view.weekStart == currentWeekStart.minusWeeks(1),
                analyzedEntries = report.stats.analyzedTotals.frequency,
                analyzedPieces = report.stats.analyzedTotals.pieces,
                analyzedGrams = report.stats.analyzedTotals.grams,
                topHotspot = report.hotspots.minByOrNull { it.rank },
                trying = trying,
                hasIdeasToPick = view.canAdopt && trying.isEmpty() && report.hotspots.any { it.recommendations.isNotEmpty() },
                impacts = report.impacts,
            )
        }
    }
}

sealed interface NextStep {
    data class FixSync(val count: Int) : NextStep

    data class PickIdea(val weekStart: LocalDate, val weekEnd: LocalDate) : NextStep

    data object LogToday : NextStep

    data class KeepGoing(val loggedToday: Int) : NextStep
}

data class CategoryShare(
    val category: WasteCategory,
    val entries: Int,
    val pieces: Int,
    val grams: Int,
    val percent: Int,
)

data class HomeUiState(
    val firstName: String?,
    val weekStart: LocalDate,
    val today: LocalDate,
    val summary: WeekSummary = WeekSummary.of(emptyList()),
    val recentEntries: List<Entry> = emptyList(),
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val latestReport: LatestReportCard? = null,
    val subcategoryLabels: Map<String, String> = emptyMap(),
    val loading: Boolean = true,
) {
    val loggedToday: Int get() = if (today < weekStart) 0 else summary.entriesPerDay[EntryFormats.dayIndex(today)]

    val nextStep: NextStep
        get() {
            val report = latestReport
            return when {
                failedCount > 0 -> NextStep.FixSync(failedCount)
                report != null && report.hasIdeasToPick -> NextStep.PickIdea(report.weekStart, report.weekEnd)
                loggedToday == 0 -> NextStep.LogToday
                else -> NextStep.KeepGoing(loggedToday)
            }
        }

    val categoryShares: List<CategoryShare> get() = categorySharesOf(summary)

    val specialEntries: Int get() = summary.byCategory[WasteCategory.SPECIAL]?.entries ?: 0

    val isNewUser: Boolean get() = !loading && summary.entries == 0 && latestReport == null

    fun labelOf(code: String): String = subcategoryLabels[code] ?: code

    companion object {
        const val RECENT_ENTRIES = 3

        fun build(
            firstName: String?,
            weekStart: LocalDate,
            today: LocalDate,
            weekEntries: List<Entry>,
            pendingCount: Int,
            latest: ReportView?,
            labels: Map<String, String>,
        ) = HomeUiState(
            firstName = firstName,
            weekStart = weekStart,
            today = today,
            summary = WeekSummary.of(weekEntries),
            recentEntries = weekEntries.sortedByDescending { it.createdAt }.take(RECENT_ENTRIES),
            pendingCount = pendingCount,
            failedCount = weekEntries.count { it.syncStatus == EntrySyncStatus.FAILED },
            latestReport = latest?.let { LatestReportCard.from(it, weekStart) },
            subcategoryLabels = labels,
            loading = false,
        )
    }
}

fun interface SubcategoryLabels {
    suspend fun labels(): Map<String, String>
}

class TaxonomySubcategoryLabels @Inject constructor(private val taxonomy: TaxonomyRepository) : SubcategoryLabels {
    override suspend fun labels(): Map<String, String> = taxonomy.taxonomy().subcategories.associate { it.code to it.label }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    @Binds
    abstract fun subcategoryLabels(impl: TaxonomySubcategoryLabels): SubcategoryLabels
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val entries: EntryRepository,
    private val reports: ReportRepository,
    labels: SubcategoryLabels,
    auth: AuthService,
    private val clock: Clock,
) : ViewModel() {
    private val firstName = auth.currentUser?.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotBlank() }

    private val messages = Channel<Int>(Channel.BUFFERED)

    val messageEvents: Flow<Int> = messages.receiveAsFlow()

    private val initial = WeekCalc.currentWeekStart(clock).let { week ->
        HomeUiState(firstName = firstName, weekStart = week, today = today())
    }

    private val labelMap: Flow<Map<String, String>> = flow {
        emit(emptyMap())
        emit(runCatching { labels.labels() }.getOrDefault(emptyMap()))
    }

    private val week: Flow<Pair<LocalDate, List<Entry>>> =
        combine(currentWeekStartFlow(clock), entries.observeCurrentWeek()) { start, list ->
            start to list.filter { it.weekStart == start }
        }

    val state: StateFlow<HomeUiState> = combine(
        week,
        entries.observePendingCount(),
        reports.observeLatestReport(),
        labelMap,
    ) { (start, list), pending, latest, labelsNow ->
        HomeUiState.build(firstName, start, today(), list, pending, latest, labelsNow)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    init {
        viewModelScope.launch { quietly { reports.refreshReports() } }
        viewModelScope.launch { quietly { entries.refreshCurrentWeek() } }
    }

    fun delete(id: String) {
        viewModelScope.launch { messages.send(deleteMessage { entries.delete(id) }) }
    }

    private fun today(): LocalDate = clock.instant().atZone(WeekCalc.ZONE).toLocalDate()
}

private suspend fun quietly(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    }
}

private val ANALYZED = listOf(WasteCategory.BIODEGRADABLE, WasteCategory.RECYCLABLE, WasteCategory.RESIDUAL)

internal fun categorySharesOf(summary: WeekSummary): List<CategoryShare> {
    val totals = ANALYZED.map { it to (summary.byCategory[it] ?: CategoryTotal()) }.filter { it.second.entries > 0 }
    val all = totals.sumOf { it.second.entries }
    if (all == 0) return emptyList()
    val exact = totals.map { it.second.entries * 100.0 / all }
    val percents = exact.map { it.toInt() }.toMutableList()
    var left = 100 - percents.sum()
    exact.indices.sortedByDescending { exact[it] - percents[it] }.forEach { if (left > 0) { percents[it]++; left-- } }
    return totals.mapIndexed { i, (category, total) -> CategoryShare(category, total.entries, total.pieces, total.grams, percents[i]) }
}
