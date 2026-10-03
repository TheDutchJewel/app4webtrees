package de.bgghome.webtrees.nativ.pruefung

import de.bgghome.webtrees.nativ.api.PlaceSummary

/** Warum zwei Ortsnamen wohl derselbe Ort sind - in der Reihenfolge der Verlaesslichkeit. */
enum class OrtsGrund { GleicheGov, Schreibweise, OhneGliederung, Variante, Nahe }

/** Ein Vorschlag: [von] in [nach] zusammenfuehren (nach = der haeufigere bzw. besser gegliederte Name). */
data class OrtsPaar(val von: String, val nach: String, val vonAnzahl: Int, val nachAnzahl: Int, val grund: OrtsGrund) {
    /** Schluessel unabhaengig von der Richtung - fuer "gehoert nicht zusammen". */
    val schluessel: String get() = listOf(von, nach).sorted().joinToString("\u0001")
    fun umgedreht() = copy(von = nach, nach = von, vonAnzahl = nachAnzahl, nachAnzahl = vonAnzahl)
}

private fun teile(name: String) = name.split(',').map { it.trim().replace(Regex("\\s+"), " ") }

/** Gleiche Schreibweise bis auf Gross/klein, Leerzeichen und leere Teile ("Celle,, Deutschland"). */
private fun schreibform(name: String) = teile(name).filter(String::isNotEmpty).joinToString(", ").lowercase()

/**
 * Kandidaten zum Zusammenfuehren in der Ortsliste: gleiche GOV-Kennung, gleiche Schreibweise bis auf Kleinigkeiten,
 * derselbe Ort ohne (oder mit kuerzerer) Gliederung, Schreibvarianten des Ortsnamens bei gleicher Gliederung und
 * gleichnamige Orte, die weniger als [nahKm] auseinanderliegen. Je Paar nur der verlaesslichste Grund; die Richtung
 * zeigt zum haeufigeren Namen, bei Gleichstand zum laengeren (besser gegliederten).
 */
fun ortsDubletten(orte: List<PlaceSummary>, nahKm: Double = 2.0): List<OrtsPaar> {
    val gefunden = LinkedHashMap<String, OrtsPaar>()
    fun paar(a: PlaceSummary, b: PlaceSummary, grund: OrtsGrund) {
        if (a.name == b.name) return
        val (von, nach) = if (a.events != b.events) (if (a.events < b.events) a to b else b to a)
            else if (teile(a.name).size != teile(b.name).size) (if (teile(a.name).size < teile(b.name).size) a to b else b to a)
            else if (a.name <= b.name) a to b else b to a
        val p = OrtsPaar(von.name, nach.name, von.events, nach.events, grund)
        val alt = gefunden[p.schluessel]
        if (alt == null || grund.ordinal < alt.grund.ordinal) gefunden[p.schluessel] = p
    }

    // Gleiche GOV-Kennung: alle zum haeufigsten Namen
    orte.filter { !it.gov.isNullOrBlank() }.groupBy { it.gov!!.trim().lowercase() }.values.filter { it.size > 1 }.forEach { g ->
        val ziel = g.maxWith(compareBy<PlaceSummary> { it.events }.thenBy { teile(it.name).size })
        g.filter { it !== ziel }.forEach { paar(it, ziel, OrtsGrund.GleicheGov) }
    }
    // Schreibweise
    orte.groupBy { schreibform(it.name) }.values.filter { it.size > 1 }.forEach { g ->
        val ziel = g.maxBy { it.events }
        g.filter { it !== ziel }.forEach { paar(it, ziel, OrtsGrund.Schreibweise) }
    }
    // Ohne Gliederung: "Hermannsburg" bzw. "Hermannsburg, Celle" neben genau einem laengeren "Hermannsburg, Celle, ..."
    val nachErstem = orte.groupBy { namensform(teile(it.name).first()) }
    for (g in nachErstem.values) {
        if (g.size < 2) continue
        for (a in g) {
            val ta = teile(a.name).map(::namensform)
            val laenger = g.filter { b -> teile(b.name).let { tb -> tb.size > ta.size && tb.take(ta.size).map(::namensform) == ta } }
            if (laenger.size == 1) paar(a, laenger.single(), OrtsGrund.OhneGliederung)
        }
    }
    // Schreibvarianten des Ortsnamens bei gleicher Gliederung (Bienenbuettel = Bienenbüttel, Hermannsbrug = Hermannsburg)
    orte.groupBy { teile(it.name).drop(1).joinToString(",") { t -> namensform(t) } }.values.forEach { g ->
        for (i in g.indices) for (j in i + 1 until g.size) {
            val a = namensform(teile(g[i].name).first()); val b = namensform(teile(g[j].name).first())
            if (a.isEmpty() || b.isEmpty()) continue
            if (a == b || (minOf(a.length, b.length) >= 5 && levenshtein(a, b) <= 1)) paar(g[i], g[j], OrtsGrund.Variante)
        }
    }
    // Gleicher Ortsname, nah beieinander, aber verschieden gegliedert
    for (g in nachErstem.values) {
        val mitKoord = g.filter { it.lat != null && it.lng != null }
        for (i in mitKoord.indices) for (j in i + 1 until mitKoord.size) {
            val a = mitKoord[i]; val b = mitKoord[j]
            if (entfernungKm(a.lat!!, a.lng!!, b.lat!!, b.lng!!) < nahKm) paar(a, b, OrtsGrund.Nahe)
        }
    }
    return gefunden.values.sortedWith(compareBy<OrtsPaar> { it.grund.ordinal }.thenBy { it.nach.lowercase() })
}
