package com.example.whatsappreminder

import com.example.whatsappreminder.util.LicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * يتحقّق أن منطق التحقّق في [LicenseManager] مطابق تماماً للمولّد keygen.mjs:
 * الأكواد أدناه وُلّدت بالبذرة السرّية التي مفتاحُها العامّ مضمَّن في التطبيق
 * (PUB_B64 = "CHS1vCGSq2GitGzMNt7d8+VkOWYVQghVOwQDfEMr5Bw=")، للجهاز TESTDEVICE234567.
 */
class LicenseManagerTest {

    private val device = "TESTDEVICE234567"

    // node keygen.mjs code --seed <SEED> --device TESTDEVICE234567 --days 0
    private val permanentCode =
        "AAADYEBZJNFAWRAJ7HABW4A9GBBD7GFV3TALESWKW2Q8JEFPXEAGLDK4EXY5QWL9NBHE749U82KE5DMLKU8SU7YEMRCH37LANYJYS9EJAA"

    // node keygen.mjs code --seed <SEED> --device TESTDEVICE234567 --days 30
    private val thirtyDayCode =
        "AARPL7B3QK8KHD59EQ54PSY52Q9XT2ENBL4DU69YUTNU47U4YMN6AWH92Y8EKX9RKW5ED74473EE8NTBH88X9TKJKWEWNJFZNMDKAZRXAW"

    @Test
    fun `permanent code verifies with duration zero`() {
        assertEquals(0, LicenseManager.verifyCode(permanentCode, device))
    }

    @Test
    fun `thirty day code verifies with duration 30`() {
        assertEquals(30, LicenseManager.verifyCode(thirtyDayCode, device))
    }

    @Test
    fun `code accepted regardless of dashes and case`() {
        val pretty = permanentCode.chunked(4).joinToString("-").lowercase()
        assertEquals(0, LicenseManager.verifyCode(pretty, device))
    }

    @Test
    fun `code rejected on a different device`() {
        assertNull(LicenseManager.verifyCode(permanentCode, "OTHERDEVICE00000"))
    }

    @Test
    fun `garbage code rejected`() {
        assertNull(LicenseManager.verifyCode("NOT-A-REAL-CODE", device))
    }
}
