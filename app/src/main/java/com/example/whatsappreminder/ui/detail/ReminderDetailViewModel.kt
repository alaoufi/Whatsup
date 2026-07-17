package com.example.whatsappreminder.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.usecase.DeleteReminderUseCase
import com.example.whatsappreminder.domain.usecase.GetRemindersUseCase
import com.example.whatsappreminder.domain.usecase.UpdateReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** حالة واجهة شاشة التفاصيل */
data class DetailUiState(
    val reminder: Reminder? = null,
    val isDeleted: Boolean = false
)

/**
 * ViewModel لشاشة تفاصيل التذكير: تحميل، تحديث الحالة، إعادة جدولة، حذف.
 */
@HiltViewModel
class ReminderDetailViewModel @Inject constructor(
    private val getRemindersUseCase: GetRemindersUseCase,
    private val updateReminderUseCase: UpdateReminderUseCase,
    private val deleteReminderUseCase: DeleteReminderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    /** تحميل التذكير ومراقبته لحظياً */
    fun load(reminderId: Long) {
        viewModelScope.launch {
            getRemindersUseCase.byId(reminderId).collect { reminder ->
                _uiState.update { it.copy(reminder = reminder) }
            }
        }
    }

    /** تحديث الحالة إلى OPENED عند فتح واتساب */
    fun markAsOpened() {
        val reminder = _uiState.value.reminder ?: return
        viewModelScope.launch {
            updateReminderUseCase.updateStatus(reminder.id, ReminderStatus.OPENED)
        }
    }

    /** إلغاء التذكير */
    fun cancelReminder() {
        val reminder = _uiState.value.reminder ?: return
        viewModelScope.launch {
            updateReminderUseCase.updateStatus(reminder.id, ReminderStatus.CANCELLED)
        }
    }

    /** إعادة جدولة التذكير بوقت جديد */
    fun reschedule(newTime: Long) {
        val reminder = _uiState.value.reminder ?: return
        viewModelScope.launch {
            updateReminderUseCase.reschedule(reminder, newTime)
        }
    }

    /** حذف التذكير */
    fun deleteReminder() {
        val reminder = _uiState.value.reminder ?: return
        viewModelScope.launch {
            deleteReminderUseCase(reminder)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
