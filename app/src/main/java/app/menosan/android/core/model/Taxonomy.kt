package app.menosan.android.core.model

import app.menosan.android.core.analytics.QuantityUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
enum class WasteCategory { BIODEGRADABLE, RECYCLABLE, RESIDUAL, SPECIAL }

@Serializable
enum class EntrySource { MANUAL, PHOTO }

@Serializable
data class TaxonomyCategory(val code: WasteCategory, val label: String, val analyzed: Boolean)

@Serializable
data class TaxonomySubcategory(
    val code: String,
    val category: WasteCategory,
    val label: String,
    val examples: List<String>,
    val avoidable: Boolean,
    val sortOrder: Int,
    val unit: QuantityUnit,
)

@Serializable
data class Taxonomy(
    val version: Int,
    val timezone: String? = null,
    val categories: List<TaxonomyCategory>,
    val subcategories: List<TaxonomySubcategory>,
) {
    @Transient
    private val byCode: Map<String, TaxonomySubcategory> = subcategories.associateBy { it.code }

    fun subcategory(code: String): TaxonomySubcategory? = byCode[code]

    fun unitOf(code: String?): QuantityUnit = code?.let { byCode[it]?.unit } ?: QuantityUnit.PIECES

    fun category(code: WasteCategory): TaxonomyCategory? = categories.firstOrNull { it.code == code }

    fun subcategoriesOf(category: WasteCategory): List<TaxonomySubcategory> =
        subcategories.filter { it.category == category }.sortedBy { it.sortOrder }
}
