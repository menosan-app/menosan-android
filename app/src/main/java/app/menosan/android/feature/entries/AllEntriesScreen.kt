package app.menosan.android.feature.entries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.menosan.android.R
import app.menosan.android.core.model.Entry
import app.menosan.android.core.ui.components.GroupedList
import app.menosan.android.core.ui.components.MetaText
import app.menosan.android.core.ui.components.ScreenHeader
import app.menosan.android.core.ui.components.SectionHeader
import app.menosan.android.core.ui.components.screenInsetsPadding
import kotlinx.serialization.Serializable

@Serializable
data object AllEntriesRoute

@Composable
fun AllEntriesRouteScreen(
    onBack: () -> Unit,
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
        AllEntriesScreen(
            state = state,
            onBack = onBack,
            onOpenEntry = onOpenEntry,
            onEditEntry = onEditEntry,
            onDeleteEntry = viewModel::delete,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
    }
}

@Composable
fun AllEntriesScreen(
    state: AuditUiState,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditEntry: (String) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var showEarlier by rememberSaveable { mutableStateOf(false) }
    val onDelete: (String) -> Unit = { pendingDelete = it }

    Column(Modifier.fillMaxSize().screenInsetsPadding()) {
        ScreenHeader(title = stringResource(R.string.entries_all_title), onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
        ) {
            item(key = "this-week") {
                SectionHeader(
                    EntryFormats.weekRange(state.weekStart),
                    modifier = Modifier.padding(bottom = 8.dp),
                    trailing = { MetaText(pluralStringResource(R.plurals.count_entries, state.entries.size, state.entries.size)) },
                )
            }
            if (state.entries.isEmpty()) {
                if (!state.loading) item(key = "empty") { EmptyWeek() }
            } else {
                entryList("entries-current", state, state.entries, onOpenEntry, onEditEntry, onDelete)
            }

            if (state.earlierWeeks.isNotEmpty()) {
                item(key = "earlier-toggle") {
                    TextButton(onClick = { showEarlier = !showEarlier }, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(if (showEarlier) R.string.audit_earlier_hide else R.string.audit_earlier_show))
                    }
                }
                if (showEarlier) {
                    item(key = "earlier-note") { MetaText(stringResource(R.string.audit_earlier_note), Modifier.padding(bottom = 4.dp)) }
                    state.earlierWeeks.forEach { week ->
                        item(key = "week-${week.weekStart}") {
                            SectionHeader(
                                EntryFormats.weekRange(week.weekStart),
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                                trailing = { MetaText(pluralStringResource(R.plurals.count_entries, week.entries.size, week.entries.size)) },
                            )
                        }
                        entryList("entries-${week.weekStart}", state, week.entries, onOpenEntry, onEditEntry, onDelete)
                    }
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

private fun LazyListScope.entryList(
    key: String,
    state: AuditUiState,
    entries: List<Entry>,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    item(key = key) {
        GroupedList(entries, dividerInset = 64.dp) { entry ->
            EntryRow(
                entry = entry,
                subcategoryLabel = state.labelOf(entry),
                onOpen = { onOpen(entry.id) },
                onEdit = { onEdit(entry.id) },
                onDelete = { onDelete(entry.id) },
            )
        }
    }
}
