package app.menosan.android.feature.logging

import androidx.annotation.StringRes
import app.menosan.android.R
import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.model.EntryDraft
import app.menosan.android.core.model.EntryRules
import app.menosan.android.core.model.EntrySource
import app.menosan.android.core.model.Taxonomy
import app.menosan.android.core.model.WasteCategory

enum class EntryField { CATEGORY, SUBCATEGORY, NAME, QUANTITY }

data class EntryFormState(
    val category: WasteCategory? = null,
    val subcategory: String? = null,
    val name: String = "",
    val quantity: String = "",
    val unit: QuantityUnit? = null,
) {
    fun withCategory(value: WasteCategory): EntryFormState =
        if (value == category) this else copy(category = value, subcategory = null)

    fun withSubcategory(code: String, taxonomy: Taxonomy): EntryFormState {
        val newUnit = taxonomy.unitOf(code)
        return copy(
            subcategory = code,
            category = taxonomy.subcategory(code)?.category ?: category,
            unit = newUnit,
            quantity = if (unit != null && unit != newUnit) "" else quantity,
        )
    }

    fun withName(value: String): EntryFormState = copy(name = value.take(EntryRules.NAME_MAX))

    fun withQuantity(value: String): EntryFormState = copy(quantity = value.filter(Char::isDigit).take(QUANTITY_DIGITS))

    fun errors(taxonomy: Taxonomy): Map<EntryField, Int> = buildMap {
        if (category == null) put(EntryField.CATEGORY, R.string.entry_error_category)
        val sub = subcategory?.let(taxonomy::subcategory)
        if (sub == null || (category != null && sub.category != category)) put(EntryField.SUBCATEGORY, R.string.entry_error_subcategory)
        if (name.isBlank()) put(EntryField.NAME, R.string.entry_error_name)
        val qty = quantity.toIntOrNull()
        val max = EntryRules.maxQuantity(taxonomy.unitOf(subcategory))
        if (qty == null || qty !in EntryRules.QUANTITY_MIN..max) {
            put(EntryField.QUANTITY, if (taxonomy.unitOf(subcategory) == QuantityUnit.GRAMS) R.string.entry_error_quantity_grams else R.string.entry_error_quantity)
        }
    }

    fun toDraft(taxonomy: Taxonomy, source: EntrySource): EntryDraft? {
        if (errors(taxonomy).isNotEmpty()) return null
        return EntryDraft(name = name.trim(), subcategory = subcategory!!, quantity = quantity.toInt(), source = source)
    }

    companion object {
        fun from(draft: EntryDraft, taxonomy: Taxonomy) = EntryFormState(
            category = taxonomy.subcategory(draft.subcategory)?.category,
            subcategory = draft.subcategory,
            name = draft.name,
            quantity = draft.quantity.toString(),
            unit = taxonomy.subcategory(draft.subcategory)?.unit,
        )

        private const val QUANTITY_DIGITS = 5
    }
}

@StringRes
fun WasteCategory.labelRes(): Int = when (this) {
    WasteCategory.BIODEGRADABLE -> R.string.category_biodegradable
    WasteCategory.RECYCLABLE -> R.string.category_recyclable
    WasteCategory.RESIDUAL -> R.string.category_residual
    WasteCategory.SPECIAL -> R.string.category_special
}
