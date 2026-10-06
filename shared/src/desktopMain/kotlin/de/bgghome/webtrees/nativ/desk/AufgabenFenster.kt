package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import de.bgghome.webtrees.nativ.api.TaskJson
import de.bgghome.webtrees.nativ.api.TaskRequest
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.UiState
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import de.bgghome.webtrees.nativ.ui.setRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Forschungsaufgaben (Ansicht > Aufgaben, Strg+7, ab API-Stufe 30): alle Aufgaben des Baums als Liste - Datum,
 * Person oder Familie, Text, Bearbeiter. Es sind die Forschungsaufgaben von webtrees (Feld _TODO), also dieselben,
 * die im Browser der Block "Forschungsaufgaben" zeigt. Neue Aufgabe fuer den Probanden, Bearbeiten, Erledigt (=
 * loeschen). Haken "nur faellige" laesst Wiedervorlagen mit Datum in der Zukunft weg.
 */

/** Was der Dialog braucht: Datensatz, Anzeigename, vorhandene Aufgabe (null = neu), Vorgabetext. */
class AufgabeZiel(val xref: String, val name: String, val vorhanden: TaskJson? = null, val vorgabe: String = "")

@Composable
fun AufgabenFenster(state: UiState, viewModel: AppViewModel, openSheet: (String) -> Unit, onClose: () -> Unit) {
    val tree = state.tree?.name ?: return
    var neu by remember { mutableIntStateOf(0) }
    var nurOffene by remember { mutableStateOf(true) }
    var dialog by remember { mutableStateOf<AufgabeZiel?>(null) }
    var meldung by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val farben = MaterialTheme.colorScheme

    val liste by produceState<Result<List<TaskJson>>?>(null, tree, neu, nurOffene) {
        value = null
        value = withContext(Dispatchers.IO) { runCatching { viewModel.client.tasks(tree, nurOffene).tasks } }
    }

    DialogWindow(
        onCloseRequest = onClose, title = stringResource(Res.string.desk_tasks_window) + " – " + state.tree?.title.orEmpty(),
        state = rememberDialogState(width = 900.dp, height = 600.dp),
        onPreviewKeyEvent = { e ->
            when {
                e.type == KeyEventType.KeyDown && e.key == Key.Escape -> { onClose(); true }
                e.type == KeyEventType.KeyDown && e.key == Key.F1 -> { Hilfe.oeffnen("hauptfenster"); true }
                else -> false
            }
        },
    ) {
        Surface(Modifier.fillMaxSize(), color = farben.surface) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = nurOffene, onCheckedChange = { nurOffene = it })
                    Text(stringResource(Res.string.desk_tasks_open_only), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    liste?.getOrNull()?.let { Text(stringResource(Res.string.desk_tasks_count, it.size), style = MaterialTheme.typography.labelMedium, color = farben.onSurfaceVariant) }
                }
                meldung?.let { Text(it, color = farben.error, style = MaterialTheme.typography.bodySmall) }
                Box(Modifier.weight(1f).fillMaxWidth().border(1.dp, farben.outlineVariant)) {
                    val l = liste
                    when {
                        l == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        l.isFailure -> Text(l.exceptionOrNull()?.message ?: "?", Modifier.padding(12.dp), color = farben.error)
                        l.getOrThrow().isEmpty() -> Text(stringResource(Res.string.desk_tasks_empty), Modifier.padding(16.dp), color = farben.onSurfaceVariant)
                        else -> {
                            val ls = rememberLazyListState()
                            LazyColumn(Modifier.fillMaxSize(), state = ls) {
                                items(l.getOrThrow(), key = { it.record + "|" + it.factId }) { t ->
                                    AufgabeZeile(t, state.root,
                                        onKlick = { viewModel.setRoot(t.record) }, onDoppel = { openSheet(t.record) },
                                        onBearbeiten = { dialog = AufgabeZiel(t.record, t.name, t) },
                                        onErledigt = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) { runCatching { viewModel.client.deleteFact(tree, t.record, t.factId) } }
                                                    .onSuccess { neu++ }.onFailure { meldung = it.message }
                                            }
                                        })
                                    HorizontalDivider(color = farben.outlineVariant)
                                }
                            }
                            ListenLeiste(ls)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val root = state.root
                    val name = state.detail?.person?.takeIf { it.xref == root }?.name ?: root.orEmpty()
                    Knopf(stringResource(Res.string.desk_task_new), root != null && state.tree?.canEdit == true) { dialog = AufgabeZiel(root!!, name) }
                    Knopf(stringResource(Res.string.desk_check_rerun), true) { neu++ }
                    Spacer(Modifier.weight(1f))
                    Knopf(stringResource(Res.string.action_close), true, onClose)
                }
            }
        }
        dialog?.let { z -> AufgabeDialog(z, viewModel, tree, onClose = { gespeichert -> dialog = null; if (gespeichert) { neu++; viewModel.setRoot(z.xref, remember = false) } }) }
    }
}

