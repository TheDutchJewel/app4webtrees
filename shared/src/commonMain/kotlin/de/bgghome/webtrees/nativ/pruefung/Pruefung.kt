package de.bgghome.webtrees.nativ.pruefung

import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.TreeExport

/*
 * Plausibilitaetspruefung (27.09.2026) ueber den ganzen Baum aus dem Export von api4webtrees (ab Stufe 17).
 * Die Regeln und ihre Kennnummern stammen aus dem Pruefprogramm von db-blank (pruefungen/pruefregeln.txt,
 * plausibilitaet.py): 0xx Chronologie, 1xx Altersgrenzen, 2xx Struktur, dazu 319. Die OFB-eigenen Regeln
 * (Namensfelder, Atlanten, Rohtext) fehlen; 218 (Verweis ins Leere) auch - der Export laesst Personen weg, die der
 * Benutzer nicht sehen darf, und webtrees prueft Verweise selbst (Menue webtrees > Stammbaum pruefen).
 * Die Pruefung laeuft ohne Server-Anfrage; was der Benutzer nicht sehen darf, prueft sie nicht.
 */

enum class Schwere { Fehler, Warnung }

/** Einheit des Grenzwerts: Jahre, Monate oder Anzahl. */
enum class Einheit { Jahre, Monate, Anzahl }

enum class RegelGruppe { Chronologie, Alter, Struktur }

class Regel(
    val id: String,
    val gruppe: RegelGruppe,
    val schwere: Schwere,
    /** Voreingestellter Grenzwert; null: Regel ohne Grenzwert. */
    val grenzwert: Double?,
    val einheit: Einheit?,
    private val de: String,
    private val en: String,
) {
    fun frage(deutsch: Boolean) = if (deutsch) de else en
}

/** Ein Treffer: Person und/oder Familie, dazu eine kurze Begruendung mit den Daten. */
data class Treffer(val regel: String, val person: String?, val familie: String?, val text: String)

object Regelkatalog {
    private val C = RegelGruppe.Chronologie
    private val A = RegelGruppe.Alter
    private val S = RegelGruppe.Struktur
    private val F = Schwere.Fehler
    private val W = Schwere.Warnung
    private val J = Einheit.Jahre
    private val M = Einheit.Monate
    private val N = Einheit.Anzahl

