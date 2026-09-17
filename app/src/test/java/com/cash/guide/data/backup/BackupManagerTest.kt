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
    fun serializeAndDeserialize_allTenEntities_preservesEverything() {
        val group = CalculationGroupEntity(
            id = "g1",
            name = "Personnel",
            colorHex = "#123456",
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 200L,
            category = "CALCULATIONS"
        )
        val calc = CalculationEntity(
            id = "c1",
            title = "Calc 1",
            currency = "DIRHAM",
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 200L,
            status = "SAVED"
        )
        val calcItem = CalculationItemEntity("ci1", "c1", "Item", 1000L, "10", 0, 100L, 200L)
        val cl = com.cash.guide.data.db.ChecklistEntity("cl1", "Courses", 100L, 200L, "g1")
        val clItem = com.cash.guide.data.db.ChecklistItemEntity("cli1", "cl1", "Lait", true, 0, 100L)
        val note = com.cash.guide.data.db.NoteEntity("n1", "Note Title", "Note Content", "YELLOW", true, 100L, 200L, "g1")
        val reminder = com.cash.guide.data.db.ReminderEntity("r1", "Facture", "Payer eau", 500000L, "MONTHLY", "1", 10, 30, true, false, "BLUE", "c1", 100L, 200L)
        val goal = com.cash.guide.data.db.SavingsGoalEntity("sg1", "Achat Voiture", 10000000L, 2000000L, 500000L, 999999L, "DIRHAM", "BLUE", "CAR", false, 24, 600000L, "MEDIUM", "CAFE", "BALANCED", 1000000L, 2500L, 6, 100L, 200L)
        val deposit = com.cash.guide.data.db.SavingsDepositEntity("sd1", "sg1", 50000L, "Versement Janvier", 300L)
        val profile = com.cash.guide.data.db.FinancialProfileEntity(
            id = "fp1",
            goalId = "sg1",
            netMonthlyIncomeCentimes = 800000L,
            housingCentimes = 250000L,
            createdAtEpochMs = 100L,
            updatedAtEpochMs = 200L
        )

        val payload = BackupPayload(
            app = "SARF",
            format = "SARF_BACKUP",
            version = 2,
            exportedAtEpochMs = 1700000000000L,
            groups = listOf(group),
            calculations = listOf(CalculationWithItems(calc, listOf(calcItem))),
            checklists = listOf(com.cash.guide.data.db.ChecklistWithItems(cl, listOf(clItem))),
            notes = listOf(note),
            reminders = listOf(reminder),
            savingsGoals = listOf(goal),
            savingsDeposits = listOf(deposit),
            financialProfiles = listOf(profile)
        )

        val manager = BackupManager(FakeCalculationDao(), FakeGroupDao())
        val json = manager.serializeToJson(payload)
        val parsed = manager.parseFromJson(json).getOrThrow()

        assertEquals(2, parsed.version)
        assertEquals(1, parsed.groups.size)
        assertEquals(1, parsed.calculations.size)
        assertEquals(1, parsed.checklists.size)
        assertEquals("Courses", parsed.checklists[0].checklist.title)
        assertEquals("Lait", parsed.checklists[0].items[0].text)
        assertTrue(parsed.checklists[0].items[0].isChecked)

        assertEquals(1, parsed.notes.size)
        assertEquals("Note Title", parsed.notes[0].title)
        assertTrue(parsed.notes[0].isPinned)

        assertEquals(1, parsed.reminders.size)
        assertEquals("Facture", parsed.reminders[0].title)
        assertEquals(10, parsed.reminders[0].timeHour)
        assertEquals(30, parsed.reminders[0].timeMinute)

        assertEquals(1, parsed.savingsGoals.size)
        assertEquals("Achat Voiture", parsed.savingsGoals[0].title)
        assertEquals(10000000L, parsed.savingsGoals[0].targetAmountCentimes)
        assertEquals(2000000L, parsed.savingsGoals[0].currentAmountCentimes)

        assertEquals(1, parsed.savingsDeposits.size)
        assertEquals("Versement Janvier", parsed.savingsDeposits[0].note)
        assertEquals(50000L, parsed.savingsDeposits[0].amountCentimes)

        assertEquals(1, parsed.financialProfiles.size)
        assertEquals("sg1", parsed.financialProfiles[0].goalId)
        assertEquals(800000L, parsed.financialProfiles[0].netMonthlyIncomeCentimes)
        assertEquals(250000L, parsed.financialProfiles[0].housingCentimes)
    }

    @Test
    fun parseLegacyV1Backup_succeedsWithEmptyListsForNewEntities() {
        val legacyJson = """
            {
              "app": "SARF",
              "format": "SARF_BACKUP",
              "version": 1,
              "exportedAtEpochMs": 1700000000000,
              "groups": [
                {
                  "id": "g-old",
                  "name": "Ancien Groupe",
                  "colorHex": "#E5A93C",
                  "createdAtEpochMs": 100,
                  "updatedAtEpochMs": 200
                }
              ],
              "calculations": []
            }
        """.trimIndent()

        val manager = BackupManager(FakeCalculationDao(), FakeGroupDao())
        val parsed = manager.parseFromJson(legacyJson).getOrThrow()

        assertEquals("SARF", parsed.app)
        assertEquals(1, parsed.groups.size)
        assertEquals("Ancien Groupe", parsed.groups[0].name)
        assertTrue(parsed.calculations.isEmpty())
        assertTrue(parsed.checklists.isEmpty())
        assertTrue(parsed.notes.isEmpty())
        assertTrue(parsed.reminders.isEmpty())
        assertTrue(parsed.savingsGoals.isEmpty())
        assertTrue(parsed.savingsDeposits.isEmpty())
        assertTrue(parsed.financialProfiles.isEmpty())
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
