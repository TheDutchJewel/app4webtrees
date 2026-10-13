package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.api.ExportFamily
import de.bgghome.webtrees.nativ.api.ExportIndividual
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.PlaceJson
import de.bgghome.webtrees.nativ.api.TreeExport
import de.bgghome.webtrees.nativ.data.adressenZuHaeusern
import kotlin.test.Test
import kotlin.test.assertEquals

class AdressenHoefeTest {
    private fun f(id: String, tag: String, place: String?, address: String?) = FactJson(id = id, tag = tag, place = place?.let { PlaceJson(it) }, address = address)

    @Test fun gruppierung() {
        val b = TreeExport(0, mapOf(
            "I1" to ExportIndividual(Person("I1", "A"), facts = listOf(
                f("a", "RESI", "Mengen, Sigmaringen", "Klosterstraße 3"),
                f("b", "PROP", "Mengen, Sigmaringen", "klosterstrasse 3".replace("ss", "ß")),
                f("c", "BIRT", "Mengen, Sigmaringen", null),
                f("d", "RESI", null, "Ohne Ort 1"),
                f("e", "RESI", "Klosterstraße 3, Mengen, Sigmaringen", "Klosterstraße 3"),
                f("g", "RESI", "Ennetach, Mengen", "Pappelweg 9\nHinterhaus"))),
            "I2" to ExportIndividual(Person("I2", "B"), facts = listOf(f("h", "RESI", "Mengen, Sigmaringen", " Klosterstraße 3 "))),
            "I3" to ExportIndividual(Person("I3", "P", isPrivate = true), facts = listOf(f("i", "RESI", "Mengen, Sigmaringen", "Geheimweg 1"))),
        ), mapOf("F1" to ExportFamily("F1", facts = listOf(f("j", "RESI", "Mengen, Sigmaringen", "Klosterstraße 3")))))
        val v = adressenZuHaeusern(b)
        assertEquals(listOf("Pappelweg 9, Ennetach, Mengen", "Klosterstraße 3, Mengen, Sigmaringen"), v.map { it.name })
        val kloster = v.first { it.adresse == "Klosterstraße 3" }
        // a, b, h und das Familienereignis j; c ohne Adresse, d ohne Ort, e steckt schon im Ortsnamen, i privat
        assertEquals(listOf("a", "b", "h", "j"), kloster.fakten.map { it.fact.id })
        assertEquals(listOf("I1", "I1", "I2", "F1"), kloster.fakten.map { it.record })
        assertEquals("Pappelweg 9", v[0].adresse)
    }
}
