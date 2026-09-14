package com.cash.guide.domain

data class LedgerEntry(
    val id: Long,
    val title: String = "",
    val expression: String = ""
) {
    val isEmpty: Boolean get() = title.isBlank() && expression.isBlank()
    val isPopulated: Boolean get() = !isEmpty
}

object JournalLedgerManager {

    /**
     * Normalizes a list of ledger rows to guarantee:
     * 1. The list always ends with exactly one empty trailing row.
     * 2. Intermediate empty rows before the last row are preserved if user left them, or collapsed if redundant.
     * 3. An empty list is initialized with exactly one empty row.
     * 4. Id stability is preserved.
     */
    fun normalize(
        rows: List<LedgerEntry>,
        nextIdGenerator: () -> Long
    ): Pair<List<LedgerEntry>, Long?> {
        if (rows.isEmpty()) {
            val newId = nextIdGenerator()
            return listOf(LedgerEntry(id = newId)) to newId
        }

        // Drop trailing empty rows except the first one if there are multiple at the end
        var lastPopulatedIndex = -1
        for (i in rows.indices.reversed()) {
            if (rows[i].isPopulated) {
                lastPopulatedIndex = i
                break
            }
        }

        val result = mutableListOf<LedgerEntry>()
        if (lastPopulatedIndex == -1) {
            // All rows are empty -> keep exactly one
            result.add(rows.first())
            return result to null
        }

        for (i in 0..lastPopulatedIndex) {
            result.add(rows[i])
        }

        // Check if there was an empty row immediately after last populated
        var appendedId: Long? = null
        if (lastPopulatedIndex + 1 < rows.size && rows[lastPopulatedIndex + 1].isEmpty) {
            result.add(rows[lastPopulatedIndex + 1])
        } else {
            // Append a new trailing empty row
            val newId = nextIdGenerator()
            result.add(LedgerEntry(id = newId))
            appendedId = newId
        }

        return result to appendedId
    }

    /**
     * Display-only formatting for mathematical numbers and expressions with French thousand separators (non-breaking space).
     * Keeps stored expressions clean and untouched.
     */
    fun formatDisplayExpression(raw: String): String {
        if (raw.isBlank()) return ""
        val operators = setOf('+', '−', '×', '÷', '-', '*', '/')
        val sb = StringBuilder()
        val numBuffer = StringBuilder()

        fun flushNumber() {
            if (numBuffer.isNotEmpty()) {
                val numStr = numBuffer.toString()
                val parts = numStr.split('.')
                val intPart = parts[0]
                val formattedInt = formatThousands(intPart)
                sb.append(formattedInt)
                if (parts.size > 1) {
                    sb.append('.').append(parts[1])
                }
                numBuffer.clear()
            }
        }

        for (c in raw) {
            if (c.isDigit() || c == '.') {
                numBuffer.append(c)
            } else if (c in operators) {
                flushNumber()
                if (sb.isNotEmpty() && sb.last() != ' ' && sb.last() != '(') sb.append(' ')
                sb.append(c)
                sb.append(' ')
            } else if (c == '(') {
                flushNumber()
                if (sb.isNotEmpty() && (sb.last().isDigit() || sb.last() == ')')) {
                    sb.append(" × ")
                }
                sb.append('(')
            } else if (c == ')') {
                flushNumber()
                sb.append(')')
            } else {
                numBuffer.append(c)
            }
        }
        flushNumber()
        return sb.toString().trim()
    }

    /**
     * Display-only French number grouping using non-breaking space (\u00A0).
     * Used for inactive ledger row amounts.
     * Examples:
     * - "1200" -> "1\u00A0200"
     * - "5000" -> "5\u00A0000"
     * - "2600" -> "2\u00A0600"
     * - "8800.5" -> "8\u00A0800.5"
     */
    fun formatFrenchNumber(raw: String): String {
        if (raw.isBlank()) return ""
        val parts = raw.split('.')
        val intPart = parts[0]
        val formattedInt = formatThousands(intPart)
        return if (parts.size > 1) {
            "$formattedInt.${parts[1]}"
        } else {
            formattedInt
        }
    }

    private fun formatThousands(digits: String): String {
        if (digits.length <= 3) return digits
        val isNegative = digits.startsWith('-')
        val cleanDigits = if (isNegative) digits.substring(1) else digits
        val sb = StringBuilder()
        val len = cleanDigits.length
        for (i in 0 until len) {
            sb.append(cleanDigits[i])
            val remaining = len - 1 - i
            if (remaining > 0 && remaining % 3 == 0) {
                sb.append('\u00A0') // Non-breaking space
            }
        }
        return if (isNegative) "-$sb" else sb.toString()
    }

    fun formatTotal(centimes: Long, unit: MoneyUnit): String {
        val raw = MoneyMath.fromCentimes(centimes, unit)
        return formatFrenchNumber(raw)
    }
}
