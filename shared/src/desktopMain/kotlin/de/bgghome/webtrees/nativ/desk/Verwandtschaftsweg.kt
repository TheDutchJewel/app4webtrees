package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.res.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/*
 * Verwandtschaftsweg (28.09.2026, war D5): wie sind zwei Personen verwandt? Aus beiden Ahnentafeln der naechste
 * gemeinsame Vorfahr (bei gemeinsamem Paar beide), darunter die zwei Linien zu den Personen. Rechnet im Programm,
 * braucht keine eigene API-Route.
 */

/** Ergebnis: Kekule-Nummer des gemeinsamen Vorfahren in A's und in B's Ahnentafel; [paar]: auch der Partner ist gemeinsam. */
class Weg(val ahnenA: Map<Long, AhnenEintrag>, val ahnenB: Map<Long, AhnenEintrag>, val na: Long, val nb: Long, val paar: Boolean) {
    val personA get() = ahnenA.getValue(1L).person
    val personB get() = ahnenB.getValue(1L).person
}

/** Den naechsten gemeinsamen Vorfahren suchen (auch eine der beiden Personen selbst, wenn sie Vorfahr der anderen ist). */
fun wegFinden(ahnenA: Map<Long, AhnenEintrag>, ahnenB: Map<Long, AhnenEintrag>): Weg? {
    fun erste(m: Map<Long, AhnenEintrag>) = m.entries.filter { it.value.person.xref.isNotEmpty() }.groupBy { it.value.person.xref }.mapValues { e -> e.value.minOf { it.key } }
    val a = erste(ahnenA); val b = erste(ahnenB)
    val bestes = a.keys.intersect(b.keys).minWithOrNull(compareBy({ reihe(a.getValue(it)) + reihe(b.getValue(it)) }, { a.getValue(it) })) ?: return null
    val na = a.getValue(bestes); val nb = b.getValue(bestes)
    // Gemeinsames Paar: der Partner steht in beiden Tafeln an der Nachbarnummer
    val pa = ahnenA[na xor 1L]?.person?.xref; val pb = ahnenB[nb xor 1L]?.person?.xref
    val paar = na > 1 && nb > 1 && pa != null && pa.isNotEmpty() && pa == pb
    // Beim Paar steht der Mann (gerade Nummer) als Wurzel, die Frau im Kasten
    return if (paar && na % 2 == 1L) Weg(ahnenA, ahnenB, na xor 1L, nb xor 1L, true) else Weg(ahnenA, ahnenB, na, nb, paar)
}

suspend fun wegLaden(client: WtClient, tree: String, a: String, b: String, generationen: Int): Weg? = coroutineScope {
    val x = async { ahnenLaden(client, tree, a, generationen) }
    val y = async { ahnenLaden(client, tree, b, generationen) }
    val ax = x.await(); val by = y.await()
    if (1L !in ax || 1L !in by) null else wegFinden(ax, by)
}

/** Die Verwandtschaft in Worten ("Cousins 2. Grades, eine Generation versetzt"). */
fun wegText(w: Weg): String {
    val ga = reihe(w.na); val gb = reihe(w.nb)
    val lo = minOf(ga, gb); val hi = maxOf(ga, gb)
    val halb = !w.paar && lo > 0
    return when {
        lo == 0 && hi == 0 -> Texte.t(Res.string.desk_way_same)
        lo == 0 -> when (hi) {
            1 -> Texte.t(Res.string.desk_way_parent)
            2 -> Texte.t(Res.string.desk_way_grandparent)
            else -> Texte.t(Res.string.desk_way_great, hi - 2)
        }
        lo == 1 && hi == 1 -> Texte.t(if (halb) Res.string.desk_way_half_siblings else Res.string.desk_way_siblings)
        lo == 1 -> when (hi) {
            2 -> Texte.t(Res.string.desk_way_uncle)
            3 -> Texte.t(Res.string.desk_way_great_uncle)
            else -> Texte.t(Res.string.desk_way_great_uncle_n, hi - 3)
        }
        else -> Texte.t(Res.string.desk_way_cousins, lo - 1) + (if (hi - lo == 1) ", " + Texte.t(Res.string.desk_way_removed_one) else if (hi > lo) ", " + Texte.t(Res.string.desk_way_removed, hi - lo) else "") +
            (if (halb) " " + Texte.t(Res.string.desk_way_half) else "")
    }
}

/** Die Tafel: der gemeinsame Vorfahr (beim Paar mit Partner im Kasten), darunter links die Linie zu A, rechts zu B. */
fun wegBaum(w: Weg): TafelPerson {
    fun linie(ahnen: Map<Long, AhnenEintrag>, n: Long): TafelPerson? {
        if (n < 1) return null
        val p = ahnen[n]?.person ?: return null
        return TafelPerson(p, listOfNotNull(if (n > 1) linie(ahnen, n / 2) else null))
    }
    val wurzel = w.ahnenA.getValue(w.na).person
    val partner = if (w.paar) listOfNotNull(w.ahnenA[w.na xor 1L]?.person) else emptyList()
    val kinder = listOfNotNull(if (w.na > 1) linie(w.ahnenA, w.na / 2) else null, if (w.nb > 1) linie(w.ahnenB, w.nb / 2) else null)
    return TafelPerson(wurzel, kinder, partner = partner)
}
