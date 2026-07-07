package com.example.whatsappreminder.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.usecase.DeleteReminderUseCase
import com.example.whatsappreminder.domain.usecase.GetRemindersUseCase
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

/**
 * ViewModel للشاشة الرئيسية.
 * يعرض قائمة التذكيرات عبر StateFlow، ويتيح حذف تذكير.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    getRemindersUseCase: GetRemindersUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase
) : ViewModel() {

    // تحويل تدفق التذكيرات إلى حالة واجهة قابلة للمراقبة
    val uiState: StateFlow<MainUiState> = getRemindersUseCase()
        .map { list -> MainUiState(reminders = list, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState()
        )

    /** حذف تذكير مع إلغاء جدولته */
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            deleteReminderUseCase(reminder)
        }
    }
}
