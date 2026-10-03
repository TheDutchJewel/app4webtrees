package de.bgghome.webtrees.nativ.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
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
import org.jetbrains.compose.resources.stringResource

/**
 * Eine Person (am PC die im Mittelpunkt) als Startperson festlegen: fuer sich selbst (Standardperson unter "Mein Konto") oder,
 * als Verwalter, fuer alle (Standardperson des Stammbaums). Ohne Verwalterrecht gibt es nur die erste Wahl.
 */
@Composable
fun StartpersonDialog(state: UiState, viewModel: AppViewModel, onClose: () -> Unit, xref: String? = state.root) {
    if (xref == null) return
    val name = state.detail?.person?.takeIf { it.xref == xref }?.name ?: xref
    val verwalter = state.tree?.role == "manager"
    var fuerAlle by remember { mutableStateOf(false) }
    var laeuft by remember { mutableStateOf(false) }

    WtAlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.desk_start_person_title)) },
        text = {
            Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(Res.string.desk_start_person_text, name), style = MaterialTheme.typography.bodyMedium)
                if (verwalter) listOf(false to Res.string.desk_start_person_me, true to Res.string.desk_start_person_tree).forEach { (wert, text) ->
                    Row(Modifier.clickable { fuerAlle = wert }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(fuerAlle == wert, { fuerAlle = wert })
                        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // Eigene Standardperson wieder entfernen - dann gilt "Das bin ich" bzw. die des Stammbaums
                if (state.tree?.defaultXref.orEmpty().isNotEmpty()) TextButton(onClick = {
                    laeuft = true; viewModel.setStartPerson("", forTree = false, onDone = onClose)
                }, enabled = !laeuft) { Text(stringResource(Res.string.desk_start_person_clear)) }
            }
        },
        confirmButton = {
            TextButton(onClick = { laeuft = true; viewModel.setStartPerson(xref, fuerAlle, onDone = onClose) }, enabled = !laeuft) {
                Text(stringResource(Res.string.desk_start_person_set))
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

