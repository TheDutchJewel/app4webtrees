package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.Person
import kotlin.test.Test
import kotlin.test.assertEquals

/** Verwandtschaftswege an kleinen, erfundenen Ahnentafeln (Kekule-Nummer -> Kennung). */
class WegeTest {
    private fun tafel(vararg e: Pair<Long, String>) = e.associate { (n, x) -> n to AhnenEintrag(Person(xref = x, name = x), false) }

    @Test
    fun cousinsUeberEinPaar() {
        // A und B haben dieselben Grosseltern vaeterlicherseits (G1 und G2): Cousins 1. Grades, ein Weg
        val a = tafel(1L to "A", 2L to "VA", 3L to "MA", 4L to "G1", 5L to "G2")
        val b = tafel(1L to "B", 2L to "VB", 3L to "MB", 4L to "G1", 5L to "G2")
        val w = wegeFinden(a, b)
        assertEquals(1, w.size); assertEquals(4L, w[0].na); assertEquals(4L, w[0].nb); assertEquals(true, w[0].paar)
    }

    @Test
    fun doppeltVerwandt() {
        // Ueber die Vaeter Cousins 1. Grades (Grosseltern G1/G2), ueber die Muetter Cousins 2. Grades (Urgrosseltern U1/U2)
        val a = tafel(1L to "A", 2L to "VA", 3L to "MA", 4L to "G1", 5L to "G2", 6L to "X", 7L to "Y", 12L to "U1", 13L to "U2")
        val b = tafel(1L to "B", 2L to "VB", 3L to "MB", 4L to "G1", 5L to "G2", 6L to "Z", 7L to "W", 12L to "U1", 13L to "U2")
        val w = wegeFinden(a, b)
        assertEquals(2, w.size)
        assertEquals(4L to 4L, w[0].na to w[0].nb)
        assertEquals(12L to 12L, w[1].na to w[1].nb)
    }

    @Test
    fun hoehererVorfahrZaehltNichtExtra() {
        // Die Eltern der gemeinsamen Grosseltern sind auch gemeinsam - das ist kein zweiter Weg
        val a = tafel(1L to "A", 2L to "VA", 4L to "G1", 5L to "G2", 8L to "O1", 9L to "O2")
        val b = tafel(1L to "B", 2L to "VB", 4L to "G1", 5L to "G2", 8L to "O1", 9L to "O2")
        assertEquals(1, wegeFinden(a, b).size)
    }
}
