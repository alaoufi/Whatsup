package com.example.whatsappreminder.ui

import androidx.compose.ui.graphics.Color

/** تصنيف تذكير: اسم ولون */
data class ReminderCategory(val name: String, val color: Color)

/**
 * التصنيفات المتاحة (ثابتة). الاسم الفارغ = بلا تصنيف.
 */
object Categories {
    val NONE = ReminderCategory("", Color.Unspecified)

    val ALL = listOf(
        NONE,
        ReminderCategory("عمل", Color(0xFF1976D2)),
        ReminderCategory("عائلة", Color(0xFF2E7D32)),
        ReminderCategory("عملاء", Color(0xFFF9A825)),
        ReminderCategory("شخصي", Color(0xFF7B1FA2)),
        ReminderCategory("مهم", Color(0xFFC62828))
    )

    /** لون التصنيف حسب اسمه (null إن كان بلا تصنيف أو غير معروف) */
    fun colorFor(name: String): Color? =
        ALL.firstOrNull { it.name == name && it.name.isNotBlank() }?.color
}
