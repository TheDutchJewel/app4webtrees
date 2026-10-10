package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.api.Bereich
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.data.Gesichter
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.ConfirmDialog
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import de.bgghome.webtrees.nativ.ui.setRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Gesichter auf einem Archivbild (Modul Sammlungen ab Stufe 5): das Bild gross, darueber die markierten Rahmen. Beim
 * Darueberfahren steht der Name am Rahmen, rechts die Liste in Lesereihenfolge (Reihe fuer Reihe, von links). Wer darf
 * ([bearbeiten]), zieht neue Rahmen auf und ordnet sie einer Person des Stammbaums zu, einem Namen ohne Datensatz (der
 * Trauzeuge, den niemand angelegt hat) oder "unbekannt". Gespeichert wird in der Datei (XMP) ueber die Route des Moduls;
 * an die Person haengt sich das Bild dabei nicht.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GesichterFenster(
    eintrag: ArchiveEntry, tree: String, viewModel: AppViewModel, bearbeiten: Boolean,
    onGespeichert: (ArchiveEntry) -> Unit, onClose: () -> Unit,
) {
    val bereiche = remember(eintrag.pfad) { mutableStateListOf<Bereich>().apply { addAll(eintrag.bereiche) } }
    var geaendert by remember { mutableStateOf(false) }
    var ueber by remember { mutableStateOf<Int?>(null) }
    var gewaehlt by remember { mutableStateOf<Int?>(null) }
    var markieren by remember { mutableStateOf(eintrag.bereiche.isEmpty() && bearbeiten) }
    var zug by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var zuordnen by remember { mutableStateOf<Int?>(null) }
    var verwerfen by remember { mutableStateOf(false) }
    var speichert by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    // Bildmasse: aus dem Eintrag, sonst vom geladenen Bild
    var bildB by remember { mutableStateOf(eintrag.breite) }
    var bildH by remember { mutableStateOf(eintrag.hoehe) }
    val scope = rememberCoroutineScope()
    val schliessen = { if (geaendert) verwerfen = true else onClose() }
    val unbekannt = stringResource(Res.string.desk_faces_unknown)
    fun beschriftung(b: Bereich) = b.name.ifBlank { unbekannt }

    DialogWindow(
        onCloseRequest = schliessen,
        title = stringResource(Res.string.desk_faces_window) + " – " + eintrag.bildunterschrift.ifEmpty { eintrag.datei },
        state = rememberDialogState(width = 1300.dp, height = 860.dp),
        onPreviewKeyEvent = { e ->
            when {
                e.type != KeyEventType.KeyDown -> false
                e.key == Key.Escape && zuordnen == null -> { schliessen(); true }
                e.key == Key.F1 -> { Hilfe.oeffnen("archiv"); true }
                (e.key == Key.Delete || e.key == Key.Backspace) && bearbeiten && gewaehlt != null && zuordnen == null -> {
                    bereiche.removeAt(gewaehlt!!); gewaehlt = null; ueber = null; geaendert = true; true
                }
                else -> false
            }
        },
    ) {
        DeskTheme {
            val farben = MaterialTheme.colorScheme
            Row(Modifier.fillMaxSize().background(farben.background)) {
                // ── Bild mit Rahmen ──
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF202020))) {
                    val dichte = LocalDensity.current.density
                    val feldB = constraints.maxWidth.toFloat()
                    val feldH = constraints.maxHeight.toFloat()
                    val lage = Gesichter.eingepasst(feldB, feldH, bildB, bildH)
                    AsyncImage(
                        model = eintrag.vollbild ?: eintrag.original, contentDescription = eintrag.bildunterschrift,
                        contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                        onState = { s ->
                            if (s is AsyncImagePainter.State.Success && (bildB <= 0 || bildH <= 0)) {
                                val g = s.painter.intrinsicSize
                                bildB = g.width.toInt(); bildH = g.height.toInt()
                            }
                        },
                    )
                    // Die Rahmen; Positionen in Pixeln des Feldes, fuer offset/size in dp umgerechnet
                    bereiche.forEachIndexed { i, b ->
                        val hervor = i == ueber || i == gewaehlt
                        val links = (lage.links + b.x * lage.breite).toFloat()
                        val oben = (lage.oben + b.y * lage.hoehe).toFloat()
                        val breite = (b.w * lage.breite).toFloat()
                        val hoehe = (b.h * lage.hoehe).toFloat()
                        Box(Modifier.offset((links / dichte).dp, (oben / dichte).dp).size((breite / dichte).dp, (hoehe / dichte).dp)
                            .border(if (hervor) 2.5.dp else 1.dp, when {
                                hervor -> Color(0xFFFFD54F)
                                b.xref == null && b.name.isBlank() -> Color.White.copy(alpha = 0.45f)
                                else -> Color.White.copy(alpha = 0.75f)
                            }))
                        if (hervor) Surface(Modifier.offset((links / dichte).dp, ((oben + hoehe) / dichte).dp + 2.dp), color = Color.Black.copy(alpha = 0.78f),
                            shape = MaterialTheme.shapes.extraSmall) {
                            Text(beschriftung(b) + if (b.xref == null && b.name.isNotBlank()) " · " + stringResource(Res.string.desk_faces_not_in_tree) else "",
                                Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    zug?.let { (a, z) ->
                        Box(Modifier.offset((minOf(a.x, z.x) / dichte).dp, (minOf(a.y, z.y) / dichte).dp)
                            .size((kotlin.math.abs(z.x - a.x) / dichte).dp, (kotlin.math.abs(z.y - a.y) / dichte).dp)
                            .border(2.dp, Color(0xFFFFD54F)))
                    }
                    // Maus: Darueberfahren zeigt den Namen, Klick waehlt, im Markiermodus zieht ein Zug einen Rahmen
                    Box(Modifier.fillMaxSize()
                        .onPointerEvent(PointerEventType.Move) { e ->
                            val p = e.changes.first().position
                            val (nx, ny) = Gesichter.normiert(lage, p.x, p.y)
                            ueber = Gesichter.treffer(bereiche, nx, ny)
                        }
                        .onPointerEvent(PointerEventType.Exit) { ueber = null }
                        .onPointerEvent(PointerEventType.Release) { e ->
                            if (zug != null) return@onPointerEvent
                            val p = e.changes.first().position
                            val (nx, ny) = Gesichter.normiert(lage, p.x, p.y)
                            gewaehlt = Gesichter.treffer(bereiche, nx, ny)
                        }
                        .then(if (bearbeiten && markieren) Modifier.pointerInput(lage) {
                            detectDragGestures(
                                onDragStart = { zug = it to it },
                                onDrag = { c, _ -> zug = zug?.let { it.first to c.position } },
                                onDragCancel = { zug = null },
                                onDragEnd = {
                                    val (a, z) = zug ?: return@detectDragGestures
                                    zug = null
                                    val (ax, ay) = Gesichter.normiert(lage, a.x, a.y)
                                    val (bx, by) = Gesichter.normiert(lage, z.x, z.y)
                                    Gesichter.gezogen(ax, ay, bx, by)?.let { neu ->
                                        bereiche.add(neu); geaendert = true
                                        gewaehlt = bereiche.lastIndex; zuordnen = bereiche.lastIndex
                                    }
                                },
                            )
                        } else Modifier))
                }

                // ── Liste und Knoepfe ──
                Column(Modifier.width(320.dp).fillMaxHeight().background(farben.surface).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (bereiche.isEmpty()) stringResource(Res.string.desk_faces_none) else stringResource(Res.string.desk_faces_count, bereiche.size),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (bearbeiten) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Haken(markieren) { markieren = it }
                        Text(stringResource(Res.string.desk_faces_mark), style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(stringResource(if (bearbeiten && markieren) Res.string.desk_faces_mark_hint else Res.string.desk_faces_hover_hint),
                        style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                    HorizontalDivider(color = farben.outlineVariant)
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        items(Gesichter.reihenfolge(bereiche)) { i ->
                            val b = bereiche[i]
                            Row(Modifier.fillMaxWidth()
                                .background(if (i == gewaehlt || i == ueber) farben.secondaryContainer else Color.Transparent)
                                .onPointerEvent(PointerEventType.Enter) { ueber = i }
                                .onPointerEvent(PointerEventType.Exit) { if (ueber == i) ueber = null }
                                .combinedClickable(onClick = { gewaehlt = i; b.xref?.let { viewModel.setRoot(it) } }, onDoubleClick = { if (bearbeiten) zuordnen = i })
                                .padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(beschriftung(b), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        fontStyle = if (b.name.isBlank()) FontStyle.Italic else FontStyle.Normal,
                                        color = if (b.xref != null) farben.primary else farben.onSurface)
                                    if (b.xref == null && b.name.isNotBlank()) Text(stringResource(Res.string.desk_faces_not_in_tree), style = MaterialTheme.typography.labelSmall, color = farben.onSurfaceVariant)
                                }
                                b.xref?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = farben.onSurfaceVariant) }
                            }
                        }
                    }
                    if (bearbeiten && gewaehlt != null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { zuordnen = gewaehlt }, shape = MaterialTheme.shapes.small) { Text(stringResource(Res.string.desk_faces_assign)) }
                        OutlinedButton(onClick = { bereiche.removeAt(gewaehlt!!); gewaehlt = null; ueber = null; geaendert = true }, shape = MaterialTheme.shapes.small) {
                            Text(stringResource(Res.string.desk_faces_delete))
                        }
                    }
                    if (bearbeiten) Text(stringResource(Res.string.desk_faces_note), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                    fehler?.let { Text(it, color = farben.error, style = MaterialTheme.typography.bodySmall) }
                    HorizontalDivider(color = farben.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = schliessen) { Text(stringResource(Res.string.action_close)) }
                        if (bearbeiten) OutlinedButton(enabled = geaendert && !speichert, shape = MaterialTheme.shapes.small, onClick = {
                            val pfad = eintrag.pfad ?: return@OutlinedButton
                            speichert = true; fehler = null
                            scope.launch {
                                runCatching { withContext(Dispatchers.IO) { viewModel.client.writeRegions(tree, pfad, bereiche.toList()) } }
                                    .onSuccess { r -> geaendert = false; onGespeichert(r.eintrag ?: eintrag.copy(bereiche = bereiche.toList())) }
                                    .onFailure { fehler = it.message ?: "?" }
                                speichert = false
                            }
                        }) { Text(stringResource(Res.string.action_save)) }
                    }
                }
            }

            zuordnen?.let { i ->
                bereiche.getOrNull(i)?.let { b ->
                    ZuordnenDialog(b, tree, viewModel,
                        onDismiss = {
                            // Ein frisch gezogener Rahmen ohne Zuordnung bleibt als "unbekannt" stehen - loeschen geht jederzeit
                            zuordnen = null
                        },
                        onWahl = { name, xref -> bereiche[i] = b.copy(name = name, xref = xref); geaendert = true; zuordnen = null })
                } ?: run { zuordnen = null }
            }
            if (verwerfen) ConfirmDialog(
                title = stringResource(Res.string.desk_faces_discard_title), text = stringResource(Res.string.desk_faces_discard_text),
                confirm = stringResource(Res.string.desk_faces_discard), onDismiss = { verwerfen = false }, onConfirm = { verwerfen = false; onClose() },
            )
        }
    }
}

