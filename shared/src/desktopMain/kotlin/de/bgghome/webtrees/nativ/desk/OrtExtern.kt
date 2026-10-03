package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.Desktop
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Was die Ortsansicht von aussen holt - wie im Ortsregister: GOV-Hierarchie (gov.genealogy.net), Bild und
 * Wikipedia-Artikel ueber Wikidata/Commons, GenWiki-Artikel zur GOV-Kennung, dazu die Suchlinks der Archivportale.
 * Alles laesst sich jederzeit neu holen und liegt darum nur 7 Tage im Zwischenspeicher auf dem PC
 * (~/.cache/app4webtrees/orte); dauerhaft im Stammbaum steht nur, was der Nutzer festlegt (GOV-Kennung, Fotos am _LOC).
 * Ohne Koordinaten des Orts kein Wikidata-Treffer: ein Name allein ist zu oft mehrdeutig - lieber nichts als das Falsche.
 */

/** Eine Stufe der GOV-Hierarchie: Name und Zeitraum, in dem der Ort zu ihr gehoerte. */
data class GovStufe(val id: String, val name: String, val von: Int?, val bis: Int?)

/** Bild aus Wikimedia Commons mit Angaben fuer den Bildnachweis. */
data class OrtBild(val bild: String, val seite: String, val urheber: String?, val lizenz: String?)

data class OrtWikimedia(val qid: String?, val bild: OrtBild?, val wikipedia: String?, val galerie: List<OrtBild> = emptyList())

/** GOV-Objekt, so weit die Ortsansicht es braucht. */
data class GovObjekt(val id: String, val name: String, val teilVon: List<Triple<String, Int?, Int?>>, val extern: List<String>)

object OrtExtern {
    private const val GUELTIG_MS = 7L * 24 * 3600 * 1000
    private const val MAX_TIEFE = 12
    private const val MAX_KM = 30.0
    var userAgent = "app4webtrees (+https://github.com/thobgg/app4webtrees)"
    private val http by lazy { OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build() }
    private val json = Json { ignoreUnknownKeys = true }

    private fun ordner(): File = (runCatching { File(Desktop.plattform.cacheOrdner, "orte") }.getOrNull()
        ?: File(System.getProperty("java.io.tmpdir"), "app4webtrees-orte")).apply { mkdirs() }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /** GET mit Zwischenspeicher; faellt der Dienst aus, gilt der alte Stand weiter. Null, wenn es nichts gibt. */
    private fun holen(url: String): JsonElement? {
        val name = MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        val datei = File(ordner(), "$name.json")
        if (datei.isFile && System.currentTimeMillis() - datei.lastModified() < GUELTIG_MS) {
            runCatching { return json.parseToJsonElement(datei.readText()) }
        }
        val neu = runCatching {
            http.newCall(Request.Builder().url(url).header("User-Agent", userAgent).build()).execute().use { a ->
                if (a.isSuccessful) a.body?.string() else null
            }
        }.getOrNull()
        if (neu != null) {
            runCatching { datei.writeText(neu) }
            return runCatching { json.parseToJsonElement(neu) }.getOrNull()
        }
        return if (datei.isFile) runCatching { json.parseToJsonElement(datei.readText()) }.getOrNull() else null
    }

    private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
    private fun JsonElement?.arr(): JsonArray? = this as? JsonArray
    private fun JsonElement?.str(): String? = runCatching { this?.jsonPrimitive?.contentOrNull }.getOrNull()
    private fun JsonElement?.int(): Int? = runCatching { this?.jsonPrimitive?.intOrNull }.getOrNull()

    /** Julianischer Tag -> Jahr (gregorianisch). */
    internal fun jdJahr(jd: Int): Int {
        val a = jd + 32044; val b = (4 * a + 3) / 146097; val c = a - 146097 * b / 4
        val d = (4 * c + 3) / 1461; val e = c - 1461 * d / 4; val m = (5 * e + 2) / 153
        return 100 * b + d - 4800 + m / 10
    }

    /** GOV-Sprachkennung (ISO 639-2) zur Oberflaechensprache. */
    private fun govSprache(s: String) = when (s) { "de" -> "deu"; "fr" -> "fre"; "nl" -> "dut"; "es" -> "spa"; else -> "eng" }

    fun govObjekt(id: String, sprache: String = Locale.getDefault().language): GovObjekt? {
        val o = holen("https://gov.genealogy.net/api/getObject?itemId=" + enc(id)).obj() ?: return null
        val namen = o["name"].arr().orEmpty().mapNotNull { it.obj() }
        fun laeuftNoch(n: JsonObject) = n["endYear"] == null && n["timespan"].obj()?.get("end") == null
        val name = (namen.firstOrNull { it["lang"].str() == govSprache(sprache) && laeuftNoch(it) }
            ?: namen.firstOrNull { it["lang"].str() == "deu" && laeuftNoch(it) }
            ?: namen.firstOrNull { laeuftNoch(it) } ?: namen.firstOrNull())?.get("value").str() ?: id
        // Zeitraum steht als beginYear/endYear oder als timespan mit julianischem Tag
        fun jahr(t: JsonObject?, ende: String): Int? = t?.get("timespan").obj()?.get(ende).obj()?.get("jd").int()?.let(::jdJahr)
        val teil = o["partOf"].arr().orEmpty().mapNotNull { it.obj() }.mapNotNull { p ->
            p["ref"].str()?.let { Triple(it, p["beginYear"].int() ?: jahr(p, "begin"), p["endYear"].int() ?: jahr(p, "end")) }
        }
        val extern = o["externalReference"].arr().orEmpty().mapNotNull { it.obj()?.get("value").str() }
        return GovObjekt(id, name, teil, extern)
    }

