package app.menosan.android.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.menosan.android.R
import app.menosan.android.core.ui.components.screenInsetsPadding
import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.ui.components.Donut
import app.menosan.android.core.ui.components.MessageBanner
import app.menosan.android.core.ui.components.Pill
import app.menosan.android.core.ui.components.gramsText
import app.menosan.android.core.ui.components.piecesAndGramsText
import app.menosan.android.core.ui.components.quantityText
import app.menosan.android.core.ui.components.ScreenHeader
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.data.remote.dto.ComparisonDto
import app.menosan.android.data.remote.dto.ComparisonRowDto
import app.menosan.android.data.remote.dto.HotspotDto
import app.menosan.android.data.remote.dto.RecommendationDto
import app.menosan.android.data.remote.dto.Trend
import app.menosan.android.data.remote.dto.WeeklyStatsDto
import app.menosan.android.data.repo.LastWeekRecap
import app.menosan.android.data.repo.ReportView
import app.menosan.android.feature.interventions.ImpactCard
import app.menosan.android.feature.interventions.RecommendationCard
import app.menosan.android.feature.interventions.RecommendationDetailsSheet

@Composable
fun ReportScreen(onBack: () -> Unit, viewModel: ReportViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                ReportMessage.Adopted -> R.string.report_adopt_done
                ReportMessage.Unadopted -> R.string.report_unadopt_done
                ReportMessage.WindowClosed -> R.string.report_adopt_window_closed
                ReportMessage.Offline -> R.string.report_adopt_offline
                ReportMessage.Failed -> R.string.report_adopt_failed
            }
            snackbar.showSnackbar(resources.getString(text))
        }
    }
    Box(Modifier.fillMaxSize()) {
        ReportContent(state = state, onBack = onBack, onRefresh = viewModel::refresh, onSetAdopted = viewModel::setAdopted)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContent(
    state: ReportUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSetAdopted: (interventionId: String, adopted: Boolean) -> Unit,
) {
    var details by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().screenInsetsPadding()) {
        ScreenHeader(title = stringResource(R.string.nav_report), onBack = onBack)
        val view = state.view
        PullToRefreshBox(
            isRefreshing = state.refreshing && view != null,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    view != null -> reportSections(state, view, onSetAdopted, onOpenDetails = { details = it })
                    state.notFound -> item { GentleMessage(title = null, body = stringResource(R.string.report_not_found)) }
                    state.refreshing || !state.loaded -> item { PatientLoading(stringResource(R.string.report_loading)) }
                    state.problem != null -> item { ProblemWithRetry(state.problem, onRetry = onRefresh) }
                    else -> item { GentleMessage(title = null, body = stringResource(R.string.report_not_found)) }
                }
            }
        }
    }
    val view = state.view
    val open = details?.let { id -> view?.report?.hotspots?.flatMap { it.recommendations }?.firstOrNull { it.interventionId == id } }
    if (view != null && open != null) {
        RecommendationDetailsSheet(
            rec = open,
            canAdopt = view.canAdopt,
            pending = open.interventionId in state.pendingAdoptions,
            onToggleAdopt = { onSetAdopted(open.interventionId, !open.adopted) },
            onDismiss = { details = null },
        )
    }
}

