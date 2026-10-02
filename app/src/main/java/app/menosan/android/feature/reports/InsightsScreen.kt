package app.menosan.android.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.menosan.android.R
import app.menosan.android.core.ui.components.GroupedList
import app.menosan.android.core.ui.components.MetaText
import app.menosan.android.core.ui.components.QuietCard
import app.menosan.android.core.ui.components.SectionGap
import app.menosan.android.core.ui.components.SectionHeader
import app.menosan.android.core.ui.components.screenInsetsPadding
import app.menosan.android.core.ui.components.piecesAndGramsText
import app.menosan.android.core.ui.components.quantityText
import app.menosan.android.core.ui.components.MessageBanner
import app.menosan.android.core.ui.components.Pill
import app.menosan.android.core.ui.components.primaryButtonColors
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.data.remote.dto.HotspotDto
import app.menosan.android.data.repo.RefreshResult
import app.menosan.android.data.repo.ReportListItem
import app.menosan.android.data.repo.ReportRepository
import app.menosan.android.data.repo.ReportView
import app.menosan.android.data.repo.TaxonomyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** The top hotspot of the latest report, while its ideas can still be adopted. */
data class InsightsFocus(val weekStart: LocalDate, val hotspot: HotspotDto, val label: String)

data class InsightsUiState(
    val reports: List<ReportListItem> = emptyList(),
    val focus: InsightsFocus? = null,
    val loaded: Boolean = false,
    val refreshing: Boolean = false,
    val problem: ReportProblem? = null,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: ReportRepository,
    private val taxonomy: TaxonomyRepository,
) : ViewModel() {
    private val status = MutableStateFlow(InsightsUiState(refreshing = true))
    private val labels = MutableStateFlow<Map<String, String>>(emptyMap())

    val state: StateFlow<InsightsUiState> =
        combine(repository.observeReports(), repository.observeLatestReport(), status, labels) { reports, latest, s, l ->
            s.copy(reports = reports, focus = focusOf(reports.firstOrNull(), latest, l), loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState(refreshing = true))

    init {
        viewModelScope.launch {
            labels.value = taxonomy.taxonomy().subcategories.associate { it.code to it.label }
        }
        refresh()
    }

    fun refresh() {
        status.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val result = repository.refreshReports()
            status.update {
                it.copy(refreshing = false, problem = (result as? RefreshResult.Failure)?.error?.toProblem())
            }
        }
    }
}

internal fun focusOf(latest: ReportListItem?, view: ReportView?, labels: Map<String, String>): InsightsFocus? {
    if (latest == null || view == null || view.weekStart != latest.weekStart) return null
    if (!view.canAdopt || view.isProvisional) return null
    val top = view.report.hotspots.minByOrNull { it.rank } ?: return null
    return InsightsFocus(view.weekStart, top, labels[top.subcategory] ?: top.subcategory)
}

