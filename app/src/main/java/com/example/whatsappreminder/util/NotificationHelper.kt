package com.example.whatsappreminder.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.whatsappreminder.R
import com.example.whatsappreminder.ui.detail.ReminderDetailActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * مساعد الإشعارات: ينشئ قناة الإشعارات ويعرض إشعارات التذكير.
 */
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val CHANNEL_NAME = "تذكيرات واتساب"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    /**
     * إنشاء قناة الإشعارات بأهمية عالية (HIGH) مع الصوت والاهتزاز.
     * آمنة للاستدعاء أكثر من مرة (النظام يتجاهل التكرار).
     */
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات تذكّرك بإرسال رسائل واتساب في مواعيدها"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * عرض إشعار التذكير في موعده.
     *
     * @param reminderId معرّف التذكير (يُمرَّر إلى شاشة التفاصيل)
     * @param contactName اسم جهة الاتصال
     * @param message نص الرسالة (يُعرض كاملاً عبر BigTextStyle)
     */
    fun showReminderNotification(reminderId: Long, contactName: String, message: String) {
        // نية تفتح شاشة التفاصيل عند الضغط على الإشعار
        val intent = Intent(context, ReminderDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text, contactName))
            // عرض الرسالة كاملة عند توسيع الإشعار
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // التحقق من صلاحية الإشعارات قبل العرض لتجنّب استثناء الأمان
        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(context)
                .notify(reminderId.toInt(), notification)
        }
    }

    /**
     * عرض إشعار بأن الموعد قد فات (للحالة EXPIRED).
     */
    fun showExpiredNotification(reminderId: Long, contactName: String) {
        val intent = Intent(context, ReminderDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_expired_title))
            .setContentText(context.getString(R.string.notification_expired_text, contactName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(context)
                .notify(reminderId.toInt(), notification)
        }
    }

    /** التحقق من منح صلاحية POST_NOTIFICATIONS (مطلوبة على Android 13+) */
    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}
