package com.cash.guide.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, targetEpochMs ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0")
    suspend fun getAllActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isEnabled = 1 AND isCompleted = 0 AND targetEpochMs <= :currentTime")
    suspend fun getDueReminders(currentTime: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE reminders SET isEnabled = :isEnabled, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateEnabled(id: String, isEnabled: Boolean, updatedAt: Long)

    @Query("UPDATE reminders SET isCompleted = :isCompleted, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Boolean, updatedAt: Long)

    @Query("UPDATE reminders SET targetEpochMs = :nextTargetEpochMs, updatedAtEpochMs = :updatedAt WHERE id = :id")
    suspend fun updateNextOccurrence(id: String, nextTargetEpochMs: Long, updatedAt: Long)
}
