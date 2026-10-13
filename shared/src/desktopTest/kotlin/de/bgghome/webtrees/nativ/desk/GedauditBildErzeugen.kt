package de.bgghome.webtrees.nativ.desk

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.Sprache
import de.bgghome.webtrees.nativ.ui.LocalAppName
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug: zeichnet das Hauptfenster von gedaudit (Pruefung einer Datei, erster Treffer gewaehlt) ohne Bildschirm.
 *
 *   WT_GEDAUDITBILD=/pfad WT_GED_A=datei.ged [WT_SPRACHE=en] ./gradlew :shared:desktopTest --rerun --tests '*GedauditBildErzeugen*'
 */
class GedauditBildErzeugen {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun erzeugen() {
        val ziel = System.getenv("WT_GEDAUDITBILD")?.let(::File) ?: return
        ziel.mkdirs()
        System.setProperty("java.util.prefs.userRoot", File(ziel, "prefs").absolutePath)
        System.getenv("WT_SPRACHE")?.let { java.util.Locale.setDefault(java.util.Locale.forLanguageTag(it)); Sprache.setzen(it) }
        val a = File(System.getenv("WT_GED_A")!!)
        val (bw, bh, dichte) = (System.getenv("WT_BILD") ?: "1240:860:1.25").split(':')
        ImageComposeScene(width = bw.toInt(), height = bh.toInt(), density = Density(dichte.toFloat())) {
            CompositionLocalProvider(LocalAppName provides "gedaudit") { DeskTheme { GedauditFenster(null, a, onQuit = {}, vorgabeTreffer = true) } }
        }.use { scene ->
            scene.render(); (1..40).forEach { scene.render(it * 50_000_000L); Thread.sleep(60) }
            File(ziel, "pruefung.png").writeBytes(scene.render(3_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}
