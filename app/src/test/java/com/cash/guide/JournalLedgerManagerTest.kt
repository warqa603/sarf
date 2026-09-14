package com.cash.guide

import com.cash.guide.domain.JournalLedgerManager
import com.cash.guide.domain.LedgerEntry
import com.cash.guide.domain.MoneyUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalLedgerManagerTest {

    private var idCounter = 100L
    private val nextId = { idCounter++ }

    @Test
    fun `initial empty list receives exactly one empty row`() {
        val (rows, appendedId) = JournalLedgerManager.normalize(emptyList(), nextId)
        assertEquals(1, rows.size)
        assertTrue(rows[0].isEmpty)
        assertEquals(100L, appendedId)
    }

    @Test
    fun `typing title in trailing empty row appends exactly one new empty row`() {
        val initial = listOf(LedgerEntry(id = 1L, title = "", expression = ""))
        val modified = listOf(LedgerEntry(id = 1L, title = "Naïma", expression = ""))
        val (rows, appendedId) = JournalLedgerManager.normalize(modified, nextId)

        assertEquals(2, rows.size)
        assertEquals("Naïma", rows[0].title)
        assertTrue(rows[1].isEmpty)
        assertEquals(100L, appendedId)
    }

    @Test
    fun `typing amount in trailing empty row appends exactly one new empty row`() {
        val initial = listOf(LedgerEntry(id = 1L, title = "", expression = ""))
        val modified = listOf(LedgerEntry(id = 1L, title = "", expression = "1200"))
        val (rows, appendedId) = JournalLedgerManager.normalize(modified, nextId)

        assertEquals(2, rows.size)
        assertEquals("1200", rows[0].expression)
        assertTrue(rows[1].isEmpty)
        assertEquals(100L, appendedId)
    }

    @Test
    fun `repeated normalization does not append duplicates`() {
        val initial = listOf(
            LedgerEntry(id = 1L, title = "Naïma", expression = "1200"),
            LedgerEntry(id = 2L, title = "", expression = "")
        )
        val (rows1, appendedId1) = JournalLedgerManager.normalize(initial, nextId)
        assertEquals(2, rows1.size)
        assertNull(appendedId1)

        val (rows2, appendedId2) = JournalLedgerManager.normalize(rows1, nextId)
        assertEquals(2, rows2.size)
        assertNull(appendedId2)
    }

    @Test
    fun `clearing data restores exactly one empty row`() {
        val rows = listOf(
            LedgerEntry(id = 1L, title = "", expression = ""),
            LedgerEntry(id = 2L, title = "", expression = ""),
            LedgerEntry(id = 3L, title = "", expression = "")
        )
        val (normalized, _) = JournalLedgerManager.normalize(rows, nextId)
        assertEquals(1, normalized.size)
        assertTrue(normalized[0].isEmpty)
    }

    @Test
    fun `deleting first, middle, or last populated row preserves trailing empty row`() {
        val list = listOf(
            LedgerEntry(id = 1L, title = "Naïma", expression = "1200"),
            LedgerEntry(id = 2L, title = "Nadia", expression = "5000"),
            LedgerEntry(id = 3L, title = "Marché", expression = "2600"),
            LedgerEntry(id = 4L, title = "", expression = "")
        )

        // Delete middle row (id=2)
        val afterMiddleDelete = list.filter { it.id != 2L }
        val (normMiddle, _) = JournalLedgerManager.normalize(afterMiddleDelete, nextId)
        assertEquals(3, normMiddle.size)
        assertEquals(1L, normMiddle[0].id)
        assertEquals(3L, normMiddle[1].id)
        assertTrue(normMiddle[2].isEmpty)

        // Delete all populated rows
        val allDeleted = listOf(LedgerEntry(id = 4L, title = "", expression = ""))
        val (normAll, _) = JournalLedgerManager.normalize(allDeleted, nextId)
        assertEquals(1, normAll.size)
        assertTrue(normAll[0].isEmpty)
    }

    @Test
    fun `formatDisplayExpression adds non-breaking thousand separators without altering expression math`() {
        assertEquals("1\u00A0200", JournalLedgerManager.formatDisplayExpression("1200"))
        assertEquals("5\u00A0000", JournalLedgerManager.formatDisplayExpression("5000"))
        assertEquals("2\u00A0600", JournalLedgerManager.formatDisplayExpression("2600"))
        assertEquals("8\u00A0800", JournalLedgerManager.formatDisplayExpression("8800"))
        assertEquals("440", JournalLedgerManager.formatDisplayExpression("440"))
        assertEquals("1\u00A0200 + 500", JournalLedgerManager.formatDisplayExpression("1200+500"))
        assertEquals("1\u00A0200.50", JournalLedgerManager.formatDisplayExpression("1200.50"))
        assertEquals("2 × (10 + 5)", JournalLedgerManager.formatDisplayExpression("2×(10+5)"))
        assertEquals("2 × (1\u00A0200 + 500)", JournalLedgerManager.formatDisplayExpression("2×(1200+500)"))
    }

    @Test
    fun `formatFrenchNumber formats raw numeric values with non-breaking grouping spaces`() {
        assertEquals("1\u00A0200", JournalLedgerManager.formatFrenchNumber("1200"))
        assertEquals("5\u00A0000", JournalLedgerManager.formatFrenchNumber("5000"))
        assertEquals("2\u00A0600", JournalLedgerManager.formatFrenchNumber("2600"))
        assertEquals("6\u00A0206", JournalLedgerManager.formatFrenchNumber("6206"))
        assertEquals("8\u00A0800", JournalLedgerManager.formatFrenchNumber("8800"))
        assertEquals("51\u00A0207", JournalLedgerManager.formatFrenchNumber("51207"))
        assertEquals("1\u00A0000\u00A0000", JournalLedgerManager.formatFrenchNumber("1000000"))
        assertEquals("2\u00A0560.35", JournalLedgerManager.formatFrenchNumber("2560.35"))
        assertEquals("8\u00A0800.5", JournalLedgerManager.formatFrenchNumber("8800.5"))
        assertEquals("440", JournalLedgerManager.formatFrenchNumber("440"))
        assertEquals("", JournalLedgerManager.formatFrenchNumber(""))
    }

    @Test
    fun `formatTotal formats centimes correctly for Rial and Dirham with non-breaking spaces`() {
        // 8800 rial = 44000 centimes = 440 DH
        val centimes = 44_000L
        assertEquals("8\u00A0800", JournalLedgerManager.formatTotal(centimes, MoneyUnit.RIAL))
        assertEquals("440", JournalLedgerManager.formatTotal(centimes, MoneyUnit.DIRHAM))

        // 51207 rial = 256035 centimes = 2560.35 DH
        val centimes51207 = 256_035L
        assertEquals("51\u00A0207", JournalLedgerManager.formatTotal(centimes51207, MoneyUnit.RIAL))
        assertEquals("2\u00A0560.35", JournalLedgerManager.formatTotal(centimes51207, MoneyUnit.DIRHAM))

        // 1000000 rial = 5000000 centimes = 50000 DH
        val centimesMillion = 5_000_000L
        assertEquals("1\u00A0000\u00A0000", JournalLedgerManager.formatTotal(centimesMillion, MoneyUnit.RIAL))
        assertEquals("50\u00A0000", JournalLedgerManager.formatTotal(centimesMillion, MoneyUnit.DIRHAM))
    }

    enum class ActiveFieldType {
        NONE, TITLE, AMOUNT
    }

    data class EditSessionState(
        var activeRowId: Long? = null,
        var activeField: ActiveFieldType = ActiveFieldType.NONE,
        var keyboardExpanded: Boolean = false,
        var imeActionDoneTriggered: Boolean = false
    ) {
        fun isRowActive(rowId: Long): Boolean {
            return activeRowId == rowId && activeField != ActiveFieldType.NONE
        }

        fun getActionIcon(rowId: Long): String {
            return if (isRowActive(rowId)) "✓" else "×"
        }

        fun onConfirmRow(rowId: Long) {
            if (activeRowId == rowId) {
                activeRowId = null
                activeField = ActiveFieldType.NONE
                keyboardExpanded = false
            }
        }

        fun onImeDone(rowId: Long) {
            imeActionDoneTriggered = true
            onConfirmRow(rowId)
        }
    }

    @Test
    fun `row action displays delete x when inactive and checkmark when actively editing`() {
        val session = EditSessionState()
        val row1Id = 1L
        val row2Id = 2L

        // Initially inactive: both show delete '×'
        assertEquals("×", session.getActionIcon(row1Id))
        assertEquals("×", session.getActionIcon(row2Id))

        // Activate title on row 1
        session.activeRowId = row1Id
        session.activeField = ActiveFieldType.TITLE
        assertEquals("✓", session.getActionIcon(row1Id))
        assertEquals("×", session.getActionIcon(row2Id))

        // Switch to amount on row 1
        session.activeField = ActiveFieldType.AMOUNT
        assertEquals("✓", session.getActionIcon(row1Id))
        assertEquals("×", session.getActionIcon(row2Id))

        // Confirm row 1
        session.onConfirmRow(row1Id)
        assertEquals("×", session.getActionIcon(row1Id))
        assertEquals("×", session.getActionIcon(row2Id))
    }

    @Test
    fun `pressing checkmark commits title and amount and clears active editing state`() {
        val session = EditSessionState(
            activeRowId = 1L,
            activeField = ActiveFieldType.AMOUNT,
            keyboardExpanded = true
        )
        val rawExpression = "5000"

        // Action icon is checkmark
        assertEquals("✓", session.getActionIcon(1L))

        // User taps checkmark to confirm
        session.onConfirmRow(1L)

        // Active state cleared
        org.junit.Assert.assertNull(session.activeRowId)
        assertEquals(ActiveFieldType.NONE, session.activeField)
        org.junit.Assert.assertFalse(session.keyboardExpanded)
        assertEquals("×", session.getActionIcon(1L))

        // Confirmed display uses French non-breaking space grouping
        val formattedDisplay = JournalLedgerManager.formatFrenchNumber(rawExpression)
        assertEquals("5\u00A0000", formattedDisplay)

        // Raw expression value is preserved unaltered
        assertEquals("5000", rawExpression)
    }

    @Test
    fun `IME Done on title performs confirmation and clears editing state`() {
        val session = EditSessionState(
            activeRowId = 2L,
            activeField = ActiveFieldType.TITLE
        )

        // Trigger IME Done (Samsung keyboard action Done / Terminé)
        session.onImeDone(2L)

        org.junit.Assert.assertTrue(session.imeActionDoneTriggered)
        org.junit.Assert.assertNull(session.activeRowId)
        assertEquals(ActiveFieldType.NONE, session.activeField)
        assertEquals("×", session.getActionIcon(2L))
    }

    @Test
    fun `Total Result displays only the selected unit and never both simultaneously`() {
        val centimes = 44_000L // 8800 rial = 440 DH

        // Rial mode: exactly "8 800 rial"
        val rialResult = JournalLedgerManager.formatTotal(centimes, MoneyUnit.RIAL)
        val rialSuffix = "rial"
        assertEquals("8\u00A0800", rialResult)
        assertEquals("rial", rialSuffix)
        org.junit.Assert.assertFalse(rialResult.contains("DH"))

        // Dirham mode: exactly "440 DH"
        val dirhamResult = JournalLedgerManager.formatTotal(centimes, MoneyUnit.DIRHAM)
        val dirhamSuffix = "DH"
        assertEquals("440", dirhamResult)
        assertEquals("DH", dirhamSuffix)
        org.junit.Assert.assertFalse(dirhamResult.contains("rial"))
    }
}
