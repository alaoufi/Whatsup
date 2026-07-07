package com.example.whatsappreminder.util

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.whatsappreminder.data.worker.ReminderWorker
import com.example.whatsappreminder.domain.model.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مسؤول عن جدولة وإلغاء أعمال التذكير عبر WorkManager.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // بادئة اسم العمل الفريد لكل تذكير
        const val WORK_NAME_PREFIX = "reminder_work_"

        /** توليد اسم عمل فريد لكل تذكير */
        fun workName(reminderId: Long): String = "$WORK_NAME_PREFIX$reminderId"
    }

    /**
     * جدولة عامل التذكير.
     * - يحسب التأخير الأولي بالفرق بين وقت التذكير والوقت الحالي.
     * - يشترط وجود اتصال بالشبكة، ولا يشترط ألا تكون البطارية منخفضة.
     * - يستخدم enqueueUniqueWork مع REPLACE ليستبدل أي عمل سابق لنفس التذكير.
     */
    fun schedule(reminder: Reminder) {
        // الفرق الزمني حتى الموعد (لا يقل عن صفر لتفادي القيم السالبة)
        val delay = (reminder.scheduledTime - System.currentTimeMillis()).coerceAtLeast(0L)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        // تمرير معرّف التذكير إلى العامل عبر Data
        val inputData = Data.Builder()
            .putLong(ReminderWorker.KEY_REMINDER_ID, reminder.id)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            // سياسة إعادة المحاولة الأسّية عند إرجاع retry
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(WORK_NAME_PREFIX + reminder.id)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(reminder.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** إلغاء عامل التذكير المرتبط بمعرّف معيّن */
    fun cancel(reminderId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(reminderId))
    }
}
