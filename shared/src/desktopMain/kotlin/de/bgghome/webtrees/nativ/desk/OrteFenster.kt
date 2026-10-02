package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import de.bgghome.webtrees.nativ.api.PlaceDetail
import de.bgghome.webtrees.nativ.api.PlaceEvent
import de.bgghome.webtrees.nativ.api.PlaceSummaryList
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.UiState
import de.bgghome.webtrees.nativ.ui.karte.GeoPunkt
import de.bgghome.webtrees.nativ.ui.karte.KachelEbene
import de.bgghome.webtrees.nativ.ui.karte.KachelKarte
import de.bgghome.webtrees.nativ.ui.karte.KartenPin
import de.bgghome.webtrees.nativ.ui.karte.KartenZustand
import de.bgghome.webtrees.nativ.ui.select
import de.bgghome.webtrees.nativ.ui.setRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Orte (ab API-Stufe 21): links alle Orte, wie sie an den Ereignissen stehen, mit Suche und Zahl der Ereignisse;
 * rechts der gewaehlte in Reitern - Personen und Familien mit ihren Ereignissen dort, Daten (Ebenen, uebergeordneter
 * Ort, Orte darunter, Ortsdatensatz _LOC, GOV-Kennung), Notizen, Quellen, Medien und Koordinaten mit Karte.
 * Stufe 1 liest nur.
 */

@Composable
fun OrteFenster(state: UiState, viewModel: AppViewModel, start: String?, openWeb: (String) -> Unit, onClose: () -> Unit) {
    val tree = state.tree?.name
    var neu by remember { mutableStateOf(0) }
    val liste by produceState<Result<PlaceSummaryList>?>(null, tree, neu) {
        value = null
        if (tree != null) value = withContext(Dispatchers.IO) { runCatching { viewModel.client.placeList(tree) } }
    }
    var suche by remember { mutableStateOf("") }
    var gewaehlt by remember(start) { mutableStateOf(start?.takeIf { it.isNotEmpty() }) }
    val detail by produceState<Result<PlaceDetail>?>(null, tree, gewaehlt, neu) {
        value = null
        val n = gewaehlt
        if (tree != null && n != null) value = withContext(Dispatchers.IO) { runCatching { viewModel.client.place(tree, n) } }
    }

    DialogWindow(
        onCloseRequest = onClose, title = stringResource(Res.string.desk_places_window) + " – " + state.tree?.title.orEmpty(),
        state = rememberDialogState(width = 1180.dp, height = 800.dp),
        onPreviewKeyEvent = { e ->
            when {
                e.type == KeyEventType.KeyDown && e.key == Key.Escape -> { onClose(); true }
                e.type == KeyEventType.KeyDown && e.key == Key.F1 -> { Hilfe.oeffnen("hauptfenster"); true }
                else -> false
            }
        },
    ) {
        var bearbeiten by remember { mutableStateOf<PlaceDetail?>(null) }
        var umbenennen by remember { mutableStateOf<de.bgghome.webtrees.nativ.api.PlaceRenameResult?>(null) }
        val scope = rememberCoroutineScope()
        var meldung by remember { mutableStateOf<String?>(null) }
        val darf = state.tree?.canEdit == true && (state.info?.api ?: 0) >= de.bgghome.webtrees.nativ.api.API_PLACE_WRITE
        val darfUmbenennen = state.tree?.canEdit == true && (state.info?.api ?: 0) >= de.bgghome.webtrees.nativ.api.API_PLACE_RENAME
        // Erst die Vorschau holen, dann fragen (wie viele Ereignisse, Zusammenfuehren, Abweichungen)
        fun vorschau(von: String, nach: String) {
            meldung = null
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { viewModel.client.renamePlace(tree.orEmpty(), von, nach, preview = true) } }
                    .onSuccess { umbenennen = it }.onFailure { meldung = it.message ?: "?" }
            }
        }
        val verweise = meldung
        DeskTheme {
            OrteInhalt(liste, detail, gewaehlt, { gewaehlt = it }, suche, { suche = it }, viewModel, openWeb,
                onBearbeiten = if (darf) ({ bearbeiten = it }) else null, meldung = verweise,
                onUmbenennen = if (darfUmbenennen) ({ von, nach -> vorschau(von, nach) }) else null)
            umbenennen?.let { v ->
                UmbenennenDialog(v, onDismiss = { umbenennen = null }) {
                    umbenennen = null
                    scope.launch {
                        runCatching { withContext(Dispatchers.IO) { viewModel.client.renamePlace(tree.orEmpty(), v.from, v.to, preview = false) } }
                            .onSuccess { r ->
                                meldung = de.bgghome.webtrees.nativ.Texte.t(Res.string.desk_place_renamed, r.events) +
                                    if (r.pending) "  " + de.bgghome.webtrees.nativ.Texte.t(Res.string.desk_place_rename_moderated) else ""
                                gewaehlt = r.to
                                neu++
                            }.onFailure { meldung = it.message ?: "?" }
                    }
                }
            }
            bearbeiten?.let { o ->
                OrtDialog(tree.orEmpty(), o, viewModel.client, state.info?.user?.isAdmin == true, openWeb, onDismiss = { bearbeiten = null }) { r ->
                    meldung = listOfNotNull(
                        r.linked?.takeIf { it > 0 }?.let { de.bgghome.webtrees.nativ.Texte.t(Res.string.desk_place_linked, it) },
                        if (r.pending) de.bgghome.webtrees.nativ.Texte.t(Res.string.msg_pending, o.name) else null,
                    ).joinToString("  ").ifEmpty { null }
                    neu++
                }
            }
        }
    }
}

