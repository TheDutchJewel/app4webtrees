package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import de.bgghome.webtrees.nativ.api.FamilyJson
import de.bgghome.webtrees.nativ.api.IndividualDetail

/*
 * Durch den Stammbaum mit der Tastatur: Pfeile fuehren zu Vater, Mutter, Kind und Geschwistern. Welche Taste in welche
 * Richtung geht, legt die Ansicht fest (im Navigator stehen die Vorfahren rechts, in der Familienansicht oben).
 */

enum class Richtung { Vater, Mutter, Kind, GeschwisterVor, GeschwisterZurueck }

/** Geschwister in der Reihenfolge der Herkunftsfamilie, samt der Person selbst (fuer Vor/Zurueck). */
fun geschwisterReihe(detail: IndividualDetail): List<String> =
    detail.parentFamilies.firstOrNull()?.children?.filter { !it.isPrivate || it.xref == detail.person.xref }?.map { it.xref }.orEmpty()

/** Wohin eine Taste fuehrt; null, wenn es dort niemanden (Sichtbaren) gibt. [familie]: die gewaehlte Partnerschaft. */
fun zielPerson(detail: IndividualDetail, familie: FamilyJson?, r: Richtung): String? {
    val eltern = detail.parentFamilies.firstOrNull()
    return when (r) {
        Richtung.Vater -> eltern?.husband?.takeIf { !it.isPrivate }?.xref
        Richtung.Mutter -> eltern?.wife?.takeIf { !it.isPrivate }?.xref
        Richtung.Kind -> familie?.children?.firstOrNull { !it.isPrivate }?.xref
        Richtung.GeschwisterVor, Richtung.GeschwisterZurueck -> {
            val reihe = geschwisterReihe(detail)
            val i = reihe.indexOf(detail.person.xref)
            if (i < 0) null else reihe.getOrNull(if (r == Richtung.GeschwisterVor) i + 1 else i - 1)
        }
    }
}

/**
 * Ein Bereich, der Tasten annimmt: holt sich den Fokus, wenn sich [schluessel] aendert (neue Mittelperson) und bei jedem
 * Mausklick hinein - sonst kaemen die Pfeile nach einem Klick auf eine Karte nicht mehr an.
 */
fun Modifier.tastenBereich(schluessel: Any?, onTaste: (KeyEvent) -> Boolean): Modifier = composed {
    val fokus = remember { FocusRequester() }
    LaunchedEffect(schluessel) { runCatching { fokus.requestFocus() } }
    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val e = awaitPointerEvent(PointerEventPass.Initial)
                    if (e.type == PointerEventType.Press) runCatching { fokus.requestFocus() }
                }
            }
        }
        .onPreviewKeyEvent(onTaste)
        .focusRequester(fokus)
        .focusable()
}
