package app.menosan.android.data.remote.dto

import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.model.WasteCategory
import kotlinx.serialization.Serializable

@Serializable
data class PhotoSuggestionDto(
    val name: String,
    val category: WasteCategory,
    val subcategory: String,
    val quantity: Int,
    val unit: QuantityUnit = QuantityUnit.PIECES,
    val confidence: Double,
)

@Serializable
data class PhotoAnalysisDto(val suggestion: PhotoSuggestionDto, val warning: String)
