package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.clickable
import de.bgghome.webtrees.nativ.ui.setRoot
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.UiState
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import de.bgghome.webtrees.nativ.ui.setStartPerson
import org.jetbrains.compose.resources.stringResource

/**
 * Stammbaum auf diesem PC: Ist fuer einen geoeffneten Baum noch keine Startperson festgelegt (weder die eigene noch
 * die des Stammbaums), fragt das Programm einmal danach - sonst beginnt es mit der ersten Person der GEDCOM-Datei.
 * Die Wahl wird als Standardperson des Stammbaums gespeichert; "Spaeter" fragt fuer diesen Baum nicht wieder.
 */
@Composable
internal fun StartpersonFrage(state: UiState, viewModel: AppViewModel, onClose: () -> Unit) {
    val tree = state.tree ?: return
    var suche by remember { mutableStateOf("") }
    val treffer by androidx.compose.runtime.produceState(emptyList<de.bgghome.webtrees.nativ.api.Person>(), suche) {
        val q = suche.trim()
        if (q.length < 2) { value = emptyList(); return@produceState }
        kotlinx.coroutines.delay(250)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { viewModel.client.individuals(tree.name, q, 1).data.filter { !it.isPrivate }.take(12) }.getOrDefault(emptyList())
        }
    }
    fun nehmen(xref: String) {
        viewModel.setStartPerson(xref, forTree = true) { viewModel.setRoot(xref) }
        onClose()
    }
    val aktuell = state.detail?.person?.takeIf { it.xref == state.root && !it.isPrivate }

    WtAlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.desk_start_ask_title)) },
        text = {
            Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.desk_start_ask_text), style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.OutlinedTextField(suche, { suche = it }, Modifier.width(560.dp), singleLine = true,
                    label = { Text(stringResource(Res.string.desk_start_ask_search)) })
                Column(Modifier.height(260.dp).verticalScroll(rememberScrollState())) {
                    treffer.forEach { p ->
                        Row(Modifier.clickable { nehmen(p.xref) }.padding(vertical = 5.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(registerName(p, "", p.xref), Modifier.width(380.dp), style = MaterialTheme.typography.bodyMedium)
                            Text(jahre(p), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            aktuell?.let { p -> TextButton(onClick = { nehmen(p.xref) }) { Text(stringResource(Res.string.desk_start_ask_current, registerName(p, "", p.xref))) } }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(Res.string.desk_start_ask_later)) } },
    )
}
