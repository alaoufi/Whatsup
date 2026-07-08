package com.example.whatsappreminder.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * قاعدة بيانات التطبيق (Room).
 * الإصدار = 2 (أضيفت أعمدة إعدادات الصوت وفتح واتساب لكل تذكير).
 * لا نحتاج TypeConverter لأن الحالة مخزّنة كنص مباشرة.
 */
@Database(
    entities = [ReminderEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "whatsapp_reminder.db"

        /** ترحيل من الإصدار 1 إلى 2: إضافة عمودَي الإعدادات لكل تذكير */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN sound_enabled INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN open_whatsapp_directly INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
