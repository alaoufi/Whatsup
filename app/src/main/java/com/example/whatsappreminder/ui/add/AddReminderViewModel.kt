package com.example.whatsappreminder.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.usecase.AddReminderUseCase
import com.example.whatsappreminder.domain.usecase.GetRemindersUseCase
import com.example.whatsappreminder.domain.usecase.UpdateReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** حالة واجهة شاشة الإضافة (الحقول + أخطاء التحقق) */
data class AddReminderUiState(
    val contactName: String = "",
    val phoneNumber: String = "",
    val message: String = "",
    val notes: String = "",
    val scheduledTime: Long? = null,
    val soundEnabled: Boolean = true,
    val openWhatsAppDirectly: Boolean = true,
    val contactNameError: String? = null,
    val phoneError: String? = null,
    val messageError: String? = null,
    val timeError: String? = null,
    val isSaved: Boolean = false,
    val isEditing: Boolean = false
) {
    // عدد أحرف الرسالة (للعدّاد)
    val messageLength: Int get() = message.length
}

/**
 * ViewModel لشاشة إضافة/تعديل تذكير: يدير الحقول ويتحقق من صحتها قبل الحفظ.
 */
@HiltViewModel
class AddReminderViewModel @Inject constructor(
    private val addReminderUseCase: AddReminderUseCase,
    private val getRemindersUseCase: GetRemindersUseCase,
    private val updateReminderUseCase: UpdateReminderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddReminderUiState())
    val uiState: StateFlow<AddReminderUiState> = _uiState.asStateFlow()

    // معرّف التذكير عند التعديل (0 = إضافة جديدة)
    private var editingId: Long = 0L
    // حقول التذكير الأصلي التي لا تُعدّل من الواجهة (وقت الإنشاء مثلاً)
    private var originalCreatedAt: Long = System.currentTimeMillis()

    /** تحميل تذكير موجود لتعديله وملء الحقول */
    fun loadForEdit(id: Long) {
        if (id <= 0L || editingId == id) return
        viewModelScope.launch {
            val reminder = getRemindersUseCase.byId(id).first() ?: return@launch
            editingId = reminder.id
            originalCreatedAt = reminder.createdAt
            _uiState.update {
                it.copy(
                    contactName = reminder.contactName,
                    phoneNumber = reminder.phoneNumber,
                    message = reminder.message,
                    notes = reminder.notes,
                    scheduledTime = reminder.scheduledTime,
                    soundEnabled = reminder.soundEnabled,
                    openWhatsAppDirectly = reminder.openWhatsAppDirectly,
                    isEditing = true
                )
            }
        }
    }

    fun onContactNameChange(value: String) =
        _uiState.update { it.copy(contactName = value, contactNameError = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(phoneNumber = value, phoneError = null) }

    fun onMessageChange(value: String) =
        _uiState.update { it.copy(message = value, messageError = null) }

    fun onNotesChange(value: String) =
        _uiState.update { it.copy(notes = value) }

    fun onTimeSelected(millis: Long) =
        _uiState.update { it.copy(scheduledTime = millis, timeError = null) }

    fun onSoundEnabledChange(value: Boolean) =
        _uiState.update { it.copy(soundEnabled = value) }

    fun onOpenDirectlyChange(value: Boolean) =
        _uiState.update { it.copy(openWhatsAppDirectly = value) }

    /**
     * التحقق من صحة المدخلات ثم حفظ التذكير.
     * يضبط isSaved=true عند النجاح لتغلق الشاشة نفسها.
     */
    fun save() {
        val state = _uiState.value
        var valid = true

        // ملاحظة: الاسم لم يعد مطلوباً — يُلتقط تلقائياً من جهات الاتصال إن وُجد.

        // رقم الهاتف: أرقام فقط بطول معقول (يُسمح بـ + في البداية)
        val digits = state.phoneNumber.filter { it.isDigit() }
        val phoneError = when {
            state.phoneNumber.isBlank() -> { valid = false; "أدخل رقم الهاتف" }
            digits.length < 8 -> { valid = false; "رقم غير صالح (أدخل الرقم مع رمز الدولة)" }
            else -> null
        }

        // الرسالة مطلوبة
        val messageError = if (state.message.isBlank()) {
            valid = false; "أدخل نص الرسالة"
        } else null

        // الوقت مطلوب ويجب أن يكون في المستقبل
        val timeError = when {
            state.scheduledTime == null -> { valid = false; "اختر التاريخ والوقت" }
            state.scheduledTime <= System.currentTimeMillis() -> {
                valid = false; "يجب أن يكون الوقت في المستقبل"
            }
            else -> null
        }

        _uiState.update {
            it.copy(
                phoneError = phoneError,
                messageError = messageError,
                timeError = timeError
            )
        }

        if (!valid) return

        // حفظ التذكير (إضافة جديدة أو تحديث الموجود)
        viewModelScope.launch {
            val reminder = Reminder(
                id = editingId,
                contactName = state.contactName.trim(),
                phoneNumber = state.phoneNumber.trim(),
                message = state.message.trim(),
                scheduledTime = state.scheduledTime!!,
                status = ReminderStatus.SCHEDULED,
                createdAt = if (editingId > 0L) originalCreatedAt else System.currentTimeMillis(),
                notes = state.notes.trim(),
                soundEnabled = state.soundEnabled,
                openWhatsAppDirectly = state.openWhatsAppDirectly
            )
            if (editingId > 0L) {
                // تحديث: يعيد الجدولة بالوقت الجديد لأن الحالة SCHEDULED
                updateReminderUseCase(reminder)
            } else {
                addReminderUseCase(reminder)
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
