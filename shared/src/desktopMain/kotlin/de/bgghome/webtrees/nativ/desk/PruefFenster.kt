package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.TreeExport
import de.bgghome.webtrees.nativ.pruefung.Einheit
import de.bgghome.webtrees.nativ.pruefung.PruefErgebnis
import de.bgghome.webtrees.nativ.pruefung.Regel
import de.bgghome.webtrees.nativ.pruefung.RegelGruppe
import de.bgghome.webtrees.nativ.pruefung.Regelkatalog
import de.bgghome.webtrees.nativ.pruefung.Schwere
import de.bgghome.webtrees.nativ.pruefung.Treffer
import de.bgghome.webtrees.nativ.pruefung.pruefen
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.LocalAppName
import de.bgghome.webtrees.nativ.ui.UiState
import de.bgghome.webtrees.nativ.ui.setRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Fenster "Plausibilitaetspruefung" (27.09.2026): links die Regeln (an/aus, Trefferzahl, Grenzwert), rechts die
 * Treffer der gewaehlten Regel oder alle. Klick zeigt die Person im Hauptfenster, Doppelklick oeffnet ihr
 * Personenblatt. Geprueft wird der ganze Baum aus dem Export (Zwischenspeicher, sonst Route Export) - ohne weitere
 * Anfragen, darum rechnet jede Aenderung eines Grenzwerts sofort neu.
 */

private object PruefWahl {
    private val prefs get() = DeskLayout.prefs
    fun aus(): Set<String> = prefs.getString("pruef_aus", null)?.split(',')?.filter(String::isNotBlank)?.toSet() ?: emptySet()
    fun aus(s: Set<String>) = prefs.putString("pruef_aus", s.sorted().joinToString(","))
    fun grenzwerte(): Map<String, Double> = Regelkatalog.alle.mapNotNull { r ->
        prefs.getString("pruef_g_${r.id}", null)?.toDoubleOrNull()?.let { r.id to it }
    }.toMap()
    fun grenzwert(id: String, wert: Double?) = prefs.putString("pruef_g_$id", wert?.toString())
}

private fun einheit(e: Einheit?, wert: Double = 2.0): String = when (e) {
    Einheit.Jahre -> Texte.t(if (wert == 1.0) Res.string.desk_check_unit_year else Res.string.desk_check_unit_years)
    Einheit.Monate -> Texte.t(if (wert == 1.0) Res.string.desk_check_unit_month else Res.string.desk_check_unit_months)
    else -> ""
}

private fun zahl(d: Double) = if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

private val deutsch get() = java.util.Locale.getDefault().language == "de"

private fun gruppenName(g: RegelGruppe) = when (g) {
    RegelGruppe.Chronologie -> Res.string.desk_check_group_chrono
    RegelGruppe.Alter -> Res.string.desk_check_group_age
    RegelGruppe.Struktur -> Res.string.desk_check_group_structure
}

/** Wer zu einem Treffer gehoert: die Person, sonst der erste Partner der Familie, sonst ein Kind. */
private fun trefferPerson(t: Treffer, b: TreeExport): String? =
    t.person ?: b.families[t.familie]?.let { f -> f.husband ?: f.wife ?: f.children.firstOrNull() }

/** Die Pruefung als Liste fuer Druck und PDF: je Regel mit Treffern eine Ueberschrift, darunter eine Tabelle. */
internal fun pruefZeilen(e: PruefErgebnis, b: TreeExport, baum: String, grenzwerte: Map<String, Double>): List<Zeile> = buildList {
    add(Zeile(Texte.t(Res.string.desk_check_title, baum), gross = true))
    val fehler = e.treffer.filterKeys { Regelkatalog.nachId[it]?.schwere == Schwere.Fehler }.values.sumOf { it.size }
    add(Zeile(Texte.t(Res.string.desk_check_summary, e.anzahl, fehler)))
    val anteile = listOf(0.34f, 0.16f, 0.50f)
    e.treffer.filterValues { it.isNotEmpty() }.forEach { (id, liste) ->
        val r = Regelkatalog.nachId.getValue(id)
        add(Zeile(""))
        val g = (grenzwerte[id] ?: r.grenzwert)?.let { "  [" + listOf(zahl(it), einheit(r.einheit, it)).filter(String::isNotBlank).joinToString(" ") + "]" }.orEmpty()
        add(Zeile("$id  ${r.frage(deutsch)}$g  –  ${schwereText(r.schwere)} (${liste.size})", fett = true))
        liste.forEach { t ->
            val p = b.person(trefferPerson(t, b))
            add(Zeile("", spalten = listOf(p?.let(::registerName) ?: t.familie.orEmpty(), p?.lifespan.orEmpty(), t.text), anteile = anteile))
        }
    }
}

