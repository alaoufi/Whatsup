package com.example.whatsappreminder.util

import android.content.Context
import com.example.whatsappreminder.data.backup.ReminderBackup
import com.example.whatsappreminder.domain.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نسخ احتياطي تلقائي: يكتب كل التذكيرات إلى ملف داخلي عند كل فتح للتطبيق،
 * ويتيح الاستعادة منه عند الحاجة (مثلاً بعد حذف خاطئ أو مشكلة).
 */
@Singleton
class AutoBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReminderRepository
) {

    private fun backupFile(): File = File(context.filesDir, "auto_backup.json")

    /** كتابة نسخة احتياطية من الوضع الحالي (تُستدعى عند فتح التطبيق) */
    suspend fun run() = withContext(Dispatchers.IO) {
        runCatching {
            val reminders = repository.getAllReminders().first()
            if (reminders.isNotEmpty()) {
                backupFile().writeText(ReminderBackup.toJson(reminders))
            }
        }
    }

    /** قراءة محتوى النسخة التلقائية (null إن لم توجد) */
    fun read(): String? = runCatching {
        backupFile().takeIf { it.exists() }?.readText()
    }.getOrNull()
}
