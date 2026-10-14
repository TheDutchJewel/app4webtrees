package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.ExportFamily
import de.bgghome.webtrees.nativ.api.ExportIndividual
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.FamilyJson
import de.bgghome.webtrees.nativ.api.IndividualDetail
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.TreeExport
import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.res.Res
import de.bgghome.webtrees.nativ.res.person_private

/*
 * Buecher zum Weitergeben (Issue 21): nur, was Besucher in webtrees sehen duerften - auch wenn ein Verwalter das Buch
 * erzeugt. api4webtrees meldet ab API-Stufe 36 an jedem Ereignis, jeder Person, Familie und jedem Paten, wer sie sehen
 * darf (RESN, Datenschutzregeln des Baums, Regeln fuer Lebende). Ohne diese Angabe (aelteres Modul) bleibt alles stehen.
 *
 * Eine Person, die Besucher nicht sehen duerfen, bleibt mit Namen stehen (oder "Privat", wenn auch der Name verborgen
 * ist), ohne Lebensdaten, Ereignisse und Bild; eine
 * Familie ohne ihre Ereignisse. Verknuepfte Paten, die Besucher nicht sehen duerfen, fallen weg.
 */

internal fun fuerBesucher(access: String?) = access == null || access == "visitor"

private val LEBENSDATEN = mapOf("BIRT" to "birth", "CHR" to "chr", "BAPM" to "chr", "DEAT" to "death", "BURI" to "buri", "CREM" to "buri", "OCCU" to "occupation")

/** Kurzfassung einer Person fuer Besucher: eingeschraenkte Person nur mit Namen, sonst ohne die gesperrten Lebensdaten. */
internal fun Person.oeffentlich(): Person {
    val neu = when {
        !fuerBesucher(access) -> copy(birth = null, death = null, chr = null, buri = null, occupation = null, thumb = null, marriages = emptyList())
        restricted.isEmpty() -> return this
        else -> {
            val weg = restricted.mapNotNull { LEBENSDATEN[it] }.toSet()
            copy(birth = birth.takeUnless { "birth" in weg }, death = death.takeUnless { "death" in weg }, chr = chr.takeUnless { "chr" in weg },
                buri = buri.takeUnless { "buri" in weg }, occupation = occupation.takeUnless { "occupation" in weg })
        }
    }
    // Den Namen sehen Besucher bei Lebenden nur, wenn der Baum es erlaubt - sonst "Privat" wie in webtrees
    val name = if (fuerBesucher(nameAccess)) neu else neu.copy(name = Texte.t(Res.string.person_private), sortName = Texte.t(Res.string.person_private), given = "", surname = "", call = "")
    return name.copy(lifespan = lebensjahre(neu), access = "visitor", restricted = emptyList(), nameAccess = "visitor")
}

/** Die Ereignisse, die Besucher sehen duerften, ohne Paten, die sie nicht sehen duerften. */
internal fun List<FactJson>.oeffentlich(): List<FactJson> =
    filter { fuerBesucher(it.access) }.map { f -> if (f.associates.all { fuerBesucher(it.access) }) f else f.copy(associates = f.associates.filter { fuerBesucher(it.access) }) }

private fun FamilyJson.oeffentlich(): FamilyJson {
    val sichtbar = fuerBesucher(access)
    val fakten = if (sichtbar) facts.oeffentlich() else emptyList()
    return copy(
        husband = husband?.oeffentlich(), wife = wife?.oeffentlich(), spouse = spouse?.oeffentlich(),
        children = children.map { it.oeffentlich() },
        facts = fakten,
        // Die Heirat der Kurzfassung nur, wenn ein Heiratsereignis sichtbar bleibt
        marriage = marriage?.takeIf { fakten.any { it.tag == "MARR" } },
        media = if (sichtbar) media else emptyList(),
    )
}

internal fun IndividualDetail.oeffentlich(): IndividualDetail {
    val sichtbar = fuerBesucher(person.access)
    return copy(
        person = person.oeffentlich(),
        facts = if (sichtbar) facts.oeffentlich() else facts.filter { it.tag == "NAME" && fuerBesucher(it.access) && fuerBesucher(person.nameAccess) },
        parentFamilies = parentFamilies.map { it.oeffentlich() },
        spouseFamilies = spouseFamilies.map { it.oeffentlich() },
        stepFamilies = stepFamilies.map { it.oeffentlich() },
        media = if (sichtbar) media else emptyList(),
        associatedIn = if (sichtbar) associatedIn else emptyList(),
    )
}

internal fun TreeExport.oeffentlich(): TreeExport = TreeExport(
    lastChange,
    individuals.mapValues { (_, i) ->
        val sichtbar = fuerBesucher(i.person.access)
        ExportIndividual(i.person.oeffentlich(), i.famc, i.fams,
            if (sichtbar) i.facts.oeffentlich() else i.facts.filter { it.tag == "NAME" && fuerBesucher(it.access) && fuerBesucher(i.person.nameAccess) },
            if (sichtbar) i.media else emptyList())
    },
    families.mapValues { (_, f) ->
        val sichtbar = fuerBesucher(f.access)
        val fakten = if (sichtbar) f.facts.oeffentlich() else emptyList()
        f.copy(facts = fakten, marriage = f.marriage?.takeIf { fakten.any { it.tag == "MARR" } }, media = if (sichtbar) f.media else emptyList())
    },
)

internal fun BuchDaten.oeffentlich() = BuchDaten(ahnen.mapValues { (_, a) -> AhnenEintrag(a.person.oeffentlich(), a.hatEltern, a.geschwister?.map { it.oeffentlich() }) },
    details.mapValues { it.value.oeffentlich() }, bilder)

internal fun NachfahrenDaten.oeffentlich() = NachfahrenDaten(wurzel, details.mapValues { it.value.oeffentlich() }, bilder)

internal fun FamilienDaten.oeffentlich() = FamilienDaten(baum.oeffentlich(), bilder, haeuser)
