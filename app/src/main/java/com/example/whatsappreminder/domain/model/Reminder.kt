package com.example.whatsappreminder.domain.model

/**
 * نموذج المجال (Domain Model) للتذكير.
 * هذا الكلاس مستقل عن قاعدة البيانات ويستخدم في طبقات الأعمال والواجهة.
 *
 * @property id المعرّف الفريد (0 يعني تذكير جديد لم يُحفظ بعد)
 * @property contactName اسم جهة الاتصال
 * @property phoneNumber رقم الهاتف مع رمز الدولة (مثال: +9665xxxxxxxx)
 * @property message نص الرسالة المراد إرسالها عبر واتساب
 * @property scheduledTime وقت التذكير المجدول بالميلي ثانية (epoch millis)
 * @property status الحالة الحالية للتذكير
 * @property createdAt وقت إنشاء التذكير بالميلي ثانية
 * @property notifiedAt وقت إرسال الإشعار (null إذا لم يُرسل بعد)
 * @property notes ملاحظات إضافية اختيارية
 * @property soundEnabled هل يصدر هذا التذكير صوتاً عند التنبيه؟ (لكل تذكير على حدة)
 * @property openWhatsAppDirectly هل يفتح إشعار هذا التذكير واتساب مباشرة؟ (لكل تذكير)
 */
data class Reminder(
    val id: Long = 0L,
    val contactName: String,
    val phoneNumber: String,
    val message: String,
    val scheduledTime: Long,
    val status: ReminderStatus = ReminderStatus.SCHEDULED,
    val createdAt: Long = System.currentTimeMillis(),
    val notifiedAt: Long? = null,
    val notes: String = "",
    val soundEnabled: Boolean = true,
    val openWhatsAppDirectly: Boolean = true
)

/**
 * حالات التذكير المختلفة.
 */
enum class ReminderStatus {
    SCHEDULED,        // ⏰ تم الجدولة، في انتظار الموعد
    PENDING_NETWORK,  // 📡 الموعد حل لكن لا توجد شبكة
    NOTIFIED,         // 🔔 تم إرسال الإشعار، في انتظار تفاعل المستخدم
    OPENED,           // ✅ المستخدم فتح واتساب
    CANCELLED,        // ❌ المستخدم ألغى التذكير
    EXPIRED           // ⚠️ فات الموعد بفترة طويلة (أكثر من 24 ساعة)
}
