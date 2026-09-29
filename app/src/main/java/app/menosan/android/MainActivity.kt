package app.menosan.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.menosan.android.core.auth.AuthService
import app.menosan.android.core.network.ConnectivityObserver
import app.menosan.android.core.network.LocalOnline
import app.menosan.android.core.network.connectionNotices
import app.menosan.android.core.settings.AppPreferences
import app.menosan.android.core.settings.ThemeMode
import app.menosan.android.core.ui.theme.MenosanTheme
import app.menosan.android.data.repo.AccountRepository
import app.menosan.android.navigation.DashboardRoute
import app.menosan.android.navigation.MenosanNavHost
import app.menosan.android.navigation.SignInRoute
import app.menosan.android.navigation.WelcomeRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: AuthService

    @Inject lateinit var accounts: AccountRepository

    @Inject lateinit var preferences: AppPreferences

    @Inject lateinit var connectivity: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination: Any = when {
            accounts.hasConfirmedAccount() -> DashboardRoute
            auth.currentUser != null -> SignInRoute
            else -> WelcomeRoute
        }

        setContent {
            val authUser by auth.authState.collectAsStateWithLifecycle(initialValue = auth.currentUser)
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    connectivity.online.connectionNotices().collect { online ->
                        val text = if (online) R.string.connection_online_toast else R.string.connection_offline_toast
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            val online by connectivity.online.collectAsStateWithLifecycle(initialValue = connectivity.isOnline())
            CompositionLocalProvider(LocalOnline provides online) {
                MenosanTheme(darkTheme = darkTheme) {
                    MenosanNavHost(startDestination = remember { startDestination }, authUser = authUser)
                }
            }
        }
    }
}
