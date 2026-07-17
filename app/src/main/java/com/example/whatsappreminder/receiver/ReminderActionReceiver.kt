package com.example.whatsappreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
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
 * يعالج أزرار إشعار التذكير: "تأجيل ساعة" و"تم".
 * (زر "فتح واتساب" يفتح شاشة مباشرة ولا يمر من هنا.)
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: ReminderRepository

    @Inject
    lateinit var scheduler: ReminderScheduler

    companion object {
        const val ACTION_SNOOZE = "com.example.whatsappreminder.action.SNOOZE"
        const val ACTION_DONE = "com.example.whatsappreminder.action.DONE"
        const val EXTRA_ID = "extra_action_id"
        // مدة التأجيل بالدقائق (تُمرَّر مع النية؛ الافتراضي ساعة)
        const val EXTRA_MINUTES = "extra_snooze_minutes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id == -1L) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = repository.getReminderByIdOnce(id) ?: return@launch
                when (intent.action) {
                    ACTION_SNOOZE -> {
                        // تأجيل بالمدة المطلوبة: موعد جديد وإعادة الجدولة
                        val minutes = intent.getIntExtra(EXTRA_MINUTES, 60)
                        val newTime = System.currentTimeMillis() + minutes * 60_000L
                        val updated = reminder.copy(
                            scheduledTime = newTime,
                            status = ReminderStatus.SCHEDULED,
                            notifiedAt = null
                        )
                        repository.updateReminder(updated)
                        scheduler.schedule(updated)
                    }
                    ACTION_DONE -> {
                        // تم الإرسال: نعتبره منتهياً
                        repository.updateStatus(id, ReminderStatus.OPENED)
                    }
                }
                // إخفاء الإشعار بعد التفاعل
                NotificationManagerCompat.from(context).cancel(id.toInt())
            } finally {
                pending.finish()
            }
        }
    }
}
