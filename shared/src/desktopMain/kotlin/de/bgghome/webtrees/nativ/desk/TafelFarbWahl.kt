package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.res.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/*
 * Bedienung der Kastenfarben im Fenster "Tafel erstellen" (C1, 28.09.2026): Schema, Farbregeln und das Menue fuer
 * den Rechtsklick auf einen Kasten in der Vorschau. Die Farben selbst: TafelFarben.kt.
 */

private val schemaNamen: Map<FarbSchema, StringResource> = mapOf(
    FarbSchema.Geschlecht to Res.string.desk_chart_colour_sex, FarbSchema.Linie to Res.string.desk_chart_colour_line,
    FarbSchema.Zweig to Res.string.desk_chart_colour_branch,
)

internal val FELD_NAMEN: Map<RegelFeld, StringResource> = mapOf(
    RegelFeld.Nachname to Res.string.desk_chart_field_surname, RegelFeld.Vorname to Res.string.desk_chart_field_given,
    RegelFeld.Geburtsort to Res.string.desk_chart_field_birthplace, RegelFeld.Sterbeort to Res.string.desk_chart_field_deathplace,
    RegelFeld.Beruf to Res.string.desk_chart_field_occupation, RegelFeld.Geschlecht to Res.string.desk_chart_field_sex,
    RegelFeld.Lebend to Res.string.desk_chart_field_living,
)

/** Feste Werte der Felder Geschlecht und Lebend: gespeicherter Wert -> Anzeige. */
internal val FESTE_WERTE: Map<RegelFeld, List<Pair<String, StringResource>>> = mapOf(
    RegelFeld.Geschlecht to listOf("M" to Res.string.desk_chart_value_male, "F" to Res.string.desk_chart_value_female, "U" to Res.string.desk_chart_value_unknown),
    RegelFeld.Lebend to listOf("1" to Res.string.desk_chart_value_alive, "0" to Res.string.desk_chart_value_dead),
)

private fun KastenFarbe.compose() = Color(fuellung.rgb)
private fun KastenFarbe.rahmenCompose() = Color(rahmen.rgb)

/** Ein Farbfeld; Klick oeffnet die Farben zur Wahl. */
@Composable
private fun FarbFeld(farbe: Int, onWahl: (Int) -> Unit) {
    var offen by remember { mutableStateOf(false) }
    Box {
        val k = KASTEN_FARBEN[farbe.coerceIn(0, KASTEN_FARBEN.size - 1)]
        Box(Modifier.size(30.dp).background(k.compose(), RoundedCornerShape(4.dp)).border(1.dp, k.rahmenCompose(), RoundedCornerShape(4.dp)).clickable { offen = true })
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            FarbReihe { offen = false; onWahl(it) }
        }
    }
}

/** Alle Farben nebeneinander zum Anklicken. */
@Composable
private fun FarbReihe(onWahl: (Int) -> Unit) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        KASTEN_FARBEN.forEachIndexed { i, k ->
            Box(Modifier.size(24.dp).background(k.compose(), RoundedCornerShape(4.dp)).border(1.dp, k.rahmenCompose(), RoundedCornerShape(4.dp)).clickable { onWahl(i) })
        }
    }
}

/**
 * Einstellungen der Farben: Schema (nur beim Stil "Farbig"), die Farbregeln des Stammbaums und die gefaerbten Zweige.
 * [linie]/[zweig]: welche Schemata diese Tafelart zeigen kann.
 */