    /**
     * Die Kette nach oben: [heute] folgt der Zugehoerigkeit ohne Ende (bis heute), sonst der aeltesten beendeten -
     * wie MODE_CURRENT/MODE_HISTORICAL im Ortsregister. Erste Stufe ist der Ort selbst.
     */
    fun govKette(id: String, heute: Boolean, sprache: String = Locale.getDefault().language): List<GovStufe> {
        val kette = mutableListOf<GovStufe>()
        val gesehen = mutableSetOf<String>()
        var aktuell: Triple<String, Int?, Int?>? = Triple(id, null, null)
        while (aktuell != null && kette.size < MAX_TIEFE && gesehen.add(aktuell.first)) {
            val o = govObjekt(aktuell.first, sprache) ?: break
            kette += GovStufe(o.id, o.name, aktuell.second, aktuell.third)
            aktuell = if (heute) o.teilVon.filter { it.third == null }.maxByOrNull { it.second ?: Int.MIN_VALUE }
            else o.teilVon.filter { it.third != null }.minByOrNull { it.second ?: Int.MAX_VALUE } ?: o.teilVon.firstOrNull { it.third == null }
        }
        return kette
    }

    /** Hat die heutige Kette eine andere Gestalt als die historische? Dann lohnt es, beide zu zeigen. */
    fun govKetten(id: String): Pair<List<GovStufe>, List<GovStufe>> {
        val heute = govKette(id, true)
        val frueher = govKette(id, false)
        return heute to (if (frueher.map { it.id } == heute.map { it.id }) emptyList() else frueher)
    }

    private fun km(a: Double, b: Double, c: Double, d: Double): Double {
        val r = Math.toRadians(c - a); val s = Math.toRadians(d - b)
        val h = sin(r / 2).pow(2) + cos(Math.toRadians(a)) * cos(Math.toRadians(c)) * sin(s / 2).pow(2)
        return 2 * 6371 * asin(sqrt(h))
    }

    private fun claim(qid: String, prop: String): JsonObject? =
        holen("https://www.wikidata.org/w/api.php?action=wbgetclaims&format=json&entity=$qid&property=$prop")
            .obj()?.get("claims").obj()?.get(prop).arr()?.firstOrNull().obj()?.get("mainsnak").obj()?.get("datavalue").obj()

    /**
     * Wikidata zum Ort: die Kennung aus GOV, sonst Treffer zum Namen, die hoechstens 30 km von den Koordinaten des Orts
     * liegen (naechster zuerst). Bild vom ersten Treffer, der eins hat; Wikipedia in der Oberflaechensprache.
     */
    fun wikimedia(name: String, lat: Double?, lng: Double?, govQid: String?, sprache: String = Locale.getDefault().language, suche: String = name): OrtWikimedia {
        val kandidaten: List<String> = if (govQid != null) listOf(govQid) else {
            if (lat == null || lng == null) return OrtWikimedia(null, null, null)
            val treffer = holen("https://www.wikidata.org/w/api.php?action=wbsearchentities&format=json&type=item&limit=7&language=$sprache&search=" + enc(name))
                .obj()?.get("search").arr().orEmpty().mapNotNull { it.obj()?.get("id").str() }
            treffer.mapNotNull { q ->
                val v = claim(q, "P625")?.get("value").obj() ?: return@mapNotNull null
                val la = runCatching { v["latitude"]?.jsonPrimitive?.doubleOrNull }.getOrNull() ?: return@mapNotNull null
                val lo = runCatching { v["longitude"]?.jsonPrimitive?.doubleOrNull }.getOrNull() ?: return@mapNotNull null
                q to km(lat, lng, la, lo)
            }.filter { it.second <= MAX_KM }.sortedBy { it.second }.map { it.first }
        }
        if (kandidaten.isEmpty()) return OrtWikimedia(null, null, null)
        val bild = kandidaten.firstNotNullOfOrNull { q -> claim(q, "P18")?.get("value").str() }?.let(::commonsBild)
        val wiki = kandidaten.firstNotNullOfOrNull { q ->
            holen("https://www.wikidata.org/w/api.php?action=wbgetentities&format=json&props=sitelinks&sitefilter=${sprache}wiki&ids=$q")
                .obj()?.get("entities").obj()?.get(q).obj()?.get("sitelinks").obj()?.get("${sprache}wiki").obj()?.get("title").str()
        }?.let { "https://$sprache.wikipedia.org/wiki/" + enc(it.replace(' ', '_')).replace("%2F", "/") }
        // Galerie nur zu einem bestaetigten Ort (wie im Ortsregister), das Hauptbild nicht doppelt
        val galerie = galerie(suche).filter { it.seite != bild?.seite }.take(6)
        return OrtWikimedia(kandidaten.first(), bild, wiki, galerie)
    }

