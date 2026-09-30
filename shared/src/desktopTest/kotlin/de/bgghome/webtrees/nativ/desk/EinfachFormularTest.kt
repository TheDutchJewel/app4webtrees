package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.DateJson
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.FactRequest
import de.bgghome.webtrees.nativ.api.FamilyJson
import de.bgghome.webtrees.nativ.api.IndividualDetail
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.PlaceJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Oberster Grundsatz des Formulars: nur Geaendertes geht an den Server, nichts wird still verworfen. */
class EinfachFormularTest {
    private val geburt = FactJson("f1", "BIRT", "Geburt", date = DateJson("14. März 1985", 1985, 2446139, "14 MAR 1985"), place = PlaceJson("Hannover"))
    private val beruf = FactJson("f2", "OCCU", "Beruf", value = "Bauingenieur")
    private val beruf2 = FactJson("f3", "OCCU", "Beruf", value = "Dozent")
    private val name = FactJson("f0", "NAME", "Name", value = "Jonas /Falkenrath/ jun.")
    private val heirat = FactJson("m1", "MARR", "Heirat", date = DateJson("18. Juni 2015", 2015, 0, "18 JUN 2015"))
    private fun detail() = IndividualDetail(
        Person("I1", "Jonas Falkenrath"), facts = listOf(name, geburt, beruf, beruf2),
        spouseFamilies = listOf(FamilyJson("F1", spouse = Person("I2", "Mira Sandvoss"), facts = listOf(heirat))),
    )
    private fun formular() = EinfachFormular("I1").also { it.abgleichen(detail()) }
    private fun EinfachFormular.feld(key: String) = felder.first { it.key == key }

    @Test
    fun unberuehrtSendetNichts() {
        val f = formular()
        assertFalse(f.geaendert)
        assertTrue(f.felder.all { it.anfrage() == null })
        assertEquals(1, f.feld("OCCU").weitere)
    }

    @Test
    fun nurDasGeaenderteTeil() {
        val f = formular()
        f.feld("BIRT").ort = "Celle"
        assertEquals(FactRequest(factId = "f1", date = null, place = "Celle"), f.feld("BIRT").anfrage())
        f.feld("BIRT").datum = "15.4.1929"
        assertEquals(FactRequest(factId = "f1", date = "15 APR 1929", place = "Celle"), f.feld("BIRT").anfrage())
    }

    @Test
    fun nameBehaeltZusatz() {
        val f = formular()
        f.feld("NAME").vornamen = "Jonas Heinrich"
        assertEquals(FactRequest(factId = "f0", value = "Jonas Heinrich /Falkenrath/ jun."), f.feld("NAME").anfrage())
    }

    @Test
    fun neuesEreignisUndHeiratAnDerFamilie() {
        val f = formular()
        f.feld("DEAT").datum = "um 2080"
        assertEquals(FactRequest(tag = "DEAT", date = "ABT 2080"), f.feld("DEAT").anfrage())
        assertNull(f.feld("BURI").anfrage())
        val marr = f.feld("MARR:F1")
        assertEquals("F1", marr.record)
        marr.ort = "Hannover"
        assertEquals(FactRequest(factId = "m1", date = null, place = "Hannover"), marr.anfrage())
    }

    @Test
    fun unbekanntesDatumWirdDatumstext() {
        assertEquals("(Frühjahr 1850)", datumGedcom("Frühjahr 1850"))
        assertEquals("BET 1850 AND 1860", datumGedcom("zwischen 1850 und 1860"))
        assertEquals("", datumGedcom("  "))
    }

    @Test
    fun eingabenUeberlebenNeuladen() {
        val f = formular()
        f.feld("OCCU").wert = "Architekt"
        f.feld("BIRT").gespeichert()
        f.abgleichen(detail()) // Server liefert den alten Stand erneut (z. B. nach Teilfehler)
        assertEquals("Architekt", f.feld("OCCU").wert)
        assertTrue(f.geaendert)
        f.feld("OCCU").gespeichert()
        assertFalse(f.geaendert)
    }
}
