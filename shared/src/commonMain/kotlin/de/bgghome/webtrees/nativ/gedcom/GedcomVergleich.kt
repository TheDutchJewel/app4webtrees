package de.bgghome.webtrees.nativ.gedcom

import de.bgghome.webtrees.nativ.api.ExportIndividual
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.TreeExport
import de.bgghome.webtrees.nativ.pruefung.levenshtein
import de.bgghome.webtrees.nativ.pruefung.namensform

/*
 * Zwei GEDCOM-Dateien vergleichen (gedaudit 0.3, 13.10.2026): was hat sich zwischen zwei Staenden getan, was ging beim
 * Umzug in ein anderes Programm verloren, welche Personen hat der Mitforscher mehr. Nur lesend, auf der Form des
 * Server-Exports, damit es fuer Dateien und (spaeter) Server gleich funktioniert.
 *
 * Zuordnung in drei Stufen: (1) gleiche Kennung und vertraeglicher Name, (2) gleicher Schluessel aus Vorname, Nachname,
 * Geburtsjahr und Geschlecht, wenn er auf beiden Seiten eindeutig ist, (3) aehnlicher Name und Geburtsjahr bis zwei Jahre
 * daneben = "wahrscheinlich dieselbe". Was uebrig bleibt, ist nur in A oder nur in B.
 */

enum class PaarArt { Gleich, Geaendert, Wahrscheinlich }

/** Ein Feld, das sich unterscheidet: Schluessel (NAME, SEX, BIRT, CHR, DEAT, BURI, ELTERN, PARTNER, KINDER, FAKTEN, ein Tag). */
class Aenderung(val feld: String, val a: String, val b: String)

class Paar(val a: String, val b: String, val art: PaarArt, val aenderungen: List<Aenderung>)

class Vergleich(
    val paare: List<Paar>,
    val nurA: List<String>,
    val nurB: List<String>,
) {
    val gleich get() = paare.filter { it.art == PaarArt.Gleich }
    val geaendert get() = paare.filter { it.art == PaarArt.Geaendert }
    val wahrscheinlich get() = paare.filter { it.art == PaarArt.Wahrscheinlich }
}

object GedcomVergleich {

    private val EREIGNISSE = listOf("BIRT", "CHR", "BAPM", "DEAT", "BURI", "CREM")

    fun vergleichen(a: TreeExport, b: TreeExport): Vergleich {
        val zuordnung = LinkedHashMap<String, Pair<String, PaarArt?>>()   // a -> b, art null = noch zu pruefen
        val belegtB = HashSet<String>()
        fun schluessel(i: ExportIndividual): String? {
            val vn = namensform(i.person.given.split(' ').firstOrNull().orEmpty()); val fn = namensform(i.person.surname)
            if (vn.isEmpty() && fn.isEmpty()) return null
            return "$vn|$fn|${geburtsjahr(i) ?: "?"}|${i.person.sex}"
        }
        fun vertraeglich(x: ExportIndividual, y: ExportIndividual): Boolean {
            val fx = namensform(x.person.surname); val fy = namensform(y.person.surname)
            val vx = namensform(x.person.given.split(' ').firstOrNull().orEmpty()); val vy = namensform(y.person.given.split(' ').firstOrNull().orEmpty())
            if (fx.isEmpty() && fy.isEmpty() && vx.isEmpty() && vy.isEmpty()) return true
            return (fx == fy || levenshtein(fx, fy) <= 1) && (vx == vy || vx.isEmpty() || vy.isEmpty() || levenshtein(vx, vy) <= 1)
        }
        // 1. gleiche Kennung
        a.individuals.forEach { (x, i) -> b.individuals[x]?.let { j -> if (vertraeglich(i, j)) { zuordnung[x] = x to null; belegtB += x } } }
        // 2. eindeutiger Schluessel
        val keyA = a.individuals.filterKeys { it !in zuordnung }.entries.mapNotNull { (x, i) -> schluessel(i)?.let { it to x } }.groupBy({ it.first }, { it.second })
        val keyB = b.individuals.filterKeys { it !in belegtB }.entries.mapNotNull { (x, i) -> schluessel(i)?.let { it to x } }.groupBy({ it.first }, { it.second })
        keyA.forEach { (k, xs) -> val ys = keyB[k]; if (xs.size == 1 && ys != null && ys.size == 1) { zuordnung[xs[0]] = ys[0] to null; belegtB += ys[0] } }
        // 3. aehnlich: Name nah, Geburtsjahr bis 2 Jahre daneben, Geschlecht nicht widerspruechlich
        val restA = a.individuals.filterKeys { it !in zuordnung }
        val restB = b.individuals.filterKeys { it !in belegtB }
        val nachNachname = restB.values.groupBy { namensform(it.person.surname) }
        restA.forEach { (x, i) ->
            val fn = namensform(i.person.surname); if (fn.isEmpty()) return@forEach
            val ja = geburtsjahr(i) ?: return@forEach
            val kandidaten = nachNachname.keys.filter { it == fn || (minOf(it.length, fn.length) >= 4 && levenshtein(it, fn) <= 1) }.flatMap { nachNachname.getValue(it) }
                .filter { j -> j.person.xref !in belegtB && vertraeglich(i, j) && geburtsjahr(j)?.let { kotlin.math.abs(it - ja) <= 2 } == true &&
                    (i.person.sex == "U" || j.person.sex == "U" || i.person.sex == j.person.sex) }
            if (kandidaten.size == 1) { zuordnung[x] = kandidaten[0].person.xref to PaarArt.Wahrscheinlich; belegtB += kandidaten[0].person.xref }
        }
        val paare = zuordnung.map { (x, zy) ->
            val (y, art) = zy
            val ae = unterschiede(a, a.individuals.getValue(x), b, b.individuals.getValue(y))
            Paar(x, y, art ?: if (ae.isEmpty()) PaarArt.Gleich else PaarArt.Geaendert, ae)
        }
        return Vergleich(paare, a.individuals.keys.filter { it !in zuordnung }, b.individuals.keys.filter { it !in belegtB })
    }

