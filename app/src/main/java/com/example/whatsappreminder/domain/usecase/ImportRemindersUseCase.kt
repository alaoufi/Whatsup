package com.example.whatsappreminder.domain.usecase

import com.example.whatsappreminder.data.backup.ReminderBackup
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.ReminderScheduler
import javax.inject.Inject

/**
 * حالة استخدام: استيراد تذكيرات من نص JSON.
 * تُدرَج كتذكيرات جديدة، ويُعاد جدولة ما كان موعده في المستقبل بحالة SCHEDULED.
 *
 * @return عدد التذكيرات التي تم استيرادها
 */
class ImportRemindersUseCase @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler
) {
    suspend operator fun invoke(json: String): Int {
        val reminders = ReminderBackup.fromJson(json)
        var imported = 0
        reminders.forEach { reminder ->
            val newId = repository.addReminder(reminder)
            imported++
            // نعيد جدولة التذكيرات المستقبلية فقط لتجنّب إشعارات فورية للماضي
            if (reminder.status == ReminderStatus.SCHEDULED &&
                reminder.scheduledTime > System.currentTimeMillis()
            ) {
                scheduler.schedule(reminder.copy(id = newId))
            }
        }
        return imported
    }
}
