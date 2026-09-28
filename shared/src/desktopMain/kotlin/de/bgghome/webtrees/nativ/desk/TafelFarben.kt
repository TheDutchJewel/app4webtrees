package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.Person
import java.awt.Color

/*
 * Farben der Kaesten (C1, 28.09.2026). Vorrang: ein per Rechtsklick gefaerbter Zweig (die Person und alle, die auf
 * der Tafel von ihr aus weitergehen - in Ahnentafeln ihre Vorfahren, in Stammtafeln ihre Nachfahren), dann die erste
 * passende Farbregel, dann das Schema (nur beim Stil "Farbig"), sonst die Farben des Stils. Schwarzweiss bleibt grau.
 */

/** Grundfarbe beim Stil "Farbig": nach Geschlecht, nach Grosseltern-Linie (Vorfahren) oder nach Zweig (Nachfahren). */
enum class FarbSchema { Geschlecht, Linie, Zweig }

/** Wonach eine Farbregel fragt. Geschlecht ("M", "F", "U") und Lebend ("1", "0") haben feste Werte statt Text. */
enum class RegelFeld { Nachname, Vorname, Geburtsort, Sterbeort, Beruf, Geschlecht, Lebend }

/** "Feld gleich/enthaelt Text -> Farbe"; [farbe] ist ein Platz in [KASTEN_FARBEN]. */
data class FarbRegel(val feld: RegelFeld = RegelFeld.Nachname, val enthaelt: Boolean = false, val text: String = "", val farbe: Int = 0) {
    val festerWert get() = feld == RegelFeld.Geschlecht || feld == RegelFeld.Lebend

    fun passt(p: Person): Boolean {
        if (p.isPrivate) return false
        if (feld == RegelFeld.Geschlecht) return p.sex == text
        if (feld == RegelFeld.Lebend) return (if (p.isDead) "0" else "1") == text
        if (text.isBlank()) return false
        val wert = when (feld) {
            RegelFeld.Nachname -> p.surname.ifBlank { p.name }
            RegelFeld.Vorname -> p.given
            RegelFeld.Geburtsort -> p.birth?.place?.name
            RegelFeld.Sterbeort -> p.death?.place?.name
            else -> p.occupation
        }?.trim() ?: return false
        return if (enthaelt) wert.contains(text.trim(), ignoreCase = true) else wert.equals(text.trim(), ignoreCase = true)
    }
}

/** Fuellung und Rahmen eines gefaerbten Kastens. */
class KastenFarbe(val fuellung: Color, val rahmen: Color)

private fun dunkler(c: Color) = Color((c.red * 0.55f).toInt(), (c.green * 0.55f).toInt(), (c.blue * 0.55f).toInt())

/** Die waehlbaren Farben; die ersten vier sind die Grosseltern-Linien wie in Faechertafel und Vorfahrenbuch. */
val KASTEN_FARBEN: List<KastenFarbe> = listOf(
    Color(0x9C, 0xBC, 0xE0), Color(0xA8, 0xD5, 0xA2), Color(0xEE, 0xA8, 0xA0), Color(0xF4, 0xDC, 0x8C),
    Color(0xC9, 0xB3, 0xE6), Color(0x9E, 0xD9, 0xD6), Color(0xF5, 0xC0, 0x8A), Color(0xD0, 0xD4, 0xD4),
).map { KastenFarbe(it, dunkler(it)) }

/**
 * Farbe je Platz der Anordnung (null: die Farbe des Stils). Die Plaetze eines Teils stehen in Layoutfolge - wer auf
 * der Tafel weitergeht, kommt nach seinem [TafelPlatz.eltern]; so erben Zweigfarben in einem Durchgang.
 */
internal fun kastenFarben(a: TafelAnordnung): Map<TafelPlatz, Int> {
    val o = a.o
    if (o.stil == TafelStil.Schwarzweiss) return emptyMap()
    val farbe = HashMap<TafelPlatz, Int>()
    fun eigene(p: Person) = o.zweige[p.xref] ?: o.regeln.firstOrNull { it.passt(p) }?.farbe
    // Kinder eines Paares ohne Elternverweis im Layout (Cousin-Tafel) erben vom Vater
    val paarElternteil = a.paare.flatMap { p -> p.kinder.filter { it.eltern == null }.map { it to p.vater } }.toMap()
    fun elternVon(pl: TafelPlatz) = pl.eltern ?: paarElternteil[pl]
    var zweigNr = 0
    a.teile.forEach { t ->
        // Zweig-Schema: die Kinder der Wurzel (in einem Teil nach unten) beginnen je einen Zweig
        val zweigVon = HashMap<TafelPlatz, Int>()
        val markiert = HashMap<TafelPlatz, Int>()
        t.layout.plaetze.forEach { pl ->
            if (t.istHalter(pl)) return@forEach
            val p = pl.knoten.person
            val eltern = elternVon(pl)
            o.zweige[p.xref]?.let { markiert[pl] = it } ?: eltern?.let { markiert[it] }?.let { markiert[pl] = it }
            if (!t.aufwaerts && eltern != null) (if (elternVon(eltern) == null) zweigNr++ % KASTEN_FARBEN.size else zweigVon[eltern])?.let { zweigVon[pl] = it }
            val schema = if (o.stil != TafelStil.Farbig) null else when (o.farbe) {
                FarbSchema.Geschlecht -> null
                FarbSchema.Linie -> pl.knoten.nummer?.takeIf { it >= 4 }?.let { n -> ((n shr (reihe(n) - 2)) - 4).toInt().coerceIn(0, 3) }
                FarbSchema.Zweig -> zweigVon[pl]
            }
            (markiert[pl] ?: o.regeln.firstOrNull { it.passt(p) }?.farbe ?: schema)?.let { farbe[pl] = it }
        }
    }
    // Geschwister neben den Vorfahren: nur eigene Markierung oder Regel
    a.geschwisterVon.values.flatten().forEach { pl -> eigene(pl.knoten.person)?.let { farbe[pl] = it } }
    return farbe
}

/** Farbregeln und gefaerbte Zweige gelten je Stammbaum fuer alle Tafeln (Desktop-Einstellungen). */
object TafelFarbSpeicher {
    private val prefs get() = DeskLayout.prefs

    fun regeln(baum: String): List<FarbRegel> = prefs.getString("tafel_regeln_$baum", null).orEmpty().lines().mapNotNull { z ->
        val t = z.split('\t')
        if (t.size < 4) null else FarbRegel(RegelFeld.entries.firstOrNull { it.name == t[0] } ?: return@mapNotNull null, t[1] == "1", t[2], t[3].toIntOrNull() ?: 0)
    }

    fun zweige(baum: String): Map<String, Int> = prefs.getString("tafel_zweige_$baum", null).orEmpty().split(';').mapNotNull { e ->
        val (x, f) = e.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
        f.toIntOrNull()?.let { x to it }
    }.toMap()

    fun sichern(baum: String, regeln: List<FarbRegel>, zweige: Map<String, Int>) {
        prefs.putString("tafel_regeln_$baum", regeln.joinToString("\n") { r -> listOf(r.feld.name, if (r.enthaelt) "1" else "0", r.text.replace('\t', ' ').replace('\n', ' '), r.farbe).joinToString("\t") })
        prefs.putString("tafel_zweige_$baum", zweige.entries.joinToString(";") { "${it.key}=${it.value}" })
    }
}
