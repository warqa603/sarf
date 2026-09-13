package com.cash.guide.data

import com.cash.guide.data.db.CalculationDao
import com.cash.guide.data.db.CalculationEntity
import com.cash.guide.data.db.CalculationGroupDao
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.CalculationGroupWithCalculations
import com.cash.guide.data.db.CalculationItemEntity
import com.cash.guide.data.db.CalculationWithItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class CalculationRepository(
    private val dao: CalculationDao,
    private val groupDao: CalculationGroupDao? = null
) {

    fun observeRecentSaved(limit: Int = 10): Flow<List<CalculationWithItems>> =
        dao.observeRecentSaved(limit)

    fun observeAllSaved(): Flow<List<CalculationWithItems>> =
        dao.observeAllSaved()

    fun observeCalculation(id: String): Flow<CalculationWithItems?> =
        dao.observeCalculation(id)

    suspend fun getCalculation(id: String): CalculationWithItems? =
        dao.getCalculation(id)

    suspend fun getAllSaved(): List<CalculationWithItems> =
        dao.getAllSaved()

    fun searchSaved(query: String): Flow<List<CalculationWithItems>> =
        dao.searchSaved(query.trim())

    suspend fun saveCalculation(
        calculation: CalculationEntity,
        items: List<CalculationItemEntity>
    ) {
        val now = System.currentTimeMillis()
        val targetId = calculation.editingCalculationId ?: calculation.id
        val existing = dao.getCalculation(targetId)
        val originalCreatedAt = existing?.calculation?.createdAtEpochMs
            ?: calculation.createdAtEpochMs.takeIf { it > 0 }
            ?: now

        val savedEntity = calculation.copy(
            id = targetId,
            status = "SAVED",
            createdAtEpochMs = originalCreatedAt,
            updatedAtEpochMs = now,
            editingCalculationId = null,
            groupId = calculation.groupId ?: existing?.calculation?.groupId,
            paymentStatus = calculation.paymentStatus,
            calcType = calculation.calcType,
            dueDateEpochMs = calculation.dueDateEpochMs ?: existing?.calculation?.dueDateEpochMs,
            reminderEnabled = calculation.reminderEnabled,
            reminderTimeEpochMs = calculation.reminderTimeEpochMs ?: existing?.calculation?.reminderTimeEpochMs
        )
        val remappedItems = items.mapIndexed { index, item ->
            val existingItem = existing?.items?.firstOrNull { it.id == item.id }
            val itemCreatedAt = existingItem?.createdAtEpochMs
                ?: item.createdAtEpochMs.takeIf { it > 0 }
                ?: now
            item.copy(
                calculationId = targetId,
                position = index,
                createdAtEpochMs = itemCreatedAt,
                updatedAtEpochMs = now
            )
        }
        dao.upsertCalculationWithItems(savedEntity, remappedItems)
        // Clean up any drafts for this calculation
        if (calculation.id != targetId) {
            dao.deleteCalculation(calculation.id)
        }
        dao.deleteDrafts(targetId, targetId)
    }

    suspend fun saveDraft(
        calculation: CalculationEntity,
        items: List<CalculationItemEntity>
    ) {
        val draftEntity = calculation.copy(
            status = "DRAFT"
        )
        dao.upsertCalculationWithItems(draftEntity, items)
    }

    suspend fun deleteCalculation(id: String) {
        dao.deleteCalculation(id)
        dao.deleteDrafts(id, id)
    }

    suspend fun deleteDraft(draftId: String) {
        dao.deleteCalculation(draftId)
    }

    suspend fun updatePaymentStatus(id: String, paymentStatus: String) {
        val now = System.currentTimeMillis()
        dao.updatePaymentStatus(id, paymentStatus, now)
    }

    suspend fun updateCalcType(id: String, calcType: String) {
        val now = System.currentTimeMillis()
        dao.updateCalcType(id, calcType, now)
    }

    suspend fun updateCreditDueDate(
        id: String,
        dueDateEpochMs: Long?,
        reminderEnabled: Boolean,
        reminderTimeEpochMs: Long?
    ) {
        val now = System.currentTimeMillis()
        dao.updateCreditDueDate(id, dueDateEpochMs, reminderEnabled, reminderTimeEpochMs, now)
    }

    suspend fun getPendingCreditReminders(fromTime: Long = System.currentTimeMillis()): List<CalculationWithItems> =
        dao.getPendingCreditReminders(fromTime)

    suspend fun getRecoverableDraft(editingCalculationId: String?): CalculationWithItems? {
        return if (editingCalculationId != null) {
            dao.getDraftForCalculation(editingCalculationId)
        } else {
            dao.getNewDraft()
        }
    }

    suspend fun duplicateCalculation(sourceId: String): CalculationWithItems? {
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        return dao.duplicateCalculation(sourceId, newId, now)
    }

    fun observeAllGroups(): Flow<List<CalculationGroupEntity>> =
        groupDao?.observeAllGroups() ?: flowOf(emptyList())

    fun observeAllGroupsWithCalculations(): Flow<List<CalculationGroupWithCalculations>> {
        val groupsFlow = groupDao?.observeAllGroups() ?: flowOf(emptyList())
        val calculationsFlow = dao.observeAllSaved()
        return combine(groupsFlow, calculationsFlow) { groups, allSaved ->
            groups.map { group ->
                val groupCalculations = allSaved.filter { it.calculation.groupId == group.id }
                CalculationGroupWithCalculations(group, groupCalculations)
            }
        }
    }

    fun observeGroup(groupId: String): Flow<CalculationGroupEntity?> =
        groupDao?.observeGroup(groupId) ?: flowOf(null)

    fun observeGroupWithCalculations(groupId: String): Flow<CalculationGroupWithCalculations?> {
        val groupFlow = observeGroup(groupId)
        val calculationsFlow = dao.observeAllSaved()
        return combine(groupFlow, calculationsFlow) { group, allSaved ->
            if (group == null) null
            else {
                val groupCalculations = allSaved.filter { it.calculation.groupId == group.id }
                CalculationGroupWithCalculations(group, groupCalculations)
            }
        }
    }

    suspend fun getGroup(groupId: String): CalculationGroupEntity? =
        groupDao?.getGroup(groupId)

    suspend fun getAllGroups(): List<CalculationGroupEntity> =
        groupDao?.getAllGroups() ?: emptyList()

    suspend fun createGroup(name: String, colorHex: String, category: String = "CALCULATIONS"): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val group = CalculationGroupEntity(
            id = id,
            name = name.trim(),
            colorHex = colorHex,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            category = category
        )
        groupDao?.insertGroup(group)
        return id
    }

    suspend fun updateGroup(group: CalculationGroupEntity) {
        groupDao?.updateGroup(group.copy(updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun deleteGroup(groupId: String) {
        groupDao?.deleteGroupAndUngroupCalculations(groupId)
    }

    suspend fun assignCalculationToGroup(calculationId: String, groupId: String?) {
        val now = System.currentTimeMillis()
        groupDao?.assignCalculationToGroup(calculationId, groupId, now)
    }

    suspend fun createDraftInGroup(groupId: String): String {
        val existingDraft = dao.getNewDraft()
        if (existingDraft != null && existingDraft.calculation.title.isBlank() && existingDraft.items.isEmpty()) {
            dao.deleteCalculation(existingDraft.calculation.id)
        }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val draftEntity = CalculationEntity(
            id = id,
            title = "",
            currency = "DIRHAM",
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            status = "DRAFT",
            groupId = groupId
        )
        dao.upsertCalculationWithItems(draftEntity, emptyList())
        return id
    }
}
