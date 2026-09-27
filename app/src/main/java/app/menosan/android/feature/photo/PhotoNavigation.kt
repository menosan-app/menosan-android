package app.menosan.android.feature.photo

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.menosan.android.navigation.LogManualRoute
import app.menosan.android.navigation.LogPhotoRoute

fun NavGraphBuilder.photoScreens(navController: NavHostController) {
    composable<LogPhotoRoute> {
        PhotoLogRoute(
            onDone = { navController.popBackStack() },
            onLogManually = {
                navController.navigate(LogManualRoute()) { popUpTo<LogPhotoRoute> { inclusive = true } }
            },
        )
    }
}
