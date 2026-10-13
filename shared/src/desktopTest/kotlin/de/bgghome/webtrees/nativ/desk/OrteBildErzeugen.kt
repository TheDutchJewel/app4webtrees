package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.data.Ablage
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug: zeichnet die Ortsverwaltung (Liste links, gewaehlter Ort rechts, Reiter nach Wahl)
 * mit echten Daten des Testservers als PNG. Laeuft nur mit gesetztem WT_ORTBILD (Zielordner):
 *
 *   WT_ORTBILD=/pfad WT_ORT="Haus Nr. 7 (Schmiede), Bienenbüttel, …" WT_REITER=Geschichte WT_URL=… WT_USER=admin WT_PASS=… WT_BAUM=falkenrath14 \
 *     ./gradlew :shared:desktopTest --rerun --tests '*OrteBildErzeugen*'
 */
class OrteBildErzeugen {
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
        val ziel = System.getenv("WT_ORTBILD")?.let(::File) ?: return
        ziel.mkdirs()
        val baum = System.getenv("WT_BAUM") ?: "falkenrath14"
        val ort = System.getenv("WT_ORT") ?: "Bienenbüttel, Uelzen, Niedersachsen, Deutschland"
        val reiter = OrtReiter.entries.indexOfFirst { it.name == (System.getenv("WT_REITER") ?: "Geschichte") }.coerceAtLeast(0)
        val breite = System.getenv("WT_BREITE")?.toInt() ?: 2200
        val hoehe = System.getenv("WT_HOEHE")?.toInt() ?: 1300
        val c = WtClient(Speicher(), Speicher(), "wtTux/dev (Ortbild)").apply { baseUrl = System.getenv("WT_URL") ?: "http://192.168.178.73:8095" }
        val (liste, detail) = runBlocking {
            System.getenv("WT_USER")?.let { c.login(it, System.getenv("WT_PASS").orEmpty()) } ?: c.info()
            c.placeList(baum) to c.place(baum, ort)
        }
        ImageComposeScene(width = breite, height = hoehe, density = Density(2f)) {
            DeskTheme {
                Box(Modifier.fillMaxSize()) {
                    OrteInhalt(Result.success(liste), Result.success(detail), ort, {}, System.getenv("WT_SUCHE").orEmpty(), {}, null, {}, reiterStart = reiter, onClose = {}, karteStart = System.getenv("WT_KARTE") != null)
                }
            }
        }.use { scene ->
            scene.render(); (1..20).forEach { scene.render(it * 50_000_000L); Thread.sleep(50) }
            File(ziel, if (System.getenv("WT_KARTE") != null) "ort-karte.png" else "ort-${reiter}.png").writeBytes(scene.render(1_100_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}
