package com.ivanlee.sesh.data.db.dao

import androidx.room.ColumnInfo

data class CategoryBreakdownResult(
    val name: String,
    val color: String,
    val minutes: Double
)

data class DayFocusResult(
    val date: String,
    val hours: Double
)

data class SessionWithCategory(
    val id: String,
    val title: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    @ColumnInfo(name = "category_title")
    val categoryTitle: String?,
    @ColumnInfo(name = "category_color")
    val categoryColor: String?,
    @ColumnInfo(name = "session_type")
    val sessionType: String,
    @ColumnInfo(name = "target_seconds")
    val targetSeconds: Long,
    @ColumnInfo(name = "actual_seconds")
    val actualSeconds: Long,
    @ColumnInfo(name = "pause_seconds")
    val pauseSeconds: Long,
    @ColumnInfo(name = "overflow_seconds")
    val overflowSeconds: Long,
    @ColumnInfo(name = "started_at")
    val startedAt: String,
    @ColumnInfo(name = "ended_at")
    val endedAt: String,
    val notes: String?
)
