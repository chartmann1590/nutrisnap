package com.charles.nutrisnap.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.charles.nutrisnap.feature.achievements.AchievementsScreen
import com.charles.nutrisnap.feature.dashboard.DashboardScreen
import com.charles.nutrisnap.feature.dashboard.EditMealScreen
import com.charles.nutrisnap.feature.entry.EntryScreen
import com.charles.nutrisnap.feature.history.DiaryScreen
import com.charles.nutrisnap.feature.history.TrendsScreen
import com.charles.nutrisnap.feature.milestones.MilestonesScreen
import com.charles.nutrisnap.feature.onboarding.DownloadScreen
import com.charles.nutrisnap.feature.onboarding.OnboardingEvent
import com.charles.nutrisnap.feature.onboarding.OnboardingScreen
import com.charles.nutrisnap.feature.onboarding.OnboardingViewModel
import com.charles.nutrisnap.feature.pip.PipChatScreen
import com.charles.nutrisnap.feature.piproom.PipRoomScreen
import com.charles.nutrisnap.feature.profile.ProfileScreen
import com.charles.nutrisnap.feature.scan.ScanResultScreen
import com.charles.nutrisnap.feature.scan.ScanScreen
import com.charles.nutrisnap.feature.settings.SettingsScreen

@Composable
fun NutriNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.HOME,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) +
                fadeIn(tween(280))
        },
        exitTransition = {
            fadeOut(tween(200))
        },
        popEnterTransition = {
            fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) +
                fadeOut(tween(280))
        },
    ) {
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                vm.events.collect { event ->
                    when (event) {
                        OnboardingEvent.NavigateToDownload -> {
                            navController.navigate(Routes.DOWNLOAD) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }
                }
            }
            OnboardingScreen(onFinished = { vm.finish() })
        }
        composable(Routes.DOWNLOAD) {
            DownloadScreen(onComplete = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }

        composable(Routes.HOME) {
            DashboardScreen(
                onOpenMeal = { mealId -> navController.navigate(Routes.editMeal(mealId)) },
                onAddMeal = { navController.navigate(Routes.entry("manual")) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPipChat = { navController.navigate(Routes.PIP_CHAT) },
            )
        }
        composable(Routes.DIARY) { DiaryScreen() }
        composable(Routes.TRENDS) { TrendsScreen() }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onPipRoom = { navController.navigate(Routes.PIP_ROOM) },
                onMilestones = { navController.navigate(Routes.MILESTONES) },
                onTrends = { navController.navigate(Routes.TRENDS) },
            )
        }
        composable(Routes.ACHIEVEMENTS) {
            AchievementsScreen(
                onBack = { navController.popBackStack() },
                onPipChat = { navController.navigate(Routes.PIP_CHAT) },
            )
        }
        composable(Routes.PIP_ROOM) {
            PipRoomScreen(
                onBack = { navController.popBackStack() },
                onPipChat = { navController.navigate(Routes.PIP_CHAT) },
            )
        }
        composable(Routes.MILESTONES) {
            MilestonesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SCAN) {
            ScanScreen(
                onClose = { navController.popBackStack() },
                onAnalyzed = { estimateKey ->
                    navController.navigate(Routes.scanResult(estimateKey))
                },
                onManualEntry = { navController.navigate(Routes.entry("manual")) },
            )
        }
        composable(
            route = Routes.SCAN_RESULT,
            arguments = listOf(
                navArgument("estimateKey") { type = NavType.StringType }
            ),
        ) { backStackEntry ->
            val estimateKey = backStackEntry.arguments?.getString("estimateKey") ?: return@composable
            ScanResultScreen(
                estimateKey = estimateKey,
                onLogged = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onClose = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.ENTRY,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("bitmapKey") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "manual"
            val bitmapKey = backStackEntry.arguments?.getString("bitmapKey")
            EntryScreen(
                mode = mode,
                bitmapKey = bitmapKey,
                onLogged = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onClose = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EDIT_MEAL,
            arguments = listOf(
                navArgument("mealId") { type = NavType.LongType }
            ),
        ) { backStackEntry ->
            val mealId = backStackEntry.arguments?.getLong("mealId") ?: return@composable
            EditMealScreen(
                mealId = mealId,
                onDeleted = { navController.popBackStack() },
                onClose = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PIP_CHAT) {
            PipChatScreen(onBack = { navController.popBackStack() })
        }
    }
}
