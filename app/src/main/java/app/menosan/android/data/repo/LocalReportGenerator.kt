package app.menosan.android.data.repo

import app.menosan.android.core.analytics.AdoptionInput
import app.menosan.android.core.analytics.EntryInput
import app.menosan.android.core.analytics.SubcategoryInfo
import app.menosan.android.core.analytics.WeeklyStats
import app.menosan.android.core.analytics.aggregate
import app.menosan.android.core.analytics.analyticsTaxonomy
import app.menosan.android.core.analytics.compare
import app.menosan.android.core.analytics.findHotspots
import app.menosan.android.core.analytics.measureImpact
import app.menosan.android.core.model.Taxonomy
import app.menosan.android.core.time.WeekCalc
import app.menosan.android.data.local.EntryDao
import app.menosan.android.data.local.EntryEntity
import app.menosan.android.data.local.ReportCacheDao
import app.menosan.android.data.local.ReportCacheEntity
import app.menosan.android.data.remote.dto.ImpactDto
import app.menosan.android.data.remote.dto.ReportDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

class LocalReportGenerator internal constructor(
    private val entries: ReportEntrySource,
    private val cacheDao: ReportCacheDao,
    private val taxonomy: suspend () -> Taxonomy,
    private val clock: Clock,
) {
    @Inject
    constructor(entries: ReportEntrySource, cacheDao: ReportCacheDao, taxonomyRepository: TaxonomyRepository, clock: Clock) :
        this(entries, cacheDao, { taxonomyRepository.taxonomy() }, clock)

    suspend fun generateMissing(): List<LocalDate> {
        val latest = WeekCalc.latestReportWeekStart(clock)
        return listOf(latest, latest.minusDays(7)).filter { generate(it) != null }
    }

    suspend fun generate(weekStart: LocalDate): ProvisionalReportPayload? {
        if (!WeekCalc.isClosed(weekStart, clock)) return null
        val cached = cacheDao.get(weekStart)
        if (cached != null && cached.serverReport() != null) return null
        val weekEntries = entries.entriesOf(weekStart)
        if (weekEntries.isEmpty()) return null

        val previousWeek = weekStart.minusDays(7)
        val previousRow = cacheDao.get(previousWeek)
        val payload = build(
            weekStart = weekStart,
            entries = weekEntries,
            previousEntries = entries.entriesOf(previousWeek),
            previousRow = previousRow,
            taxonomy = taxonomy(),
            now = clock.instant(),
            latestReportWeekStart = WeekCalc.latestReportWeekStart(clock),
        )
        cacheDao.upsert(payload.toCacheEntity())
        return payload
    }

    companion object {
        internal fun build(
            weekStart: LocalDate,
            entries: List<EntryEntity>,
            previousEntries: List<EntryEntity>,
            previousRow: ReportCacheEntity?,
            taxonomy: Taxonomy,
            now: Instant,
            latestReportWeekStart: LocalDate,
        ): ProvisionalReportPayload {
            val tax = taxonomy.analyticsTaxonomy()
            val stats = aggregate(entries.toInputs(tax), tax)
            val hotspots = findHotspots(stats, tax)

            val previousWeek = weekStart.minusDays(7)
            val previousReport = previousRow?.serverReport()
            val localPrevious = previousEntries.toInputs(tax).takeIf { it.isNotEmpty() }?.let { aggregate(it, tax) }
            val (previousStats: WeeklyStats?, source) = when {
                previousReport != null -> previousReport.stats.toAnalytics() to ComparisonSource.SERVER_REPORT
                localPrevious != null -> localPrevious to ComparisonSource.LOCAL_ENTRIES
                else -> null to ComparisonSource.NONE
            }
            val comparison = compare(stats, previousStats, previousWeek.toString(), tax)

            val adopted = previousReport?.hotspots.orEmpty().flatMap { hotspot ->
                hotspot.recommendations.filter { it.adopted }.map { hotspot to it }
            }.distinctBy { (_, rec) -> rec.interventionId }
            val titles = adopted.associate { (_, rec) -> rec.interventionId to rec.title }
            val impacts = measureImpact(
                adopted.map { (hotspot, rec) -> AdoptionInput(rec.interventionId, hotspot.subcategory, hotspot.quantity) },
                stats,
                tax,
            ).map {
                ImpactDto(
                    interventionId = it.interventionId,
                    title = titles.getValue(it.interventionId),
                    targetSubcategory = it.targetSubcategory,
                    unit = it.unit,
                    baselineWeekStart = previousWeek,
                    baselineQuantity = it.baselineQuantity,
                    followupQuantity = it.followupQuantity,
                    result = it.result.toDto(),
                )
            }

            val recap = previousReport?.let { prev ->
                LastWeekRecap(
                    weekStart = prev.weekStart,
                    weekEnd = prev.weekEnd,
                    hotspots = prev.hotspots.sortedBy { it.rank }.map { RecapHotspot(it.subcategory, it.frequency, it.quantity, it.unit) },
                    adopted = adopted.map { (hotspot, rec) -> RecapAdoption(rec.interventionId, rec.title, hotspot.subcategory) },
                )
            }

            val serverSummary = previousRow?.takeIf { !it.isProvisional && previousReport == null }
            val comparisonMissing = comparison == null && (serverSummary?.analyzedEntries ?: 0) > 0
            val impactMissing = (serverSummary?.adoptedCount ?: 0) > 0

            return ProvisionalReportPayload(
                report = ReportDto(
                    weekStart = weekStart,
                    weekEnd = WeekCalc.weekEnd(weekStart),
                    revision = 0,
                    isLatest = weekStart == latestReportWeekStart,
                    stats = stats.toDto(),
                    hotspots = hotspots.map { it.toDto() },
                    comparison = comparison?.toDto(),
                    impacts = impacts,
                ),
                recap = recap,
                comparisonSource = if (comparison == null) ComparisonSource.NONE else source,
                missingComparisons = comparisonMissing || impactMissing,
                generatedAt = now,
            )
        }

        private fun List<EntryEntity>.toInputs(tax: Map<String, SubcategoryInfo>): List<EntryInput> =
            filter { it.subcategory in tax }.map { EntryInput(it.subcategory, it.quantity) }
    }
}

interface ReportEntrySource {
    suspend fun entriesOf(weekStart: LocalDate): List<EntryEntity>

    suspend fun weeksWithPendingEntries(): Set<LocalDate>
}

class RoomReportEntrySource @Inject constructor(private val dao: EntryDao) : ReportEntrySource {
    override suspend fun entriesOf(weekStart: LocalDate): List<EntryEntity> = dao.getWeek(weekStart)

    override suspend fun weeksWithPendingEntries(): Set<LocalDate> = dao.getPending().map { it.weekStart }.toSet()
}
