package de.bgghome.webtrees.nativ.lokal

import de.bgghome.webtrees.nativ.DesktopPlattform
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Stammbaum auf diesem PC (Stufe 4): haelt den PHP-Server fuer die Laufzeit von wtWin. Der Rest des Programms merkt
 * davon nichts - fuer ihn ist das ein webtrees unter http://127.0.0.1:<port>/, bei dem man schon angemeldet ist.
 */
object LokalBetrieb {
    private var server: LokalerServer? = null

    /** webtrees-ZIP und api4webtrees-ZIP liegen im Paket unter resources/webtrees (desktop/build.gradle.kts). */
    private fun paketDatei(praefix: String, env: String): File? {
        System.getenv(env)?.takeIf { it.isNotBlank() }?.let { return File(it).takeIf(File::isFile) }
        val res = System.getProperty("compose.application.resources.dir") ?: return null
        return File(res, "webtrees").listFiles()?.filter { it.name.startsWith(praefix) && it.name.endsWith(".zip") }?.maxByOrNull { it.name }
    }
    private fun webtreesZip() = paketDatei("webtrees-", "WTAND_WEBTREES_ZIP")
    private fun apiZip() = paketDatei("api4webtrees-", "WTAND_API_ZIP")

    /** Bringt dieses Paket alles mit, um einen Stammbaum auf dem PC anzulegen? */
    val verfuegbar: Boolean get() = mitgeliefertesPhp() != null && webtreesZip() != null

    /** Fuer "Ueber": ob der Stammbaum auf diesem PC moeglich ist, sonst was im Paket fehlt und wo gesucht wurde. */
    fun zustand(): String {
        val php = runCatching { mitgeliefertesPhp() }.getOrNull()
        val wt = webtreesZip()
        return if (php != null && wt != null) "Stammbaum auf diesem PC: bereit (${wt.name})"
        else "Stammbaum auf diesem PC: nicht verfügbar – PHP ${if (php != null) "ok" else "fehlt"}, webtrees ${if (wt != null) "ok" else "fehlt"}" +
            " (${System.getProperty("compose.application.resources.dir") ?: "kein Ressourcen-Ordner"})"
    }

    fun istLokal(baseUrl: String): Boolean = baseUrl.startsWith("http://127.0.0.1:")

    /**
     * "Neuen Stammbaum auf diesem PC anlegen", mit [gedcom] aus einer GEDCOM-Datei - blockiert (Auspacken, Datenbank anlegen), also nicht auf dem
     * Hauptthread. Danach zeigt [LokalerZugang] auf den laufenden Server; anmelden macht der Aufrufer.
     */
    fun anlegen(titel: String, gedcom: File? = null, schritt: (String) -> Unit): Pair<String, LokalerZugang> {
        val php = checkNotNull(mitgeliefertesPhp()) { "PHP fehlt im Paket" }
        val zip = checkNotNull(webtreesZip()) { "webtrees fehlt im Paket" }
        server?.beenden()
        val (s, z) = LokaleEinrichtung(php, zip, apiZip()).einrichten(titel, gedcom, schritt = schritt)
        server = s
        return s.adresse to z
    }

    /**
     * Beim Programmstart, vor dem ersten Blick auf den Server: war zuletzt der Stammbaum auf diesem PC gewaehlt,
     * PHP starten und still anmelden. Scheitert das, bleibt es beim normalen Weg (Startbildschirm mit Fehler).
     */
    fun beimStart(plattform: DesktopPlattform) {
        val settings = plattform.settings
        if (!istLokal(settings.baseUrl)) return
        val zugang = LokalerZugang.laden() ?: return
        val php = mitgeliefertesPhp() ?: return
        runCatching {
            val s = LokalerServer(php).also { server = it }
            s.starten(wunschPort = zugang.port)
            if (s.port != zugang.port) zugang.copy(port = s.port).sichern()
            settings.baseUrl = s.adresse.trimEnd('/')
            plattform.client.baseUrl = settings.baseUrl
            runBlocking { plattform.client.login(zugang.benutzer, zugang.passwort) }
            settings.userName = zugang.benutzer
        }.onFailure { System.err.println("Lokaler Stammbaum: ${it.message}") }
    }

    fun beenden() {
        server?.beenden()
        server = null
    }
}
