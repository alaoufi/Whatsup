package com.example.whatsappreminder.data.local.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * كائن الوصول للبيانات (DAO) لجدول reminders.
 * يستخدم Flow في الاستعلامات المتدفقة كما هو مطلوب.
 */
@Dao
interface ReminderDao {

    /** كل التذكيرات مرتبة حسب الوقت المجدول تصاعدياً */
    @Query("SELECT * FROM reminders ORDER BY scheduled_time ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    /** تذكير واحد كتدفق حسب المعرّف */
    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getReminderById(id: Long): Flow<ReminderEntity?>

    /** تذكير واحد لمرة واحدة (يُستخدم داخل العامل و BootReceiver) */
    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderByIdOnce(id: Long): ReminderEntity?

    /** التذكيرات حسب حالة معينة (لمرة واحدة) */
    @Query("SELECT * FROM reminders WHERE status = :status ORDER BY scheduled_time ASC")
    suspend fun getRemindersByStatus(status: String): List<ReminderEntity>

    /** إدراج تذكير جديد وإرجاع معرّفه المتولّد */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    /** تحديث تذكير كامل */
    @Update
    suspend fun update(reminder: ReminderEntity)

    /** تحديث الحالة ووقت الإشعار فقط */
    @Query("UPDATE reminders SET status = :status, notified_at = :notifiedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, notifiedAt: Long?)

    /** حذف تذكير */
    @Delete
    suspend fun delete(reminder: ReminderEntity)

    /** حذف كل التذكيرات (للمسح الشامل) */
    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
