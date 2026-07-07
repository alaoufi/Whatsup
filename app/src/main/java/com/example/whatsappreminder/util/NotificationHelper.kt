package com.example.whatsappreminder.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.whatsappreminder.R
import com.example.whatsappreminder.ui.detail.ReminderDetailActivity
import com.example.whatsappreminder.ui.open.OpenWhatsAppActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * مساعد الإشعارات: ينشئ قناة الإشعارات ويعرض إشعارات التذكير.
 */
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID_PREFIX = "reminder_channel_v"
        const val CHANNEL_NAME = "تذكيرات واتساب"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_MESSAGE = "extra_message"
    }

    // تفضيلات الصوت (النغمة المختارة + إصدار القناة)
    private val soundPrefs = SoundPreferences(context)

    /** معرّف القناة الحالي (يتضمّن رقم الإصدار لدعم تغيير النغمة) */
    private fun currentChannelId(): String =
        CHANNEL_ID_PREFIX + soundPrefs.getChannelVersion()

    /**
     * إنشاء قناة الإشعارات بأهمية عالية (HIGH) مع الصوت والاهتزاز.
     * الصوت يُؤخذ من النغمة التي اختارها المستخدم (أو الافتراضية).
     * آمنة للاستدعاء أكثر من مرة (النظام يتجاهل التكرار لنفس المعرّف).
     */
    fun createNotificationChannel() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val channel = NotificationChannel(
            currentChannelId(),
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات تذكّرك بإرسال رسائل واتساب في مواعيدها"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            // تعيين النغمة المختارة على القناة
            setSound(soundPrefs.getSoundUri(), audioAttributes)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * تغيير نغمة التنبيه: نحذف القناة الحالية، نحفظ النغمة الجديدة،
     * نرفع رقم الإصدار، ثم ننشئ قناة جديدة بالنغمة المختارة.
     * (لأن Android لا يسمح بتغيير صوت قناة قائمة.)
     */
    fun updateSound(uri: Uri?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(currentChannelId())
        soundPrefs.setSoundUri(uri)
        soundPrefs.bumpChannelVersion()
        createNotificationChannel()
    }

    /** النغمة المختارة حالياً (لعرضها في منتقي النغمات) */
    fun currentSoundUri(): Uri = soundPrefs.getSoundUri()

    /**
     * عرض إشعار التذكير في موعده.
     * الضغط على الإشعار يفتح واتساب مباشرة (عبر شاشة وسيطة شفافة) بالرسالة الجاهزة.
     *
     * @param reminderId معرّف التذكير
     * @param contactName اسم جهة الاتصال (يُعرض في نص الإشعار)
     * @param phoneNumber رقم الهاتف (لفتح محادثة واتساب الصحيحة)
     * @param message نص الرسالة (يُعرض كاملاً عبر BigTextStyle ويُمرَّر لواتساب)
     */
    fun showReminderNotification(
        reminderId: Long,
        contactName: String,
        phoneNumber: String,
        message: String
    ) {
        // نية تفتح واتساب مباشرة عبر الشاشة الوسيطة الشفافة عند الضغط على الإشعار
        val intent = Intent(context, OpenWhatsAppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_PHONE, phoneNumber)
            putExtra(EXTRA_MESSAGE, message)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, currentChannelId())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text, contactName))
            // عرض الرسالة كاملة عند توسيع الإشعار
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // النغمة (تُستخدم على الإصدارات الأقدم؛ على Android 8+ تحكمها القناة)
            .setSound(soundPrefs.getSoundUri())
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

        val notification = NotificationCompat.Builder(context, currentChannelId())
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
