package com.example.whatsappreminder

import com.example.whatsappreminder.data.local.database.toDomain
import com.example.whatsappreminder.data.local.database.toEntity
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * اختبارات تحويل النموذج بين المجال (Domain) وكيان Room.
 */
class ReminderMappingTest {

    @Test
    fun `domain to entity and back preserves values`() {
        val reminder = Reminder(
            id = 10,
            contactName = "خالد",
            phoneNumber = "+966522222222",
            message = "اجتماع",
            scheduledTime = 999L,
            status = ReminderStatus.EXPIRED,
            createdAt = 111L,
            notifiedAt = 222L,
            notes = "غرفة 3"
        )

        val restored = reminder.toEntity().toDomain()
        assertEquals(reminder, restored)
    }

    @Test
    fun `unknown status name falls back to SCHEDULED`() {
        val entity = Reminder(
            contactName = "ن",
            phoneNumber = "1",
            message = "م",
            scheduledTime = 1L
        ).toEntity().copy(status = "GARBAGE_VALUE")

        assertEquals(ReminderStatus.SCHEDULED, entity.toDomain().status)
    }
}
