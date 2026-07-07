package com.example.whatsappreminder.domain.usecase

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.ReminderScheduler
import javax.inject.Inject

/**
 * حالة استخدام: حذف تذكير.
 * تحذف السجل من المستودع وتلغي أي عامل مجدول مرتبط به.
 */
class DeleteReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) {
    suspend operator fun invoke(reminder: Reminder) {
        // إلغاء العامل المجدول أولاً حتى لا يعمل بعد الحذف
        scheduler.cancel(reminder.id)
        repository.deleteReminder(reminder)
    }
}
