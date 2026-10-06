package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.api.IndividualDetail
import de.bgghome.webtrees.nativ.api.LinkRequest
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.ChipRow
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import de.bgghome.webtrees.nativ.ui.linkRelative
import de.bgghome.webtrees.nativ.ui.relationLabel
import de.bgghome.webtrees.nativ.ui.select
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * Vorhandene Person anfuegen: eine Person, die es im Baum schon gibt, wird Vater, Mutter, Partner oder Kind von [ziel] -
 * ohne neuen Datensatz (Route Link). Aufruf aus den Rechtsklick-Menues und aus "Person hinzufuegen" der Familienansicht.
 */
object Anfuegewahl {
    var person: Person? by mutableStateOf(null)
}

@Composable
fun AnfuegenDialog(ziel: Person, viewModel: AppViewModel, tree: String, onClose: () -> Unit) {
    val detail by produceState<IndividualDetail?>(null, ziel.xref) {
        value = withContext(Dispatchers.IO) { runCatching { viewModel.client.individual(tree, ziel.xref) }.getOrNull() }
    }
    val eltern = detail?.parentFamilies?.firstOrNull()
    val beziehungen = buildList {
        add("child"); add("spouse")
        if (detail != null && eltern?.husband == null) add("father")
        if (detail != null && eltern?.wife == null) add("mother")
    }
    var beziehung by remember { mutableStateOf("child") }
    var familie by remember { mutableStateOf<String?>(null) }
    var gewaehlt by remember { mutableStateOf<Person?>(null) }
    val familien = detail?.spouseFamilies.orEmpty()
    WtAlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.desk_link_title, ziel.name)) },
        text = {
            Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChipRow(beziehungen.map { it to relationLabel(it) }, beziehung) { beziehung = it }
                if (beziehung == "child" && familien.size > 1) {
                    Text(stringResource(Res.string.relative_child_of), style = MaterialTheme.typography.labelMedium)
                    ChipRow(familien.map { it.xref to (it.spouse?.name ?: stringResource(Res.string.unknown_person)) }, familie ?: familien.first().xref) { familie = it }
                }
                Text(stringResource(Res.string.desk_link_who), style = MaterialTheme.typography.labelLarge)
                PersonSuche(gewaehlt, { gewaehlt = it }) { q -> runCatching { viewModel.client.individuals(tree, q, 1).data }.getOrNull().orEmpty().filter { it.xref != ziel.xref } }
                Text(stringResource(Res.string.desk_link_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(enabled = gewaehlt != null, onClick = {
                val p = gewaehlt ?: return@TextButton
                viewModel.select(ziel.xref)
                viewModel.linkRelative(LinkRequest(p.xref, beziehung, ziel.xref, (familie ?: familien.firstOrNull()?.xref).takeIf { beziehung == "child" }))
                onClose()
            }) { Text(stringResource(Res.string.desk_link_do)) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
