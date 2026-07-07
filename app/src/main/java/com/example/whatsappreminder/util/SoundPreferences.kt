package com.example.whatsappreminder.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.content.edit

/**
 * تفضيلات الصوت: تخزّن نغمة التنبيه التي اختارها المستخدم.
 * تُخزَّن كـ URI نصّي في SharedPreferences.
 *
 * ملاحظة: قنوات الإشعارات على Android 8+ لا يتغيّر صوتها بعد الإنشاء،
 * لذا نحتفظ برقم إصدار للقناة ونرفعه عند تغيير النغمة لإنشاء قناة جديدة.
 */
class SoundPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "reminder_settings"
        private const val KEY_SOUND_URI = "sound_uri"
        private const val KEY_CHANNEL_VERSION = "channel_version"
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

    /** رقم إصدار القناة الحالي */
    fun getChannelVersion(): Int = prefs.getInt(KEY_CHANNEL_VERSION, 0)

    /** رفع رقم إصدار القناة (يُستدعى عند تغيير النغمة) */
    fun bumpChannelVersion() {
        prefs.edit { putInt(KEY_CHANNEL_VERSION, getChannelVersion() + 1) }
    }
}
