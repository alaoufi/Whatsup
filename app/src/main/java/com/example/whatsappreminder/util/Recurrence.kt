package com.example.whatsappreminder.util

import com.example.whatsappreminder.domain.model.RecurrenceType
import java.util.Calendar

/**
 * حساب الموعد التالي لتذكير متكرر.
 */
object Recurrence {

    /**
     * يعيد الموعد التالي بعد [fromMillis] حسب نوع التكرار،
     * ويستمر بالتقديم حتى يصبح الموعد بعد [notBefore] (لتفادي مواعيد فائتة).
     */
    fun next(fromMillis: Long, type: RecurrenceType, notBefore: Long): Long {
        if (type == RecurrenceType.NONE) return fromMillis
        val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
        do {
            when (type) {
                RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RecurrenceType.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
                RecurrenceType.MONTHLY -> cal.add(Calendar.MONTH, 1)
                RecurrenceType.NONE -> return cal.timeInMillis
            }
        } while (cal.timeInMillis <= notBefore)
        return cal.timeInMillis
    }

    /** التسمية العربية لنوع التكرار */
    fun label(type: RecurrenceType): String = when (type) {
        RecurrenceType.NONE -> "بدون تكرار"
        RecurrenceType.DAILY -> "يومي"
        RecurrenceType.WEEKLY -> "أسبوعي"
        RecurrenceType.MONTHLY -> "شهري"
    }
}
