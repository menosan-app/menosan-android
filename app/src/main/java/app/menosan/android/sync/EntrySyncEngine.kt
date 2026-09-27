package app.menosan.android.sync

import app.menosan.android.core.network.ApiError
import app.menosan.android.core.network.ApiErrorCode
import app.menosan.android.core.network.ApiResult
import app.menosan.android.core.time.WeekCalc
import app.menosan.android.data.local.EntryDao
import app.menosan.android.data.local.EntryEntity
import app.menosan.android.data.local.SyncState
import app.menosan.android.data.remote.dto.EntryDto
import app.menosan.android.data.remote.dto.SyncOp
import app.menosan.android.data.remote.dto.SyncRequest
import app.menosan.android.data.remote.dto.SyncResultDto
import app.menosan.android.data.remote.dto.SyncStatus
import app.menosan.android.data.remote.dto.SyncUpsertDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class PushStats(
    val sent: Int = 0,
    val synced: Int = 0,
    val reverted: Int = 0,
    val failed: Int = 0,
    val conflicts: Int = 0,
    val retryable: Int = 0,
) {
    operator fun plus(other: PushStats) = PushStats(
        sent + other.sent, synced + other.synced, reverted + other.reverted,
        failed + other.failed, conflicts + other.conflicts, retryable + other.retryable,
    )
}

sealed interface PushOutcome {
    val stats: PushStats

    data class Done(override val stats: PushStats) : PushOutcome

    data class RetryLater(override val stats: PushStats) : PushOutcome

    data class Stopped(override val stats: PushStats, val reason: String) : PushOutcome
}

