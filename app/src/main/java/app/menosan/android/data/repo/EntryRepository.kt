package app.menosan.android.data.repo

import app.menosan.android.core.model.Entry
import app.menosan.android.core.model.EntryDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

sealed class EntryChangeException(message: String) : Exception(message) {
    class WeekClosed : EntryChangeException("That week is closed.")
    class NotFound : EntryChangeException("Entry not found.")
    class Invalid(val field: String) : EntryChangeException("Invalid $field.")
}

interface EntryRepository {
    fun observeWeek(weekStart: LocalDate): Flow<List<Entry>>

    fun observeCurrentWeek(): Flow<List<Entry>>

    suspend fun get(id: String): Entry?

    suspend fun create(draft: EntryDraft): Entry

    suspend fun update(id: String, draft: EntryDraft): Entry

    suspend fun delete(id: String)

    fun observePendingCount(): Flow<Int>

    suspend fun unsyncedCount(): Int

    fun requestSync()

    suspend fun refreshCurrentWeek()
}
