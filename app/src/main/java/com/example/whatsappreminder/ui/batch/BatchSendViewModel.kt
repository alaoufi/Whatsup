package com.example.whatsappreminder.ui.batch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.usecase.GetRemindersUseCase
import com.example.whatsappreminder.domain.usecase.UpdateReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel لشاشة الإرسال الجماعي: تعرض مجموعة تذكيرات (نفس الموعد)
 * ليرسل المستخدم لكل مستلم واحداً تلو الآخر.
 */
@HiltViewModel
class BatchSendViewModel @Inject constructor(
    getRemindersUseCase: GetRemindersUseCase,
    private val updateReminderUseCase: UpdateReminderUseCase
) : ViewModel() {

    private val idsFlow = MutableStateFlow<List<Long>>(emptyList())

    /** تذكيرات المجموعة مرتبة: غير المُرسلة أولاً */
    val reminders: StateFlow<List<Reminder>> =
        combine(getRemindersUseCase(), idsFlow) { all, ids ->
            all.filter { it.id in ids }
                .sortedBy { if (it.status == ReminderStatus.OPENED) 1 else 0 }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setIds(ids: List<Long>) {
        idsFlow.value = ids
    }

    /** وسم تذكير بأنه فُتح (أُرسل) */
    fun markOpened(id: Long) {
        viewModelScope.launch {
            updateReminderUseCase.updateStatus(id, ReminderStatus.OPENED)
        }
    }
}
