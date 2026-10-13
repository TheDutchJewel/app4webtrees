package de.bgghome.webtrees.nativ.gedaudit

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import de.bgghome.webtrees.nativ.Sprache
import de.bgghome.webtrees.nativ.data.DesktopAblage
import de.bgghome.webtrees.nativ.data.Settings
import de.bgghome.webtrees.nativ.desk.DeskTheme
import de.bgghome.webtrees.nativ.desk.GedauditAblage
import de.bgghome.webtrees.nativ.desk.GedauditFenster
import de.bgghome.webtrees.nativ.desk.Marke
import de.bgghome.webtrees.nativ.desk.gedauditHilfeEinrichten
import de.bgghome.webtrees.nativ.desk.gedauditKopierTaste
import de.bgghome.webtrees.nativ.ui.LocalAppName
import java.io.File
import javax.swing.UIManager

/**
 * gedaudit: ein Fenster um die Plausibilitaetspruefung des geteilten Kerns, fuer eine GEDCOM-Datei. Kein Server,
 * keine Anmeldung; die Datei kommt als Startargument (Doppelklick auf .ged) oder ueber Datei > Oeffnen.
 */
fun main(args: Array<String>) {
    Sprache.start(Settings(GedauditAblage.prefs))
    Marke.programm = "gedaudit"
    gedauditHilfeEinrichten()
    if (System.getProperty("os.name").orEmpty().startsWith("Mac")) System.setProperty("apple.laf.useScreenMenuBar", "true")
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
    val startDatei = args.firstOrNull { !it.startsWith("-") }?.let(::File)?.takeIf { it.isFile }
    val fenster = FensterAblage()

    application {
        val state = remember { fenster.laden() }
        Window(
            onCloseRequest = { fenster.sichern(state); exitApplication() },
            title = "gedaudit",
            icon = remember { Marke.icon()?.let { BitmapPainter(it) } },
            state = state,
            // Strg+C kopiert den Personentext des gewaehlten Treffers, wenn kein Eingabefeld die Taste nimmt (wie wtWin)
            onKeyEvent = ::gedauditKopierTaste,
        ) {
            CompositionLocalProvider(LocalAppName provides "gedaudit") {
                DeskTheme { Sprache.Umgebung { GedauditFenster(this, startDatei, onQuit = { fenster.sichern(state); exitApplication() }) } }
            }
        }
    }
}

/** Groesse, Lage und Maximierung des Fensters in den gedaudit-Einstellungen (wie bei wtWin). */
private class FensterAblage {
    private val prefs = DesktopAblage("gedaudit")
    private fun zahl(key: String) = prefs.getString(key, null)?.toFloatOrNull()

    fun laden(): WindowState {
        val w = zahl("win_w"); val h = zahl("win_h"); val x = zahl("win_x"); val y = zahl("win_y")
        val schirm = runCatching {
            val gc = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
            val skala = gc.defaultTransform.scaleX.toFloat()
            val b = gc.bounds; val rand = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc)
            DpSize(((b.width - rand.left - rand.right) / skala).dp, ((b.height - rand.top - rand.bottom) / skala).dp)
        }.getOrNull()
        val breite = minOf(w ?: 1240f, schirm?.width?.value ?: 1240f)
        val hoehe = minOf(h ?: 860f, (schirm?.height?.value ?: 860f) - 40f)
        return WindowState(
            placement = if (prefs.getBoolean("win_max", false)) WindowPlacement.Maximized else WindowPlacement.Floating,
            position = if (x != null && y != null) WindowPosition(x.dp, y.dp) else WindowPosition.PlatformDefault,
            size = DpSize(breite.dp, hoehe.dp),
        )
    }

    fun sichern(state: WindowState) {
        prefs.putBoolean("win_max", state.placement == WindowPlacement.Maximized)
        if (state.placement == WindowPlacement.Floating) {
            prefs.putString("win_w", state.size.width.value.toString())
            prefs.putString("win_h", state.size.height.value.toString())
            (state.position as? WindowPosition.Absolute)?.let { prefs.putString("win_x", it.x.value.toString()); prefs.putString("win_y", it.y.value.toString()) }
        }
    }
}
