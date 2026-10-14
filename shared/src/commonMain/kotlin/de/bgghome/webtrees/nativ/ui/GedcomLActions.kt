package de.bgghome.webtrees.nativ.ui

import de.bgghome.webtrees.nativ.api.FactRequest
import de.bgghome.webtrees.nativ.api.FamilyJson
import de.bgghome.webtrees.nativ.res.*

/*
 * GEDCOM-L schreiben (api4webtrees ab API-Stufe 35): Hauptbild mit "_PRIM Y" und den Familienstand (1 _STAT).
 * Die Aufgabenfelder schreibt der Aufgaben-Dialog direkt ueber saveTask.
 */

/** Medium zum Hauptbild der gewaehlten Person machen: an die erste Stelle und mit "_PRIM Y". */
fun AppViewModel.primaryMedia(media: String) = write(Res.string.msg_saved) { tree, xref -> client.primaryMedia(tree, xref, media) }

/** Familienstand einer Familie setzen ("NOT MARRIED", "NEVER MARRIED", "UNKNOWN") oder mit "" entfernen. */
fun AppViewModel.familyStatus(family: FamilyJson, wert: String) = write(Res.string.msg_saved) { tree, _ ->
    val alt = family.facts.firstOrNull { it.tag == "_STAT" }
    when {
        wert.isBlank() && alt != null -> client.deleteFact(tree, family.xref, alt.id)
        alt != null -> client.saveFact(tree, family.xref, FactRequest(factId = alt.id, value = wert))
        else -> client.saveFact(tree, family.xref, FactRequest(tag = "_STAT", value = wert))
    }
}
