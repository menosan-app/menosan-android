package app.menosan.android.data.remote.dto

import app.menosan.android.core.model.EntrySource
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.network.FallbackEnumSerializer
import app.menosan.android.core.network.IsoDate
import app.menosan.android.core.network.IsoInstant
import kotlinx.serialization.Serializable

@Serializable
data class EntryDto(
    val id: String,
    val name: String,
    val category: WasteCategory,
    val subcategory: String,
    val quantity: Int,
    val source: EntrySource,
    val createdAt: IsoInstant,
    val weekStart: IsoDate,
    val updatedAt: IsoInstant,
    val editable: Boolean,
)

@Serializable
data class EntryListDto(
    val weekStart: IsoDate,
    val weekEnd: IsoDate,
    val editable: Boolean,
    val entries: List<EntryDto>,
)

@Serializable
data class EntryPutRequest(
    val name: String,
    val subcategory: String,
    val quantity: Int,
    val source: EntrySource,
    val createdAt: IsoInstant,
)

@Serializable
data class SyncUpsertDto(
    val id: String,
    val name: String,
    val subcategory: String,
    val quantity: Int,
    val source: EntrySource,
    val createdAt: IsoInstant,
)

@Serializable
data class SyncRequest(
    val upserts: List<SyncUpsertDto> = emptyList(),
    val deletes: List<String> = emptyList(),
)

@Serializable(with = SyncOpSerializer::class)
enum class SyncOp { UPSERT, DELETE, UNKNOWN }

object SyncOpSerializer : FallbackEnumSerializer<SyncOp>("SyncOp", SyncOp.entries.toTypedArray(), SyncOp.UNKNOWN)

@Serializable(with = SyncStatusSerializer::class)
enum class SyncStatus { OK, WEEK_CLOSED, INVALID, INVALID_TIMESTAMP, CONFLICT, ERROR, UNKNOWN }

object SyncStatusSerializer :
    FallbackEnumSerializer<SyncStatus>("SyncStatus", SyncStatus.entries.toTypedArray(), SyncStatus.UNKNOWN)

@Serializable
data class SyncResultDto(
    val id: String? = null,
    val op: SyncOp,
    val status: SyncStatus,
    val entry: EntryDto? = null,
    val message: String? = null,
)

@Serializable
data class SyncResponse(val results: List<SyncResultDto>)
