package de.bgghome.webtrees.nativ.desk

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.Desktop
import de.bgghome.webtrees.nativ.DesktopPlattform
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.Screen
import de.bgghome.webtrees.nativ.ui.chooseTree
import de.bgghome.webtrees.nativ.ui.login
import de.bgghome.webtrees.nativ.ui.setRoot
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug fuer README und Homepage: zeichnet die Familienansicht mit echten Daten des
 * Testservers als PNG, mit eigenem Einstellungsprofil. Laeuft nur mit gesetztem WT_FAMILIENBILD (Zielordner):
 *
 *   WT_FAMILIENBILD=/pfad WT_XREFS="I1 I130" WT_USER=admin WT_PASS=... ./gradlew :shared:desktopTest --rerun --tests '*FamilieBildErzeugen*'
 *
 * WT_URL (Vorgabe http://192.168.178.73:8095), WT_BILD="Breite:Hoehe:Dichte" (Vorgabe 1900:1000:1.25).
 */
class FamilieBildErzeugen {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun erzeugen() {
        val ziel = System.getenv("WT_FAMILIENBILD")?.let(::File) ?: return
        ziel.mkdirs()
        System.setProperty("java.util.prefs.userRoot", File(ziel, "prefs").absolutePath)
        val url = System.getenv("WT_URL") ?: "http://192.168.178.73:8095"
        val plat = DesktopPlattform().also { Desktop.plattform = it }
        plat.client.klartextUeberall = true
        plat.settings.baseUrl = url
        plat.client.baseUrl = url
        val vm = AppViewModel(plat)
        vm.login(System.getenv("WT_USER") ?: "admin", System.getenv("WT_PASS").orEmpty())
        val baum = System.getenv("WT_BAUM") ?: "falkenrath"
        var warten = 0
        while (vm.state.value.screen != Screen.Main && warten++ < 300) {
            Thread.sleep(100)
            if (vm.state.value.screen == Screen.Trees) vm.state.value.info?.trees?.firstOrNull { it.name == baum }?.let { vm.chooseTree(it) }
        }
        check(vm.state.value.screen == Screen.Main) { "Anmeldung fehlgeschlagen: ${vm.state.value.error} screen=${vm.state.value.screen} busy=${vm.state.value.busy} tree=${vm.state.value.tree?.name} info=${vm.state.value.info?.api}" }
        val (bw, bh, dichte) = (System.getenv("WT_BILD") ?: "1900:1000:1.25").split(':')
        (System.getenv("WT_XREFS") ?: "I1").split(' ').filter(String::isNotBlank).forEach { xref ->
            vm.setRoot(xref)
            Thread.sleep(1500)
            ImageComposeScene(width = bw.toInt(), height = bh.toInt(), density = Density(dichte.toFloat())) {
                val st by vm.state.collectAsState()
                DeskTheme { DeskFamilie(st, vm, {}, {}) }
            }.use { scene ->
                scene.render(); (1..40).forEach { scene.render(it * 50_000_000L); Thread.sleep(60) }
                File(ziel, "familie-$xref.png").writeBytes(scene.render(2_100_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
            }
        }
    }
}
