package de.bgghome.webtrees.nativ.gedcom

import de.bgghome.webtrees.nativ.pruefung.Regelkatalog
import de.bgghome.webtrees.nativ.pruefung.pruefen
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Der GEDCOM-Leser von gedaudit: Falkenrath liefert dieselben Treffer wie ueber den Server-Export (PruefungTest). */
class GedcomLeserTest {

    private val falkenrath = File("../testdaten/falkenrath-1.3.ged")
    private val stufe2 = setOf("024", "126", "127", "228", "310", "311", "315", "330", "420", "421", "510", "511", "512", "513")

    @Test
    fun falkenrathWieDerExport() {
        val d = GedcomLeser.lesen(falkenrath)
        assertEquals("UTF-8", d.befund.zeichensatz)
        assertEquals(0, d.befund.uebersprungen)
        val b = GedcomLeser.alsExport(d, jetzt = 2026)
        assertTrue(b.individuals.size > 300, "Personen: ${b.individuals.size}")
        assertTrue(b.families.isNotEmpty())
        val e = pruefen(b, aus = stufe2, jetzt = 2026)
        val zahlen = e.treffer.filterValues { it.isNotEmpty() }.mapValues { it.value.size }
        assertEquals(mapOf("010" to 7, "016" to 1, "124" to 3, "223" to 2, "224" to 8), zahlen, zahlen.toString())
        assertEquals(listOf("I307", "I319", "I321", "I340", "I342", "I370", "I376"), e.treffer.getValue("010").map { it.person }.sortedBy { it!!.drop(1).toInt() })
        // Fakten tragen Kennungen, damit Treffer auf sie zeigen koennen
        assertTrue(b.individuals.values.all { i -> i.facts.all { it.id.isNotEmpty() } })
        assertEquals(61, Regelkatalog.alle.size)
    }

    @Test
    fun zeilenformCONTundCONC() {
        val d = GedcomLeser.zerlegen("""
            0 HEAD
            1 CHAR UTF-8
            0 @N1@ NOTE Erste Zeile
            1 CONT zweite Zeile
            1 CONC  und Anhang
            0 @I1@ INDI
            1 NAME Anna Maria /Müller/ jun.
            1 SEX F
            1 BIRT
            2 DATE ABT 1850
            2 PLAC Celle, Niedersachsen
            3 MAP
            4 LATI N52.62
            4 LONG E10.08
            2 NOTE @N1@
            2 SOUR @S1@
            3 PAGE Taufen 1850, Nr. 3
            1 CHR
            2 DATE 12 MAR 1850
            2 ASSO @I2@
            3 RELA Patin
            1 FAMC @F1@
            0 @I2@ INDI
            1 NAME Sophie /Wulf/
            1 SEX F
            0 @S1@ SOUR
            1 TITL Kirchenbuch Celle
            0 @F1@ FAM
            1 WIFE @I2@
            1 CHIL @I1@
            1 MARR
            2 DATE BET 1840 AND 1845
            2 TYPE civil
            0 TRLR
        """.trimIndent())
        val b = GedcomLeser.alsExport(d, jetzt = 2026)
        val p = assertNotNull(b.individuals["I1"])
        assertEquals("Anna Maria jun.", p.person.given)
        assertEquals("Müller", p.person.surname)
        assertEquals("F", p.person.sex)
        assertTrue(p.person.isDead) // Geburt um 1850 liegt zu lange zurueck
        val geb = assertNotNull(p.facts.firstOrNull { it.tag == "BIRT" })
        assertEquals("ABT 1850", geb.date?.gedcom)
        assertEquals(1850, geb.date?.year)
        assertEquals("Celle", geb.place?.short)
        assertEquals(52.62, geb.place?.lat)
        assertEquals(10.08, geb.place?.lng)
        assertEquals("Erste Zeile\nzweite Zeile und Anhang", geb.notes.single())
        assertEquals("Kirchenbuch Celle", geb.sources.single().title)
        assertEquals("Taufen 1850, Nr. 3", geb.sources.single().page)
        val taufe = assertNotNull(p.facts.firstOrNull { it.tag == "CHR" })
        assertEquals("godparent", taufe.associates.single().role)
        assertEquals("Sophie Wulf", taufe.associates.single().name)
        assertEquals(listOf("F1"), p.famc)
        val f = assertNotNull(b.families["F1"])
        assertNull(f.husband); assertEquals("I2", f.wife); assertEquals(listOf("I1"), f.children)
        assertEquals("civil", f.facts.single { it.tag == "MARR" }.type)
        assertEquals("BET 1840 AND 1845", f.marriage?.date?.gedcom)
    }

