package de.bgghome.webtrees.nativ.lokal

import java.io.File
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
    private val basis = createTempDirectory("wtlokal").toFile()

    @Test fun einrichtenUndAnmelden() {
        if (!php.canExecute() || zip?.isFile != true) return
        System.setProperty("wtand.lokal", basis.absolutePath)
        try {
            val schritte = mutableListOf<String>()
            val (server, zugang) = LokaleEinrichtung(php, zip, api).einrichten("Familie Test") { schritte += it }
            try {
                assertTrue(LokalOrte.eingerichtet, schritte.toString())
                val client = de.bgghome.webtrees.nativ.api.WtClient(
                    SpeicherAblage(), SpeicherAblage(),
                    userAgent = "test",
                )
                client.baseUrl = server.adresse
                val info = kotlinx.coroutines.runBlocking { client.login(zugang.benutzer, zugang.passwort) }
                println("Info: $info")
                assertTrue(info.user.loggedIn && info.user.isAdmin, info.toString())
                assertTrue(info.trees.single().autoAccept, info.toString())
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