    val alle: List<Regel> = listOf(
        Regel("010", C, F, null, null, "Stirbt jemand vor seiner Geburt?", "Does someone die before being born?"),
        Regel("011", C, F, null, null, "Heiratet jemand vor seiner Geburt?", "Does someone marry before being born?"),
        Regel("012", C, F, null, null, "Heiratet jemand nach seinem Tod?", "Does someone marry after their death?"),
        Regel("013", C, F, null, null, "Wird ein Kind vor seiner Mutter geboren?", "Is a child born before its mother?"),
        Regel("014", C, F, null, null, "Wird ein Kind vor seinem Vater geboren?", "Is a child born before its father?"),
        Regel("015", C, F, 0.0, M, "Wird ein Kind nach dem Tod der Mutter geboren?", "Is a child born after its mother's death?"),
        Regel("016", C, F, 9.5, M, "Wird ein Kind später als die Grenze nach dem Tod des Vaters geboren?", "Is a child born later than the limit after its father's death?"),
        Regel("017", C, W, 1.0, J, "Wird ein Kind mehr als die Grenze vor der Ehe der Eltern geboren?", "Is a child born more than the limit before its parents' marriage?"),
        Regel("018", C, F, null, null, "Gibt es Daten, die es nicht gibt (31. April, 30. Februar …)?", "Are there dates that do not exist (31 April, 30 February …)?"),
        Regel("019", C, F, null, null, "Liegt ein Datum in der Zukunft?", "Is a date in the future?"),
        Regel("020", C, F, null, null, "Wird jemand vor seiner Geburt getauft?", "Is someone baptised before being born?"),
        Regel("021", C, F, null, null, "Wird jemand vor seinem Tod begraben?", "Is someone buried before their death?"),
        Regel("022", C, W, null, null, "Ist das Geburtsdatum gleich dem Heiratsdatum?", "Is the date of birth the same as the date of marriage?"),
        Regel("023", C, W, null, null, "Stehen die Kinder einer Familie nicht in Geburtsreihenfolge?", "Are the children of a family out of birth order?"),

        Regel("110", A, F, 110.0, J, "Wird jemand älter als die Grenze?", "Does someone live longer than the limit?"),
        Regel("111", A, F, 14.0, J, "Ist eine Mutter bei der Geburt jünger als die Grenze?", "Is a mother younger than the limit at the birth?"),
        Regel("112", A, F, 55.0, J, "Ist eine Mutter bei der Geburt älter als die Grenze?", "Is a mother older than the limit at the birth?"),
        Regel("113", A, F, 14.0, J, "Ist ein Vater bei der Geburt jünger als die Grenze?", "Is a father younger than the limit at the birth?"),
        Regel("114", A, W, 75.0, J, "Ist ein Vater bei der Geburt älter als die Grenze?", "Is a father older than the limit at the birth?"),
        Regel("115", A, F, 15.0, J, "Heiratet jemand jünger als die Grenze?", "Does someone marry younger than the limit?"),
        Regel("116", A, W, 90.0, J, "Heiratet jemand älter als die Grenze?", "Does someone marry older than the limit?"),
        Regel("117", A, W, 40.0, J, "Liegen zwischen Ehepartnern mehr Jahre als die Grenze?", "Are spouses further apart in age than the limit?"),
        Regel("118", A, W, 8.0, M, "Liegen zwei Geschwister näher beieinander als die Grenze (keine Zwillinge)?", "Are two siblings closer together than the limit (not twins)?"),
        Regel("119", A, W, 30.0, J, "Liegen zwischen ältestem und jüngstem Kind mehr Jahre als die Grenze?", "Are the oldest and youngest child further apart than the limit?"),
        Regel("120", A, W, 16.0, N, "Hat eine Ehe mehr Kinder als die Grenze?", "Does a marriage have more children than the limit?"),
        Regel("121", A, W, 3.0, N, "Hat jemand mehr Ehen als die Grenze?", "Does someone have more marriages than the limit?"),
        Regel("122", A, W, 2.0, M, "Heiratet jemand früher als die Grenze nach dem Tod des vorigen Partners wieder?", "Does someone remarry sooner than the limit after the previous spouse's death?"),
        Regel("123", A, F, 20.0, N, "Hat eine Mutter über alle Ehen mehr Kinder als die Grenze?", "Does a mother have more children than the limit over all marriages?"),
        Regel("124", A, W, 12.0, J, "Liegen zwischen zwei aufeinanderfolgenden Geschwistern mehr Jahre als die Grenze?", "Are two consecutive siblings further apart than the limit?"),
        Regel("125", A, F, null, null, "Heiratet jemand nach seinem eigenen Sterbedatum?", "Does someone marry after their own date of death?"),

        Regel("210", S, F, null, null, "Gibt es Personen ohne Namen, ohne Daten und ohne jede Verbindung?", "Are there people without name, dates or any link?"),
        Regel("211", S, W, null, null, "Gibt es Personen, die in keiner Familie vorkommen?", "Are there people who belong to no family?"),
        Regel("212", S, W, null, null, "Gibt es Partner ohne jede eigene Angabe in einer kinderlosen Ehe?", "Are there spouses without any details in a childless marriage?"),
        Regel("213", S, W, null, null, "Ist dieselbe Person Kind in zwei Familien?", "Is the same person a child in two families?"),
        Regel("214", S, F, null, null, "Ist jemand Kind in der Familie, in der er selbst Elternteil ist?", "Is someone a child in the family where they are a parent?"),
        Regel("215", S, F, null, null, "Haben beide Ehepartner dasselbe Geschlecht?", "Do both spouses have the same sex?"),
        Regel("216", S, F, null, null, "Steht eine Frau als Vater oder ein Mann als Mutter?", "Is a woman recorded as father or a man as mother?"),
        Regel("217", S, F, null, null, "Gibt es Familien ohne Partner und ohne Kinder?", "Are there families without spouses and children?"),
        Regel("219", S, W, null, null, "Gibt es zwei Personen mit gleichem Namen und gleichem vollen Geburtsdatum?", "Are there two people with the same name and the same full date of birth?"),
        Regel("220", S, F, null, null, "Sind Ehepartner Geschwister?", "Are spouses siblings?"),
        Regel("221", S, F, null, null, "Sind Ehepartner Elternteil und Kind?", "Are spouses parent and child?"),
        Regel("222", S, F, null, null, "Heiratet jemand, während der vorige Partner noch lebt?", "Does someone marry while the previous spouse is still alive?"),
        Regel("223", S, W, null, null, "Haben beide Ehepartner denselben Nachnamen?", "Do both spouses have the same surname?"),
        Regel("224", S, W, null, null, "Haben zwei lebende Geschwister denselben Vornamen?", "Do two living siblings have the same given name?"),
        Regel("225", S, F, null, null, "Ist jemand sein eigener Vorfahr?", "Is someone their own ancestor?"),
        Regel("226", S, W, null, null, "Gibt es dasselbe Elternpaar als zwei Familien?", "Is the same couple recorded as two families?"),
        Regel("319", S, W, null, null, "Hat ein Ehepartner unbekanntes Geschlecht?", "Does a spouse have an unknown sex?"),
    )

