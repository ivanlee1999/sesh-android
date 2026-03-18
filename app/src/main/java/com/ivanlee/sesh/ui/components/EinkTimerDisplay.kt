package com.ivanlee.sesh.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ivanlee.sesh.domain.model.TimerPhase
import com.ivanlee.sesh.service.TimerService
import com.ivanlee.sesh.ui.theme.EinkColors

@Composable
fun EinkTimerDisplay(
    timeMs: Long,
    phase: TimerPhase,
    isOverflow: Boolean,
    modifier: Modifier = Modifier
) {
    val color = phaseColor(phase)
    val timeText = TimerService.formatTime(timeMs)
    val displayText = if (isOverflow) "+$timeText" else timeText

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.displayLarge,
            color = color
        )
    }
}

fun phaseColor(phase: TimerPhase): Color {
    return when (phase) {
        TimerPhase.Focus -> EinkColors.Focus
        TimerPhase.Overflow -> EinkColors.Overflow
        TimerPhase.Break -> EinkColors.Break
        TimerPhase.BreakOverflow -> EinkColors.Overflow
        TimerPhase.Paused -> EinkColors.Paused
        TimerPhase.Abandoned -> EinkColors.Abandoned
        TimerPhase.Idle -> EinkColors.OnBackground
    }
}
