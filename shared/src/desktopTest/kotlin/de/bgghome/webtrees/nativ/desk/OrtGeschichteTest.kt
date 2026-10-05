package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.DateJson
import de.bgghome.webtrees.nativ.api.LocationEvent
import de.bgghome.webtrees.nativ.api.LocationJson
import de.bgghome.webtrees.nativ.api.PlaceDetail
import de.bgghome.webtrees.nativ.api.PlaceEvent
import de.bgghome.webtrees.nativ.api.PlaceUseFamily
import de.bgghome.webtrees.nativ.api.PlaceUsePerson
import kotlin.test.Test
import kotlin.test.assertEquals

/** Reiter Geschichte im Ortsfenster: Ereignisse am Ort (EVEN am _LOC) und der Personen hier, der Zeit nach. */
class OrtGeschichteTest {
    private fun datum(jahr: Int, jd: Int) = DateJson(text = "$jahr", year = jahr, jd = jd)

    @Test
    fun chronologischMitOrtsereignissenZuerst() {
        val ort = PlaceDetail(
            name = "Hof Nr. 1, Offenbach",
            location = LocationJson("L3", "Hof Nr. 1", type = "Hof", events = listOf(
                LocationEvent(type = "Brand", date = datum(1734, 2354500), notes = listOf("Scheune abgebrannt")),
                LocationEvent(label = "Ereignis", value = "Neubau"),  // ohne Datum: ans Ende
            )),
            individuals = listOf(
                PlaceUsePerson("I4", "Karl Offen", facts = listOf(PlaceEvent("RESI", "Wohnort", datum(1860, 2400500)))),
                PlaceUsePerson("I9", "Anna Offen", facts = listOf(PlaceEvent("BIRT", "Geburt", datum(1734, 2354500)), PlaceEvent("DEAT", "Tod", datum(1800, 2378500)))),
            ),
            families = listOf(PlaceUseFamily("F2", "Offen + Meier", husband = "I4", facts = listOf(PlaceEvent("MARR", "Heirat", datum(1862, 2401200))))),
        )
        val g = geschichte(ort)
        assertEquals(listOf("Brand", "Geburt", "Tod", "Wohnort", "Heirat", "Ereignis: Neubau"), g.map { it.was })
        // Gleiches Datum: das Ereignis des Orts vor dem der Person
        assertEquals(listOf(true, false), g.take(2).map { it.amOrt })
        assertEquals(listOf("Scheune abgebrannt"), g[0].notizen)
        // Familienereignis oeffnet den Ehemann
        assertEquals("I4", g.first { it.was == "Heirat" }.xref)
        assertEquals(null, g.last().xref)
    }

    @Test
    fun ohneOrtsdatensatzNurPersonen() {
        val ort = PlaceDetail(name = "Bieber, Offenbach", individuals = listOf(PlaceUsePerson("I4", "Karl", facts = listOf(PlaceEvent("BIRT", "Geburt", datum(1830, 2389500))))))
        assertEquals(listOf("Geburt"), geschichte(ort).map { it.was })
        assertEquals(emptyList(), geschichte(PlaceDetail(name = "Leer")))
    }
}
