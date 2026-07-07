package com.example.whatsappreminder.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * أدوات تنسيق التاريخ والوقت للعرض في الواجهة.
 */
object DateFormatter {

    private val fullFormat = SimpleDateFormat("EEEE، d MMMM yyyy - hh:mm a", Locale("ar"))
    private val shortFormat = SimpleDateFormat("d MMM yyyy، hh:mm a", Locale("ar"))

    /** تنسيق كامل (يوم الأسبوع + التاريخ + الوقت) */
    fun formatFull(millis: Long): String = fullFormat.format(Date(millis))

    /** تنسيق مختصر للعرض داخل البطاقات */
    fun formatShort(millis: Long): String = shortFormat.format(Date(millis))
}
