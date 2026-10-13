package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.AddIndividualRequest
import de.bgghome.webtrees.nativ.api.FactRequest
import de.bgghome.webtrees.nativ.api.PlaceRequest
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.api.exportTree
import de.bgghome.webtrees.nativ.data.Ablage
import de.bgghome.webtrees.nativ.data.adressenZuHaeusern
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Kein Test, sondern eine Probe gegen einen Server ab API-Stufe 33: legt eine Wegwerfperson mit Wohnsitz "Ort + Adresse"
 * an, wandelt die Adresse wie im Ortsfenster in einen eigenen Ort um, prueft Ereignis und Ortsdatensatz und raeumt ab.
 *   WT_ADRESSPROBE=1 WT_URL=http://192.168.178.73:8095 WT_USER=admin WT_PASS=… WT_BAUM=falkenrath14 ./gradlew :shared:desktopTest --rerun --tests '*AdressenProbe*'
 */
class AdressenProbe {
    private class Speicher : Ablage {
        private val m = mutableMapOf<String, Any?>()
        override fun getString(key: String, default: String?) = m[key] as? String ?: default
        override fun putString(key: String, value: String?) { m[key] = value }
        override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun alle() = m.filterValues { it is String }.mapValues { it.value as String }
        override fun leeren() = m.clear()
    }

    @Test fun umwandeln() {
        if (System.getenv("WT_ADRESSPROBE") == null) return
        val tree = System.getenv("WT_BAUM") ?: "falkenrath14"
        val ort = "Bienenbüttel, Uelzen, Niedersachsen, Deutschland"
        val adresse = "Probeweg 99"
        val c = WtClient(Speicher(), Speicher(), "wtTux/dev (Adressprobe)").apply { baseUrl = System.getenv("WT_URL") ?: "http://192.168.178.73:8095" }
        runBlocking {
            val info = c.login(System.getenv("WT_USER") ?: "admin", System.getenv("WT_PASS").orEmpty())
            assertTrue(info.api >= de.bgghome.webtrees.nativ.api.API_ADDRESS, "Server hat Stufe ${info.api}")
            val xref = c.addIndividual(tree, AddIndividualRequest(relation = "none", given = "Adresse", surname = "Probe", sex = "F", dead = true)).xref
            var hausXref: String? = null
            try {
                c.saveFact(tree, xref, FactRequest(tag = "RESI", date = "FROM 1850 TO 1870", place = ort, address = "$adresse\nHinterhaus", note = "Probe"))
                val resi0 = c.individual(tree, xref).facts.first { it.tag == "RESI" }
                assertEquals("$adresse\nHinterhaus", resi0.address)
                // Wie das Ortsfenster: Vorschlaege aus dem Export, dann Ort anlegen und Ereignisse umschreiben
                val baum = c.exportTree(tree) { _, _ -> }
                val v = adressenZuHaeusern(baum).first { it.adresse == adresse && it.fakten.any { a -> a.record == xref } }
                assertEquals("$adresse, $ort", v.name)
                val oben = assertNotNull(ortsdatensatz(c, tree, v.ort), "Oberort ohne Ortsdatensatz")
                val neu = c.savePlace(tree, PlaceRequest(v.name, type = "Haus", parentAendern = true, parent = oben, govTypeAendern = true, govType = govTypZu("Haus")))
                assertTrue(neu.ok, neu.toString())
                for (a in v.fakten) {
                    val r = c.saveFact(tree, a.record, FactRequest(factId = a.fact.id, tag = a.fact.tag, place = v.name, address = ""))
                    assertTrue(r.ok, r.toString())
                }
                val resi = c.individual(tree, xref).facts.first { it.tag == "RESI" }
                assertEquals(v.name, resi.place?.name)
                assertEquals(null, resi.address)
                assertTrue(resi.notes.any { "Probe" in it }, "Notiz weg: ${resi.notes}")
                val detail = c.place(tree, v.name)
                val loc = assertNotNull(detail.location, "kein Ortsdatensatz")
                hausXref = loc.xref
                assertEquals("Haus", loc.type); assertEquals("17", loc.govType)
                assertTrue(loc.parents.any { it.name.startsWith("Bienenbüttel") }, "Oberort fehlt: ${loc.parents}")
                assertEquals(0, detail.unlinked, "Ereignis ohne Verweis 3 _LOC")
                println("PROBE OK: $xref -> ${v.name} (${loc.xref})")
            } finally {
                c.deleteRecord(tree, xref)
                hausXref?.let { runCatching { c.deleteRecord(tree, it) } }
            }
        }
    }
}
