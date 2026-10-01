package de.bgghome.webtrees.nativ.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.Sprache
import de.bgghome.webtrees.nativ.res.Res
import de.bgghome.webtrees.nativ.res.action_close
import de.bgghome.webtrees.nativ.res.language_system
import de.bgghome.webtrees.nativ.res.menu_language
import org.jetbrains.compose.resources.stringResource

/** Sprache waehlen (Handy): „wie das System“ oder eine der uebersetzten; wirkt sofort, der Dialog bleibt zum Vergleichen offen. */
@Composable
fun SpracheDialog(onDismiss: () -> Unit, onChosen: () -> Unit) {
    val gewaehlt = Sprache.wahl.value
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.menu_language)) },
        text = {
            Column {
                Wahl(stringResource(Res.string.language_system), gewaehlt == null) { Sprache.setzen(null); onChosen() }
                Sprache.ALLE.forEach { (code, name) -> Wahl(name, gewaehlt == code) { Sprache.setzen(code); onChosen() } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_close)) } },
    )
}

@Composable
private fun Wahl(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}
