package app.menosan.android.data.local

import android.content.Context
import androidx.work.WorkManager
import app.menosan.android.core.auth.SessionStore
import app.menosan.android.sync.SyncNotices
import app.menosan.android.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalDataCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MenosanDatabase,
    private val session: SessionStore,
    private val syncNotices: SyncNotices,
) {
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.UNIQUE_NAME)
        db.clearAllTables()
        syncNotices.clearRevertedChanges()
        session.clear()
    }
}
