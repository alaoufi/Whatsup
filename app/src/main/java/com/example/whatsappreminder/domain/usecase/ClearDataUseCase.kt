package com.example.whatsappreminder.domain.usecase

import android.content.Context
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

/**
 * مسح شامل وعميق لكل بيانات التطبيق:
 * - إلغاء كل المنبّهات المجدولة.
 * - حذف كل التذكيرات من قاعدة البيانات.
 * - مسح كل التفضيلات (الإعدادات، النغمة، القوالب، القفل...).
 * - حذف ملفات النسخ التلقائي والكاش.
 */
class ClearDataUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        // إلغاء كل المنبّهات أولاً حتى لا تعمل بعد المسح
        val all = repository.getAllReminders().first()
        all.forEach { scheduler.cancel(it.id) }

        // حذف كل التذكيرات
        repository.deleteAllReminders()

        // مسح التفضيلات
        context.getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
            .edit().clear().apply()

        // حذف ملف النسخة التلقائية والكاش
        runCatching { File(context.filesDir, "auto_backup.json").delete() }
        runCatching { context.cacheDir.deleteRecursively() }
    }
}
