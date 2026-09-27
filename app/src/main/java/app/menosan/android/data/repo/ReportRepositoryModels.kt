package app.menosan.android.data.repo

import app.menosan.android.core.analytics.ALGORITHM_VERSION
import app.menosan.android.core.analytics.CategoryStats
import app.menosan.android.core.analytics.Comparison
import app.menosan.android.core.analytics.ComparisonRow
import app.menosan.android.core.analytics.Hotspot
import app.menosan.android.core.analytics.QuantityUnit
import app.menosan.android.core.analytics.SubcategoryStats
import app.menosan.android.core.analytics.Totals
import app.menosan.android.core.analytics.WeeklyStats
import app.menosan.android.core.model.WasteCategory
import app.menosan.android.core.network.IsoDate
import app.menosan.android.core.network.IsoInstant
import app.menosan.android.core.network.MenosanJson
import app.menosan.android.data.local.ReportCacheEntity
import app.menosan.android.data.remote.dto.CategoryComparisonDto
import app.menosan.android.data.remote.dto.CategoryStatsDto
import app.menosan.android.data.remote.dto.ComparisonDto
import app.menosan.android.data.remote.dto.ComparisonRowDto
import app.menosan.android.data.remote.dto.HotspotCriterion
import app.menosan.android.data.remote.dto.HotspotDto
import app.menosan.android.data.remote.dto.ReportDto
import app.menosan.android.data.remote.dto.ReportSummaryDto
import app.menosan.android.data.remote.dto.SubcategoryComparisonDto
import app.menosan.android.data.remote.dto.SubcategoryStatsDto
import app.menosan.android.data.remote.dto.TotalsDto
import app.menosan.android.data.remote.dto.Trend
import app.menosan.android.data.remote.dto.WeeklyStatsDto
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import app.menosan.android.core.analytics.HotspotCriterion as AnalyticsCriterion
import app.menosan.android.core.analytics.Trend as AnalyticsTrend

@Serializable
data class LastWeekRecap(
    val weekStart: IsoDate,
    val weekEnd: IsoDate,
    val hotspots: List<RecapHotspot>,
    val adopted: List<RecapAdoption>,
)

@Serializable
data class RecapHotspot(val subcategory: String, val frequency: Int, val quantity: Int, val unit: QuantityUnit = QuantityUnit.PIECES)

@Serializable
data class RecapAdoption(val interventionId: String, val title: String, val targetSubcategory: String)

@Serializable
enum class ComparisonSource { SERVER_REPORT, LOCAL_ENTRIES, NONE }

@Serializable
data class ProvisionalReportPayload(
    val report: ReportDto,
    val recap: LastWeekRecap? = null,
    val comparisonSource: ComparisonSource = ComparisonSource.NONE,
    val missingComparisons: Boolean = false,
    val algorithmVersion: Int = ALGORITHM_VERSION,
    val generatedAt: IsoInstant,
)

internal fun ReportDto.adoptedCount(): Int = hotspots.sumOf { h -> h.recommendations.count { it.adopted } }

internal fun ReportDto.toCacheEntity(fetchedAt: Instant): ReportCacheEntity = ReportCacheEntity(
    weekStart = weekStart,
    weekEnd = weekEnd,
    analyzedPieces = stats.analyzedTotals.pieces,
    analyzedGrams = stats.analyzedTotals.grams,
    analyzedEntries = stats.analyzedTotals.frequency,
    hotspotCount = hotspots.size,
    adoptedCount = adoptedCount(),
    isLatest = isLatest,
    isProvisional = false,
    revision = revision,
    algorithmVersion = null,
    payloadJson = MenosanJson.encodeToString(ReportDto.serializer(), this),
    fetchedAt = fetchedAt,
)

internal fun ReportSummaryDto.toCacheEntity(fetchedAt: Instant): ReportCacheEntity = ReportCacheEntity(
    weekStart = weekStart,
    weekEnd = weekEnd,
    analyzedPieces = analyzedPieces,
    analyzedGrams = analyzedGrams,
    analyzedEntries = analyzedEntries,
    hotspotCount = hotspotCount,
    adoptedCount = adoptedCount,
    isLatest = isLatest,
    isProvisional = false,
    revision = null,
    algorithmVersion = null,
    payloadJson = null,
    fetchedAt = fetchedAt,
)

