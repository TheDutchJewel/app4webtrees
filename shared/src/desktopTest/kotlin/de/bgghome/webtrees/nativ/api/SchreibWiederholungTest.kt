package de.bgghome.webtrees.nativ.api

import com.sun.net.httpserver.HttpServer
import de.bgghome.webtrees.nativ.data.Ablage
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Gutachten 27.09.2026: Nach einer Nicht-JSON-Antwort sendete der Client einen Schreibzugriff noch einmal - kam die
 * Fehlerseite erst NACH dem Speichern (PHP-Fehler), entstand die Aenderung doppelt. Jetzt nur noch nach einer
 * Umleitung (so lehnt webtrees ein abgelaufenes CSRF-Token ab, bevor es etwas ausfuehrt).
 */
class SchreibWiederholungTest {
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    private val posts = AtomicInteger()
    private val infos = AtomicInteger()
    /** Antwort auf den n-ten POST (0-basiert): "umleiten", "fehlerseite" oder "ok". */
    private var antworten = listOf<String>()

    init {
        server.createContext("/") { ex ->
            val route = ex.requestURI.query.orEmpty().split('&').firstOrNull { it.startsWith("route=") }.orEmpty()
            fun senden(code: Int, typ: String, text: String) {
                val b = text.toByteArray()
                ex.responseHeaders.add("Content-Type", typ)
                ex.sendResponseHeaders(code, b.size.toLong())
                ex.responseBody.use { it.write(b) }
            }
            when {
                route.endsWith("Info") ->
                    senden(200, "application/json", """{"api":17,"module":"1.9.5","csrf":"T${infos.incrementAndGet()}"}""")
                ex.requestMethod == "POST" -> when (antworten.getOrElse(posts.getAndIncrement()) { "ok" }) {
                    "umleiten" -> { ex.responseHeaders.add("Location", ex.requestURI.toString()); senden(302, "text/html", "") }
                    "fehlerseite" -> senden(500, "text/html", "<b>Fatal error</b>")
                    else -> senden(200, "application/json", """{"ok":true,"xref":"I1"}""")
                }
                // GET nach der Umleitung: webtrees zeigt eine HTML-Seite mit "Formular abgelaufen".
                else -> senden(200, "text/html", "<html>Dieses Formular ist abgelaufen</html>")
            }
        }
        server.start()
    }

    @AfterTest fun stop() = server.stop(0)

    private fun client() = WtClient(SpeicherAblage(), SpeicherAblage(), userAgent = "test").also {
        it.baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @Test fun csrfUmleitungWirdEinmalWiederholt() = runBlocking {
        antworten = listOf("umleiten", "ok")
        val r = client().deleteFact("baum", "I1", "abc")
        assertTrue(r.ok)
        assertEquals(2, posts.get())
    }

    @Test fun fehlerseiteOhneUmleitungWirdNichtWiederholt() {
        antworten = listOf("fehlerseite", "ok")
        assertFailsWith<WriteUnclearException> { runBlocking { client().deleteFact("baum", "I1", "abc") } }
        assertEquals(1, posts.get(), "nach einer Fehlerseite darf nicht noch einmal gesendet werden")
    }

    @Test fun fehlerseiteNachWiederholungWirdNichtVerschluckt() {
        antworten = listOf("umleiten", "fehlerseite", "ok")
        assertFailsWith<WriteUnclearException> { runBlocking { client().deleteFact("baum", "I1", "abc") } }
        assertEquals(2, posts.get())
    }

    @Test fun zweimalUmgeleitetBleibtNotJson() {
        // Sitzung wirklich weg (auch mit frischem Token umgeleitet): wie bisher NotJsonException, die das ViewModel
        // als "abgemeldet" erkennt.
        antworten = listOf("umleiten", "umleiten")
        val e = assertFailsWith<NotJsonException> { runBlocking { client().deleteFact("baum", "I1", "abc") } }
        assertTrue(e.umgeleitet)
        assertEquals(2, posts.get())
    }
}

private class SpeicherAblage : Ablage {
    private val m = mutableMapOf<String, Any>()
    override fun getString(key: String, default: String?) = m[key] as? String ?: default
    override fun putString(key: String, value: String?) { if (value == null) m.remove(key) else m[key] = value }
    override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { m[key] = value }
    override fun alle() = m.filterValues { it is String }.mapValues { it.value as String }
    override fun leeren() = m.clear()
}
