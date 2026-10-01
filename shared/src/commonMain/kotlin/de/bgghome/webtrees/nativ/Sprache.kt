package de.bgghome.webtrees.nativ

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import de.bgghome.webtrees.nativ.data.Settings
import java.util.Locale

/**
 * Die Sprache der Oberflaeche: wie das System oder fest gewaehlt (Ansicht › Sprache; am Handy im Menue).
 *
 * Die Compose-Ressourcen (stringResource, Texte.t) richten sich nach java.util.Locale.getDefault(); die Wahl
 * setzt genau das. Damit folgen auch Datumsformate, Monatsnamen und die Sprache gegenueber dem Server.
 * Ohne Neustart: [Umgebung] baut die Oberflaeche beim Umschalten einmal neu auf (wie ein Fensterwechsel),
 * Daten und Anmeldung bleiben, weil sie im ViewModel liegen.
 * Uebersetzt ist, was in composeResources/values-xx liegt; fehlt die Systemsprache, spricht die App Englisch.
 */
object Sprache {
    /** Alle Sprachen mit Uebersetzung, in dieser Reihenfolge im Menue; der Name steht in der Sprache selbst. */
    val ALLE: List<Pair<String, String>> = listOf(
        "de" to "Deutsch", "en" to "English", "fr" to "Français", "nl" to "Nederlands", "es" to "Español",
    )
    val CODES: List<String> = ALLE.map { it.first }

    /** Die Sprache des Geraets beim Start - wohin „wie das System“ zurueckfuehrt. */
    private val system: Locale = Locale.getDefault()
    private var settings: Settings? = null

    /** Gewaehlter Code oder null = wie das System. Zustand, damit die Oberflaeche der Wahl folgt. */
    val wahl = mutableStateOf<String?>(null)

    /** Die wirksame Sprache: die Wahl, sonst die Systemsprache, falls uebersetzt, sonst Englisch. */
    val aktiv: String get() = wahl.value ?: systemSprache()

    fun systemSprache(): String = system.language.takeIf { it in CODES } ?: "en"

    /** Beim Start der App, vor der ersten Oberflaeche. */
    fun start(s: Settings) {
        settings = s
        wahl.value = s.language.takeIf { it in CODES }
        anwenden()
    }

    fun setzen(code: String?) {
        wahl.value = code?.takeIf { it in CODES }
        settings?.language = wahl.value.orEmpty()
        anwenden()
    }

    /**
     * Die Wahl auf java.util.Locale legen. Android setzt die Voreinstellung bei jeder Konfigurationsaenderung
     * (Drehen, Systemsprache) auf das System zurueck - darum ruft die Activity das erneut auf.
     */
    fun anwenden() {
        val w = wahl.value
        Locale.setDefault(if (w == null || w == system.language) system else Locale.forLanguageTag(w))
    }

    /** Um die Wurzel der Oberflaeche legen: beim Umschalten entsteht sie neu und liest alle Texte frisch. */
    @Composable
    fun Umgebung(content: @Composable () -> Unit) {
        key(aktiv) { content() }
    }
}
