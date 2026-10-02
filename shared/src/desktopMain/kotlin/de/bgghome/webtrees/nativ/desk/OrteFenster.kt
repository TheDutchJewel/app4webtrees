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
    val liste by produceState<Result<PlaceSummaryList>?>(null, tree) {
        value = null
        if (tree != null) value = withContext(Dispatchers.IO) { runCatching { viewModel.client.placeList(tree) } }
    }
    var suche by remember { mutableStateOf("") }
    var gewaehlt by remember(start) { mutableStateOf(start?.takeIf { it.isNotEmpty() }) }
    val detail by produceState<Result<PlaceDetail>?>(null, tree, gewaehlt) {
        value = null
        val n = gewaehlt
        if (tree != null && n != null) value = withContext(Dispatchers.IO) { runCatching { viewModel.client.place(tree, n) } }
    }

    DialogWindow(
        onCloseRequest = onClose, title = stringResource(Res.string.desk_places_window) + " – " + state.tree?.title.orEmpty(),
        state = rememberDialogState(width = 1180.dp, height = 800.dp),
        onPreviewKeyEvent = { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) { onClose(); true } else false },
    ) {
        DeskTheme { OrteInhalt(liste, detail, gewaehlt, { gewaehlt = it }, suche, { suche = it }, viewModel, openWeb) }
    }
}

/** Der Inhalt des Fensters, ohne Fenster (testbar ohne Bildschirm). */
@Composable
internal fun OrteInhalt(
    liste: Result<PlaceSummaryList>?, detail: Result<PlaceDetail>?, gewaehlt: String?, onWahl: (String) -> Unit,
    suche: String, onSuche: (String) -> Unit, viewModel: AppViewModel?, openWeb: (String) -> Unit, reiterStart: Int = 0,
) {
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
            Text(stringResource(Res.string.desk_places_count, treffer.size, alle.size), Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // ── Der gewaehlte Ort ──
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when {
                gewaehlt == null -> Text(stringResource(Res.string.desk_places_choose), Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                detail == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                detail.exceptionOrNull() != null -> Text(detail.exceptionOrNull()?.message ?: "?", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                else -> OrtDetail(detail.getOrThrow(), viewModel, onWahl, openWeb, reiterStart)
            }
        }
    }
}

private enum class OrtReiter(val titel: StringResource) {
    Personen(Res.string.desk_place_tab_people), Daten(Res.string.desk_place_tab_data), Notizen(Res.string.desk_tab_notes),
    Quellen(Res.string.desk_sources_window), Medien(Res.string.tab_media), Koordinaten(Res.string.desk_place_tab_coords),
}

@Composable
private fun OrtDetail(o: PlaceDetail, viewModel: AppViewModel?, onWahl: (String) -> Unit, openWeb: (String) -> Unit, reiterStart: Int) {
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
            SelectionContainer(Modifier.weight(1f)) { Text(o.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
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
