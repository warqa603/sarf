package com.cash.guide.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {

    @Transaction
    @Query("SELECT * FROM checklists ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ChecklistWithItems>>

    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :id")
    fun observeChecklist(id: String): Flow<ChecklistWithItems?>

    @Transaction
    @Query("SELECT * FROM checklists WHERE id = :id")
    suspend fun getChecklist(id: String): ChecklistWithItems?

    @Transaction
    @Query("SELECT * FROM checklists ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestChecklist(): ChecklistWithItems?

    @Transaction
    @Query("SELECT * FROM checklists ORDER BY updatedAtEpochMs DESC")
    suspend fun getAll(): List<ChecklistWithItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklist(checklist: ChecklistEntity)

    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)

    @Delete
    suspend fun deleteChecklist(checklist: ChecklistEntity)

    @Query("DELETE FROM checklists WHERE id = :id")
    suspend fun deleteChecklistById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Transaction
    suspend fun insertChecklistWithItems(checklist: ChecklistEntity, items: List<ChecklistItemEntity>) {
        insertChecklist(checklist)
        if (items.isNotEmpty()) {
            insertItems(items)
        }
    }

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Delete
    suspend fun deleteItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :itemId")
    suspend fun deleteItemById(itemId: String)

    @Query("UPDATE checklist_items SET isChecked = :isChecked WHERE id = :itemId")
    suspend fun setItemChecked(itemId: String, isChecked: Boolean)

    @Query("UPDATE checklist_items SET isChecked = :isChecked WHERE checklistId = :checklistId")
    suspend fun setAllItemsChecked(checklistId: String, isChecked: Boolean)

    @Query("DELETE FROM checklist_items WHERE checklistId = :checklistId AND isChecked = 1")
    suspend fun deleteCompletedItems(checklistId: String)

    @Transaction
    @Query("SELECT * FROM checklists WHERE groupId = :groupId ORDER BY updatedAtEpochMs DESC")
    fun observeByGroup(groupId: String): Flow<List<ChecklistWithItems>>

    @Transaction
    @Query("SELECT * FROM checklists WHERE groupId = :groupId ORDER BY updatedAtEpochMs DESC")
    suspend fun getChecklistsByGroup(groupId: String): List<ChecklistWithItems>

    @Query("UPDATE checklists SET groupId = :groupId, updatedAtEpochMs = :now WHERE id = :checklistId")
    suspend fun assignChecklistToGroup(checklistId: String, groupId: String?, now: Long)

    @Query("DELETE FROM checklists")
    suspend fun deleteAllChecklists()

    @Transaction
    suspend fun restoreChecklists(
        items: List<ChecklistWithItems>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            deleteAllChecklists()
        }
        for (item in items) {
            insertChecklistWithItems(item.checklist, item.items)
        }
    }
}
