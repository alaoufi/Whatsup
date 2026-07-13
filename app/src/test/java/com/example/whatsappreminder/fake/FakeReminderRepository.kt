package com.example.whatsappreminder.fake

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * مستودع وهمي في الذاكرة لاستخدامه في اختبارات الوحدة.
 */
class FakeReminderRepository : ReminderRepository {

    private val state = MutableStateFlow<List<Reminder>>(emptyList())
    private var nextId = 1L

    override fun getAllReminders(): Flow<List<Reminder>> = state

    override fun getReminderById(id: Long): Flow<Reminder?> =
        state.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getReminderByIdOnce(id: Long): Reminder? =
        state.value.firstOrNull { it.id == id }

    override suspend fun getRemindersByStatus(status: ReminderStatus): List<Reminder> =
        state.value.filter { it.status == status }

    override suspend fun addReminder(reminder: Reminder): Long {
        val id = nextId++
        state.value = state.value + reminder.copy(id = id)
        return id
    }

    override suspend fun updateReminder(reminder: Reminder) {
        state.value = state.value.map { if (it.id == reminder.id) reminder else it }
    }

    override suspend fun updateStatus(id: Long, status: ReminderStatus, notifiedAt: Long?) {
        state.value = state.value.map {
            if (it.id == id) it.copy(status = status, notifiedAt = notifiedAt) else it
        }
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        state.value = state.value.filterNot { it.id == reminder.id }
    }

    override suspend fun deleteAllReminders() {
        state.value = emptyList()
    }
}
