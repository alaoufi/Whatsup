package com.example.whatsappreminder.domain.usecase

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * حالة استخدام: جلب كل التذكيرات كتدفق.
 */
class GetRemindersUseCase @Inject constructor(
    private val repository: ReminderRepository
) {
    operator fun invoke(): Flow<List<Reminder>> = repository.getAllReminders()

    /** جلب تذكير واحد حسب المعرّف */
    fun byId(id: Long): Flow<Reminder?> = repository.getReminderById(id)
}
