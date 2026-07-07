// ملف الـ Gradle على مستوى المشروع (Project level)
// يعرّف الإضافات (Plugins) المستخدمة في المشروع دون تطبيقها هنا
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}