private fun LazyListScope.reportSections(
    state: ReportUiState,
    view: ReportView,
    onSetAdopted: (String, Boolean) -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    val report = view.report
    item(key = "header") { WeekHeader(view) }
    if (view.isProvisional) {
        item(key = "offline") {
            val text = stringResource(R.string.report_offline_banner) +
                if (view.missingComparisons) "\n" + stringResource(R.string.report_offline_banner_partial) else ""
            MessageBanner(text, icon = Icons.Outlined.CloudOff)
        }
    } else if (state.problem != null) {
        item(key = "saved") { MessageBanner(stringResource(R.string.report_showing_saved), icon = Icons.Outlined.CloudOff) }
    }
    item(key = "totals") { TotalsCard(report.stats) }
    item(key = "categories") { CategoryBarsCard(report.stats, state) }
    item(key = "breakdown") { BreakdownCard(report.stats, state) }
    item(key = "comparison") { ComparisonCard(report.comparison, state) }
    item(key = "hotspots") {
        SectionTitle(stringResource(R.string.report_hotspots_title), subtitle = stringResource(R.string.report_hotspots_subtitle))
    }
    if (report.hotspots.isEmpty()) {
        item(key = "special-only") { MessageBanner(stringResource(R.string.report_hotspots_special_only)) }
    } else {
        items(report.hotspots, key = { "hotspot-${it.subcategory}" }) { HotspotCard(it, state) }
    }
    if (!view.isProvisional && report.hotspots.isNotEmpty()) {
        item(key = "ideas") {
            IdeasSection(view, state, onSetAdopted, onOpenDetails)
        }
    }
    if (report.impacts.isNotEmpty()) {
        item(key = "impact-title") {
            SectionTitle(stringResource(R.string.report_impact_title), subtitle = stringResource(R.string.report_impact_subtitle))
        }
        items(report.impacts, key = { "impact-${it.interventionId}" }) { ImpactCard(it, state.label(it.targetSubcategory)) }
    }
    view.recap?.let { recap -> item(key = "recap") { RecapCard(recap, state) } }
}

@Composable
private fun WeekHeader(view: ReportView) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            weekRangeWithYear(view.weekStart, view.weekEnd),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (view.isProvisional) {
                Pill(stringResource(R.string.reports_offline_summary), MenosanTheme.colors.calm, MenosanTheme.colors.onCalm)
            }
            if (view.canAdopt) {
                Pill(stringResource(R.string.reports_latest), MenosanTheme.colors.highlight, MenosanTheme.colors.onHighlight)
            }
        }
    }
}

