package app.menosan.android.feature.entries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.menosan.android.R
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.network.LocalOnline
import app.menosan.android.core.ui.components.BannerTone
import app.menosan.android.core.ui.components.CardElevation
import app.menosan.android.core.ui.components.DISABLED_ALPHA
import app.menosan.android.core.ui.components.GroupedList
import app.menosan.android.core.ui.components.MessageBanner
import app.menosan.android.core.ui.components.MetaText
import app.menosan.android.core.ui.components.QuietCard
import app.menosan.android.core.ui.components.SectionGap
import app.menosan.android.core.ui.components.SectionHeader
import app.menosan.android.core.ui.components.piecesAndGramsText
import app.menosan.android.core.ui.components.screenInsetsPadding
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.feature.logging.icon
import app.menosan.android.feature.logging.labelRes

private const val PREVIEW_ENTRIES = 5

@Composable
fun AuditRoute(
    onLogManually: () -> Unit,
    onScanWithPhoto: () -> Unit,
    onViewAllEntries: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditEntry: (String) -> Unit,
    viewModel: AuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.messageEvents.collect { snackbar.showSnackbar(resources.getString(it)) }
    }
    Box(Modifier.fillMaxSize()) {
        AuditScreen(
            state = state,
            onLogManually = onLogManually,
            onScanWithPhoto = onScanWithPhoto,
            onViewAllEntries = onViewAllEntries,
            onOpenEntry = onOpenEntry,
            onEditEntry = onEditEntry,
            onDeleteEntry = viewModel::delete,
            onDismissReverted = viewModel::dismissRevertedNotice,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
fun AuditScreen(
    state: AuditUiState,
    onLogManually: () -> Unit,
    onScanWithPhoto: () -> Unit,
    onViewAllEntries: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditEntry: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onDismissReverted: () -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val summary = state.summary

    LazyColumn(
        modifier = Modifier.fillMaxSize().screenInsetsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = SectionGap)) {
                Text(
                    stringResource(R.string.audit_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.audit_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SectionHeader(
                stringResource(R.string.audit_this_week),
                modifier = Modifier.padding(bottom = 8.dp),
                trailing = { MetaText(EntryFormats.weekRange(state.weekStart)) },
            )
        }
        item { WeekCard(summary) }
        item { QuickActions(onLogManually, onScanWithPhoto, Modifier.padding(top = 12.dp, bottom = SectionGap)) }

        if (state.revertedChanges > 0) {
            item {
                Column(Modifier.padding(bottom = 12.dp)) {
                    MessageBanner(
                        title = stringResource(R.string.audit_reverted_title),
                        text = pluralStringResource(R.plurals.audit_reverted_body, state.revertedChanges, state.revertedChanges),
                    )
                    TextButton(onClick = onDismissReverted, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.audit_dismiss))
                    }
                }
            }
        }
        if (state.pendingCount > 0) {
            item {
                MessageBanner(
                    title = stringResource(R.string.audit_pending_title),
                    text = pluralStringResource(R.plurals.audit_pending_body, state.pendingCount, state.pendingCount),
                    icon = Icons.Outlined.HourglassEmpty,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
        if (state.failedCount > 0) {
            item {
                MessageBanner(
                    title = stringResource(R.string.audit_failed_title),
                    text = stringResource(R.string.audit_failed_body),
                    tone = BannerTone.Error,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }

        item {
            SectionHeader(
                stringResource(R.string.audit_entries_title),
                modifier = Modifier.padding(bottom = 8.dp),
                trailing = if (state.entries.isNotEmpty() || state.earlierWeeks.isNotEmpty()) {
                    { TextButton(onClick = onViewAllEntries) { Text(stringResource(R.string.dashboard_view_all)) } }
                } else {
                    null
                },
            )
        }
        item {
            if (state.entries.isEmpty()) {
                if (!state.loading) EmptyWeek()
            } else {
                GroupedList(state.entries.take(PREVIEW_ENTRIES), dividerInset = 64.dp) { entry ->
                    EntryRow(
                        entry = entry,
                        subcategoryLabel = state.labelOf(entry),
                        onOpen = { onOpenEntry(entry.id) },
                        onEdit = { onEditEntry(entry.id) },
                        onDelete = { pendingDelete = entry.id },
                    )
                }
            }
        }
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
private fun WeekCard(summary: WeekSummary) {
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
        Row {
            WasteCategory.entries.forEach { category ->
                CategoryCount(category, summary.byCategory[category] ?: CategoryTotal(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryCount(category: WasteCategory, total: CategoryTotal, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(category.icon(), contentDescription = null, tint = category.color(), modifier = Modifier.size(22.dp))
        Text(total.entries.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(
            stringResource(category.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuickActions(onLogManually: () -> Unit, onScanWithPhoto: () -> Unit, modifier: Modifier = Modifier) {
    val online = LocalOnline.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        QuickAction(Icons.Outlined.EditNote, R.string.action_log_manually, R.string.action_log_manually_body, onLogManually, Modifier.weight(1f))
        QuickAction(
            if (online) Icons.Outlined.CameraAlt else Icons.Outlined.CloudOff,
            R.string.action_take_photo,
            if (online) R.string.action_take_photo_body else R.string.photo_needs_internet_short,
            onScanWithPhoto,
            Modifier.weight(1f),
            enabled = online,
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    title: Int,
    body: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MenosanTheme.colors.card,
        shadowElevation = CardElevation,
        modifier = modifier,
    ) {
        Row(
            Modifier
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .heightIn(min = 64.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                MetaText(stringResource(body), maxLines = 1)
            }
        }
    }
}

@Composable
internal fun EmptyWeek() {
    Surface(
        color = MenosanTheme.colors.mist,
        shape = MaterialTheme.shapes.large,
        shadowElevation = CardElevation,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(stringResource(R.string.audit_empty_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.audit_empty_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
