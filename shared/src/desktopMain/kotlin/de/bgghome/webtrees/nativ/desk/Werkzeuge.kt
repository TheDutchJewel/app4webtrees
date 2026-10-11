package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import de.bgghome.webtrees.nativ.data.Kalender
import de.bgghome.webtrees.nativ.data.Kirchenjahr
import de.bgghome.webtrees.nativ.res.*
import org.jetbrains.compose.resources.stringResource
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/*
 * Werkzeuge im Menue Extras: Datumsrechner (Abstand, Geburtsdatum aus Alter, ein Datum umrechnen) und Kirchenjahr
 * (Sonntage und Feste eines Jahres, Name -> Datum, Datum -> Name). Reine Rechnung auf diesem PC, kein Server noetig.
 */

/** Wochentag in der Programmsprache (1 = Montag). */
private fun wochentagName(n: Int): String = DayOfWeek.of(n).getDisplayName(TextStyle.FULL, Locale.getDefault())
private fun wochentage(): List<String> = (1..7).map(::wochentagName)

private fun kopieren(text: String) =
    runCatching { java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(text), null) }

/** Datum mit Wochentag und (bei julianisch) dem gregorianischen Gegenstueck. */
private fun beschreibung(d: Kalender.Datum): String {
    val wt = wochentagName(Kalender.wochentag(d.jd()))
    return if (d.julianisch) "$d ($wt) · ${Kalender.umrechnen(d)} greg." else "$d ($wt)"
}