@Composable
private fun TotalsCard(stats: WeeklyStatsDto) {
    ReportCard {
        SectionTitle(stringResource(R.string.report_totals_title))
        val totals = stats.analyzedTotals
        Row(Modifier.fillMaxWidth()) {
            BigNumber(totals.frequency.toString(), stringResource(R.string.report_totals_entries), Modifier.weight(1f))
            BigNumber(totals.pieces.toString(), stringResource(R.string.report_totals_pieces), Modifier.weight(1f))
            if (totals.grams > 0) {
                BigNumber(gramsText(totals.grams), stringResource(R.string.report_totals_food), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BigNumber(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryBarsCard(stats: WeeklyStatsDto, state: ReportUiState) {
    ReportCard {
        SectionTitle(stringResource(R.string.report_categories_title), subtitle = stringResource(R.string.report_categories_share_note))
        val shown = stats.categories.filter { it.frequency > 0 }
        if (shown.isEmpty()) {
            Text(stringResource(R.string.report_breakdown_empty), style = MaterialTheme.typography.bodyMedium)
            return@ReportCard
        }
        val description = shown.joinToString { "${state.label(it.category.name)} ${formatShare(it.sharePct)}%" }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(104.dp).clearAndSetSemantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                Donut(shown.map { it.frequency.toFloat() to categoryColor(it.category) })
                Text(
                    stats.analyzedTotals.frequency.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                shown.forEach { category ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.padding(top = 5.dp)) { CategoryDot(categoryColor(category.category)) }
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(
                                    state.label(category.category.name),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    stringResource(R.string.report_category_share, formatShare(category.sharePct)),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }
                            Text(
                                listOf(entriesText(category.frequency), piecesAndGramsText(category.pieces, category.grams)).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatShare(value: Double): String = String.format(java.util.Locale.ENGLISH, "%.1f", value)

@Composable
private fun CategoryDot(color: Color) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@Composable
private fun BreakdownCard(stats: WeeklyStatsDto, state: ReportUiState) {
    ReportCard {
        SectionTitle(stringResource(R.string.report_breakdown_title))
        if (stats.subcategories.isEmpty()) {
            Text(stringResource(R.string.report_breakdown_empty), style = MaterialTheme.typography.bodyMedium)
        }
        stats.subcategories.forEach { sub ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryDot(categoryColor(sub.category))
                Text(state.label(sub.code), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    entriesAndQuantity(sub.frequency, sub.quantity, sub.unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (stats.special.frequency > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryDot(MenosanTheme.colors.special)
                Text(
                    stringResource(R.string.report_special_line, entriesAndPieces(stats.special.frequency, stats.special.pieces)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun trendIcon(trend: Trend): Pair<ImageVector, Color> = when (trend) {
    Trend.DECREASED -> Icons.AutoMirrored.Filled.TrendingDown to MaterialTheme.colorScheme.primary
    Trend.INCREASED -> Icons.AutoMirrored.Filled.TrendingUp to MaterialTheme.colorScheme.tertiary
    Trend.SAME, Trend.UNKNOWN -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun ComparisonCard(comparison: ComparisonDto?, state: ReportUiState) {
    ReportCard {
        SectionTitle(stringResource(R.string.report_comparison_title))
        if (comparison == null) {
            Text(stringResource(R.string.report_comparison_none), style = MaterialTheme.typography.bodyMedium)
        } else {
            ComparisonDetails(comparison, state)
        }
    }
}

@Composable
private fun ComparisonDetails(comparison: ComparisonDto, state: ReportUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.report_comparison_week, formatWeekRange(comparison.previousWeekStart, comparison.previousWeekStart.plusDays(6))),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val pieces = comparison.pieces
        val grams = comparison.grams
        if (pieces.previous > 0 || pieces.current > 0 || (grams.previous == 0 && grams.current == 0)) {
            ComparisonHeadline(
                pieces,
                QuantityUnit.PIECES,
                when (pieces.trend) {
                    Trend.DECREASED -> pluralStringResource(R.plurals.report_comparison_fewer, absInt(pieces.delta), absInt(pieces.delta))
                    Trend.INCREASED -> pluralStringResource(R.plurals.report_comparison_more, absInt(pieces.delta), absInt(pieces.delta))
                    else -> stringResource(R.string.report_comparison_same)
                },
            )
        }
        if (grams.previous > 0 || grams.current > 0) {
            ComparisonHeadline(
                grams,
                QuantityUnit.GRAMS,
                when (grams.trend) {
                    Trend.DECREASED -> stringResource(R.string.report_comparison_food_less, gramsText(absInt(grams.delta)))
                    Trend.INCREASED -> stringResource(R.string.report_comparison_food_more, gramsText(absInt(grams.delta)))
                    else -> stringResource(R.string.report_comparison_food_same)
                },
            )
        }
        comparison.categories.filter { it.previous > 0 || it.current > 0 }.forEach { row ->
            val label = state.label(row.category.name).let {
                if (row.unit == QuantityUnit.GRAMS) stringResource(R.string.report_comparison_category_food, it) else it
            }
            ComparisonRow(label, row.previous, row.current, row.unit, row.trend, categoryColor(row.category))
        }
        var showAll by rememberSaveable { mutableStateOf(false) }
        if (comparison.subcategories.isNotEmpty()) {
            TextButton(onClick = { showAll = !showAll }) {
                Text(stringResource(if (showAll) R.string.report_comparison_hide_all else R.string.report_comparison_show_all))
            }
        }
        if (showAll) {
            comparison.subcategories.forEach { row ->
                ComparisonRow(state.label(row.code), row.previous, row.current, row.unit, row.trend, categoryColor(row.category))
            }
        }
    }
}

@Composable
private fun ComparisonHeadline(row: ComparisonRowDto, unit: QuantityUnit, summary: String) {
    val (icon, tint) = trendIcon(row.trend)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = tint)
            Text(
                stringResource(R.string.report_comparison_total, quantityText(row.previous, unit), quantityText(row.current, unit)),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            row.deltaPct?.let {
                Text(
                    stringResource(R.string.report_comparison_pct, signedPercent(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(summary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ComparisonRow(label: String, previous: Int, current: Int, unit: QuantityUnit, trend: Trend, color: Color) {
    val (icon, tint) = trendIcon(trend)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryDot(color)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.report_comparison_row, quantityText(previous, unit), quantityText(current, unit)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotspotCard(hotspot: HotspotDto, state: ReportUiState) {
    ReportCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(hotspot.rank.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(state.label(hotspot.subcategory), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    entriesAndQuantity(hotspot.frequency, hotspot.quantity, hotspot.unit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (hotspot.rank == 1) {
                Pill(stringResource(R.string.report_top_hotspot), MenosanTheme.colors.highlight, MenosanTheme.colors.onHighlight)
            }
            hotspot.criteria.mapNotNull { criterionLabel(it) }.forEach {
                Pill(it, MenosanTheme.colors.pending, MenosanTheme.colors.onPending)
            }
        }
    }
}

@Composable
private fun AdoptedIdeas(view: ReportView, state: ReportUiState, onOpenDetails: (String) -> Unit) {
    val adopted = adoptedIdeas(view.report)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            stringResource(R.string.report_ideas_tried_title),
            subtitle = stringResource(R.string.report_ideas_tried_subtitle).takeIf { adopted.isNotEmpty() },
        )
        if (adopted.isEmpty()) {
            Text(stringResource(R.string.report_ideas_tried_none), style = MaterialTheme.typography.bodyMedium)
        }
        adopted.forEach { (hotspot, rec) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.report_ideas_tried_for, state.label(hotspot)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RecommendationCard(
                    rec = rec,
                    canAdopt = false,
                    pending = false,
                    notMeasured = view.followupNotMeasured,
                    onToggleAdopt = {},
                    onOpenDetails = { onOpenDetails(rec.interventionId) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdeasSection(
    view: ReportView,
    state: ReportUiState,
    onSetAdopted: (String, Boolean) -> Unit,
    onOpenDetails: (String) -> Unit,
) {
    if (!view.canAdopt) {
        AdoptedIdeas(view, state, onOpenDetails)
        return
    }
    val hotspots = view.report.hotspots
    var selected by rememberSaveable(view.weekStart.toString()) { mutableIntStateOf(0) }
    val hotspot = hotspots[selected.coerceIn(0, hotspots.lastIndex)]
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            stringResource(R.string.report_ideas_title),
            subtitle = stringResource(R.string.report_ideas_subtitle_latest),
        )
        if (hotspots.size > 1) {
            Text(stringResource(R.string.report_choose_hotspot), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                hotspots.forEachIndexed { index, h ->
                    FilterChip(
                        selected = h == hotspot,
                        onClick = { selected = index },
                        label = { Text(state.label(h.subcategory)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }
        if (hotspot.recommendations.isEmpty()) {
            Text(stringResource(R.string.report_ideas_none), style = MaterialTheme.typography.bodyMedium)
        }
        hotspot.recommendations.forEach { rec: RecommendationDto ->
            RecommendationCard(
                rec = rec,
                canAdopt = view.canAdopt,
                pending = rec.interventionId in state.pendingAdoptions,
                notMeasured = view.followupNotMeasured,
                onToggleAdopt = { onSetAdopted(rec.interventionId, !rec.adopted) },
                onOpenDetails = { onOpenDetails(rec.interventionId) },
            )
        }
    }
}

@Composable
private fun RecapCard(recap: LastWeekRecap, state: ReportUiState) {
    ReportCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            SectionTitle(stringResource(R.string.report_recap_title, formatWeekRange(recap.weekStart, recap.weekEnd)))
        }
        if (recap.hotspots.isNotEmpty()) {
            Text(
                stringResource(R.string.report_recap_hotspots, recap.hotspots.joinToString(", ") { state.label(it.subcategory) }),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (recap.adopted.isNotEmpty()) {
            Text(
                stringResource(R.string.report_recap_tried, recap.adopted.joinToString(", ") { it.title }),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