    val nachId: Map<String, Regel> = alle.associateBy { it.id }
}

// ── Modell ──

internal class PPerson(
    val xref: String,
    val name: String,
    val vn: String,
    val fn: String,
    /** m, w oder u */
    val geschlecht: Char,
    val privat: Boolean,
    val leer: Boolean,
    val geb: PruefDatum?,
    val taufe: PruefDatum?,
    val tod: PruefDatum?,
    val begr: PruefDatum?,
    val eltern: List<String>,
) {
    /** Partnerfamilien, nach Heirat geordnet (ohne Datum zuletzt). */
    var ehen: List<String> = emptyList()
}

internal class PFamilie(val xref: String, val vater: String?, val mutter: String?, val heirat: PruefDatum?, val kinder: List<String>)

internal class PModell(val p: Map<String, PPerson>, val f: Map<String, PFamilie>) {
    fun partner(f: PFamilie) = listOfNotNull(p[f.vater], p[f.mutter])
    fun kinder(f: PFamilie) = f.kinder.mapNotNull { p[it] }
    fun vater(f: PFamilie) = p[f.vater]
    fun mutter(f: PFamilie) = p[f.mutter]
}

private fun datumAus(f: FactJson?): PruefDatum? {
    val d = f?.date ?: return null
    if (d.gedcom.isNotBlank()) return PruefDatum.aus(d.gedcom, d.text)
    // aeltere Server ohne GEDCOM-Form: nur das Jahr
    return if (d.year > 0) PruefDatum.aus(d.year.toString(), d.text) else null
}

private fun erstes(facts: List<FactJson>, vararg tags: String): PruefDatum? {
    for (t in tags) facts.filter { it.tag == t }.firstNotNullOfOrNull(::datumAus)?.let { return it }
    return null
}

internal fun modellAus(b: TreeExport): PModell {
    val personen = b.individuals.mapValues { (x, i) ->
        val p = i.person
        PPerson(
            xref = x, name = p.name, vn = p.given.trim(), fn = p.surname.trim(),
            geschlecht = when (p.sex) { "M" -> 'm'; "F" -> 'w'; else -> 'u' },
            privat = p.isPrivate,
            leer = !p.isPrivate && p.given.isBlank() && p.surname.isBlank() && i.facts.isEmpty(),
            geb = erstes(i.facts, "BIRT"), taufe = erstes(i.facts, "CHR", "BAPM"),
            tod = erstes(i.facts, "DEAT"), begr = erstes(i.facts, "BURI", "CREM"),
            eltern = i.famc.filter { it in b.families },
        )
    }
    val familien = b.families.mapValues { (x, f) ->
        PFamilie(x, f.husband?.takeIf { it in personen }, f.wife?.takeIf { it in personen },
            erstes(f.facts, "MARR") ?: f.marriage?.date?.let { d -> PruefDatum.aus(d.gedcom.ifBlank { d.year.takeIf { it > 0 }?.toString().orEmpty() }, d.text) },
            f.children.filter { it in personen })
    }
    // Partnerfamilien aus Sicht der Familien (so sind sie vollstaendig), chronologisch
    val ehen = HashMap<String, MutableList<String>>()
    familien.values.forEach { f -> listOfNotNull(f.vater, f.mutter).distinct().forEach { ehen.getOrPut(it) { mutableListOf() } += f.xref } }
    personen.values.forEach { p ->
        p.ehen = ehen[p.xref].orEmpty().sortedWith(compareBy({ familien.getValue(it).heirat == null }, { familien.getValue(it).heirat?.min ?: 0L }))
    }
    return PModell(personen, familien)
}

