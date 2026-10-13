package de.bgghome.webtrees.nativ.data

import androidx.compose.runtime.mutableStateOf
import java.util.Locale

/**
 * Wie Ortsnamen in den Ansichten erscheinen (Issue 18): vollstaendig wie in webtrees ("Amsterdam, Noord-Holland,
 * Nederland"), nur Ort und Land ("Amsterdam, Nederland") oder Ort und Laenderkuerzel nach ISO 3166 ("Amsterdam, NL" /
 * "Amsterdam, NLD"). Dazu ein Heimatland: Orte darin erscheinen nur mit dem Ortsnamen ("Amsterdam"), Orte anderswo nach
 * der gewaehlten Form. Das Land ist der letzte Teil des Ortsnamens; erkannt wird es ueber die Laendernamen in allen
 * Sprachen der App und die ISO-Kuerzel selbst (viele Stammbaeume schreiben schon "NLD" oder "DEU").
 *
 * Gilt fuer Ansichten, Listen, Tafeln und Karteikarten; Buecher haben ihre eigene Einstellung (Orte kuerzen), die
 * Ortsverwaltung und alle Eingabefelder zeigen immer den vollen Namen, denn dort wird gesucht und gespeichert.
 */
object Ortsanzeige {
    enum class Form { Voll, OrtLand, OrtIso2, OrtIso3 }

    /** Zustand, damit die Oberflaeche einer Aenderung folgt. */
    val form = mutableStateOf(Form.Voll)
    /** ISO-3166-Kuerzel (zwei Buchstaben) des Heimatlands, leer = keins. */
    val heimat = mutableStateOf("")

    private var settings: Settings? = null

    fun start(s: Settings) {
        settings = s
        form.value = Form.entries.firstOrNull { it.name == s.placeFormat } ?: Form.Voll
        heimat.value = s.homeCountry.uppercase()
    }

    fun setzen(f: Form, heimatland: String) {
        form.value = f
        heimat.value = (Laender.iso2(heimatland) ?: "").uppercase()
        settings?.let { it.placeFormat = f.name; it.homeCountry = heimat.value }
    }

    /** Der Name, wie er angezeigt wird; null oder leer bleibt leer. */
    fun text(name: String?): String = text(name, form.value, heimat.value)

    /** Dasselbe mit ausdruecklicher Form und Heimatland (Vorschau im Dialog). */
    fun text(name: String?, f: Form, h: String): String {
        val voll = name?.trim().orEmpty()
        if (voll.isEmpty()) return voll
        if (f == Form.Voll && h.isEmpty()) return voll
        val teile = Ortsnamen.teile(voll)
        if (teile.size < 2) return voll
        val land = teile.last()
        val iso = Laender.iso2(land)
        if (h.isNotEmpty() && iso == h) return teile.first()
        return when (f) {
            Form.Voll -> voll
            Form.OrtLand -> "${teile.first()}, $land"
            Form.OrtIso2 -> "${teile.first()}, ${iso ?: land}"
            Form.OrtIso3 -> "${teile.first()}, ${iso?.let(Laender::iso3) ?: land}"
        }
    }

    /** Kurzform fuer Kaesten und Listen: webtrees' Kurzname (erster Teil), solange nichts eingestellt ist, sonst nach Form. */
    fun kurz(name: String?, short: String?): String =
        if (form.value == Form.Voll && heimat.value.isEmpty()) short.orEmpty().ifBlank { Ortsnamen.blatt(name.orEmpty()) } else text(name)

    /** Anzeigename eines Landes in der Sprache der App, fuer den Dialog. */
    fun landName(iso2: String): String = if (iso2.isBlank()) "" else Locale("", iso2).getDisplayCountry(Locale.getDefault()).ifBlank { iso2 }
}

/** Laender nach ISO 3166: Namen in den Sprachen der App und beide Kuerzel, alles in Kleinbuchstaben nachschlagbar. */
object Laender {
    private val sprachen = listOf(Locale.GERMAN, Locale.ENGLISH, Locale.FRENCH, Locale("nl"), Locale("es"))

    /** Weitere gaengige Schreibweisen, die java.util.Locale nicht kennt. */
    private val extra = mapOf(
        "usa" to "US", "u.s.a." to "US", "united states of america" to "US", "vereinigte staaten von amerika" to "US", "america" to "US",
        "holland" to "NL", "the netherlands" to "NL", "nederland" to "NL", "niederlande" to "NL", "pays-bas" to "NL",
        "deutsches reich" to "DE", "brd" to "DE", "ddr" to "DE", "germany" to "DE", "great britain" to "GB", "uk" to "GB", "u.k." to "GB", "england" to "GB",
        "schweiz" to "CH", "suisse" to "CH", "svizzera" to "CH", "oesterreich" to "AT", "czechia" to "CZ", "russia" to "RU",
    )

    private val nachName: Map<String, String> by lazy {
        val m = HashMap<String, String>()
        Locale.getISOCountries().forEach { cc ->
            val l = Locale("", cc)
            m[cc.lowercase()] = cc
            runCatching { m[l.isO3Country.lowercase()] = cc }
            sprachen.forEach { s -> l.getDisplayCountry(s).takeIf(String::isNotBlank)?.let { m[it.lowercase()] = cc } }
            l.getDisplayCountry(l).takeIf(String::isNotBlank)?.let { m.putIfAbsent(it.lowercase(), cc) }
        }
        extra.forEach { (k, v) -> m[k] = v }
        m
    }

    /** Zwei-Buchstaben-Kuerzel zu einem Landesnamen oder Kuerzel, sonst null. */
    fun iso2(land: String?): String? {
        val k = land?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return null
        return nachName[k] ?: nachName[k.replace("ß", "ss")]
    }

    fun iso3(iso2: String): String = runCatching { Locale("", iso2).isO3Country }.getOrDefault(iso2).ifBlank { iso2 }
}
