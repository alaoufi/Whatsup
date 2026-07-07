package com.example.whatsappreminder.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * قاعدة بيانات التطبيق (Room).
 * الإصدار = 1 كما هو مطلوب.
 * لا نحتاج TypeConverter لأن الحالة مخزّنة كنص مباشرة.
 */
@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "whatsapp_reminder.db"
    }
}
