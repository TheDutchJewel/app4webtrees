package de.bgghome.webtrees.nativ.pruefung

import de.bgghome.webtrees.nativ.api.DateJson
import de.bgghome.webtrees.nativ.api.ExportFamily
import de.bgghome.webtrees.nativ.api.ExportIndividual
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.TreeExport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Plausibilitaetspruefung gegen den Demo-Baum Falkenrath: dieselben Treffer wie das Pruefprogramm aus db-blank
 * (`python3 plausibilitaet.py demo-tree/falkenrath.ged`, Stand 27.09.2026). Der Baum wird hier aus dem GEDCOM in
 * die Form des Server-Exports gebracht (Fakten mit GEDCOM-Datum), so wie ihn wtWin von api4webtrees bekommt.
 */
class PruefungTest {

    private fun gedcomAlsExport(datei: File): TreeExport {
        val saetze = mutableListOf<Triple<String, String, MutableList<Triple<Int, String, String>>>>()
        datei.forEachLine { roh ->
            val z = roh.trimEnd('\r'); if (z.isEmpty()) return@forEachLine
            val t = z.split(' ', limit = 3)
            val lvl = t[0].toIntOrNull() ?: return@forEachLine
            if (lvl == 0) { if (t.size >= 3 && t[1].startsWith("@")) saetze += Triple(t[2].trim(), t[1].trim('@'), mutableListOf()) else saetze += Triple("", "", mutableListOf()); return@forEachLine }
            saetze.lastOrNull()?.third?.add(Triple(lvl, t.getOrElse(1) { "" }, t.getOrElse(2) { "" }))
        }
        fun fakten(zeilen: List<Triple<Int, String, String>>, tags: Set<String>): List<FactJson> {
            val out = mutableListOf<FactJson>()
            var offen: String? = null; var datum: String? = null
            fun schliessen() { offen?.let { out += FactJson(id = "${out.size}", tag = it, date = datum?.let { d -> DateJson(text = d, gedcom = d) }) } }
            for ((lvl, tag, wert) in zeilen) {
                if (lvl == 1) { schliessen(); offen = tag.takeIf { it in tags }; datum = null }
                else if (lvl == 2 && tag == "DATE" && offen != null && datum == null) datum = wert
            }
            schliessen()
            return out
        }
        val ereignisse = setOf("BIRT", "CHR", "BAPM", "DEAT", "BURI", "CREM", "OCCU", "RELI", "NOTE")
        val personen = LinkedHashMap<String, ExportIndividual>()
        val familien = LinkedHashMap<String, ExportFamily>()
        for ((typ, id, zeilen) in saetze) when (typ) {
            "INDI" -> {
                val name = zeilen.firstOrNull { it.first == 1 && it.second == "NAME" }?.third.orEmpty()
                val m = Regex("^(.*?)\\s*/(.*?)/\\s*(.*)$").find(name)
                val (vn, fn) = if (m != null) m.groupValues[1].trim() to m.groupValues[2].trim() else name.trim() to ""
                val sex = zeilen.firstOrNull { it.first == 1 && it.second == "SEX" }?.third?.trim() ?: "U"
                personen[id] = ExportIndividual(
                    person = Person(xref = id, name = "$vn $fn".trim(), given = vn, surname = fn, sex = sex),
                    famc = zeilen.filter { it.first == 1 && it.second == "FAMC" }.map { it.third.trim('@') },
                    fams = zeilen.filter { it.first == 1 && it.second == "FAMS" }.map { it.third.trim('@') },
                    facts = fakten(zeilen, ereignisse),
                )
            }
            "FAM" -> familien[id] = ExportFamily(
                xref = id,
                husband = zeilen.firstOrNull { it.first == 1 && it.second == "HUSB" }?.third?.trim('@'),
                wife = zeilen.firstOrNull { it.first == 1 && it.second == "WIFE" }?.third?.trim('@'),
                children = zeilen.filter { it.first == 1 && it.second == "CHIL" }.map { it.third.trim('@') },
                facts = fakten(zeilen, setOf("MARR")),
            )
        }
        return TreeExport(1, personen, familien)
    }

    private val falkenrath = File("../demo-tree/falkenrath.ged")

