package app.menosan.android.sync

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import app.menosan.android.core.network.ApiResult
import app.menosan.android.core.network.safeApiCall
import app.menosan.android.data.local.MenosanDatabase
import app.menosan.android.data.remote.MenosanApi
import app.menosan.android.data.remote.dto.EntryListDto
import app.menosan.android.data.remote.dto.SyncRequest
import app.menosan.android.data.remote.dto.SyncResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface EntryRemote {
    suspend fun sync(request: SyncRequest): ApiResult<SyncResponse>

    suspend fun currentWeekEntries(): ApiResult<EntryListDto>
}

class ApiEntryRemote @Inject constructor(private val api: MenosanApi) : EntryRemote {
    override suspend fun sync(request: SyncRequest): ApiResult<SyncResponse> = safeApiCall { api.syncEntries(request) }

    override suspend fun currentWeekEntries(): ApiResult<EntryListDto> = safeApiCall { api.entries() }
}

interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner @Inject constructor(private val db: MenosanDatabase) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction { block() }
}

fun interface SyncRequester {
    fun requestSync()
}

interface SyncNotices {
    val revertedChanges: StateFlow<Int>

    fun addRevertedChanges(count: Int)

    fun clearRevertedChanges()
}

@Singleton
class PrefsSyncNotices @Inject constructor(@ApplicationContext context: Context) : SyncNotices {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val reverted = MutableStateFlow(prefs.getInt(KEY_REVERTED, 0))

    override val revertedChanges: StateFlow<Int> = reverted.asStateFlow()

    @Synchronized
    override fun addRevertedChanges(count: Int) {
        if (count <= 0) return
        val total = reverted.value + count
        prefs.edit { putInt(KEY_REVERTED, total) }
        reverted.value = total
    }

    @Synchronized
    override fun clearRevertedChanges() {
        prefs.edit { remove(KEY_REVERTED) }
        reverted.value = 0
    }

    private companion object {
        const val PREFS = "menosan_sync"
        const val KEY_REVERTED = "reverted_changes"
    }
}

fun interface AfterSyncAction {
    suspend fun afterSync(sentItems: Int)
}
