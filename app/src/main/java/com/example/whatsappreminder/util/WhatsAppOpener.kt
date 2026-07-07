package com.example.whatsappreminder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.whatsappreminder.R
import java.net.URLEncoder

/**
 * أداة فتح واتساب عبر رابط رسمي (wa.me deep link) فقط.
 *
 * ⚠️ ملاحظة أمنية مهمة:
 * هذا التطبيق "تذكير" فقط. لا يوجد أي أتمتة ولا AccessibilityService.
 * المستخدم هو من يضغط زر الإرسال داخل واتساب بنفسه.
 * نكتفي بفتح واتساب على المحادثة الصحيحة مع نص الرسالة مُعبّأ مسبقاً.
 */
object WhatsAppOpener {

    /**
     * فتح واتساب على محادثة الرقم المحدّد مع نص الرسالة.
     *
     * @param phoneNumber رقم الهاتف مع رمز الدولة (تُزال منه الرموز غير الرقمية)
     * @param message نص الرسالة (يُرمّز URL-encoding)
     * @return true إذا نجح فتح واتساب، false إذا لم يُعثر على تطبيق مناسب
     */
    fun openChat(context: Context, phoneNumber: String, message: String): Boolean {
        // تنظيف الرقم: إبقاء الأرقام فقط (إزالة +، مسافات، شرطات...)
        val sanitizedNumber = phoneNumber.filter { it.isDigit() }
        // ترميز الرسالة لتكون صالحة داخل عنوان URL
        val encodedMessage = URLEncoder.encode(message, "UTF-8")

        // رابط wa.me الرسمي — الطريقة الوحيدة المسموح بها لفتح واتساب
        val url = "https://wa.me/$sanitizedNumber?text=$encodedMessage"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // في حال عدم توفّر تطبيق يفتح الرابط
            Toast.makeText(
                context,
                context.getString(R.string.error_whatsapp_not_found),
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
