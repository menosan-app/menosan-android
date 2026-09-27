package app.menosan.android.feature.logging

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.menosan.android.navigation.LogManualRoute

fun NavGraphBuilder.loggingScreens(navController: NavHostController) {
    composable<LogManualRoute> {
        LogEntryRoute(onDone = { navController.popBackStack() })
    }
}
