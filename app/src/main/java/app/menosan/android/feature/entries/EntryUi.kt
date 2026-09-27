package app.menosan.android.feature.entries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.menosan.android.R
import app.menosan.android.core.ui.components.quantityText
import app.menosan.android.core.model.Entry
import app.menosan.android.core.model.EntrySyncStatus
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.ui.components.Pill
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.feature.logging.icon

@Composable
fun WasteCategory.color(): Color = when (this) {
    WasteCategory.BIODEGRADABLE -> MenosanTheme.colors.biodegradable
    WasteCategory.RECYCLABLE -> MenosanTheme.colors.recyclable
    WasteCategory.RESIDUAL -> MenosanTheme.colors.residual
    WasteCategory.SPECIAL -> MenosanTheme.colors.special
}

@Composable
fun CategoryBadge(category: WasteCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(category.color().copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon(), contentDescription = null, tint = category.color(), modifier = Modifier.size(22.dp))
    }
}

@Composable
fun SyncChip(status: EntrySyncStatus, modifier: Modifier = Modifier) {
    when (status) {
        EntrySyncStatus.SYNCED -> Pill(
            stringResource(R.string.sync_synced), MenosanTheme.colors.calm, MenosanTheme.colors.onCalm, modifier, Icons.Outlined.CheckCircle,
        )
        EntrySyncStatus.PENDING -> Pill(
            stringResource(R.string.sync_pending), MenosanTheme.colors.pending, MenosanTheme.colors.onPending, modifier, Icons.Outlined.HourglassEmpty,
        )
        EntrySyncStatus.FAILED -> Pill(
            stringResource(R.string.sync_failed),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            modifier,
            Icons.Outlined.ErrorOutline,
        )
    }
}

@Composable
fun EntryRow(
    entry: Entry,
    subcategoryLabel: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen)
            .heightIn(min = 64.dp)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryBadge(entry.category)
        Column(Modifier.weight(1f)) {
            Text(
                entry.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.entry_row_meta, subcategoryLabel, EntryFormats.shortDateTime(entry.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                quantityText(entry.quantity, entry.unit),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            if (entry.syncStatus == EntrySyncStatus.SYNCED) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = stringResource(R.string.sync_synced),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            } else {
                SyncChip(entry.syncStatus)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.entry_more_options, entry.name))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.entry_menu_details)) },
                    onClick = {
                        menuOpen = false
                        onOpen()
                    },
                )
                if (entry.editable) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.entry_menu_edit)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.entry_menu_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteEntryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MenosanTheme.colors.card,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp)) },
        title = {
            Text(
                stringResource(R.string.delete_title),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = { Text(stringResource(R.string.delete_body), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, shape = MaterialTheme.shapes.small, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.delete_cancel), color = MaterialTheme.colorScheme.onSurface)
                }
                Button(
                    onClick = onConfirm,
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            }
        },
    )
}
