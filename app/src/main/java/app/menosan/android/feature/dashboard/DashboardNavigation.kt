package app.menosan.android.feature.dashboard

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.menosan.android.feature.entries.EntryDetailsRoute
import app.menosan.android.navigation.DashboardRoute
import app.menosan.android.navigation.EntriesRoute
import app.menosan.android.navigation.LogManualRoute
import app.menosan.android.navigation.LogPhotoRoute
import app.menosan.android.navigation.ReportRoute

fun NavGraphBuilder.dashboardScreens(navController: NavHostController) {
    composable<DashboardRoute> {
        HomeRoute(
            actions = HomeActions(
                onLogManually = { navController.navigate(LogManualRoute()) },
                onLogWithPhoto = { navController.navigate(LogPhotoRoute) },
                onOpenReport = { navController.navigate(ReportRoute(it.toString())) },
                onViewAllEntries = {
                    navController.navigate(EntriesRoute) {
                        popUpTo<DashboardRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenEntry = { navController.navigate(EntryDetailsRoute(it)) },
                onEditEntry = { navController.navigate(LogManualRoute(entryId = it)) },
            ),
        )
    }
}
