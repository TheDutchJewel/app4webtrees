package de.bgghome.webtrees.nativ.lokal

import java.io.File
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Der lokale webtrees-Server (Stufe 4). Braucht ein PHP (WTAND_PHP oder /usr/bin/php) und die webtrees-ZIP
 * (WTAND_WEBTREES_ZIP); fehlt eins, wird still uebersprungen. Kern ist die Sperre fuer data/.
 */
class LokalerServerTest {
    private val php = (System.getenv("WTAND_PHP") ?: "/usr/bin/php").let(::File)
    private val zip = System.getenv("WTAND_WEBTREES_ZIP")?.let(::File)
    private val basis = createTempDirectory("wtlokal").toFile()
    private var server: LokalerServer? = null

    @AfterTest fun aufraeumen() {
        server?.beenden()
        basis.deleteRecursively()
        System.clearProperty("wtand.lokal")
    }

    /** Roh ueber den Socket, damit Pfade wie /DATA./ unveraendert beim Server ankommen. */
    private fun status(port: Int, pfad: String): Int = Socket("127.0.0.1", port).use { s ->
        s.getOutputStream().write("GET $pfad HTTP/1.0\r\nHost: 127.0.0.1\r\n\r\n".toByteArray())
        s.getInputStream().bufferedReader().readLine().split(' ')[1].toInt()
    }

    @Test fun sperrtDatenordner() {
        if (!php.canExecute() || zip?.isFile != true) return
        System.setProperty("wtand.lokal", basis.absolutePath)
        LokalerServer.entpacken(zip, LokalOrte.webtrees)
        // So liegen Datenbank und Einstellungen nach der Einrichtung.
        File(LokalOrte.webtrees, "data/webtrees.sqlite").writeText("GEHEIM")
        File(LokalOrte.webtrees, "data/geheim.ini.php").writeText("; GEHEIM")

        val s = LokalerServer(php).also { server = it }
        s.starten()
        val p = s.port
        assertEquals(200, status(p, "/public/css/webtrees.min.css"))
        for (pfad in listOf(
            "/data/webtrees.sqlite", "/data/geheim.ini.php", "/DATA/webtrees.sqlite", "/data./webtrees.sqlite",
            "/data%20/webtrees.sqlite", "/Data%2Fwebtrees.sqlite", "/public/../data/webtrees.sqlite", "/data/",
            "/data/.htaccess", "/vendor/tecnickcom/tcpdf/tools/tcpdf_addfont.php",
            "/index.php/../data/webtrees.sqlite", "/.htaccess",
        )) {
            assertEquals(403, status(p, pfad), pfad)
        }
        // Die Startseite laeuft ueber index.php (ohne config: Weiterleitung oder Einrichtungsassistent).
        val c = URI("${s.adresse}index.php").toURL().openConnection() as HttpURLConnection
        c.instanceFollowRedirects = false
        assertTrue(c.responseCode in listOf(200, 302), "index.php liefert ${c.responseCode}")
        val body = runCatching { c.inputStream.bufferedReader().readText() }.getOrDefault("")
        assertTrue("GEHEIM" !in body)

        s.beenden()
        assertTrue(!s.laeuft)
    }
}

/** Ganze Einrichtung wie beim Klick auf "Neuen Stammbaum anlegen" - danach meldet sich der WtClient an. */
class LokaleEinrichtungTest {
    private val php = (System.getenv("WTAND_PHP") ?: "/usr/bin/php").let(::File)
    private val zip = System.getenv("WTAND_WEBTREES_ZIP")?.let(::File)
    private val api = System.getenv("WTAND_API_ZIP")?.let(::File)
    private val sammlungen = System.getenv("WTAND_SAMMLUNGEN_ZIP")?.let(::File)
    private val basis = createTempDirectory("wtlokal").toFile()

