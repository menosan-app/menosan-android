package app.menosan.android.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "reports_cache")
data class ReportCacheEntity(
    @PrimaryKey @ColumnInfo(name = "week_start") val weekStart: LocalDate,
    @ColumnInfo(name = "week_end") val weekEnd: LocalDate,
    @ColumnInfo(name = "analyzed_quantity") val analyzedPieces: Int,
    @ColumnInfo(name = "analyzed_grams", defaultValue = "0") val analyzedGrams: Int = 0,
    @ColumnInfo(name = "analyzed_entries", defaultValue = "0") val analyzedEntries: Int = 0,
    @ColumnInfo(name = "hotspot_count") val hotspotCount: Int,
    @ColumnInfo(name = "adopted_count") val adoptedCount: Int,
    @ColumnInfo(name = "is_latest") val isLatest: Boolean,
    @ColumnInfo(name = "is_provisional") val isProvisional: Boolean,
    val revision: Int?,
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: Int?,
    @ColumnInfo(name = "payload_json") val payloadJson: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Instant,
)
