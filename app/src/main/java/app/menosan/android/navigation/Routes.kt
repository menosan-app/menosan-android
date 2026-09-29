package app.menosan.android.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object SignInRoute

@Serializable
data object CreateAccountRoute

@Serializable
data object AccountReadyRoute

@Serializable
data object DashboardRoute

@Serializable
data class LogManualRoute(val entryId: String? = null)

@Serializable
data object LogPhotoRoute

@Serializable
data object EntriesRoute

@Serializable
data object HistoryRoute

@Serializable
data class ReportRoute(val weekStart: String, val tab: String? = null)

@Serializable
data object SettingsRoute
