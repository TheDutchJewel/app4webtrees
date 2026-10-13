package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.DateJson
import de.bgghome.webtrees.nativ.api.EventJson
import de.bgghome.webtrees.nativ.api.ExportFamily
import de.bgghome.webtrees.nativ.api.ExportIndividual
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.PlaceJson
import de.bgghome.webtrees.nativ.api.TreeExport
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Personenbericht, Lebende-Regel und Website-Export an einem kleinen Baum: Vater, Mutter, Sohn (lebend) in Mengen. */
class PersonenberichtTest {
    private fun datum(j: Int) = DateJson(text = "$j", year = j, jd = 1, gedcom = "$j")
    private fun person(x: String, name: String, given: String, surname: String, geb: Int, tot: Boolean) =
        Person(x, name = name, given = given, surname = surname, sex = "M", isDead = tot, birth = EventJson(datum(geb), PlaceJson("Mengen, Sigmaringen")))
    private val baum = TreeExport(0, mapOf(
        "I1" to ExportIndividual(person("I1", "Karl Löw", "Karl", "Löw", 1900, true), fams = listOf("F1"),
            facts = listOf(FactJson("a", "BIRT", "Geburt", date = datum(1900), place = PlaceJson("Mengen, Sigmaringen")), FactJson("b", "DEAT", "Tod", date = datum(1970), place = PlaceJson("Mengen, Sigmaringen")))),
        "I2" to ExportIndividual(person("I2", "Anna Löw", "Anna", "Löw", 1905, true), fams = listOf("F1"), facts = listOf(FactJson("c", "BIRT", "Geburt", date = datum(1905)))),
        "I3" to ExportIndividual(person("I3", "Peter Löw", "Peter", "Löw", 1960, false), famc = listOf("F1"), facts = listOf(FactJson("d", "BIRT", "Geburt", date = datum(1960), place = PlaceJson("Mengen, Sigmaringen")))),
    ), mapOf("F1" to ExportFamily("F1", husband = "I1", wife = "I2", children = listOf("I3"), marriage = EventJson(datum(1930), PlaceJson("Mengen, Sigmaringen")))))
    private val daten = FamilienDaten(baum, emptyMap())

    private fun text(b: Buch) = b.bloecke.filterIsInstance<Absatz>().joinToString("\n") { a -> (a.marke?.let { "$it " } ?: "") + a.laeufe.joinToString("") { it.text } }

    @Test fun lebendeNurName() {
        val t = text(familienbuch(daten, BuchOptionen(bilder = false, lebende = Lebende.NurName), "Test", "wtTux"))
        assertTrue("Löw, Karl" in t && "1900" in t, t)
        assertTrue("Peter ♂ (lebt)" in t, t)
        assertFalse("1960" in t, "Geburtsjahr des Lebenden darf nicht erscheinen: $t")
        val voll = text(familienbuch(daten, BuchOptionen(bilder = false, lebende = Lebende.Voll), "Test", "wtTux"))
        assertTrue("1960" in voll, voll)
        val weg = text(familienbuch(daten, BuchOptionen(bilder = false, lebende = Lebende.Weg), "Test", "wtTux"))
        assertFalse("Peter" in weg, weg)
        // Grenzjahr: wer vor mehr als 50 Jahren geboren ist, gilt trotz fehlendem Tod als nicht lebend
        val alt = text(familienbuch(daten, BuchOptionen(bilder = false, lebende = Lebende.NurName, grenzjahr = 50), "Test", "wtTux"))
        assertTrue("1960" in alt, alt)
    }

    @Test fun personenbericht() {
        val b = personenbericht(daten, BuchOptionen(bilder = false, lebende = Lebende.Voll), "Test", "wtTux")
        val t = text(b)
        // Alphabetisch: Anna 1, Karl 2, Peter 3; Eltern und Kinder als Verweise
        assertEquals(listOf("1", "2", "3"), b.bloecke.filterIsInstance<Absatz>().mapNotNull { it.marke })
        assertTrue(t.lines().any { it.startsWith("2 Löw, Karl") }, t)
        assertTrue("Eltern: Karl Löw [2] ∞ Anna Löw [1]" in t, t)
        assertTrue("Kinder: Peter Löw [3]" in t, t)
        assertTrue("∞ 1930 Mengen mit Anna Löw [1]" in t, t)
        assertEquals("Personenbericht Test", b.titel)
        val ziele = b.bloecke.filterIsInstance<Absatz>().flatMap { it.laeufe }.mapNotNull { it.ziel }.toSet()
        val anker = b.bloecke.filterIsInstance<Absatz>().mapNotNull { it.anker }.toSet()
        assertTrue(anker.containsAll(ziele), "Verweise ohne Ziel: ${ziele - anker}")
    }

    @Test fun website() {
        val ordner = Files.createTempDirectory("wtweb").toFile()
        try {
            buchWebsite(familienbuch(daten, BuchOptionen(bilder = false, vorwort = "Ein Vorwort."), "Test", "wtTux"), ordner)
            val dateien = ordner.walkTopDown().filter { it.isFile }.map { it.relativeTo(ordner).path }.toSet()
            assertTrue(setOf("index.html", "familien.html", "style.css", "register-namen.html", "eintraege/n1.html").all { it in dateien }, dateien.toString())
            val index = File(ordner, "index.html").readText()
            assertTrue("Ein Vorwort." in index && "familien.html" in index, index)
            val familien = File(ordner, "familien.html").readText()
            assertTrue("id=\"n1\"" in familien && "eintraege/n1.html" in familien, familien)
            val n1 = File(ordner, "eintraege/n1.html").readText()
            assertTrue("../style.css" in n1 && "../familien.html#n1" in n1, n1)
            // Alle Links zeigen auf vorhandene Dateien
            ordner.walkTopDown().filter { it.extension == "html" }.forEach { f ->
                Regex("href=\"([^\"#]+)").findAll(f.readText()).map { it.groupValues[1] }.filter { !it.startsWith("http") }.forEach { h ->
                    assertTrue(File(f.parentFile, h).isFile, "${f.name}: $h fehlt")
                }
            }
        } finally { ordner.deleteRecursively() }
    }
}
