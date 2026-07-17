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
        private const val KEY_TEMPLATES = "templates_json"
        private const val KEY_COUNTRY_CODE = "country_code"
        private const val KEY_HIDE_PREVIEW = "hide_preview"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        // رمز افتراضي (السعودية) — يمكن للمستخدم تغييره
        private const val DEFAULT_COUNTRY_CODE = "966"
    }

    // ملاحظة: التفعيل انتقل إلى LicenseManager (Ed25519، تخزين خاص "app_guard").

    /** رمز الدولة الافتراضي (أرقام فقط، بلا +) يُضاف للأرقام المحلية */
    fun getDefaultCountryCode(): String =
        prefs.getString(KEY_COUNTRY_CODE, DEFAULT_COUNTRY_CODE) ?: DEFAULT_COUNTRY_CODE

    fun setDefaultCountryCode(code: String) {
        val digits = code.filter { it.isDigit() }
        prefs.edit { putString(KEY_COUNTRY_CODE, digits.ifBlank { DEFAULT_COUNTRY_CODE }) }
    }

    /** إخفاء نص الرسالة من الإشعار (يظهر الاسم فقط) */
    fun isHidePreview(): Boolean = prefs.getBoolean(KEY_HIDE_PREVIEW, false)

    fun setHidePreview(hide: Boolean) {
        prefs.edit { putBoolean(KEY_HIDE_PREVIEW, hide) }
    }

    /** وضع السمة: 0=تلقائي (النظام)، 1=فاتح، 2=داكن */
    fun getThemeMode(): Int = prefs.getInt(KEY_THEME_MODE, 0)

    fun setThemeMode(mode: Int) {
        prefs.edit { putInt(KEY_THEME_MODE, mode.coerceIn(0, 2)) }
    }

    /** قفل التطبيق بالبصمة */
    fun isBiometricLock(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_LOCK, false)

    fun setBiometricLock(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_BIOMETRIC_LOCK, enabled) }
    }

    /** قوالب الرسائل المخصّصة (JSON) — null يعني استخدام الافتراضية */
    fun getTemplatesJson(): String? = prefs.getString(KEY_TEMPLATES, null)

    fun setTemplatesJson(json: String) {
        prefs.edit { putString(KEY_TEMPLATES, json) }
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
