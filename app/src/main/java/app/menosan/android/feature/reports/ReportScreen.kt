package app.menosan.android.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.menosan.android.R
import app.menosan.android.core.ui.components.screenInsetsPadding
import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.ui.components.MessageBanner
import app.menosan.android.core.ui.components.Pill
import app.menosan.android.core.ui.components.primaryButtonColors
import app.menosan.android.core.ui.components.gramsText
import app.menosan.android.core.ui.components.quantityText
import app.menosan.android.core.ui.components.ScreenHeader
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.data.remote.dto.ComparisonDto
import app.menosan.android.data.remote.dto.ComparisonRowDto
import app.menosan.android.data.remote.dto.HotspotCriterion
import app.menosan.android.data.remote.dto.HotspotDto
import app.menosan.android.data.remote.dto.RecommendationDto
import app.menosan.android.data.remote.dto.Trend
import app.menosan.android.data.remote.dto.WeeklyStatsDto
import app.menosan.android.data.repo.LastWeekRecap
import app.menosan.android.data.repo.ReportView
import app.menosan.android.feature.interventions.AdoptButton
import app.menosan.android.feature.interventions.ImpactCard
import app.menosan.android.feature.interventions.RecommendationCard
import app.menosan.android.feature.interventions.RecommendationDetailsSheet
import kotlinx.coroutines.launch

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
        ReportContent(
            state = state,
            initialTab = viewModel.initialTab,
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onSetAdopted = viewModel::setAdopted,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

/** What the tabs share: the open tab, the hotspot picked for ideas, and the actions between them. */
private class ReportTabActions(
    val tab: ReportTab,
    val selectedHotspot: Int,
    val onSelectTab: (ReportTab) -> Unit,
    val onShowIdeasFor: (hotspotIndex: Int) -> Unit,
    val onSelectHotspot: (Int) -> Unit,
    val onSetAdopted: (String, Boolean) -> Unit,
    val onOpenDetails: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContent(
    state: ReportUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSetAdopted: (interventionId: String, adopted: Boolean) -> Unit,
    initialTab: ReportTab = ReportTab.OVERVIEW,
) {
    var details by remember { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var selectedHotspot by rememberSaveable { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val selectTab: (ReportTab) -> Unit = { next ->
        tab = next
        scope.launch { listState.scrollToItem(0) }
    }
    val actions = ReportTabActions(
        tab = tab,
        selectedHotspot = selectedHotspot,
        onSelectTab = selectTab,
        onShowIdeasFor = { index ->
            selectedHotspot = index
            selectTab(ReportTab.IDEAS)
        },
        onSelectHotspot = { selectedHotspot = it },
        onSetAdopted = onSetAdopted,
        onOpenDetails = { details = it },
    )
    Column(Modifier.fillMaxSize().screenInsetsPadding()) {
        ScreenHeader(title = stringResource(R.string.nav_report), onBack = onBack)
        val view = state.view
        PullToRefreshBox(
            isRefreshing = state.refreshing && view != null,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when {
                    view != null -> reportSections(state, view, actions)
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

private fun LazyListScope.reportSections(state: ReportUiState, view: ReportView, actions: ReportTabActions) {
    item(key = "header") { WeekHeader(view) }
    stickyHeader(key = "tabs") { ReportTabs(actions.tab, actions.onSelectTab) }
    if (!view.isProvisional && state.problem != null) {
        item(key = "saved") { MessageBanner(stringResource(R.string.report_showing_saved), icon = Icons.Outlined.CloudOff) }
    }
    when (actions.tab) {
        ReportTab.OVERVIEW -> overviewTab(state, view, actions)
        ReportTab.HOTSPOTS -> hotspotsTab(state, view, actions)
        ReportTab.IDEAS -> ideasTab(state, view, actions)
        ReportTab.PROGRESS -> progressTab(state, view)
    }
}

private fun LazyListScope.overviewTab(state: ReportUiState, view: ReportView, actions: ReportTabActions) {
    val report = view.report
    if (view.isProvisional) {
        item(key = "offline") {
            val text = stringResource(R.string.report_offline_banner) +
                if (view.missingComparisons) "\n" + stringResource(R.string.report_offline_banner_partial) else ""
            MessageBanner(text, icon = Icons.Outlined.CloudOff)
        }
    }
    item(key = "glance") { GlanceCard(report.stats, report.comparison) }
    item(key = "focus") { WhereToFocusCard(report.hotspots, state, onOpenHotspots = { actions.onSelectTab(ReportTab.HOTSPOTS) }) }
    val topIdea = report.hotspots.minByOrNull { it.rank }?.recommendations?.firstOrNull()
    if (view.canAdopt && !view.isProvisional && topIdea != null) {
        item(key = "one-idea") {
            OneIdeaCard(
                rec = topIdea,
                pending = topIdea.interventionId in state.pendingAdoptions,
                onToggleAdopt = { actions.onSetAdopted(topIdea.interventionId, !topIdea.adopted) },
                onOpenDetails = { actions.onOpenDetails(topIdea.interventionId) },
                onSeeAll = { actions.onShowIdeasFor(0) },
            )
        }
    }
    item(key = "categories") { CategoryCard(report.stats, state) }
}

private fun LazyListScope.hotspotsTab(state: ReportUiState, view: ReportView, actions: ReportTabActions) {
    val hotspots = view.report.hotspots.sortedBy { it.rank }
    if (hotspots.isEmpty()) {
        item(key = "special-only") { MessageBanner(stringResource(R.string.report_hotspots_special_only)) }
        return
    }
    item(key = "hotspots-intro") { TabIntro(stringResource(R.string.report_hotspots_intro)) }
    val showIdeas = view.canAdopt && !view.isProvisional
    hotspots.forEachIndexed { index, hotspot ->
        item(key = "hotspot-${hotspot.subcategory}") {
            HotspotCard(
                hotspot = hotspot,
                state = state,
                onSeeIdeas = if (showIdeas && hotspot.recommendations.isNotEmpty()) ({ actions.onShowIdeasFor(index) }) else null,
            )
        }
    }
}

private fun LazyListScope.ideasTab(state: ReportUiState, view: ReportView, actions: ReportTabActions) {
    val hotspots = view.report.hotspots.sortedBy { it.rank }
    when {
        view.isProvisional -> item(key = "ideas-offline") {
            MessageBanner(stringResource(R.string.report_offline_banner), icon = Icons.Outlined.CloudOff)
        }
        hotspots.isEmpty() -> item(key = "special-only") { MessageBanner(stringResource(R.string.report_hotspots_special_only)) }
        !view.canAdopt -> item(key = "ideas-tried") { AdoptedIdeas(view, state, actions.onOpenDetails) }
        else -> {
            val hotspot = hotspots[actions.selectedHotspot.coerceIn(0, hotspots.lastIndex)]
            item(key = "ideas-intro") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabIntro(stringResource(R.string.report_ideas_subtitle_latest))
                    if (hotspots.size > 1) HotspotChips(hotspots, hotspot, state, actions.onSelectHotspot)
                }
            }
            if (hotspot.recommendations.isEmpty()) {
                item(key = "ideas-none") { Text(stringResource(R.string.report_ideas_none), style = MaterialTheme.typography.bodyMedium) }
            }
            items(hotspot.recommendations, key = { "idea-${hotspot.subcategory}-${it.interventionId}" }) { rec ->
                RecommendationCard(
                    rec = rec,
                    canAdopt = true,
                    pending = rec.interventionId in state.pendingAdoptions,
                    notMeasured = view.followupNotMeasured,
                    onToggleAdopt = { actions.onSetAdopted(rec.interventionId, !rec.adopted) },
                    onOpenDetails = { actions.onOpenDetails(rec.interventionId) },
                )
            }
        }
    }
}

private fun LazyListScope.progressTab(state: ReportUiState, view: ReportView) {
    val report = view.report
    if (report.impacts.isEmpty()) {
        item(key = "impact-none") {
            ReportCard {
                SectionTitle(stringResource(R.string.report_progress_none_title))
                Text(stringResource(R.string.report_progress_none_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        item(key = "impact-title") {
            SectionTitle(stringResource(R.string.report_impact_title), subtitle = stringResource(R.string.report_impact_subtitle))
        }
        items(report.impacts, key = { "impact-${it.interventionId}" }) { ImpactCard(it, state.label(it.targetSubcategory)) }
    }
    item(key = "comparison") { ComparisonCard(report.comparison, state) }
    view.recap?.let { recap -> item(key = "recap") { RecapCard(recap, state) } }
    item(key = "how-it-works") {
        ReportCard {
            SectionTitle(stringResource(R.string.report_progress_how_title))
            Text(
                stringResource(R.string.report_progress_how_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekHeader(view: ReportView) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            weekRangeWithYear(view.weekStart, view.weekEnd),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            stringResource(R.string.report_week_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (view.isProvisional || view.canAdopt) {
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (view.isProvisional) {
                    Pill(stringResource(R.string.reports_offline_summary), MenosanTheme.colors.calm, MenosanTheme.colors.onCalm)
                }
                if (view.canAdopt) {
                    Pill(stringResource(R.string.reports_latest), MenosanTheme.colors.highlight, MenosanTheme.colors.onHighlight)
                }
            }
        }
    }
}

@Composable
private fun ReportTabs(selected: ReportTab, onSelect: (ReportTab) -> Unit) {
    // Opaque background so cards don't show through while the tabs stick to the top.
    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MenosanTheme.colors.mist, MaterialTheme.shapes.medium)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ReportTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tabLabel(tab),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun tabLabel(tab: ReportTab): String = stringResource(
    when (tab) {
        ReportTab.OVERVIEW -> R.string.report_tab_overview
        ReportTab.HOTSPOTS -> R.string.report_tab_hotspots
        ReportTab.IDEAS -> R.string.report_tab_ideas
        ReportTab.PROGRESS -> R.string.report_tab_progress
    },
)

@Composable
private fun TabIntro(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GlanceCard(stats: WeeklyStatsDto, comparison: ComparisonDto?) {
    ReportCard {
        SectionTitle(stringResource(R.string.report_glance_title))
        val totals = stats.analyzedTotals
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(totals.frequency.toString(), stringResource(R.string.report_totals_entries), Modifier.weight(1f))
            MetricTile(totals.pieces.toString(), stringResource(R.string.report_totals_pieces), Modifier.weight(1f))
            if (totals.grams > 0) {
                MetricTile(gramsText(totals.grams), stringResource(R.string.report_totals_food), Modifier.weight(1f))
            }
        }
        comparison?.let { DeltaLine(it) }
    }
}

@Composable
private fun MetricTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(MenosanTheme.colors.mist, MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One line on how the week compares: pieces when any were logged, otherwise food grams. */
@Composable
private fun DeltaLine(comparison: ComparisonDto) {
    val usePieces = comparison.pieces.previous > 0 || comparison.pieces.current > 0
    val row = if (usePieces) comparison.pieces else comparison.grams
    if (!usePieces && row.previous == 0 && row.current == 0) return
    val summary = if (usePieces) {
        when (row.trend) {
            Trend.DECREASED -> pluralStringResource(R.plurals.report_comparison_fewer, absInt(row.delta), absInt(row.delta))
            Trend.INCREASED -> pluralStringResource(R.plurals.report_comparison_more, absInt(row.delta), absInt(row.delta))
            else -> stringResource(R.string.report_comparison_same)
        }
    } else {
        when (row.trend) {
            Trend.DECREASED -> stringResource(R.string.report_comparison_food_less, gramsText(absInt(row.delta)))
            Trend.INCREASED -> stringResource(R.string.report_comparison_food_more, gramsText(absInt(row.delta)))
            else -> stringResource(R.string.report_comparison_food_same)
        }
    }
    val unit = if (usePieces) QuantityUnit.PIECES else QuantityUnit.GRAMS
    val range = stringResource(R.string.report_comparison_total, quantityText(row.previous, unit), quantityText(row.current, unit))
    val (icon, _) = trendIcon(row.trend)
    val (background, content) = when (row.trend) {
        Trend.DECREASED -> MenosanTheme.colors.calm to MenosanTheme.colors.onCalm
        Trend.INCREASED -> MenosanTheme.colors.pending to MenosanTheme.colors.onPending
        else -> MenosanTheme.colors.mist to MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(background, MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.report_delta_line, summary, range), style = MaterialTheme.typography.bodySmall, color = content)
    }
}

@Composable
private fun WhereToFocusCard(hotspots: List<HotspotDto>, state: ReportUiState, onOpenHotspots: () -> Unit) {
    ReportCard {
        SectionTitle(stringResource(R.string.report_focus_title))
        if (hotspots.isEmpty()) {
            Text(stringResource(R.string.report_hotspots_special_only), style = MaterialTheme.typography.bodyMedium)
            return@ReportCard
        }
        Column {
            hotspots.sortedBy { it.rank }.forEachIndexed { index, hotspot ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onOpenHotspots)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RankBadge(hotspot.rank)
                    Column(Modifier.weight(1f)) {
                        Text(state.label(hotspot.subcategory), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            entriesAndQuantity(hotspot.frequency, hotspot.quantity, hotspot.unit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TextButton(onClick = onOpenHotspots) {
            Text(stringResource(R.string.report_explore_hotspots))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    Box(
        Modifier.size(32.dp).background(MenosanTheme.colors.mist, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            rank.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun OneIdeaCard(
    rec: RecommendationDto,
    pending: Boolean,
    onToggleAdopt: () -> Unit,
    onOpenDetails: () -> Unit,
    onSeeAll: () -> Unit,
) {
    ReportCard {
        SectionTitle(stringResource(R.string.report_one_idea_title))
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpenDetails),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(40.dp).background(MenosanTheme.colors.mist, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(rec.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    rec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        listOfNotNull(costLabel(rec.costLevel), effortLabel(rec.effort)).takeIf { it.isNotEmpty() }?.let {
            Pill(it.joinToString(" · "), MenosanTheme.colors.mist, MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onSeeAll) { Text(stringResource(R.string.report_see_all_ideas)) }
            Spacer(Modifier.weight(1f))
            AdoptButton(adopted = rec.adopted, pending = pending, onClick = onToggleAdopt)
        }
    }
}

@Composable
private fun CategoryCard(stats: WeeklyStatsDto, state: ReportUiState) {
    var showItems by rememberSaveable { mutableStateOf(false) }
    ReportCard {
        SectionTitle(stringResource(R.string.report_categories_title), subtitle = stringResource(R.string.report_categories_share_note))
        val shown = stats.categories.filter { it.frequency > 0 }
        if (shown.isEmpty()) {
            Text(stringResource(R.string.report_breakdown_empty), style = MaterialTheme.typography.bodyMedium)
        }
        shown.forEach { category ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    state.label(category.category.name),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(104.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ShareBar((category.sharePct / 100.0).toFloat(), categoryColor(category.category), Modifier.weight(1f))
                Text(
                    stringResource(R.string.report_category_share, formatShare(category.sharePct)),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
        if (stats.special.frequency > 0) {
            Text(
                stringResource(R.string.report_special_line, entriesAndPieces(stats.special.frequency, stats.special.pieces)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (stats.subcategories.isNotEmpty()) {
            TextButton(onClick = { showItems = !showItems }) {
                Text(stringResource(if (showItems) R.string.report_hide_logged else R.string.report_show_logged))
            }
        }
        if (showItems) {
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
        }
    }
}

private fun formatShare(value: Double): String = String.format(java.util.Locale.ENGLISH, "%.1f", value)

@Composable
private fun CategoryDot(color: Color) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
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

@Composable
private fun HotspotCard(hotspot: HotspotDto, state: ReportUiState, onSeeIdeas: (() -> Unit)?) {
    val eyebrow = buildList {
        add(if (hotspot.rank == 1) stringResource(R.string.report_top_hotspot) else stringResource(R.string.report_hotspot_rank, hotspot.rank))
        hotspot.criteria.filter { it != HotspotCriterion.AVOIDABLE }.mapNotNullTo(this) { criterionLabel(it) }
    }.joinToString(" · ")
    val detail = buildList {
        add(stringResource(R.string.report_hotspot_quantity_in, quantityText(hotspot.quantity, hotspot.unit), entriesText(hotspot.frequency)))
        if (HotspotCriterion.AVOIDABLE in hotspot.criteria) add(stringResource(R.string.report_criterion_avoidable))
    }.joinToString(" · ")
    ReportCard {
        Text(
            eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(state.label(hotspot.subcategory), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onSeeIdeas != null) {
            Button(onClick = onSeeIdeas, shape = MaterialTheme.shapes.small, colors = primaryButtonColors(), modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.insights_see_ideas))
            }
        }
    }
}

@Composable
private fun HotspotChips(hotspots: List<HotspotDto>, selected: HotspotDto, state: ReportUiState, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        hotspots.forEachIndexed { index, h ->
            FilterChip(
                selected = h == selected,
                onClick = { onSelect(index) },
                label = { Text(state.label(h.subcategory)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
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
