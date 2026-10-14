package de.bgghome.webtrees.nativ.desk

import androidx.compose.runtime.Composable
import de.bgghome.webtrees.nativ.gedcom.GedcomDatum
import de.bgghome.webtrees.nativ.res.*
import org.jetbrains.compose.resources.stringResource

/*
 * Anzeige der GEDCOM-L-Angaben (Addendum zu GEDCOM 5.5.1), die api4webtrees ab API-Stufe 34 liefert und gedaudit aus der
 * Datei liest: Beschriftung je Pfad unter dem Ereignis und ein Wert mit seinem Datum in der Sprache der Oberfläche.
 */

/** Beschriftung einer Angabe an einem Ereignis ("PLAC:_GOV" ...); unbekannte Pfade zeigen sich roh. */
@Composable
internal fun gedcomLLabel(tag: String): String = when (tag) {
    "PLAC:_GOV" -> stringResource(Res.string.desk_place_gov)
    "PLAC:_POST" -> stringResource(Res.string.desk_place_postal)
    "PLAC:_MAIDENHEAD" -> stringResource(Res.string.desk_gl_maidenhead)
    "ADDR:_NAME" -> stringResource(Res.string.desk_gl_addressee)
    else -> tag
}

/** "29553  (ab 1. Juli 1993)": der Wert und, wenn vorhanden, das GEDCOM-Datum lesbar dahinter. */
internal fun gedcomLWert(wert: String, datum: String?, zusatz: List<String?> = emptyList()): String =
    (listOf(wert) + zusatz.filterNotNull().filter(String::isNotBlank)).joinToString(" · ") +
        (datum?.takeIf(String::isNotBlank)?.let { "  (" + GedcomDatum.anzeige(it) + ")" } ?: "")

/** Art einer Forschungsaufgabe nach GEDCOM-L (TYPE 0, 1, 2) als Text; andere Werte roh. */
@Composable
internal fun aufgabenArt(typ: String?): String? = when (typ?.trim()) {
    null, "" -> null
    "0" -> stringResource(Res.string.desk_task_type_0)
    "1" -> stringResource(Res.string.desk_task_type_1)
    "2" -> stringResource(Res.string.desk_task_type_2)
    else -> typ
}

/** Status einer Forschungsaufgabe nach GEDCOM-L (open, completed, auto) als Text; andere Werte roh. */
@Composable
internal fun aufgabenStatus(status: String?): String? = when (status?.trim()?.lowercase()) {
    null, "" -> null
    "open" -> stringResource(Res.string.desk_task_status_open)
    "completed" -> stringResource(Res.string.desk_task_status_completed)
    "auto" -> stringResource(Res.string.desk_task_status_auto)
    else -> status
}
