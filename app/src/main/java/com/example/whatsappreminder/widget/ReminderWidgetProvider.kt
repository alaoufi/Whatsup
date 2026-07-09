package com.example.whatsappreminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.whatsappreminder.R
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.ui.main.MainActivity
import com.example.whatsappreminder.util.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * أداة الشاشة الرئيسية: تعرض حتى 3 تذكيرات قادمة، والضغط عليها يفتح التطبيق.
 */
@AndroidEntryPoint
class ReminderWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var repository: ReminderRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                // أقرب 3 تذكيرات قادمة
                val upcoming = repository.getAllReminders().first()
                    .filter { it.status == ReminderStatus.SCHEDULED && it.scheduledTime > now }
                    .sortedBy { it.scheduledTime }
                    .take(3)
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, upcoming))
                }
            } finally {
                pending.finish()
            }
        }
    }

    /** بناء واجهة الأداة */
    private fun buildViews(context: Context, upcoming: List<Reminder>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_reminders)

        val lineIds = listOf(R.id.widget_line1, R.id.widget_line2, R.id.widget_line3)
        lineIds.forEachIndexed { index, id ->
            val reminder = upcoming.getOrNull(index)
            if (reminder != null) {
                val name = reminder.contactName.ifBlank { reminder.phoneNumber }
                views.setTextViewText(
                    id,
                    "⏰ $name — ${DateFormatter.formatRelative(reminder.scheduledTime)}"
                )
                views.setViewVisibility(id, View.VISIBLE)
            } else {
                views.setViewVisibility(id, View.GONE)
            }
        }
        views.setViewVisibility(
            R.id.widget_empty,
            if (upcoming.isEmpty()) View.VISIBLE else View.GONE
        )

        // الضغط على الأداة يفتح التطبيق
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openIntent)
        return views
    }

    companion object {
        /** طلب تحديث كل نسخ الأداة (يُستدعى بعد أي تغيير في التذكيرات) */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ReminderWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            val intent = Intent(context, ReminderWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
