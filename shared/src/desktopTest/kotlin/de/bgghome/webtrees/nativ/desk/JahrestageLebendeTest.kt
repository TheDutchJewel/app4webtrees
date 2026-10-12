package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.Anniversary
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.ui.vonLebenden
import kotlin.test.Test
import kotlin.test.assertEquals

/** Jahrestage nur von Lebenden (Issue 17): Todestage nie, Geburtstage Lebender, Hochzeitstage nur mit beiden lebend. */
class JahrestageLebendeTest {
    private fun p(x: String, tot: Boolean) = Person(xref = x, name = x, isDead = tot)

    @Test
    fun filter() {
        val liste = listOf(
            Anniversary(tag = "BIRT", xref = "I1", person = p("I1", false)),
            Anniversary(tag = "BIRT", xref = "I2", person = p("I2", true)),
            Anniversary(tag = "DEAT", xref = "I3", person = p("I3", true)),
            Anniversary(tag = "MARR", xref = "F1", couple = listOf(p("I4", false), p("I5", false))),
            Anniversary(tag = "MARR", xref = "F2", couple = listOf(p("I6", false), p("I7", true))),
            Anniversary(tag = "BIRT", xref = "I8"),
        )
        assertEquals(listOf("I1", "F1"), liste.filter { it.vonLebenden() }.map { it.xref })
    }
}
