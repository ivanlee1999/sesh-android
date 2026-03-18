package com.ivanlee.sesh.ui.screen.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ivanlee.sesh.ui.components.EinkButton
import com.ivanlee.sesh.ui.theme.EinkColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || result.data != null) {
            result.data?.let { intent ->
                viewModel.handleGoogleSignInResult(intent)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Google Calendar section
        Text(
            text = "Google Calendar",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, EinkColors.OnBackground, RectangleShape)
                .padding(16.dp)
        ) {
            // Sync enabled toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync sessions",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = uiState.isCalendarSyncEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCalendarSync(enabled)
                    },
                    enabled = uiState.isGoogleAuthorized
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Auth status
            Text(
                text = if (uiState.isGoogleAuthorized) "Connected" else "Not connected",
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isGoogleAuthorized) EinkColors.Focus else EinkColors.Disabled
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sign in / Sign out button
            if (uiState.isGoogleAuthorized) {
                EinkButton(
                    text = "SIGN OUT",
                    onClick = { viewModel.signOutGoogle() },
                    filled = false
                )
            } else {
                EinkButton(
                    text = "SIGN IN WITH GOOGLE",
                    onClick = {
                        val intent = viewModel.getGoogleSignInIntent()
                        authLauncher.launch(intent)
                    },
                    backgroundColor = EinkColors.OnBackground,
                    contentColor = EinkColors.Background
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider(color = EinkColors.OnBackground, thickness = 1.dp)

        Spacer(modifier = Modifier.height(24.dp))

        // App info
        Text(
            text = "Sesh v1.0.0",
            style = MaterialTheme.typography.bodyMedium,
            color = EinkColors.Disabled
        )
    }
}
