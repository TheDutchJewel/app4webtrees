package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.bgghome.webtrees.nativ.api.MediaJson
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.Field
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Medien an Quelle und Quellenverweis: wie in webtrees haengt ein Dokument entweder an der Quelle (das Digitalisat des
 * ganzen Kirchenbuchs) oder am einzelnen Verweis (der Scan dieser Taufe). Beides geht mit einer neuen Datei oder mit
 * einem Medium, das der Stammbaum schon hat.
 */

/** Ein vorhandenes Medium des Stammbaums waehlen: alle Medien mit Vorschaubild und Suche im Titel. */
@Composable
fun MedienWahlDialog(tree: String, client: WtClient, schonDa: Set<String>, onDismiss: () -> Unit, onWahl: (MediaJson) -> Unit) {
    val alle = remember { mutableStateListOf<MediaJson>() }
    var laden by remember { mutableStateOf(true) }
    var suche by remember { mutableStateOf("") }
    LaunchedEffect(tree) {
        var seite: Int? = 1
        while (seite != null) {
            val p = withContext(Dispatchers.IO) { runCatching { client.mediaList(tree, seite!!) }.getOrNull() } ?: break
            alle += p.data; seite = p.nextPage
        }
        laden = false
    }
    val treffer = alle.filter { it.xref !in schonDa && (suche.isBlank() || it.title.contains(suche.trim(), ignoreCase = true)) }.distinctBy { it.xref }
    WtAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.desk_media_pick_title)) },
        text = {
            Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field(suche, { suche = it }, Res.string.desk_media_pick_search)
                Text(if (laden) stringResource(Res.string.desk_chart_loading_any) else stringResource(Res.string.desk_media_pick_count, treffer.size),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().height(360.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)) {
                    LazyColumn(Modifier.fillMaxWidth()) {
                        items(treffer, key = { it.xref }) { m ->
                            Row(Modifier.fillMaxWidth().clickable { onWahl(m); onDismiss() }.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(44.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant), contentAlignment = Alignment.Center) {
                                    if (m.thumb != null) AsyncImage(model = m.thumb, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp))
                                    else Text(m.mime.substringAfter('/').take(4), style = MaterialTheme.typography.labelSmall)
                                }
                                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                    Text(m.title.ifBlank { m.xref }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(m.xref + (if (m.people.isNotEmpty()) " · " + m.people.joinToString(", ") { it.name } else ""), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

/**
 * Die Medien eines Verweises oder einer Quelle zum Bearbeiten: Vorschau, Entfernen (nur die Verknuepfung), Datei
 * hochladen (ohne Verknuepfung an die Person, die App haengt sie hier an) und vorhandenes Medium waehlen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedienBearbeiten(medien: List<MediaJson>, onEntfernen: (MediaJson) -> Unit, onDatei: () -> Unit, onVorhanden: () -> Unit, fehler: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(Res.string.tab_media), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        if (medien.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            medien.forEach { m ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Tipp(m.title) {
                        Box(Modifier.size(72.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant), contentAlignment = Alignment.Center) {
                            if (m.thumb != null) AsyncImage(model = m.thumb, contentDescription = m.title, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp))
                            else Text(m.title, Modifier.padding(4.dp), style = MaterialTheme.typography.labelSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text("✕", Modifier.clickable { onEntfernen(m) }.background(MaterialTheme.colorScheme.surface).padding(horizontal = 6.dp),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onDatei) { Text(stringResource(Res.string.desk_media_upload)) }
            TextButton(onClick = onVorhanden) { Text(stringResource(Res.string.desk_media_existing)) }
        }
        fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
