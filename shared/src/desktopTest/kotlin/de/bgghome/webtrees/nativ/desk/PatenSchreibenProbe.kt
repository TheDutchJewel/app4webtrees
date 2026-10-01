package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.api.AddIndividualRequest
import de.bgghome.webtrees.nativ.api.AssociationRequest
import de.bgghome.webtrees.nativ.api.FactRequest
import de.bgghome.webtrees.nativ.api.FreeAssociateRequest
import de.bgghome.webtrees.nativ.api.LinkedAssociateRequest
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.data.Ablage
import de.bgghome.webtrees.nativ.data.Heiratsart
import de.bgghome.webtrees.nativ.data.heiratsart
import de.bgghome.webtrees.nativ.data.patenText
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Kein Test im ueblichen Lauf, sondern eine Probe gegen einen echten Server mit api4webtrees ab Stufe 20: legt eine
 * Wegwerfperson mit Taufe und Heirat an, schreibt Paten, Trauzeugen und Heiratsart ueber den Client der App, liest sie
 * zurueck, zeichnet den Paten-Dialog als PNG und loescht alles wieder. Laeuft nur mit gesetztem WT_PATENPROBE (Ordner
 * fuer das Bild):
 *
 *   WT_PATENPROBE=/pfad WT_USER=admin WT_PASS=... ./gradlew :shared:desktopTest --rerun --tests '*PatenSchreibenProbe*'
 *
 * WT_URL (Vorgabe http://192.168.178.48:8377), WT_BAUM (Vorgabe falkenrath), WT_PATE (verknuepfter Pate, Vorgabe I62).
 * Das Konto braucht Sofortfreigabe (auto_accept), sonst warten die Aenderungen und das Zuruecklesen schlaegt fehl.
 */
class PatenSchreibenProbe {
    private class Speicher : Ablage {
        private val m = mutableMapOf<String, Any?>()
        override fun getString(key: String, default: String?) = m[key] as? String ?: default
        override fun putString(key: String, value: String?) { m[key] = value }
        override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun alle() = m.filterValues { it is String }.mapValues { it.value as String }
        override fun leeren() = m.clear()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun probe() {
        val ziel = System.getenv("WT_PATENPROBE")?.let(::File) ?: return
        ziel.mkdirs()
        val baum = System.getenv("WT_BAUM") ?: "falkenrath"
        val pate = System.getenv("WT_PATE") ?: "I62"
        val c = WtClient(Speicher(), Speicher(), "wtTux/dev (Patenprobe)").apply { baseUrl = System.getenv("WT_URL") ?: "http://192.168.178.48:8377" }
        runBlocking { c.login(System.getenv("WT_USER").orEmpty(), System.getenv("WT_PASS").orEmpty()) }
        val angelegt = mutableListOf<String>()
        try {
            runBlocking {
                // Wegwerfperson mit Taufe und Partnerin mit Heirat
                val x = c.addIndividual(baum, AddIndividualRequest(relation = "none", given = "Probe", surname = "Patenschreiben", sex = "M", dead = true, birthDate = "1 MAY 1850")).xref
                angelegt += x
                c.saveFact(baum, x, FactRequest(tag = "CHR", date = "3 MAY 1850", place = "Celle"))
                val p = c.addIndividual(baum, AddIndividualRequest(relation = "spouse", relativeTo = x, given = "Probe", surname = "Partnerin", sex = "F", dead = true, marriageDate = "10 JUN 1875"))
                angelegt += p.xref
                p.family?.let { angelegt += it }

                // Paten schreiben: einer verknuepft mit Notiz, einer ohne Datensatz
                val taufe = c.individual(baum, x).facts.first { it.tag == "CHR" }
                val r = c.association(baum, x, AssociationRequest(taufe.id,
                    linked = listOf(LinkedAssociateRequest(pate, "godparent", note = "Schwester des Vaters")),
                    free = listOf(FreeAssociateRequest("Friedrich Plate, Anbauer zu Celle", "godparent"))))
                val zurueck = c.individual(baum, x).facts.first { it.tag == "CHR" }
                assertEquals(r.factId, zurueck.id)
                assertEquals(listOf(pate), zurueck.associates.map { it.xref })
                assertEquals(listOf("Schwester des Vaters"), zurueck.associates.single().notes)
                assertEquals(listOf("Friedrich Plate, Anbauer zu Celle"), zurueck.freeAssociates.map { it.text })
                println("Taufe: " + zurueck.patenText("privat"))

                // Heirat: Art kirchlich, ein Trauzeuge ohne Datensatz
                val fam = p.family!!
                suspend fun heiratLesen() = c.individual(baum, x).spouseFamilies.first { it.xref == fam }.facts.first { it.tag == "MARR" }
                val heirat = heiratLesen()
                val h = c.saveFact(baum, fam, FactRequest(factId = heirat.id, type = "religious"))
                val heirat2 = heiratLesen()
                assertEquals(Heiratsart.Kirchlich, heirat2.heiratsart)
                c.association(baum, fam, AssociationRequest(heirat2.id, free = listOf(FreeAssociateRequest("Carl Müller, Gastwirt", "witness"))))
                val heirat3 = heiratLesen()
                assertEquals(listOf("witness"), heirat3.freeAssociates.map { it.role })
                println("Heirat: ${heirat3.label} / ${heirat3.typeLabel} / " + heirat3.patenText("privat") + " (pending=${h.pending})")

                // Den Dialog zeichnen, wie er die Taufe zeigt
                ImageComposeScene(width = 1500, height = 1300, density = Density(2f)) {
                    DeskTheme { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        PatenDialog(zurueck, x, "Kindstaufe", suche = { emptyList() }, onSpeichern = { _, _ -> }, onDismiss = {})
                    } }
                }.use { scene ->
                    // Die Einblend-Animation des Dialogs auslaufen lassen, sonst liegt das Bild halb im Grau
                    scene.render(); (1..40).forEach { scene.render(it * 25_000_000L) }
                    File(ziel, "paten-dialog.png").writeBytes(scene.render(1_100_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
                }
                assertTrue(File(ziel, "paten-dialog.png").length() > 0)
            }
        } finally {
            // Aufraeumen: Familie zuerst, dann die beiden Personen
            runBlocking { angelegt.reversed().forEach { runCatching { c.deleteRecord(baum, it) } } }
        }
    }
}
