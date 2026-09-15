package com.cash.guide.feature.savings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsFormattingTest {

    @Test
    fun testFormatSavingsMoney_ArabicRtl() {
        val result100k = formatSavingsMoney(100000L, isRtl = true)
        assertTrue("Must start with LTR mark", result100k.startsWith("\u200E"))
        assertTrue("Must end with درهم", result100k.endsWith("درهم"))
        assertTrue("Should contain non-breaking space", result100k.contains("\u00A0"))
        assertEquals("\u200E100\u00A0000\u200E درهم", result100k)

        val result5k = formatSavingsMoney(5000L, isRtl = true)
        assertEquals("\u200E5\u00A0000\u200E درهم", result5k)

        val result8800 = formatSavingsMoney(8800.0, isRtl = true)
        assertEquals("\u200E8\u00A0800\u200E درهم", result8800)
    }

    @Test
    fun testFormatSavingsMoney_FrenchLtr() {
        val result100k = formatSavingsMoney(100000L, isRtl = false)
        assertEquals("\u200E100\u00A0000\u200E DH", result100k)

        val result5k = formatSavingsMoney(5000L, isRtl = false)
        assertEquals("\u200E5\u00A0000\u200E DH", result5k)
    }

    @Test
    fun testFormatSavingsMoney_Zero() {
        val resultZeroAr = formatSavingsMoney(0L, isRtl = true)
        assertEquals("\u200E0\u200E درهم", resultZeroAr)

        val resultZeroFr = formatSavingsMoney(0L, isRtl = false)
        assertEquals("\u200E0\u200E DH", resultZeroFr)
    }
}
