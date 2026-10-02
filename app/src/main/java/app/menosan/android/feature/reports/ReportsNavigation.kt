package app.menosan.android.feature.reports

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.menosan.android.navigation.HistoryRoute
import app.menosan.android.navigation.ReportRoute

fun NavGraphBuilder.reportsScreens(navController: NavHostController) {
    composable<HistoryRoute> {
        InsightsScreen(onOpenReport = { week, openIdeas ->
            navController.navigate(ReportRoute(week.toString(), if (openIdeas) REPORT_ROUTE_IDEAS else null))
        })
    }
    composable<ReportRoute> {
        ReportScreen(onBack = { navController.popBackStack() })
    }
}
