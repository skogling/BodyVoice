package com.liangji.bodymeasure.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.liangji.bodymeasure.core.designsystem.BodyMeasureTheme
import com.liangji.bodymeasure.core.designsystem.Motion
import com.liangji.bodymeasure.core.designsystem.SoftGradientBackground
import com.liangji.bodymeasure.ui.metrics.ArchivedMetricsScreen
import com.liangji.bodymeasure.ui.metrics.MetricDetailScreen
import com.liangji.bodymeasure.ui.metrics.MetricEditScreen
import com.liangji.bodymeasure.ui.metrics.MetricsScreen
import com.liangji.bodymeasure.ui.navigation.ArchivedMetricsRoute
import com.liangji.bodymeasure.ui.navigation.MetricDetailRoute
import com.liangji.bodymeasure.ui.navigation.MetricEditRoute
import com.liangji.bodymeasure.ui.navigation.MetricsRoute
import com.liangji.bodymeasure.ui.navigation.SettingsRoute
import com.liangji.bodymeasure.ui.settings.SettingsScreen

@Composable
fun BodyMeasureApp(viewModel: AppViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    BodyMeasureTheme(themeMode = themeMode) {
        SoftGradientBackground {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = MetricsRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                enterTransition = {
                    fadeIn(tween(Motion.standard)) +
                        slideInHorizontally(tween(Motion.standard)) { it / 18 }
                },
                exitTransition = { fadeOut(tween(Motion.quick)) },
                popEnterTransition = { fadeIn(tween(Motion.standard)) },
                popExitTransition = {
                    fadeOut(tween(Motion.quick)) +
                        slideOutHorizontally(tween(Motion.quick)) { it / 18 }
                },
                predictivePopEnterTransition = { fadeIn(tween(Motion.standard)) },
                predictivePopExitTransition = {
                    fadeOut(tween(Motion.quick)) +
                        slideOutHorizontally(tween(Motion.quick)) { it / 18 }
                },
            ) {
                composable<MetricsRoute> {
                    MetricsScreen(
                        onOpenSettings = {
                            navController.navigate(SettingsRoute) { launchSingleTop = true }
                        },
                        onCreateMetric = {
                            navController.navigate(MetricEditRoute()) { launchSingleTop = true }
                        },
                        onEditMetric = { metricId ->
                            navController.navigate(MetricEditRoute(metricId)) { launchSingleTop = true }
                        },
                        onOpenMetric = { metricId ->
                            navController.navigate(MetricDetailRoute(metricId)) { launchSingleTop = true }
                        },
                    )
                }
                composable<MetricDetailRoute> {
                    MetricDetailScreen(
                        onBack = { navController.popBackStack() },
                        onEditMetric = { metricId ->
                            navController.navigate(MetricEditRoute(metricId)) { launchSingleTop = true }
                        },
                    )
                }
                composable<MetricEditRoute> {
                    MetricEditScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable<ArchivedMetricsRoute> {
                    ArchivedMetricsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenMetric = { metricId ->
                            navController.navigate(MetricDetailRoute(metricId)) { launchSingleTop = true }
                        },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenArchive = {
                            navController.navigate(ArchivedMetricsRoute) { launchSingleTop = true }
                        },
                    )
                }
            }
        }
    }
}
