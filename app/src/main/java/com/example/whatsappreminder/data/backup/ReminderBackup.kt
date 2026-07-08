package com.example.whatsappreminder.data.backup

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * مسؤول عن تحويل قائمة التذكيرات إلى نص JSON والعكس (نسخ احتياطي/استعادة).
 * لا يعتمد على أي إطار خارجي، يستخدم org.json المدمج.
 */
object ReminderBackup {

    private const val KEY_VERSION = "version"
    private const val KEY_REMINDERS = "reminders"
    private const val CURRENT_VERSION = 1

    /** تحويل قائمة تذكيرات إلى نص JSON منسّق */
    fun toJson(reminders: List<Reminder>): String {
        val array = JSONArray()
        reminders.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("contactName", r.contactName)
                put("phoneNumber", r.phoneNumber)
                put("message", r.message)
                put("scheduledTime", r.scheduledTime)
                put("status", r.status.name)
                put("createdAt", r.createdAt)
                // القيمة الفارغة تُخزّن كـ JSONObject.NULL
                put("notifiedAt", r.notifiedAt ?: JSONObject.NULL)
                put("notes", r.notes)
                put("soundEnabled", r.soundEnabled)
                put("openWhatsAppDirectly", r.openWhatsAppDirectly)
            }
            array.put(obj)
        }
        return JSONObject().apply {
            put(KEY_VERSION, CURRENT_VERSION)
            put(KEY_REMINDERS, array)
        }.toString(2)
    }

    /**
     * قراءة قائمة التذكيرات من نص JSON.
     * يعيد قائمة فارغة إذا كان النص غير صالح.
     * تُصفَّر المعرّفات (id = 0) حتى تُدرَج كتذكيرات جديدة عند الاستيراد.
     */
    fun fromJson(json: String): List<Reminder> {
        val result = mutableListOf<Reminder>()
        val root = JSONObject(json)
        val array = root.optJSONArray(KEY_REMINDERS) ?: return emptyList()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val status = runCatching {
                ReminderStatus.valueOf(obj.optString("status", ReminderStatus.SCHEDULED.name))
            }.getOrDefault(ReminderStatus.SCHEDULED)

            result.add(
                Reminder(
                    id = 0L, // معرّف جديد عند الاستيراد
                    contactName = obj.optString("contactName", ""),
                    phoneNumber = obj.optString("phoneNumber", ""),
                    message = obj.optString("message", ""),
                    scheduledTime = obj.optLong("scheduledTime", 0L),
                    status = status,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    notifiedAt = if (obj.isNull("notifiedAt")) null else obj.optLong("notifiedAt"),
                    notes = obj.optString("notes", ""),
                    soundEnabled = obj.optBoolean("soundEnabled", true),
                    openWhatsAppDirectly = obj.optBoolean("openWhatsAppDirectly", true)
                )
            )
        }
        return result
    }
}
