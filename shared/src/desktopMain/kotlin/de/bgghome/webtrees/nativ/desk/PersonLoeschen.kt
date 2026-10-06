package de.bgghome.webtrees.nativ.desk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.ConfirmDialog
import de.bgghome.webtrees.nativ.ui.deletePerson
import org.jetbrains.compose.resources.stringResource

/**
 * "Person loeschen ..." aus Menue und Rechtsklick-Menues (Navigator, Familienansicht, Personentabelle): die Menues setzen
 * nur die Person, die Rueckfrage zeichnet das Hauptfenster - dieselbe wie unten im Personenblatt.
 */
object Loeschwahl {
    /** Kennung und Name der Person, nach der gefragt wird; null = keine Rueckfrage offen. */
    var person: Pair<String, String>? by mutableStateOf(null)
}

@Composable
fun LoeschRueckfrage(viewModel: AppViewModel) {
    val (xref, name) = Loeschwahl.person ?: return
    ConfirmDialog(
        title = stringResource(Res.string.delete_person_title, name),
        text = stringResource(Res.string.delete_person_text),
        confirm = stringResource(Res.string.action_delete),
        onDismiss = { Loeschwahl.person = null },
        onConfirm = { Loeschwahl.person = null; viewModel.deletePerson(xref) },
    )
}
