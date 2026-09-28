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
fun wegFinden(ahnenA: Map<Long, AhnenEintrag>, ahnenB: Map<Long, AhnenEintrag>): Weg? = wegeFinden(ahnenA, ahnenB, 1).firstOrNull()

/**
 * Alle Verwandtschaftswege, der naechste zuerst, hoechstens [hoechstens]: jeder gemeinsame Vorfahr, unter dem auf
 * beiden Linien kein weiterer gemeinsamer Vorfahr liegt; ein gemeinsames Paar zaehlt als ein Weg.
 */
fun wegeFinden(ahnenA: Map<Long, AhnenEintrag>, ahnenB: Map<Long, AhnenEintrag>, hoechstens: Int = 4): List<Weg> {
    fun alle(m: Map<Long, AhnenEintrag>) = m.entries.filter { it.value.person.xref.isNotEmpty() }.groupBy({ it.value.person.xref }, { it.key })
    val a = alle(ahnenA); val b = alle(ahnenB)
    // Jedes Vorkommen eines gemeinsamen Vorfahren (bei Ahnenschwund mehrere) ist ein Kandidat
    val kandidaten = a.keys.intersect(b.keys).flatMap { x -> a.getValue(x).flatMap { na -> b.getValue(x).map { nb -> na to nb } } }
    // n liegt auf der Linie ueber m, wenn m durch Halbieren aus n entsteht
    fun ueber(n: Long, m: Long) = n > m && (n shr (reihe(n) - reihe(m))) == m
    val naechste = kandidaten.filter { (na, nb) -> kandidaten.none { (ma, mb) -> ueber(na, ma) && ueber(nb, mb) } }
    val wege = ArrayList<Weg>()
    naechste.sortedWith(compareBy({ reihe(it.first) + reihe(it.second) }, { it.first })).forEach { (na, nb) ->
        // Gemeinsames Paar: der Partner steht in beiden Tafeln an der Nachbarnummer - dann nur einmal, mit dem Mann als Wurzel
        val pa = ahnenA[na xor 1L]?.person?.xref; val pb = ahnenB[nb xor 1L]?.person?.xref
        val paar = na > 1 && nb > 1 && pa != null && pa.isNotEmpty() && pa == pb
        val w = if (paar && na % 2 == 1L) Weg(ahnenA, ahnenB, na xor 1L, nb xor 1L, true) else Weg(ahnenA, ahnenB, na, nb, paar)
        if (wege.none { it.na == w.na && it.nb == w.nb } && wege.size < hoechstens) wege += w
    }
    return wege
}

suspend fun wegLaden(client: WtClient, tree: String, a: String, b: String, generationen: Int): List<Weg> = coroutineScope {
    val x = async { ahnenLaden(client, tree, a, generationen) }
    val y = async { ahnenLaden(client, tree, b, generationen) }
    val ax = x.await(); val by = y.await()
    if (1L !in ax || 1L !in by) emptyList() else wegeFinden(ax, by)
}

/** Alle Verwandtschaften in Worten: die naechste, dann "ausserdem ..." fuer die weiteren. */
fun wegeText(wege: List<Weg>): String = wege.mapIndexed { i, w -> if (i == 0) wegText(w) else Texte.t(Res.string.desk_way_also, wegText(w)) }.joinToString("  ·  ")

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
