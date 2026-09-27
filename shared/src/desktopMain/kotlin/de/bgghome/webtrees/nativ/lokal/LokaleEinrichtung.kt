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
 * "Neuen Stammbaum auf diesem PC anlegen" (Stufe 4): webtrees und api4webtrees entpacken, PHP starten, den
 * Einrichtungsassistenten ausfuellen und den ersten Stammbaum anlegen - dieselben Schritte wie im
 * nas4webtrees-Image (nas4webtrees-entry.py: run_setup_wizard, first_run, make_private).
 */
class LokaleEinrichtung(
    private val php: File,
    private val webtreesZip: File,
    private val apiZip: File?,
) {
    fun einrichten(
        titel: String,
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
        zugang.sichern()

        if (baeume().none { it == zugang.baum }) {
            schritt("Stammbaum „$titel“ wird angelegt …")
            cli("site-setting", "LANGUAGE", sprache)
            cli("site-setting", "TIMEZONE", java.util.TimeZone.getDefault().id)
            cli("tree", zugang.baum, "--create", "--title=$titel", pruefen = true)
            cli("site-setting", "DEFAULT_GEDCOM", zugang.baum)
            // Nur fuer Angemeldete (webtrees 2.2.6: Spalte gedcom.private) und kein Konto-Beantragen.
            sql("UPDATE wt_gedcom SET private=1 WHERE gedcom_name=?", zugang.baum)
            cli("site-setting", "USE_REGISTRATION_MODULE", "0")
            // Ein Benutzer, keine Moderation: eigene Aenderungen gelten sofort (wie in jedem Genealogie-Programm).
            cli("user-setting", zugang.benutzer, "auto_accept", "1")
        }
        schritt("Fertig.")
        return server to zugang
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

    private fun baeume(): List<String> =
        php("-r", "\$d=new PDO('sqlite:data/webtrees.sqlite');foreach(\$d->query('SELECT gedcom_name FROM wt_gedcom WHERE gedcom_id>0') as \$r)echo \$r[0],\"\\n\";")
            .lines().filter { it.isNotBlank() }

    private fun sql(anweisung: String, vararg werte: String) {
        php("-r", "\$d=new PDO('sqlite:data/webtrees.sqlite');\$d->prepare(\$argv[1])->execute(array_slice(\$argv,2));", anweisung, *werte)
    }

    /** webtrees-Kommandozeile (index.php mit Argumenten), wie wt() im nas4webtrees-Image. */
    private fun cli(vararg args: String, pruefen: Boolean = false): String =
        php("index.php", "--no-interaction", *args, pruefen = pruefen)

    private fun php(vararg args: String, pruefen: Boolean = true): String {
        val befehl = listOf(php.absolutePath, "-d", "memory_limit=1024M",
            "-d", "date.timezone=${java.util.TimeZone.getDefault().id}") +
            (if (args.first() == "-r") listOf(args[0], args[1], "--") + args.drop(2) else args.toList())
        val p = ProcessBuilder(befehl).directory(LokalOrte.webtrees).redirectErrorStream(true).start()
        val aus = p.inputStream.bufferedReader().readText()
        check(p.waitFor(120, TimeUnit.SECONDS)) { "PHP haengt: ${args.take(3)}" }
        if (pruefen) check(p.exitValue() == 0) { "PHP ${args.take(3)}: ${aus.trim().take(400)}" }
        return aus
    }

    companion object {
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
