package de.bgghome.webtrees.nativ.gedcom

import de.bgghome.webtrees.nativ.desk.AufbauZeile
import de.bgghome.webtrees.nativ.desk.aufbauZeilen
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** gedaudit liest das GEDCOM-L-Addendum: die Testdatei enthaelt jede Angabe daraus (dieselbe wie in den Tests von api4webtrees). */
class GedcomLTest {

    private val text = File("../testdaten/gedcoml.ged").readText().replace("\r\n", "\n")

    @Test
    fun angabenAnEreignissenPersonenUndFamilien() {
        val (baum, befund) = GedcomLeser.auswerten(GedcomLeser.zerlegen(text), jetzt = 2026)
        // Rufname
        assertEquals("Heinrich", baum.individuals.getValue("I1").person.call)
        // GOV, Postleitzahl mit Datum, Maidenhead am Ort eines Ereignisses in Bienenbuettel
        val zusaetze = baum.individuals.values.flatMap { it.facts }.flatMap { it.extras }
        assertTrue(zusaetze.any { it.tag == "PLAC:_GOV" && it.value == "HOFNR3JO53PC" })
        assertTrue(zusaetze.any { it.tag == "PLAC:_POST" && it.value == "29553" && it.date == "FROM 1993" })
        assertTrue(zusaetze.any { it.tag == "PLAC:_MAIDENHEAD" && it.value == "JO53PC" })
        assertTrue(zusaetze.any { it.tag == "ADDR:_NAME" && it.value == "Familie Mohwinkel" })
        assertEquals("Hauptstrasse 3", baum.individuals.getValue("I1").facts.first { it.tag == "BIRT" }.address)
        // Paten und Zeugen ohne Datensatz (_GODP, _WITN) kommen als freie Beteiligte an
        val frei = baum.individuals.values.flatMap { it.facts }.flatMap { it.freeAssociates } +
            baum.families.values.flatMap { it.facts }.flatMap { it.freeAssociates }
        assertTrue(frei.any { it.role == "godparent" } && frei.any { it.role == "witness" })
        // Familienstand als Ereignis der Familie
        assertTrue(baum.families.getValue("F1").facts.any { it.tag == "_STAT" && it.value.isNotBlank() })
        // alle Ortsverweise (3 _LOC, 1 _LOC) zeigen auf vorhandene Ortsdatensaetze
        assertEquals(emptyList(), befund.verweise.filter { it.tag == "_LOC" })
    }

    @Test
    fun ortsverweisInsLeereWirdGemeldet() {
        val kaputt = text.replaceFirst("3 _LOC @L2@", "3 _LOC @L99@")
        val (_, befund) = GedcomLeser.auswerten(GedcomLeser.zerlegen(kaputt), jetzt = 2026)
        assertEquals(listOf("L99"), befund.verweise.filter { it.tag == "_LOC" }.map { it.ziel })
    }

    @Test
    fun aufbauZeigtGedcomLAlsEigeneGruppe() {
        val zeilen = aufbauZeilen(GedcomLeser.zerlegen(text)).flatMap { it.second }
        assertTrue(zeilen.filter { it.tag == "_UID" }.all { it.art == AufbauZeile.Art.GedcomL })
        assertTrue(zeilen.filter { it.tag == "_LOC" }.all { it.art == AufbauZeile.Art.GedcomL })
        assertTrue(zeilen.filter { it.tag == "_DMGD" }.all { it.art == AufbauZeile.Art.GedcomL })
        // ein eigenes Tag eines Programms (im Kopf unter _SCHEMA erklaert) bleibt Hersteller-Tag
        assertTrue(zeilen.any { it.tag == "_HOBBY" && it.art == AufbauZeile.Art.Hersteller })
    }
}