@Composable
fun InsightsScreen(onOpenReport: (LocalDate, openIdeas: Boolean) -> Unit, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InsightsContent(state = state, onRefresh = viewModel::refresh, onOpenReport = onOpenReport)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsContent(state: InsightsUiState, onRefresh: () -> Unit, onOpenReport: (LocalDate, openIdeas: Boolean) -> Unit) {
    PullToRefreshBox(
        isRefreshing = state.refreshing && state.reports.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().screenInsetsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(SectionGap),
        ) {
            item(key = "header") {
                Column {
                    Text(
                        stringResource(R.string.insights_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.insights_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val reports = state.reports
            when {
                reports.isEmpty() && (state.refreshing || !state.loaded) -> item(key = "loading") {
                    PatientLoading(stringResource(R.string.insights_loading))
                }
                reports.isEmpty() && state.problem != null -> item(key = "problem") {
                    ProblemWithRetry(state.problem, onRetry = onRefresh)
                }
                reports.isEmpty() -> item(key = "empty") {
                    GentleMessage(
                        title = stringResource(R.string.insights_empty_title),
                        body = stringResource(R.string.insights_empty_body),
                        hint = stringResource(R.string.insights_empty_hint),
                    )
                }
                else -> {
                    if (state.problem != null) {
                        item(key = "saved") { MessageBanner(stringResource(R.string.insights_showing_saved), icon = Icons.Outlined.CloudOff) }
                    }
                    val latest = reports.first()
                    item(key = "latest") {
                        Section(
                            stringResource(R.string.insights_latest_label),
                            trailing = { MetaText(weekRangeWithYear(latest.weekStart, latest.weekEnd)) },
                        ) { LatestReportCard(latest, onClick = { onOpenReport(latest.weekStart, false) }) }
                    }
                    state.focus?.let { focus ->
                        item(key = "focus") {
                            Section(stringResource(R.string.insights_focus_title)) {
                                FocusCard(focus, onSeeIdeas = { onOpenReport(focus.weekStart, true) })
                            }
                        }
                    }
                    if (reports.size > 1) {
                        item(key = "past") {
                            Section(
                                stringResource(R.string.insights_past_reports),
                                trailing = { MetaText(stringResource(R.string.insights_past_private)) },
                            ) {
                                GroupedList(reports.drop(1), dividerInset = 72.dp) { report ->
                                    ReportRow(report, onClick = { onOpenReport(report.weekStart, false) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title, trailing = trailing)
        content()
    }
}

@Composable
private fun reportStats(report: ReportListItem): String {
    val base = stringResource(
        R.string.insights_report_stats,
        piecesAndGramsText(report.analyzedPieces, report.analyzedGrams),
        pluralStringResource(R.plurals.reports_hotspots, report.hotspotCount, report.hotspotCount),
    )
    return if (report.adoptedCount > 0) "$base · ${pluralStringResource(R.plurals.insights_adopted, report.adoptedCount, report.adoptedCount)}" else base
}

@Composable
private fun LatestReportCard(report: ReportListItem, onClick: () -> Unit) {
    QuietCard(onClick = onClick, onClickLabel = stringResource(R.string.insights_open_report), spacing = 16.dp) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                report.analyzedEntries.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 56.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f).padding(bottom = 8.dp)) {
                Text(
                    pluralStringResource(R.plurals.dashboard_entries_word, report.analyzedEntries),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                MetaText(piecesAndGramsText(report.analyzedPieces, report.analyzedGrams))
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        val offline = report.isProvisional
        if (offline || report.hotspotCount > 0 || report.adoptedCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (offline) {
                    Pill(stringResource(R.string.reports_offline_summary), MenosanTheme.colors.calm, MenosanTheme.colors.onCalm)
                } else if (report.hotspotCount > 0) {
                    Pill(
                        pluralStringResource(R.plurals.insights_hotspots_found, report.hotspotCount, report.hotspotCount),
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (report.adoptedCount > 0) {
                    Pill(
                        pluralStringResource(R.plurals.insights_adopted, report.adoptedCount, report.adoptedCount),
                        MenosanTheme.colors.mist,
                        MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusCard(focus: InsightsFocus, onSeeIdeas: () -> Unit) {
    val hotspot = focus.hotspot
    ReportCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).background(MenosanTheme.colors.mist, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(focus.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    stringResource(R.string.insights_focus_body, quantityText(hotspot.quantity, hotspot.unit), entriesText(hotspot.frequency)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.insights_focus_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSeeIdeas, shape = MaterialTheme.shapes.small, colors = primaryButtonColors(), modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.insights_see_ideas))
            }
        }
    }
}

@Composable
private fun ReportRow(report: ReportListItem, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(40.dp).background(MenosanTheme.colors.mist, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    weekRangeWithYear(report.weekStart, report.weekEnd),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    reportStats(report),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (report.isProvisional) {
                    Pill(stringResource(R.string.reports_offline_summary), MenosanTheme.colors.calm, MenosanTheme.colors.onCalm)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
