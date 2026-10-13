package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.bgghome.webtrees.nativ.data.Laender
import de.bgghome.webtrees.nativ.data.Ortsanzeige
import de.bgghome.webtrees.nativ.res.*
import org.jetbrains.compose.resources.stringResource

/** Ansicht › Ortsanzeige … (Issue 18): Form der Ortsnamen und Heimatland, mit Beispiel. */
@Composable
fun OrtsanzeigeDialog(onDismiss: () -> Unit) {
    var form by remember { mutableStateOf(Ortsanzeige.form.value) }
    var heimat by remember { mutableStateOf(Ortsanzeige.heimat.value) }
    val beispielVoll = "Amsterdam, Noord-Holland, Nederland"
    val beispielFern = "Walferdange, Luxembourg"
    // Vorschau mit den Werten des Dialogs, ohne die Einstellung schon zu aendern
    fun vorschau(name: String) = Ortsanzeige.text(name, form, Laender.iso2(heimat).orEmpty())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.place_display_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.place_display_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                listOf(
                    Ortsanzeige.Form.Voll to Res.string.place_format_full, Ortsanzeige.Form.OrtLand to Res.string.place_format_city_country,
                    Ortsanzeige.Form.OrtIso2 to Res.string.place_format_city_iso2, Ortsanzeige.Form.OrtIso3 to Res.string.place_format_city_iso3,
                ).forEach { (f, r) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = form == f, onClick = { form = f })
                        Text(stringResource(r), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val erkannt = Laender.iso2(heimat)
                OutlinedTextField(heimat, { heimat = it }, label = { Text(stringResource(Res.string.place_home_country)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(if (heimat.isBlank()) stringResource(Res.string.place_home_country_hint) else erkannt?.let { "$it – ${Ortsanzeige.landName(it)}" } ?: "?") },
                    isError = heimat.isNotBlank() && erkannt == null)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.place_display_example) + ":", style = MaterialTheme.typography.labelMedium)
                Text("$beispielVoll → ${vorschau(beispielVoll)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
                Text("$beispielFern → ${vorschau(beispielFern)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = { Ortsanzeige.setzen(form, heimat); onDismiss() }) { Text(stringResource(Res.string.desk_place_take)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
