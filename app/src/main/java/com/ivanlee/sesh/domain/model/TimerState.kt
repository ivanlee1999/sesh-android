package com.ivanlee.sesh.domain.model

data class TimerState(
    val phase: TimerPhase = TimerPhase.Idle,
    val remainingMs: Long = 0L,
    val elapsedMs: Long = 0L,
    val targetMs: Long = 25 * 60 * 1000L,
    val pausedPhase: TimerPhase? = null,
    val totalPausedMs: Long = 0L,
    val intention: String = "",
    val categoryId: String? = null,
    val categoryName: String = "",
    val categoryColor: String = "#1976D2",
    val undoDeadlineMs: Long = 0L
) {
    val isActive: Boolean
        get() = phase != TimerPhase.Idle && phase != TimerPhase.Abandoned

    val isRunning: Boolean
        get() = phase == TimerPhase.Focus || phase == TimerPhase.Overflow ||
                phase == TimerPhase.Break || phase == TimerPhase.BreakOverflow

    val progress: Float
        get() = when (phase) {
            TimerPhase.Focus -> {
                if (targetMs > 0) 1f - (remainingMs.toFloat() / targetMs)
                else 0f
            }
            TimerPhase.Overflow -> 1f
            TimerPhase.Break, TimerPhase.BreakOverflow -> {
                if (targetMs > 0) 1f - (remainingMs.toFloat() / targetMs)
                else 0f
            }
            TimerPhase.Paused -> {
                if (targetMs > 0 && pausedPhase == TimerPhase.Focus) {
                    1f - (remainingMs.toFloat() / targetMs)
                } else if (pausedPhase == TimerPhase.Overflow) {
                    1f
                } else {
                    0f
                }
            }
            else -> 0f
        }.coerceIn(0f, 1f)

    val displayTimeMs: Long
        get() = when (phase) {
            TimerPhase.Focus, TimerPhase.Break -> remainingMs.coerceAtLeast(0L)
            TimerPhase.Overflow, TimerPhase.BreakOverflow -> elapsedMs
            TimerPhase.Paused -> {
                if (pausedPhase == TimerPhase.Overflow || pausedPhase == TimerPhase.BreakOverflow) {
                    elapsedMs
                } else {
                    remainingMs.coerceAtLeast(0L)
                }
            }
            else -> targetMs
        }

    val isOverflow: Boolean
        get() = phase == TimerPhase.Overflow || phase == TimerPhase.BreakOverflow ||
                (phase == TimerPhase.Paused && (pausedPhase == TimerPhase.Overflow || pausedPhase == TimerPhase.BreakOverflow))
}
