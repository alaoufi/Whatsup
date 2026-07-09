package com.example.whatsappreminder.data.local.repository

import android.content.Context
import com.example.whatsappreminder.data.local.database.ReminderDao
import com.example.whatsappreminder.data.local.database.toDomain
import com.example.whatsappreminder.data.local.database.toEntity
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.widget.ReminderWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * التنفيذ الفعلي للمستودع باستخدام Room DAO.
 * يحوّل بين الكيانات (Entities) ونماذج المجال (Domain Models)،
 * ويحدّث أداة الشاشة الرئيسية بعد كل تغيير.
 */
@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    @ApplicationContext private val context: Context
) : ReminderRepository {

    override fun getAllReminders(): Flow<List<Reminder>> =
        dao.getAllReminders().map { list -> list.map { it.toDomain() } }

    override fun getReminderById(id: Long): Flow<Reminder?> =
        dao.getReminderById(id).map { it?.toDomain() }

    override suspend fun getReminderByIdOnce(id: Long): Reminder? =
        dao.getReminderByIdOnce(id)?.toDomain()

    override suspend fun getRemindersByStatus(status: ReminderStatus): List<Reminder> =
        dao.getRemindersByStatus(status.name).map { it.toDomain() }

    override suspend fun addReminder(reminder: Reminder): Long =
        dao.insert(reminder.toEntity()).also { refreshWidget() }

    override suspend fun updateReminder(reminder: Reminder) =
        dao.update(reminder.toEntity()).also { refreshWidget() }

    override suspend fun updateStatus(id: Long, status: ReminderStatus, notifiedAt: Long?) =
        dao.updateStatus(id, status.name, notifiedAt).also { refreshWidget() }

    override suspend fun deleteReminder(reminder: Reminder) =
        dao.delete(reminder.toEntity()).also { refreshWidget() }

    /** تحديث أداة الشاشة الرئيسية بعد أي تغيير في البيانات */
    private fun refreshWidget() {
        runCatching { ReminderWidgetProvider.refresh(context) }
    }
}
