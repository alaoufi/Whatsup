package com.example.whatsappreminder

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.whatsappreminder.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * كلاس التطبيق الرئيسي.
 * - مُعلّم بـ @HiltAndroidApp لتفعيل حقن التبعيات في كامل التطبيق.
 * - ينفّذ Configuration.Provider ليزوّد WorkManager بمصنع العمال (HiltWorkerFactory)
 *   بحيث يمكن حقن التبعيات داخل ReminderWorker.
 */
@HiltAndroidApp
class WhatsAppReminderApp : Application(), Configuration.Provider {

    // مصنع العمال المحقون عبر Hilt (يُنشئ العمال مع تبعياتهم)
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // إنشاء قناة الإشعارات مبكراً عند تشغيل التطبيق
        NotificationHelper(this).createNotificationChannel()
    }

    // تزويد إعدادات WorkManager بمصنع Hilt بدلاً من التهيئة الافتراضية
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
