package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.data.Ablage
import de.bgghome.webtrees.nativ.ui.Timeline
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug zum Pruefen ohne Bildschirm: zeichnet den Lebenslauf (Timeline, wie am Handy und im
 * Personenblatt) und den Abschnitt "Patenschaften" fuer Personen des Testservers als PNG. Laeuft nur mit gesetztem
 * WT_PATENBILDER (Zielordner), sonst sofort fertig:
 *
 *   WT_PATENBILDER=/pfad WT_XREFS="I21 I58 I377" WT_USER=admin WT_PASS=... ./gradlew :shared:desktopTest --tests '*PatenBilderErzeugen*'
 *
 * WT_URL (Vorgabe http://192.168.178.48:8377), WT_BAUM (Vorgabe falkenrath); ohne WT_USER als Gast.
 */
class PatenBilderErzeugen {
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
    fun erzeugen() {
        val ziel = System.getenv("WT_PATENBILDER")?.let(::File) ?: return
        ziel.mkdirs()
        val baum = System.getenv("WT_BAUM") ?: "falkenrath"
        val client = WtClient(Speicher(), Speicher(), "wtTux/dev (Patenbilder)").apply { baseUrl = System.getenv("WT_URL") ?: "http://192.168.178.48:8377" }
        runBlocking { System.getenv("WT_USER")?.let { client.login(it, System.getenv("WT_PASS").orEmpty()) } ?: client.info() }
        val wer = if (System.getenv("WT_USER") != null) "mitglied" else "gast"
        (System.getenv("WT_XREFS") ?: "I21 I58 I377").split(' ').filter(String::isNotBlank).forEach { xref ->
            val detail = runBlocking { client.individual(baum, xref) }
            println("$xref: ${detail.person.name}, associatedIn=${detail.associatedIn.size}, Paten an Fakten=" +
                detail.facts.count { it.associates.isNotEmpty() || it.freeAssociates.isNotEmpty() })
            ImageComposeScene(width = 820, height = 900, density = Density(1.25f)) {
                DeskTheme {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        Column(Modifier.height(560.dp)) { Timeline(detail, canEdit = false, onEdit = { _, _ -> }, onDelete = { _, _ -> }, onPerson = {}) }
                        Patenschaften(detail) {}
                    }
                }
            }.use { scene ->
                // Zweimal zeichnen: beim ersten Mal laden Texte und Layout, beim zweiten steht alles
                scene.render(); scene.render(16_000_000L)
                val png = scene.render(32_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes
                File(ziel, "$xref-$wer.png").writeBytes(png)
            }
        }
    }
}
