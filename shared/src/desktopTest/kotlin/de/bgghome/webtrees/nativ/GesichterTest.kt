package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.api.Bereich
import de.bgghome.webtrees.nativ.data.Gesichter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GesichterTest {
    @Test
    fun breitesBildBekommtRandObenUndUnten() {
        // 1310×715 (Hochzeit 1924) in ein Feld 1000×1000: volle Breite, oben und unten Rand
        val l = Gesichter.eingepasst(1000f, 1000f, 1310, 715)
        assertEquals(0f, l.links)
        assertEquals(1000f, l.breite)
        assertEquals(545.8f, l.hoehe, 0.1f)
        assertEquals((1000f - l.hoehe) / 2, l.oben, 0.01f)
    }

    @Test
    fun trefferNimmtDenKleinstenRahmen() {
        val gross = Bereich(0.1, 0.1, 0.5, 0.5, name = "Mutter")
        val klein = Bereich(0.2, 0.2, 0.1, 0.1, name = "Kind")
        assertEquals(1, Gesichter.treffer(listOf(gross, klein), 0.25, 0.25))
        assertEquals(0, Gesichter.treffer(listOf(gross, klein), 0.5, 0.5))
        assertNull(Gesichter.treffer(listOf(gross, klein), 0.9, 0.9))
    }

    @Test
    fun gezogenerRahmenInJederRichtungUndBeschnitten() {
        val b = Gesichter.gezogen(0.5, 0.6, 0.2, -0.1)!!
        assertEquals(0.2, b.x, 1e-9); assertEquals(0.0, b.y, 1e-9)
        assertEquals(0.3, b.w, 1e-9); assertEquals(0.6, b.h, 1e-9)
        // ein Klick ist kein Rahmen
        assertNull(Gesichter.gezogen(0.5, 0.5, 0.503, 0.6))
    }

    @Test
    fun reihenfolgeReiheFuerReiheVonLinks() {
        // hintere Reihe (oben) rechts, links; vordere Reihe (unten) rechts, links - leicht versetzt wie auf echten Fotos
        val bereiche = listOf(
            Bereich(0.6, 0.20, 0.05, 0.1), Bereich(0.1, 0.22, 0.05, 0.1),
            Bereich(0.7, 0.60, 0.05, 0.1), Bereich(0.2, 0.58, 0.05, 0.1),
        )
        assertEquals(listOf(1, 0, 3, 2), Gesichter.reihenfolge(bereiche))
    }

    @Test
    fun pixelAusDerTestdatei() {
        val b = Gesichter.ausPixeln(108, 168, 71, 104, 1310, 715)
        assertEquals(108.0 / 1310, b.x, 1e-9)
        assertEquals(104.0 / 715, b.h, 1e-9)
        assertEquals("", b.name)
    }

    @Test
    fun verschiebenBleibtImBildUndGroesseHatUntergrenze() {
        val b = Bereich(0.8, 0.8, 0.1, 0.1)
        val v = Gesichter.verschoben(b, 0.5, -0.9)
        assertEquals(0.9, v.x, 1e-9); assertEquals(0.0, v.y, 1e-9); assertEquals(0.1, v.w, 1e-9)
        val g = Gesichter.vergroessert(b, 0.7, 1.4)
        assertEquals(Gesichter.MINDEST, g.w, 1e-9); assertEquals(0.2, g.h, 1e-9)
    }
}
