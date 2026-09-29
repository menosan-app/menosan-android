package app.menosan.android.core.network

import androidx.compose.runtime.compositionLocalOf

/** Whether the device has working internet, provided once by `MainActivity` from [ConnectivityObserver]. */
val LocalOnline = compositionLocalOf { true }
