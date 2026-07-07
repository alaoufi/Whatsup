package com.example.whatsappreminder.domain.usecase

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.ReminderScheduler
import javax.inject.Inject

/**
 * حالة استخدام: تحديث تذكير موجود.
 * تحدّث السجل في المستودع، وتعيد الجدولة أو تلغيها حسب الحالة الجديدة.
 */
class UpdateReminderUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) {
    /** تحديث كامل لكائن التذكير مع إعادة الجدولة إن كان مجدولاً */
    suspend operator fun invoke(reminder: Reminder) {
        repository.updateReminder(reminder)
        when (reminder.status) {
            // إن أصبح مجدولاً من جديد، أعد جدولة العامل
            ReminderStatus.SCHEDULED -> scheduler.schedule(reminder)
            // إن أُلغي، ألغِ العامل المرتبط
            ReminderStatus.CANCELLED -> scheduler.cancel(reminder.id)
            else -> Unit
        }
    }

    /** تحديث الحالة فقط */
    suspend fun updateStatus(id: Long, status: ReminderStatus, notifiedAt: Long? = null) {
        repository.updateStatus(id, status, notifiedAt)
        if (status == ReminderStatus.CANCELLED) {
            scheduler.cancel(id)
        }
    }

    /** إعادة جدولة تذكير بوقت جديد (يُعيد الحالة إلى SCHEDULED) */
    suspend fun reschedule(reminder: Reminder, newTime: Long) {
        val updated = reminder.copy(
            scheduledTime = newTime,
            status = ReminderStatus.SCHEDULED,
            notifiedAt = null
        )
        repository.updateReminder(updated)
        scheduler.schedule(updated)
    }
}
