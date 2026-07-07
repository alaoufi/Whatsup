package com.example.whatsappreminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.receiver.ReminderAlarmReceiver
import com.example.whatsappreminder.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * جدولة التذكيرات عبر AlarmManager باستخدام منبّه دقيق (setAlarmClock).
 *
 * لماذا setAlarmClock؟
 * - أدق وسيلة توقيت في أندرويد: يُطلق في الوقت المحدّد بالضبط.
 * - مُعفى من وضع Doze وقيود توفير الطاقة (يُطلق حتى والجهاز نائم).
 * - لا يتطلب صلاحية SCHEDULE_EXACT_ALARM (بخلاف setExact).
 * هذا يحل مشكلة تأخّر التذكير الذي كان يحدث مع WorkManager.
 */
@Singleton
class AlarmReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminder: Reminder) {
        val triggerAt = reminder.scheduledTime
        // النية التي تُطلق عند حلول الوقت (بثّ إلى مستقبِل المنبّه)
        val operation = alarmPendingIntent(reminder.id)

        // النية التي تُفتح إن ضغط المستخدم أيقونة المنبّه (تفتح التطبيق)
        val showIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
        // setAlarmClock يضمن الدقة حتى في وضع توفير الطاقة
        alarmManager.setAlarmClock(info, operation)
    }

    override fun cancel(reminderId: Long) {
        alarmManager.cancel(alarmPendingIntent(reminderId))
    }

    /** بناء نية البثّ الفريدة لكل تذكير */
    private fun alarmPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            // إجراء فريد لكل تذكير حتى لا تتداخل النوايا
            action = ReminderAlarmReceiver.ACTION_FIRE + reminderId
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
