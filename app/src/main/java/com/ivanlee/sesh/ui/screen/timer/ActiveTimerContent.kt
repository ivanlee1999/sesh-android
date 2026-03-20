package com.ivanlee.sesh.ui.screen.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivanlee.sesh.domain.model.TimerPhase
import com.ivanlee.sesh.domain.model.TimerState
import com.ivanlee.sesh.ui.components.CircularTimer
import com.ivanlee.sesh.ui.components.EinkButton
import com.ivanlee.sesh.ui.components.phaseColor
import com.ivanlee.sesh.ui.theme.EinkColors

@Composable
fun ActiveTimerContent(
    state: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onAbandon: () -> Unit,
    onUndoAbandon: () -> Unit,
    onFinishBreak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = phaseColor(state.phase)
    val phaseLabel = when (state.phase) {
        TimerPhase.Focus -> "FOCUS"
        TimerPhase.Overflow -> "OVERFLOW"
        TimerPhase.Break -> "BREAK"
        TimerPhase.BreakOverflow -> "BREAK OVERFLOW"
        TimerPhase.Paused -> "PAUSED"
        TimerPhase.Abandoned -> "ABANDONED"
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Phase label
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Circular timer
        CircularTimer(
            timeMs = state.displayTimeMs,
            phase = state.phase,
            progress = state.progress,
            isOverflow = state.isOverflow
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Intention
        if (state.intention.isNotEmpty()) {
            Text(
                text = "\"${state.intention}\"",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        // Category
        if (state.categoryName.isNotEmpty()) {
            Text(
                text = state.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                color = EinkColors.Disabled
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        when (state.phase) {
            TimerPhase.Focus, TimerPhase.Overflow -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EinkButton(
                        text = "PAUSE",
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        filled = false
                    )
                    EinkButton(
                        text = "FINISH",
                        onClick = onFinish,
                        modifier = Modifier.weight(1f),
                        backgroundColor = EinkColors.Focus,
                        contentColor = EinkColors.Background
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                EinkButton(
                    text = "ABANDON",
                    onClick = onAbandon,
                    filled = false
                )
            }
            TimerPhase.Paused -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EinkButton(
                        text = "RESUME",
                        onClick = onResume,
                        modifier = Modifier.weight(1f),
                        backgroundColor = EinkColors.Paused,
                        contentColor = EinkColors.Background
                    )
                    EinkButton(
                        text = "FINISH",
                        onClick = onFinish,
                        modifier = Modifier.weight(1f),
                        filled = false
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                EinkButton(
                    text = "ABANDON",
                    onClick = onAbandon,
                    filled = false
                )
            }
            TimerPhase.Break, TimerPhase.BreakOverflow -> {
                EinkButton(
                    text = "END BREAK",
                    onClick = onFinishBreak,
                    backgroundColor = EinkColors.Break,
                    contentColor = EinkColors.Background
                )
            }
            TimerPhase.Abandoned -> {
                EinkButton(
                    text = "UNDO",
                    onClick = onUndoAbandon,
                    backgroundColor = EinkColors.Abandoned,
                    contentColor = EinkColors.Background
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Session will be discarded in 5 seconds...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EinkColors.Abandoned
                )
            }
            else -> {}
        }
    }
}
