package app.menosan.android.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    fun isOnline(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)?.hasWorkingInternet() == true

    /**
     * Online state of the default network. Each update comes from the callback's own arguments:
     * re-reading `activeNetwork` inside `onLost` can still return the network that was just lost,
     * which left the app showing "online" after going to airplane mode.
     */
    val online: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasWorkingInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(isOnline())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    /**
     * Tells Android that a request just failed on the current network, so it re-checks it. A network
     * whose internet dropped upstream (e.g. an emulator whose host went offline) keeps its "validated"
     * flag until Android checks again; after this, [online] turns false once the check fails.
     */
    fun reportUnreachable() {
        val network = manager.activeNetwork ?: return
        runCatching { manager.reportNetworkConnectivity(network, false) }
    }

    private fun NetworkCapabilities.hasWorkingInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
