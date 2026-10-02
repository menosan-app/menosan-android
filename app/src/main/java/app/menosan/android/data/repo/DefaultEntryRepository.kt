package app.menosan.android.data.repo

import app.menosan.android.core.model.Entry
import app.menosan.android.core.model.EntryDraft
import app.menosan.android.core.model.EntryRules
import app.menosan.android.core.model.EntrySyncStatus
import app.menosan.android.core.model.Taxonomy
import app.menosan.android.core.time.WeekCalc
import app.menosan.android.data.local.EntryDao
import app.menosan.android.data.local.EntryEntity
import app.menosan.android.data.local.SyncState
import app.menosan.android.sync.EntrySyncEngine
import app.menosan.android.sync.SyncRequester
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultEntryRepository internal constructor(
    private val dao: EntryDao,
    private val taxonomy: suspend () -> Taxonomy,
    private val clock: Clock,
    private val syncRequester: SyncRequester,
    private val engine: EntrySyncEngine,
) : EntryRepository {
    @Inject
    constructor(
        dao: EntryDao,
        taxonomyRepository: TaxonomyRepository,
        clock: Clock,
        syncRequester: SyncRequester,
        engine: EntrySyncEngine,
    ) : this(dao, taxonomyRepository::taxonomy, clock, syncRequester, engine)

    override fun observeWeek(weekStart: LocalDate): Flow<List<Entry>> =
        dao.observeWeek(weekStart).map { rows ->
            val tax = taxonomy()
            rows.map { it.toEntry(tax) }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentWeek(): Flow<List<Entry>> = currentWeekStartFlow(clock).flatMapLatest { observeWeek(it) }

    override suspend fun get(id: String): Entry? =
        dao.getById(id)?.takeIf { it.syncState != SyncState.PENDING_DELETE }?.toEntry(taxonomy())

    override suspend fun create(draft: EntryDraft): Entry {
        val clean = validate(draft)
        val now = clock.instant().truncatedTo(ChronoUnit.MILLIS)
        val entity = EntryEntity(
            id = UUID.randomUUID().toString(),
            name = clean.name,
            category = categoryOf(clean.subcategory),
            subcategory = clean.subcategory,
            quantity = clean.quantity,
            source = clean.source,
            createdAt = now,
            weekStart = WeekCalc.weekStart(now),
            updatedAt = now,
            syncState = SyncState.PENDING_CREATE,
        )
        dao.upsert(entity)
        requestSync()
        return entity.toEntry(taxonomy())
    }

    override suspend fun update(id: String, draft: EntryDraft): Entry {
        val existing = dao.getById(id)?.takeIf { it.syncState != SyncState.PENDING_DELETE }
            ?: throw EntryChangeException.NotFound()
        if (!WeekCalc.isCurrentWeek(existing.weekStart, clock)) throw EntryChangeException.WeekClosed()
        val clean = validate(draft)
        val updated = existing.copy(
            name = clean.name,
            category = categoryOf(clean.subcategory),
            subcategory = clean.subcategory,
            quantity = clean.quantity,
            updatedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS),
            syncState = if (existing.syncState == SyncState.PENDING_CREATE) SyncState.PENDING_CREATE else SyncState.PENDING_UPDATE,
            lastError = null,
        )
        dao.upsert(updated)
        requestSync()
        return updated.toEntry(taxonomy())
    }

    override suspend fun delete(id: String) {
        val existing = dao.getById(id)?.takeIf { it.syncState != SyncState.PENDING_DELETE }
            ?: throw EntryChangeException.NotFound()
        if (!WeekCalc.isCurrentWeek(existing.weekStart, clock)) throw EntryChangeException.WeekClosed()
        dao.upsert(
            existing.copy(
                syncState = SyncState.PENDING_DELETE,
                updatedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS),
                lastError = null,
            ),
        )
        requestSync()
    }

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun unsyncedCount(): Int = dao.countUnsynced()

    override fun requestSync() = syncRequester.requestSync()

    override suspend fun refreshCurrentWeek() {
        engine.pullCurrentWeek()
    }

    private suspend fun validate(draft: EntryDraft): EntryDraft {
        val name = draft.name.trim()
        if (name.isEmpty() || name.length > EntryRules.NAME_MAX) throw EntryChangeException.Invalid("name")
        val sub = taxonomy().subcategory(draft.subcategory) ?: throw EntryChangeException.Invalid("subcategory")
        if (draft.quantity !in EntryRules.QUANTITY_MIN..EntryRules.maxQuantity(sub.unit)) throw EntryChangeException.Invalid("quantity")
        return draft.copy(name = name)
    }

    private suspend fun categoryOf(subcategory: String) =
        taxonomy().subcategory(subcategory)?.category ?: throw EntryChangeException.Invalid("subcategory")

    private fun EntryEntity.toEntry(taxonomy: Taxonomy) = Entry(
        id = id,
        name = name,
        category = category,
        subcategory = subcategory,
        quantity = quantity,
        source = source,
        createdAt = createdAt,
        weekStart = weekStart,
        syncStatus = when {
            syncState == SyncState.SYNCED -> EntrySyncStatus.SYNCED
            lastError != null -> EntrySyncStatus.FAILED
            else -> EntrySyncStatus.PENDING
        },
        lastError = lastError,
        editable = WeekCalc.isCurrentWeek(weekStart, clock),
        unit = taxonomy.unitOf(subcategory),
    )
}
