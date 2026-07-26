package com.example.blue.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.blue.data.local.dao.AccountDao
import com.example.blue.data.local.dao.DiaryDao
import com.example.blue.data.local.dao.HomeDao
import com.example.blue.data.local.dao.SleepRecordDao
import com.example.blue.data.local.dao.TimeDao
import com.example.blue.data.local.entity.AccountCategoryEntity
import com.example.blue.data.local.entity.AccountEntryEntity
import com.example.blue.data.local.entity.DiaryEntity
import com.example.blue.data.local.entity.DiaryImageEntity
import com.example.blue.data.local.entity.SleepRecordEntity
import com.example.blue.data.local.entity.TimeEventEntity
import com.example.blue.data.local.entity.TimeProfileEntity

@Database(
    entities = [
        DiaryEntity::class,
        DiaryImageEntity::class,
        AccountEntryEntity::class,
        AccountCategoryEntity::class,
        SleepRecordEntity::class,
        TimeProfileEntity::class,
        TimeEventEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun homeDao(): HomeDao

    abstract fun diaryDao(): DiaryDao

    abstract fun accountDao(): AccountDao

    abstract fun sleepRecordDao(): SleepRecordDao

    abstract fun timeDao(): TimeDao

    companion object {
        const val DATABASE_NAME = "life_record.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE diaries ADD COLUMN mood INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sleep_records (
                        id TEXT NOT NULL,
                        recordDate TEXT NOT NULL,
                        sleepDateTime TEXT NOT NULL,
                        wakeDateTime TEXT,
                        source TEXT NOT NULL,
                        isEstimated INTEGER NOT NULL,
                        note TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_sleep_records_recordDate " +
                        "ON sleep_records(recordDate)",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS time_profiles (
                        id INTEGER NOT NULL,
                        age INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS time_events (
                        id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        eventDate TEXT NOT NULL,
                        type TEXT NOT NULL,
                        imagePath TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_time_events_type ON time_events(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_time_events_eventDate ON time_events(eventDate)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE time_profiles ADD COLUMN birthday TEXT")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
        )
    }
}