@Composable
private fun AufgabeZeile(t: TaskJson, root: String?, onKlick: () -> Unit, onDoppel: () -> Unit, onBearbeiten: () -> Unit, onErledigt: () -> Unit) {
    val farben = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().fokusRahmen().combinedClickable(onClick = onKlick, onDoubleClick = onDoppel)
            .background(if (t.record == root) farben.secondaryContainer.copy(alpha = 0.4f) else farben.surface)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(t.date?.text ?: "–", Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(t.text + if (t.pending) "  (" + stringResource(Res.string.desk_change_pending) + ")" else "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOf(t.name + " (" + t.record + ")", t.user).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onBearbeiten) { Text(stringResource(Res.string.desk_task_edit)) }
        TextButton(onClick = onErledigt) { Text(stringResource(Res.string.desk_task_done)) }
    }
}

/** Aufgabe anlegen oder aendern: Text, Datum (leer = heute), Bearbeiter, Notiz. */
@Composable
fun AufgabeDialog(ziel: AufgabeZiel, viewModel: AppViewModel, tree: String, onClose: (gespeichert: Boolean) -> Unit) {
    var text by remember { mutableStateOf(ziel.vorhanden?.text ?: ziel.vorgabe) }
    var datum by remember { mutableStateOf(ziel.vorhanden?.date?.gedcom?.ifBlank { null } ?: ziel.vorhanden?.date?.text.orEmpty()) }
    var bearbeiter by remember { mutableStateOf(ziel.vorhanden?.user.orEmpty()) }
    var notiz by remember { mutableStateOf(ziel.vorhanden?.note.orEmpty()) }
    var fehler by remember { mutableStateOf<String?>(null) }
    var laeuft by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    WtAlertDialog(
        onDismissRequest = { onClose(false) },
        title = { Text(stringResource(Res.string.desk_task_for, ziel.name)) },
        text = {
            Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.desk_task_text)) }, singleLine = false, minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(datum, { datum = it }, Modifier.weight(1f), label = { Text(stringResource(Res.string.desk_task_date)) }, singleLine = true)
                    OutlinedTextField(bearbeiter, { bearbeiter = it }, Modifier.weight(1f), label = { Text(stringResource(Res.string.desk_task_user)) }, singleLine = true)
                }
                OutlinedTextField(notiz, { notiz = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.desk_task_note)) }, minLines = 3)
                fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank() && !laeuft, onClick = {
                laeuft = true; fehler = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { viewModel.client.saveTask(tree, ziel.xref, TaskRequest(ziel.vorhanden?.factId, text.trim(), datum.trim().ifBlank { null }, bearbeiter.trim().ifBlank { null }, notiz.trim())) }
                    }.onSuccess { onClose(true) }.onFailure { fehler = it.message }
                    laeuft = false
                }
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = { onClose(false) }) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
