package com.example.whatsappreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.whatsappreminder.domain.model.RecurrenceType
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.ui.open.OpenWhatsAppActivity
import com.example.whatsappreminder.util.NotificationHelper
import com.example.whatsappreminder.util.Recurrence
import com.example.whatsappreminder.util.ReminderScheduler
import com.example.whatsappreminder.util.composeMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * مستقبِل يُطلق عند حلول موعد التذكير (عبر AlarmManager).
 * يعرض إشعار التذكير مباشرة في الوقت المحدّد — دون أي إرسال تلقائي.
 */
@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var scheduler: ReminderScheduler

    companion object {
        const val ACTION_FIRE = "com.example.whatsappreminder.ALARM_FIRE_"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        // المدة التي بعدها يُعتبر الموعد فائتاً (24 ساعة)
        const val EXPIRY_THRESHOLD_MILLIS = 24 * 60 * 60 * 1000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        // نسمح بعمل غير متزامن قصير داخل المستقبِل
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = repository.getReminderByIdOnce(reminderId) ?: return@launch

                // تجاهل إن كان أُلغي أو فُتح مسبقاً
                if (reminder.status == ReminderStatus.CANCELLED ||
                    reminder.status == ReminderStatus.OPENED
                ) return@launch

                // عنوان الإشعار: الاسم إن وُجد، وإلا الرقم
                val title = reminder.contactName.ifBlank { reminder.phoneNumber }
                // نص الرسالة بعد استبدال المتغيّرات ({الاسم}/{التاريخ})
                val composed = reminder.composeMessage()
                val now = System.currentTimeMillis()

                if (reminder.recurrence == RecurrenceType.NONE &&
                    now - reminder.scheduledTime > EXPIRY_THRESHOLD_MILLIS
                ) {
                    // فات الموعد بأكثر من 24 ساعة (لغير المتكرر)
                    repository.updateStatus(reminderId, ReminderStatus.EXPIRED)
                    notificationHelper.showExpiredNotification(reminderId, title)
                } else {
                    // الحالة الطبيعية: إشعار في الوقت المحدّد بإعدادات هذا التذكير
                    // (إشعار ملء الشاشة يفتح واتساب تلقائياً عند القفل)
                    notificationHelper.showReminderNotification(
                        reminderId = reminderId,
                        contactName = title,
                        phoneNumber = reminder.phoneNumber,
                        message = composed,
                        soundEnabled = reminder.soundEnabled,
                        openDirectly = reminder.openWhatsAppDirectly
                    )

                    // متكرر: جدولة الموعد التالي (null = انتهى التكرار أو غير متكرر)
                    val next = Recurrence.next(
                        fromMillis = reminder.scheduledTime,
                        type = reminder.recurrence,
                        notBefore = now,
                        daysMask = reminder.recurrenceDays,
                        endMillis = reminder.recurrenceEnd
                    )
                    if (next != null) {
                        val nextReminder = reminder.copy(
                            scheduledTime = next,
                            status = ReminderStatus.SCHEDULED,
                            notifiedAt = now
                        )
                        repository.updateReminder(nextReminder)
                        scheduler.schedule(nextReminder)
                    } else {
                        repository.updateStatus(reminderId, ReminderStatus.NOTIFIED, notifiedAt = now)
                    }

                    // فتح تلقائي فوري لواتساب دون ضغط الإشعار — يتطلب صلاحية
                    // "العرض فوق التطبيقات الأخرى" للسماح بالتشغيل من الخلفية.
                    if (reminder.openWhatsAppDirectly && Settings.canDrawOverlays(context)) {
                        val openIntent = Intent(context, OpenWhatsAppActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
                            putExtra(NotificationHelper.EXTRA_PHONE, reminder.phoneNumber)
                            putExtra(NotificationHelper.EXTRA_MESSAGE, composed)
                        }
                        runCatching { context.startActivity(openIntent) }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
