package com.example.whatsappreminder

import com.example.whatsappreminder.domain.model.RecurrenceType
import com.example.whatsappreminder.util.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * اختبارات حساب مواعيد التكرار.
 */
class RecurrenceTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 10): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `daily advances one day`() {
        val from = at(2026, Calendar.JULY, 10)
        val next = Recurrence.next(from, RecurrenceType.DAILY, notBefore = from)
        assertEquals(at(2026, Calendar.JULY, 11), next)
    }

    @Test
    fun `none returns null`() {
        assertNull(Recurrence.next(123L, RecurrenceType.NONE, notBefore = 0L))
    }

    @Test
    fun `custom days with empty mask returns null`() {
        assertNull(
            Recurrence.next(123L, RecurrenceType.CUSTOM_DAYS, notBefore = 0L, daysMask = 0)
        )
    }

    @Test
    fun `custom days lands on selected weekday`() {
        val from = at(2026, Calendar.JULY, 10) // اختر يوم بداية
        // قناع يوم الأحد فقط (بت 0)
        val next = Recurrence.next(
            from, RecurrenceType.CUSTOM_DAYS, notBefore = from, daysMask = 1
        )
        val cal = Calendar.getInstance().apply { timeInMillis = next!! }
        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertTrue(next!! > from)
    }

    @Test
    fun `end date stops recurrence`() {
        val from = at(2026, Calendar.JULY, 10)
        val end = at(2026, Calendar.JULY, 10, 23) // ينتهي في نفس اليوم
        assertNull(
            Recurrence.next(from, RecurrenceType.DAILY, notBefore = from, endMillis = end)
        )
    }

    @Test
    fun `weekly advances seven days past notBefore`() {
        val from = at(2026, Calendar.JULY, 1)
        val notBefore = at(2026, Calendar.JULY, 20)
        val next = Recurrence.next(from, RecurrenceType.WEEKLY, notBefore = notBefore)
        assertTrue(next!! > notBefore)
    }
}
