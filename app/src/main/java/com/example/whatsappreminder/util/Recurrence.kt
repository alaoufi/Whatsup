package com.example.whatsappreminder.util

import com.example.whatsappreminder.domain.model.RecurrenceType
import java.util.Calendar

/**
 * حساب الموعد التالي لتذكير متكرر.
 */
object Recurrence {

    /** أسماء أيام الأسبوع بالترتيب (بت 0=الأحد ... بت 6=السبت) */
    val DAY_NAMES = listOf("أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")

    /** هل اليوم (0=أحد..6=سبت) مفعّل في القناع؟ */
    fun isDaySet(mask: Int, day: Int): Boolean = (mask shr day) and 1 == 1

    /** تبديل يوم في القناع */
    fun toggleDay(mask: Int, day: Int): Int = mask xor (1 shl day)

    /**
     * يعيد الموعد التالي بعد [fromMillis] حسب نوع التكرار،
     * ويستمر بالتقديم حتى يصبح الموعد بعد [notBefore].
     * يعيد null إذا تجاوز الموعد تاريخ الانتهاء [endMillis] أو كان القناع فارغاً.
     */
    fun next(
        fromMillis: Long,
        type: RecurrenceType,
        notBefore: Long,
        daysMask: Int = 0,
        endMillis: Long? = null
    ): Long? {
        if (type == RecurrenceType.NONE) return null
        if (type == RecurrenceType.CUSTOM_DAYS && daysMask == 0) return null

        val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
        // حد أقصى للدوران حمايةً من أي حالة شاذة
        var guard = 0
        do {
            when (type) {
                RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RecurrenceType.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
                RecurrenceType.MONTHLY -> cal.add(Calendar.MONTH, 1)
                RecurrenceType.CUSTOM_DAYS -> {
                    // نتقدّم يوماً بيوم حتى نصادف يوماً مفعّلاً في القناع
                    do {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        val day = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0..6
                        if (isDaySet(daysMask, day)) break
                    } while (++guard < 4000)
                }
                RecurrenceType.NONE -> return null
            }
        } while (cal.timeInMillis <= notBefore && ++guard < 4000)

        val next = cal.timeInMillis
        // انتهى التكرار إن تجاوزنا تاريخ الانتهاء
        if (endMillis != null && next > endMillis) return null
        return next
    }

    /** التسمية العربية لنوع التكرار */
    fun label(type: RecurrenceType): String = when (type) {
        RecurrenceType.NONE -> "بدون تكرار"
        RecurrenceType.DAILY -> "يومي"
        RecurrenceType.WEEKLY -> "أسبوعي"
        RecurrenceType.MONTHLY -> "شهري"
        RecurrenceType.CUSTOM_DAYS -> "أيام محددة"
    }
}
