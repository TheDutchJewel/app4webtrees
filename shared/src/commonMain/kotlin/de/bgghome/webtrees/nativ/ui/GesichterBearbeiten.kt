package de.bgghome.webtrees.nativ.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.api.Bereich
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.data.Gesichter
import de.bgghome.webtrees.nativ.res.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Gesichter bearbeiten mit dem Finger (Handy, Tablet): das Bild eingepasst, ohne Zoom und Drehung, damit die Rahmen sicher
 * sitzen. Einen Rahmen ziehen, dann zuordnen (Person aus dem Stammbaum, nur ein Name oder unbekannt); einen Rahmen antippen,
 * um ihn neu zuzuordnen oder zu entfernen. Gespeichert wird in der Bilddatei ueber die Route exif des Moduls Sammlungen
 * (ab Stufe 5, nur Verwalter) - derselbe Weg wie am PC.
 */

/** Ob dieser Nutzer die Gesichter eines Archivbilds bearbeiten darf. */
fun AppViewModel.kannGesichterBearbeiten(item: ViewerItem): Boolean {
    val a = uiState.value.archive ?: return false
    val e = item.entry ?: return false
    return a.api >= 5 && a.darfBereiche && e.istBild && e.pfad != null
}

/** Neue Gesichter in die Datei schreiben und den Eintrag im Betrachter und in der Sammlung austauschen. */
suspend fun AppViewModel.gesichterSpeichern(entry: ArchiveEntry, bereiche: List<Bereich>): ArchiveEntry {
    val tree = uiState.value.tree?.name ?: error("kein Stammbaum")
    val neu = withContext(Dispatchers.Default) { client.writeRegions(tree, entry.pfad.orEmpty(), bereiche) }.eintrag ?: entry.copy(bereiche = bereiche)
    uiState.update { st ->
        st.copy(
            collectionEntries = st.collectionEntries.map { if (it.pfad == neu.pfad) neu else it },
            viewer = st.viewer?.let { v -> v.copy(items = v.items.map { i -> if (i.entry?.pfad == neu.pfad) viewerItem(neu).copy(text = i.text) else i }) },
        )
    }
    return neu
}

