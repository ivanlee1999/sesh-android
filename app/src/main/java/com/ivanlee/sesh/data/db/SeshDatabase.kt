package com.ivanlee.sesh.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ivanlee.sesh.data.db.dao.CategoryDao
import com.ivanlee.sesh.data.db.dao.SessionDao
import com.ivanlee.sesh.data.db.entity.CategoryEntity
import com.ivanlee.sesh.data.db.entity.PauseEntity
import com.ivanlee.sesh.data.db.entity.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

@Database(
    entities = [
        CategoryEntity::class,
        SessionEntity::class,
        PauseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SeshDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DATABASE_NAME = "sesh.db"

        fun buildDatabase(context: Context): SeshDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SeshDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(SeedCallback())
                .build()
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val now = Instant.now().toString()
                val defaults = listOf(
                    Triple("Development", "#61AFEF", 0),
                    Triple("Writing", "#E06C75", 1),
                    Triple("Design", "#C678DD", 2),
                    Triple("Research", "#E5C07B", 3),
                    Triple("Meeting", "#56B6C2", 4),
                    Triple("Exercise", "#98C379", 5),
                    Triple("Reading", "#D19A66", 6),
                    Triple("Admin", "#ABB2BF", 7)
                )
                for ((title, color, order) in defaults) {
                    db.execSQL(
                        """INSERT OR IGNORE INTO categories (id, title, hex_color, status, sort_order, created_at, updated_at)
                           VALUES (?, ?, ?, 'active', ?, ?, ?)""",
                        arrayOf(UUID.randomUUID().toString(), title, color, order, now, now)
                    )
                }
            }
        }
    }
}
