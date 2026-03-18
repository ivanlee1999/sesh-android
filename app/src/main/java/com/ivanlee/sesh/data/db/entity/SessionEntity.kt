package com.ivanlee.sesh.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("started_at"),
        Index("category_id"),
        Index("session_type")
    ]
)
data class SessionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(defaultValue = "")
    val title: String = "",

    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,

    @ColumnInfo(name = "session_type")
    val sessionType: String,

    @ColumnInfo(name = "target_seconds")
    val targetSeconds: Long,

    @ColumnInfo(name = "actual_seconds")
    val actualSeconds: Long,

    @ColumnInfo(name = "pause_seconds", defaultValue = "0")
    val pauseSeconds: Long = 0,

    @ColumnInfo(name = "overflow_seconds", defaultValue = "0")
    val overflowSeconds: Long = 0,

    @ColumnInfo(name = "started_at")
    val startedAt: String,

    @ColumnInfo(name = "ended_at")
    val endedAt: String,

    val notes: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: String
)
