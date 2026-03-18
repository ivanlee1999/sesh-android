package com.ivanlee.sesh.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.ivanlee.sesh.MainActivity
import com.ivanlee.sesh.R
import com.ivanlee.sesh.SeshApplication
import com.ivanlee.sesh.data.db.entity.SessionEntity
import com.ivanlee.sesh.data.repository.SessionRepository
import com.ivanlee.sesh.domain.model.BreakType
import com.ivanlee.sesh.domain.model.TimerPhase
import com.ivanlee.sesh.domain.model.TimerState
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ivanlee.sesh.data.calendar.CalendarSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var repository: SessionRepository

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // Timing state
    private var startRealtimeMs: Long = 0L
    private var pauseStartRealtimeMs: Long = 0L
    private var accumulatedPauseMs: Long = 0L
    private var startedAtInstant: Instant = Instant.now()

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateTimerState()
            val interval = calculateTickInterval()
            handler.postDelayed(this, interval)
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOCUS -> {
                val targetMs = intent.getLongExtra(EXTRA_TARGET_MS, 25 * 60 * 1000L)
                val intention = intent.getStringExtra(EXTRA_INTENTION) ?: ""
                val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
                val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
                val categoryColor = intent.getStringExtra(EXTRA_CATEGORY_COLOR) ?: "#1976D2"
                startFocus(targetMs, intention, categoryId, categoryName, categoryColor)
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_FINISH -> finish()
            ACTION_ABANDON -> abandon()
            ACTION_UNDO_ABANDON -> undoAbandon()
            ACTION_START_BREAK -> {
                val breakType = intent.getStringExtra(EXTRA_BREAK_TYPE) ?: BreakType.Short.name
                startBreak(BreakType.valueOf(breakType))
            }
            ACTION_FINISH_BREAK -> finishBreak()
            ACTION_ALARM_FIRED -> onAlarmFired()
        }
        return START_STICKY
    }

    private fun startFocus(
        targetMs: Long,
        intention: String,
        categoryId: String?,
        categoryName: String,
        categoryColor: String
    ) {
        startRealtimeMs = SystemClock.elapsedRealtime()
        accumulatedPauseMs = 0L
        startedAtInstant = Instant.now()

        _timerState.value = TimerState(
            phase = TimerPhase.Focus,
            remainingMs = targetMs,
            elapsedMs = 0L,
            targetMs = targetMs,
            intention = intention,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryColor = categoryColor
        )

        scheduleAlarm(targetMs)
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(tickRunnable)
    }

    private fun pause() {
        val current = _timerState.value
        if (current.phase != TimerPhase.Focus && current.phase != TimerPhase.Overflow &&
            current.phase != TimerPhase.Break && current.phase != TimerPhase.BreakOverflow
        ) return

        pauseStartRealtimeMs = SystemClock.elapsedRealtime()
        handler.removeCallbacks(tickRunnable)
        cancelAlarm()

        _timerState.value = current.copy(
            phase = TimerPhase.Paused,
            pausedPhase = current.phase
        )
        updateNotification()
    }

    private fun resume() {
        val current = _timerState.value
        if (current.phase != TimerPhase.Paused || current.pausedPhase == null) return

        val pauseDuration = SystemClock.elapsedRealtime() - pauseStartRealtimeMs
        accumulatedPauseMs += pauseDuration

        val resumePhase = current.pausedPhase
        _timerState.value = current.copy(
            phase = resumePhase,
            pausedPhase = null,
            totalPausedMs = accumulatedPauseMs
        )

        if (resumePhase == TimerPhase.Focus || resumePhase == TimerPhase.Break) {
            val remaining = current.remainingMs
            if (remaining > 0) {
                scheduleAlarm(remaining)
            }
        }

        handler.post(tickRunnable)
        updateNotification()
    }

    private fun finish() {
        val current = _timerState.value
        if (current.phase != TimerPhase.Focus && current.phase != TimerPhase.Overflow &&
            current.phase != TimerPhase.Paused
        ) return

        // If paused, account for final pause duration
        if (current.phase == TimerPhase.Paused) {
            val pauseDuration = SystemClock.elapsedRealtime() - pauseStartRealtimeMs
            accumulatedPauseMs += pauseDuration
        }

        handler.removeCallbacks(tickRunnable)
        cancelAlarm()

        val totalElapsedMs = SystemClock.elapsedRealtime() - startRealtimeMs
        val actualMs = totalElapsedMs - accumulatedPauseMs
        val actualSeconds = actualMs / 1000
        val targetSeconds = current.targetMs / 1000
        val overflowSeconds = maxOf(0L, actualSeconds - targetSeconds)
        val sessionType = if (actualSeconds >= targetSeconds) "full_focus" else "partial_focus"
        val now = Instant.now().toString()

        val session = SessionEntity(
            id = UUID.randomUUID().toString(),
            title = current.intention,
            categoryId = current.categoryId,
            sessionType = sessionType,
            targetSeconds = targetSeconds,
            actualSeconds = actualSeconds,
            pauseSeconds = accumulatedPauseMs / 1000,
            overflowSeconds = overflowSeconds,
            startedAt = startedAtInstant.toString(),
            endedAt = now,
            createdAt = now
        )

        serviceScope.launch {
            repository.saveSession(session)
        }

        // Enqueue calendar sync (worker checks if enabled before syncing)
        val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
            .setInputData(workDataOf("session_id" to session.id))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(syncRequest)

        _timerState.value = TimerState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun abandon() {
        val current = _timerState.value
        if (current.phase != TimerPhase.Focus && current.phase != TimerPhase.Overflow &&
            current.phase != TimerPhase.Paused
        ) return

        handler.removeCallbacks(tickRunnable)
        cancelAlarm()

        _timerState.value = current.copy(
            phase = TimerPhase.Abandoned,
            undoDeadlineMs = SystemClock.elapsedRealtime() + UNDO_WINDOW_MS
        )
        updateNotification()

        // Auto-reset after undo window
        handler.postDelayed({
            val state = _timerState.value
            if (state.phase == TimerPhase.Abandoned) {
                _timerState.value = TimerState()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }, UNDO_WINDOW_MS)
    }

    private fun undoAbandon() {
        val current = _timerState.value
        if (current.phase != TimerPhase.Abandoned) return

        val resumePhase = current.pausedPhase ?: TimerPhase.Focus
        _timerState.value = current.copy(
            phase = resumePhase,
            pausedPhase = null,
            undoDeadlineMs = 0L
        )

        if (resumePhase == TimerPhase.Focus && current.remainingMs > 0) {
            scheduleAlarm(current.remainingMs)
        }

        handler.post(tickRunnable)
        updateNotification()
    }

    private fun startBreak(breakType: BreakType) {
        val targetMs = breakType.durationMinutes * 60 * 1000L
        startRealtimeMs = SystemClock.elapsedRealtime()
        accumulatedPauseMs = 0L

        _timerState.value = TimerState(
            phase = TimerPhase.Break,
            remainingMs = targetMs,
            targetMs = targetMs
        )

        scheduleAlarm(targetMs)
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(tickRunnable)
    }

    private fun finishBreak() {
        handler.removeCallbacks(tickRunnable)
        cancelAlarm()
        _timerState.value = TimerState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onAlarmFired() {
        val current = _timerState.value
        when (current.phase) {
            TimerPhase.Focus -> {
                _timerState.value = current.copy(
                    phase = TimerPhase.Overflow,
                    remainingMs = 0L,
                    elapsedMs = 0L
                )
            }
            TimerPhase.Break -> {
                _timerState.value = current.copy(
                    phase = TimerPhase.BreakOverflow,
                    remainingMs = 0L,
                    elapsedMs = 0L
                )
            }
            else -> {}
        }
        // Fire completion notification
        fireCompletionNotification()
    }

    private fun updateTimerState() {
        val current = _timerState.value
        val now = SystemClock.elapsedRealtime()
        val totalElapsed = now - startRealtimeMs - accumulatedPauseMs

        when (current.phase) {
            TimerPhase.Focus -> {
                val remaining = current.targetMs - totalElapsed
                if (remaining <= 0) {
                    _timerState.value = current.copy(
                        phase = TimerPhase.Overflow,
                        remainingMs = 0L,
                        elapsedMs = -remaining
                    )
                    fireCompletionNotification()
                } else {
                    _timerState.value = current.copy(
                        remainingMs = remaining,
                        elapsedMs = totalElapsed
                    )
                }
            }
            TimerPhase.Overflow -> {
                val overflowElapsed = totalElapsed - current.targetMs
                _timerState.value = current.copy(elapsedMs = overflowElapsed)
            }
            TimerPhase.Break -> {
                val remaining = current.targetMs - totalElapsed
                if (remaining <= 0) {
                    _timerState.value = current.copy(
                        phase = TimerPhase.BreakOverflow,
                        remainingMs = 0L,
                        elapsedMs = -remaining
                    )
                    fireCompletionNotification()
                } else {
                    _timerState.value = current.copy(
                        remainingMs = remaining,
                        elapsedMs = totalElapsed
                    )
                }
            }
            TimerPhase.BreakOverflow -> {
                val overflowElapsed = totalElapsed - current.targetMs
                _timerState.value = current.copy(elapsedMs = overflowElapsed)
            }
            else -> {}
        }
        updateNotification()
    }

    private fun calculateTickInterval(): Long {
        val current = _timerState.value
        return when {
            current.phase == TimerPhase.Focus && current.remainingMs <= 60_000 -> 1_000L
            current.phase == TimerPhase.Break && current.remainingMs <= 60_000 -> 1_000L
            else -> 15_000L
        }
    }

    private fun scheduleAlarm(delayMs: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TimerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMs,
                pendingIntent
            )
        } else {
            // Fall back to inexact alarm when exact alarm permission is not granted
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMs,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TimerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun buildNotification(): Notification {
        val state = _timerState.value
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = formatTime(state.displayTimeMs)
        val phaseText = when (state.phase) {
            TimerPhase.Focus -> "Focus"
            TimerPhase.Overflow -> "Overflow"
            TimerPhase.Break -> "Break"
            TimerPhase.BreakOverflow -> "Break Overflow"
            TimerPhase.Paused -> "Paused"
            TimerPhase.Abandoned -> "Abandoned"
            else -> "Sesh"
        }

        val builder = NotificationCompat.Builder(this, SeshApplication.TIMER_CHANNEL_ID)
            .setContentTitle(phaseText)
            .setContentText(if (state.isOverflow) "+$timeText" else timeText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setSilent(true)

        // Add action buttons
        when (state.phase) {
            TimerPhase.Focus, TimerPhase.Overflow -> {
                builder.addAction(0, "Pause", createActionIntent(ACTION_PAUSE))
                builder.addAction(0, "Finish", createActionIntent(ACTION_FINISH))
            }
            TimerPhase.Paused -> {
                builder.addAction(0, "Resume", createActionIntent(ACTION_RESUME))
                builder.addAction(0, "Finish", createActionIntent(ACTION_FINISH))
            }
            TimerPhase.Break, TimerPhase.BreakOverflow -> {
                builder.addAction(0, "End Break", createActionIntent(ACTION_FINISH_BREAK))
            }
            else -> {}
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun fireCompletionNotification() {
        val state = _timerState.value
        val title = when {
            state.phase == TimerPhase.Overflow || state.phase == TimerPhase.Focus -> "Focus Complete!"
            state.phase == TimerPhase.BreakOverflow || state.phase == TimerPhase.Break -> "Break Complete!"
            else -> "Timer Complete!"
        }

        val builder = NotificationCompat.Builder(this, SeshApplication.ALERTS_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(state.intention.ifEmpty { "Your session is done" })
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))

        // Add context-appropriate action buttons
        when {
            state.phase == TimerPhase.Overflow || state.phase == TimerPhase.Focus -> {
                builder.addAction(0, "Start Break", createActionIntent(ACTION_START_BREAK))
                builder.addAction(0, "Dismiss", createActionIntent(ACTION_FINISH))
            }
            state.phase == TimerPhase.BreakOverflow || state.phase == TimerPhase.Break -> {
                builder.addAction(0, "Start Focus", createActionIntent(ACTION_START_FOCUS))
                builder.addAction(0, "Dismiss", createActionIntent(ACTION_FINISH_BREAK))
            }
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(COMPLETION_NOTIFICATION_ID, builder.build())
    }

    private fun createActionIntent(action: String): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        cancelAlarm()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_FOCUS = "com.ivanlee.sesh.START_FOCUS"
        const val ACTION_PAUSE = "com.ivanlee.sesh.PAUSE"
        const val ACTION_RESUME = "com.ivanlee.sesh.RESUME"
        const val ACTION_FINISH = "com.ivanlee.sesh.FINISH"
        const val ACTION_ABANDON = "com.ivanlee.sesh.ABANDON"
        const val ACTION_UNDO_ABANDON = "com.ivanlee.sesh.UNDO_ABANDON"
        const val ACTION_START_BREAK = "com.ivanlee.sesh.START_BREAK"
        const val ACTION_FINISH_BREAK = "com.ivanlee.sesh.FINISH_BREAK"
        const val ACTION_ALARM_FIRED = "com.ivanlee.sesh.ALARM_FIRED"

        const val EXTRA_TARGET_MS = "target_ms"
        const val EXTRA_INTENTION = "intention"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_CATEGORY_COLOR = "category_color"
        const val EXTRA_BREAK_TYPE = "break_type"

        const val NOTIFICATION_ID = 1
        const val COMPLETION_NOTIFICATION_ID = 2
        const val UNDO_WINDOW_MS = 5_000L

        fun formatTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
