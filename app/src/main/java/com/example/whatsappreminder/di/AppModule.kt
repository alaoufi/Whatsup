package com.example.whatsappreminder.di

import android.content.Context
import androidx.room.Room
import com.example.whatsappreminder.data.local.database.AppDatabase
import com.example.whatsappreminder.data.local.database.ReminderDao
import com.example.whatsappreminder.data.local.repository.ReminderRepositoryImpl
import com.example.whatsappreminder.domain.repository.ReminderRepository
import com.example.whatsappreminder.util.ReminderScheduler
import com.example.whatsappreminder.util.WorkManagerReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * وحدة Hilt الرئيسية: توفّر قاعدة البيانات، الـ DAO، والمستودع.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** توفير نسخة واحدة (Singleton) من قاعدة البيانات */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

    /** توفير الـ DAO من قاعدة البيانات */
    @Provides
    @Singleton
    fun provideReminderDao(database: AppDatabase): ReminderDao =
        database.reminderDao()

    /** ربط واجهة المستودع بتنفيذها الفعلي */
    @Provides
    @Singleton
    fun provideReminderRepository(dao: ReminderDao): ReminderRepository =
        ReminderRepositoryImpl(dao)

    /** ربط واجهة الجدولة بتنفيذها المعتمد على WorkManager */
    @Provides
    @Singleton
    fun provideReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler =
        impl
}