internal fun ProvisionalReportPayload.toCacheEntity(): ReportCacheEntity = ReportCacheEntity(
    weekStart = report.weekStart,
    weekEnd = report.weekEnd,
    analyzedPieces = report.stats.analyzedTotals.pieces,
    analyzedGrams = report.stats.analyzedTotals.grams,
    analyzedEntries = report.stats.analyzedTotals.frequency,
    hotspotCount = report.hotspots.size,
    adoptedCount = 0,
    isLatest = report.isLatest,
    isProvisional = true,
    revision = null,
    algorithmVersion = algorithmVersion,
    payloadJson = MenosanJson.encodeToString(ProvisionalReportPayload.serializer(), this),
    fetchedAt = generatedAt,
)

internal fun ReportCacheEntity.serverReport(): ReportDto? {
    if (isProvisional) return null
    val json = payloadJson ?: return null
    return runCatching { MenosanJson.decodeFromString(ReportDto.serializer(), json) }.getOrNull()
}

internal fun ReportCacheEntity.provisionalPayload(): ProvisionalReportPayload? {
    if (!isProvisional) return null
    val json = payloadJson ?: return null
    return runCatching { MenosanJson.decodeFromString(ProvisionalReportPayload.serializer(), json) }.getOrNull()
}

internal fun ReportCacheEntity.differsFrom(summary: ReportSummaryDto): Boolean =
    analyzedPieces != summary.analyzedPieces || analyzedGrams != summary.analyzedGrams ||
        analyzedEntries != summary.analyzedEntries || hotspotCount != summary.hotspotCount ||
        adoptedCount != summary.adoptedCount || isLatest != summary.isLatest

internal fun WeeklyStatsDto.toAnalytics(): WeeklyStats = WeeklyStats(
    analyzedTotals = analyzedTotals.toAnalytics(),
    categories = categories.map { CategoryStats(it.category.name, it.frequency, it.pieces, it.grams, it.sharePct) },
    subcategories = subcategories.map { SubcategoryStats(it.code, it.category.name, it.unit, it.frequency, it.quantity) },
    special = special.toAnalytics(),
)

internal fun WeeklyStats.toDto(): WeeklyStatsDto = WeeklyStatsDto(
    analyzedTotals = analyzedTotals.toDto(),
    categories = categories.map {
        CategoryStatsDto(WasteCategory.valueOf(it.category), it.frequency, it.pieces, it.grams, it.sharePct)
    },
    subcategories = subcategories.map {
        SubcategoryStatsDto(it.code, WasteCategory.valueOf(it.category), it.unit, it.frequency, it.quantity)
    },
    special = special.toDto(),
)

private fun TotalsDto.toAnalytics() = Totals(frequency, pieces, grams)

private fun Totals.toDto() = TotalsDto(frequency, pieces, grams)

internal fun Hotspot.toDto(): HotspotDto = HotspotDto(
    rank = rank,
    subcategory = subcategory,
    criteria = criteria.map { it.toDto() },
    frequency = frequency,
    quantity = quantity,
    unit = unit,
    score = score,
    recommendations = emptyList(),
)

internal fun Comparison.toDto(): ComparisonDto = ComparisonDto(
    previousWeekStart = LocalDate.parse(previousWeekStart),
    pieces = pieces.toDto(),
    grams = grams.toDto(),
    categories = categories.map {
        CategoryComparisonDto(
            WasteCategory.valueOf(it.category), it.unit, it.previous, it.current, it.delta, it.deltaPct, it.trend.toDto(),
        )
    },
    subcategories = subcategories.map {
        SubcategoryComparisonDto(
            it.code, WasteCategory.valueOf(it.category), it.unit, it.previous, it.current, it.delta, it.deltaPct, it.trend.toDto(),
        )
    },
)

private fun ComparisonRow.toDto() = ComparisonRowDto(previous, current, delta, deltaPct, trend.toDto())

internal fun AnalyticsTrend.toDto(): Trend = Trend.valueOf(name)

internal fun AnalyticsCriterion.toDto(): HotspotCriterion = HotspotCriterion.valueOf(name)
