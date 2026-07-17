package com.example.whatsappreminder.domain.usecase

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.ReminderScheduler
import javax.inject.Inject

/**
 * حالة استخدام: إضافة تذكير جديد.
 * تحفظ التذكير في المستودع ثم تجدوله عبر WorkManager.
 */
class AddReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) {
    /**
     * @return معرّف التذكير المتولّد
     */
    suspend operator fun invoke(reminder: Reminder): Long {
        // حفظ التذكير في قاعدة البيانات والحصول على معرّفه
        val id = repository.addReminder(reminder)
        // جدولة العامل ليعمل عند وقت التذكير
        scheduler.schedule(reminder.copy(id = id))
        return id
    }
}
