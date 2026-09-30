package de.bgghome.webtrees.nativ.desk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonenTabelleTest {
    private fun z(x: String, name: String, jahr: Int = 0, ort: String = "") =
        TabellenZeile(x, name, name, "M", if (jahr > 0) "$jahr" else "", jahr * 365, jahr, ort, "", 0, 0, "", "", "")

    @Test
    fun filter() {
        assertTrue(filterPasst("", "", null))
        assertTrue(filterPasst("hann", "Hannover", null))
        assertTrue(filterPasst("!", " ", null)); assertFalse(filterPasst("!", "Celle", null))
        assertTrue(filterPasst("*", "Celle", null)); assertFalse(filterPasst("*", "", null))
        assertTrue(filterPasst("1800-1850", "1820", 1820)); assertFalse(filterPasst("1800-1850", "1851", 1851))
        assertTrue(filterPasst("-1850", "1700", 1700)); assertTrue(filterPasst("1800-", "1900", 1900))
        assertFalse(filterPasst("1800-", "", 0))
        // In Textspalten ist "1800-1850" ein gewoehnlicher Teiltext
        assertFalse(filterPasst("1800-1850", "Celle", null))
    }

    @Test
    fun sortierenLeeresUnten() {
        val zeilen = listOf(z("I3", "Cäsar", 1850), z("I1", "Anna"), z("I2", "Berta", 1800))
        assertEquals(listOf("I2", "I3", "I1"), tabelleAnwenden(zeilen, emptyMap(), Spalte.Geburt, false).map { it.xref })
        assertEquals(listOf("I3", "I2", "I1"), tabelleAnwenden(zeilen, emptyMap(), Spalte.Geburt, true).map { it.xref })
        assertEquals(listOf("I1", "I2", "I3"), tabelleAnwenden(zeilen, emptyMap(), Spalte.Id, false).map { it.xref })
        assertEquals(listOf("I2"), tabelleAnwenden(zeilen, mapOf(Spalte.Geburt to "-1820"), Spalte.Name, false).map { it.xref })
    }
}