    @Test fun einrichtenUndAnmelden() {
        if (!php.canExecute() || zip?.isFile != true) return
        System.setProperty("wtand.lokal", basis.absolutePath)
        try {
            val schritte = mutableListOf<String>()
            val (server, zugang) = LokaleEinrichtung(php, zip, api, sammlungen).einrichten("Familie Test") { schritte += it }
            try {
                assertTrue(LokalOrte.eingerichtet, schritte.toString())
                // Modul Sammlungen (Archiv) liegt neben api4webtrees und ist eingeschaltet: die Archiv-Uebersicht antwortet
                if (sammlungen != null) assertTrue(File(LokalOrte.webtrees, "modules_v4/sammlungen/module.php").isFile, "Sammlungen fehlt")
                val client = de.bgghome.webtrees.nativ.api.WtClient(
                    SpeicherAblage(), SpeicherAblage(),
                    userAgent = "test",
                )
                client.baseUrl = server.adresse
                val info = kotlinx.coroutines.runBlocking { client.login(zugang.benutzer, zugang.passwort) }
                println("Info: $info")
                assertTrue(info.user.loggedIn && info.user.isAdmin, info.toString())
                assertTrue(info.trees.single().autoAccept, info.toString())
                if (sammlungen != null) {
                    val archiv = kotlinx.coroutines.runBlocking { client.archive(info.trees.single().name) }
                    println("Archiv: api=${archiv.api} darfHochladen=${archiv.darfHochladen}")
                    assertTrue(archiv.api >= 1, "Archiv-Route antwortet nicht: $archiv")
                }
                // Zweiter Lauf (Neustart): nichts neu anlegen, derselbe Zugang.
                server.beenden()
                val (s2, z2) = LokaleEinrichtung(php, zip, api).einrichten("Familie Test")
                assertEquals(zugang.passwort, z2.passwort)
                assertEquals(server.port, s2.port)
                s2.beenden()
            } finally { server.beenden() }
        } finally {
            basis.deleteRecursively()
            System.clearProperty("wtand.lokal")
        }
    }

    /** Umsteiger: Stammbaum aus einer GEDCOM-Datei; ein zweiter Import legt einen neuen Baum an statt zu ueberschreiben. */
    @Test fun ausGedcomUebernehmen() {
        val ged = File("../demo-tree/falkenrath.ged").takeIf { it.isFile } ?: File("demo-tree/falkenrath.ged")
        if (!php.canExecute() || zip?.isFile != true || !ged.isFile) return
        System.setProperty("wtand.lokal", basis.absolutePath)
        try {
            val (server, zugang) = LokaleEinrichtung(php, zip, api).einrichten("falkenrath", ged)
            try {
                val client = de.bgghome.webtrees.nativ.api.WtClient(SpeicherAblage(), SpeicherAblage(), userAgent = "test")
                client.baseUrl = server.adresse
                kotlinx.coroutines.runBlocking {
                    client.login(zugang.benutzer, zugang.passwort)
                    val leute = client.individuals(zugang.baum, "Falkenrath", 1).data
                    assertTrue(leute.size > 5, leute.toString())
                }
                server.beenden()
                val (s2, z2) = LokaleEinrichtung(php, zip, api).einrichten("zweiter", ged)
                try {
                    assertEquals(zugang.baum + "2", z2.baum)
                    val c2 = de.bgghome.webtrees.nativ.api.WtClient(SpeicherAblage(), SpeicherAblage(), userAgent = "test")
                    c2.baseUrl = s2.adresse
                    val info = kotlinx.coroutines.runBlocking { c2.login(z2.benutzer, z2.passwort) }
                    assertEquals(setOf(zugang.baum, z2.baum), info.trees.map { it.name }.toSet(), info.toString())
                } finally { s2.beenden() }
            } finally { server.beenden() }
        } finally {
            basis.deleteRecursively()
            System.clearProperty("wtand.lokal")
        }
    }
}

/**
 * Issue 1: GEDCOM-Dateien, wie andere Programme sie schreiben - Byte-Reihenfolge-Marke mit Windows-Zeilenenden, ANSI
 * (Windows-1252) mit doppeltem Kennzeichen, UTF-16 - und eine Datei, die gar keine GEDCOM ist. Lebende Personen stehen
 * mit Namen im Index (nicht "Private"), ein Fehlschlag laesst keinen halben Baum zurueck.
 */
class GedcomVariantenTest {
    private val php = (System.getenv("WTAND_PHP") ?: "/usr/bin/php").let(::File)
    private val zip = System.getenv("WTAND_WEBTREES_ZIP")?.let(::File)
    private val api = System.getenv("WTAND_API_ZIP")?.let(::File)
    private val basis = createTempDirectory("wtlokal").toFile()

    private fun namen(): List<String> = ProcessBuilder(
        php.absolutePath, "-r",
        "\$d=new PDO('sqlite:data/webtrees.sqlite');foreach(\$d->query('SELECT n_full FROM wt_name') as \$r)echo \$r[0],PHP_EOL;",
    ).directory(LokalOrte.webtrees).redirectErrorStream(true).start().inputStream.bufferedReader().readText().lines()

