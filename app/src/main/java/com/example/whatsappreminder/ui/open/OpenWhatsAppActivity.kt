package com.example.whatsappreminder.ui.open

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.usecase.UpdateReminderUseCase
import com.example.whatsappreminder.util.NotificationHelper
import com.example.whatsappreminder.util.WhatsAppOpener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * شاشة وسيطة شفافة (بلا واجهة) تُفتح عند الضغط على إشعار التذكير.
 * وظيفتها: فتح محادثة واتساب مباشرة بالرسالة الجاهزة، ثم تحديث الحالة إلى OPENED،
 * ثم إغلاق نفسها فوراً دون عرض أي واجهة.
 *
 * ⚠️ لا يوجد أي إرسال تلقائي — المستخدم يضغط زر الإرسال داخل واتساب بنفسه.
 */
@AndroidEntryPoint
class OpenWhatsAppActivity : ComponentActivity() {

    @Inject
    lateinit var updateReminderUseCase: UpdateReminderUseCase

    // نطاق مستقل لتحديث الحالة في الخلفية دون إلغائه عند إغلاق الشاشة الفورية
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        val phoneNumber = intent.getStringExtra(NotificationHelper.EXTRA_PHONE).orEmpty()
        val message = intent.getStringExtra(NotificationHelper.EXTRA_MESSAGE).orEmpty()

        // فتح واتساب مباشرة عبر رابط wa.me الرسمي
        WhatsAppOpener.openChat(this, phoneNumber, message)

        // تحديث الحالة إلى "تم الفتح" في الخلفية
        if (reminderId != -1L) {
            ioScope.launch {
                runCatching {
                    updateReminderUseCase.updateStatus(reminderId, ReminderStatus.OPENED)
                }
            }
        }

        // إغلاق فوري — لا نعرض أي واجهة
        finish()
    }
}
