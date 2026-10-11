package de.bgghome.webtrees.nativ

import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.api.PersonRef
import de.bgghome.webtrees.nativ.data.ArchivAbgleich
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchivAbgleichTest {
    private val basis = ArchiveEntry(pfad = "a.jpg", xref = "M1", titel = "Hochzeit 1924", beschreibung = "Hochzeit",
        datumIso = "1924-05-17", exifPersonen = listOf("Anna Rosskamp"), keywords = listOf("Celle"),
        personen = listOf(PersonRef("I21", "Heinrich Falkenrath"), PersonRef("I22", "Anna Rosskamp")), personenGesamt = 2)

    @Test
    fun freieDateiHatNichtsZumAbgleichen() = assertTrue(ArchivAbgleich.unterschiede(basis.copy(xref = null)).isEmpty())

    @Test
    fun beschreibungUndPersonenWerdenVerglichen() {
        val u = ArchivAbgleich.unterschiede(basis)
        assertEquals(2, u.size)
        val p = u.first { it.feld == ArchivAbgleich.Feld.Personen }
        assertEquals("Anna Rosskamp, Heinrich Falkenrath", p.uebernahme)
        // Uebernahme der Personen laesst Beschreibung, Datum und Schlagwoerter wie in der Datei
        val r = ArchivAbgleich.anfrage(basis, p)
        assertEquals("Hochzeit", r.beschreibung); assertEquals("1924-05-17", r.datum); assertEquals(listOf("Celle"), r.keywords)
        assertEquals(listOf("Anna Rosskamp", "Heinrich Falkenrath"), r.personen)
    }

    @Test
    fun ausschnittDerPersonenWirdNichtUebernommen() {
        val p = ArchivAbgleich.unterschiede(basis.copy(personenGesamt = 40)).first { it.feld == ArchivAbgleich.Feld.Personen }
        assertTrue(p.gekuerzt); assertNull(p.uebernahme)
    }

    @Test
    fun gleicheWerteSindKeinUnterschied() =
        assertTrue(ArchivAbgleich.unterschiede(basis.copy(beschreibung = "Hochzeit 1924", exifPersonen = listOf("Heinrich Falkenrath", "Anna Rosskamp"))).isEmpty())
}