@Singleton
class EntrySyncEngine @Inject constructor(
    private val dao: EntryDao,
    private val remote: EntryRemote,
    private val tx: TransactionRunner,
    private val notices: SyncNotices,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    suspend fun pushOutbox(): PushOutcome = mutex.withLock {
        var stats = PushStats()
        val attempted = HashSet<EntryEntity>()
        repeat(MAX_PASSES) {
            val outbox = dao.getOutbox().filterNot { it in attempted }
            if (outbox.isEmpty()) return@withLock finish(stats)
            for (batch in outbox.chunked(MAX_BATCH)) {
                attempted += batch
                when (val result = sendBatch(batch)) {
                    is BatchResult.Applied -> stats += result.stats
                    is BatchResult.Failed -> return@withLock result.outcome(stats)
                }
            }
        }
        finish(stats)
    }

    suspend fun pullCurrentWeek(): Boolean = mutex.withLock {
        val list = when (val result = remote.currentWeekEntries()) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return@withLock false
        }
        tx.run {
            val serverIds = HashSet<String>(list.entries.size)
            for (dto in list.entries) {
                serverIds += dto.id
                val local = dao.getById(dto.id)
                if (local == null || local.syncState == SyncState.SYNCED) dao.upsert(dto.toEntity())
            }
            dao.getWeekIncludingDeleted(list.weekStart)
                .filter { it.syncState == SyncState.SYNCED && it.id !in serverIds }
                .forEach { dao.deleteById(it.id) }
        }
        true
    }

    suspend fun pruneOldEntries(): Int = dao.pruneSyncedBefore(oldestKeptWeekStart())

    fun oldestKeptWeekStart(): LocalDate = WeekCalc.currentWeekStart(clock).minusWeeks(KEPT_PREVIOUS_WEEKS)

    private fun finish(stats: PushStats): PushOutcome =
        if (stats.retryable > 0) PushOutcome.RetryLater(stats) else PushOutcome.Done(stats)

    private sealed interface BatchResult {
        data class Applied(val stats: PushStats) : BatchResult
        data class Failed(val outcome: (PushStats) -> PushOutcome) : BatchResult
    }

    private suspend fun sendBatch(batch: List<EntryEntity>): BatchResult {
        val upserts = batch.filter { it.syncState != SyncState.PENDING_DELETE }
        val deletes = batch.filter { it.syncState == SyncState.PENDING_DELETE }
        val request = SyncRequest(upserts = upserts.map { it.toUpsertDto() }, deletes = deletes.map { it.id })
        val response = when (val result = remote.sync(request)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return BatchResult.Failed(failureOutcome(result.error))
        }
        val ordered = upserts + deletes
        val stats = tx.run {
            var stats = PushStats(sent = ordered.size)
            ordered.forEachIndexed { index, sent ->
                val op = if (sent.syncState == SyncState.PENDING_DELETE) SyncOp.DELETE else SyncOp.UPSERT
                val answer = response.results.getOrNull(index)?.takeIf { it.id == sent.id && it.op == op }
                    ?: response.results.firstOrNull { it.id == sent.id && it.op == op }
                stats += apply(sent, answer)
            }
            stats
        }
        notices.addRevertedChanges(stats.reverted)
        return BatchResult.Applied(stats)
    }

    private suspend fun apply(sent: EntryEntity, answer: SyncResultDto?): PushStats {
        val current = dao.getById(sent.id)
            ?: return if (answer == null || answer.status.isRetryable()) PushStats(retryable = 1) else PushStats()
        val unchanged = current == sent
        val isDelete = sent.syncState == SyncState.PENDING_DELETE
        return when (answer?.status) {
            SyncStatus.OK -> {
                when {
                    isDelete -> if (current.syncState == SyncState.PENDING_DELETE) dao.deleteById(sent.id)
                    unchanged -> dao.upsert(answer.entry?.toEntity() ?: sent.copy(syncState = SyncState.SYNCED, lastError = null))
                    current.syncState == SyncState.PENDING_CREATE -> dao.upsert(current.copy(syncState = SyncState.PENDING_UPDATE))
                }
                PushStats(synced = 1)
            }

            SyncStatus.WEEK_CLOSED -> {
                val server = answer.entry?.toEntity()
                when {
                    server != null -> dao.upsert(server)
                    isDelete -> dao.upsert(current.copy(syncState = SyncState.SYNCED, lastError = null))
                    unchanged -> dao.upsert(current.copy(lastError = answer.message ?: DEFAULT_ERROR))
                }
                if (server != null || isDelete) PushStats(reverted = 1) else PushStats(failed = 1)
            }

            SyncStatus.INVALID, SyncStatus.INVALID_TIMESTAMP, SyncStatus.CONFLICT -> {
                if (unchanged) dao.upsert(current.copy(lastError = answer.message ?: DEFAULT_ERROR))
                PushStats(failed = 1, conflicts = if (answer.status == SyncStatus.CONFLICT) 1 else 0)
            }

            else -> PushStats(retryable = 1)
        }
    }

    private fun failureOutcome(error: ApiError): (PushStats) -> PushOutcome = { stats ->
        when (error) {
            is ApiError.Network, is ApiError.Unexpected -> PushOutcome.RetryLater(stats)
            is ApiError.Http -> when {
                error.code == ApiErrorCode.UNAUTHENTICATED -> PushOutcome.Stopped(stats, "signed out")
                error.code == ApiErrorCode.ACCOUNT_NOT_FOUND -> PushOutcome.RetryLater(stats)
                error.status >= 500 || error.status == 408 || error.status == 429 -> PushOutcome.RetryLater(stats)
                else -> PushOutcome.Stopped(stats, "http ${error.status}")
            }
        }
    }

    private fun SyncStatus.isRetryable() = this == SyncStatus.ERROR || this == SyncStatus.UNKNOWN

    companion object {
        const val MAX_BATCH = 500

        const val MAX_PASSES = 3

        const val KEPT_PREVIOUS_WEEKS = 2L

        const val DEFAULT_ERROR = "The server couldn't save this entry."
    }
}

internal fun EntryEntity.toUpsertDto() = SyncUpsertDto(
    id = id,
    name = name,
    subcategory = subcategory,
    quantity = quantity,
    source = source,
    createdAt = createdAt,
)

internal fun EntryDto.toEntity() = EntryEntity(
    id = id,
    name = name,
    category = category,
    subcategory = subcategory,
    quantity = quantity,
    source = source,
    createdAt = createdAt,
    weekStart = weekStart,
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED,
    lastError = null,
)
