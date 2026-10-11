package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.data.Kalender
import de.bgghome.webtrees.nativ.data.Kalender.Datum
import de.bgghome.webtrees.nativ.data.Kirchenjahr
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KalenderTest {
    @Test
    fun tageszahlUndWochentag() {
        assertEquals(2451545, Datum(2000, 1, 1).jd())
        assertEquals(6, Kalender.wochentag(Datum(1924, 5, 17).jd()))      // Samstag
        assertEquals(5, Kalender.wochentag(Datum(1582, 10, 15).jd()))
        // Kalenderwechsel: 4.10.1582 julianisch folgt direkt 15.10.1582 gregorianisch
        assertEquals(Datum(1582, 10, 4, true).jd() + 1, Datum(1582, 10, 15).jd())
        assertEquals(Datum(1700, 2, 18, true).jd() + 1, Datum(1700, 3, 1).jd())
    }

    @Test
    fun umrechnenUndLesen() {
        assertEquals(Datum(1700, 2, 27, false), Kalender.umrechnen(Datum(1700, 2, 17, true)))
        assertEquals(Datum(1924, 5, 17), Kalender.lesen("17.05.1924"))
        assertEquals(Datum(1924, 5, 17), Kalender.lesen("17 MAY 1924"))
        assertEquals(Datum(1924, 5, 17), Kalender.lesen("1924-05-17"))
        assertEquals(Datum(1650, 3, 1, true), Kalender.lesen("@#DJULIAN@ 1 MAR 1650"))
        assertNull(Kalender.lesen("31.02.1900"))
        assertNull(Kalender.lesen("1850"))
        assertTrue(Kalender.schaltjahr(1700, true)); assertTrue(!Kalender.schaltjahr(1700, false))
    }

    @Test
    fun abstandUndGeburt() {
        val a = Kalender.abstand(Datum(1897, 3, 2), Datum(1969, 8, 15))
        assertEquals(Triple(72, 5, 13), Triple(a.jahre, a.monate, a.tage))
        assertEquals(Datum(1969, 8, 15).jd() - Datum(1897, 3, 2).jd(), a.tageGesamt)
        assertEquals(Datum(1787, 5, 8), Kalender.geburt(Datum(1850, 9, 10), 63, 4, tage = 2))
        // Monatsende: 31.03. minus 1 Monat -> 28./29.02.
        assertEquals(Datum(1900, 2, 28), Kalender.geburt(Datum(1900, 3, 31), 0, 1))
        assertEquals(Datum(1900, 2, 21), Kalender.geburt(Datum(1900, 3, 31), 0, 1, wochen = 1))
    }

    @Test
    fun ostern() {
        assertEquals(Datum(2024, 3, 31), Kirchenjahr.ostern(2024, false))
        assertEquals(Datum(1924, 4, 20), Kirchenjahr.ostern(1924, false))
        assertEquals(Datum(2000, 4, 23), Kirchenjahr.ostern(2000, false))
        // Orthodoxe Ostern 2024: 5. Mai gregorianisch = 22. April julianisch
        assertEquals(Datum(2024, 4, 22, true), Kirchenjahr.ostern(2024, true))
        assertEquals(Datum(2024, 5, 5), Kalender.umrechnen(Kirchenjahr.ostern(2024, true)))
    }

    @Test
    fun kirchenjahrSuchenUndBenennen() {
        val laetare = Kirchenjahr.suchen("Dominica Laetare", 1924, false).single()
        assertEquals(Datum(1924, 3, 30), laetare.datum)
        assertEquals(Datum(1924, 3, 23), Kirchenjahr.suchen("Oculi", 1924, false).single().datum)
        // Trinitatis 1924 = 15.06., der 3. Sonntag danach 06.07.
        assertEquals(Datum(1924, 7, 6), Kirchenjahr.suchen("3. Sonntag nach Trinitatis", 1924, false).single().datum)
        assertEquals(Datum(1924, 7, 6), Kirchenjahr.suchen("Dom. III p. Trin.", 1924, false).single().datum)
        assertTrue(Kirchenjahr.benennen(Datum(1924, 3, 30)).first().startsWith("Lätare"))
        assertEquals("Mittwoch nach Okuli", Kirchenjahr.benennen(Datum(1924, 3, 26)).single())
        // 1. Advent 1924: 30.11.
        assertEquals(Datum(1924, 11, 30), Kirchenjahr.suchen("1. Advent", 1924, false).single().datum)
        // katholisch: Sonntage nach Pfingsten
        assertEquals(Datum(1924, 7, 6), Kirchenjahr.suchen("4. Sonntag nach Pfingsten", 1924, false, katholisch = true).single().datum)
    }
}
