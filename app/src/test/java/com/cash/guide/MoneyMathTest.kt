package com.cash.guide

import com.cash.guide.domain.MoneyMath
import com.cash.guide.domain.MoneyUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyMathTest {

    @Test
    fun expressionHonorsOperatorPrecedence() {
        assertEquals("880", MoneyMath.evaluate("600 + 250 + 30")?.toPlainString())
        assertEquals("14", MoneyMath.evaluate("2 + 3 × 4")?.toPlainString())
        assertEquals("10", MoneyMath.evaluate("20 ÷ 2")?.toPlainString())
        assertEquals("14", MoneyMath.evaluate("20 - 2 × 3")?.toPlainString())
        assertEquals("14", MoneyMath.evaluate("20 − 2 × 3")?.toPlainString()) // Keypad Unicode minus (U+2212)
        assertEquals("2.5", MoneyMath.evaluate("5 ÷ 2")?.toPlainString())
    }

    @Test
    fun keypadSubtractionWithUnicodeMinusEvaluatesCorrectly() {
        // Exact keypad expression test as requested
        val result = MoneyMath.evaluate("20 − 2 × 3")
        assertEquals("14", result?.toPlainString())
    }

    @Test
    fun validationIdentifiesIncompleteAndInvalidExpressions() {
        assertTrue(MoneyMath.isValidExpression(""))
        assertTrue(MoneyMath.isValidExpression("   "))
        assertTrue(MoneyMath.isValidExpression("1200"))
        assertTrue(MoneyMath.isValidExpression("20 − 2 × 3"))
        assertTrue(MoneyMath.isValidExpression("5000 + 2600"))

        assertFalse(MoneyMath.isValidExpression("1200+"))
        assertFalse(MoneyMath.isValidExpression("1200−"))
        assertFalse(MoneyMath.isValidExpression("1200×"))
        assertFalse(MoneyMath.isValidExpression("1200÷"))
        assertFalse(MoneyMath.isValidExpression("12..5"))
        assertFalse(MoneyMath.isValidExpression("abc"))
        assertFalse(MoneyMath.isValidExpression("10 ÷ 0"))
        assertFalse(MoneyMath.isValidExpression("-50")) // Negative value not allowed for money amount
    }

    @Test
    fun expressionHandlesDivisionByZeroGracefully() {
        assertNull(MoneyMath.evaluate("10 ÷ 0"))
        assertNull(MoneyMath.evaluate("5 / 0"))
    }

    @Test
    fun expressionHandlesInvalidSyntaxGracefully() {
        assertNull(MoneyMath.evaluate(""))
        assertNull(MoneyMath.evaluate("   "))
        assertNull(MoneyMath.evaluate("abc"))
        assertNull(MoneyMath.evaluate("12..5"))
        assertNull(MoneyMath.evaluate("+"))
    }

    @Test
    fun dirhamAndRialFixedConversionRule() {
        // 1 Dirham = 20 Rial
        // 50 Dirham = 1000 Rial
        assertEquals(5_000L, MoneyMath.toCentimes("50", MoneyUnit.DIRHAM))
        assertEquals(5_000L, MoneyMath.toCentimes("1000", MoneyUnit.RIAL))
        assertEquals("1000", MoneyMath.convertExpression("50", MoneyUnit.DIRHAM, MoneyUnit.RIAL))
        assertEquals("50", MoneyMath.convertExpression("1000", MoneyUnit.RIAL, MoneyUnit.DIRHAM))

        // 880 Dirham = 17600 Rial
        assertEquals(88_000L, MoneyMath.toCentimes("880", MoneyUnit.DIRHAM))
        assertEquals(88_000L, MoneyMath.toCentimes("17600", MoneyUnit.RIAL))
        assertEquals("17600", MoneyMath.convertExpression("600 + 250 + 30", MoneyUnit.DIRHAM, MoneyUnit.RIAL))

        // 8800 Rial = 440 Dirham
        assertEquals(44_000L, MoneyMath.toCentimes("8800", MoneyUnit.RIAL))
        assertEquals("440", MoneyMath.convertExpression("8800", MoneyUnit.RIAL, MoneyUnit.DIRHAM))

        // 14000 Rial = 700 Dirham
        assertEquals(70_000L, MoneyMath.toCentimes("14000", MoneyUnit.RIAL))
        assertEquals("700", MoneyMath.convertExpression("14000", MoneyUnit.RIAL, MoneyUnit.DIRHAM))
    }

    @Test
    fun fromCentimesFormatsCleanly() {
        assertEquals("50", MoneyMath.fromCentimes(5000L, MoneyUnit.DIRHAM))
        assertEquals("1000", MoneyMath.fromCentimes(5000L, MoneyUnit.RIAL))
        assertEquals("0", MoneyMath.fromCentimes(0L, MoneyUnit.DIRHAM))
        assertEquals("0", MoneyMath.fromCentimes(0L, MoneyUnit.RIAL))
        assertEquals("0.5", MoneyMath.fromCentimes(50L, MoneyUnit.DIRHAM))
        assertEquals("10", MoneyMath.fromCentimes(50L, MoneyUnit.RIAL))
    }

    @Test
    fun breakdownRebuildsExactTotals() {
        listOf(
            0L,
            5L,          // 1 rial
            15L,         // 3 rial (10c + 5c)
            75L,         // 15 rial (50c + 20c + 5c)
            100L,        // 1 dirham
            200L,        // 2 dirhams
            500L,        // 5 dirhams
            1_000L,      // 10 dirhams
            2_000L,      // 20 dirhams
            5_000L,      // 50 dirhams
            10_000L,     // 100 dirhams
            20_000L,     // 200 dirhams
            44_000L,     // 440 dirhams / 8800 rial
            70_000L,     // 700 dirhams / 14000 rial
            88_000L      // 880 dirhams / 17600 rial
        ).forEach { total ->
            val pieces = MoneyMath.breakdown(total)
            val sum = pieces.sumOf { it.denomination.valueCentimes * it.count }
            assertEquals("Breakdown sum must match total for $total centimes", total, sum)
        }
    }

    @Test
    fun breakdownPreservesOneRialRemainder() {
        val pieces = MoneyMath.breakdown(5L)
        assertEquals(1, pieces.size)
        assertEquals("ريال واحد", pieces[0].denomination.label)
        assertEquals(1L, pieces[0].count)
    }

    @Test
    fun expressionHandlesParenthesesAndImplicitMultiplication() {
        assertEquals("100", MoneyMath.evaluate("(20 + 30) × 2")?.toPlainString())
        assertEquals("60", MoneyMath.evaluate("100 − (15 + 25)")?.toPlainString())
        assertEquals("16", MoneyMath.evaluate("2(5 + 3)")?.toPlainString())
        assertEquals("70", MoneyMath.evaluate("(50 + 20")?.toPlainString()) // Auto-closes open parentheses for preview
        assertEquals("140", MoneyMath.evaluate("(50 + 20) * 2")?.toPlainString())
        assertEquals("6", MoneyMath.evaluate("(10 + 20) ÷ (2 + 3)")?.toPlainString())
    }
}

