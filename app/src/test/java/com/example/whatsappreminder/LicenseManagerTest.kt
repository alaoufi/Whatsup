package com.example.whatsappreminder

import com.example.whatsappreminder.util.LicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * اختبارات نظام التفعيل (Ed25519 — نظام UNI3).
 *
 * الأكواد أدناه هي **المتّجهات الرسمية** من مولّد المالك (المعادلة_FORMULA.md)،
 * موقّعة بالمفتاح العامّ المضمّن في التطبيق (W5Kc...) والبادئة UNI3، للجهاز
 * JGNKT87QXZ4AZVBE. نجاحها يثبت أن التطبيق يقبل أكواد مولّد المالك الحقيقية.
 */
class LicenseManagerTest {

    private val device = "JGNKT87QXZ4AZVBE"
    private val ownerPub = "W5Kc9hRB7lb9xSh/VqdR4T8GT6VaDznEwYQgXZpLZz0="

    // كود دائم (duration=0) للجهاز أعلاه
    private val permanentCode =
        "AAAANNHY8FJL25Z5WYXNLWFL868FYST9C9XR94ANECKZLSL9LCYHKDT9UP57TF8NUC62S26JAM8UATFLQP6BSR7LN8HXHNHT85QZ7ATTBE"

    // كود 30 يوماً (duration=30) لنفس الجهاز
    private val thirtyDayCode =
        "AARLCPHN6DH2X3QYEAUFRAD3PF8A5PDUPBXQ7LV8PH93VD8P7DPZTGDSS64ZJKULLSF6SGY3GTG93BY23DKPSUSXACSDKP6RXRE5QB7XB2"

    @Test
    fun `owner permanent code verifies with duration zero`() {
        assertEquals(0, LicenseManager.verifyCode(permanentCode, device))
    }

    @Test
    fun `owner thirty day code verifies with duration 30`() {
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

    @Test
    fun `embedded production key equals owner generator key`() {
        assertEquals(ownerPub, LicenseManager.publicKeyB64())
    }
}
