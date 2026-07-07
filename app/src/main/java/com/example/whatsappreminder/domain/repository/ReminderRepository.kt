package com.example.whatsappreminder.domain.repository

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import kotlinx.coroutines.flow.Flow

/**
 * واجهة المستودع (Repository) لطبقة المجال.
 * تعرّف العمليات المتاحة على التذكيرات دون الاعتماد على تفاصيل قاعدة البيانات.
 */
interface ReminderRepository {

    /** الحصول على كل التذكيرات كتدفق (Flow) يتحدث تلقائياً عند أي تغيير */
    fun getAllReminders(): Flow<List<Reminder>>

    /** الحصول على تذكير واحد كتدفق حسب معرّفه */
    fun getReminderById(id: Long): Flow<Reminder?>

    /** الحصول على تذكير واحد لمرة واحدة (غير متدفق) - يُستخدم داخل العامل */
    suspend fun getReminderByIdOnce(id: Long): Reminder?

    /** الحصول على التذكيرات حسب حالة معينة (لمرة واحدة) - يُستخدم في BootReceiver */
    suspend fun getRemindersByStatus(status: ReminderStatus): List<Reminder>

    /** إضافة تذكير جديد وإرجاع معرّفه المتولّد */
    suspend fun addReminder(reminder: Reminder): Long

    /** تحديث تذكير موجود */
    suspend fun updateReminder(reminder: Reminder)

    /** تحديث حالة تذكير فقط */
    suspend fun updateStatus(id: Long, status: ReminderStatus, notifiedAt: Long? = null)

    /** حذف تذكير */
    suspend fun deleteReminder(reminder: Reminder)
}
