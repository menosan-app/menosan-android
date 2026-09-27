package app.menosan.android.data.remote.dto

import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.network.FallbackEnumSerializer
import app.menosan.android.core.network.IsoDate
import kotlinx.serialization.Serializable

@Serializable
data class ReportSummaryDto(
    val weekStart: IsoDate,
    val weekEnd: IsoDate,
    val analyzedEntries: Int,
    val analyzedPieces: Int,
    val analyzedGrams: Int,
    val hotspotCount: Int,
    val adoptedCount: Int,
    val isLatest: Boolean,
)

@Serializable
data class ReportDto(
    val weekStart: IsoDate,
    val weekEnd: IsoDate,
    val revision: Int,
    val isLatest: Boolean,
    val stats: WeeklyStatsDto,
    val hotspots: List<HotspotDto>,
    val comparison: ComparisonDto? = null,
    val impacts: List<ImpactDto> = emptyList(),
)

@Serializable
data class TotalsDto(val frequency: Int, val pieces: Int, val grams: Int)

@Serializable
data class CategoryStatsDto(val category: WasteCategory, val frequency: Int, val pieces: Int, val grams: Int, val sharePct: Double)

@Serializable
data class SubcategoryStatsDto(val code: String, val category: WasteCategory, val unit: QuantityUnit, val frequency: Int, val quantity: Int)

@Serializable
data class WeeklyStatsDto(
    val analyzedTotals: TotalsDto,
    val categories: List<CategoryStatsDto>,
    val subcategories: List<SubcategoryStatsDto>,
    val special: TotalsDto,
)

@Serializable(with = HotspotCriterionSerializer::class)
enum class HotspotCriterion { MOST_FREQUENT, HIGHEST_QUANTITY, AVOIDABLE, UNKNOWN }

object HotspotCriterionSerializer :
    FallbackEnumSerializer<HotspotCriterion>("HotspotCriterion", HotspotCriterion.entries.toTypedArray(), HotspotCriterion.UNKNOWN)

@Serializable
data class HotspotDto(
    val rank: Int,
    val subcategory: String,
    val criteria: List<HotspotCriterion>,
    val frequency: Int,
    val quantity: Int,
    val unit: QuantityUnit,
    val score: Double,
    val recommendations: List<RecommendationDto> = emptyList(),
)

@Serializable(with = InterventionTypeSerializer::class)
enum class InterventionType { PREVENT, REDUCE, REUSE, UNKNOWN }

object InterventionTypeSerializer :
    FallbackEnumSerializer<InterventionType>("InterventionType", InterventionType.entries.toTypedArray(), InterventionType.UNKNOWN)

@Serializable(with = CostLevelSerializer::class)
enum class CostLevel { FREE, SAVES_MONEY, SMALL_ONE_TIME_COST, UNKNOWN }

object CostLevelSerializer : FallbackEnumSerializer<CostLevel>("CostLevel", CostLevel.entries.toTypedArray(), CostLevel.UNKNOWN)

@Serializable(with = EffortSerializer::class)
enum class Effort { LOW, MEDIUM, UNKNOWN }

object EffortSerializer : FallbackEnumSerializer<Effort>("Effort", Effort.entries.toTypedArray(), Effort.UNKNOWN)

@Serializable
data class RecommendationDto(
    val interventionId: String,
    val code: String,
    val type: InterventionType,
    val title: String,
    val description: String,
    val howTo: List<String> = emptyList(),
    val costLevel: CostLevel,
    val effort: Effort,
    val note: String? = null,
    val continued: Boolean = false,
    val adopted: Boolean = false,
)

@Serializable(with = TrendSerializer::class)
enum class Trend { DECREASED, SAME, INCREASED, UNKNOWN }

object TrendSerializer : FallbackEnumSerializer<Trend>("Trend", Trend.entries.toTypedArray(), Trend.UNKNOWN)

@Serializable
data class ComparisonRowDto(val previous: Int, val current: Int, val delta: Int, val deltaPct: Double? = null, val trend: Trend)

@Serializable
data class CategoryComparisonDto(
    val category: WasteCategory,
    val unit: QuantityUnit,
    val previous: Int,
    val current: Int,
    val delta: Int,
    val deltaPct: Double? = null,
    val trend: Trend,
)

@Serializable
data class SubcategoryComparisonDto(
    val code: String,
    val category: WasteCategory,
    val unit: QuantityUnit,
    val previous: Int,
    val current: Int,
    val delta: Int,
    val deltaPct: Double? = null,
    val trend: Trend,
)

@Serializable
data class ComparisonDto(
    val previousWeekStart: IsoDate,
    val pieces: ComparisonRowDto,
    val grams: ComparisonRowDto,
    val categories: List<CategoryComparisonDto>,
    val subcategories: List<SubcategoryComparisonDto>,
)

@Serializable
data class ImpactDto(
    val interventionId: String,
    val title: String,
    val targetSubcategory: String,
    val unit: QuantityUnit,
    val baselineWeekStart: IsoDate,
    val baselineQuantity: Int,
    val followupQuantity: Int,
    val result: Trend,
)

@Serializable
data class AdoptRequest(val interventionIds: List<String>)