    @Test
    fun falkenrathWieDasPythonProgramm() {
        val e = pruefen(gedcomAlsExport(falkenrath), jetzt = 2026, deutsch = true)
        val zahlen = e.treffer.filterValues { it.isNotEmpty() }.mapValues { it.value.size }
        assertEquals(mapOf("010" to 7, "016" to 1, "124" to 3, "223" to 2, "224" to 8), zahlen, e.treffer.filterValues { it.isNotEmpty() }.toString())
        assertEquals(listOf("I307", "I319", "I321", "I340", "I342", "I370", "I376"), e.treffer.getValue("010").map { it.person }.sortedBy { it!!.drop(1).toInt() })
        assertEquals("I329", e.treffer.getValue("016").single().person)
        assertEquals("F75", e.treffer.getValue("016").single().familie)
        assertEquals(47, Regelkatalog.alle.size)
    }

    @Test
    fun grenzwerteUndAbschalten() {
        val b = gedcomAlsExport(falkenrath)
        // Geschwisterluecke ab 10 statt 12 Jahren: mehr Treffer; Regel 224 abgeschaltet
        val e = pruefen(b, grenzwerte = mapOf("124" to 10.0), aus = setOf("224"), jetzt = 2026)
        assertTrue(e.treffer.getValue("124").size > 3)
        assertNull(e.treffer["224"])
    }

    @Test
    fun datumsformen() {
        val genau = assertNotNull(PruefDatum.aus("21 JUN 1844"))
        assertTrue(genau.voll); assertEquals(genau.min, genau.max)
        val jahr = assertNotNull(PruefDatum.aus("1844"))
        assertEquals(1844, jahr.jahr)
        assertEquals(365L, jahr.max - jahr.min) // 1844 ist ein Schaltjahr: 366 Tage
        assertEquals(1850, PruefDatum.aus("ABT 1850")?.jahr)
        val bet = assertNotNull(PruefDatum.aus("BET 1840 AND 1845"))
        assertNull(bet.jahr)
        assertEquals(true, vor(PruefDatum.aus("BEF 1850"), PruefDatum.aus("1850")))
        assertEquals(false, vor(PruefDatum.aus("AFT 1850"), PruefDatum.aus("1851")))
        assertEquals(true, vor(PruefDatum.aus("MAR 1844"), PruefDatum.aus("JUN 1844")))
        assertEquals(false, vor(PruefDatum.aus("MAR 1844"), PruefDatum.aus("1844")))
        assertEquals(1721, PruefDatum.aus("10 FEB 1720/21")?.jahr)
        // Umstellung 1700: auf den julianischen 18.2. folgte der gregorianische 1.3. - der 19.2. julianisch ist der 1.3.
        assertEquals(PruefDatum.aus("1 MAR 1700")!!.min, PruefDatum.aus("@#DJULIAN@ 19 FEB 1700")!!.min)
        assertNull(PruefDatum.aus("@#DFRENCH R@ 1 VEND 10"))
        assertNull(PruefDatum.aus("(unbekannt)"))
        assertTrue(PruefDatum.aus("31 APR 1850")!!.unmoeglich)
        assertTrue(PruefDatum.aus("29 FEB 1900")!!.unmoeglich)
        assertTrue(!PruefDatum.aus("29 FEB 2000")!!.unmoeglich)
    }

    @Test
    fun kreisInDerAbstammung() {
        fun p(x: String, famc: List<String>, fams: List<String>) = ExportIndividual(Person(xref = x, name = x, given = x, surname = "T", sex = "M"), famc, fams)
        // A ist Kind von B, B ist Kind von A (F1: B Vater von A, F2: A Vater von B); C haengt unbeteiligt daran
        val b = TreeExport(1,
            mapOf("A" to p("A", listOf("F1"), listOf("F2")), "B" to p("B", listOf("F2"), listOf("F1")), "C" to p("C", listOf("F2"), emptyList())),
            mapOf("F1" to ExportFamily("F1", husband = "B", children = listOf("A")), "F2" to ExportFamily("F2", husband = "A", children = listOf("B", "C"))))
        assertEquals(setOf("A", "B"), pruefen(b, jetzt = 2026).treffer.getValue("225").map { it.person }.toSet())
    }
}