@Composable
fun GesichterBearbeiten(entry: ArchiveEntry, viewModel: AppViewModel, onFertig: () -> Unit) {
    val bereiche = remember(entry.pfad) { mutableStateListOf<Bereich>().apply { addAll(entry.bereiche) } }
    var gewaehlt by remember { mutableStateOf<Int?>(null) }
    var zuordnen by remember { mutableStateOf<Int?>(null) }
    var zug by remember { mutableStateOf<Pair<Offset, Offset>?>(null) }
    var bildB by remember { mutableStateOf(entry.breite) }
    var bildH by remember { mutableStateOf(entry.hoehe) }
    var speichert by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val unbekannt = stringResource(Res.string.desk_faces_unknown)
    val gespeichert = stringResource(Res.string.desk_faces_saved)
    val gelb = Color(0xFFFFD54F)

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onFertig, enabled = !speichert) { Text(stringResource(Res.string.action_cancel), color = Color.White) }
            Text(stringResource(Res.string.desk_faces_window), Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.titleMedium)
            TextButton(enabled = !speichert, onClick = {
                speichert = true; fehler = null
                scope.launch {
                    runCatching { viewModel.gesichterSpeichern(entry, bereiche.toList()) }
                        .onSuccess { viewModel.uiState.update { st -> st.copy(message = gespeichert) }; onFertig() }
                        .onFailure { fehler = (it as? Exception)?.let(::explain) ?: it.message; speichert = false }
                }
            }) { Text(stringResource(Res.string.action_save), color = gelb) }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val dichte = LocalDensity.current.density
            val lage = Gesichter.eingepasst(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), bildB, bildH)
            AsyncImage(model = entry.vollbild ?: entry.original, contentDescription = entry.bildunterschrift, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                onState = { s -> if (s is coil3.compose.AsyncImagePainter.State.Success && (bildB <= 0 || bildH <= 0)) { val g = s.painter.intrinsicSize; bildB = g.width.toInt(); bildH = g.height.toInt() } })
            // Ziehen zeichnet einen neuen Rahmen
            Box(Modifier.fillMaxSize().pointerInput(lage) {
                detectDragGestures(
                    onDragStart = { zug = it to it },
                    onDrag = { c, _ -> zug = zug?.let { it.first to c.position } },
                    onDragCancel = { zug = null },
                    onDragEnd = {
                        val (a, z) = zug ?: return@detectDragGestures
                        zug = null
                        val (ax, ay) = Gesichter.normiert(lage, a.x, a.y)
                        val (bx, by) = Gesichter.normiert(lage, z.x, z.y)
                        Gesichter.gezogen(ax, ay, bx, by)?.let { neu -> bereiche.add(neu); gewaehlt = bereiche.lastIndex; zuordnen = bereiche.lastIndex }
                    },
                )
            })
            bereiche.forEachIndexed { i, b ->
                val l = lage.links + (b.x * lage.breite).toFloat(); val o = lage.oben + (b.y * lage.hoehe).toFloat()
                Box(Modifier.offset((l / dichte).dp, (o / dichte).dp).size(((b.w * lage.breite).toFloat() / dichte).dp, ((b.h * lage.hoehe).toFloat() / dichte).dp)
                    .border(if (gewaehlt == i) 2.5.dp else 1.5.dp, if (gewaehlt == i) gelb else Color.White.copy(alpha = 0.8f))
                    .clickable { gewaehlt = if (gewaehlt == i) null else i })
            }
            zug?.let { (a, e) ->
                Box(Modifier.offset((minOf(a.x, e.x) / dichte).dp, (minOf(a.y, e.y) / dichte).dp)
                    .size((kotlin.math.abs(e.x - a.x) / dichte).dp, (kotlin.math.abs(e.y - a.y) / dichte).dp).border(2.dp, gelb))
            }
        }
        // Unten: Hinweis oder der gewaehlte Rahmen mit seinen Handgriffen
        Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.8f)).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            val g = gewaehlt?.let { bereiche.getOrNull(it) }
            if (g == null) Text(stringResource(Res.string.desk_faces_mark_hint), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            else {
                Text(g.name.ifBlank { unbekannt } + if (g.xref == null && g.name.isNotBlank()) " · " + stringResource(Res.string.desk_faces_not_in_tree) else "",
                    color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { zuordnen = gewaehlt }) { Text(stringResource(Res.string.desk_faces_assign), color = gelb) }
                    TextButton(onClick = { bereiche.removeAt(gewaehlt!!); gewaehlt = null }) { Text(stringResource(Res.string.desk_faces_delete), color = Color.White) }
                }
            }
            fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }

    zuordnen?.let { i ->
        bereiche.getOrNull(i)?.let { b ->
            val tree = viewModel.state.value.tree?.name.orEmpty()
            PersonZuordnen(b.name, suche = { q -> viewModel.client.individuals(tree, q, 1).data.filter { !it.isPrivate } },
                onDismiss = { zuordnen = null },
                onWahl = { name, xref -> bereiche[i] = b.copy(name = name, xref = xref); zuordnen = null })
        } ?: run { zuordnen = null }
    }
}

/** Einem Rahmen eine Person zuordnen: Treffer aus dem Stammbaum, nur den Namen oder unbekannt. */
@Composable
fun PersonZuordnen(start: String, suche: suspend (String) -> List<Person>, onDismiss: () -> Unit, onWahl: (String, String?) -> Unit) {
    var text by remember { mutableStateOf(start) }
    var treffer by remember { mutableStateOf<List<Person>>(emptyList()) }
    LaunchedEffect(text) {
        val q = text.trim()
        if (q.length < 2) { treffer = emptyList(); return@LaunchedEffect }
        delay(300)
        treffer = withContext(Dispatchers.Default) { runCatching { suche(q) }.getOrDefault(emptyList()) }
    }
    WtAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.desk_faces_assign).removeSuffix(" …")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(text, { text = it }, label = { Text(stringResource(Res.string.desk_faces_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 260.dp)) {
                    items(treffer, key = { it.xref }) { p ->
                        Column(Modifier.fillMaxWidth().clickable { onWahl(p.name.replace("/", "").trim(), p.xref) }.padding(vertical = 8.dp)) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOf(p.lifespan, p.xref).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