    /**
     * Weitere Bilder zum Ort aus Wikimedia Commons: Dateisuche nach dem Ortsnamen (mit dem uebergeordneten Ort, damit
     * gleichnamige Orte anderswo seltener dazwischenrutschen), ohne Wappen und Karten als SVG/PNG, hoechstens 8.
     */
    private fun galerie(suche: String): List<OrtBild> {
        val titel = holen("https://commons.wikimedia.org/w/api.php?action=query&format=json&list=search&srnamespace=6&srlimit=14&srsearch=" + enc(suche))
            .obj()?.get("query").obj()?.get("search").arr().orEmpty().mapNotNull { it.obj()?.get("title").str() }
            .filter { t -> t.lowercase().let { !it.endsWith(".svg") && !it.endsWith(".png") } }.take(8)
        if (titel.isEmpty()) return emptyList()
        val seiten = holen("https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=1200&titles="
            + titel.joinToString("|") { enc(it) }).obj()?.get("query").obj()?.get("pages").obj() ?: return emptyList()
        return seiten.values.mapNotNull { seite ->
            val info = seite.obj()?.get("imageinfo").arr()?.firstOrNull().obj() ?: return@mapNotNull null
            val meta = info["extmetadata"].obj()
            fun m(k: String) = meta?.get(k).obj()?.get("value").str()?.replace(Regex("<[^>]*>"), "")?.trim()?.takeIf(String::isNotEmpty)
            val url = info["thumburl"].str() ?: info["url"].str() ?: return@mapNotNull null
            OrtBild(url, info["descriptionurl"].str() ?: return@mapNotNull null, m("Artist"), m("LicenseShortName"))
        }
    }

    private fun commonsBild(datei: String): OrtBild? {
        val seiten = holen("https://commons.wikimedia.org/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url|extmetadata&iiurlwidth=1200&titles=" + enc("File:$datei"))
            .obj()?.get("query").obj()?.get("pages").obj() ?: return null
        val info = seiten.values.firstOrNull().obj()?.get("imageinfo").arr()?.firstOrNull().obj() ?: return null
        val meta = info["extmetadata"].obj()
        fun m(k: String) = meta?.get(k).obj()?.get("value").str()?.replace(Regex("<[^>]*>"), "")?.trim()?.takeIf(String::isNotEmpty)
        val url = info["thumburl"].str() ?: info["url"].str() ?: return null
        return OrtBild(url, info["descriptionurl"].str() ?: "https://commons.wikimedia.org/wiki/File:" + enc(datei), m("Artist"), m("LicenseShortName"))
    }

    /** GenWiki-Artikel zur GOV-Kennung - nur, wenn GenWiki ihn kennt (nichts geraten). */
    fun genWiki(gov: String): String? {
        val q = holen("https://wiki.genealogy.net/api.php?action=query&format=json&redirects=1&prop=info&inprop=url&titles=" + enc("GOV:$gov")).obj()?.get("query").obj() ?: return null
        if (q["redirects"].arr().isNullOrEmpty()) return null
        return q["pages"].obj()?.values?.firstOrNull().obj()?.takeIf { it["missing"] == null }?.get("fullurl").str()
    }

    /** Externe GOV-Kennungen mit gepruefter Adresse (Auswahl wie im Ortsregister). */
    fun externeLinks(extern: List<String>): List<Pair<String, String>> = extern.mapNotNull { e ->
        val (p, id) = e.split(':', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        when (p.lowercase()) {
            "gnd" -> "GND" to "https://d-nb.info/gnd/$id"
            "geonames" -> "GeoNames" to "https://www.geonames.org/$id"
            "wikidata" -> "Wikidata" to "https://www.wikidata.org/wiki/$id"
            "leobw" -> "LEO-BW" to "https://www.leo-bw.de/detail/-/Detail/details/ORT/$id"
            else -> null
        }
    }

    /** Suchlinks der Archivportale zum Ortsnamen (Adressen wie im Ortsregister, dort im Browser geprueft). */
    fun suchLinks(name: String): List<Pair<String, String>> = listOf(
        "Archion" to "https://www.archion.de/de/suche?location_name=" + enc(name),
        "Matricula" to "https://data.matricula-online.eu/de/suchen/?place=" + enc(name),
        "Archivportal-D" to "https://www.archivportal-d.de/objekte?query=" + enc("place:$name"),
        "DDB" to "https://www.deutsche-digitale-bibliothek.de/searchresults?query=" + enc(name),
    )
}
