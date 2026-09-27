package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.api.exportTree
import de.bgghome.webtrees.nativ.data.Ablage
import de.bgghome.webtrees.nativ.pruefung.pruefen
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Kein Test, sondern ein Werkzeug: Plausibilitaetspruefung ueber den Export des lokalen Testservers, Trefferzahlen
 * auf die Konsole, das Ergebnis als PDF und die ersten Seiten als PNG. Nur mit gesetztem WT_PRUEF_ZIEL, z. B.
 *   WT_PRUEF_ZIEL=/pfad WT_BAUM=falkenrath WT_USER=admin WT_PASS=... ./gradlew :shared:desktopTest --rerun --tests '*PruefungErzeugen*'
 */
class PruefungErzeugen {
    private class Speicher : Ablage {
        private val m = mutableMapOf<String, Any?>()
        override fun getString(key: String, default: String?) = m[key] as? String ?: default
        override fun putString(key: String, value: String?) { m[key] = value }
        override fun getBoolean(key: String, default: Boolean) = m[key] as? Boolean ?: default
        override fun putBoolean(key: String, value: Boolean) { m[key] = value }
        override fun alle() = m.filterValues { it is String }.mapValues { it.value as String }
        override fun leeren() = m.clear()
    }

    @Test
    fun erzeugen() {
        val ziel = System.getenv("WT_PRUEF_ZIEL")?.let(::File) ?: return
        ziel.mkdirs()
        val client = WtClient(Speicher(), Speicher(), "wtTux/dev (Pruefung)").apply { baseUrl = System.getenv("WT_URL") ?: "http://127.0.0.1:8377" }
        val info = runBlocking { System.getenv("WT_USER")?.let { client.login(it, System.getenv("WT_PASS").orEmpty()) } ?: client.info() }
        (System.getenv("WT_BAUM") ?: "falkenrath").split(' ').filter(String::isNotBlank).forEach { baum ->
            val titel = info.trees.first { it.name == baum }.title
            val t0 = System.currentTimeMillis()
            val b = runBlocking { client.exportTree(baum) }
            val t1 = System.currentTimeMillis()
            val e = pruefen(b, de.bgghome.webtrees.nativ.pruefung.PruefOptionen(aus = emptySet()))
            val t2 = System.currentTimeMillis()
            println("$baum: ${b.individuals.size} Personen, ${b.families.size} Familien; Export ${t1 - t0} ms, Pruefung ${t2 - t1} ms, ${e.anzahl} Treffer")
            e.treffer.filterValues { it.isNotEmpty() }.forEach { (id, l) -> println("  $id  ${l.size}   z. B. ${l.first().person ?: l.first().familie}: ${l.first().text}") }
            val bytes = ByteArrayOutputStream().also { out -> listenPdf(pruefZeilen(e, b, titel, emptyMap()), "wtTux", titel).use { it.save(out) } }.toByteArray()
            File(ziel, "pruefung-$baum.pdf").writeBytes(bytes)
            Loader.loadPDF(bytes).use { d ->
                (0 until minOf(2, d.numberOfPages)).forEach { i -> ImageIO.write(PDFRenderer(d).renderImage(i, 1.6f), "png", File(ziel, "pruefung-$baum-s${i + 1}.png")) }
            }
        }
    }
}
