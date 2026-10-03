package de.bgghome.webtrees.nativ.pruefung

import de.bgghome.webtrees.nativ.api.PlaceSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrtsDublettenTest {
    private fun o(name: String, n: Int, gov: String? = null, lat: Double? = null, lng: Double? = null) =
        PlaceSummary(name, events = n, gov = gov, lat = lat, lng = lng)

    @Test
    fun findetDieGruende() {
        val orte = listOf(
            o("Bienenbüttel, Uelzen, Niedersachsen, Deutschland", 94),
            o("Bienenbuettel, Uelzen, Niedersachsen, Deutschland", 3),
            o("Celle, Niedersachsen, Deutschland", 257, gov = "object_320227"),
            o("Celle,, Deutschland", 2),
            o("Zelle", 1, gov = "OBJECT_320227"),
            o("Hermannsburg", 4),
            o("Hermannsburg, Celle, Niedersachsen, Deutschland", 40),
            o("Eschede, Celle, Niedersachsen", 5, lat = 52.735, lng = 10.250),
            o("Eschede, Celle, Niedersachsen, Deutschland", 260, lat = 52.736, lng = 10.252),
            o("Bremen, Deutschland", 4),
            o("Bremerhaven, Deutschland", 1),
            o("celle, niedersachsen,  deutschland", 1),
        )
        val p = ortsDubletten(orte).associateBy { it.von }
        assertEquals(OrtsGrund.Variante, p.getValue("Bienenbuettel, Uelzen, Niedersachsen, Deutschland").grund)
        assertEquals("Bienenbüttel, Uelzen, Niedersachsen, Deutschland", p.getValue("Bienenbuettel, Uelzen, Niedersachsen, Deutschland").nach)
        assertEquals(OrtsGrund.GleicheGov, p.getValue("Zelle").grund)
        assertEquals("Celle, Niedersachsen, Deutschland", p.getValue("Zelle").nach)
        assertEquals(OrtsGrund.Schreibweise, p.getValue("celle, niedersachsen,  deutschland").grund)
        assertEquals(OrtsGrund.OhneGliederung, p.getValue("Hermannsburg").grund)
        // Eschede mit drei Teilen ist ein Anfang der vierteiligen Fassung - der verlaesslichere Grund gewinnt
        assertEquals(OrtsGrund.OhneGliederung, p.getValue("Eschede, Celle, Niedersachsen").grund)
        // Bremen und Bremerhaven sind verschiedene Orte
        assertTrue(ortsDubletten(orte).none { it.von.startsWith("Brem") || it.nach.startsWith("Brem") })
    }

    @Test
    fun naheOrteMitAndererGliederung() {
        val orte = listOf(o("Wietze, Celle", 3, lat = 52.65, lng = 9.83), o("Wietze, Landkreis Celle, Deutschland", 9, lat = 52.651, lng = 9.832),
            o("Wietze, Fernland", 2, lat = 48.0, lng = 11.0))
        val p = ortsDubletten(orte)
        assertEquals(1, p.size)
        assertEquals(OrtsGrund.Nahe, p.single().grund)
        assertEquals("Wietze, Landkreis Celle, Deutschland", p.single().nach)
    }
}