@Composable
private fun DatumFeld(titel: String, text: String, julianisch: Boolean, onText: (String) -> Unit, onJulianisch: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(text, onText, label = { Text(titel) }, singleLine = true, modifier = Modifier.width(240.dp))
        Checkbox(julianisch, onJulianisch)
        Text(stringResource(Res.string.desk_date_julian), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Ueberschrift(text: String) =
    Text(text, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

@Composable
private fun Ergebnis(text: String?, fehler: Boolean = false) {
    if (text.isNullOrBlank()) return
    SelectionContainer {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = if (fehler) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ZahlFeld(titel: String, wert: String, onWert: (String) -> Unit) =
    OutlinedTextField(wert, { onWert(it.filter(Char::isDigit).take(4)) }, label = { Text(titel) }, singleLine = true, modifier = Modifier.width(110.dp))

@Composable
fun DatumsrechnerFenster(onClose: () -> Unit) {
    Window(onCloseRequest = onClose, title = stringResource(Res.string.desk_tool_date).removeSuffix(" …"), state = arbeitsfenster("fenster_datumsrechner", 720, 760),
        onPreviewKeyEvent = { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) { onClose(); true } else false }) {
        DeskTheme { DatumsrechnerInhalt(onClose) }
    }
}

/** Inhalt des Datumsrechners ohne Fensterrahmen. */
@Composable
fun DatumsrechnerInhalt(onClose: () -> Unit) {
    run {
        run {
            val ungueltig = stringResource(Res.string.desk_date_invalid)
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.desk_date_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // ── Abstand ──
                Ueberschrift(stringResource(Res.string.desk_date_diff))
                var a by remember { mutableStateOf("") }; var aJ by remember { mutableStateOf(false) }
                var b by remember { mutableStateOf("") }; var bJ by remember { mutableStateOf(false) }
                DatumFeld(stringResource(Res.string.desk_date_first), a, aJ, { a = it }, { aJ = it })
                DatumFeld(stringResource(Res.string.desk_date_second), b, bJ, { b = it }, { bJ = it })
                val da = Kalender.lesen(a, aJ); val db = Kalender.lesen(b, bJ)
                if (a.isNotBlank() && b.isNotBlank()) {
                    if (da == null || db == null) Ergebnis(ungueltig, true)
                    else {
                        val ab = Kalender.abstand(da, db)
                        Ergebnis((if (ab.negativ) "− " else "") + stringResource(Res.string.desk_date_result_diff, ab.jahre, ab.monate, ab.tage) +
                            " · " + stringResource(Res.string.desk_date_days_total, ab.tageGesamt))
                        Text(beschreibung(da) + "  →  " + beschreibung(db), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // ── Geburt aus Alter ──
                Ueberschrift(stringResource(Res.string.desk_date_birth))
                var tod by remember { mutableStateOf("") }; var todJ by remember { mutableStateOf(false) }
                var jj by remember { mutableStateOf("") }; var mm by remember { mutableStateOf("") }
                var ww by remember { mutableStateOf("") }; var tt by remember { mutableStateOf("") }
                DatumFeld(stringResource(Res.string.desk_date_death), tod, todJ, { tod = it }, { todJ = it })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZahlFeld(stringResource(Res.string.desk_date_years), jj) { jj = it }
                    ZahlFeld(stringResource(Res.string.desk_date_months), mm) { mm = it }
                    ZahlFeld(stringResource(Res.string.desk_date_weeks), ww) { ww = it }
                    ZahlFeld(stringResource(Res.string.desk_date_days), tt) { tt = it }
                }
                Text(stringResource(Res.string.desk_date_birth_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tod.isNotBlank()) {
                    val dt = Kalender.lesen(tod, todJ)
                    if (dt == null) Ergebnis(ungueltig, true)
                    else if (listOf(jj, mm, ww, tt).any { it.isNotBlank() }) {
                        val geb = Kalender.geburt(dt, jj.toIntOrNull() ?: 0, mm.toIntOrNull() ?: 0, ww.toIntOrNull() ?: 0, tt.toIntOrNull() ?: 0)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Ergebnis(stringResource(Res.string.desk_date_born) + " " + beschreibung(geb))
                            TextButton(onClick = { kopieren(geb.gedcom()) }) { Text(stringResource(Res.string.desk_date_copy)) }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // ── Ein Datum ──
                Ueberschrift(stringResource(Res.string.desk_date_single))
                var e by remember { mutableStateOf("") }; var eJ by remember { mutableStateOf(false) }
                DatumFeld(stringResource(Res.string.desk_date_date), e, eJ, { e = it }, { eJ = it })
                if (e.isNotBlank()) {
                    val d = Kalender.lesen(e, eJ)
                    if (d == null) Ergebnis(ungueltig, true)
                    else {
                        val anders = Kalender.umrechnen(d)
                        Feld(stringResource(Res.string.desk_date_weekday), wochentagName(Kalender.wochentag(d.jd())))
                        Feld(stringResource(Res.string.desk_date_other), "$anders " + stringResource(if (anders.julianisch) Res.string.desk_date_julian else Res.string.desk_date_gregorian_cal))
                        Feld(stringResource(Res.string.desk_date_jd), d.jd().toString())
                        Feld(stringResource(Res.string.desk_date_church), Kirchenjahr.benennen(d, wochentage = wochentage()).joinToString("\n").ifBlank { "–" })
                        TextButton(onClick = { kopieren(d.gedcom()) }) { Text(stringResource(Res.string.desk_date_copy)) }
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text(stringResource(Res.string.action_close)) }
                }
            }
        }
    }
}

@Composable
private fun Feld(titel: String, wert: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(titel, Modifier.width(200.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SelectionContainer { Text(wert, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun KirchenjahrFenster(onClose: () -> Unit) {
    Window(onCloseRequest = onClose, title = stringResource(Res.string.desk_tool_church).removeSuffix(" …"), state = arbeitsfenster("fenster_kirchenjahr", 900, 820),
        onPreviewKeyEvent = { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) { onClose(); true } else false }) {
        DeskTheme { KirchenjahrInhalt(onClose) }
    }
}

/** Inhalt des Kirchenjahr-Fensters ohne Fensterrahmen. */
@Composable
fun KirchenjahrInhalt(onClose: () -> Unit, startJahr: Int = java.time.LocalDate.now().year) {
    run {
        run {
            var jahrText by remember { mutableStateOf(startJahr.toString()) }
            var julianisch by remember { mutableStateOf(false) }
            var katholisch by remember { mutableStateOf(false) }
            var suche by remember { mutableStateOf("") }
            var frage by remember { mutableStateOf("") }
            val jahr = jahrText.toIntOrNull()?.takeIf { it in 300..4000 }
            val farben = MaterialTheme.colorScheme
            Column(Modifier.fillMaxSize().background(farben.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { jahr?.let { jahrText = (it - 1).toString() } }) { Text("◀") }
                    OutlinedTextField(jahrText, { jahrText = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(Res.string.desk_church_year)) },
                        singleLine = true, modifier = Modifier.width(110.dp))
                    TextButton(onClick = { jahr?.let { jahrText = (it + 1).toString() } }) { Text("▶") }
                    RadioButton(!julianisch, { julianisch = false }); Text(stringResource(Res.string.desk_date_gregorian_cal))
                    RadioButton(julianisch, { julianisch = true }); Text(stringResource(Res.string.desk_date_julian))
                    Box(Modifier.width(12.dp))
                    RadioButton(!katholisch, { katholisch = false }); Text(stringResource(Res.string.desk_church_protestant))
                    RadioButton(katholisch, { katholisch = true }); Text(stringResource(Res.string.desk_church_catholic))
                }
                if (jahr == null) return@Column
                val o = Kirchenjahr.ostern(jahr, julianisch)
                Text(stringResource(Res.string.desk_church_easter, beschreibung(o)), style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(suche, { suche = it }, label = { Text(stringResource(Res.string.desk_church_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (suche.isNotBlank()) {
                            val t = Kirchenjahr.suchen(suche, jahr, julianisch, katholisch)
                            if (t.isEmpty()) Text(stringResource(Res.string.desk_church_none), color = farben.onSurfaceVariant)
                            t.take(6).forEach { Ergebnis(beschreibung(it.datum) + " – " + it.name) }
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(frage, { frage = it }, label = { Text(stringResource(Res.string.desk_church_name_date)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (frage.isNotBlank()) {
                            val d = Kalender.lesen(frage, julianisch)
                            if (d == null) Ergebnis(stringResource(Res.string.desk_date_invalid), true)
                            else Kirchenjahr.benennen(d, katholisch, wochentage()).ifEmpty { listOf("–") }.forEach { Ergebnis(beschreibung(d) + ": " + it) }
                        }
                    }
                }
                HorizontalDivider(color = farben.outlineVariant)
                Text(stringResource(Res.string.desk_church_all), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val tage = Kirchenjahr.tage(jahr, julianisch, katholisch)
                val zustand = rememberLazyListState()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(Modifier.fillMaxSize(), state = zustand) {
                        items(tage) { t ->
                            val ostern = t.zuOstern == 0
                            ContextMenuArea(items = { listOf(ContextMenuItem(de.bgghome.webtrees.nativ.Texte.t(Res.string.desk_date_copy)) { kopieren(t.datum.gedcom()) }) }) {
                                Row(Modifier.fillMaxWidth().background(if (ostern) farben.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(t.datum.toString(), Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
                                    Text(wochentagName(Kalender.wochentag(t.datum.jd())), Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium, color = farben.onSurfaceVariant)
                                    Text(t.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = if (t.beweglich && Kalender.wochentag(t.datum.jd()) == 7) FontWeight.Medium else FontWeight.Normal)
                                    Text(t.latein, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (julianisch) Text(Kalender.umrechnen(t.datum).toString() + " greg.", Modifier.width(120.dp), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Box(Modifier.matchParentSize()) { androidx.compose.foundation.VerticalScrollbar(androidx.compose.foundation.rememberScrollbarAdapter(zustand), Modifier.align(Alignment.CenterEnd)) }
                }
                Text(stringResource(Res.string.desk_church_note), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                Row(Modifier.fillMaxWidth()) { Box(Modifier.weight(1f)); TextButton(onClick = onClose) { Text(stringResource(Res.string.action_close)) } }
            }
        }
    }
}
