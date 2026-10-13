package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import de.bgghome.webtrees.nativ.Desktop
import de.bgghome.webtrees.nativ.DesktopPlattform
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.ProfilePanel
import de.bgghome.webtrees.nativ.ui.Screen
import de.bgghome.webtrees.nativ.ui.chooseTree
import de.bgghome.webtrees.nativ.ui.login
import de.bgghome.webtrees.nativ.ui.select
import de.bgghome.webtrees.nativ.ui.setRoot
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug: das Hauptfenster im OFB-Aufbau (Symbolleiste, Personenliste, Buch, Haeuser,
 * Personentafel) mit echten Daten des Testservers als PNG. WT_OFBFENSTER=/pfad WT_XREF=I130 WT_ORT=Bienenbüttel.
 */
class OfbFensterBildErzeugen {
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun erzeugen() {
        val ziel = System.getenv("WT_OFBFENSTER")?.let(::File) ?: return
        ziel.mkdirs()
        System.setProperty("java.util.prefs.userRoot", File(ziel, "prefs").absolutePath)
        val url = System.getenv("WT_URL") ?: "http://192.168.178.73:8095"
        val plat = DesktopPlattform().also { Desktop.plattform = it }
        plat.client.klartextUeberall = true
        plat.settings.baseUrl = url; plat.client.baseUrl = url
        val vm = AppViewModel(plat)
        vm.login(System.getenv("WT_USER") ?: "admin", System.getenv("WT_PASS").orEmpty())
        val baum = System.getenv("WT_BAUM") ?: "falkenrath14"
        var warten = 0
        while (vm.state.value.screen != Screen.Main && warten++ < 300) {
            Thread.sleep(100)
            if (vm.state.value.screen == Screen.Trees) vm.state.value.info?.trees?.firstOrNull { it.name == baum }?.let { vm.chooseTree(it) }
        }
        check(vm.state.value.screen == Screen.Main) { "Anmeldung fehlgeschlagen: ${vm.state.value.error}" }
        var start = 0
        while ((vm.state.value.root == null || vm.state.value.busy) && start++ < 100) Thread.sleep(100)
        Thread.sleep(2500)   // der Start setzt die Startperson nach - erst danach die gewuenschte Person waehlen
        val xref = System.getenv("WT_XREF") ?: "I130"
        vm.setRoot(xref); vm.select(xref)
        var w = 0
        while ((vm.state.value.root != xref || vm.state.value.detail?.person?.xref != xref || vm.state.value.busy) && w++ < 100) Thread.sleep(100)
        Thread.sleep(2500)
        check(vm.state.value.root == xref) { "Person $xref nicht geladen" }
        System.getenv("WT_ORT")?.let { DeskLayout.prefs.putString("ofb_ort_$baum", it) }
        DeskLayout.prefs.putBoolean("symboltexte", true)
        System.getenv("WT_SPALTEN")?.split(':')?.let { (l, h) -> DeskLayout.prefs.putString("ofb_liste", l); DeskLayout.prefs.putString("ofb_haeuser", h) }
        val (bw, bh, dichte) = (System.getenv("WT_BILD") ?: "1900:1050:1.25").split(':')
        ImageComposeScene(width = bw.toInt(), height = bh.toInt(), density = Density(dichte.toFloat())) {
            val st by vm.state.collectAsState()
            DeskTheme {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    ClassicToolbar(st, vm, {}, {}, {}, {}, DeskNav(true, false, {}, {}), DeskDruck(st, vm, "wtTux"), symboltexte = true,
                        onMerkliste = {}, onListe = {}, onTabelle = {}, onTafel = {}, onPruefung = {}, onQuit = {}, onQuellen = {}, onOrte = {},
                        layout = DeskLayout.Ofb, onAufgaben = {}, onAenderungen = {}, onArchiv = {})
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        Box(Modifier.weight(1f).fillMaxHeight()) { DeskOfb(st, vm, {}, {}) }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Box(Modifier.width(380.dp).fillMaxHeight()) { st.detail?.let { ProfilePanel(st, it, vm, {}, onClose = null) } }
                    }
                }
            }
        }.use { scene ->
            scene.render(); (1..180).forEach { scene.render(it * 50_000_000L); Thread.sleep(80) }
            File(ziel, "ofb-fenster.png").writeBytes(scene.render(2_100_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }
}
