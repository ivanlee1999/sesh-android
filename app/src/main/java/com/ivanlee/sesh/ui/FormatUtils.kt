package com.ivanlee.sesh.ui

/**
 * Shared duration formatting utilities used across screens.
 */
object FormatUtils {

    /**
     * Formats a duration given in total seconds.
     * - 0 seconds → "0m"
     * - 1–59 seconds → "<1m"
     * - 60+ seconds → "Xm" or "Xh Ym"
     */
    fun formatDurationSeconds(totalSeconds: Long): String {
        if (totalSeconds <= 0) return "0m"
        val minutes = totalSeconds / 60
        if (minutes == 0L) return "<1m"
        return formatMinutes(minutes)
    }

    /**
     * Formats a duration given in fractional minutes (e.g. from analytics queries).
     * - <1 minute (but > 0) → "<1m"
     * - 0 → "0m"
     * - 1+ → "Xm" or "Xh Ym"
     */
    fun formatDurationMinutes(minutes: Double): String {
        if (minutes <= 0.0) return "0m"
        val totalMinutes = minutes.toLong()
        if (totalMinutes == 0L) return "<1m"
        return formatMinutes(totalMinutes)
    }

    private fun formatMinutes(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${minutes}m"
    }
}
