package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.TreeExport

/**
 * Adressen zu Haeusern (Orte bereinigen): Viele Programme fuehren den Hof als Adresse unter dem Wohnort
 * ("2 PLAC Mengen", "2 ADDR Klosterstrasse 3"). Fuer Haeuserbuch und Hoefeliste muss das Haus ein eigener Ort mit
 * Ortsdatensatz sein ("Klosterstrasse 3, Mengen" mit Verweis). Hier die reine Rechnung: alle Ereignisse mit Ort und
 * Adresse, nach Haus gruppiert, mit dem Namen, den der neue Ort bekommt. Geschrieben wird im Ortsfenster.
 */
data class AdressFakt(val record: String, val fact: FactJson) {
    val place: String get() = fact.place?.name?.trim().orEmpty()
    /** Die erste Zeile der Adresse - Strasse und Hausnummer; weitere Zeilen (Hinterhaus) bleiben als Zusatz. */
    val adresse: String get() = fact.address.orEmpty().lineSequence().map(String::trim).firstOrNull(String::isNotEmpty).orEmpty()
}

data class HausVorschlag(val adresse: String, val ort: String, val fakten: List<AdressFakt>) {
    /** Der neue Ortsname: Adresse als unterste Ebene unter dem bisherigen Ort. */
    val name: String get() = "$adresse, $ort"
    val schluessel: String get() = adresse.lowercase() + "|" + Ortsnamen.schluessel(ort)
}

/** Alle Haus-Vorschlaege eines Baums: Ereignisse mit Ort und Adresse, gruppiert nach Adresse und Ort, nach Ort sortiert. */
fun adressenZuHaeusern(b: TreeExport): List<HausVorschlag> {
    val alle = ArrayList<AdressFakt>()
    b.individuals.forEach { (x, i) -> if (!i.person.isPrivate) i.facts.forEach { f -> AdressFakt(x, f).takeIf(::taugt)?.let(alle::add) } }
    b.families.forEach { (x, f) -> if (!f.isPrivate) f.facts.forEach { e -> AdressFakt(x, e).takeIf(::taugt)?.let(alle::add) } }
    return alle.groupBy { it.adresse.lowercase() + "|" + Ortsnamen.schluessel(it.place) }.values
        .map { g -> HausVorschlag(g.first().adresse, g.first().place, g) }
        .sortedWith(compareBy<HausVorschlag, String>(Ortsnamen.NATUERLICH) { it.ort }.thenBy(Ortsnamen.NATUERLICH) { it.adresse })
}

/** Ein Ereignis taugt, wenn Ort und Adresse da sind und die Adresse nicht schon im Ortsnamen steckt. */
private fun taugt(a: AdressFakt): Boolean {
    if (a.place.isEmpty() || a.adresse.isEmpty()) return false
    val teile = Ortsnamen.teile(a.place).map { it.lowercase() }
    return a.adresse.lowercase() !in teile
}