// ── Pruefung ──

class PruefErgebnis(val treffer: Map<String, List<Treffer>>) {
    val anzahl: Int get() = treffer.values.sumOf { it.size }
}

/**
 * Prueft den Baum. [grenzwerte]: abweichende Grenzwerte je Regel; [aus]: abgeschaltete Regeln;
 * [jetzt]: das laufende Jahr (Regel 019); [deutsch]: Sprache der Begruendungen.
 */
fun pruefen(
    baum: TreeExport,
    grenzwerte: Map<String, Double> = emptyMap(),
    aus: Set<String> = emptySet(),
    jetzt: Int = java.time.Year.now().value,
    deutsch: Boolean = java.util.Locale.getDefault().language == "de",
): PruefErgebnis {
    val m = modellAus(baum)
    val r = Regeln(m, jetzt, deutsch)
    val ergebnis = LinkedHashMap<String, List<Treffer>>()
    for (regel in Regelkatalog.alle) {
        if (regel.id in aus) continue
        val g = grenzwerte[regel.id] ?: regel.grenzwert
        ergebnis[regel.id] = r.laufen(regel.id, g).map { (p, f, t) -> Treffer(regel.id, p, f, t) }
    }
    return PruefErgebnis(ergebnis)
}

private typealias T = Triple<String?, String?, String>

private class Regeln(val m: PModell, val jetzt: Int, val deutsch: Boolean) {
    fun w(de: String, en: String) = if (deutsch) de else en
    fun d(x: PruefDatum?) = x?.text ?: "?"
    fun n(p: PPerson) = p.name.ifBlank { "?" }
    fun jahreText(a: Double) = w("%.0f J", "%.0f y").format(a)
    fun monateText(a: Double) = w("%.1f M", "%.1f mo").format(a)

    /** Personen mit Daten; private Platzhalter haben keine und bleiben aussen vor. */
    val personen = m.p.values.filter { !it.privat }
    val familien = m.f.values

    fun elterKind(vater: Boolean) = sequence {
        for (f in familien) {
            val e = (if (vater) m.vater(f) else m.mutter(f)) ?: continue
            for (k in m.kinder(f)) yield(Triple(k, e, f))
        }
    }

