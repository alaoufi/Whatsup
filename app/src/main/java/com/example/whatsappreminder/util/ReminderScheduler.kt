package com.example.whatsappreminder.util

import com.example.whatsappreminder.domain.model.Reminder

/**
 * واجهة جدولة التذكيرات.
 * تجريدها كواجهة يفصل طبقة الأعمال عن WorkManager ويُسهّل الاختبار (fakes).
 */
interface ReminderScheduler {
    /** جدولة تذكير ليعمل في وقته المحدّد */
    fun schedule(reminder: Reminder)

    /** إلغاء جدولة تذكير حسب معرّفه */
    fun cancel(reminderId: Long)
}
