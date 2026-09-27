package app.menosan.android.feature.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.menosan.android.navigation.SettingsRoute

@Suppress("UNUSED_PARAMETER")
fun NavGraphBuilder.settingsScreens(navController: NavHostController) {
    composable<SettingsRoute> { ProfileRoute() }
}
