package app.menosan.android.data.repo

import app.menosan.android.core.network.ApiError
import app.menosan.android.data.remote.dto.RecommendationDto
import app.menosan.android.data.remote.dto.ReportDto
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

interface ReportRepository {
    fun observeReports(): Flow<List<ReportListItem>>

    fun observeReport(weekStart: LocalDate): Flow<ReportView?>

    fun observeLatestReport(): Flow<ReportView?>

    suspend fun refreshReports(): RefreshResult

    suspend fun refreshReport(weekStart: LocalDate): RefreshResult

    suspend fun setAdopted(weekStart: LocalDate, interventionId: String, adopted: Boolean): AdoptionResult

    suspend fun refreshAfterSync(): RefreshResult

    suspend fun generateOfflineReports(): List<LocalDate>
}

data class ReportListItem(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val analyzedEntries: Int,
    val analyzedPieces: Int,
    val analyzedGrams: Int,
    val hotspotCount: Int,
    val adoptedCount: Int,
    val isLatest: Boolean,
    val isProvisional: Boolean,
    val hasDetails: Boolean,
)

data class ReportView(
    val report: ReportDto,
    val isProvisional: Boolean,
    val canAdopt: Boolean,
    val recap: LastWeekRecap?,
    val missingComparisons: Boolean,
    val followupNotMeasured: Boolean,
    val savedAt: Instant,
) {
    val weekStart: LocalDate get() = report.weekStart
    val weekEnd: LocalDate get() = report.weekEnd

    val adopted: List<RecommendationDto> get() = report.hotspots.flatMap { it.recommendations }.filter { it.adopted }
}

sealed interface RefreshResult {
    data object Success : RefreshResult

    data object NotFound : RefreshResult

    data class Failure(val error: ApiError) : RefreshResult
}

sealed interface AdoptionResult {
    data object Success : AdoptionResult

    data object WindowClosed : AdoptionResult

    data object NotAvailable : AdoptionResult

    data class Failure(val error: ApiError) : AdoptionResult
}

val ApiError.isUnreachable: Boolean
    get() = when (this) {
        is ApiError.Network -> true
        is ApiError.Http -> status >= 500
        is ApiError.Unexpected -> false
    }
