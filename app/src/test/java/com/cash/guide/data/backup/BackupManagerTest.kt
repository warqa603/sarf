package com.cash.guide.data.backup

import com.cash.guide.data.db.CalculationEntity
import com.cash.guide.data.db.CalculationGroupEntity
import com.cash.guide.data.db.CalculationItemEntity
import com.cash.guide.data.db.CalculationWithItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupManagerTest {

    @Test
    fun serializeAndDeserialize_preservesAllFieldsAndStructure() {
        val group = CalculationGroupEntity(
            id = "grp-1",
            name = "Maison",
            colorHex = "#FF5500",
            createdAtEpochMs = 1700000000000L,
            updatedAtEpochMs = 1700000001000L
        )
        val calc = CalculationEntity(
            id = "calc-1",
            title = "Achat Marché",
            currency = "DIRHAM",
            createdAtEpochMs = 1700000002000L,
            updatedAtEpochMs = 1700000003000L,
            status = "SAVED",
            note = "Notes du marché",
            editingCalculationId = null,
            groupId = "grp-1"
        )
        val item1 = CalculationItemEntity(
            id = "item-1",
            calculationId = "calc-1",
            label = "Légumes",
            amountCentimes = 4500L,
            rawExpression = "45",
            position = 0,
            createdAtEpochMs = 1700000002500L,
            updatedAtEpochMs = 1700000002500L
        )
        val item2 = CalculationItemEntity(
            id = "item-2",
            calculationId = "calc-1",
            label = "Fruits",
            amountCentimes = 3000L,
            rawExpression = "30",
            position = 1,
            createdAtEpochMs = 1700000002600L,
            updatedAtEpochMs = 1700000002600L
        )

        val payload = BackupPayload(
            app = "SARF",
            format = "SARF_BACKUP",
            version = 1,
            exportedAtEpochMs = 1700000010000L,
            groups = listOf(group),
            calculations = listOf(CalculationWithItems(calc, listOf(item1, item2)))
        )

        val manager = BackupManager(FakeCalculationDao(), FakeGroupDao())
        val json = manager.serializeToJson(payload)

        assertTrue(json.contains("SARF_BACKUP"))
        assertTrue(json.contains("Achat Marché"))
        assertTrue(json.contains("Légumes"))
        assertTrue(json.contains("Fruits"))

        val parsed = manager.parseFromJson(json).getOrThrow()

        assertEquals("SARF", parsed.app)
        assertEquals("SARF_BACKUP", parsed.format)
        assertEquals(1, parsed.version)
        assertEquals(1, parsed.groups.size)
        assertEquals("grp-1", parsed.groups[0].id)
        assertEquals("Maison", parsed.groups[0].name)
        assertEquals("#FF5500", parsed.groups[0].colorHex)

        assertEquals(1, parsed.calculations.size)
        val parsedCalc = parsed.calculations[0]
        assertEquals("calc-1", parsedCalc.calculation.id)
        assertEquals("Achat Marché", parsedCalc.calculation.title)
        assertEquals("grp-1", parsedCalc.calculation.groupId)
        assertEquals(2, parsedCalc.items.size)
        assertEquals(4500L, parsedCalc.items[0].amountCentimes)
        assertEquals("Légumes", parsedCalc.items[0].label)
        assertEquals(3000L, parsedCalc.items[1].amountCentimes)
        assertEquals(7500L, parsedCalc.totalCentimes)
    }

    @Test
    fun parseInvalidJson_failsGracefully() {
        val manager = BackupManager(FakeCalculationDao(), FakeGroupDao())
        val result = manager.parseFromJson("{ \"invalid\": true }")
        assertTrue(result.isFailure)
    }

    private class FakeCalculationDao : com.cash.guide.data.db.CalculationDao {
        override fun observeRecentSaved(limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<CalculationWithItems>())
        override fun observeAllSaved() = kotlinx.coroutines.flow.flowOf(emptyList<CalculationWithItems>())
        override fun observeCalculation(id: String) = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun getCalculation(id: String) = null
        override suspend fun getDraftForCalculation(editingCalculationId: String) = null
        override suspend fun getNewDraft() = null
        override fun searchSaved(query: String) = kotlinx.coroutines.flow.flowOf(emptyList<CalculationWithItems>())
        override suspend fun insertCalculation(calculation: CalculationEntity) {}
        override suspend fun insertItems(items: List<CalculationItemEntity>) {}
        override suspend fun deleteItemsForCalculation(calculationId: String) {}
        override suspend fun deleteCalculation(id: String) {}
        override suspend fun deleteDrafts(draftId: String, targetId: String?) {}
        override suspend fun updatePaymentStatus(id: String, paymentStatus: String, now: Long) {}
        override suspend fun updateCalcType(id: String, calcType: String, now: Long) {}
        override suspend fun updateCreditDueDate(id: String, dueDateEpochMs: Long?, reminderEnabled: Boolean, reminderTimeEpochMs: Long?, now: Long) {}
        override suspend fun getPendingCreditReminders(fromTime: Long): List<CalculationWithItems> = emptyList()
        override suspend fun getAllSaved(): List<CalculationWithItems> = emptyList()
        override suspend fun deleteAllCalculations() {}
    }

    private class FakeGroupDao : com.cash.guide.data.db.CalculationGroupDao {
        override fun observeAllGroups() = kotlinx.coroutines.flow.flowOf(emptyList<CalculationGroupEntity>())
        override fun observeGroup(groupId: String) = kotlinx.coroutines.flow.flowOf(null)
        override suspend fun getGroup(groupId: String) = null
        override suspend fun insertGroup(group: CalculationGroupEntity) {}
        override suspend fun updateGroup(group: CalculationGroupEntity) {}
        override suspend fun deleteGroup(groupId: String) {}
        override suspend fun clearGroupIdFromCalculations(groupId: String) {}
        override suspend fun clearGroupIdFromNotes(groupId: String) {}
        override suspend fun clearGroupIdFromChecklists(groupId: String) {}
        override suspend fun assignCalculationToGroup(calculationId: String, groupId: String?, now: Long) {}
        override suspend fun getAllGroups(): List<CalculationGroupEntity> = emptyList()
        override suspend fun deleteAllGroups() {}
    }
}
