package com.example.whatsappreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.NotificationHelper
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
                val now = System.currentTimeMillis()

                if (now - reminder.scheduledTime > EXPIRY_THRESHOLD_MILLIS) {
                    // فات الموعد بأكثر من 24 ساعة
                    repository.updateStatus(reminderId, ReminderStatus.EXPIRED)
                    notificationHelper.showExpiredNotification(reminderId, title)
                } else {
                    // الحالة الطبيعية: إشعار في الوقت المحدّد بإعدادات هذا التذكير
                    notificationHelper.showReminderNotification(
                        reminderId = reminderId,
                        contactName = title,
                        phoneNumber = reminder.phoneNumber,
                        message = reminder.message,
                        soundEnabled = reminder.soundEnabled,
                        openDirectly = reminder.openWhatsAppDirectly
                    )
                    repository.updateStatus(reminderId, ReminderStatus.NOTIFIED, notifiedAt = now)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