    fun geburtsjahr(i: ExportIndividual): Int? =
        (i.person.birth?.date?.year ?: 0).takeIf { it > 0 } ?: (i.person.chr?.date?.year ?: 0).takeIf { it > 0 }

    /** Die Unterschiede zweier zugeordneter Personen: Name, Geschlecht, Hauptereignisse, Eltern, Partner, Kinder, weitere Fakten. */
    fun unterschiede(a: TreeExport, i: ExportIndividual, b: TreeExport, j: ExportIndividual): List<Aenderung> {
        val out = ArrayList<Aenderung>()
        fun feld(f: String, x: String, y: String) { if (x.trim() != y.trim()) out += Aenderung(f, x.trim(), y.trim()) }
        feld("NAME", i.person.name, j.person.name)
        feld("SEX", i.person.sex, j.person.sex)
        fun ereignis(x: ExportIndividual, tags: List<String>): String = tags.firstNotNullOfOrNull { t -> x.facts.firstOrNull { it.tag == t && (it.date != null || it.place != null) } }?.let { ereignisText(it) }.orEmpty()
        feld("BIRT", ereignis(i, listOf("BIRT")), ereignis(j, listOf("BIRT")))
        feld("CHR", ereignis(i, listOf("CHR", "BAPM")), ereignis(j, listOf("CHR", "BAPM")))
        feld("DEAT", ereignis(i, listOf("DEAT")), ereignis(j, listOf("DEAT")))
        feld("BURI", ereignis(i, listOf("BURI", "CREM")), ereignis(j, listOf("BURI", "CREM")))
        // Verwandte ueber Namen, weil Kennungen zwischen Dateien nichts bedeuten muessen
        fun namen(t: TreeExport, xrefs: List<String>) = xrefs.mapNotNull { t.person(it)?.name?.takeIf(String::isNotBlank) }.sorted().joinToString("; ")
        fun eltern(t: TreeExport, x: ExportIndividual) = namen(t, x.famc.mapNotNull { t.families[it] }.flatMap { listOfNotNull(it.husband, it.wife) })
        fun partner(t: TreeExport, x: ExportIndividual) = namen(t, x.fams.mapNotNull { t.families[it] }.flatMap { listOfNotNull(it.husband, it.wife) }.filter { it != x.person.xref })
        fun kinder(t: TreeExport, x: ExportIndividual) = namen(t, x.fams.mapNotNull { t.families[it] }.flatMap { it.children })
        feld("ELTERN", eltern(a, i), eltern(b, j))
        feld("PARTNER", partner(a, i), partner(b, j))
        feld("KINDER", kinder(a, i), kinder(b, j))
        // uebrige Fakten je Tag: Mengen der Texte vergleichen
        val tagsA = i.facts.filter { it.tag !in EREIGNISSE && it.tag != "NAME" && it.tag != "SEX" }.groupBy { it.tag }
        val tagsB = j.facts.filter { it.tag !in EREIGNISSE && it.tag != "NAME" && it.tag != "SEX" }.groupBy { it.tag }
        (tagsA.keys + tagsB.keys).sorted().forEach { tag ->
            val x = tagsA[tag].orEmpty().map(::ereignisText).sorted().joinToString("; "); val y = tagsB[tag].orEmpty().map(::ereignisText).sorted().joinToString("; ")
            feld(tag, x, y)
        }
        return out
    }

    /** Datum, Ort und Wert eines Fakts als vergleichbarer Text ("12 MAR 1890 · Celle · Schmied"). */
    fun ereignisText(f: FactJson): String =
        listOfNotNull(f.date?.gedcom?.takeIf(String::isNotBlank) ?: f.date?.text?.takeIf(String::isNotBlank), f.place?.name?.takeIf(String::isNotBlank), f.value.takeIf(String::isNotBlank)).joinToString(" · ")
}