    private fun baeume(): List<String> = ProcessBuilder(
        php.absolutePath, "-r",
        "\$d=new PDO('sqlite:data/webtrees.sqlite');foreach(\$d->query('SELECT gedcom_name FROM wt_gedcom WHERE gedcom_id>0') as \$r)echo \$r[0],PHP_EOL;",
    ).directory(LokalOrte.webtrees).redirectErrorStream(true).start().inputStream.bufferedReader().readText().lines().filter { it.isNotBlank() }

    private fun gefunden(server: LokalerServer, zugang: LokalerZugang, name: String): Int {
        val client = de.bgghome.webtrees.nativ.api.WtClient(SpeicherAblage(), SpeicherAblage(), userAgent = "test")
        client.baseUrl = server.adresse
        return kotlinx.coroutines.runBlocking {
            client.login(zugang.benutzer, zugang.passwort)
            client.individuals(zugang.baum, name, 1).data.size
        }
    }

    @Test fun varianten() {
        val ged = File("../demo-tree/falkenrath.ged").takeIf { it.isFile } ?: File("demo-tree/falkenrath.ged")
        if (!php.canExecute() || zip?.isFile != true || !ged.isFile) return
        System.setProperty("wtand.lokal", basis.absolutePath)
        val text = ged.readText()
        val bom = File(basis, "bom.ged").apply { writeBytes("\uFEFF".toByteArray() + text.replace("\n", "\r\n").toByteArray()) }
        val ansi = File(basis, "ansi.ged").apply {
            writeBytes(text.replace("1 CHAR UTF-8", "1 CHAR ANSI").replace("0 TRLR", "0 @I1@ INDI\n1 NAME Doppelt /Kennzeichen/\n0 TRLR")
                .toByteArray(java.nio.charset.Charset.forName("windows-1252")))
        }
        val utf16 = File(basis, "utf16.ged").apply { writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)) }
        val keine = File(basis, "keine.ged").apply { writeText("Das ist keine GEDCOM-Datei.\n") }
        try {
            val (s1, z1) = LokaleEinrichtung(php, zip, api).einrichten("bom", bom)
            try {
                assertTrue(gefunden(s1, z1, "Krümmel") > 0, "Umlaut-Name nach BOM/CRLF nicht gefunden")
                assertTrue("Private" !in namen(), "Lebende stehen als Private im Namensindex")
            } finally { s1.beenden() }

            val (s2, z2) = LokaleEinrichtung(php, zip, api).einrichten("ansi", ansi)
            try {
                assertEquals(z1.baum + "2", z2.baum)
                assertTrue(gefunden(s2, z2, "Krümmel") > 0, "Umlaut-Name aus Windows-1252 nicht gefunden")
                val log = LokalOrte.importProtokoll.readText()
                assertTrue("CP1252" in log && "UEBERSPRUNGEN 1" in log && "@I1@" in log, log)
            } finally { s2.beenden() }

            val (s3, z3) = LokaleEinrichtung(php, zip, api).einrichten("utf16", utf16)
            try {
                assertTrue(gefunden(s3, z3, "Krümmel") > 0, "Umlaut-Name aus UTF-16 nicht gefunden")
            } finally { s3.beenden() }

            val vorher = baeume()
            val e = assertFailsWith<IllegalStateException> { LokaleEinrichtung(php, zip, api).einrichten("keine", keine) }
            assertTrue("0 HEAD" in e.message.orEmpty(), e.message)
            assertEquals(vorher, baeume())
            assertEquals(z3.baum, LokalerZugang.laden()?.baum)
            assertTrue("FEHLGESCHLAGEN" in LokalOrte.importProtokoll.readText())
        } finally {
            basis.deleteRecursively()
            System.clearProperty("wtand.lokal")
        }
    }
}

private class SpeicherAblage : de.bgghome.webtrees.nativ.data.Ablage {
    private val m = mutableMapOf<String, Any>()
    override fun getString(key: String, default: String?) = m[key] as? String ?: default
    override fun putString(key: String, value: String?) { if (value == null) m.remove(key) else m[key] = value }
    override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { m[key] = value }
    override fun alle() = m.filterValues { it is String }.mapValues { it.value as String }
    override fun leeren() = m.clear()
}
