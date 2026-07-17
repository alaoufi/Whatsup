package com.example.whatsappreminder.domain.usecase

import com.example.whatsappreminder.data.backup.ReminderBackup
import com.example.whatsappreminder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * حالة استخدام: تصدير كل التذكيرات إلى نص JSON (نسخة احتياطية).
 */
class ExportRemindersUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    suspend operator fun invoke(): String {
        // نأخذ لقطة واحدة من التذكيرات الحالية
        val reminders = repository.getAllReminders().first()
        return ReminderBackup.toJson(reminders)
    }
}
