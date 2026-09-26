package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.api.WtClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlTest {
    @Test
    fun addsHttpsAndTrims() {
        assertEquals("https://example.org/webtrees", WtClient.normalizeBaseUrl(" example.org/webtrees/ "))
        assertEquals("https://example.org/webtrees", WtClient.normalizeBaseUrl("https://example.org/webtrees/index.php?route=/tree/x"))
        assertEquals("https://example.org", WtClient.normalizeBaseUrl("HTTPS://example.org"))
        assertEquals("", WtClient.normalizeBaseUrl("  "))
    }

    @Test
    fun detectsCleartext() {
        assertTrue(WtClient.isCleartext("http://example.org/webtrees"))
        assertTrue(WtClient.isCleartext("  HTTP://example.org"))
        assertFalse(WtClient.isCleartext("https://example.org"))
        assertFalse(WtClient.isCleartext("example.org/webtrees"))
        assertFalse(WtClient.isCleartext("httpx.example.org"))
    }

    @Test
    fun adresseAusZwischenablage() {
        assertEquals("http://192.168.178.109:8095", WtClient.adresseAusText("http://192.168.178.109:8095/"))
        assertEquals("https://example.org/webtrees", WtClient.adresseAusText("  https://example.org/webtrees/index.php?route=%2Ftree%2Fx\n"))
        assertEquals(null, WtClient.adresseAusText(null))
        assertEquals(null, WtClient.adresseAusText(""))
        assertEquals(null, WtClient.adresseAusText("example.org"))
        assertEquals(null, WtClient.adresseAusText("Hallo https://example.org"))
        assertEquals(null, WtClient.adresseAusText("https://example.org\nhttps://zwei.org"))
        assertEquals(null, WtClient.adresseAusText("https://"))
        assertEquals(null, WtClient.adresseAusText("ftp://example.org"))
    }
}