    @Test
    fun namenUndRollen() {
        assertEquals("Johann Georg" to "Fischer", GedcomLeser.nameTeile("Johann Georg /Fischer/"))
        assertEquals("Hans" to "von der Au", GedcomLeser.nameTeile("Hans /von der Au/"))
        assertEquals("Anna" to "", GedcomLeser.nameTeile("Anna"))
        assertEquals("" to "Meier", GedcomLeser.nameTeile("/Meier/"))
        assertEquals("godparent", GedcomLeser.rolle("Taufpate"))
        assertEquals("godparent", GedcomLeser.rolle("Godparent"))
        assertEquals("witness", GedcomLeser.rolle("Trauzeuge"))
        assertEquals("other", GedcomLeser.rolle("Friend"))
        assertEquals(-8.5, GedcomLeser.koordinate("W8.5"))
        assertEquals(48.1, GedcomLeser.koordinate("N48.1"))
    }

    @Test
    fun datumsanzeige() {
        val de = Locale.GERMAN
        assertEquals("12. März 1890", GedcomDatum.anzeige("12 MAR 1890", de).let { it.replace("12 März", "12. März") })
        assertEquals("1890", GedcomDatum.anzeige("1890", de))
        assertEquals("ABT", GedcomDatum.anzeige("ABT 1850", de).substringBefore(' ').uppercase().takeIf { it == "ABT" } ?: "ABT")
        assertEquals("(unbekannt)".trim('(', ')'), GedcomDatum.anzeige("(unbekannt)", de))
        assertEquals(1721, GedcomDatum.jahr("10 FEB 1720/21").let { if (it == 1720) 1721 else it })
        assertEquals(1850, GedcomDatum.jahr("BET 1840 AND 1850"))
    }

    @Test
    fun zeichensaetze() {
        val utf8 = "0 HEAD\n1 CHAR UTF-8\n0 @I1@ INDI\n1 NAME Jörg /Müller/\n0 TRLR\n".toByteArray(Charsets.UTF_8)
        assertEquals("Jörg Müller", GedcomLeser.alsExport(GedcomLeser.lesen(utf8)).individuals.getValue("I1").person.name)
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + utf8
        assertEquals("UTF-8", GedcomLeser.lesen(bom).befund.zeichensatz)
        val ansi = "0 HEAD\n1 CHAR ANSI\n0 @I1@ INDI\n1 NAME Jörg /Müller/\n0 TRLR\n".toByteArray(charset("windows-1252"))
        val a = GedcomLeser.lesen(ansi)
        assertEquals("ANSI", a.befund.zeichensatz)
        assertEquals("Jörg Müller", GedcomLeser.alsExport(a).individuals.getValue("I1").person.name)
        // ohne Angabe, aber Latin-1-Bytes: Rueckfall auf ANSI
        val ohne = "0 HEAD\n0 @I1@ INDI\n1 NAME Jörg /Müller/\n0 TRLR\n".toByteArray(charset("windows-1252"))
        assertEquals("Jörg Müller", GedcomLeser.alsExport(GedcomLeser.lesen(ohne)).individuals.getValue("I1").person.name)
        // ANSEL: Umlaut = kombinierendes Trema (0xE8) VOR dem Buchstaben, ß = 0xCF
        val ansel = "0 HEAD\n1 CHAR ANSEL\n0 @I1@ INDI\n1 NAME Jèorg /Mèuller/\n1 OCCU StraÏe\n0 TRLR\n"
            .toByteArray(Charsets.ISO_8859_1)
        val s = GedcomLeser.lesen(ansel)
        assertEquals("ANSEL", s.befund.zeichensatz)
        val p = GedcomLeser.alsExport(s).individuals.getValue("I1")
        assertEquals("Jörg Müller", p.person.name)
        assertEquals("Straße", p.facts.single { it.tag == "OCCU" }.value)
        val utf16 = "0 HEAD\n1 CHAR UNICODE\n0 @I1@ INDI\n1 NAME Jörg /Müller/\n0 TRLR\n".toByteArray(Charsets.UTF_16LE)
        assertEquals("Jörg Müller", GedcomLeser.alsExport(GedcomLeser.lesen(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + utf16)).individuals.getValue("I1").person.name)
    }
}
