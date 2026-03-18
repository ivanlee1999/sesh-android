package com.ivanlee.sesh.ui.screen.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivanlee.sesh.data.db.entity.CategoryEntity
import com.ivanlee.sesh.data.repository.SessionRepository
import com.ivanlee.sesh.domain.model.BreakType
import com.ivanlee.sesh.domain.model.TimerPhase
import com.ivanlee.sesh.domain.model.TimerState
import com.ivanlee.sesh.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerUiState(
    val timerState: TimerState = TimerState(),
    val intention: String = "",
    val selectedCategory: CategoryEntity? = null,
    val targetMinutes: Int = 25,
    val todayMinutes: Double = 0.0,
    val todaySessionCount: Int = 0
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.getActiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerService: TimerService? = null
    private var bound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            bound = true
            viewModelScope.launch {
                binder.getService().timerState.collect { state ->
                    _uiState.value = _uiState.value.copy(timerState = state)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            bound = false
        }
    }

    init {
        bindService()
        viewModelScope.launch {
            repository.getTodayFocusMinutes().collect { minutes ->
                _uiState.value = _uiState.value.copy(todayMinutes = minutes)
            }
        }
        viewModelScope.launch {
            repository.getTodaySessionCount().collect { count ->
                _uiState.value = _uiState.value.copy(todaySessionCount = count)
            }
        }
    }

    private fun bindService() {
        val intent = Intent(context, TimerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun updateIntention(intention: String) {
        _uiState.value = _uiState.value.copy(intention = intention)
    }

    fun selectCategory(category: CategoryEntity?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun adjustDuration(deltaMinutes: Int) {
        val newMinutes = (_uiState.value.targetMinutes + deltaMinutes).coerceIn(1, 120)
        _uiState.value = _uiState.value.copy(targetMinutes = newMinutes)
    }

    fun startFocus() {
        val state = _uiState.value
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_FOCUS
            putExtra(TimerService.EXTRA_TARGET_MS, state.targetMinutes * 60 * 1000L)
            putExtra(TimerService.EXTRA_INTENTION, state.intention)
            putExtra(TimerService.EXTRA_CATEGORY_ID, state.selectedCategory?.id)
            putExtra(TimerService.EXTRA_CATEGORY_NAME, state.selectedCategory?.title ?: "")
            putExtra(TimerService.EXTRA_CATEGORY_COLOR, state.selectedCategory?.hexColor ?: "#1976D2")
        }
        context.startForegroundService(intent)
    }

    fun pause() {
        sendAction(TimerService.ACTION_PAUSE)
    }

    fun resume() {
        sendAction(TimerService.ACTION_RESUME)
    }

    fun finish() {
        sendAction(TimerService.ACTION_FINISH)
    }

    fun abandon() {
        sendAction(TimerService.ACTION_ABANDON)
    }

    fun undoAbandon() {
        sendAction(TimerService.ACTION_UNDO_ABANDON)
    }

    fun startBreak(breakType: BreakType) {
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_BREAK
            putExtra(TimerService.EXTRA_BREAK_TYPE, breakType.name)
        }
        context.startForegroundService(intent)
    }

    fun finishBreak() {
        sendAction(TimerService.ACTION_FINISH_BREAK)
    }

    private fun sendAction(action: String) {
        val intent = Intent(context, TimerService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }

    override fun onCleared() {
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
        super.onCleared()
    }
}
