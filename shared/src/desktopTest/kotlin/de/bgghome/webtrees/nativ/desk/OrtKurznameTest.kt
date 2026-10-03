package de.bgghome.webtrees.nativ.desk

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** "Ortsnamen kürzen" im Buch: Kurzname aus der Ortsverwaltung vor dem ersten Namensteil. */
class OrtKurznameTest {
    @AfterTest fun leeren() { OrtsKurznamen.je = emptyMap() }

    @Test
    fun kurznameGehtVor() {
        OrtsKurznamen.je = mapOf("allenstein, ostpreußen" to "Allenst.")
        assertEquals("Allenst.", ortText("Allenstein, Ostpreußen", true))
        assertEquals("Allenst.", ortText("  Allenstein, Ostpreußen ", true))
        assertEquals("Celle", ortText("Celle, Niedersachsen, Deutschland", true), "ohne Kurzname der erste Teil")
        assertEquals("Allenstein, Ostpreußen", ortText("Allenstein, Ostpreußen", false), "ungekürzt bleibt der volle Name")
        assertEquals("", ortText(null, true))
    }
}
