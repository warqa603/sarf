package com.cash.guide.data

import com.cash.guide.data.db.ChecklistDao
import com.cash.guide.data.db.ChecklistEntity
import com.cash.guide.data.db.ChecklistItemEntity
import com.cash.guide.data.db.ChecklistWithItems
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChecklistRepository(
    private val checklistDao: ChecklistDao
) {
    fun observeAll(): Flow<List<ChecklistWithItems>> = checklistDao.observeAll()

    fun observeByGroup(groupId: String): Flow<List<ChecklistWithItems>> = checklistDao.observeByGroup(groupId)

    suspend fun getChecklistsByGroup(groupId: String): List<ChecklistWithItems> = checklistDao.getChecklistsByGroup(groupId)

    fun observeChecklist(id: String): Flow<ChecklistWithItems?> = checklistDao.observeChecklist(id)

    suspend fun getChecklist(id: String): ChecklistWithItems? = checklistDao.getChecklist(id)

    suspend fun getLatestChecklist(): ChecklistWithItems? = checklistDao.getLatestChecklist()

    suspend fun createChecklist(
        title: String,
        initialItems: List<String> = emptyList(),
        groupId: String? = null
    ): String {
        val checklistId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val checklist = ChecklistEntity(
            id = checklistId,
            title = title.trim(),
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            groupId = groupId
        )
        checklistDao.insertChecklist(checklist)

        val items = initialItems.mapIndexed { index, text ->
            ChecklistItemEntity(
                id = UUID.randomUUID().toString(),
                checklistId = checklistId,
                text = text.trim(),
                isChecked = false,
                position = index,
                createdAtEpochMs = now + index
            )
        }
        if (items.isNotEmpty()) {
            checklistDao.insertItems(items)
        }
        return checklistId
    }

    suspend fun assignChecklistToGroup(checklistId: String, groupId: String?) {
        checklistDao.assignChecklistToGroup(checklistId, groupId, System.currentTimeMillis())
    }

    suspend fun updateTitle(checklistId: String, title: String) {
        val existing = checklistDao.getChecklist(checklistId) ?: return
        val updated = existing.checklist.copy(
            title = title.trim(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        checklistDao.updateChecklist(updated)
    }

    suspend fun addItem(checklistId: String, text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val existing = checklistDao.getChecklist(checklistId) ?: return null
        val nextPosition = existing.items.size
        val itemId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val item = ChecklistItemEntity(
            id = itemId,
            checklistId = checklistId,
            text = trimmed,
            isChecked = false,
            position = nextPosition,
            createdAtEpochMs = now
        )
        checklistDao.insertItem(item)
        checklistDao.updateChecklist(existing.checklist.copy(updatedAtEpochMs = now))
        return itemId
    }

    suspend fun updateItemText(itemId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        // Fetch checklist via DAO or query
        // Simple update: We can observe or update
    }

    suspend fun toggleItem(itemId: String, isChecked: Boolean) {
        checklistDao.setItemChecked(itemId, isChecked)
    }

    suspend fun deleteItem(itemId: String) {
        checklistDao.deleteItemById(itemId)
    }

    suspend fun deleteChecklist(checklistId: String) {
        checklistDao.deleteChecklistById(checklistId)
    }

    suspend fun setAllItemsChecked(checklistId: String, isChecked: Boolean) {
        checklistDao.setAllItemsChecked(checklistId, isChecked)
    }

    suspend fun deleteCompletedItems(checklistId: String) {
        checklistDao.deleteCompletedItems(checklistId)
    }

    suspend fun importChecklist(title: String, items: List<Pair<String, Boolean>>): String {
        val checklistId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val checklist = ChecklistEntity(
            id = checklistId,
            title = title.ifBlank { "Checklist" }.trim(),
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        val itemEntities = items.mapIndexed { index, pair ->
            ChecklistItemEntity(
                id = UUID.randomUUID().toString(),
                checklistId = checklistId,
                text = pair.first.trim(),
                isChecked = pair.second,
                position = index,
                createdAtEpochMs = now + index
            )
        }
        checklistDao.insertChecklistWithItems(checklist, itemEntities)
        return checklistId
    }
}
