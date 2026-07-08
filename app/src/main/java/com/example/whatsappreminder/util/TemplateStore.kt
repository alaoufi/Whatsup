package com.example.whatsappreminder.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * تخزين وقراءة قوالب الرسائل المخصّصة للمستخدم.
 * إن لم يحفظ المستخدم قوالب، تُستخدم القوالب الافتراضية.
 */
object TemplateStore {

    fun load(context: Context): List<MessageTemplates.Template> {
        val json = SettingsPreferences(context).getTemplatesJson()
            ?: return MessageTemplates.ALL
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MessageTemplates.Template(o.optString("title"), o.optString("body"))
            }
        }.getOrDefault(MessageTemplates.ALL)
    }

    fun save(context: Context, templates: List<MessageTemplates.Template>) {
        val arr = JSONArray()
        templates.forEach { t ->
            arr.put(JSONObject().put("title", t.title).put("body", t.body))
        }
        SettingsPreferences(context).setTemplatesJson(arr.toString())
    }
}
