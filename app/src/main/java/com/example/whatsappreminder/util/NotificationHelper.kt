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
 * مساعد الإشعارات: ينشئ قنوات الإشعارات ويعرض إشعارات التذكير.
 * الصوت وسلوك فتح واتساب أصبحا إعدادات لكل تذكير على حدة.
 */
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val SOUND_CHANNEL_PREFIX = "reminder_sound_v"
        const val SILENT_CHANNEL_ID = "reminder_silent"
        const val CHANNEL_NAME = "تذكيرات واتساب (بصوت)"
        const val SILENT_CHANNEL_NAME = "تذكيرات واتساب (صامت)"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_MESSAGE = "extra_message"
    }

    // تفضيلات التطبيق (النغمة المختارة + إصدار القناة). الصوت/السلوك أصبحا لكل تذكير.
    private val settings = SettingsPreferences(context)

    /** معرّف قناة الصوت الحالية (يتضمّن رقم الإصدار لدعم تغيير النغمة) */
    private fun soundChannelId(): String =
        SOUND_CHANNEL_PREFIX + settings.getChannelVersion()

    /**
     * إنشاء قناتَي الإشعارات:
     * - قناة بصوت (بالنغمة المختارة) للتذكيرات التي فُعِّل صوتها.
     * - قناة صامتة للتذكيرات المكتومة (تكتفي بالإشعار والاهتزاز).
     * كل تذكير يُوجَّه للقناة المناسبة حسب إعداده الخاص.
     */
    fun createNotificationChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val soundChannel = NotificationChannel(
            soundChannelId(),
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات تذكّرك بإرسال رسائل واتساب — بصوت"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            setSound(settings.getSoundUri(), audioAttributes)
        }

        val silentChannel = NotificationChannel(
            SILENT_CHANNEL_ID,
            SILENT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "إشعارات تذكّرك بإرسال رسائل واتساب — صامتة"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            setSound(null, null)
        }

        manager.createNotificationChannel(soundChannel)
        manager.createNotificationChannel(silentChannel)
    }

    /**
     * تغيير نغمة التنبيه: نحذف قناة الصوت الحالية، نحفظ النغمة الجديدة،
     * نرفع رقم الإصدار، ثم نعيد إنشاء القنوات بالنغمة المختارة.
     * (لأن Android لا يسمح بتغيير صوت قناة قائمة.)
     */
    fun updateSound(uri: Uri?) {
        context.getSystemService(NotificationManager::class.java)
            .deleteNotificationChannel(soundChannelId())
        settings.setSoundUri(uri)
        settings.bumpChannelVersion()
        createNotificationChannel()
    }

    /** النغمة المختارة حالياً (لعرضها في منتقي النغمات) */
    fun currentSoundUri(): Uri = settings.getSoundUri()

    /**
     * عرض إشعار التذكير في موعده — بإعدادات خاصة بهذا التذكير.
     *
     * @param soundEnabled هل يصدر هذا التذكير صوتاً؟ (يحدّد القناة)
     * @param openDirectly هل يفتح الإشعار واتساب مباشرة أم شاشة التفاصيل؟
     */
    fun showReminderNotification(
        reminderId: Long,
        contactName: String,
        phoneNumber: String,
        message: String,
        soundEnabled: Boolean,
        openDirectly: Boolean
    ) {
        // حسب إعداد هذا التذكير: يفتح واتساب مباشرة، أو يفتح شاشة التفاصيل (إشعار فقط)
        val intent = if (openDirectly) {
            Intent(context, OpenWhatsAppActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_PHONE, phoneNumber)
                putExtra(EXTRA_MESSAGE, message)
            }
        } else {
            Intent(context, ReminderDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_REMINDER_ID, reminderId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // اختيار القناة حسب إعداد الصوت لهذا التذكير
        val channelId = if (soundEnabled) soundChannelId() else SILENT_CHANNEL_ID

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text, contactName))
            // عرض الرسالة كاملة عند توسيع الإشعار
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (openDirectly) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // عند "يفتح واتساب مباشرة": إشعار ملء الشاشة يفتح واتساب تلقائياً
        // (خاصة عند قفل الشاشة) دون حاجة لضغط الإشعار.
        if (openDirectly) {
            builder.setFullScreenIntent(pendingIntent, true)
        }

        if (soundEnabled) builder.setSound(settings.getSoundUri()) else builder.setSilent(true)

        // التحقق من صلاحية الإشعارات قبل العرض لتجنّب استثناء الأمان
        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(context)
                .notify(reminderId.toInt(), builder.build())
        }
    }

    /**
     * عرض إشعار "رسالة فائتة" بعد تشغيل الجهاز:
     * يخبر المستخدم بوجود تذكير فات موعده، والضغط عليه يفتح واتساب لإرسال الرسالة.
     */
    fun showMissedNotification(
        reminderId: Long,
        contactName: String,
        phoneNumber: String,
        message: String,
        soundEnabled: Boolean
    ) {
        // الضغط على الإشعار يفتح واتساب مباشرة بالرسالة الجاهزة
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

        val channelId = if (soundEnabled) soundChannelId() else SILENT_CHANNEL_ID

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_missed_title))
            .setContentText(context.getString(R.string.notification_missed_text, contactName))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (soundEnabled) builder.setSound(settings.getSoundUri()) else builder.setSilent(true)

        if (hasNotificationPermission()) {
            NotificationManagerCompat.from(context)
                .notify(reminderId.toInt(), builder.build())
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

        val notification = NotificationCompat.Builder(context, SILENT_CHANNEL_ID)
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
