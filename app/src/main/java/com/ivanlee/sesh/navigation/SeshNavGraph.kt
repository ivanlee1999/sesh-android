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
            PlaceholderScreen("Analytics")
        }
        composable(Screen.History.route) {
            PlaceholderScreen("History")
        }
        composable(Screen.Settings.route) {
            PlaceholderScreen("Settings")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$title — Coming Soon")
    }
}