    fun laufen(id: String, g: Double?): List<T> = when (id) {
        // ── 0xx Chronologie ──
        "010" -> personen.filter { vor(it.tod, it.geb) == true }.map { T(it.xref, null, "* ${d(it.geb)}  † ${d(it.tod)}") }
        "011" -> familien.flatMap { f -> m.partner(f).filter { vor(f.heirat, it.geb) == true }.map { T(it.xref, f.xref, "⚭ ${d(f.heirat)}  * ${d(it.geb)}") } }
        "012" -> familien.flatMap { f -> m.partner(f).filter { vor(it.tod, f.heirat) == true }.map { T(it.xref, f.xref, "⚭ ${d(f.heirat)}  † ${d(it.tod)}") } }
        "013" -> elterKind(false).filter { (k, e) -> vor(k.geb, e.geb) == true }.map { (k, e, f) -> T(k.xref, f.xref, w("Kind", "Child") + " * ${d(k.geb)}  " + w("Mutter", "Mother") + " * ${d(e.geb)}") }.toList()
        "014" -> elterKind(true).filter { (k, e) -> vor(k.geb, e.geb) == true }.map { (k, e, f) -> T(k.xref, f.xref, w("Kind", "Child") + " * ${d(k.geb)}  " + w("Vater", "Father") + " * ${d(e.geb)}") }.toList()
        "015" -> kindNachTod(false, g ?: 0.0)
        "016" -> kindNachTod(true, g ?: 9.5)
        "017" -> familien.filter { it.vater != null && it.mutter != null && it.heirat != null }.flatMap { f ->
            m.kinder(f).mapNotNull { k -> jahre(k.geb, f.heirat)?.takeIf { it > (g ?: 1.0) }?.let { T(k.xref, f.xref, "* ${d(k.geb)}  ⚭ ${w("Eltern", "parents")} ${d(f.heirat)}") } }
        }
        "018" -> personen.flatMap { p ->
            listOf("*" to p.geb, "~" to p.taufe, "†" to p.tod, "⚰" to p.begr).filter { it.second?.unmoeglich == true }.map { (z, x) -> T(p.xref, null, "$z ${x!!.text}") }
        } + familien.filter { it.heirat?.unmoeglich == true }.map { T(null, it.xref, "⚭ ${it.heirat!!.text}") }
        "019" -> personen.filter { p -> listOf(p.geb, p.taufe, p.tod, p.begr).any { (it?.jahr ?: 0) > jetzt } }.map { T(it.xref, null, "* ${d(it.geb)}  † ${d(it.tod)}") } +
            familien.filter { (it.heirat?.jahr ?: 0) > jetzt }.map { T(null, it.xref, "⚭ ${d(it.heirat)}") }
        "020" -> personen.filter { vor(it.taufe, it.geb) == true }.map { T(it.xref, null, "* ${d(it.geb)}  ~ ${d(it.taufe)}") }
        "021" -> personen.filter { vor(it.begr, it.tod) == true }.map { T(it.xref, null, "† ${d(it.tod)}  ⚰ ${d(it.begr)}") }
        "022" -> familien.flatMap { f -> m.partner(f).filter { it.geb?.gleich(f.heirat) == true }.map { T(it.xref, f.xref, "* = ⚭ ${d(it.geb)}") } }
        "023" -> familien.mapNotNull { f ->
            val ks = m.kinder(f).filter { it.geb?.voll == true }
            ks.zipWithNext().firstOrNull { (a, b) -> a.geb!!.tage > b.geb!!.tage }?.let { (a, b) ->
                T(b.xref, f.xref, "* ${d(b.geb)}, " + w("steht nach", "listed after") + " ${n(a)} * ${d(a.geb)}")
            }
        }

        // ── 1xx Altersgrenzen ──
        "110" -> personen.mapNotNull { p -> jahre(p.geb, p.tod)?.takeIf { it > (g ?: 110.0) }?.let { T(p.xref, null, "* ${d(p.geb)}  † ${d(p.tod)} = ${jahreText(it)}") } }
        "111" -> elterAlter(false, unter = g ?: 14.0)
        "112" -> elterAlter(false, ueber = g ?: 55.0)
        "113" -> elterAlter(true, unter = g ?: 14.0)
        "114" -> elterAlter(true, ueber = g ?: 75.0)
        "115" -> heiratAlter(unter = g ?: 15.0)
        "116" -> heiratAlter(ueber = g ?: 90.0)
        "117" -> familien.mapNotNull { f ->
            val v = m.vater(f) ?: return@mapNotNull null; val mu = m.mutter(f) ?: return@mapNotNull null
            jahre(v.geb, mu.geb)?.takeIf { kotlin.math.abs(it) > (g ?: 40.0) }?.let { T(v.xref, f.xref, "${n(v)} * ${d(v.geb)}  ${n(mu)} * ${d(mu.geb)}") }
        }
        "118" -> familien.flatMap { f ->
            val ks = m.kinder(f).filter { it.geb?.voll == true }
            ks.flatMapIndexed { i, a ->
                ks.drop(i + 1).mapNotNull { b ->
                    val x = kotlin.math.abs(monate(a.geb, b.geb)!!)
                    if (x >= 0.1 && x <= (g ?: 8.0)) T(a.xref, f.xref, "* ${d(a.geb)}, ${n(b)} * ${d(b.geb)}") else null
                }
            }
        }
        "119" -> familien.mapNotNull { f ->
            val js = m.kinder(f).mapNotNull { it.geb?.jahr }
            if (js.isNotEmpty() && js.max() - js.min() > (g ?: 30.0)) T(null, f.xref, w("Spanne", "Span") + " ${js.max() - js.min()} " + w("J", "y")) else null
        }
        "120" -> familien.filter { it.kinder.size > (g ?: 16.0) }.map { T(null, it.xref, "${it.kinder.size} " + w("Kinder", "children")) }
        "121" -> personen.filter { it.ehen.size > (g ?: 3.0) }.map { T(it.xref, null, "${it.ehen.size} " + w("Ehen", "marriages")) }
        "122" -> personen.flatMap { p ->
            p.ehen.zipWithNext().mapNotNull { (f1, f2) ->
                val alt = m.partner(m.f.getValue(f1)).firstOrNull { it.xref != p.xref } ?: return@mapNotNull null
                val neu = m.f.getValue(f2)
                if (alt.tod?.voll != true || neu.heirat?.voll != true) return@mapNotNull null
                val x = monate(alt.tod, neu.heirat)!!
                if (x >= 0 && x <= (g ?: 2.0)) T(p.xref, f2, "† ${n(alt)} ${d(alt.tod)}  ⚭ ${d(neu.heirat)}") else null
            }
        }
        "123" -> personen.filter { it.geschlecht != 'm' }.mapNotNull { p ->
            val kids = p.ehen.map { m.f.getValue(it) }.filter { it.mutter == p.xref }.flatMap { it.kinder }.toSet()
            if (kids.size > (g ?: 20.0)) T(p.xref, null, "${kids.size} " + w("Kinder in", "children in") + " ${p.ehen.size} " + w("Ehe(n)", "marriage(s)")) else null
        }
        "124" -> familien.flatMap { f ->
            m.kinder(f).filter { it.geb?.jahr != null }.sortedBy { it.geb!!.jahr }.zipWithNext().mapNotNull { (a, b) ->
                val x = b.geb!!.jahr!! - a.geb!!.jahr!!
                if (x > (g ?: 12.0)) T(b.xref, f.xref, "* ${d(b.geb)}, $x " + w("J nach", "y after") + " ${n(a)} * ${d(a.geb)}") else null
            }
        }
        "125" -> personen.filter { it.tod?.voll == true }.flatMap { p ->
            p.ehen.map { m.f.getValue(it) }.filter { it.heirat?.voll == true && monate(p.tod, it.heirat)!! > 0 }.map { T(p.xref, it.xref, "† ${d(p.tod)}  ⚭ ${d(it.heirat)}") }
        }

        // ── 2xx Struktur ──
        "210" -> personen.filter { it.leer && it.ehen.isEmpty() && it.eltern.isEmpty() }.map { T(it.xref, null, it.xref) }
        "211" -> personen.filter { it.ehen.isEmpty() && it.eltern.isEmpty() }.map { T(it.xref, null, n(it)) }
        "212" -> familien.filter { it.kinder.isEmpty() }.flatMap { f -> m.partner(f).filter { it.leer }.map { T(it.xref, f.xref, w("ohne Angaben", "no details")) } }
        "213" -> personen.filter { it.eltern.size > 1 }.map { T(it.xref, it.eltern.first(), w("Familien ", "Families ") + it.eltern.joinToString(", ")) }
        "214" -> personen.flatMap { p -> p.eltern.filter { it in p.ehen }.map { T(p.xref, it, w("Kind und Elternteil zugleich", "child and parent at once")) } }
        "215" -> familien.mapNotNull { f ->
            val v = m.vater(f); val mu = m.mutter(f)
            if (v != null && mu != null && v.geschlecht != 'u' && v.geschlecht == mu.geschlecht) T(v.xref, f.xref, "${n(v)}, ${n(mu)}") else null
        }
        "216" -> familien.flatMap { f ->
            listOfNotNull(
                m.vater(f)?.takeIf { it.geschlecht == 'w' }?.let { T(it.xref, f.xref, w("Frau als Vater", "woman as father")) },
                m.mutter(f)?.takeIf { it.geschlecht == 'm' }?.let { T(it.xref, f.xref, w("Mann als Mutter", "man as mother")) },
            )
        }
        "217" -> familien.filter { it.vater == null && it.mutter == null && it.kinder.isEmpty() }.map { T(null, it.xref, w("leere Familie", "empty family")) }
        "219" -> personen.filter { it.geb?.voll == true && it.fn.isNotEmpty() && it.vn.isNotEmpty() }
            .groupBy { Triple(it.vn, it.fn, it.geb!!.tage) }.values.flatMap { ps ->
                ps.flatMapIndexed { i, a -> ps.drop(i + 1).map { b -> T(a.xref, null, "${n(a)} * ${d(a.geb)} = ${b.xref}") } }
            }
        "220" -> familien.mapNotNull { f ->
            val v = m.vater(f) ?: return@mapNotNull null; val mu = m.mutter(f) ?: return@mapNotNull null
            if (mu.xref in geschwister(v)) T(v.xref, f.xref, "${n(v)}, ${n(mu)}") else null
        }
        "221" -> familien.flatMap { f ->
            val v = m.vater(f); val mu = m.mutter(f)
            if (v == null || mu == null) return@flatMap emptyList()
            buildList {
                if (mu.eltern.any { v.xref in listOf(m.f.getValue(it).vater, m.f.getValue(it).mutter) }) add(T(v.xref, f.xref, "${n(v)} → ${n(mu)}"))
                if (v.eltern.any { mu.xref in listOf(m.f.getValue(it).vater, m.f.getValue(it).mutter) }) add(T(mu.xref, f.xref, "${n(mu)} → ${n(v)}"))
            }
        }
        "222" -> personen.flatMap { p ->
            p.ehen.zipWithNext().mapNotNull { (f1, f2) ->
                val alt = m.partner(m.f.getValue(f1)).firstOrNull { it.xref != p.xref } ?: return@mapNotNull null
                val neu = m.f.getValue(f2)
                if (vor(neu.heirat, alt.tod) == true) T(p.xref, f2, "⚭ ${d(neu.heirat)}  † ${n(alt)} ${d(alt.tod)}") else null
            }
        }
        "223" -> familien.mapNotNull { f ->
            val v = m.vater(f); val mu = m.mutter(f)
            if (v != null && mu != null && !v.privat && !mu.privat && v.fn.isNotEmpty() && v.fn == mu.fn) T(v.xref, f.xref, v.fn) else null
        }
        "224" -> familien.flatMap { f ->
            val ks = m.kinder(f).filter { !it.privat }
            ks.flatMapIndexed { i, a ->
                ks.drop(i + 1).mapNotNull { b ->
                    if (a.vn.isEmpty() || a.vn != b.vn || a.geb == null || b.geb == null) return@mapNotNull null
                    val (frueh, spaet) = if (vor(b.geb, a.geb) == true) b to a else a to b
                    if (frueh.tod == null || vor(spaet.geb, frueh.tod) == true)
                        T(spaet.xref, f.xref, "${a.vn}: * ${d(frueh.geb)}" + (frueh.tod?.let { " († ${it.text})" } ?: "") + ", * ${d(spaet.geb)}")
                    else null
                }
            }
        }
        "225" -> eigeneVorfahren().map { T(it, null, w("eigener Vorfahr", "own ancestor")) }
        "226" -> {
            val paare = HashMap<Pair<String, String>, PFamilie>()
            familien.mapNotNull { f ->
                if (f.vater == null || f.mutter == null) return@mapNotNull null
                val a = paare.putIfAbsent(f.vater to f.mutter, f) ?: return@mapNotNull null
                if (a.heirat == null || f.heirat == null || a.heirat.gleich(f.heirat) || a.heirat.text == f.heirat.text)
                    T(null, f.xref, w("wie Familie", "same as family") + " ${a.xref}") else null
            }
        }
        "319" -> familien.flatMap { f -> m.partner(f).filter { it.geschlecht == 'u' && !it.privat }.map { T(it.xref, f.xref, n(it)) } }
        else -> emptyList()
    }

