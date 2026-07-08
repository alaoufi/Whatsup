package com.example.whatsappreminder.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus

/**
 * كيان Room يمثّل صفاً في جدول reminders.
 * الحالة تُخزّن كنص (اسم الـ enum) لسهولة القراءة والترحيل.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "contact_name")
    val contactName: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "notified_at")
    val notifiedAt: Long? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "sound_enabled", defaultValue = "1")
    val soundEnabled: Boolean = true,

    @ColumnInfo(name = "open_whatsapp_directly", defaultValue = "1")
    val openWhatsAppDirectly: Boolean = true
)

/**
 * تحويل الكيان إلى نموذج المجال.
 * إذا كانت قيمة الحالة غير معروفة، نعود افتراضياً إلى SCHEDULED.
 */
fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    contactName = contactName,
    phoneNumber = phoneNumber,
    message = message,
    scheduledTime = scheduledTime,
    status = runCatching { ReminderStatus.valueOf(status) }
        .getOrDefault(ReminderStatus.SCHEDULED),
    createdAt = createdAt,
    notifiedAt = notifiedAt,
    notes = notes,
    soundEnabled = soundEnabled,
    openWhatsAppDirectly = openWhatsAppDirectly
)

/**
 * تحويل نموذج المجال إلى كيان قابل للتخزين.
 */
fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    contactName = contactName,
    phoneNumber = phoneNumber,
    message = message,
    scheduledTime = scheduledTime,
    status = status.name,
    createdAt = createdAt,
    notifiedAt = notifiedAt,
    notes = notes,
    soundEnabled = soundEnabled,
    openWhatsAppDirectly = openWhatsAppDirectly
)
