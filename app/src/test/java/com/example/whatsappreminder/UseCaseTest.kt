package com.example.whatsappreminder

import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import com.example.whatsappreminder.domain.usecase.AddReminderUseCase
import com.example.whatsappreminder.domain.usecase.DeleteReminderUseCase
import com.example.whatsappreminder.domain.usecase.GetRemindersUseCase
import com.example.whatsappreminder.domain.usecase.ImportRemindersUseCase
import com.example.whatsappreminder.data.backup.ReminderBackup
import com.example.whatsappreminder.fake.FakeReminderRepository
import com.example.whatsappreminder.fake.FakeReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * اختبارات حالات الاستخدام مع المستودع والجدولة الوهميين.
 */
class UseCaseTest {

    private lateinit var repository: FakeReminderRepository
    private lateinit var scheduler: FakeReminderScheduler

    @Before
    fun setup() {
        repository = FakeReminderRepository()
        scheduler = FakeReminderScheduler()
    }

    private fun futureReminder() = Reminder(
        contactName = "أحمد",
        phoneNumber = "+966500000000",
        message = "مرحبا",
        scheduledTime = System.currentTimeMillis() + 3_600_000L,
        status = ReminderStatus.SCHEDULED
    )

    @Test
    fun `add reminder saves and schedules`() = runTest {
        val useCase = AddReminderUseCase(repository, scheduler)
        val id = useCase(futureReminder())

        assertEquals(1, repository.getAllReminders().first().size)
        assertEquals(1, scheduler.scheduled.size)
        assertEquals(id, scheduler.scheduled.first().id)
    }

    @Test
    fun `delete reminder removes and cancels`() = runTest {
        val add = AddReminderUseCase(repository, scheduler)
        val id = add(futureReminder())
        val saved = repository.getReminderByIdOnce(id)!!

        val delete = DeleteReminderUseCase(repository, scheduler)
        delete(saved)

        assertTrue(repository.getAllReminders().first().isEmpty())
        assertTrue(scheduler.cancelled.contains(id))
    }

    @Test
    fun `get reminders emits current list`() = runTest {
        AddReminderUseCase(repository, scheduler)(futureReminder())
        val get = GetRemindersUseCase(repository)
        assertEquals(1, get().first().size)
    }

    @Test
    fun `import inserts all and schedules future scheduled ones`() = runTest {
        val json = ReminderBackup.toJson(
            listOf(
                futureReminder(),
                // تذكير ماضٍ لا يجب إعادة جدولته
                futureReminder().copy(
                    scheduledTime = System.currentTimeMillis() - 1000L
                )
            )
        )

        val import = ImportRemindersUseCase(repository, scheduler)
        val count = import(json)

        assertEquals(2, count)
        assertEquals(2, repository.getAllReminders().first().size)
        // فقط التذكير المستقبلي يُجدول
        assertEquals(1, scheduler.scheduled.size)
    }
}
