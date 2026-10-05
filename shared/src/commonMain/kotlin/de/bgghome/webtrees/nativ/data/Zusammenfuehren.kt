package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.MergeFact
import de.bgghome.webtrees.nativ.pruefung.PruefErgebnis
import de.bgghome.webtrees.nativ.pruefung.Treffer

/** Zwei Personen, die wahrscheinlich dieselbe sind: [bleibt] nimmt [geht] auf. */
data class Dublette(val bleibt: String, val geht: String, val regel: String, val text: String) {
    val schluessel: String get() = listOf(bleibt, geht).sorted().joinToString("|")
    fun umgedreht() = copy(bleibt = geht, geht = bleibt)
}

/** Die Prüfregeln, deren Treffer Personenpaare sind. */
val DUBLETTEN_REGELN = listOf("219", "228")

/**
 * Das Personenpaar eines Treffers der Regeln 219/228: die beiden Datensätze seiner Fakten, sonst die Kennung am Ende
 * des Texts ("… = I12" bzw. "… (I12) …"). Null, wenn sich kein zweiter Datensatz finden lässt.
 */
fun paarAus(t: Treffer): Dublette? {
    val erste = t.person ?: return null
    val zweite = t.fakten.map { it.record }.firstOrNull { it != erste }
        ?: Regex("""= (\S+)$""").find(t.text)?.groupValues?.get(1)
        ?: Regex("""\(([A-Za-z0-9_:.-]+)\)""").find(t.text)?.groupValues?.get(1)
        ?: return null
    return if (zweite == erste) null else Dublette(erste, zweite, t.regel, t.text)
}

/** Alle Paare aus einem Prüfergebnis, 219 (sicher) vor 228 (unscharf), ohne Doppelte. */
fun dubletten(e: PruefErgebnis): List<Dublette> =
    DUBLETTEN_REGELN.flatMap { r -> e.treffer[r].orEmpty().mapNotNull(::paarAus) }.distinctBy { it.schluessel }

/** Die Kennungen der Fakten, die nach dem Vorschlag bleiben (Verknüpfungen immer). */
fun vorschlag(fakten: List<MergeFact>): Set<String> = fakten.filter { it.keep || it.link }.map { it.id }.toSet()