/** Der Inhalt des Fensters, ohne Fenster (testbar ohne Bildschirm). */
@Composable
internal fun OrteInhalt(
    liste: Result<PlaceSummaryList>?, detail: Result<PlaceDetail>?, gewaehlt: String?, onWahl: (String) -> Unit,
    suche: String, onSuche: (String) -> Unit, viewModel: AppViewModel?, openWeb: (String) -> Unit, reiterStart: Int = 0,
    onBearbeiten: ((PlaceDetail) -> Unit)? = null, meldung: String? = null, onUmbenennen: ((String, String) -> Unit)? = null,
    karteStart: Boolean = false,
) {
    // Rechts entweder der gewaehlte Ort oder die Karte aller Orte
    var karte by remember { mutableStateOf(karteStart) }
    Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── Liste ──
        Column(Modifier.width(380.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
            Box(Modifier.fillMaxWidth().padding(10.dp)) {
                BasicTextField(suche, onSuche, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small).padding(8.dp))
                if (suche.isEmpty()) Text(stringResource(Res.string.desk_places_search), Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
            val alle = liste?.getOrNull()?.places.orEmpty()
            val treffer = if (suche.isBlank()) alle else alle.filter { it.name.contains(suche.trim(), ignoreCase = true) || it.gov?.contains(suche.trim(), ignoreCase = true) == true }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.desk_places_count, treffer.size, alle.size), Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Umschalter Ort | Karte
                listOf(false to Res.string.desk_places_details, true to Res.string.desk_places_map).forEach { (k, text) ->
                    val an = karte == k
                    Text(stringResource(text), Modifier.padding(start = 4.dp)
                        .background(if (an) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                        .clickable { karte = k }.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium, fontWeight = if (an) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f)) {
                val fehler = liste?.exceptionOrNull()
                when {
                    fehler != null -> Text(fehler.message ?: "?", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                    liste == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    else -> {
                        val ls = rememberLazyListState()
                        LazyColumn(Modifier.fillMaxSize(), state = ls) {
                            items(treffer, key = { it.name }) { o ->
                                val aktiv = o.name.equals(gewaehlt, ignoreCase = true)
                                Row(Modifier.fillMaxWidth().background(if (aktiv) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                    .fokusRahmen().clickable { onWahl(o.name) }.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(o.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    // Ein Punkt fuer "hat Koordinaten", wie das Symbol im Reiter
                                    if (o.lat != null) Text("◉ ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text("${o.events}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        ListenLeiste(ls)
                    }
                }
            }
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Der gewaehlte Ort oder die Karte ──
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val alleOrte = liste?.getOrNull()?.places
            when {
                karte && alleOrte != null -> {
                    val treffer = if (suche.isBlank()) alleOrte else alleOrte.filter { it.name.contains(suche.trim(), ignoreCase = true) || it.gov?.contains(suche.trim(), ignoreCase = true) == true }
                    OrtsKarte(treffer, gewaehlt, onWahl, onAnzeigen = { onWahl(it); karte = false })
                }
                gewaehlt == null -> Text(stringResource(Res.string.desk_places_choose), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                detail == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                detail.exceptionOrNull() != null -> Text(detail.exceptionOrNull()?.message ?: "?", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                else -> OrtDetail(detail.getOrThrow(), viewModel, onWahl, openWeb, reiterStart, onBearbeiten, onUmbenennen)
            }
            meldung?.let { Text(it, Modifier.align(Alignment.BottomStart).padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

private enum class OrtReiter(val titel: StringResource) {
    Personen(Res.string.desk_place_tab_people), Daten(Res.string.desk_place_tab_data), Notizen(Res.string.desk_tab_notes),
    Quellen(Res.string.desk_sources_window), Medien(Res.string.tab_media), Koordinaten(Res.string.desk_place_tab_coords),
}

@Composable
private fun OrtDetail(o: PlaceDetail, viewModel: AppViewModel?, onWahl: (String) -> Unit, openWeb: (String) -> Unit, reiterStart: Int,
                      onBearbeiten: ((PlaceDetail) -> Unit)?, onUmbenennen: ((String, String) -> Unit)? = null) {
    var reiter by remember(o.name) { mutableStateOf(OrtReiter.entries[reiterStart]) }
    val loc = o.location
    // Wie viel in einem Reiter steht - 0 = leer (der Reiter bleibt, steht aber blasser)
    fun anzahl(r: OrtReiter): Int = when (r) {
        OrtReiter.Personen -> o.individuals.size + o.families.size + o.moreIndividuals + o.moreFamilies
        OrtReiter.Daten -> 1
        OrtReiter.Notizen -> loc?.notes?.size ?: 0
        OrtReiter.Quellen -> loc?.sources?.size ?: 0
        OrtReiter.Medien -> loc?.media?.size ?: 0
        OrtReiter.Koordinaten -> if (o.lat != null && o.lng != null) 1 else 0
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (onUmbenennen != null && o.canEdit) {
                // Wie ein Eingabefeld: aendern und mit dem Haken (oder Enter) bestaetigen - gibt es den Namen schon,
                // wird daraus ein Zusammenfuehren
                var neuerName by remember(o.name) { mutableStateOf(o.name) }
                val geaendert = neuerName.trim().isNotEmpty() && neuerName.trim() != o.name
                OutlinedTextField(neuerName, { neuerName = it }, singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f).onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.Enter && geaendert) { onUmbenennen(o.name, neuerName.trim()); true } else false
                    },
                    trailingIcon = {
                        Tipp(stringResource(Res.string.desk_place_rename_tip)) {
                            IconButton(onClick = { onUmbenennen(o.name, neuerName.trim()) }, enabled = geaendert) {
                                Icon(Icons.Default.CheckCircle, stringResource(Res.string.desk_place_rename_tip),
                                    tint = if (geaendert) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    })
            } else {
                SelectionContainer(Modifier.weight(1f)) { Text(o.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            }
            if (onBearbeiten != null && o.canEdit && loc?.canEdit != false) OutlinedButton(onClick = { onBearbeiten(o) }, shape = MaterialTheme.shapes.small) {
                Text(stringResource(Res.string.action_edit))
            }
            if (loc != null && loc.url.isNotBlank()) OutlinedButton(onClick = { openWeb(loc.url) }, shape = MaterialTheme.shapes.small) { Text(stringResource(Res.string.chip_open_web)) }
        }
        PrimaryScrollableTabRow(selectedTabIndex = reiter.ordinal, edgePadding = 12.dp, containerColor = MaterialTheme.colorScheme.background) {
            OrtReiter.entries.forEach { r ->
                val n = anzahl(r)
                val text = stringResource(r.titel) + if (n > 0 && r != OrtReiter.Daten && r != OrtReiter.Koordinaten) " ($n)" else ""
                Tab(selected = r == reiter, onClick = { reiter = r }, text = {
                    Text(text, maxLines = 1, color = if (n == 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (reiter) {
                OrtReiter.Personen -> Rollbar { Personen(o, viewModel) }
                OrtReiter.Daten -> Rollbar { Daten(o, onWahl, openWeb) }
                OrtReiter.Notizen -> Rollbar {
                    val notes = loc?.notes.orEmpty()
                    if (notes.isEmpty()) Leer() else notes.forEach { SelectionContainer { Text(it, style = MaterialTheme.typography.bodyMedium) } }
                }
                OrtReiter.Quellen -> Rollbar {
                    val q = loc?.sources.orEmpty()
                    if (q.isEmpty()) Leer() else q.forEach { s ->
                        Row { Text(s.title ?: s.xref.orEmpty(), Modifier.width(360.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(s.page.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                OrtReiter.Medien -> Rollbar { val m = loc?.media.orEmpty(); if (m.isEmpty()) Leer() else MedienReihe(m, openWeb) }
                OrtReiter.Koordinaten -> Koordinaten(o, openWeb)
            }
        }
    }
}

@Composable
private fun Rollbar(inhalt: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { inhalt() }
        SenkrechteLeiste(scroll)
    }
}

@Composable
private fun Leer() = Text(stringResource(Res.string.desk_place_nothing), color = MaterialTheme.colorScheme.onSurfaceVariant)

/** "Geburt 1800, Taufe 1800" */
private fun ereignisse(f: List<PlaceEvent>) = f.joinToString(", ") { e -> listOfNotNull(e.label, e.date?.year?.takeIf { it > 0 }?.toString()).joinToString(" ") }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Personen(o: PlaceDetail, viewModel: AppViewModel?) {
    val farben = MaterialTheme.colorScheme
    if (o.individuals.isEmpty() && o.families.isEmpty()) Leer()
    o.individuals.forEach { p ->
        Row(Modifier.fillMaxWidth().fokusRahmen()
            .combinedClickable(enabled = !p.isPrivate && viewModel != null, onDoubleClick = { viewModel?.setRoot(p.xref) }) { viewModel?.select(p.xref) }
            .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(registerName(p.person(), stringResource(Res.string.person_private), stringResource(Res.string.person_no_name)) + jahre(p.person()).let { if (it.isNotEmpty()) "  $it" else "" },
                Modifier.width(340.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ereignisse(p.facts), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
        }
    }
    if (o.moreIndividuals > 0) Text(stringResource(Res.string.desk_source_more, o.moreIndividuals), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
    if (o.families.isNotEmpty()) {
        Text(stringResource(Res.string.desk_place_families), Modifier.padding(top = 10.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(color = farben.outlineVariant)
    }
    o.families.forEach { f ->
        // Klick waehlt den Ehemann (sonst die Ehefrau) - die Familie selbst hat keine eigene Ansicht
        val wer = f.husband ?: f.wife
        Row(Modifier.fillMaxWidth().fokusRahmen().clickable(enabled = wer != null && viewModel != null) { wer?.let { viewModel?.select(it) } }.padding(vertical = 4.dp)) {
            Text(f.name, Modifier.width(340.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ereignisse(f.facts), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
        }
    }
    if (o.moreFamilies > 0) Text(stringResource(Res.string.desk_source_more, o.moreFamilies), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
}

@Composable
private fun Daten(o: PlaceDetail, onWahl: (String) -> Unit, openWeb: (String) -> Unit) {
    val farben = MaterialTheme.colorScheme
    @Composable
    fun Zeile(label: StringResource, inhalt: @Composable () -> Unit) {
        Row(Modifier.padding(vertical = 2.dp)) {
            Text(stringResource(label), Modifier.width(160.dp), style = MaterialTheme.typography.bodyMedium, color = farben.onSurfaceVariant)
            Column { inhalt() }
        }
    }
    @Composable
    fun Verweis(text: String, onClick: () -> Unit) =
        Text(text, Modifier.clickable(onClick = onClick), style = MaterialTheme.typography.bodyMedium, color = farben.primary)

    Zeile(Res.string.desk_place_levels) { Text(o.levels.joinToString(" › "), style = MaterialTheme.typography.bodyMedium) }
    o.parent?.let { p -> Zeile(Res.string.desk_place_parent) { Verweis(p) { onWahl(p) } } }
    if (o.children.isNotEmpty()) Zeile(Res.string.desk_place_children) {
        o.children.forEach { c -> Verweis(c.name.substringBefore(", ") + if (c.events > 0) "  (${c.events})" else "") { onWahl(c.name) } }
    }
    Zeile(Res.string.desk_place_events) { Text("${o.events}", style = MaterialTheme.typography.bodyMedium) }
    val loc = o.location
    Zeile(Res.string.desk_place_record) {
        if (loc == null) Text(stringResource(Res.string.desk_place_no_record), style = MaterialTheme.typography.bodyMedium, color = farben.onSurfaceVariant)
        else Text(listOf(loc.name, loc.xref).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
    }
    loc?.gov?.let { gov ->
        Zeile(Res.string.desk_place_gov) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectionContainer { Text(gov, style = MaterialTheme.typography.bodyMedium) }
                Verweis(stringResource(Res.string.desk_place_gov_show)) { openWeb("https://gov.genealogy.net/item/show/$gov") }
            }
        }
    }
}

@Composable
private fun Koordinaten(o: PlaceDetail, openWeb: (String) -> Unit) {
    val lat = o.lat; val lng = o.lng
    val farben = MaterialTheme.colorScheme
    if (lat == null || lng == null) {
        Text(stringResource(Res.string.desk_place_no_coords), Modifier.padding(20.dp), color = farben.onSurfaceVariant)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row { Text(stringResource(Res.string.desk_place_decimal), Modifier.width(200.dp), style = MaterialTheme.typography.bodyMedium, color = farben.onSurfaceVariant)
                SelectionContainer { Text(dezimal(lat, lng), style = MaterialTheme.typography.bodyMedium) } }
            Row { Text(stringResource(Res.string.desk_place_dms), Modifier.width(200.dp), style = MaterialTheme.typography.bodyMedium, color = farben.onSurfaceVariant)
                SelectionContainer { Text(gms(lat, true) + "   " + gms(lng, false), style = MaterialTheme.typography.bodyMedium) } }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (o.coordSource) {
                    "location" -> Res.string.desk_place_coords_location
                    "mapData" -> Res.string.desk_place_coords_mapdata
                    "event" -> Res.string.desk_place_coords_event
                    else -> null
                }?.let { Text(stringResource(it), Modifier.width(200.dp), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant) }
                OutlinedButton(onClick = { openWeb("https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=13/$lat/$lng") }, shape = MaterialTheme.shapes.small) {
                    Text("OpenStreetMap")
                }
            }
        }
        HorizontalDivider(color = farben.outlineVariant)
        val zustand = remember(o.name) { KartenZustand() }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val w = constraints.maxWidth; val h = constraints.maxHeight
            LaunchedEffect(o.name, w, h) { zustand.passeEin(listOf(GeoPunkt(lat, lng)), w, h, einzelZoom = 12, maxZoom = 15) }
            KachelKarte(zustand, KachelEbene.STANDARD, Modifier.fillMaxSize(), pins = listOf(KartenPin(lat, lng, farbe = farben.primary, radiusDp = 9f)))
        }
    }
}

internal fun dezimal(lat: Double, lng: Double) = "%.6f, %.6f".format(java.util.Locale.ROOT, lat, lng)

/** 53,778417 -> 53° 46′ 42,3″ N */
internal fun gms(wert: Double, breite: Boolean): String {
    val a = abs(wert)
    var grad = a.toInt()
    var min = ((a - grad) * 60).toInt()
    var sek = ((a - grad - min / 60.0) * 3600 * 10).roundToInt() / 10.0
    if (sek >= 60) { sek -= 60; min++ }
    if (min >= 60) { min -= 60; grad++ }
    val seite = if (breite) (if (wert < 0) "S" else "N") else (if (wert < 0) "W" else "E")
    return "$grad° $min′ ${"%.1f".format(java.util.Locale.ROOT, sek)}″ $seite"
}

/** Rueckfrage vor dem Umbenennen/Zusammenfuehren mit den Zahlen aus der Vorschau. */
@Composable
private fun UmbenennenDialog(v: de.bgghome.webtrees.nativ.api.PlaceRenameResult, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val text = buildList {
        add(stringResource(Res.string.desk_place_rename_text, v.from, v.to, v.events, v.records))
        if (v.subPlaces > 0) add(stringResource(Res.string.desk_place_rename_sub, v.subPlaces))
        if (v.merge) add(stringResource(Res.string.desk_place_merge_text, v.to))
        if ("gov" in v.location.conflicts) add(stringResource(Res.string.desk_place_conflict_gov, v.to))
        if ("coordinates" in v.location.conflicts) add(stringResource(Res.string.desk_place_conflict_coords, v.to))
        if (v.skipped > 0) add(stringResource(Res.string.desk_place_rename_skipped, v.skipped))
    }.joinToString("\n\n")
    de.bgghome.webtrees.nativ.ui.ConfirmDialog(
        title = stringResource(if (v.merge) Res.string.desk_place_merge_title else Res.string.desk_place_rename_title),
        text = text,
        confirm = stringResource(if (v.merge) Res.string.desk_place_merge_do else Res.string.desk_place_rename_do),
        onDismiss = onDismiss, onConfirm = onConfirm,
    )
}

private val farbeWenig = androidx.compose.ui.graphics.Color(0xFF6FA8AE)
private val farbeMittel = androidx.compose.ui.graphics.Color(0xFF1F6F78)
private val farbeViel = androidx.compose.ui.graphics.Color(0xFF0B3D44)
private val farbeGruppe = androidx.compose.ui.graphics.Color(0xFF6B7276)
private val farbeGewaehlt = androidx.compose.ui.graphics.Color(0xFFC0582A)

/** Farbe nach Zahl der Ereignisse wie im Ortsregister: wenige hell, viele dunkel. */
private fun ortFarbe(ereignisse: Int) = when { ereignisse >= 100 -> farbeViel; ereignisse >= 20 -> farbeMittel; else -> farbeWenig }

/** Durchmesser 18-32 px nach Wurzel der Ereignisse (wie im Ortsregister) - hier als Radius in dp. */
private fun ortRadius(ereignisse: Int) = (kotlin.math.sqrt(ereignisse.toDouble()) * 6).coerceIn(18.0, 32.0).toFloat() / 2

/**
 * Orte, die auf der Karte naeher als etwa 50 Pixel beieinander liegen, als eine Gruppe - je Zoomstufe neu, wie
 * MarkerCluster im Ortsregister.
 */
internal fun ortsGruppen(orte: List<de.bgghome.webtrees.nativ.api.PlaceSummary>, zoom: Int, zelle: Double = 50.0): List<List<de.bgghome.webtrees.nativ.api.PlaceSummary>> =
    orte.filter { it.lat != null && it.lng != null }.groupBy { o ->
        val x = de.bgghome.webtrees.nativ.ui.karte.WebMercator.xTile(o.lng!!, zoom) * 256 / zelle
        val y = de.bgghome.webtrees.nativ.ui.karte.WebMercator.yTile(o.lat!!, zoom) * 256 / zelle
        kotlin.math.floor(x).toLong() to kotlin.math.floor(y).toLong()
    }.values.toList()

/** Karte aller Orte mit Koordinaten: Groesse und Farbe nach Ereignissen, Gruppen je Zoomstufe, Klick waehlt. */
@Composable
private fun OrtsKarte(orte: List<de.bgghome.webtrees.nativ.api.PlaceSummary>, gewaehlt: String?, onWahl: (String) -> Unit, onAnzeigen: (String) -> Unit) {
    val mit = remember(orte) { orte.filter { it.lat != null && it.lng != null } }
    val farben = MaterialTheme.colorScheme
    if (mit.isEmpty()) {
        Text(stringResource(Res.string.desk_places_none_on_map), Modifier.padding(24.dp), color = farben.onSurfaceVariant)
        return
    }
    val zustand = remember { KartenZustand() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth; val h = constraints.maxHeight
        // Alle (gefundenen) Orte ins Bild - neu, wenn die Suche die Auswahl aendert
        LaunchedEffect(mit, w, h) { zustand.passeEin(mit.map { GeoPunkt(it.lat!!, it.lng!!) }, w, h, randPx = 40, einzelZoom = 10, maxZoom = 12) }
        // Erst spaeter in der Liste gewaehlt: in die Mitte holen (beim Oeffnen bleiben alle Orte im Bild)
        var vorher by remember { mutableStateOf(gewaehlt) }
        LaunchedEffect(gewaehlt) {
            if (gewaehlt != vorher) mit.firstOrNull { it.name.equals(gewaehlt, ignoreCase = true) }?.let { zustand.setze(it.lat!!, it.lng!!, maxOf(zustand.zoom, 9)) }
            vorher = gewaehlt
        }
        // Der gewaehlte Ort steht immer einzeln, die uebrigen werden gruppiert
        val gruppen = remember(mit, zustand.zoom, gewaehlt) {
            val (ich, rest) = mit.partition { it.name.equals(gewaehlt, ignoreCase = true) }
            ortsGruppen(rest, zustand.zoom) + ich.map { listOf(it) }
        }
        val pins = gruppen.map { g ->
            val ereignisse = g.sumOf { it.events }
            val drin = g.any { it.name.equals(gewaehlt, ignoreCase = true) }
            if (g.size == 1) KartenPin(g[0].lat!!, g[0].lng!!, text = "$ereignisse", farbe = if (drin) farbeGewaehlt else ortFarbe(ereignisse), radiusDp = ortRadius(ereignisse), tag = g[0])
            else KartenPin(g.map { it.lat!! }.average(), g.map { it.lng!! }.average(), text = "${g.size}",
                farbe = if (drin) farbeGewaehlt else farbeGruppe, radiusDp = ortRadius(ereignisse), tag = g)
        }
        KachelKarte(zustand, KachelEbene.STANDARD, Modifier.fillMaxSize(), pins = pins, onPinTap = { p ->
            when (val t = p.tag) {
                is de.bgghome.webtrees.nativ.api.PlaceSummary -> onWahl(t.name)
                // Gruppe: hineinzoomen, bis sie zerfaellt
                is List<*> -> zustand.setze(p.lat, p.lon, (zustand.zoom + 2).coerceAtMost(KachelEbene.STANDARD.maxZoom))
            }
        })
        // Oben: wie viele Orte fehlen
        val ohne = orte.size - mit.size
        if (ohne > 0) Text(stringResource(Res.string.desk_places_without_coords, ohne, orte.size),
            Modifier.align(Alignment.TopStart).padding(8.dp).background(farben.surface.copy(alpha = 0.92f), MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = farben.onSurfaceVariant)
        // Unten rechts: Legende
        Column(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(farben.surface.copy(alpha = 0.92f), MaterialTheme.shapes.small).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(Res.string.desk_places_legend), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            listOf(farbeWenig to "1–19", farbeMittel to "20–99", farbeViel to "100+").forEach { (f, t) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(f, androidx.compose.foundation.shape.CircleShape))
                    Text(t, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(farbeGruppe, androidx.compose.foundation.shape.CircleShape))
                Text(stringResource(Res.string.desk_places_legend_cluster), Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        // Unten links: der gewaehlte Ort mit Knopf zur Ansicht
        mit.firstOrNull { it.name.equals(gewaehlt, ignoreCase = true) }?.let { o ->
            Row(Modifier.align(Alignment.BottomStart).padding(8.dp).background(farben.surface, MaterialTheme.shapes.small)
                .border(1.dp, farben.outlineVariant, MaterialTheme.shapes.small).padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.widthIn(max = 360.dp)) {
                    Text(o.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${o.events} " + stringResource(Res.string.desk_place_events) + " · ${o.individuals} " + stringResource(Res.string.desk_col_persons),
                        style = MaterialTheme.typography.labelSmall, color = farben.onSurfaceVariant)
                }
                androidx.compose.material3.TextButton(onClick = { onAnzeigen(o.name) }) { Text(stringResource(Res.string.desk_places_show)) }
            }
        }
    }
}
