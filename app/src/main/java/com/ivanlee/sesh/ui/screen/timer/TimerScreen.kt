package com.ivanlee.sesh.ui.screen.timer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanlee.sesh.domain.model.TimerPhase

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val timerState = uiState.timerState

    when (timerState.phase) {
        TimerPhase.Idle -> {
            IdleContent(
                intention = uiState.intention,
                onIntentionChange = viewModel::updateIntention,
                selectedCategory = uiState.selectedCategory,
                categories = categories,
                onCategorySelect = viewModel::selectCategory,
                targetMinutes = uiState.targetMinutes,
                onAdjustDuration = viewModel::adjustDuration,
                onStartFocus = viewModel::startFocus,
                onStartBreak = viewModel::startBreak,
                todayMinutes = uiState.todayMinutes,
                todaySessionCount = uiState.todaySessionCount,
                modifier = modifier.fillMaxSize()
            )
        }
        else -> {
            ActiveTimerContent(
                state = timerState,
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onFinish = viewModel::finish,
                onAbandon = viewModel::abandon,
                onUndoAbandon = viewModel::undoAbandon,
                onFinishBreak = viewModel::finishBreak,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}
