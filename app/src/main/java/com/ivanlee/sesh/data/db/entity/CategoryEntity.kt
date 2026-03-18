package com.ivanlee.sesh.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,

    val title: String,

    @ColumnInfo(name = "hex_color", defaultValue = "#61AFEF")
    val hexColor: String = "#61AFEF",

    @ColumnInfo(defaultValue = "active")
    val status: String = "active",

    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
