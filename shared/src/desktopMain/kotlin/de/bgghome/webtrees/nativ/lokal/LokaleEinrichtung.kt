package de.bgghome.webtrees.nativ.lokal

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Zugang zum lokalen webtrees: Konto, das nur wtWin benutzt, und der zuletzt benutzte Port (gleiche Adresse =
 * Sitzungs-Cookie gilt weiter). Liegt neben webtrees im Benutzerordner, nur fuer den Benutzer lesbar - wer diese
 * Datei lesen kann, kann ohnehin die Datenbank lesen; der Server hoert nur auf 127.0.0.1.
 */
data class LokalerZugang(val benutzer: String, val passwort: String, val baum: String, val port: Int) {
    fun sichern() {
        val f = File(LokalOrte.basis, "zugang.properties")
        LokalOrte.basis.mkdirs()
        f.writeText("")
        runCatching { Files.setPosixFilePermissions(f.toPath(), PosixFilePermissions.fromString("rw-------")) }
        Properties().apply {
            setProperty("benutzer", benutzer); setProperty("passwort", passwort)
            setProperty("baum", baum); setProperty("port", port.toString())
        }.let { p -> f.outputStream().use { p.store(it, "wtWin/wtTux: lokales webtrees - nicht weitergeben") } }
    }

    companion object {
        fun laden(): LokalerZugang? = runCatching {
            val p = Properties().apply { File(LokalOrte.basis, "zugang.properties").inputStream().use(::load) }
            LokalerZugang(p.getProperty("benutzer"), p.getProperty("passwort"), p.getProperty("baum"),
                p.getProperty("port")?.toIntOrNull() ?: 0)
        }.getOrNull()
    }
}

/**
 * "Neuen Stammbaum auf diesem PC anlegen" (Stufe 4): webtrees, api4webtrees und Sammlungen entpacken, PHP starten, den
 * Einrichtungsassistenten ausfuellen und den ersten Stammbaum anlegen - dieselben Schritte wie im
 * nas4webtrees-Image (nas4webtrees-entry.py: run_setup_wizard, first_run, make_private).
 */
