package app.menosan.android.feature.reports

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.menosan.android.navigation.HistoryRoute
import app.menosan.android.navigation.ReportRoute

fun NavGraphBuilder.reportsScreens(navController: NavHostController) {
    composable<HistoryRoute> {
        InsightsScreen(onOpenReport = { week -> navController.navigate(ReportRoute(week.toString())) })
    }
    composable<ReportRoute> {
        ReportScreen(onBack = { navController.popBackStack() })
    }
}
