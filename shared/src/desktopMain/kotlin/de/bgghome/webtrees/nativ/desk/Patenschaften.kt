package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.api.AssociatedIn
import de.bgghome.webtrees.nativ.api.IndividualDetail
import de.bgghome.webtrees.nativ.res.*
import org.jetbrains.compose.resources.stringResource

/*
 * Abschnitt "Patenschaften und Trauzeugenschaften" im Personenblatt (unter der Ereignistabelle), aus associatedIn der
 * API (ab Stufe 19). Nach Rolle gruppiert ("Patin bei", "Zeuge bei"), je Zeile Jahr, Ereignis, Person oder Paar,
 * Ort; ein Klick oeffnet die Person (bei Familien den Mann, sonst die Frau - sofern die API die XREFs liefert).
 * Fehlt associatedIn (aeltere Module), erscheint nichts. Auf- oder zugeklappt wird gemerkt.
 */
@Composable
internal fun Patenschaften(detail: IndividualDetail, onPerson: (String) -> Unit) {
    val liste = detail.associatedIn
    if (liste.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    var offen by remember { mutableStateOf(DeskLayout.prefs.getBoolean("blatt_paten", true)) }
    HorizontalDivider(color = colors.outlineVariant)
    Row(
        Modifier.fillMaxWidth().background(colors.surfaceVariant).clickable { offen = !offen; DeskLayout.prefs.putBoolean("blatt_paten", offen) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text((if (offen) "▾ " else "▸ ") + stringResource(Res.string.assoc_section) + " (${liste.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
    if (!offen) return
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxWidth().heightIn(max = 150.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(scroll).padding(horizontal = 12.dp, vertical = 4.dp)) {
            // Gruppen in Reihenfolge des ersten Auftretens: "Patin bei", "Zeugin bei" ...
            liste.groupBy { it.label2.ifBlank { it.role } }.forEach { (rolle, eintraege) ->
                Text(stringResource(Res.string.assoc_in_role, rolle), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                eintraege.forEach { a -> PatenschaftZeile(a, onPerson) }
            }
        }
        SenkrechteLeiste(scroll)
    }
}

@Composable
private fun PatenschaftZeile(a: AssociatedIn, onPerson: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ziel = if (a.recordType == "FAM") a.husband ?: a.wife else a.record.ifBlank { null }
    val was = if (a.recordType == "FAM") a.label + " " + a.name else stringResource(Res.string.assoc_in_of, a.label, a.name)
    val ort = a.place?.short?.ifBlank { null } ?: a.place?.name?.ifBlank { null }
    Row(Modifier.fillMaxWidth().then(if (ziel != null) Modifier.clickable { onPerson(ziel) } else Modifier).padding(vertical = 1.dp)) {
        Text(a.date?.year?.takeIf { it > 0 }?.toString().orEmpty(), Modifier.width(44.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(
            was + (ort?.let { ", $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium, color = if (ziel != null) colors.primary else colors.onSurface,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
