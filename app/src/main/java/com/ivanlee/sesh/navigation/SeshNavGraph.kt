package com.ivanlee.sesh.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ivanlee.sesh.ui.screen.analytics.AnalyticsScreen
import com.ivanlee.sesh.ui.screen.history.HistoryScreen
import com.ivanlee.sesh.ui.screen.timer.TimerScreen

@Composable
fun SeshNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Timer.route,
        modifier = modifier
    ) {
        composable(Screen.Timer.route) {
            TimerScreen()
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }
        composable(Screen.History.route) {
            HistoryScreen()
        }
        composable(Screen.Settings.route) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Settings — Coming Soon")
            }
        }
    }
}
