package com.example.whatsappreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.NotificationHelper
import com.example.whatsappreminder.util.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * مستقبِل يعمل بعد تشغيل الجهاز:
 * - التذكيرات المستقبلية: يعيد جدولتها في AlarmManager.
 * - التذكيرات التي فات موعدها أثناء إطفاء الجهاز: يعرض إشعار "رسالة فائتة"
 *   يسأل المستخدم إن كان يرغب بإرسالها الآن.
 *
 * ملاحظة: بعض أجهزة هواوي تمنع أحداث الإقلاع ما لم يُفعَّل "التشغيل التلقائي"
 * للتطبيق، لذا يوجد أيضاً فحص داخل التطبيق عند فتحه كشبكة أمان.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                // التأكد من وجود قنوات الإشعارات
                notificationHelper.createNotificationChannel()

                val now = System.currentTimeMillis()
                // كل التذكيرات التي ما زالت في انتظار موعدها
                val pending = repository.getRemindersByStatus(ReminderStatus.SCHEDULED) +
                    repository.getRemindersByStatus(ReminderStatus.PENDING_NETWORK)

                pending.forEach { reminder ->
                    if (reminder.scheduledTime > now) {
                        // موعده في المستقبل → أعد جدولته
                        scheduler.schedule(reminder)
                    } else {
                        // فات موعده أثناء إطفاء الجهاز → إشعار "رسالة فائتة"
                        val title = reminder.contactName.ifBlank { reminder.phoneNumber }
                        notificationHelper.showMissedNotification(
                            reminderId = reminder.id,
                            contactName = title,
                            phoneNumber = reminder.phoneNumber,
                            message = reminder.message,
                            soundEnabled = reminder.soundEnabled
                        )
                        repository.updateStatus(
                            reminder.id, ReminderStatus.NOTIFIED, notifiedAt = now
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
