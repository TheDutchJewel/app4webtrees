package de.bgghome.webtrees.nativ.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.res.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * "Auf Gruppenbildern" im Profil (Handy, Tablet, Personentafel am PC): Archivbilder, auf denen die Person als Gesicht
 * markiert ist (Modul Sammlungen ab Stufe 5, Filter person), als Leiste mit ihrem Gesicht; Antippen oeffnet den Betrachter
 * mit allen Gesichtern. Nichts gefunden: die Leiste bleibt weg.
 */
@Composable
fun AufGruppenbildernLeiste(xref: String, tree: String, viewModel: AppViewModel) {
    val bilder by produceState<List<ArchiveEntry>>(emptyList(), xref, tree) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val alle = mutableListOf<ArchiveEntry>()
                var seite = 1
                while (true) {
                    val p = viewModel.client.archivePerson(tree, xref, seite, 100)
                    alle += p.eintraege.filter { it.istBild }
                    if (seite >= p.seiten || seite >= 5) break
                    seite++
                }
                alle.toList()
            }.getOrDefault(emptyList())
        }
    }
    if (bilder.isEmpty()) return
    Column(Modifier.padding(top = 8.dp)) {
        Text(stringResource(Res.string.desk_group_photos, bilder.size), Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(bilder, key = { _, e -> e.pfad ?: e.original }) { i, e ->
                Column(Modifier.width(84.dp).clickable { viewModel.openArchiveEntries(bilder, i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    GesichtAusschnitt(e, e.bereiche.firstOrNull { it.xref == xref }, 84.dp)
                    Text(e.bildunterschrift.ifEmpty { e.datei }, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
