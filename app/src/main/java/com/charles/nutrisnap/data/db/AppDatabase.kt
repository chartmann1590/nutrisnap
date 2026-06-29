package com.charles.nutrisnap.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MealEntity::class,
        WeightEntryEntity::class,
        ChatMessageEntity::class,
        BadgeEntity::class,
        DailyChallengeEntity::class,
        MilestoneEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun weightDao(): WeightDao
    abstract fun chatDao(): ChatDao
    abstract fun badgeDao(): BadgeDao
    abstract fun dailyChallengeDao(): DailyChallengeDao
    abstract fun milestoneDao(): MilestoneDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_message` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, `timestampMs` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_message_timestampMs` ON `chat_message` (`timestampMs`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS badges (badgeType TEXT NOT NULL PRIMARY KEY, earnedAtMs INTEGER NOT NULL, seen INTEGER NOT NULL DEFAULT 0)")
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_challenges (dateEpochDay INTEGER NOT NULL PRIMARY KEY, challengeId TEXT NOT NULL, completedAtMs INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS milestones (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, occurredAtMs INTEGER NOT NULL, payload TEXT NOT NULL DEFAULT '')")
            }
        }
    }
}
