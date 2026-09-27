package app.menosan.android.core.analytics

import app.menosan.android.core.model.Taxonomy

fun Taxonomy.analyticsTaxonomy(): Map<String, SubcategoryInfo> =
    subcategories.associate { it.code to SubcategoryInfo(it.code, it.category.name, it.avoidable, it.unit) }
