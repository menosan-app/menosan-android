package app.menosan.android.sync

import app.menosan.android.core.auth.AuthService
import app.menosan.android.core.network.ConnectivityObserver
import app.menosan.android.data.repo.EntryRepository
import app.menosan.android.data.repo.ReportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncTriggers @Inject constructor(
    private val auth: AuthService,
    private val connectivity: ConnectivityObserver,
    private val repository: EntryRepository,
    private val engine: EntrySyncEngine,
    private val reports: ReportRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch { runCatching { engine.pruneOldEntries() } }
        scope.launch {
            val signedIn = auth.authState.map { it?.uid }.distinctUntilChanged()
            var lastUid: String? = null
            combine(signedIn, connectivity.online) { uid, online -> uid to online }.collectLatest { (uid, online) ->
                if (uid == null) {
                    lastUid = null
                    return@collectLatest
                }
                if (uid != lastUid) {
                    lastUid = uid
                    repository.requestSync()
                }
                if (online) repository.refreshCurrentWeek()
                runCatching { reports.refreshReports() }
            }
        }
    }
}
