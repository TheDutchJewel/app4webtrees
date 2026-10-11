package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.api.Bereich
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.GesichtAusschnitt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * "Auf Gruppenbildern" im Personenblatt (Modul Sammlungen ab Stufe 5, Filter person): alle Archivbilder, auf denen die
 * Person als Gesicht markiert ist - auch die, die nicht an ihr haengen. Je Bild das Gesicht als Ausschnitt; ein Klick
 * oeffnet das Bild mit allen Gesichtern. Am Stammbaum aendert sich dadurch nichts.
 */

/** Ergebnis der Suche: die Bilder und ob das Modul seinen Index schon fertig hat. */
private data class Gruppenbilder(val bilder: List<ArchiveEntry>, val vollstaendig: Boolean)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AufGruppenbildern(xref: String, tree: String, viewModel: AppViewModel, darfBereiche: Boolean) {
    var neu by remember { mutableIntStateOf(0) }
    val ergebnis by produceState<Result<Gruppenbilder>?>(null, xref, tree, neu) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bilder = mutableListOf<ArchiveEntry>()
                var seite = 1
                var ganz = true
                while (true) {
                    val p = viewModel.client.archivePerson(tree, xref, seite, 100)
                    bilder += p.eintraege
                    ganz = ganz && p.vollstaendig
                    if (seite >= p.seiten || seite >= 10) break
                    seite++
                }
                Gruppenbilder(bilder, ganz)
            }
        }
    }
    var offen by remember { mutableStateOf<ArchiveEntry?>(null) }
    val g = ergebnis?.getOrNull() ?: return
    // Nichts gefunden: der Abschnitt bleibt weg - ausser der Index ist noch nicht fertig
    if (g.bilder.isEmpty() && g.vollstaendig) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(Res.string.desk_group_photos, g.bilder.size), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            g.bilder.forEach { e ->
                val b = e.bereiche.firstOrNull { it.xref == xref }
                Tipp(e.bildunterschrift.ifEmpty { e.datei }) {
                    Column(Modifier.width(96.dp).clickable { offen = e }, horizontalAlignment = Alignment.CenterHorizontally) {
                        GesichtAusschnitt(e, b, 96.dp)
                        Text(e.bildunterschrift.ifEmpty { e.datei }, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (!g.vollstaendig) Text(stringResource(Res.string.desk_group_photos_incomplete), Modifier.clickable { neu++ },
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    offen?.let { e ->
        GesichterFenster(e, tree, viewModel, bearbeiten = darfBereiche, onGespeichert = { neu++ }, onClose = { offen = null })
    }
}

