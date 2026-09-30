package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.FamilyJson
import de.bgghome.webtrees.nativ.api.IndividualDetail
import de.bgghome.webtrees.nativ.api.Person
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeskTastenTest {
    private fun p(x: String, privat: Boolean = false) = Person(x, x, isPrivate = privat)
    private val eltern = FamilyJson("F0", husband = p("V"), wife = p("M"), children = listOf(p("A"), p("B"), p("X", privat = true), p("C")))
    private val ehe = FamilyJson("F1", spouse = p("P"), children = listOf(p("K", privat = true), p("L")))
    private fun d(x: String) = IndividualDetail(p(x), parentFamilies = listOf(eltern), spouseFamilies = listOf(ehe))

    @Test
    fun elternUndKind() {
        assertEquals("V", zielPerson(d("B"), ehe, Richtung.Vater))
        assertEquals("M", zielPerson(d("B"), ehe, Richtung.Mutter))
        assertEquals("L", zielPerson(d("B"), ehe, Richtung.Kind)) // private Kinder werden uebersprungen
        assertNull(zielPerson(d("B"), null, Richtung.Kind))
        assertNull(zielPerson(IndividualDetail(p("Z")), null, Richtung.Vater))
    }

    @Test
    fun geschwisterOhnePrivate() {
        assertEquals("C", zielPerson(d("B"), ehe, Richtung.GeschwisterVor))
        assertEquals("A", zielPerson(d("B"), ehe, Richtung.GeschwisterZurueck))
        assertNull(zielPerson(d("A"), ehe, Richtung.GeschwisterZurueck))
        assertNull(zielPerson(d("C"), ehe, Richtung.GeschwisterVor))
    }
}
