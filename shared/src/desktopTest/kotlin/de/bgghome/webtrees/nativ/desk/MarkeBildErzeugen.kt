package de.bgghome.webtrees.nativ.desk

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.Desktop
import de.bgghome.webtrees.nativ.DesktopPlattform
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.LocalAppName
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug: zeichnet den Startbildschirm am PC (Marke, beide Wege) ohne Server als PNG.
 *
 *   WT_MARKEBILD=/pfad ./gradlew :shared:desktopTest --rerun --tests '*MarkeBildErzeugen*'
 *
 * WT_BILD="Breite:Hoehe:Dichte" (Vorgabe 1400:820:1.25).
 */
class MarkeBildErzeugen {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun erzeugen() {
        val ziel = System.getenv("WT_MARKEBILD")?.let(::File) ?: return
        ziel.mkdirs()
        System.setProperty("java.util.prefs.userRoot", File(ziel, "prefs").absolutePath)
        val plat = DesktopPlattform().also { Desktop.plattform = it }
        val vm = AppViewModel(plat)
        val (bw, bh, dichte) = (System.getenv("WT_BILD") ?: "1400:820:1.25").split(':')
        ImageComposeScene(width = bw.toInt(), height = bh.toInt(), density = Density(dichte.toFloat())) {
            CompositionLocalProvider(LocalAppName provides "wtWin") { DeskTheme { DeskStart(vm) } }
        }.use { scene ->
            scene.render(); (1..30).forEach { scene.render(it * 50_000_000L); Thread.sleep(50) }
            File(ziel, "start.png").writeBytes(scene.render(2_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
        ImageComposeScene(width = 520, height = 380, density = Density(dichte.toFloat())) {
            DeskTheme { Markenkopf("wtWin", groesse = 26) }
        }.use { scene ->
            scene.render(); (1..10).forEach { scene.render(it * 50_000_000L); Thread.sleep(50) }
            File(ziel, "marke-klein.png").writeBytes(scene.render(1_000_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}