class LokaleEinrichtung(
    private val php: File,
    private val webtreesZip: File,
    private val apiZip: File?,
    /** Modul Sammlungen (Archiv); null = nicht im Paket. Wie api4webtrees: webtrees schaltet gefundene Module selbst ein. */
    private val sammlungenZip: File? = null,
) {
    fun einrichten(
        titel: String,
        gedcom: File? = null,
        anzeigename: String = System.getProperty("user.name").orEmpty(),
        sprache: String = "de",
        schritt: (String) -> Unit = {},
    ): Pair<LokalerServer, LokalerZugang> {
        val wt = LokalOrte.webtrees
        if (!File(wt, "index.php").isFile) {
            schritt("webtrees wird ausgepackt …")
            // Erst daneben auspacken, dann umbenennen: ein Abbruch hinterlaesst kein halbes webtrees.
            val tmp = File(LokalOrte.basis, "webtrees.neu").apply { deleteRecursively(); mkdirs() }
            LokalerServer.entpacken(webtreesZip, tmp)
            wt.deleteRecursively()
            check(tmp.renameTo(wt)) { "Konnte $tmp nicht nach $wt verschieben" }
        }
        if (apiZip != null) {
            schritt("api4webtrees wird eingerichtet …")
            val modul = File(wt, "modules_v4/api4webtrees")
            modul.deleteRecursively()
            LokalerServer.entpacken(apiZip, File(wt, "modules_v4"), ohneOberordner = false)
        }
        if (sammlungenZip != null) {
            schritt("Archiv (Sammlungen) wird eingerichtet …")
            File(wt, "modules_v4/sammlungen").deleteRecursively()
            LokalerServer.entpacken(sammlungenZip, File(wt, "modules_v4"), ohneOberordner = false)
        }

        val alt = LokalerZugang.laden()
        val server = LokalerServer(php)
        schritt("webtrees wird gestartet …")
        server.starten(wunschPort = alt?.port ?: 0)

        val zugang = if (!LokalOrte.eingerichtet || alt == null) {
            schritt("Datenbank und Konto werden angelegt …")
            val z = LokalerZugang(
                benutzer = benutzername(),
                passwort = zufallsPasswort(),
                baum = "stammbaum",
                port = server.port,
            )
            assistent(server, z, anzeigename.ifBlank { z.benutzer }, sprache)
            z
        } else alt.copy(port = server.port)
        // GEDCOM-Uebernahme nie in einen vorhandenen Baum (tree-import loescht dessen Daten): dann ein neuer daneben.
        val vorhanden = baeume()
        val baum = if (gedcom == null || zugang.baum !in vorhanden) zugang.baum
            else generateSequence(2) { it + 1 }.map { "${zugang.baum}$it" }.first { it !in vorhanden }
        val z = zugang.copy(baum = baum)
        z.sichern()

        if (baum !in vorhanden) {
            schritt("Stammbaum „$titel“ wird angelegt …")
            cli("site-setting", "LANGUAGE", sprache)
            cli("site-setting", "TIMEZONE", java.util.TimeZone.getDefault().id)
            cli("tree", baum, "--create", "--title=$titel", pruefen = true)
            cli("site-setting", "DEFAULT_GEDCOM", baum)
            // Nur fuer Angemeldete (webtrees 2.2.6: Spalte gedcom.private) und kein Konto-Beantragen.
            sql("UPDATE wt_gedcom SET private=1 WHERE gedcom_name=?", baum)
            cli("site-setting", "USE_REGISTRATION_MODULE", "0")
            // Ein Benutzer, keine Moderation: eigene Aenderungen gelten sofort (wie in jedem Genealogie-Programm).
            cli("user-setting", z.benutzer, "auto_accept", "1")
        }
        if (gedcom != null) {
            try {
                gedcomEinlesen(baum, gedcom, z.benutzer, schritt)
            } catch (e: Exception) {
                // Nichts Halbes stehen lassen: der eben angelegte Baum kommt weg (ausser es ist der allererste, dann bleibt
                // er leer), der Zugang zeigt wieder auf den alten, der Server steht - der naechste Versuch faengt sauber an.
                if (baum != zugang.baum) runCatching { einlesenSkript(z.benutzer, baum, "--loeschen") }
                zugang.sichern()
                server.beenden()
                throw e
            }
            cli("site-setting", "DEFAULT_GEDCOM", baum)
        }
        schritt("Fertig.")
        return server to z
    }

    /**
     * GEDCOM-Datei einlesen - nicht mit `tree-import`, sondern mit dem mitgelieferten Skript, das es so macht wie der
     * Import im Browser (Issue 1): tree-import liest die Datei roh, und eine Byte-Reihenfolge-Marke oder ein einzelner
     * unbrauchbarer Datensatz brechen alles ab, UTF-16 ergibt einen leeren Baum, ANSEL/ANSI Zeichensalat, und lebende
     * Personen landen als "Private" im Namensindex, weil auf der Kommandozeile niemand angemeldet ist. Was das Skript
     * meldet (Zeichensatz, uebersprungene Datensaetze), steht in import.log neben php.log.
     */
    private fun gedcomEinlesen(baum: String, gedcom: File, benutzer: String, schritt: (String) -> Unit) {
        schritt("„${gedcom.name}“ wird eingelesen …")
        // Kopie unter schlichtem Namen: der Weg zur Originaldatei (Netzlaufwerk, Sonderzeichen) geht so nie an PHP.
        val kopie = File(LokalOrte.basis, "import.ged")
        gedcom.copyTo(kopie, overwrite = true)
        LokalOrte.importProtokoll.writeText("== ${gedcom.absolutePath} (${java.time.LocalDateTime.now().withNano(0)})\n")
        try {
            val aus = einlesenSkript(benutzer, baum, kopie.absolutePath) { zeile ->
                if (zeile.startsWith("FORTSCHRITT ")) schritt("„${gedcom.name}“ wird eingelesen … ${zeile.substringAfter(' ')} Datensätze")
            }
            LokalOrte.importProtokoll.appendText(aus.lines().filterNot { it.startsWith("FORTSCHRITT ") }.joinToString("\n").trim() + "\n")
        } catch (e: Exception) {
            runCatching { LokalOrte.importProtokoll.appendText("FEHLGESCHLAGEN\n${e.message}\n") }
            throw e
        } finally {
            kopie.delete()
        }
    }

    /** Das Skript resources/lokal/gedcom-einlesen.php, neben router.php abgelegt und im webtrees-Ordner ausgefuehrt. */
    private fun einlesenSkript(vararg args: String, zeile: ((String) -> Unit)? = null): String {
        val quelle = checkNotNull(LokaleEinrichtung::class.java.classLoader?.getResourceAsStream("lokal/gedcom-einlesen.php")) {
            "gedcom-einlesen.php fehlt im Paket"
        }.use { it.readBytes() }
        val skript = File(LokalOrte.basis, "gedcom-einlesen.php").apply { writeBytes(quelle) }
        return php(skript.absolutePath, *args, pruefen = true, zeile = zeile)
    }

    /** Der webtrees-Assistent, Schritt 6 - wie im Browser, nur ohne Browser. */
    private fun assistent(server: LokalerServer, z: LokalerZugang, name: String, sprache: String) {
        val felder = mapOf(
            "lang" to sprache, "tblpfx" to "wt_", "baseurl" to "",
            "dbtype" to "sqlite", "dbhost" to "", "dbport" to "", "dbuser" to "", "dbpass" to "", "dbname" to "webtrees",
            "wtname" to name, "wtuser" to z.benutzer, "wtpass" to z.passwort, "wtemail" to "${z.benutzer}@localhost",
            "step" to "6",
        )
        val body = felder.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }
        val c = URI("${server.adresse}index.php").toURL().openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.instanceFollowRedirects = false
        c.connectTimeout = 5_000; c.readTimeout = 180_000
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val antwort = runCatching { (if (code >= 400) c.errorStream else c.inputStream)?.bufferedReader()?.readText() }.getOrNull().orEmpty()
        c.disconnect()
        check(LokalOrte.eingerichtet) {
            "webtrees-Einrichtung fehlgeschlagen (HTTP $code): " +
                antwort.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").take(400)
        }
    }

    // Im PHP-Code nie doppelte Anfuehrungszeichen: Java reicht sie unter Windows nicht sauber an das Programm weiter.
    private fun baeume(): List<String> =
        php("-r", "\$d=new PDO('sqlite:data/webtrees.sqlite');foreach(\$d->query('SELECT gedcom_name FROM wt_gedcom WHERE gedcom_id>0') as \$r)echo \$r[0],PHP_EOL;")
            .lines().filter { it.isNotBlank() }

    private fun sql(anweisung: String, vararg werte: String) {
        php("-r", "\$d=new PDO('sqlite:data/webtrees.sqlite');\$d->prepare(\$argv[1])->execute(array_slice(\$argv,2));", anweisung, *werte)
    }

    /** webtrees-Kommandozeile (index.php mit Argumenten), wie wt() im nas4webtrees-Image. */
    private fun cli(vararg args: String, pruefen: Boolean = false): String =
        php("index.php", "--no-interaction", *args, pruefen = pruefen)

    /** PHP im webtrees-Ordner ausfuehren; [zeile] bekommt jede Ausgabezeile sofort (Fortschritt). */
    private fun php(vararg args: String, pruefen: Boolean = true, zeile: ((String) -> Unit)? = null): String {
        val befehl = listOf(php.absolutePath, "-d", "memory_limit=1024M",
            "-d", "date.timezone=${java.util.TimeZone.getDefault().id}") +
            (if (args.first() == "-r") listOf(args[0], args[1], "--") + args.drop(2) else args.toList())
        val p = ProcessBuilder(befehl).directory(LokalOrte.webtrees).redirectErrorStream(true).start()
        val aus = StringBuilder()
        p.inputStream.bufferedReader().useLines { zeilen -> zeilen.forEach { aus.appendLine(it); zeile?.invoke(it) } }
        check(p.waitFor(120, TimeUnit.SECONDS)) { "PHP haengt: ${args.take(3)}" }
        // Der Fehler steht am Ende der Ausgabe, hinter Fortschrittsbalken und Fuellzeilen: das Ende zeigen, nicht den Anfang.
        if (pruefen) check(p.exitValue() == 0) {
            val was = args.take(3).joinToString(" ") { it.substringAfterLast('/').substringAfterLast('\\') }
            "PHP $was:\n${kern(aus.toString()).takeLast(1200)}"
        }
        return aus.toString()
    }

    /** Konsolenausgabe ohne Fortschrittsbalken, HTML-Reste und Fuellzeilen - fuer die Fehlermeldung. */
    private fun kern(aus: String): String = aus
        .replace(Regex("</?pre>"), "\n")
        .lines()
        .filterNot { it.matches(FORTSCHRITT) }
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    companion object {
        private val FORTSCHRITT = Regex("""\s*\d+/\d+ \[.*""")

        /** Windows-/Linux-Benutzername, auf das beschraenkt, was webtrees und Anmeldeformulare sicher vertragen. */
        fun benutzername(): String =
            System.getProperty("user.name").orEmpty().filter { it.isLetterOrDigit() || it in "._-" }.take(30).ifBlank { "ich" }

        fun zufallsPasswort(): String {
            val z = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
            val r = SecureRandom()
            return (1..24).map { z[r.nextInt(z.length)] }.joinToString("")
        }
    }
}
