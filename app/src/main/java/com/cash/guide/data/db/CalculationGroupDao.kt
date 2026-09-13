package com.cash.guide.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationGroupDao {

    @Query("SELECT * FROM calculation_groups ORDER BY createdAtEpochMs ASC")
    fun observeAllGroups(): Flow<List<CalculationGroupEntity>>

    @Query("SELECT * FROM calculation_groups WHERE id = :groupId")
    fun observeGroup(groupId: String): Flow<CalculationGroupEntity?>

    @Query("SELECT * FROM calculation_groups WHERE id = :groupId")
    suspend fun getGroup(groupId: String): CalculationGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: CalculationGroupEntity)

    @Update
    suspend fun updateGroup(group: CalculationGroupEntity)

    @Query("DELETE FROM calculation_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("UPDATE calculations SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroupIdFromCalculations(groupId: String)

    @Query("UPDATE notes SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroupIdFromNotes(groupId: String)

    @Query("UPDATE checklists SET groupId = NULL WHERE groupId = :groupId")
    suspend fun clearGroupIdFromChecklists(groupId: String)

    @Transaction
    suspend fun deleteGroupAndUngroupCalculations(groupId: String) {
        clearGroupIdFromCalculations(groupId)
        clearGroupIdFromNotes(groupId)
        clearGroupIdFromChecklists(groupId)
        deleteGroup(groupId)
    }

    @Query("UPDATE calculations SET groupId = :groupId, updatedAtEpochMs = :now WHERE id = :calculationId")
    suspend fun assignCalculationToGroup(calculationId: String, groupId: String?, now: Long)

    @Query("SELECT * FROM calculation_groups ORDER BY createdAtEpochMs ASC")
    suspend fun getAllGroups(): List<CalculationGroupEntity>

    @Query("DELETE FROM calculation_groups")
    suspend fun deleteAllGroups()

    @Transaction
    suspend fun restoreGroups(
        groups: List<CalculationGroupEntity>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            deleteAllGroups()
        }
        for (group in groups) {
            insertGroup(group)
        }
    }
}
