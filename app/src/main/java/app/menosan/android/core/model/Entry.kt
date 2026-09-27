package app.menosan.android.core.model

import app.menosan.android.core.analytics.QuantityUnit
import java.time.Instant
import java.time.LocalDate

enum class EntrySyncStatus { SYNCED, PENDING, FAILED }

data class Entry(
    val id: String,
    val name: String,
    val category: WasteCategory,
    val subcategory: String,
    val quantity: Int,
    val source: EntrySource,
    val createdAt: Instant,
    val weekStart: LocalDate,
    val syncStatus: EntrySyncStatus,
    val lastError: String? = null,
    val editable: Boolean,
    val unit: QuantityUnit = QuantityUnit.PIECES,
)

data class EntryDraft(
    val name: String,
    val subcategory: String,
    val quantity: Int,
    val source: EntrySource,
)

object EntryRules {
    const val NAME_MAX = 60
    const val QUANTITY_MIN = 1
    const val QUANTITY_MAX_PIECES = 999
    const val QUANTITY_MAX_GRAMS = 10_000

    fun maxQuantity(unit: QuantityUnit): Int = if (unit == QuantityUnit.GRAMS) QUANTITY_MAX_GRAMS else QUANTITY_MAX_PIECES
}