/** Kaestchen ohne Material-Checkbox-Abstaende (wie in den Einstellungsspalten). */
@Composable
private fun Haken(an: Boolean, onWechsel: (Boolean) -> Unit) =
    androidx.compose.material3.Checkbox(checked = an, onCheckedChange = onWechsel)

/**
 * Einem Rahmen eine Person zuordnen: Personen des Stammbaums zum getippten Text (Name aus webtrees, mit Lebensdaten), den
 * getippten Namen ohne Datensatz uebernehmen oder "unbekannt".
 */
@Composable
private fun ZuordnenDialog(b: Bereich, tree: String, viewModel: AppViewModel, onDismiss: () -> Unit, onWahl: (String, String?) -> Unit) {
    var text by remember { mutableStateOf(b.name) }
    var treffer by remember { mutableStateOf<List<Person>>(emptyList()) }
    LaunchedEffect(text) {
        val q = text.trim()
        if (q.length < 2) { treffer = emptyList(); return@LaunchedEffect }
        delay(250)
        treffer = withContext(Dispatchers.IO) { runCatching { viewModel.client.individuals(tree, q, 1).data.filter { !it.isPrivate } }.getOrDefault(emptyList()) }
    }
    WtAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.desk_faces_assign).removeSuffix(" …")) },
        text = {
            Column(Modifier.width(460.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text(stringResource(Res.string.desk_faces_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Box(Modifier.fillMaxWidth().height(260.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(treffer, key = { it.xref }) { p ->
                            Row(Modifier.fillMaxWidth().clickable { onWahl(p.name.replace("/", "").trim(), p.xref) }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOf(p.lifespan, p.xref).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (text.isNotBlank()) TextButton(onClick = { onWahl(text.trim(), null) }) { Text(stringResource(Res.string.desk_faces_use_name, text.trim())) }
                TextButton(onClick = { onWahl("", null) }) { Text(stringResource(Res.string.desk_faces_set_unknown)) }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
