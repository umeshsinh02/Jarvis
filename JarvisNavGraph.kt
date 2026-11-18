package com.jarvis.assistant.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jarvis.assistant.presentation.ui.screens.MainScreen
import com.jarvis.assistant.presentation.ui.screens.SettingsScreen
import com.jarvis.assistant.presentation.ui.screens.SplashScreen
import com.jarvis.assistant.presentation.viewmodel.JarvisViewModel

sealed class JarvisRoute(val route: String) {
    data object Splash : JarvisRoute("splash")
    data object Main : JarvisRoute("main")
    data object Settings : JarvisRoute("settings")
}

@Composable
fun JarvisNavGraph(
    jarvisViewModel: JarvisViewModel,
    onRequestScreenshot: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = JarvisRoute.Splash.route
    ) {
        composable(JarvisRoute.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(JarvisRoute.Main.route) {
                        popUpTo(JarvisRoute.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(JarvisRoute.Main.route) {
            MainScreen(
                viewModel = jarvisViewModel,
                onOpenSettings = { navController.navigate(JarvisRoute.Settings.route) },
                onRequestScreenshot = onRequestScreenshot
            )
        }

        composable(JarvisRoute.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
