package com.example.whatsappreminder

import com.example.whatsappreminder.data.backup.ReminderBackup
import com.example.whatsappreminder.domain.model.Reminder
import com.example.whatsappreminder.domain.model.ReminderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات منطق النسخ الاحتياطي (تصدير/استيراد JSON).
 */
class ReminderBackupTest {

    @Test
    fun `round trip preserves reminder fields`() {
        val original = listOf(
            Reminder(
                id = 5,
                contactName = "أحمد",
                phoneNumber = "+966500000000",
                message = "تذكير مهم",
                scheduledTime = 1_700_000_000_000L,
                status = ReminderStatus.NOTIFIED,
                createdAt = 1_699_000_000_000L,
                notifiedAt = 1_700_000_100_000L,
                notes = "ملاحظة"
            )
        )

        val json = ReminderBackup.toJson(original)
        val restored = ReminderBackup.fromJson(json)

        assertEquals(1, restored.size)
        val r = restored.first()
        // يُصفَّر المعرّف عند الاستيراد ليُدرج كتذكير جديد
        assertEquals(0L, r.id)
        assertEquals("أحمد", r.contactName)
        assertEquals("+966500000000", r.phoneNumber)
        assertEquals("تذكير مهم", r.message)
        assertEquals(1_700_000_000_000L, r.scheduledTime)
        assertEquals(ReminderStatus.NOTIFIED, r.status)
        assertEquals(1_700_000_100_000L, r.notifiedAt)
        assertEquals("ملاحظة", r.notes)
    }

    @Test
    fun `null notifiedAt survives round trip`() {
        val original = listOf(
            Reminder(
                contactName = "سارة",
                phoneNumber = "+966511111111",
                message = "رسالة",
                scheduledTime = 123L,
                notifiedAt = null
            )
        )
        val restored = ReminderBackup.fromJson(ReminderBackup.toJson(original))
        assertEquals(null, restored.first().notifiedAt)
    }

    @Test
    fun `invalid json returns empty list`() {
        assertTrue(ReminderBackup.fromJson("{}").isEmpty())
    }
}
