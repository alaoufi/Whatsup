package com.example.whatsappreminder.data.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * عامل التذكير: يعمل عند حلول موعد التذكير عبر WorkManager.
 * مُعلّم بـ @HiltWorker لحقن التبعيات (المستودع ومساعد الإشعارات).
 *
 * ⚠️ هذا العامل لا يرسل أي رسالة تلقائياً؛ يكتفي بإظهار إشعار تذكير للمستخدم.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ReminderRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_REMINDER_ID = "key_reminder_id"
        // المدة التي بعدها يُعتبر الموعد فائتاً (24 ساعة)
        const val EXPIRY_THRESHOLD_MILLIS = 24 * 60 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        if (reminderId == -1L) return Result.failure()

        // جلب التذكير من قاعدة البيانات
        val reminder = repository.getReminderByIdOnce(reminderId)
            ?: return Result.success() // حُذف التذكير — لا شيء لفعله

        // إذا كان التذكير قد أُلغي أو فُتح مسبقاً، لا داعي للمتابعة
        if (reminder.status == ReminderStatus.CANCELLED ||
            reminder.status == ReminderStatus.OPENED
        ) {
            return Result.success()
        }

        val now = System.currentTimeMillis()

        // 1) لم يحن الموعد بعد → أعد المحاولة لاحقاً
        if (now < reminder.scheduledTime) {
            return Result.retry()
        }

        // 2) فات الموعد بأكثر من 24 ساعة → EXPIRED
        if (now - reminder.scheduledTime > EXPIRY_THRESHOLD_MILLIS) {
            repository.updateStatus(reminderId, ReminderStatus.EXPIRED)
            notificationHelper.showExpiredNotification(reminderId, reminder.contactName)
            return Result.success()
        }

        // 3) لا توجد شبكة → PENDING_NETWORK وأعد المحاولة
        if (!isNetworkAvailable()) {
            repository.updateStatus(reminderId, ReminderStatus.PENDING_NETWORK)
            return Result.retry()
        }

        // 4) الحالة الطبيعية → أظهر الإشعار وحدّث الحالة إلى NOTIFIED
        notificationHelper.showReminderNotification(
            reminderId = reminderId,
            contactName = reminder.contactName,
            message = reminder.message
        )
        repository.updateStatus(reminderId, ReminderStatus.NOTIFIED, notifiedAt = now)
        return Result.success()
    }

    /** التحقق من توفّر اتصال شبكي فعّال */
    private fun isNetworkAvailable(): Boolean {
        val cm = applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
