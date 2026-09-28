package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.DescendantNode
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.WtClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/*
 * Verwandtschaftstafel (A3, 28.09.2026): die Nachfahren aller Stammpaare einer Ahnen-Generation nebeneinander, die
 * vaeterliche Seite links. Linien, die frueher enden, beginnen bei ihrem letzten bekannten Paar. Jede Person steht
 * einmal ausfuehrlich (beim ersten Vorkommen von links); weitere Vorkommen sind Kaesten ohne Nachfahren, die das
 * Gitter mit "= C IV" auf die ausfuehrliche Stelle verweisen laesst.
 */

/** Ein Stammpaar: Kekule-Nummer des Mannes (Platz), die Wurzel (Mann, sonst Frau) und ihr Nachkommenbaum. */
class Stammpaar(val nummer: Long, val wurzel: Person, val partner: Person?, val baum: DescendantNode)

/**
 * Die Stammpaare zur Generation [generation] (1 Eltern, 2 Grosseltern, 3 Urgrosseltern ...): jedes Paar dieser Reihe,
 * dazu fruehere Paare, deren Linie endet (beide ohne bekannte Eltern). Sortiert nach ihrem Platz in der obersten Reihe.
 */
fun stammpaarNummern(ahnen: Map<Long, AhnenEintrag>, generation: Int): List<Long> {
    fun da(n: Long) = n in ahnen
    fun hatEltern(n: Long) = da(2 * n) || da(2 * n + 1)
    return (1..generation).flatMap { r ->
        (1L shl r until (1L shl (r + 1)) step 2).filter { m ->
            (da(m) || da(m + 1)) && (r == generation || (!hatEltern(m) && !hatEltern(m + 1)))
        }.map { it to r }
    }.sortedBy { (m, r) -> m shl (generation - r) }.map { it.first }
}

suspend fun verwandtLaden(client: WtClient, tree: String, xref: String, generation: Int, nachfahren: Int): TafelDaten {
    val ahnen = ahnenLaden(client, tree, xref, generation + 1)
    // Vom Stammpaar in Reihe r bis zum Probanden sind es r + 1 Reihen, darunter [nachfahren] weitere
    val paare = coroutineScope {
        stammpaarNummern(ahnen, generation).mapNotNull { m ->
            val wurzel = (ahnen[m] ?: ahnen[m + 1])?.person?.takeIf { it.xref.isNotEmpty() && !it.isPrivate } ?: return@mapNotNull null
            val partner = if (ahnen[m]?.person == wurzel) ahnen[m + 1]?.person else null
            Triple(m, wurzel, partner)
        }.chunked(6).flatMap { gruppe ->
            gruppe.map { (m, w, p) -> async { Stammpaar(m, w, p, nachfahrenLaden(client, tree, w.xref, reihe(m) + 1 + nachfahren)) } }.awaitAll()
        }
    }
    return TafelDaten(ahnen, null, stammpaare = paare)
}

/** Die Baeume der Tafel; wer schon weiter links steht, erscheint nur als Kasten ohne Nachfahren. */
fun verwandtWald(d: TafelDaten, o: TafelOptionen): List<TafelPerson> {
    val nummerVon = d.ahnen.entries.filter { it.value.person.xref.isNotEmpty() }.associate { it.value.person.xref to it.key }
    val gesehen = HashSet<String>()
    fun knoten(k: DescendantNode, rest: Int): TafelPerson {
        val x = k.person.xref
        val neu = x.isEmpty() || gesehen.add(x)
        val kinder = k.families.flatMap { it.children }
        return TafelPerson(
            k.person,
            if (!neu || rest <= 1) emptyList() else kinder.map { knoten(it, rest - 1) },
            nummer = nummerVon[x], partner = if (o.partner) k.families.mapNotNull { it.spouse } else emptyList(),
            hinweis = if (o.mehrHinweise && neu && rest <= 1) mehrKinder(kinder.size) else null,
            verheiratet = k.families.any { it.spouse != null },
        )
    }
    return d.stammpaare.filter { it.wurzel.xref !in o.ohneStamm }.map { knoten(it.baum, reihe(it.nummer) + 1 + o.nachfahren) }
}
