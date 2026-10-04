package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.data.Ablage
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug fuer Homepage und README: zeichnet die Quellenverwaltung (Liste links, gewaehlte
 * Quelle rechts) mit echten Daten des Testservers als PNG. Laeuft nur mit gesetztem WT_QUELLENBILD (Zielordner):
 *
 *   WT_QUELLENBILD=/pfad WT_QUELLE=S1 WT_USER=admin WT_PASS=... ./gradlew :shared:desktopTest --rerun --tests '*QuellenBildErzeugen*'
 *
 * WT_URL (Vorgabe http://192.168.178.48:8377), WT_BAUM (Vorgabe falkenrath).
 */
class QuellenBildErzeugen {
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
        val ziel = System.getenv("WT_QUELLENBILD")?.let(::File) ?: return
        ziel.mkdirs()
        val baum = System.getenv("WT_BAUM") ?: "falkenrath"
        val xref = System.getenv("WT_QUELLE") ?: "S1"
        val c = WtClient(Speicher(), Speicher(), "wtTux/dev (Quellenbild)").apply { baseUrl = System.getenv("WT_URL") ?: "http://192.168.178.48:8377" }
        val (liste, quelle) = runBlocking {
            System.getenv("WT_USER")?.let { c.login(it, System.getenv("WT_PASS").orEmpty()) } ?: c.info()
            c.sources(baum).sources to c.source(baum, xref)
        }
        ImageComposeScene(width = 2400, height = 1400, density = Density(2f)) {
            DeskTheme {
                Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Column(Modifier.width(380.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                        Text("${liste.size} Quellen", Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(Modifier.weight(1f).fillMaxWidth()) { QuellenListe(liste, xref) {} }
                    }
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(Modifier.weight(1f).fillMaxHeight()) { QuelleDetail(quelle, onPerson = {}, onMitte = {}, openWeb = {}, canEdit = true) }
                }
            }
        }.use { scene ->
            // Mehrmals zeichnen: Vorschaubilder der Scans kommen nachgeladen
            scene.render(); (1..40).forEach { scene.render(it * 50_000_000L); Thread.sleep(50) }
            File(ziel, "quellen.png").writeBytes(scene.render(2_100_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}
