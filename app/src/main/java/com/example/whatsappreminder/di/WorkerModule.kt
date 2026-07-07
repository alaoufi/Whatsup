package com.example.whatsappreminder.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * وحدة Hilt الخاصة بعمّال WorkManager.
 *
 * ملاحظة: مصنع العمال (HiltWorkerFactory) يُوفَّر تلقائياً بواسطة امتداد
 * androidx.hilt:hilt-work، ويُحقن مباشرة في كلاس التطبيق WhatsAppReminderApp
 * الذي ينفّذ Configuration.Provider. لذلك لا نحتاج لتوفيره يدوياً هنا.
 *
 * نُبقي هذه الوحدة كنقطة توسّع مستقبلية لأي تبعيات خاصة بالعمّال
 * (مثل مزوّدات إضافية يحتاجها ReminderWorker).
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    // لا توجد مزوّدات إضافية حالياً — HiltWorkerFactory يُدار تلقائياً.
}