private fun schwereText(s: Schwere) = Texte.t(if (s == Schwere.Fehler) Res.string.desk_check_error else Res.string.desk_check_warning)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PruefFenster(state: UiState, viewModel: AppViewModel, openSheet: (String) -> Unit, onClose: () -> Unit) {
    val appName = LocalAppName.current
    val baumTitel = state.tree?.title.orEmpty()
    var aus by remember { mutableStateOf(PruefWahl.aus()) }
    var grenzwerte by remember { mutableStateOf(PruefWahl.grenzwerte()) }
    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var neu by remember { mutableIntStateOf(0) }
    var fortschritt by remember { mutableStateOf(0 to 0) }

    // Der ganze Baum: aus dem Zwischenspeicher, solange sich auf dem Server nichts geaendert hat
    val baum by produceState<Result<TreeExport>?>(null, state.tree?.name, neu) {
        value = null
        val tree = state.tree ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                BaumSpeicher.holen(viewModel.client, tree.name, Int.MAX_VALUE / 64) { a, b -> fortschritt = a to b }
                    ?: throw IllegalStateException(Texte.t(Res.string.desk_list_needs_export))
            }
        }
    }
    val ergebnis by produceState<PruefErgebnis?>(null, baum, aus, grenzwerte) {
        val b = baum?.getOrNull() ?: run { value = null; return@produceState }
        value = withContext(Dispatchers.Default) { pruefen(b, grenzwerte, aus) }
    }
    LaunchedEffect(aus) { PruefWahl.aus(aus) }

    DialogWindow(
        onCloseRequest = onClose, title = stringResource(Res.string.desk_check_window),
        state = rememberDialogState(width = 1180.dp, height = 820.dp),
        onPreviewKeyEvent = { e -> if (e.key == Key.Escape) { onClose(); true } else false },
    ) {
        DeskTheme {
            Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // ── Regeln ──
                Column(Modifier.width(460.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    val liste = rememberLazyListState()
                    Box(Modifier.weight(1f)) {
                        LazyColumn(Modifier.fillMaxSize().padding(vertical = 8.dp), state = liste) {
                            item {
                                val alle = ergebnis?.anzahl
                                RegelZeile(null, stringResource(Res.string.desk_check_all), alle, false, gewaehlt == null, true, onHaken = null) { gewaehlt = null }
                            }
                            RegelGruppe.entries.forEach { g ->
                                item(key = g.name) { Spacer(Modifier.height(8.dp)); ArtGruppe(stringResource(gruppenName(g))) }
                                items(Regelkatalog.alle.filter { it.gruppe == g }, key = { it.id }) { r ->
                                    val an = r.id !in aus
                                    val zahl = if (an) ergebnis?.treffer?.get(r.id)?.size else null
                                    RegelZeile(r, r.frage(deutsch), zahl, r.schwere == Schwere.Fehler, gewaehlt == r.id, an,
                                        onHaken = { aus = if (it) aus - r.id else aus + r.id }) { gewaehlt = r.id }
                                    if (gewaehlt == r.id && r.grenzwert != null) Grenzfeld(r, grenzwerte[r.id]) { w ->
                                        PruefWahl.grenzwert(r.id, w)
                                        grenzwerte = if (w == null) grenzwerte - r.id else grenzwerte + (r.id to w)
                                    }
                                }
                            }
                        }
                        ListenLeiste(liste)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(stringResource(Res.string.desk_check_hint), Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.padding(12.dp)) {
                        Knopf(stringResource(Res.string.desk_check_reset), grenzwerte.isNotEmpty() || aus.isNotEmpty()) {
                            Regelkatalog.alle.forEach { PruefWahl.grenzwert(it.id, null) }
                            grenzwerte = emptyMap(); aus = emptySet()
                        }
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // ── Treffer ──
                Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val r = gewaehlt?.let { Regelkatalog.nachId[it] }
                    Text(r?.let { "${it.id}  ${it.frage(deutsch)}" } ?: stringResource(Res.string.desk_check_title, baumTitel),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    val e = ergebnis
                    val b = baum?.getOrNull()
                    if (e != null) {
                        val fehler = e.treffer.filterKeys { Regelkatalog.nachId[it]?.schwere == Schwere.Fehler }.values.sumOf { it.size }
                        Text(stringResource(Res.string.desk_check_summary, e.anzahl, fehler) +
                            (b?.let { "  ·  " + stringResource(Res.string.desk_check_checked, it.individuals.count { (_, i) -> !i.person.isPrivate }, it.families.size) }.orEmpty()),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val fehlerText = baum?.exceptionOrNull()
                        when {
                            fehlerText != null -> Text(fehlerText.message ?: "?", color = MaterialTheme.colorScheme.error)
                            e == null || b == null -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                val (x, n) = fortschritt
                                if (n > 0) LinearProgressIndicator(progress = { x.toFloat() / n }, modifier = Modifier.width(280.dp)) else CircularProgressIndicator()
                                Text(if (n > 0) stringResource(Res.string.desk_check_loading, x, n) else stringResource(Res.string.desk_chart_loading_any), Modifier.padding(top = 8.dp))
                            }
                            else -> {
                                val zeilen = if (r == null) e.treffer.values.flatten() else e.treffer[r.id].orEmpty()
                                if (zeilen.isEmpty()) Text(stringResource(if (r != null && r.id in aus) Res.string.desk_check_off else Res.string.desk_check_none),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else {
                                    val trefferListe = rememberLazyListState()
                                    LazyColumn(Modifier.fillMaxSize(), state = trefferListe) {
                                        items(zeilen) { t -> TrefferZeile(t, b, r == null, state.root, viewModel, openSheet) }
                                    }
                                    ListenLeiste(trefferListe)
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bereit = e != null && b != null
                        fun doc() = listenPdf(pruefZeilen(e!!, b!!, baumTitel, grenzwerte), appName, baumTitel)
                        val titel = stringResource(Res.string.desk_check_title, baumTitel)
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.desk_check_rerun), baum != null) { neu++ } }
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.desk_print), bereit) { drucken(doc(), titel) } }
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.desk_save_pdf), bereit) { alsPdf(doc(), titel) } }
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.action_close), true, onClose) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegelZeile(r: Regel?, text: String, zahl: Int?, fehler: Boolean, aktiv: Boolean, an: Boolean, onHaken: ((Boolean) -> Unit)?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (aktiv) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .fokusRahmen().clickable(onClick = onClick).padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onHaken != null) Checkbox(checked = an, onCheckedChange = onHaken, modifier = Modifier.size(32.dp)) else Spacer(Modifier.width(32.dp))
        Text(r?.id.orEmpty(), Modifier.width(34.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
            fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal,
            color = if (an) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        val farbe = when {
            zahl == null || zahl == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
            fehler -> MaterialTheme.colorScheme.error
            else -> Color(0xFFB26A00)
        }
        Text(zahl?.toString() ?: "–", Modifier.width(40.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium, color = farbe,
            fontWeight = if ((zahl ?: 0) > 0) FontWeight.SemiBold else FontWeight.Normal, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun Grenzfeld(r: Regel, wert: Double?, onWert: (Double?) -> Unit) {
    var text by remember(r.id) { mutableStateOf(zahl(wert ?: r.grenzwert!!)) }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(start = 70.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.desk_check_limit), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(text, { neu ->
            text = neu
            val d = neu.replace(',', '.').toDoubleOrNull()
            if (d != null && d >= 0) onWert(if (d == r.grenzwert) null else d)
        }, singleLine = true, modifier = Modifier.width(110.dp), textStyle = MaterialTheme.typography.bodyMedium)
        Text(einheit(r.einheit, wert ?: r.grenzwert!!), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(stringResource(Res.string.desk_check_default, zahl(r.grenzwert!!)), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrefferZeile(t: Treffer, b: TreeExport, mitRegel: Boolean, root: String?, viewModel: AppViewModel, openSheet: (String) -> Unit) {
    val xref = trefferPerson(t, b)
    val p = b.person(xref)
    val r = Regelkatalog.nachId[t.regel]
    Row(
        Modifier.fillMaxWidth().fokusRahmen()
            .combinedClickable(enabled = xref != null, onClick = { xref?.let { viewModel.setRoot(it) } }, onDoubleClick = { xref?.let(openSheet) })
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mitRegel && r != null) Text(t.regel, Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
            color = if (r.schwere == Schwere.Fehler) MaterialTheme.colorScheme.error else Color(0xFFB26A00))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p?.let(::registerName) ?: t.familie.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (xref == root) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                p?.lifespan?.takeIf(String::isNotBlank)?.let { Text("  ($it)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("  " + listOfNotNull(xref, t.familie).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(t.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
