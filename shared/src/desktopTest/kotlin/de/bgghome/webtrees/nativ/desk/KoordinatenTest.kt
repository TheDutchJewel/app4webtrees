package de.bgghome.webtrees.nativ.desk

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Koordinaten eintippen und aus der Zwischenablage uebernehmen (Ortsdaten bearbeiten). */
class KoordinatenTest {
    private fun nah(erwartet: Double, ist: Double?) = assertTrue(ist != null && abs(erwartet - ist) < 1e-4, "erwartet $erwartet, ist $ist")

    @Test
    fun einzelneKoordinate() {
        nah(53.778417, grad("53.778417"))
        nah(53.778417, grad("53,778417"))
        nah(-8.5, grad("-8.5"))
        nah(53.77861, grad("53° 46′ 43″ N"))
        nah(53.77861, grad("53 46 43"))
        nah(-20.48, grad("20° 28′ 48″ W"))
        nah(-34.6, grad("34.6 S"))
        nah(-34.6, grad("34,6 Z"))
        nah(20.48, grad("20° 28′ 48″ O"))
        nah(53.778, grad("N53.778"))
        assertNull(grad(""))
        assertNull(grad("Celle"))
    }

    @Test
    fun paarAusZwischenablage() {
        val a = koordinatenPaar("53.778417, 20.480111")!!; nah(53.778417, a.first); nah(20.480111, a.second)
        val b = koordinatenPaar("53,7784; 20,4801")!!; nah(53.7784, b.first); nah(20.4801, b.second)
        val c = koordinatenPaar("53° 46′ 42″ N, 20° 28′ 48″ O")!!; nah(53.778333, c.first); nah(20.48, c.second)
        val d = koordinatenPaar("34° 36′ S 58° 22′ W")!!; nah(-34.6, d.first); nah(-58.366667, d.second)
        val e = koordinatenPaar("-33.86 151.21")!!; nah(-33.86, e.first); nah(151.21, e.second)
        assertNull(koordinatenPaar("Hallo"))
        assertNull(koordinatenPaar("53.7"))
        assertEquals(null, koordinatenPaar("200, 300"))
    }
}
