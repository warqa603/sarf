package com.cash.guide

import com.cash.guide.data.db.ChecklistItemEntity
import com.cash.guide.domain.ChecklistLinkHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistLinkHelperTest {

    @Test
    fun createAndParseDeepLinkWithArabicItems() {
        val items = listOf(
            ChecklistItemEntity(
                id = "1",
                checklistId = "cl-1",
                text = "مطيشة",
                isChecked = false,
                position = 0,
                createdAtEpochMs = 1000L
            ),
            ChecklistItemEntity(
                id = "2",
                checklistId = "cl-1",
                text = "بطاطا",
                isChecked = true,
                position = 1,
                createdAtEpochMs = 1001L
            ),
            ChecklistItemEntity(
                id = "3",
                checklistId = "cl-1",
                text = "حليب",
                isChecked = false,
                position = 2,
                createdAtEpochMs = 1002L
            )
        )

        val link = ChecklistLinkHelper.createDeepLink("تقضية الجمعة", items)
        assertTrue("Link should start with HTTPS base", link.startsWith("https://warqa603.github.io/sarf/checklist/?d="))

        val parsed = ChecklistLinkHelper.parseDeepLink(link)
        assertNotNull(parsed)
        assertEquals("تقضية الجمعة", parsed!!.title)
        assertEquals(3, parsed.items.size)
        assertEquals("مطيشة" to false, parsed.items[0])
        assertEquals("بطاطا" to true, parsed.items[1])
        assertEquals("حليب" to false, parsed.items[2])
    }

    @Test
    fun parseLegacyHttpsLink() {
        val legacyLink = "https://sarf.app/checklist?t=Marjane&i=Pain"
        val parsed = ChecklistLinkHelper.parseDeepLink(legacyLink)
        assertNotNull(parsed)
        assertEquals("Marjane", parsed!!.title)
        assertEquals(1, parsed.items.size)
        assertEquals("Pain" to false, parsed.items[0])
    }

    @Test
    fun parseCustomSchemeLink() {
        val link = "sarf://checklist?t=Souk&i=Pain&i=Lait"
        val parsed = ChecklistLinkHelper.parseDeepLink(link)
        assertNotNull(parsed)
        assertEquals("Souk", parsed!!.title)
        assertEquals(2, parsed.items.size)
        assertEquals("Pain" to false, parsed.items[0])
        assertEquals("Lait" to false, parsed.items[1])
    }

    @Test
    fun parsePercentEncodedAndPaddedLink() {
        // Sample JSON: {"t":"سوق الأحد","i":["نعناع","سكر"],"c":[0,1]}
        val rawJson = """{"t":"سوق الأحد","i":["نعناع","سكر"],"c":[0,1]}"""
        val b64 = java.util.Base64.getUrlEncoder().encodeToString(rawJson.toByteArray(Charsets.UTF_8))
        // Simulate browser encoding '=' as '%3D'
        val percentEncoded = b64.replace("=", "%3D")
        val link = "https://warqa603.github.io/sarf/checklist?d=$percentEncoded"
        
        val parsed = ChecklistLinkHelper.parseDeepLink(link)
        assertNotNull(parsed)
        assertEquals("سوق الأحد", parsed!!.title)
        assertEquals(2, parsed.items.size)
        assertEquals("نعناع" to false, parsed.items[0])
        assertEquals("سكر" to true, parsed.items[1])
    }

    @Test
    fun parseCustomSchemeWithDataParam() {
        val rawJson = """{"t":"مرجان","i":["زيت"],"c":[0]}"""
        val b64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(rawJson.toByteArray(Charsets.UTF_8))
        val link = "sarf://checklist?d=$b64"
        val parsed = ChecklistLinkHelper.parseDeepLink(link)
        assertNotNull(parsed)
        assertEquals("مرجان", parsed!!.title)
        assertEquals(1, parsed.items.size)
        assertEquals("زيت" to false, parsed.items[0])
    }
}
