package com.example.whatsappreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * مستقبِل يُعيد جدولة التذكيرات بعد إعادة تشغيل الجهاز.
 * عند إعادة التشغيل يفقد WorkManager الأعمال المجدولة زمنياً، فنعيد جدولتها.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        // نتعامل مع أحداث اكتمال الإقلاع فقط
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        // نستخدم goAsync للسماح بعمل غير متزامن قصير داخل المستقبِل
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                // إعادة جدولة كل التذكيرات التي ما زالت بحالة SCHEDULED
                val scheduled = repository.getRemindersByStatus(ReminderStatus.SCHEDULED)
                scheduled.forEach { reminder ->
                    scheduler.schedule(reminder)
                }
                // كذلك التذكيرات التي كانت تنتظر الشبكة نعيد جدولتها
                val pendingNetwork =
                    repository.getRemindersByStatus(ReminderStatus.PENDING_NETWORK)
                pendingNetwork.forEach { reminder ->
                    scheduler.schedule(reminder)
                }
            } finally {
                // إنهاء البثّ بشكل صحيح
                pendingResult.finish()
            }
        }
    }
}
