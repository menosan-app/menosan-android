package app.menosan.android.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.menosan.android.R
import app.menosan.android.core.network.LocalOnline
import app.menosan.android.core.ui.components.CardElevation
import app.menosan.android.core.ui.components.Donut
import app.menosan.android.core.ui.components.GroupedList
import app.menosan.android.core.ui.components.MetaText
import app.menosan.android.core.ui.components.Pill
import app.menosan.android.core.ui.components.QuietCard
import app.menosan.android.core.ui.components.SectionGap
import app.menosan.android.core.ui.components.SectionHeader
import app.menosan.android.core.ui.components.piecesAndGramsText
import app.menosan.android.core.ui.components.quantityText
import app.menosan.android.core.ui.components.screenInsetsPadding
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.data.remote.dto.ImpactDto
import app.menosan.android.data.remote.dto.Trend
import app.menosan.android.feature.entries.DeleteEntryDialog
import app.menosan.android.feature.entries.EntryFormats
import app.menosan.android.feature.entries.EntryRow
import app.menosan.android.feature.logging.labelRes
import app.menosan.android.feature.reports.categoryColor
import app.menosan.android.feature.reports.formatWeekRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class HomeActions(
    val onLogManually: () -> Unit,
    val onLogWithPhoto: () -> Unit,
    val onOpenReport: (LocalDate) -> Unit,
    val onViewAllEntries: () -> Unit,
    val onOpenEntry: (String) -> Unit,
    val onEditEntry: (String) -> Unit,
)

@Composable
fun HomeRoute(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.messageEvents.collect { snackbar.showSnackbar(resources.getString(it)) }
    }
    Box(Modifier.fillMaxSize()) {
        HomeScreen(state = state, actions = actions, onDeleteEntry = viewModel::delete)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions, onDeleteEntry: (String) -> Unit) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenInsetsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        Header(state)
        Section(
            stringResource(R.string.dashboard_this_week),
            trailing = { MetaText(EntryFormats.weekRange(state.weekStart)) },
        ) { WeekCard(state) }
        if (!state.loading) NextStepCard(state.nextStep, actions)
        val report = state.latestReport
        if (report == null) {
            FirstReportCard()
        } else {
            Section(
                stringResource(
                    when {
                        report.isProvisional -> R.string.reports_offline_summary
                        report.isLastWeek -> R.string.dashboard_report_last_week
                        else -> R.string.dashboard_report_latest
                    },
                ),
            ) { ReportCard(report, state, actions) }
        }
        Section(
            stringResource(R.string.dashboard_recent_title),
            trailing = if (state.recentEntries.isNotEmpty()) {
                { TextButton(onClick = actions.onViewAllEntries) { Text(stringResource(R.string.dashboard_view_all)) } }
            } else {
                null
            },
        ) { RecentEntries(state, actions) { pendingDelete = it } }
    }

    pendingDelete?.let { id ->
        DeleteEntryDialog(
            onConfirm = {
                pendingDelete = null
                onDeleteEntry(id)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun Section(
    title: String,
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title, trailing = trailing)
        content()
    }
}

private val TODAY = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)

@Composable
private fun Header(state: HomeUiState) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            text = state.firstName?.let { stringResource(R.string.dashboard_greeting_name, it) }
                ?: stringResource(R.string.dashboard_greeting),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = TODAY.format(state.today),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NextStepCard(step: NextStep, actions: HomeActions) {
    val (container, content) = when (step) {
        is NextStep.FixSync -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        is NextStep.KeepGoing -> MenosanTheme.colors.mist to MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    val icon = when (step) {
        is NextStep.FixSync -> Icons.Outlined.ErrorOutline
        is NextStep.PickIdea -> Icons.Outlined.EmojiObjects
        NextStep.LogToday -> Icons.Outlined.EditNote
        is NextStep.KeepGoing -> Icons.Outlined.CheckCircle
    }
    val title = when (step) {
        is NextStep.FixSync -> pluralStringResource(R.plurals.dashboard_next_fix_title, step.count, step.count)
        is NextStep.PickIdea -> stringResource(R.string.dashboard_next_idea_title)
        NextStep.LogToday -> stringResource(R.string.dashboard_next_log_title)
        is NextStep.KeepGoing -> pluralStringResource(R.plurals.dashboard_next_keep_title, step.loggedToday, step.loggedToday)
    }
    val body = when (step) {
        is NextStep.FixSync -> stringResource(R.string.dashboard_next_fix_body)
        is NextStep.PickIdea -> stringResource(R.string.dashboard_next_idea_body, formatWeekRange(step.weekStart, step.weekEnd))
        NextStep.LogToday -> stringResource(R.string.dashboard_next_log_body)
        is NextStep.KeepGoing -> stringResource(R.string.dashboard_next_keep_body)
    }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
        shadowElevation = CardElevation,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.semantics { heading() },
                )
                Text(body, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.85f))
            }
            when (step) {
                is NextStep.FixSync ->
                    CardButton(stringResource(R.string.dashboard_next_fix_action), content, container, Modifier.fillMaxWidth(), onClick = actions.onViewAllEntries)
                is NextStep.PickIdea ->
                    CardButton(stringResource(R.string.dashboard_next_idea_action), content, container, Modifier.fillMaxWidth()) { actions.onOpenReport(step.weekStart) }
                NextStep.LogToday -> LogButtons(actions, content, container, filled = true)
                is NextStep.KeepGoing -> LogButtons(actions, content, container, filled = false)
            }
        }
    }
}

