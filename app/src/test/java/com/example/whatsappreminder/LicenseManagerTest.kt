package com.example.whatsappreminder

import com.example.whatsappreminder.util.LicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * اختبارات نظام التفعيل (Ed25519).
 *
 * 1) تطابق الخوارزمية مع المولّد: نتحقّق أن منطق [LicenseManager] يقبل أكواداً
 *    وُلّدت بـ keygen.mjs — عبر مفتاح اختبار مُمرَّر (verifyCodeWithPub)، فلا نحتاج
 *    البذرة السرّية لمفتاح الإنتاج (التي تبقى في مولّد المالك فقط).
 *    مفتاح الاختبار = "CHS1..." (البذرة 02ffa2be...) على الجهاز TESTDEVICE234567.
 * 2) مفتاح الإنتاج المضمّن يطابق مفتاح مولّد المالك بالضبط.
 */
class LicenseManagerTest {

    private val device = "TESTDEVICE234567"

    // مفتاح اختبار عامّ (بذرته معروفة للاختبار فقط)
    private val testPub = "CHS1vCGSq2GitGzMNt7d8+VkOWYVQghVOwQDfEMr5Bw="

    // مفتاح الإنتاج الفعلي المضمّن في التطبيق (مطابق لمولّد المالك)
    private val ownerPub = "ucd/BzIBLoU2ol9GVwYeEjoTb7SsbfOgPtNwYls0rI0="

    // أكواد وُلّدت بمفتاح الاختبار (keygen.mjs --seed 02ffa2be... --device TESTDEVICE234567)
    private val permanentCode =
        "AAADYEBZJNFAWRAJ7HABW4A9GBBD7GFV3TALESWKW2Q8JEFPXEAGLDK4EXY5QWL9NBHE749U82KE5DMLKU8SU7YEMRCH37LANYJYS9EJAA"
    private val thirtyDayCode =
        "AARPL7B3QK8KHD59EQ54PSY52Q9XT2ENBL4DU69YUTNU47U4YMN6AWH92Y8EKX9RKW5ED74473EE8NTBH88X9TKJKWEWNJFZNMDKAZRXAW"

    @Test
    fun `algorithm matches generator - permanent code, duration zero`() {
        assertEquals(0, LicenseManager.verifyCodeWithPub(permanentCode, device, testPub))
    }

    @Test
    fun `algorithm matches generator - thirty day code`() {
        assertEquals(30, LicenseManager.verifyCodeWithPub(thirtyDayCode, device, testPub))
    }

    @Test
    fun `dashes and lowercase are ignored`() {
        val pretty = permanentCode.chunked(4).joinToString("-").lowercase()
        assertEquals(0, LicenseManager.verifyCodeWithPub(pretty, device, testPub))
    }

    @Test
    fun `code rejected on a different device`() {
        assertNull(LicenseManager.verifyCodeWithPub(permanentCode, "OTHERDEVICE00000", testPub))
    }

    @Test
    fun `code rejected under a different public key`() {
        // نفس الكود لا يُقبل تحت مفتاح مختلف (مثل مفتاح الإنتاج)
        assertNull(LicenseManager.verifyCodeWithPub(permanentCode, device, ownerPub))
    }

    @Test
    fun `garbage code rejected`() {
        assertNull(LicenseManager.verifyCode("NOT-A-REAL-CODE", device))
    }

    @Test
    fun `embedded production key equals owner generator key`() {
        assertEquals(ownerPub, LicenseManager.publicKeyB64())
    }
}
