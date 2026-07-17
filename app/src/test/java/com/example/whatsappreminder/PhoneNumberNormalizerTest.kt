package com.example.whatsappreminder

import com.example.whatsappreminder.util.PhoneNumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * اختبارات تطبيع أرقام الهاتف إلى الصيغة الدولية.
 */
class PhoneNumberNormalizerTest {

    private val code = "966"

    @Test
    fun `local number with leading zero gets country code`() {
        assertEquals("966501234567", PhoneNumberNormalizer.normalize("0501234567", code))
    }

    @Test
    fun `plus international is kept as digits`() {
        assertEquals("966501234567", PhoneNumberNormalizer.normalize("+966501234567", code))
    }

    @Test
    fun `double zero international prefix is stripped`() {
        assertEquals("966501234567", PhoneNumberNormalizer.normalize("00966501234567", code))
    }

    @Test
    fun `number already starting with country code is unchanged`() {
        assertEquals("966501234567", PhoneNumberNormalizer.normalize("966501234567", code))
    }

    @Test
    fun `local number without zero gets country code`() {
        assertEquals("966501234567", PhoneNumberNormalizer.normalize("501234567", code))
    }

    @Test
    fun `spaces and dashes are ignored`() {
        assertEquals("966501234567", PhoneNumberNormalizer.normalize("+966 50-123 4567", code))
    }
}
