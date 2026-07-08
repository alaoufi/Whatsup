package com.example.whatsappreminder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.whatsappreminder.R
import java.net.URLEncoder

/**
 * أداة فتح واتساب على محادثة رقم معيّن مع نص الرسالة جاهزاً.
 *
 * ⚠️ ملاحظة أمنية:
 * التطبيق يفتح واتساب فقط عبر Intent رسمي — لا إرسال تلقائي ولا AccessibilityService.
 * المستخدم يضغط زر الإرسال داخل واتساب بنفسه.
 */
object WhatsAppOpener {

    // حزم واتساب المعروفة (العادي ثم الأعمال)
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    /**
     * فتح محادثة واتساب على الرقم المحدّد مع نص الرسالة.
     * نُجبر النية على حزمة واتساب مباشرة حتى تُفتح داخل التطبيق لا في المتصفح.
     *
     * @return true إذا نجح فتح واتساب
     */
    fun openChat(context: Context, phoneNumber: String, message: String): Boolean {
        val number = phoneNumber.filter { it.isDigit() }
        val text = URLEncoder.encode(message, "UTF-8")
        val url = "https://api.whatsapp.com/send?phone=$number&text=$text"

        // 1) محاولة الفتح داخل واتساب مباشرة بتحديد الحزمة
        for (pkg in WHATSAPP_PACKAGES) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage(pkg)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                // هذه الحزمة غير مثبّتة — نجرّب التالية
            }
        }

        // 2) محاولة عبر مخطط whatsapp:// دون تحديد حزمة
        try {
            val schemeIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("whatsapp://send?phone=$number&text=$text")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(schemeIntent)
            return true
        } catch (_: Exception) {
        }

        // 3) الطريقة الاحتياطية الأخيرة: رابط wa.me (قد يمرّ عبر المتصفح)
        return try {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/$number?text=$text")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(webIntent)
            true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.error_whatsapp_not_found),
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
