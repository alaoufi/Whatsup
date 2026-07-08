package com.example.whatsappreminder.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.edit

/**
 * تفضيلات التطبيق العامة (تُخزَّن في SharedPreferences):
 * - نغمة التنبيه المختارة + رقم إصدار القناة.
 * - تفعيل/كتم صوت التنبيه.
 * - سلوك الإشعار: يفتح واتساب مباشرة أم إشعار فقط.
 *
 * ملاحظة: قنوات الإشعارات على Android 8+ لا يتغيّر صوتها بعد الإنشاء،
 * لذا نحتفظ برقم إصدار للقناة ونرفعه عند تغيير النغمة أو حالة الصوت.
 */
class SettingsPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "reminder_settings"
        private const val KEY_SOUND_URI = "sound_uri"
        private const val KEY_CHANNEL_VERSION = "channel_version"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_OPEN_WHATSAPP_DIRECTLY = "open_whatsapp_directly"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_COUNTRY_CODE = "country_code"
        // رمز افتراضي (السعودية) — يمكن للمستخدم تغييره
        private const val DEFAULT_COUNTRY_CODE = "966"
    }

    /** رمز الدولة الافتراضي (أرقام فقط، بلا +) يُضاف للأرقام المحلية */
    fun getDefaultCountryCode(): String =
        prefs.getString(KEY_COUNTRY_CODE, DEFAULT_COUNTRY_CODE) ?: DEFAULT_COUNTRY_CODE

    fun setDefaultCountryCode(code: String) {
        val digits = code.filter { it.isDigit() }
        prefs.edit { putString(KEY_COUNTRY_CODE, digits.ifBlank { DEFAULT_COUNTRY_CODE }) }
    }

    /** هل عُرضت شاشة الترحيب/الأذونات من قبل؟ */
    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDED, false)

    fun setOnboardingDone() {
        prefs.edit { putBoolean(KEY_ONBOARDED, true) }
    }

    /** نغمة التنبيه المختارة، أو نغمة الإشعار الافتراضية للنظام إن لم يُختَر شيء */
    fun getSoundUri(): Uri {
        val stored = prefs.getString(KEY_SOUND_URI, null)
        return if (stored != null) Uri.parse(stored)
        else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    /** حفظ نغمة جديدة (null يعيد للنغمة الافتراضية) */
    fun setSoundUri(uri: Uri?) {
        prefs.edit {
            if (uri == null) remove(KEY_SOUND_URI) else putString(KEY_SOUND_URI, uri.toString())
        }
    }

    /** هل صوت التنبيه مفعّل؟ (افتراضياً نعم) */
    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND_ENABLED, enabled) }
    }

    /** هل يفتح الإشعار واتساب مباشرة؟ (افتراضياً نعم) */
    fun isOpenWhatsAppDirectly(): Boolean = prefs.getBoolean(KEY_OPEN_WHATSAPP_DIRECTLY, true)

    fun setOpenWhatsAppDirectly(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_OPEN_WHATSAPP_DIRECTLY, enabled) }
    }

    /** رقم إصدار القناة الحالي */
    fun getChannelVersion(): Int = prefs.getInt(KEY_CHANNEL_VERSION, 0)

    /** رفع رقم إصدار القناة (يُستدعى عند تغيير النغمة أو حالة الصوت) */
    fun bumpChannelVersion() {
        prefs.edit { putInt(KEY_CHANNEL_VERSION, getChannelVersion() + 1) }
    }
}
