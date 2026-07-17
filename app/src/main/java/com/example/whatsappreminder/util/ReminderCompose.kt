package com.example.whatsappreminder.util

import com.example.whatsappreminder.domain.model.Reminder

/**
 * استبدال المتغيّرات في نص الرسالة قبل إرسالها:
 * {الاسم}   → اسم جهة الاتصال (أو الرقم إن لم يوجد اسم)
 * {التاريخ} → موعد التذكير بصيغة نسبية مقروءة
 */
fun Reminder.composeMessage(): String {
    val name = contactName.ifBlank { phoneNumber }
    return message
        .replace("{الاسم}", name)
        .replace("{التاريخ}", DateFormatter.formatRelative(scheduledTime))
}
