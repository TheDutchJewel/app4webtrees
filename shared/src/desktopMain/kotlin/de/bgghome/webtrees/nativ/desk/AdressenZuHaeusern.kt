package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.data.GovTypen
import de.bgghome.webtrees.nativ.data.HausVorschlag
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import org.jetbrains.compose.resources.stringResource

/** GOV-Typnummer zum eingetippten Ortstyp: Haus/Hof-Woerter und die GOV-Namen (de/en); null, wenn unbekannt. */
internal fun govTypZu(typ: String): String? {
    val t = typ.trim().lowercase()
    if (t.isEmpty()) return null
    if (t in setOf("haus", "house", "maison", "huis", "casa", "gebäude", "gebaeude", "building")) return "17"
    if (t in setOf("hof", "farm", "bauernhof", "ferme", "boerderij", "granja")) return "24"
    return GovTypen.NAMEN.entries.firstOrNull { (_, n) -> n.first.equals(t, true) || n.second.equals(t, true) }?.key?.toString()
}

/**
 * Kennung des Ortsdatensatzes (_LOC) zu einem Ort - angelegt, wenn der Ort noch keinen hat (der neue Haus-Datensatz
 * haengt darunter; die Route Place will dafuer die Kennung). Null, wenn beides scheitert.
 */
internal suspend fun ortsdatensatz(client: de.bgghome.webtrees.nativ.api.WtClient, tree: String, ort: String): String? {
    runCatching { client.place(tree, ort).location?.xref }.getOrNull()?.let { return it }
    val neu = runCatching { client.savePlace(tree, de.bgghome.webtrees.nativ.api.PlaceRequest(ort)) }.getOrNull() ?: return null
    return neu.xref.takeIf { it.isNotEmpty() } ?: runCatching { client.place(tree, ort).location?.xref }.getOrNull()
}

/**
 * Adressen zu Haeusern (Orte bereinigen, ab API-Stufe 33): je Vorschlag ein Haken, die Art des neuen Ortsdatensatzes,
 * dann "Umwandeln". Das Schreiben macht das Ortsfenster; [fortschritt] zeigt es hier an.
 */
@Composable
internal fun AdressenDialog(vorschlaege: List<HausVorschlag>, fortschritt: String?, onAusfuehren: (List<HausVorschlag>, String) -> Unit, onDismiss: () -> Unit) {
    var gewaehlt by remember(vorschlaege) { mutableStateOf(vorschlaege.map { it.schluessel }.toSet()) }
    var typ by remember { mutableStateOf("") }
    val typVorgabe = stringResource(Res.string.desk_places_addresses_type_default)
    val farben = MaterialTheme.colorScheme
    val laeuft = fortschritt != null
    WtAlertDialog(
        onDismissRequest = { if (!laeuft) onDismiss() },
        title = { Text(stringResource(Res.string.desk_places_addresses_title) + " (${vorschlaege.size})") },
        text = {
            Column(Modifier.width(900.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.desk_places_addresses_hint), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(typ, { typ = it }, label = { Text(stringResource(Res.string.desk_places_addresses_type)) }, placeholder = { Text(typVorgabe) },
                        singleLine = true, modifier = Modifier.width(260.dp), enabled = !laeuft)
                    val g = govTypZu(typ.ifBlank { typVorgabe })
                    Text(if (g != null) "GOV ${g} – ${GovTypen.NAMEN[g.toInt()]?.first ?: ""}" else "", style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                    Box(Modifier.weight(1f))
                    TextButton(onClick = { gewaehlt = if (gewaehlt.size == vorschlaege.size) emptySet() else vorschlaege.map { it.schluessel }.toSet() }, enabled = !laeuft) {
                        Text(stringResource(if (gewaehlt.size == vorschlaege.size) Res.string.desk_places_addresses_none_sel else Res.string.desk_places_addresses_all_sel))
                    }
                }
                Box(Modifier.fillMaxWidth().height(420.dp).border(1.dp, farben.outlineVariant)) {
                    val ls = rememberLazyListState()
                    LazyColumn(state = ls) {
                        items(vorschlaege, key = { it.schluessel }) { v ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(v.schluessel in gewaehlt, { on -> gewaehlt = if (on) gewaehlt + v.schluessel else gewaehlt - v.schluessel }, enabled = !laeuft)
                                Column(Modifier.weight(1f)) {
                                    Text("${v.adresse}  ·  ${v.ort}", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("→ ${v.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(stringResource(Res.string.desk_places_addresses_events, v.fakten.size), style = MaterialTheme.typography.labelMedium, color = farben.onSurfaceVariant)
                            }
                            HorizontalDivider(color = farben.outlineVariant)
                        }
                    }
                    ListenLeiste(ls)
                }
                if (laeuft) { Text(fortschritt.orEmpty(), style = MaterialTheme.typography.bodySmall); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
        },
        breite = 960.dp,
        confirmButton = {
            TextButton(onClick = { onAusfuehren(vorschlaege.filter { it.schluessel in gewaehlt }, typ.ifBlank { typVorgabe }) }, enabled = !laeuft && gewaehlt.isNotEmpty()) {
                Text(stringResource(Res.string.desk_places_addresses_run, gewaehlt.size))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !laeuft) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
