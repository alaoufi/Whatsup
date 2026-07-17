package com.example.whatsappreminder.util

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

/**
 * نظام حماية بكود تفعيل (Activation / Keygen):
 * - كل جهاز له "رمز جهاز" فريد يُشتق من معرّف الجهاز.
 * - "كود التفعيل" الخاص بالجهاز يُحسب من رمز الجهاز عبر خوارزمية سرّية.
 * - "الكود العالمي" (univ1) يفعّل أي جهاز.
 *
 * ⚠️ مهم: غيّر SECRET و UNIVERSAL_CODE إلى قيمك الخاصة قبل النشر،
 * واحتفظ بهما سرّاً — بهما يُولَّد الكود (نفس القيم في ملف المولّد keygen).
 */
object LicenseManager {

    // السرّ المستخدم في توليد الأكواد (سرّي — نفس القيمة في المولّد)
    private const val SECRET = "WAR-2026-#Alaoufi#-KEY"

    // الكود العالمي: يفعّل أي جهاز دون الحاجة لرمز الجهاز
    const val UNIVERSAL_CODE = "UNIV1-WAR2026"

    /** رمز الجهاز الذي يُعرض للمستخدم (يُرسله للمطوّر للحصول على كوده) */
    fun deviceCode(context: Context): String {
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().takeUnless { it.isNullOrBlank() } ?: "UNKNOWN-DEVICE"
        return format(sha256(androidId))
    }

    /** كود التفعيل الصحيح لهذا الجهاز (نفس ما يُنتجه المولّد) */
    private fun expectedCode(deviceCode: String): String =
        format(sha256(SECRET + deviceCode))

    /** التحقق من كود أدخله المستخدم (يقبل الكود العالمي أو كود الجهاز) */
    fun validate(context: Context, entered: String): Boolean {
        val code = normalize(entered)
        if (code == normalize(UNIVERSAL_CODE)) return true
        return code == normalize(expectedCode(deviceCode(context)))
    }

    private fun normalize(s: String): String =
        s.trim().uppercase().replace(" ", "")

    /** تنسيق: XXXX-XXXX-XXXX من أول 12 حرفاً من الهاش */
    private fun format(hex: String): String {
        val up = hex.uppercase()
        return "${up.substring(0, 4)}-${up.substring(4, 8)}-${up.substring(8, 12)}"
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