    fun kindNachTod(vater: Boolean, grenzeMonate: Double): List<T> = elterKind(vater).mapNotNull { (k, e, f) ->
        if (k.geb == null || e.tod == null) return@mapNotNull null
        val treffer = if (k.geb.voll && e.tod.voll) monate(e.tod, k.geb)!! > grenzeMonate else (jahre(e.tod, k.geb) ?: return@mapNotNull null) > 1
        if (treffer) T(k.xref, f.xref, "* ${d(k.geb)}  † " + (if (vater) w("Vater", "father") else w("Mutter", "mother")) + " ${d(e.tod)}") else null
    }.toList()

    fun elterAlter(vater: Boolean, unter: Double? = null, ueber: Double? = null): List<T> = elterKind(vater).mapNotNull { (k, e, f) ->
        val a = jahre(e.geb, k.geb) ?: return@mapNotNull null
        if ((unter != null && a >= 0 && a < unter) || (ueber != null && a > ueber))
            T(k.xref, f.xref, (if (vater) w("Vater", "Father") else w("Mutter", "Mother")) + " * ${d(e.geb)}, " + w("Kind", "child") + " * ${d(k.geb)} = ${jahreText(a)}")
        else null
    }.toList()

    fun heiratAlter(unter: Double? = null, ueber: Double? = null): List<T> = familien.flatMap { f ->
        m.partner(f).mapNotNull { p ->
            val a = jahre(p.geb, f.heirat) ?: return@mapNotNull null
            if ((unter != null && a >= 0 && a < unter) || (ueber != null && a > ueber)) T(p.xref, f.xref, "* ${d(p.geb)}  ⚭ ${d(f.heirat)} = ${jahreText(a)}") else null
        }
    }

