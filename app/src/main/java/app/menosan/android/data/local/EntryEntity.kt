package app.menosan.android.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.menosan.android.core.model.EntrySource
import app.menosan.android.core.model.WasteCategory
import java.time.Instant
import java.time.LocalDate

enum class SyncState { PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE, SYNCED }

@Entity(
    tableName = "entries",
    indices = [Index("week_start"), Index("sync_state")],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: WasteCategory,
    val subcategory: String,
    val quantity: Int,
    val source: EntrySource,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "week_start") val weekStart: LocalDate,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    @ColumnInfo(name = "sync_state") val syncState: SyncState,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
)
