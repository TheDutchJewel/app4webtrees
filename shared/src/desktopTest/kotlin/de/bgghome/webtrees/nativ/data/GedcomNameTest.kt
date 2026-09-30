package de.bgghome.webtrees.nativ.data

import kotlin.test.Test
import kotlin.test.assertEquals

class GedcomNameTest {
    @Test
    fun zerlegenUndZusammensetzen() {
        val n = GedcomName.aus("Jonas Heinrich /Falkenrath/ jun.")
        assertEquals(GedcomName("Jonas Heinrich", "Falkenrath", "jun."), n)
        assertEquals("Jonas Heinrich /Falkenrath/ jun.", n.gedcom())
        assertEquals("Falkenrath, Jonas Heinrich jun.", n.anzeige())
        assertEquals("Jonas /Falkenrath/", GedcomName.aus("Jonas /Falkenrath/").gedcom())
        assertEquals("/Falkenrath/", GedcomName("", "Falkenrath", "").gedcom())
        assertEquals("Maria", GedcomName.aus("Maria").gedcom())
        assertEquals("Maria", GedcomName.aus("Maria //").anzeige())
        assertEquals("Falkenrath", GedcomName.aus("/Falkenrath/").anzeige())
    }
}
