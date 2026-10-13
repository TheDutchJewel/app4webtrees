package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import de.bgghome.webtrees.nativ.api.PlaceSummary
import de.bgghome.webtrees.nativ.pruefung.OrtsGrund
import de.bgghome.webtrees.nativ.pruefung.OrtsPaar
import de.bgghome.webtrees.nativ.pruefung.ortsDubletten
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import org.jetbrains.compose.resources.stringResource

/** Paare, die als "gehoert nicht zusammen" markiert sind - je Baum in den Desktop-Einstellungen. */
private object GetrenntListe {
    private fun schluessel(tree: String) = "orte_getrennt_$tree"
    fun laden(tree: String): Set<String> = DeskLayout.prefs.getString(schluessel(tree), null)?.split('\n')?.filter(String::isNotEmpty)?.toSet().orEmpty()
    fun speichern(tree: String, s: Set<String>) = DeskLayout.prefs.putString(schluessel(tree), s.joinToString("\n"))
}

@Composable
private fun grundText(g: OrtsGrund): String = stringResource(when (g) {
    OrtsGrund.GleicheGov -> Res.string.desk_places_reason_gov
    OrtsGrund.Schreibweise -> Res.string.desk_places_reason_spelling
    OrtsGrund.OhneGliederung -> Res.string.desk_places_reason_structure
    OrtsGrund.Variante -> Res.string.desk_places_reason_variant
    OrtsGrund.Nahe -> Res.string.desk_places_reason_near
})

/**
 * Orte bereinigen: Kandidaten zum Zusammenfuehren aus der Ortsliste ([ortsDubletten]). Je Paar Richtung umkehren,
 * zusammenfuehren (ueber die Vorschau von PlaceRename, also mit Rueckfrage und Zahlen) oder getrennt lassen.
 * Nach dem Zusammenfuehren laedt das Ortsfenster die Liste neu, die Paare ergeben sich daraus von selbst neu.
 */
@Composable
internal fun OrteBereinigenDialog(
    tree: String, orte: List<PlaceSummary>, onZeigen: (String) -> Unit, onZusammenfuehren: (String, String) -> Unit, onDismiss: () -> Unit,
    onVerweise: ((List<PlaceSummary>) -> Unit)? = null, fortschritt: String? = null,
    adressen: List<de.bgghome.webtrees.nativ.data.HausVorschlag>? = null, onAdressen: (() -> Unit)? = null,
) {
    var getrennt by remember(tree) { mutableStateOf(GetrenntListe.laden(tree)) }
    var zeigeGetrennte by remember { mutableStateOf(false) }
    var umgedreht by remember { mutableStateOf(emptySet<String>()) }
    val alle = remember(orte) { ortsDubletten(orte) }
    val paare = alle.filter { zeigeGetrennte || it.schluessel !in getrennt }.map { if (it.schluessel in umgedreht) it.umgedreht() else it }
    val zahlGetrennt = alle.count { it.schluessel in getrennt }
    val farben = MaterialTheme.colorScheme

    WtAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.desk_places_cleanup_title) + " (${paare.size})") },
        text = {
            Column(Modifier.width(980.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Ab Stufe 32: Orte mit Ortsdatensatz, deren Ereignisse den Verweis "3 _LOC" noch nicht tragen
                val ohneVerweis = orte.filter { it.location != null && (it.unlinked ?: 0) > 0 }
                if (onVerweise != null && (ohneVerweis.isNotEmpty() || fortschritt != null)) Tipp(stringResource(Res.string.tipp_place_link_events)) {
                    Row(Modifier.fillMaxWidth().background(farben.secondaryContainer.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(fortschritt ?: stringResource(Res.string.desk_places_cleanup_links, ohneVerweis.size, ohneVerweis.sumOf { it.unlinked ?: 0 }),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { onVerweise(ohneVerweis) }, enabled = fortschritt == null && ohneVerweis.isNotEmpty(), shape = MaterialTheme.shapes.small) {
                            Text(stringResource(Res.string.desk_places_cleanup_links_button))
                        }
                    }
                }
                // Ab Stufe 33: Ereignisse mit Ort und Adresse - die Adresse kann ein eigener Ort unter dem Ort werden
                if (onAdressen != null && !adressen.isNullOrEmpty()) Tipp(stringResource(Res.string.tipp_places_addresses)) {
                    Row(Modifier.fillMaxWidth().background(farben.secondaryContainer.copy(alpha = 0.5f)).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.desk_places_addresses_banner, adressen.size, adressen.sumOf { it.fakten.size }), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = onAdressen, shape = MaterialTheme.shapes.small) { Text(stringResource(Res.string.desk_places_addresses_button)) }
                    }
                }
                Text(stringResource(Res.string.desk_places_cleanup_hint), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                if (paare.isEmpty()) Text(stringResource(Res.string.desk_places_cleanup_none), Modifier.padding(vertical = 20.dp), color = farben.onSurfaceVariant)
                else Box(Modifier.fillMaxWidth().height(460.dp).border(1.dp, farben.outlineVariant)) {
                    val ls = rememberLazyListState()
                    LazyColumn(state = ls) {
                        items(paare, key = { it.schluessel }) { p -> PaarZeile(p, p.schluessel in getrennt, onZeigen,
                            onUmkehren = { umgedreht = if (p.schluessel in umgedreht) umgedreht - p.schluessel else umgedreht + p.schluessel },
                            onZusammenfuehren = { onZusammenfuehren(p.von, p.nach) },
                            onGetrennt = {
                                getrennt = if (p.schluessel in getrennt) getrennt - p.schluessel else getrennt + p.schluessel
                                GetrenntListe.speichern(tree, getrennt)
                            })
                            HorizontalDivider(color = farben.outlineVariant)
                        }
                    }
                    ListenLeiste(ls)
                }
                if (zahlGetrennt > 0 && !zeigeGetrennte) Text(stringResource(Res.string.desk_places_cleanup_hidden, zahlGetrennt),
                    Modifier.clickable { zeigeGetrennte = true }, style = MaterialTheme.typography.labelMedium, color = farben.primary)
            }
        },
        breite = 1040.dp,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) } },
    )
}

@Composable
private fun PaarZeile(p: OrtsPaar, istGetrennt: Boolean, onZeigen: (String) -> Unit, onUmkehren: () -> Unit, onZusammenfuehren: () -> Unit, onGetrennt: () -> Unit) {
    val farben = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(if (istGetrennt) farben.surfaceVariant.copy(alpha = 0.5f) else farben.surface).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(grundText(p.grund), Modifier.width(150.dp), style = MaterialTheme.typography.labelMedium, color = farben.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text("${p.von}  (${p.vonAnzahl}×)", Modifier.clickable { onZeigen(p.von) }, style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("→ ${p.nach}  (${p.nachAnzahl}×)", Modifier.clickable { onZeigen(p.nach) }, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Tipp(stringResource(Res.string.desk_places_cleanup_swap)) {
            TextButton(onClick = onUmkehren) { Text("⇅") }
        }
        OutlinedButton(onClick = onZusammenfuehren, enabled = !istGetrennt, shape = MaterialTheme.shapes.small) { Text(stringResource(Res.string.desk_places_cleanup_merge)) }
        TextButton(onClick = onGetrennt) { Text(if (istGetrennt) "↺" else stringResource(Res.string.desk_places_cleanup_keep)) }
    }
}