@Composable
private fun CardButton(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = textColor),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            modifier = Modifier.padding(start = if (icon != null) 8.dp else 0.dp),
        )
    }
}

@Composable
private fun LogButtons(actions: HomeActions, content: Color, container: Color, filled: Boolean) {
    val online = LocalOnline.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            val manual = stringResource(R.string.dashboard_log_manual)
            if (filled) {
                CardButton(manual, content, container, Modifier.weight(1f), Icons.Outlined.EditNote, actions.onLogManually)
            } else {
                OutlinedLogButton(manual, Icons.Outlined.EditNote, content, actions.onLogManually, Modifier.weight(1f))
            }
            OutlinedLogButton(
                stringResource(R.string.dashboard_log_photo),
                if (online) Icons.Outlined.CameraAlt else Icons.Outlined.CloudOff,
                content,
                actions.onLogWithPhoto,
                Modifier.weight(1f),
                enabled = online,
            )
        }
        if (!online) {
            Text(stringResource(R.string.photo_offline_body), style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun OutlinedLogButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, color.copy(alpha = if (enabled) 0.6f else 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color, disabledContentColor = color.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun WeekCard(state: HomeUiState) {
    val summary = state.summary
    QuietCard(spacing = 20.dp) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                summary.entries.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 56.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(
                    pluralStringResource(R.plurals.count_entries_word, summary.entries),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                MetaText(piecesAndGramsText(summary.pieces, summary.grams))
            }
        }
        val todayIndex = if (state.today >= state.weekStart) EntryFormats.dayIndex(state.today) else -1
        DayBars(summary.entriesPerDay, todayIndex)
        if (state.categoryShares.isNotEmpty()) WasteMix(state)
        if (state.pendingCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Outlined.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                MetaText(pluralStringResource(R.plurals.dashboard_pending_short, state.pendingCount, state.pendingCount))
            }
        }
    }
}

