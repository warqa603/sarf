package com.cash.guide.feature.cashregister

import com.cash.guide.domain.MoneyUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CashRegisterViewModelTest {

    private lateinit var viewModel: CashRegisterViewModel

    @Before
    fun setup() {
        viewModel = CashRegisterViewModel()
    }

    @Test
    fun computeChange_withHigherReceived_calculatesChangeAndPieces() {
        viewModel.setPurchaseText("68")
        viewModel.setReceivedText("100")

        val state = viewModel.uiState.value
        assertEquals(6800L, state.purchaseCentimes)
        assertEquals(10000L, state.receivedCentimes)
        assertEquals(3200L, state.changeCentimes)
        assertFalse(state.isExactAmount)
        assertFalse(state.isInsufficient)

        // 32 DH = 1x 20 DH, 1x 10 DH, 1x 2 DH
        assertEquals(3, state.pieces.size)
        assertEquals("20 درهم", state.pieces[0].denomination.label)
        assertEquals(1L, state.pieces[0].count)
        assertEquals("10 درهم", state.pieces[1].denomination.label)
        assertEquals(1L, state.pieces[1].count)
        assertEquals("درهمان", state.pieces[2].denomination.label)
        assertEquals(1L, state.pieces[2].count)
    }

    @Test
    fun computeChange_withExactAmount_flagsExact() {
        viewModel.setPurchaseText("50")
        viewModel.setReceivedText("50")

        val state = viewModel.uiState.value
        assertEquals(5000L, state.purchaseCentimes)
        assertEquals(5000L, state.receivedCentimes)
        assertEquals(0L, state.changeCentimes)
        assertTrue(state.isExactAmount)
        assertFalse(state.isInsufficient)
        assertTrue(state.pieces.isEmpty())
    }

    @Test
    fun computeChange_withInsufficientAmount_flagsInsufficient() {
        viewModel.setPurchaseText("100")
        viewModel.setReceivedText("85")

        val state = viewModel.uiState.value
        assertEquals(10000L, state.purchaseCentimes)
        assertEquals(8500L, state.receivedCentimes)
        assertEquals(0L, state.changeCentimes)
        assertFalse(state.isExactAmount)
        assertTrue(state.isInsufficient)
        assertEquals(1500L, state.shortageCentimes)
        assertTrue(state.pieces.isEmpty())
    }

    @Test
    fun selectPresetReceived_setsPresetCorrectly() {
        viewModel.setPurchaseText("35")
        viewModel.selectPresetReceived(100L)

        val state = viewModel.uiState.value
        assertEquals("100", state.receivedText)
        assertEquals(6500L, state.changeCentimes)
    }

    @Test
    fun expressionInPurchaseText_evaluatesAccurately() {
        viewModel.setPurchaseText("20 + 15 + 5")
        viewModel.setReceivedText("50")

        val state = viewModel.uiState.value
        assertEquals(4000L, state.purchaseCentimes)
        assertEquals(1000L, state.changeCentimes) // 50 - 40 = 10 DH
        assertEquals(1, state.pieces.size)
        assertEquals("10 درهم", state.pieces[0].denomination.label)
    }

    @Test
    fun clear_resetsAllInputsAndResults() {
        viewModel.setPurchaseText("68")
        viewModel.setReceivedText("100")
        viewModel.clear()

        val state = viewModel.uiState.value
        assertEquals("", state.purchaseText)
        assertEquals("", state.receivedText)
        assertEquals(0L, state.changeCentimes)
        assertFalse(state.isExactAmount)
        assertFalse(state.isInsufficient)
        assertTrue(state.pieces.isEmpty())
    }

    @Test
    fun toggleCurrency_convertsUnitsBetweenDirhamAndRial() {
        viewModel.setPurchaseText("10")
        assertEquals(MoneyUnit.DIRHAM, viewModel.uiState.value.currencyUnit)

        viewModel.toggleCurrency()
        val rialState = viewModel.uiState.value
        assertEquals(MoneyUnit.RIAL, rialState.currencyUnit)
        assertEquals("200", rialState.purchaseText)
    }

    @Test
    fun calculatorKeys_evaluateInRealTime_andStepTransitionsWork() {
        // Initial state is CALCULATOR
        assertEquals(CashRegisterStep.CALCULATOR, viewModel.uiState.value.step)

        // Type 15 + 20 + 8
        viewModel.applyCalculatorKey("1")
        viewModel.applyCalculatorKey("5")
        viewModel.applyCalculatorKey("+")
        viewModel.applyCalculatorKey("2")
        viewModel.applyCalculatorKey("0")
        viewModel.applyCalculatorKey("+")
        viewModel.applyCalculatorKey("8")

        val calcState = viewModel.uiState.value
        assertEquals("15+20+8", calcState.calcExpression)
        assertEquals("43", calcState.purchaseText)
        assertEquals(4300L, calcState.purchaseCentimes)

        // Go to Change Return
        viewModel.goToChangeReturn()
        val changeStepState = viewModel.uiState.value
        assertEquals(CashRegisterStep.CHANGE_RETURN, changeStepState.step)
        assertEquals("43", changeStepState.purchaseText)

        // Select preset 50 DH received
        viewModel.selectPresetReceived(50L)
        val receivedState = viewModel.uiState.value
        assertEquals(700L, receivedState.changeCentimes) // 50 - 43 = 7 DH
        // 7 DH = 1x 5 DH, 1x 2 DH
        assertEquals(2, receivedState.pieces.size)

        // Return back to calculator
        viewModel.goToCalculator()
        assertEquals(CashRegisterStep.CALCULATOR, viewModel.uiState.value.step)

        // Client suivant / clear resets everything and returns to CALCULATOR
        viewModel.clear()
        val clearedState = viewModel.uiState.value
        assertEquals(CashRegisterStep.CALCULATOR, clearedState.step)
        assertEquals("", clearedState.calcExpression)
        assertEquals("", clearedState.purchaseText)
        assertEquals("", clearedState.receivedText)
    }

    @Test
    fun equalsKey_preservesOperationOnTop_andCalculatesResult() {
        // Type 25 + 13
        viewModel.applyCalculatorKey("2")
        viewModel.applyCalculatorKey("5")
        viewModel.applyCalculatorKey("+")
        viewModel.applyCalculatorKey("1")
        viewModel.applyCalculatorKey("3")

        // Press '='
        viewModel.applyCalculatorKey("=")

        val stateAfterEquals = viewModel.uiState.value
        // Operation remains visible on top!
        assertEquals("25+13", stateAfterEquals.calcExpression)
        // Result in orange at bottom is 38
        assertEquals("38", stateAfterEquals.purchaseText)
        assertEquals(3800L, stateAfterEquals.purchaseCentimes)
        assertTrue(stateAfterEquals.isEvaluated)

        // Typing a new digit starts a new operation
        viewModel.applyCalculatorKey("5")
        val stateAfterNewDigit = viewModel.uiState.value
        assertEquals("5", stateAfterNewDigit.calcExpression)
        assertEquals("5", stateAfterNewDigit.purchaseText)
        assertFalse(stateAfterNewDigit.isEvaluated)
    }

    @Test
    fun equalsKey_thenOperator_continuesExpression() {
        // Type 10 + 20
        viewModel.applyCalculatorKey("1")
        viewModel.applyCalculatorKey("0")
        viewModel.applyCalculatorKey("+")
        viewModel.applyCalculatorKey("2")
        viewModel.applyCalculatorKey("0")
        viewModel.applyCalculatorKey("=")

        val state1 = viewModel.uiState.value
        assertEquals("10+20", state1.calcExpression)
        assertEquals("30", state1.purchaseText)

        // Press '+' after '=' continues operation
        viewModel.applyCalculatorKey("+")
        viewModel.applyCalculatorKey("5")

        val state2 = viewModel.uiState.value
        assertEquals("10+20+5", state2.calcExpression)
        assertEquals("35", state2.purchaseText)
    }
}