    fun geschwister(p: PPerson): Set<String> = p.eltern.flatMap { m.f.getValue(it).kinder }.toSet() - p.xref

    /** Alle, die in einem Kreis der Abstammung liegen (starke Zusammenhangskomponenten, iterativ nach Tarjan). */
    fun eigeneVorfahren(): List<String> {
        val eltern = m.p.mapValues { (_, p) -> p.eltern.flatMap { listOfNotNull(m.f.getValue(it).vater, m.f.getValue(it).mutter) } }
        val index = HashMap<String, Int>(); val low = HashMap<String, Int>()
        val aufStapel = HashSet<String>(); val stapel = ArrayDeque<String>()
        val kreis = ArrayList<String>()
        var zaehler = 0
        for (start in m.p.keys) {
            if (start in index) continue
            val arbeit = ArrayDeque<Pair<String, Int>>()
            arbeit.addLast(start to 0)
            while (arbeit.isNotEmpty()) {
                val (v, i) = arbeit.removeLast()
                if (i == 0) { index[v] = zaehler; low[v] = zaehler; zaehler++; stapel.addLast(v); aufStapel += v }
                val nachbarn = eltern[v].orEmpty()
                if (i < nachbarn.size) {
                    arbeit.addLast(v to i + 1)
                    val w = nachbarn[i]
                    if (w !in index) arbeit.addLast(w to 0)
                    else if (w in aufStapel) low[v] = minOf(low.getValue(v), index.getValue(w))
                    continue
                }
                // v fertig: low an den Aufrufer weitergeben, Komponente abschliessen
                arbeit.lastOrNull()?.let { (u, _) -> low[u] = minOf(low.getValue(u), low.getValue(v)) }
                if (low[v] == index[v]) {
                    val komp = ArrayList<String>()
                    do { val x = stapel.removeLast(); aufStapel -= x; komp += x } while (x != v)
                    if (komp.size > 1 || v in eltern[v].orEmpty()) kreis += komp
                }
            }
        }
        return kreis
    }
}
