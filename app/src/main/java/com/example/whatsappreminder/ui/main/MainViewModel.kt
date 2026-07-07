package com.example.whatsappreminder.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.usecase.DeleteReminderUseCase
import com.example.whatsappreminder.domain.usecase.ExportRemindersUseCase
import com.example.whatsappreminder.domain.usecase.GetRemindersUseCase
import com.example.whatsappreminder.domain.usecase.ImportRemindersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** حالة واجهة الشاشة الرئيسية */
data class MainUiState(
    val reminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true
)

/** رسائل لمرة واحدة تُعرض للمستخدم (Snackbar/Toast) */
sealed interface MainEvent {
    data class Message(val text: String) : MainEvent
    data class ExportReady(val json: String) : MainEvent
}

/**
 * ViewModel للشاشة الرئيسية.
 * يعرض قائمة التذكيرات عبر StateFlow، ويتيح حذف/تصدير/استيراد التذكيرات.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    getRemindersUseCase: GetRemindersUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase,
    private val exportRemindersUseCase: ExportRemindersUseCase,
    private val importRemindersUseCase: ImportRemindersUseCase
) : ViewModel() {

    // تحويل تدفق التذكيرات إلى حالة واجهة قابلة للمراقبة
    val uiState: StateFlow<MainUiState> = getRemindersUseCase()
        .map { list -> MainUiState(reminders = list, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState()
        )

    // قناة الأحداث لمرة واحدة
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<MainEvent>()
    val events = _events

    /** حذف تذكير مع إلغاء جدولته */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            deleteReminderUseCase(reminder)
        }
    }

    /** تجهيز نص JSON للتصدير ثم إطلاق حدث لحفظه في ملف */
    fun requestExport() {
        viewModelScope.launch {
            val json = exportRemindersUseCase()
            _events.emit(MainEvent.ExportReady(json))
        }
    }

    /** استيراد التذكيرات من نص JSON مقروء من ملف */
    fun importFromJson(json: String) {
        viewModelScope.launch {
            val count = runCatching { importRemindersUseCase(json) }.getOrNull()
            val message = if (count != null) {
                "تم استيراد $count تذكيراً"
            } else {
                "تعذّر قراءة الملف — تأكد أنه ملف نسخ احتياطي صالح"
            }
            _events.emit(MainEvent.Message(message))
        }
    }
}
