package com.cash.guide.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cash.guide.data.CalculationRepository
import com.cash.guide.data.db.CalculationDao
import com.cash.guide.data.db.CalculationEntity
import com.cash.guide.data.db.CalculationItemEntity
import com.cash.guide.data.db.CalculationWithItems
import com.cash.guide.domain.JvmGraphemeSegmenter
import com.cash.guide.domain.MoneyUnit
import com.cash.guide.ui.ActiveField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculationEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeDao : CalculationDao {
        val calculations = mutableMapOf<String, CalculationEntity>()
        val items = mutableMapOf<String, MutableList<CalculationItemEntity>>()

        override fun observeRecentSaved(limit: Int): Flow<List<CalculationWithItems>> = flowOf(emptyList())
        override fun observeAllSaved(): Flow<List<CalculationWithItems>> = flowOf(emptyList())
        override fun observeCalculation(id: String): Flow<CalculationWithItems?> =
            flowOf(calculations[id]?.let { CalculationWithItems(it, items[it.id] ?: emptyList()) })

        override suspend fun getCalculation(id: String): CalculationWithItems? {
            val calc = calculations[id] ?: return null
            return CalculationWithItems(calc, items[calc.id] ?: emptyList())
        }

        override suspend fun getDraftForCalculation(editingCalculationId: String): CalculationWithItems? {
            val draft = calculations.values.find { it.status == "DRAFT" && it.editingCalculationId == editingCalculationId }
                ?: return null
            return CalculationWithItems(draft, items[draft.id] ?: emptyList())
        }

        override suspend fun getNewDraft(): CalculationWithItems? {
            val draft = calculations.values.find { it.status == "DRAFT" && it.editingCalculationId == null }
                ?: return null
            return CalculationWithItems(draft, items[draft.id] ?: emptyList())
        }

        override fun searchSaved(query: String): Flow<List<CalculationWithItems>> = flowOf(emptyList())

        override suspend fun insertCalculation(calculation: CalculationEntity) {
            calculations[calculation.id] = calculation
        }

        override suspend fun insertItems(items: List<CalculationItemEntity>) {
            items.forEach { item ->
                this.items.getOrPut(item.calculationId) { mutableListOf() }.add(item)
            }
        }

        override suspend fun deleteItemsForCalculation(calculationId: String) {
            items.remove(calculationId)
        }

        override suspend fun deleteCalculation(id: String) {
            calculations.remove(id)
            items.remove(id)
        }

        override suspend fun deleteDrafts(draftId: String, targetId: String?) {
            val idsToRemove = calculations.values
                .filter { it.status == "DRAFT" && (it.id == draftId || (targetId != null && it.editingCalculationId == targetId)) }
                .map { it.id }
            idsToRemove.forEach { deleteCalculation(it) }
        }

        override suspend fun updatePaymentStatus(id: String, paymentStatus: String, now: Long) {
            val existing = calculations[id]
            if (existing != null) {
                calculations[id] = existing.copy(paymentStatus = paymentStatus, updatedAtEpochMs = now)
            }
        }

        override suspend fun updateCalcType(id: String, calcType: String, now: Long) {
            val existing = calculations[id]
            if (existing != null) {
                calculations[id] = existing.copy(calcType = calcType, updatedAtEpochMs = now)
            }
        }

        override suspend fun updateCreditDueDate(id: String, dueDateEpochMs: Long?, reminderEnabled: Boolean, reminderTimeEpochMs: Long?, now: Long) {
            val existing = calculations[id]
            if (existing != null) {
                calculations[id] = existing.copy(
                    dueDateEpochMs = dueDateEpochMs,
                    reminderEnabled = reminderEnabled,
                    reminderTimeEpochMs = reminderTimeEpochMs,
                    updatedAtEpochMs = now
                )
            }
        }

        override suspend fun getPendingCreditReminders(fromTime: Long): List<CalculationWithItems> = emptyList()

        override suspend fun getAllSaved(): List<CalculationWithItems> = emptyList()
        override suspend fun deleteAllCalculations() {
            calculations.clear()
            items.clear()
        }
    }

    private lateinit var dao: FakeDao
    private lateinit var repository: CalculationRepository
    private lateinit var viewModel: CalculationEditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeDao()
        repository = CalculationRepository(dao)
        viewModel = CalculationEditorViewModel(
            calculationRepository = repository,
            settingsRepository = null,
            graphemeSegmenter = JvmGraphemeSegmenter(),
            monotonicClock = { 1000L }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun newCalculation_initializesWithOneRowAndTitleFocus() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(EditorMode.NEW, state.mode)
        assertEquals(1, state.rows.size)
        assertEquals(ActiveField.TITLE, state.activeField)
        assertEquals(1L, state.activeRowId)
        assertFalse(state.isDirty)
        assertEquals(0L, state.totalCentimes)
    }

    @Test
    fun typingTitleAndAmount_updatesStateAndTotal() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.updateTitle(TextFieldValue("Courses semaine"))
        viewModel.updateRowTitle(1L, TextFieldValue("Légumes"))
        viewModel.updateRowAmount(1L, TextFieldValue("150"))

        val state = viewModel.uiState.value
        assertEquals("Courses semaine", state.title.text)
        assertEquals("Légumes", state.rows[0].title.text)
        assertEquals("150", state.rows[0].amount.text)
        assertTrue(state.isDirty)
        // 150 DH = 15 000 centimes
        assertEquals(15_000L, state.totalCentimes)
    }

    @Test
    fun addingAndRemovingRow_maintainsRhythmAndTotal() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.updateRowAmount(1L, TextFieldValue("100"))
        viewModel.addNewRow()

        var state = viewModel.uiState.value
        assertEquals(2, state.rows.size)
        assertEquals(2L, state.rows[1].id)
        assertEquals(ActiveField.TITLE, state.activeField)
        assertEquals(2L, state.activeRowId)

        viewModel.updateRowAmount(2L, TextFieldValue("50"))
        state = viewModel.uiState.value
        assertEquals(15_000L, state.totalCentimes)

        viewModel.removeRow(1L)
        state = viewModel.uiState.value
        assertEquals(1, state.rows.size)
        assertEquals(2L, state.rows[0].id)
        assertEquals(5_000L, state.totalCentimes)
    }

    @Test
    fun switchingCurrency_convertsRowAmountsAccurately() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.updateRowAmount(1L, TextFieldValue("50")) // 50 DH = 1000 rial = 5000 centimes
        assertEquals(5_000L, viewModel.uiState.value.totalCentimes)

        // Switch to RIAL
        viewModel.selectUnit(MoneyUnit.RIAL)
        val state = viewModel.uiState.value
        assertEquals(MoneyUnit.RIAL, state.currency)
        assertEquals("1000", state.rows[0].amount.text)
        assertEquals(5_000L, state.totalCentimes)
    }

    @Test
    fun calculatorPopup_evaluatesOnlyAndConfirmerInsertsToTargetRow() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.selectRowField(1L, ActiveField.AMOUNT)
        viewModel.openCalculatorPopup(1L)

        assertTrue(viewModel.uiState.value.calculator.isVisible)

        // Enter 600 + 250 + 30
        "600+250+30".forEach { ch ->
            viewModel.applyPopupKey(ch.toString())
        }
        viewModel.applyPopupKey("=")

        var state = viewModel.uiState.value
        assertEquals("880", state.calculator.result)
        assertTrue(state.calculator.isEvaluated)
        // Row is still empty before Confirmer
        assertEquals("", state.rows[0].amount.text)

        // User taps Confirmer
        viewModel.confirmPopupResult()
        state = viewModel.uiState.value
        assertFalse(state.calculator.isVisible)
        assertEquals("880", state.rows[0].amount.text)
        // 880 DH = 88 000 centimes
        assertEquals(88_000L, state.totalCentimes)
    }

    @Test
    fun explicitSave_commitsAtomicallyAndDraftNeverMutatesSavedRecord() = runTest {
        // Step 1: Pre-populate an existing saved calculation
        val savedId = "existing-1"
        dao.insertCalculation(
            CalculationEntity(
                id = savedId,
                title = "Original Title",
                currency = "DIRHAM",
                createdAtEpochMs = 1000L,
                updatedAtEpochMs = 1000L,
                status = "SAVED"
            )
        )
        dao.insertItems(
            listOf(
                CalculationItemEntity(
                    id = "item-1",
                    calculationId = savedId,
                    label = "Original Item",
                    amountCentimes = 20_000L,
                    position = 0,
                    createdAtEpochMs = 1000L,
                    updatedAtEpochMs = 1000L
                )
            )
        )

        // Step 2: Load calculation into ViewModel
        viewModel.loadCalculation(savedId)
        advanceUntilIdle()

        val initialState = viewModel.uiState.value
        assertEquals(EditorMode.EXISTING, initialState.mode)
        assertEquals("Original Title", initialState.title.text)
        assertEquals("200", initialState.rows[0].amount.text)
        assertFalse(initialState.isDirty)

        // Step 3: User edits title and amount
        viewModel.updateTitle(TextFieldValue("Modified Title"))
        viewModel.updateRowAmount(initialState.rows[0].id, TextFieldValue("500"))
        assertTrue(viewModel.uiState.value.isDirty)

        // Let debounce draft save trigger
        advanceTimeBy(1000)
        advanceUntilIdle()

        // Step 4: Verify the saved calculation in repository is UNCHANGED!
        val preserved = repository.getCalculation(savedId)
        assertNotNull(preserved)
        assertEquals("SAVED", preserved!!.calculation.status)
        assertEquals("Original Title", preserved.calculation.title)
        assertEquals(1000L, preserved.calculation.updatedAtEpochMs)
        assertEquals(1, preserved.items.size)
        assertEquals(20_000L, preserved.totalCentimes)

        // Step 5: User explicitly saves
        viewModel.saveCalculation()
        advanceUntilIdle()

        // Step 6: Verify original calculation is now atomically updated
        val updated = repository.getCalculation(savedId)
        assertNotNull(updated)
        assertEquals("Modified Title", updated!!.calculation.title)
        assertEquals(50_000L, updated.totalCentimes)
        assertFalse(viewModel.uiState.value.isDirty)
    }

    @Test
    fun dirtyBackPress_triggersUnsavedChangesDialog() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.updateTitle(TextFieldValue("Unsaved Work"))
        assertTrue(viewModel.uiState.value.isDirty)

        var navigatedBack = false
        viewModel.handleBackPress { navigatedBack = true }

        assertFalse(navigatedBack)
        assertTrue(viewModel.uiState.value.showUnsavedDialog)

        // User chooses to discard
        viewModel.discardChanges { navigatedBack = true }
        advanceUntilIdle()

        assertTrue(navigatedBack)
        assertFalse(viewModel.uiState.value.showUnsavedDialog)
    }

    @Test
    fun saveCalculation_failsWhenTitleIsBlank_andSetsValidationError() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.updateTitle(TextFieldValue("   "))
        viewModel.updateRowAmount(1L, TextFieldValue("150"))

        var saved = false
        viewModel.saveCalculation { saved = true }
        advanceUntilIdle()

        assertFalse(saved)
        assertEquals("TITLE_REQUIRED", viewModel.uiState.value.validationError)
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun updateTitle_clearsValidationError() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.saveCalculation()
        assertEquals("TITLE_REQUIRED", viewModel.uiState.value.validationError)

        viewModel.updateTitle(TextFieldValue("A"))
        assertNull(viewModel.uiState.value.validationError)
    }

    @Test
    fun losslessDraftRecovery_restoresRawExpressionFaithfully() = runTest {
        val draftId = "draft_raw"
        val now = 1000L
        val draftCalc = CalculationEntity(
            id = draftId,
            title = "Brouillon",
            currency = "DIRHAM",
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            status = "DRAFT"
        )
        val draftItem = CalculationItemEntity(
            id = "d_i1",
            calculationId = draftId,
            label = "Courses",
            amountCentimes = 85_000L,
            rawExpression = "600 + 250",
            position = 0,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        dao.upsertCalculationWithItems(draftCalc, listOf(draftItem))

        val draftVm = CalculationEditorViewModel(
            calculationRepository = repository,
            settingsRepository = null,
            graphemeSegmenter = JvmGraphemeSegmenter(),
            monotonicClock = { 1000L }
        )
        draftVm.loadCalculation(null)
        advanceUntilIdle()

        val state = draftVm.uiState.value
        assertTrue(state.recoveredDraft)
        assertEquals("Brouillon", state.title.text)
        assertEquals("600 + 250", state.rows[0].amount.text)
        assertEquals("600 + 250", state.rows[0].rawExpression)
    }

    @Test
    fun togglePaymentStatus_flipsBetweenPaidAndUnpaid() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        assertEquals("PAID", viewModel.uiState.value.paymentStatus)

        viewModel.togglePaymentStatus()
        assertEquals("UNPAID", viewModel.uiState.value.paymentStatus)
        assertTrue(viewModel.uiState.value.isDirty)

        viewModel.togglePaymentStatus()
        assertEquals("PAID", viewModel.uiState.value.paymentStatus)
    }

    @Test
    fun loadCalculation_withInitialType_initializesCorrectlyAndToggles() = runTest {
        viewModel.loadCalculation(null, initialType = "CREDIT")
        advanceUntilIdle()

        assertEquals("CREDIT", viewModel.uiState.value.calcType)
        assertEquals("UNPAID", viewModel.uiState.value.paymentStatus)

        viewModel.toggleCalcType()
        assertEquals("PERSONNEL", viewModel.uiState.value.calcType)
        assertEquals("PAID", viewModel.uiState.value.paymentStatus)
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun calculatorPopup_evaluatesParenthesesExpressionAndClearKey() = runTest {
        viewModel.loadCalculation(null)
        advanceUntilIdle()

        viewModel.selectRowField(1L, ActiveField.AMOUNT)
        viewModel.openCalculatorPopup(1L)
        assertTrue(viewModel.uiState.value.calculator.isVisible)

        // Type 2 × ( 1 0 + 5 )
        viewModel.applyPopupKey("2")
        viewModel.applyPopupKey("×")
        viewModel.applyPopupKey("(")
        viewModel.applyPopupKey("1")
        viewModel.applyPopupKey("0")
        viewModel.applyPopupKey("+")
        viewModel.applyPopupKey("5")
        viewModel.applyPopupKey(")")
        viewModel.applyPopupKey("=")

        var state = viewModel.uiState.value
        assertEquals("30", state.calculator.result)
        assertTrue(state.calculator.isEvaluated)

        // Test C key
        viewModel.applyPopupKey("C")
        state = viewModel.uiState.value
        assertEquals("", state.calculator.expression)
        assertEquals("", state.calculator.result)
        assertFalse(state.calculator.isEvaluated)
        assertFalse(state.calculator.hasError)
    }
}
