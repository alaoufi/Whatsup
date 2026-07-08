package com.example.whatsappreminder.util

/**
 * تطبيع أرقام الهاتف إلى صيغة دولية (أرقام فقط بلا +) يفهمها واتساب.
 *
 * القواعد:
 * - يبدأ بـ +            → دولي جاهز (نأخذ الأرقام فقط).
 * - يبدأ بـ 00           → بادئة دولية، نحذف 00.
 * - يبدأ بـ 0            → رقم محلي، نحذف الصفر ونضيف رمز الدولة.
 * - يبدأ برمز الدولة     → يحتوي الرمز مسبقاً، نتركه.
 * - غير ذلك             → رقم محلي بلا صفر، نضيف رمز الدولة.
 */
object PhoneNumberNormalizer {

    fun normalize(raw: String, countryCode: String): String {
        val trimmed = raw.trim()
        val hasPlus = trimmed.startsWith("+")
        val digits = trimmed.filter { it.isDigit() }
        val code = countryCode.filter { it.isDigit() }

        if (digits.isBlank()) return digits

        return when {
            hasPlus -> digits
            digits.startsWith("00") -> digits.removePrefix("00")
            digits.startsWith("0") -> code + digits.trimStart('0')
            code.isNotBlank() && digits.startsWith(code) -> digits
            else -> code + digits
        }
    }
}