@Composable
internal fun FarbEinstellungen(
    o: TafelOptionen, linie: Boolean, zweig: Boolean, onSchema: (FarbSchema) -> Unit,
    regeln: List<FarbRegel>, onRegeln: (List<FarbRegel>) -> Unit, zweige: Int, onZweigeLeeren: () -> Unit,
) {
    if (o.stil == TafelStil.Farbig) Einstellung(stringResource(Res.string.desk_chart_colour_by)) {
        val arten = FarbSchema.entries.filter { it == FarbSchema.Geschlecht || (it == FarbSchema.Linie && linie) || (it == FarbSchema.Zweig && zweig) }
        val namen = arten.associateWith { stringResource(schemaNamen.getValue(it)) }
        Auswahl(namen[o.farbe] ?: namen.getValue(FarbSchema.Geschlecht), namen.values.toList()) { w -> onSchema(namen.entries.first { it.value == w }.key) }
    }
    if (o.stil == TafelStil.Schwarzweiss) return
    Text(stringResource(Res.string.desk_chart_rules), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text(stringResource(Res.string.desk_chart_rules_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    regeln.forEachIndexed { i, r ->
        fun aendern(neu: FarbRegel) = onRegeln(regeln.toMutableList().also { it[i] = neu })
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val felder = RegelFeld.entries.associateWith { stringResource(FELD_NAMEN.getValue(it)) }
                Box(Modifier.weight(1f)) {
                    Auswahl(felder.getValue(r.feld), felder.values.toList()) { w ->
                        val feld = felder.entries.first { it.value == w }.key
                        aendern(r.copy(feld = feld, text = FESTE_WERTE[feld]?.first()?.first ?: if (r.festerWert) "" else r.text))
                    }
                }
                Box(Modifier.weight(1f)) {
                    val werte = FESTE_WERTE[r.feld]
                    if (werte != null) {
                        val namen = werte.map { it.first to stringResource(it.second) }
                        Auswahl(namen.firstOrNull { it.first == r.text }?.second ?: namen.first().second, namen.map { it.second }) { w -> aendern(r.copy(text = namen.first { it.second == w }.first)) }
                    } else {
                        val gleich = stringResource(Res.string.desk_chart_rule_equals); val enthaelt = stringResource(Res.string.desk_chart_rule_contains)
                        Auswahl(if (r.enthaelt) enthaelt else gleich, listOf(gleich, enthaelt)) { w -> aendern(r.copy(enthaelt = w == enthaelt)) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!r.festerWert) OutlinedTextField(r.text, { aendern(r.copy(text = it)) }, singleLine = true, modifier = Modifier.weight(1f))
                else Box(Modifier.weight(1f))
                FarbFeld(r.farbe) { aendern(r.copy(farbe = it)) }
                TextButton(onClick = { onRegeln(regeln.filterIndexed { j, _ -> j != i }) }) { Text("✕") }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    TextButton(onClick = { onRegeln(regeln + FarbRegel(farbe = regeln.size % KASTEN_FARBEN.size)) }) { Text(stringResource(Res.string.desk_chart_rule_add)) }
    Text(stringResource(Res.string.desk_chart_branch_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (zweige > 0) Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.desk_chart_branches, zweige), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onZweigeLeeren) { Text(stringResource(Res.string.desk_chart_branches_clear)) }
    }
}

/** Rechtsklick irgendwo auf dem Element: [onKlick] bekommt die Stelle in dessen Koordinaten. */
@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.rechtsklick(onKlick: (Offset) -> Unit): Modifier = onPointerEvent(PointerEventType.Press) { e ->
    if (e.buttons.isSecondaryPressed) e.changes.firstOrNull()?.let { onKlick(it.position) }
}

/** Menue zum Kasten unter dem Rechtsklick: Name, Farben, "Farbe entfernen". [stelle]: Anker in dp. */
@Composable
internal fun ZweigMenue(karte: KartenOrt?, stelle: DpOffset, markiert: Boolean, onWahl: (Int?) -> Unit, onZu: () -> Unit) {
    DropdownMenu(expanded = karte != null, onDismissRequest = onZu, offset = stelle) {
        karte ?: return@DropdownMenu
        Text(karte.person.name.ifBlank { "?" }, Modifier.padding(horizontal = 12.dp, vertical = 4.dp).width(240.dp), fontWeight = FontWeight.SemiBold)
        Text(stringResource(Res.string.desk_chart_branch_menu), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FarbReihe { onWahl(it) }
        if (markiert) TextButton(onClick = { onWahl(null) }, Modifier.padding(horizontal = 4.dp)) { Text(stringResource(Res.string.desk_chart_branch_remove)) }
    }
}
