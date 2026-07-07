# قواعد ProGuard/R8

# الإبقاء على التعليقات التوضيحية (مطلوبة لـ Hilt/Room)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# نماذج المجال وكيانات Room (تُستخدم عبر الانعكاس أحياناً)
-keep class com.example.whatsappreminder.domain.model.** { *; }
-keep class com.example.whatsappreminder.data.local.database.** { *; }

# الإبقاء على أسماء ثوابت enum (نستخدم valueOf/name للحالة)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Hilt و Room و WorkManager تُوفّر قواعد consumer خاصة بها تلقائياً.
