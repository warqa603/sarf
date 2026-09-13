package com.cash.guide.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class MoneyUnit(val arabicName: String, val centimesPerUnit: BigDecimal) {
    DIRHAM("درهم", BigDecimal("100")),
    RIAL("ريال", BigDecimal("5"));
}

data class Denomination(
    val valueCentimes: Long,
    val label: String,
    val assetPath: String?
)

data class MoneyPiece(val denomination: Denomination, val count: Long)

object MoneyMath {
    val denominations = listOf(
        Denomination(20_000, "200 درهم", "sarfpic/200dh.png"),
        Denomination(10_000, "100 درهم", "sarfpic/100dh.png"),
        Denomination(5_000, "50 درهم", "sarfpic/50dh.png"),
        Denomination(2_000, "20 درهم", "sarfpic/20dh.png"),
        Denomination(1_000, "10 درهم", "sarfpic/10dh.png"),
        Denomination(500, "5 دراهم", "sarfpic/5dh.png"),
        Denomination(200, "درهمان", "sarfpic/2dh.png"),
        Denomination(100, "درهم", "sarfpic/1dh.png"),
        Denomination(50, "50 سنتيم", "sarfpic/50centime.png"),
        Denomination(20, "20 سنتيم", "sarfpic/20centime.png"),
        Denomination(10, "10 سنتيم", "sarfpic/10centime.png"),
        Denomination(5, "ريال واحد", null)
    )

    fun evaluate(expression: String): BigDecimal? = runCatching {
        Parser(expression).parse()
    }.getOrNull()

    fun toCentimes(expression: String, unit: MoneyUnit): Long? {
        val value = evaluate(expression) ?: return null
        if (value.signum() < 0) return null
        return runCatching {
            value.multiply(unit.centimesPerUnit)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()
    }

    fun fromCentimes(centimes: Long, unit: MoneyUnit): String =
        BigDecimal.valueOf(centimes)
            .divide(unit.centimesPerUnit, 2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()

    fun convertExpression(expression: String, from: MoneyUnit, to: MoneyUnit): String {
        if (expression.isBlank() || from == to) return expression
        val cents = toCentimes(expression, from) ?: return expression
        return fromCentimes(cents, to)
    }

    fun breakdown(totalCentimes: Long): List<MoneyPiece> {
        var remaining = totalCentimes.coerceAtLeast(0)
        return buildList {
            denominations.forEach { denomination ->
                val count = remaining / denomination.valueCentimes
                if (count > 0) {
                    add(MoneyPiece(denomination, count))
                    remaining %= denomination.valueCentimes
                }
            }
        }
    }

    fun isValidExpression(expression: String): Boolean {
        if (expression.isBlank()) return true
        val result = evaluate(expression) ?: return false
        return result.signum() >= 0
    }

    private class Parser(raw: String) {
        private val text = run {
            var s = raw
                .replace('×', '*')
                .replace('÷', '/')
                .replace('−', '-') // Unicode minus U+2212
                .replace('—', '-') // Em-dash
                .replace('–', '-') // En-dash
                .replace(',', '.')
                .replace(" ", "")
            // Automatically balance open parentheses for live preview evaluation
            val openCount = s.count { it == '(' }
            val closeCount = s.count { it == ')' }
            if (openCount > closeCount) {
                s += ")".repeat(openCount - closeCount)
            }
            s
        }
        private var index = 0

        fun parse(): BigDecimal {
            require(text.isNotBlank())
            val value = parseExpression()
            require(index == text.length)
            return value
        }

        private fun parseExpression(): BigDecimal {
            var value = parseTerm()
            while (index < text.length) {
                value = when (text[index]) {
                    '+' -> { index++; value.add(parseTerm()) }
                    '-' -> { index++; value.subtract(parseTerm()) }
                    else -> return value
                }
            }
            return value
        }

        private fun parseTerm(): BigDecimal {
            var value = parseFactor()
            while (index < text.length) {
                value = when (text[index]) {
                    '*' -> { index++; value.multiply(parseFactor()) }
                    '/' -> {
                        index++
                        val divisor = parseFactor()
                        require(divisor.signum() != 0)
                        value.divide(divisor, 8, RoundingMode.HALF_UP).stripTrailingZeros()
                    }
                    '(' -> {
                        // Implicit multiplication: e.g. 2(3+4)
                        value.multiply(parseFactor())
                    }
                    else -> return value
                }
            }
            return value
        }

        private fun parseFactor(): BigDecimal {
            var negative = false
            while (index < text.length && (text[index] == '+' || text[index] == '-')) {
                if (text[index] == '-') negative = !negative
                index++
            }
            if (index < text.length && text[index] == '(') {
                index++ // skip '('
                val value = parseExpression()
                if (index < text.length && text[index] == ')') {
                    index++ // skip ')'
                }
                return if (negative) value.negate() else value
            }
            val start = index
            var dotSeen = false
            while (index < text.length) {
                val char = text[index]
                if (char.isDigit()) index++
                else if (char == '.' && !dotSeen) { dotSeen = true; index++ }
                else break
            }
            require(start < index)
            val number = text.substring(start, index).let(::BigDecimal)
            return if (negative) number.negate() else number
        }
    }
}
