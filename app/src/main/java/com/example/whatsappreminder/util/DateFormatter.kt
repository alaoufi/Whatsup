package com.example.whatsappreminder.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * أدوات تنسيق التاريخ والوقت للعرض في الواجهة.
 */
object DateFormatter {

    private val arabic = Locale("ar")
    private val fullFormat = SimpleDateFormat("EEEE، d MMMM yyyy - hh:mm a", arabic)
    private val shortFormat = SimpleDateFormat("d MMM yyyy، hh:mm a", arabic)
    private val timeFormat = SimpleDateFormat("hh:mm a", arabic)
    private val dayMonthFormat = SimpleDateFormat("EEEE d MMM، hh:mm a", arabic)

    /** تنسيق كامل (يوم الأسبوع + التاريخ + الوقت) */
    fun formatFull(millis: Long): String = fullFormat.format(Date(millis))

    /** تنسيق مختصر للعرض داخل البطاقات */
    fun formatShort(millis: Long): String = shortFormat.format(Date(millis))

    /**
     * تنسيق نسبي مقروء: "اليوم/غداً/أمس + الوقت"، وإلا اليوم والشهر أو التاريخ الكامل.
     */
    fun formatRelative(millis: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        val diffDays = dayIndex(target) - dayIndex(now)
        val time = timeFormat.format(Date(millis))
        return when (diffDays) {
            0L -> "اليوم $time"
            1L -> "غداً $time"
            -1L -> "أمس $time"
            in 2L..6L -> dayMonthFormat.format(Date(millis))
            else -> shortFormat.format(Date(millis))
        }
    }

    /** رقم اليوم المطلق (منذ الحقبة) لمقارنة الأيام بغض النظر عن الوقت */
    private fun dayIndex(cal: Calendar): Long {
        val days = cal.get(Calendar.YEAR) * 366L + cal.get(Calendar.DAY_OF_YEAR)
        return days
    }
}
