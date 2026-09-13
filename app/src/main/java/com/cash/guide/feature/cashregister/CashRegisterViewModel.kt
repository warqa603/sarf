package com.cash.guide.feature.cashregister

import androidx.lifecycle.ViewModel
import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyPiece
import com.cash.guide.domain.MoneyUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CashRegisterStep {
    CALCULATOR,
    CHANGE_RETURN
}

data class CashRegisterUiState(
    val step: CashRegisterStep = CashRegisterStep.CALCULATOR,
    val calcExpression: String = "",
    val calcResult: String = "",
    val calcHasError: Boolean = false,
    val isEvaluated: Boolean = false,
    val purchaseText: String = "",
    val receivedText: String = "",
    val currencyUnit: MoneyUnit = MoneyUnit.DIRHAM,
    val purchaseCentimes: Long = 0L,
    val receivedCentimes: Long = 0L,
    val changeCentimes: Long = 0L,
    val shortageCentimes: Long = 0L,
    val isExactAmount: Boolean = false,
    val isInsufficient: Boolean = false,
    val pieces: List<MoneyPiece> = emptyList(),
    val isPurchaseValid: Boolean = true,
    val isReceivedValid: Boolean = true
)

class CashRegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CashRegisterUiState())
    val uiState: StateFlow<CashRegisterUiState> = _uiState.asStateFlow()

    fun applyCalculatorKey(key: String) {
        _uiState.update { current ->
            var expr = current.calcExpression
            var hasErr = false
            var isEvaluated = current.isEvaluated

            when (key) {
                "C" -> {
                    expr = ""
                    isEvaluated = false
                }
                "⌫" -> {
                    isEvaluated = false
                    if (expr.isNotEmpty()) {
                        expr = expr.dropLast(1)
                    }
                }
                "=" -> {
                    val sanitized = expr.trimEnd('+', '−', '-', '×', '*', '÷', '/')
                    if (sanitized.isNotBlank()) {
                        val evaluated = MoneyMath.evaluate(sanitized)
                        if (evaluated != null && evaluated.signum() >= 0) {
                            expr = sanitized
                            isEvaluated = true
                        } else {
                            hasErr = true
                        }
                    }
                }
                "+", "−", "-", "×", "*", "÷", "/" -> {
                    isEvaluated = false
                    val op = when (key) {
                        "-", "−" -> "−"
                        "*", "×" -> "×"
                        "/", "÷" -> "÷"
                        else -> "+"
                    }
                    if (expr.isEmpty()) {
                        // Only allow minus if negative isn't blocked, but we disallow empty start ops except maybe zero
                    } else {
                        val lastChar = expr.last()
                        if (lastChar == '+' || lastChar == '−' || lastChar == '-' || lastChar == '×' || lastChar == '*' || lastChar == '÷' || lastChar == '/') {
                            expr = expr.dropLast(1) + op
                        } else {
                            expr += op
                        }
                    }
                }
                "." -> {
                    if (isEvaluated) {
                        expr = "0."
                        isEvaluated = false
                    } else {
                        // Check if current number segment already has a dot
                        val lastSegment = expr.split(Regex("[+\\−\\-×*÷/]")).lastOrNull() ?: ""
                        if (!lastSegment.contains('.')) {
                            expr = if (lastSegment.isEmpty()) expr + "0." else expr + "."
                        }
                    }
                }
                "(" -> {
                    if (isEvaluated) {
                        expr = "("
                        isEvaluated = false
                    } else if (expr.isEmpty()) {
                        expr = "("
                    } else {
                        val lastChar = expr.last()
                        if (lastChar.isDigit() || lastChar == ')') {
                            expr += "×("
                        } else {
                            expr += "("
                        }
                    }
                }
                ")" -> {
                    val openCount = expr.count { it == '(' }
                    val closeCount = expr.count { it == ')' }
                    if (openCount > closeCount && expr.isNotEmpty()) {
                        val lastChar = expr.last()
                        if (lastChar.isDigit() || lastChar == ')') {
                            expr += ")"
                            isEvaluated = false
                        }
                    }
                }
                else -> {
                    // Digits 0-9 and 00
                    if (isEvaluated) {
                        expr = if (key == "00") "0" else key
                        isEvaluated = false
                    } else {
                        expr += key
                    }
                }
            }

            // Real-time evaluation
            val evalTarget = expr.trimEnd('+', '−', '-', '×', '*', '÷', '/')
            val previewVal = if (evalTarget.isNotBlank()) MoneyMath.evaluate(evalTarget) else null
            val resultStr = if (previewVal != null && previewVal.signum() >= 0) {
                previewVal.stripTrailingZeros().toPlainString()
            } else ""

            val effectivePurchase = resultStr.ifBlank {
                if (evalTarget.isNotBlank() && MoneyMath.isValidExpression(evalTarget)) evalTarget else ""
            }

            computeState(
                step = current.step,
                calcExpression = expr,
                calcResult = resultStr,
                calcHasError = hasErr,
                purchaseText = effectivePurchase,
                receivedText = current.receivedText,
                currencyUnit = current.currencyUnit,
                isEvaluated = isEvaluated
            )
        }
    }

    fun goToChangeReturn() {
        _uiState.update { current ->
            // If current expression has something not yet evaluated
            var purchase = current.purchaseText
            if (purchase.isBlank() && current.calcExpression.isNotBlank()) {
                val evalTarget = current.calcExpression.trimEnd('+', '−', '-', '×', '*', '÷', '/')
                val eval = MoneyMath.evaluate(evalTarget)
                if (eval != null && eval.signum() >= 0) {
                    purchase = eval.stripTrailingZeros().toPlainString()
                }
            }
            computeState(
                step = CashRegisterStep.CHANGE_RETURN,
                calcExpression = current.calcExpression,
                calcResult = current.calcResult,
                calcHasError = false,
                purchaseText = purchase,
                receivedText = current.receivedText,
                currencyUnit = current.currencyUnit,
                isEvaluated = current.isEvaluated
            )
        }
    }

    fun goToCalculator() {
        _uiState.update { current ->
            current.copy(step = CashRegisterStep.CALCULATOR)
        }
    }

    fun setPurchaseText(text: String) {
        _uiState.update { current ->
            computeState(
                step = current.step,
                calcExpression = text,
                calcResult = "",
                calcHasError = false,
                purchaseText = text,
                receivedText = current.receivedText,
                currencyUnit = current.currencyUnit
            )
        }
    }

    fun setReceivedText(text: String) {
        _uiState.update { current ->
            computeState(
                step = current.step,
                calcExpression = current.calcExpression,
                calcResult = current.calcResult,
                calcHasError = current.calcHasError,
                purchaseText = current.purchaseText,
                receivedText = text,
                currencyUnit = current.currencyUnit,
                isEvaluated = current.isEvaluated
            )
        }
    }

    fun selectPresetReceived(dh: Long) {
        val currentUnit = _uiState.value.currencyUnit
        val amountText = if (currentUnit == MoneyUnit.DIRHAM) {
            dh.toString()
        } else {
            (dh * 20).toString()
        }
        setReceivedText(amountText)
    }

    fun toggleCurrency() {
        _uiState.update { current ->
            val nextUnit = if (current.currencyUnit == MoneyUnit.DIRHAM) MoneyUnit.RIAL else MoneyUnit.DIRHAM
            val convertedPurchase = MoneyMath.convertExpression(current.purchaseText, current.currencyUnit, nextUnit)
            val convertedReceived = MoneyMath.convertExpression(current.receivedText, current.currencyUnit, nextUnit)
            val convertedCalc = MoneyMath.convertExpression(current.calcExpression, current.currencyUnit, nextUnit)
            computeState(
                step = current.step,
                calcExpression = convertedCalc,
                calcResult = "",
                calcHasError = false,
                purchaseText = convertedPurchase,
                receivedText = convertedReceived,
                currencyUnit = nextUnit,
                isEvaluated = current.isEvaluated
            )
        }
    }

    fun clear() {
        _uiState.update { current ->
            CashRegisterUiState(currencyUnit = current.currencyUnit, step = CashRegisterStep.CALCULATOR)
        }
    }

    private fun computeState(
        step: CashRegisterStep,
        calcExpression: String,
        calcResult: String,
        calcHasError: Boolean,
        purchaseText: String,
        receivedText: String,
        currencyUnit: MoneyUnit,
        isEvaluated: Boolean = false
    ): CashRegisterUiState {
        val purchaseTrimmed = purchaseText.trim()
        val receivedTrimmed = receivedText.trim()

        val isPurchaseValid = purchaseTrimmed.isEmpty() || MoneyMath.isValidExpression(purchaseTrimmed)
        val isReceivedValid = receivedTrimmed.isEmpty() || MoneyMath.isValidExpression(receivedTrimmed)

        val purchaseCentimes = if (purchaseTrimmed.isNotEmpty() && isPurchaseValid) {
            MoneyMath.toCentimes(purchaseTrimmed, currencyUnit) ?: 0L
        } else 0L

        val receivedCentimes = if (receivedTrimmed.isNotEmpty() && isReceivedValid) {
            MoneyMath.toCentimes(receivedTrimmed, currencyUnit) ?: 0L
        } else 0L

        val hasBoth = purchaseTrimmed.isNotEmpty() && receivedTrimmed.isNotEmpty() && isPurchaseValid && isReceivedValid

        val changeCentimes: Long
        val shortageCentimes: Long
        val isExactAmount: Boolean
        val isInsufficient: Boolean
        val pieces: List<MoneyPiece>

        if (hasBoth && purchaseCentimes > 0L) {
            when {
                receivedCentimes > purchaseCentimes -> {
                    changeCentimes = receivedCentimes - purchaseCentimes
                    shortageCentimes = 0L
                    isExactAmount = false
                    isInsufficient = false
                    pieces = MoneyMath.breakdown(changeCentimes)
                }
                receivedCentimes == purchaseCentimes -> {
                    changeCentimes = 0L
                    shortageCentimes = 0L
                    isExactAmount = true
                    isInsufficient = false
                    pieces = emptyList()
                }
                else -> {
                    changeCentimes = 0L
                    shortageCentimes = purchaseCentimes - receivedCentimes
                    isExactAmount = false
                    isInsufficient = true
                    pieces = emptyList()
                }
            }
        } else {
            changeCentimes = 0L
            shortageCentimes = 0L
            isExactAmount = false
            isInsufficient = false
            pieces = emptyList()
        }

        return CashRegisterUiState(
            step = step,
            calcExpression = calcExpression,
            calcResult = calcResult,
            calcHasError = calcHasError,
            isEvaluated = isEvaluated,
            purchaseText = purchaseText,
            receivedText = receivedText,
            currencyUnit = currencyUnit,
            purchaseCentimes = purchaseCentimes,
            receivedCentimes = receivedCentimes,
            changeCentimes = changeCentimes,
            shortageCentimes = shortageCentimes,
            isExactAmount = isExactAmount,
            isInsufficient = isInsufficient,
            pieces = pieces,
            isPurchaseValid = isPurchaseValid,
            isReceivedValid = isReceivedValid
        )
    }
}