@Composable
private fun DayBars(perDay: List<Int>, todayIndex: Int) {
    val max = (perDay.maxOrNull() ?: 0).coerceAtLeast(1)
    val days = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1)
    val labels = days.map { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
    val description = stringResource(R.string.dashboard_bars_description, labels.zip(perDay).joinToString { (d, n) -> "$d $n" })
    val bar = MaterialTheme.colorScheme.secondary
    val today = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(40.dp)) {
            val slot = size.width / 7f
            val width = (slot * 0.42f).coerceAtMost(20.dp.toPx())
            val radius = CornerRadius(width / 2f, width / 2f)
            perDay.forEachIndexed { i, count ->
                val left = i * slot + (slot - width) / 2f
                drawRoundRect(track, Offset(left, 0f), Size(width, size.height), radius)
                if (count > 0) {
                    val h = (size.height * count / max).coerceAtLeast(width)
                    drawRoundRect(if (i == todayIndex) today else bar, Offset(left, size.height - h), Size(width, h), radius)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEachIndexed { i, label ->
                Text(
                    label.take(1),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (i == todayIndex) FontWeight.Bold else FontWeight.Normal),
                    color = if (i == todayIndex) today else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WasteMix(state: HomeUiState) {
    val shares = state.categoryShares
    val labels = shares.map { stringResource(it.category.labelRes()) }
    val description = stringResource(
        R.string.dashboard_mix_description,
        shares.indices.joinToString { "${labels[it]} ${shares[it].percent}%" },
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(88.dp).clearAndSetSemantics { contentDescription = description }) {
                Donut(shares.map { it.entries.toFloat() to categoryColor(it.category) }, thickness = 14.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shares.forEachIndexed { i, share ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(10.dp).background(categoryColor(share.category), CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(labels[i], style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            MetaText(
                                stringResource(
                                    R.string.count_joined,
                                    pluralStringResource(R.plurals.count_entries, share.entries, share.entries),
                                    piecesAndGramsText(share.pieces, share.grams),
                                ),
                                maxLines = 1,
                            )
                        }
                        Text(
                            stringResource(R.string.dashboard_mix_percent, share.percent),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        }
        if (state.specialEntries > 0) {
            MetaText(pluralStringResource(R.plurals.dashboard_mix_special, state.specialEntries, state.specialEntries))
        }
    }
}

@Composable
private fun FirstReportCard() {
    Surface(
        color = MenosanTheme.colors.mist,
        shape = MaterialTheme.shapes.large,
        shadowElevation = CardElevation,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.dashboard_first_report_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.dashboard_first_report_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ReportCard(report: LatestReportCard, state: HomeUiState, actions: HomeActions) {
    QuietCard(
        onClick = { actions.onOpenReport(report.weekStart) },
        onClickLabel = stringResource(R.string.action_open_report),
        spacing = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatWeekRange(report.weekStart, report.weekEnd),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                MetaText(
                    stringResource(
                        R.string.count_joined,
                        pluralStringResource(R.plurals.count_entries, report.analyzedEntries, report.analyzedEntries),
                        piecesAndGramsText(report.analyzedPieces, report.analyzedGrams),
                    ),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (report.isProvisional) {
            Text(stringResource(R.string.dashboard_report_offline_body), style = MaterialTheme.typography.bodyMedium)
        }

        val hotspot = report.topHotspot
        if (hotspot != null) {
            Detail(stringResource(R.string.dashboard_hotspot_title)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(10.dp).background(MenosanTheme.colors.highlight, CircleShape))
                    Text(
                        state.labelOf(hotspot.subcategory),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                    )
                    MetaText(
                        stringResource(
                            R.string.count_joined,
                            pluralStringResource(R.plurals.count_entries, hotspot.frequency, hotspot.frequency),
                            quantityText(hotspot.quantity, hotspot.unit),
                        ),
                    )
                }
            }
        } else if (report.analyzedEntries == 0) {
            MetaText(stringResource(R.string.dashboard_report_only_special))
        }

        if (report.trying.isNotEmpty()) {
            Detail(stringResource(R.string.dashboard_trying_title)) {
                report.trying.forEach { title ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.EmojiObjects, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (report.impacts.isNotEmpty()) {
            Detail(stringResource(R.string.impact_title)) {
                report.impacts.forEach { ImpactLine(it, state) }
            }
        }
    }
}

@Composable
private fun Detail(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MetaText(label)
        content()
    }
}

@Composable
private fun ImpactLine(impact: ImpactDto, state: HomeUiState) {
    val (icon, tint) = when (impact.result) {
        Trend.DECREASED -> Icons.AutoMirrored.Filled.TrendingDown to MaterialTheme.colorScheme.primary
        Trend.INCREASED -> Icons.AutoMirrored.Filled.TrendingUp to MaterialTheme.colorScheme.tertiary
        else -> Icons.AutoMirrored.Filled.TrendingFlat to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val result = when (impact.result) {
        Trend.DECREASED -> R.string.impact_decreased
        Trend.SAME -> R.string.impact_same
        Trend.INCREASED -> R.string.impact_increased
        Trend.UNKNOWN -> null
    }
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(impact.title, style = MaterialTheme.typography.bodyMedium)
            MetaText(
                "${state.labelOf(impact.targetSubcategory)} · " + stringResource(
                    R.string.quantity_change,
                    quantityText(impact.baselineQuantity, impact.unit),
                    quantityText(impact.followupQuantity, impact.unit),
                ),
            )
        }
        if (result != null) {
            Pill(
                stringResource(result),
                background = if (impact.result == Trend.DECREASED) MenosanTheme.colors.calm else MenosanTheme.colors.mist,
                content = if (impact.result == Trend.DECREASED) MenosanTheme.colors.onCalm else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RecentEntries(state: HomeUiState, actions: HomeActions, onDelete: (String) -> Unit) {
    if (state.recentEntries.isEmpty()) {
        MetaText(stringResource(R.string.dashboard_recent_empty))
    } else {
        GroupedList(state.recentEntries, dividerInset = 68.dp) { entry ->
            EntryRow(
                entry = entry,
                subcategoryLabel = state.labelOf(entry.subcategory),
                onOpen = { actions.onOpenEntry(entry.id) },
                onEdit = { actions.onEditEntry(entry.id) },
                onDelete = { onDelete(entry.id) },
            )
        }
    }
}

private val previewActions = HomeActions({}, {}, {}, {}, {}, {})

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun HomeNewUserPreview() {
    MenosanTheme {
        HomeScreen(
            state = HomeUiState.build(
                firstName = "Liza",
                weekStart = LocalDate.of(2026, 9, 27),
                today = LocalDate.of(2026, 9, 29),
                weekEntries = emptyList(),
                pendingCount = 0,
                latest = null,
                labels = emptyMap(),
            ),
            actions = previewActions,
            onDeleteEntry = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, backgroundColor = 0xFF171C19)
@Composable
private fun HomeNewUserDarkPreview() {
    MenosanTheme(darkTheme = true) {
        HomeScreen(
            state = HomeUiState.build(
                firstName = null,
                weekStart = LocalDate.of(2026, 9, 27),
                today = LocalDate.of(2026, 9, 29),
                weekEntries = emptyList(),
                pendingCount = 2,
                latest = null,
                labels = emptyMap(),
            ),
            actions = previewActions,
            onDeleteEntry = {},
        )
    }
}
