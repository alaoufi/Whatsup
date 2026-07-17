package com.example.whatsappreminder.fake

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.util.ReminderScheduler

/**
 * جدولة وهمية تسجّل ما تم جدولته وإلغاؤه للتحقق في الاختبارات.
 */
class FakeReminderScheduler : ReminderScheduler {

    val scheduled = mutableListOf<Reminder>()
    val cancelled = mutableListOf<Long>()

    override fun schedule(reminder: Reminder) {
        scheduled.add(reminder)
    }

    override fun cancel(reminderId: Long) {
        cancelled.add(reminderId)
    }
}
