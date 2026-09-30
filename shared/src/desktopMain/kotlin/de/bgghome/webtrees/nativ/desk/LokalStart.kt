package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.lokal.LokalBetrieb
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppRoot
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.LocalAppName
import de.bgghome.webtrees.nativ.ui.login
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * Erster Start am PC (Stufe 4): zwei Wege nebeneinander - links mit einem webtrees verbinden (der Bildschirm der
 * App wie bisher), rechts einen neuen Stammbaum auf diesem PC anlegen. Danach sieht man keinen Server und kein Passwort.
 */
@Composable
fun DeskStart(viewModel: AppViewModel) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxHeight()) { AppRoot(viewModel) }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.weight(1f).fillMaxHeight()) { LokalAnlegen(viewModel) }
    }
}

@Composable
private fun LokalAnlegen(viewModel: AppViewModel) {
    val vorgabe = stringResource(Res.string.lokal_title_default)
    var titel by remember { mutableStateOf(vorgabe) }
    var schritt by remember { mutableStateOf<String?>(null) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val appName = LocalAppName.current

    // Anlegen, leer oder aus einer GEDCOM-Datei; blockiert (Auspacken, Import), also im Hintergrund.
    fun anlegen(gedcom: java.io.File?) {
        fehler = null
        schritt = "…"
        // Beim Uebernehmen heisst der Baum wie die Datei, solange der Name nicht von Hand geaendert wurde.
        val name = titel.trim().takeUnless { it.isEmpty() || (gedcom != null && it == vorgabe) }
            ?: gedcom?.nameWithoutExtension ?: vorgabe
        scope.launch {
            runCatching {
                val (adresse, zugang) = withContext(Dispatchers.IO) { LokalBetrieb.anlegen(name, gedcom) { schritt = it } }
                viewModel.client.baseUrl = adresse
                viewModel.settings.baseUrl = viewModel.client.baseUrl
                // Gleich den eben angelegten Baum oeffnen, auch wenn schon andere da sind
                viewModel.settings.tree = zugang.baum
                viewModel.login(zugang.benutzer, zugang.passwort)
            }.onFailure { fehler = it.message ?: it.toString() }
            schritt = null
        }
    }
    val dialogTitel = stringResource(Res.string.lokal_gedcom_choose)

    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 460.dp).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.lokal_heading), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(Res.string.lokal_text, appName),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = titel, onValueChange = { titel = it }, singleLine = true, enabled = schritt == null,
                    label = { Text(stringResource(Res.string.lokal_title_label)) }, modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { anlegen(null) },
                    enabled = schritt == null && titel.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.lokal_create)) }
                OutlinedButton(
                    onClick = { gedcomWaehlen(dialogTitel)?.let(::anlegen) },
                    enabled = schritt == null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.lokal_gedcom)) }
                Text(
                    stringResource(Res.string.lokal_gedcom_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                schritt?.let {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.padding(2.dp).widthIn(max = 18.dp), strokeWidth = 2.dp)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                fehler?.let {
                    // Verstaendlich oben, der rohe Text klein darunter (fuer die Fehlermeldung an uns)
                    Text(stringResource(Res.string.lokal_error, LokalBetrieb.protokoll), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Text(it.take(1500), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    stringResource(Res.string.lokal_hint),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** GEDCOM-Datei aus dem bisherigen Programm waehlen (.ged, unter Windows auch .GED). */
private fun gedcomWaehlen(titel: String): java.io.File? {
    val d = java.awt.FileDialog(null as java.awt.Frame?, titel, java.awt.FileDialog.LOAD).apply {
        setFilenameFilter { _, name -> name.endsWith(".ged", ignoreCase = true) }
        file = "*.ged"
        isVisible = true
    }
    return d.file?.let { java.io.File(d.directory, it) }?.takeIf { it.isFile }
}
