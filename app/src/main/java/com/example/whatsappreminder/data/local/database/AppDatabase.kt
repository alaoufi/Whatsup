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
    version = 4,
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

        /** ترحيل من 2 إلى 3: إضافة عمودَي التكرار والتصنيف */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'"
                )
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN category TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /** ترحيل من 3 إلى 4: أيام التكرار المخصصة وتاريخ انتهاء التكرار */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN recurrence_days INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN recurrence_end INTEGER"
                )
            }
        }
    }
}
