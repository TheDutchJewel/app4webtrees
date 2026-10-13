package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import de.bgghome.webtrees.nativ.Sprache
import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.TreeExport
import de.bgghome.webtrees.nativ.data.DesktopAblage
import de.bgghome.webtrees.nativ.gedcom.GedcomBefund
import de.bgghome.webtrees.nativ.gedcom.GedcomLeser
import de.bgghome.webtrees.nativ.pruefung.Abhakliste
import de.bgghome.webtrees.nativ.pruefung.PruefErgebnis
import de.bgghome.webtrees.nativ.pruefung.PruefOptionen
import de.bgghome.webtrees.nativ.pruefung.PruefTexte
import de.bgghome.webtrees.nativ.pruefung.RegelGruppe
import de.bgghome.webtrees.nativ.pruefung.Regelkatalog
import de.bgghome.webtrees.nativ.pruefung.Schwere
import de.bgghome.webtrees.nativ.pruefung.Treffer
import de.bgghome.webtrees.nativ.pruefung.Voreinstellung
import de.bgghome.webtrees.nativ.pruefung.pruefen
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.LocalAppName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/*
 * gedaudit (13.10.2026): die Plausibilitaetspruefung von wtWin als eigenes Programm fuer eine GEDCOM-Datei, ohne
 * webtrees. Derselbe Regelkatalog, dieselben Bausteine (Regelliste, Voreinstellungen, Abhakliste, Druck); statt
 * Personenblatt und Bearbeiten gibt es unter den Treffern eine Karte der Person mit ihren Ereignissen und Familien.
 * Die Datei wird nur gelesen (GedcomLeser) und in die Form des Server-Exports gebracht, nichts wird geschrieben.
 */

/** Einstellungen von gedaudit: eigene Ablage, zuletzt geoeffnete Dateien. */
object GedauditAblage {
    val prefs = DesktopAblage("gedaudit")
    fun zuletzt(): List<File> = prefs.getString("zuletzt", null)?.split('\n')?.filter { it.isNotBlank() }?.map(::File).orEmpty()
    fun merken(f: File) = prefs.putString("zuletzt", (listOf(f.absolutePath) + zuletzt().map { it.absolutePath }).distinct().take(8).joinToString("\n"))
    fun vergessen(f: File) = prefs.putString("zuletzt", zuletzt().map { it.absolutePath }.filter { it != f.absolutePath }.joinToString("\n"))
}

/** Hilfe von gedaudit: das eigene Kapitel und das der Pruefung. */
fun gedauditHilfeEinrichten() { HilfeTexte.REIHENFOLGE = listOf("gedaudit", "pruefung") }

class GeladeneDatei(val datei: File, val baum: TreeExport, val befund: GedcomBefund)

private fun gedcomWaehlen(titel: String): File? {
    val dialog = FileDialog(null as Frame?, titel, FileDialog.LOAD).apply {
        setFilenameFilter { _, name -> name.endsWith(".ged", true) || name.endsWith(".gedcom", true) }
        isVisible = true
    }
    val name = dialog.file ?: return null
    return File(dialog.directory, name)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrameWindowScope.GedauditFenster(startDatei: File?, onQuit: () -> Unit) {
    val appName = LocalAppName.current
    val wahl = remember { PruefWahl(GedauditAblage.prefs) }
    val ablage = remember { AbhakAblage("gedaudit") }
    var datei by remember { mutableStateOf(startDatei) }
    var neu by remember { mutableIntStateOf(0) }
    var aus by remember { mutableStateOf(wahl.aus()) }
    var grenzwerte by remember { mutableStateOf(wahl.grenzwerte()) }
    var schwere by remember { mutableStateOf(wahl.schwere()) }
    var schaetzen by remember { mutableStateOf(wahl.schaetzen) }
    var voreinstellung by remember { mutableStateOf(wahl.voreinstellung) }
    var gewaehlt by remember { mutableStateOf<String?>(null) }
    var person by remember { mutableStateOf<String?>(null) }
    var abhak by remember { mutableStateOf(ablage.laden()) }
    var zeigeAbgehakt by remember { mutableStateOf(false) }
    var ueber by remember { mutableStateOf(false) }
    var zuletzt by remember { mutableStateOf(GedauditAblage.zuletzt()) }
    fun schwereVon(id: String) = schwere[id] ?: Regelkatalog.nachId.getValue(id).schwere
    fun eigene() { voreinstellung = "eigene"; wahl.voreinstellung = "eigene" }
    fun oeffnen(f: File) { datei = f; person = null; gewaehlt = null; GedauditAblage.merken(f); zuletzt = GedauditAblage.zuletzt() }
    fun waehlen() { gedcomWaehlen(Texte.t(Res.string.ga_open))?.let(::oeffnen) }

    val geladen by produceState<Result<GeladeneDatei>?>(null, datei, neu) {
        value = null
        val f = datei ?: return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val d = GedcomLeser.lesen(f)
                GeladeneDatei(f, GedcomLeser.alsExport(d, stand = f.lastModified()), d.befund)
            }
        }
    }
    val baum = geladen?.getOrNull()?.baum
    val baumTitel = datei?.name.orEmpty()
    val baumSchluessel = "gedaudit|" + (datei?.absolutePath ?: "")
    val abgehakt = abhak.fuer(baumSchluessel)
    val ergebnis by produceState<PruefErgebnis?>(null, baum, aus, grenzwerte, schaetzen) {
        val b = baum ?: run { value = null; return@produceState }
        value = withContext(Dispatchers.Default) { pruefen(b, PruefOptionen(grenzwerte, aus, schaetzen, texte = PruefTexte.ausRessourcen(Sprache.aktiv))) }
    }
    LaunchedEffect(aus) { wahl.aus(aus) }
    LaunchedEffect(geladen) { geladen?.exceptionOrNull()?.let { datei?.let(GedauditAblage::vergessen); zuletzt = GedauditAblage.zuletzt() } }

    fun abhaken(t: Treffer) { abhak = abhak.umschalten(baumSchluessel, t, java.time.LocalDate.now().toString()); ablage.sichern(abhak) }
    fun voreinstellungSetzen(v: Voreinstellung) {
        aus = v.aus; grenzwerte = v.grenzwerte; schwere = v.schwere; schaetzen = v.schaetzen; voreinstellung = v.id
        wahl.grenzwerte(v.grenzwerte); wahl.schwere(v.schwere); wahl.schaetzen = v.schaetzen; wahl.voreinstellung = v.id
    }
    val e = ergebnis
    val bereit = e != null && baum != null
    fun doc() = listenPdf(pruefZeilen(e!!, baum!!, baumTitel, grenzwerte, schwere, abgehakt), appName, baumTitel)
    val druckTitel = Texte.t(Res.string.desk_check_title, baumTitel)

    MenuBar {
        Menu(stringResource(Res.string.desk_menu_file)) {
            Item(stringResource(Res.string.ga_open), shortcut = KeyShortcut(Key.O, ctrl = true), onClick = ::waehlen)
            if (zuletzt.isNotEmpty()) Menu(stringResource(Res.string.ga_recent)) {
                zuletzt.forEach { f -> Item(f.name + "  –  " + f.parent.orEmpty(), onClick = { oeffnen(f) }) }
            }
            Item(stringResource(Res.string.ga_reload), enabled = datei != null, shortcut = KeyShortcut(Key.F5), onClick = { neu++ })
            Separator()
            Item(stringResource(Res.string.desk_print), enabled = bereit, shortcut = KeyShortcut(Key.P, ctrl = true), onClick = { drucken(doc(), druckTitel) })
            Item(stringResource(Res.string.desk_save_pdf), enabled = bereit, onClick = { alsPdf(doc(), druckTitel) })
            Separator()
            Item(stringResource(Res.string.desk_quit), shortcut = KeyShortcut(Key.Q, ctrl = true), onClick = onQuit)
        }
        Menu(stringResource(Res.string.desk_menu_view)) {
            Menu(stringResource(Res.string.ga_menu_language)) {
                RadioButtonItem(stringResource(Res.string.ga_menu_system), selected = Sprache.wahl.value == null, onClick = { Sprache.setzen(null) })
                Sprache.ALLE.forEach { (code, name) -> RadioButtonItem(name, selected = Sprache.wahl.value == code, onClick = { Sprache.setzen(code) }) }
            }
            Menu(stringResource(Res.string.desk_appearance)) {
                RadioButtonItem(stringResource(Res.string.ga_appearance_light), selected = DeskErscheinung.wahl.value == DeskErscheinung.Wahl.Hell, onClick = { DeskErscheinung.setzen(DeskErscheinung.Wahl.Hell) })
                RadioButtonItem(stringResource(Res.string.ga_appearance_dark), selected = DeskErscheinung.wahl.value == DeskErscheinung.Wahl.Dunkel, onClick = { DeskErscheinung.setzen(DeskErscheinung.Wahl.Dunkel) })
                RadioButtonItem(stringResource(Res.string.ga_menu_system), selected = DeskErscheinung.wahl.value == DeskErscheinung.Wahl.System, onClick = { DeskErscheinung.setzen(DeskErscheinung.Wahl.System) })
            }
        }
        Menu(stringResource(Res.string.desk_menu_help)) {
            Item(stringResource(Res.string.desk_help), shortcut = KeyShortcut(Key.F1), onClick = { Hilfe.oeffnen("gedaudit") })
            Item(stringResource(Res.string.desk_about, appName), onClick = { ueber = true })
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            datei == null -> Startseite(zuletzt, onOeffnen = ::waehlen, onDatei = ::oeffnen)
            geladen == null -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(); Text(stringResource(Res.string.ga_loading), Modifier.padding(top = 8.dp))
            }
            geladen?.isFailure == true -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.ga_read_error, geladen?.exceptionOrNull()?.message ?: "?"), color = MaterialTheme.colorScheme.error)
                Knopf(stringResource(Res.string.ga_open), true, onClick = ::waehlen)
            }
            else -> Row(Modifier.fillMaxSize()) {
                val b = baum!!
                // ── Einstellungen und Regeln ──
                val pruefBreite = rememberBreite("ga_liste", 470f)
                Column(Modifier.width(pruefBreite.value.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Einstellung(stringResource(Res.string.desk_check_preset), stringResource(Res.string.tipp_preset)) {
                            val namen = Voreinstellung.alle.associate { it.id to PruefTexte.nameText(it) } + ("eigene" to stringResource(Res.string.desk_check_preset_own))
                            Auswahl(namen[voreinstellung] ?: namen.getValue("eigene"), Voreinstellung.alle.map { namen.getValue(it.id) }) { w ->
                                Voreinstellung.alle.firstOrNull { namen[it.id] == w }?.let(::voreinstellungSetzen)
                            }
                        }
                        Haken(stringResource(Res.string.desk_check_estimate), schaetzen, stringResource(Res.string.tipp_estimate)) { schaetzen = it; wahl.schaetzen = it; eigene() }
                    }
                    HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    val liste = rememberLazyListState()
                    Box(Modifier.weight(1f)) {
                        LazyColumn(Modifier.fillMaxSize().padding(vertical = 6.dp), state = liste) {
                            item {
                                val alle = e?.treffer?.values?.sumOf { l -> l.count { it.schluessel !in abgehakt } }
                                RegelZeile(null, stringResource(Res.string.desk_check_all), alle, false, gewaehlt == null, true, onHaken = null) { gewaehlt = null }
                            }
                            RegelGruppe.entries.forEach { g ->
                                item(key = g.name) { Spacer(Modifier.height(8.dp)); ArtGruppe(stringResource(gruppenName(g))) }
                                items(Regelkatalog.alle.filter { it.gruppe == g }, key = { it.id }) { r ->
                                    val an = r.id !in aus
                                    val zahl = if (an) e?.treffer?.get(r.id)?.count { it.schluessel !in abgehakt } else null
                                    RegelZeile(r, PruefTexte.frageText(r), zahl, schwereVon(r.id) == Schwere.Fehler, gewaehlt == r.id, an,
                                        onHaken = { aus = if (it) aus - r.id else aus + r.id; eigene() }) { gewaehlt = r.id }
                                    if (gewaehlt == r.id) RegelEinstellung(r, grenzwerte[r.id], schwereVon(r.id),
                                        onWert = { w -> grenzwerte = if (w == null) grenzwerte - r.id else grenzwerte + (r.id to w); wahl.grenzwerte(grenzwerte); eigene() },
                                        onSchwere = { s -> schwere = if (s == r.schwere) schwere - r.id else schwere + (r.id to s); wahl.schwere(schwere); eigene() })
                                }
                            }
                        }
                        ListenLeiste(liste)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(stringResource(Res.string.ga_hint), Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            Knopf(stringResource(Res.string.desk_check_ticks_export), abhak.anzahl(baumSchluessel) > 0, stringResource(Res.string.tipp_ticks_export)) {
                                dateiWaehlen(Texte.t(Res.string.desk_check_ticks_export), true, "pruefung-abgehakt.json")?.let { runCatching { it.writeText(abhak.alsJson()) } }
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            Knopf(stringResource(Res.string.desk_check_ticks_import), true, stringResource(Res.string.tipp_ticks_import)) {
                                dateiWaehlen(Texte.t(Res.string.desk_check_ticks_import), false)?.let { f ->
                                    runCatching { Abhakliste.dateiAusJson(f.readText()) }.getOrNull()?.let { abhak = abhak.zusammenfuehren(it); ablage.sichern(abhak) }
                                }
                            }
                        }
                    }
                }
                Trenner(pruefBreite, "ga_liste", 300f, 900f)

                // ── Treffer und Person ──
                Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val r = gewaehlt?.let { Regelkatalog.nachId[it] }
                    Text(r?.let { "${it.id}  ${PruefTexte.frageText(it)}" } ?: stringResource(Res.string.desk_check_title, baumTitel),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val befund = geladen?.getOrNull()?.befund
                        val zeile = if (e != null) {
                            val offen = e.treffer.mapValues { (_, l) -> l.count { it.schluessel !in abgehakt } }
                            val fehler = offen.filterKeys { schwereVon(it) == Schwere.Fehler }.values.sum()
                            stringResource(Res.string.desk_check_summary, offen.values.sum(), fehler) + "  ·  "
                        } else ""
                        Text(zeile + stringResource(Res.string.ga_file_info, b.individuals.size, b.families.size, befund?.let { stringResource(Res.string.ga_charset, it.zeichensatz) }.orEmpty()),
                            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val zahlAbgehakt = (e?.anzahl ?: 0) - (e?.treffer?.values?.sumOf { l -> l.count { it.schluessel !in abgehakt } } ?: 0)
                        if (zahlAbgehakt > 0) Haken(stringResource(Res.string.desk_check_show_ticked, zahlAbgehakt), zeigeAbgehakt) { zeigeAbgehakt = it }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val personHoehe = rememberBreite("ga_person", 260f)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            e == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            else -> {
                                val alle = if (r == null) e.treffer.values.flatten() else e.treffer[r.id].orEmpty()
                                val zeilen = if (zeigeAbgehakt) alle else alle.filter { it.schluessel !in abgehakt }
                                if (zeilen.isEmpty()) Text(stringResource(if (r != null && r.id in aus) Res.string.desk_check_off else Res.string.desk_check_none),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else {
                                    val trefferListe = rememberLazyListState()
                                    LazyColumn(Modifier.fillMaxSize(), state = trefferListe) {
                                        items(zeilen) { t ->
                                            val xref = trefferPerson(t, b)
                                            TrefferZeileDatei(t, b, r == null, schwereVon(t.regel), t.schluessel in abgehakt, aktiv = xref != null && xref == person,
                                                onClick = { person = xref }, onAbhaken = { abhaken(t) })
                                        }
                                    }
                                    ListenLeiste(trefferListe)
                                }
                            }
                        }
                    }
                    TrennerWaagrecht(personHoehe, "ga_person", 120f, 600f, unten = true)
                    Box(Modifier.height(personHoehe.value.dp).fillMaxWidth()) { PersonKarte(b, person) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.ga_open), true, onClick = ::waehlen) }
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.ga_reload), true) { neu++ } }
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.desk_print), bereit) { drucken(doc(), druckTitel) } }
                        Box(Modifier.weight(1f)) { Knopf(stringResource(Res.string.desk_save_pdf), bereit) { alsPdf(doc(), druckTitel) } }
                    }
                }
            }
        }
    }
    HilfeFenster()
    if (ueber) GedauditUeber(appName) { ueber = false }
}

@Composable
private fun Startseite(zuletzt: List<File>, onOeffnen: () -> Unit, onDatei: (File) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Markenkopf(mitClaim = false, groesse = 36)
        Text(stringResource(Res.string.ga_welcome), Modifier.padding(top = 20.dp).widthIn(max = 560.dp), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.padding(top = 20.dp).width(280.dp)) { Knopf(stringResource(Res.string.ga_open), true, onClick = onOeffnen) }
        if (zuletzt.isNotEmpty()) {
            Text(stringResource(Res.string.ga_recent), Modifier.padding(top = 28.dp, bottom = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            zuletzt.forEach { f ->
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().fokusRahmen().clickable { onDatei(f) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(f.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(f.parent.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun TrefferZeileDatei(t: Treffer, b: TreeExport, mitRegel: Boolean, schwere: Schwere, abgehakt: Boolean, aktiv: Boolean, onClick: () -> Unit, onAbhaken: () -> Unit) {
    val xref = trefferPerson(t, b)
    val p = b.person(xref)
    val blass = if (abgehakt) 0.45f else 1f
    Row(
        Modifier.fillMaxWidth().background(if (aktiv) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .fokusRahmen().clickable(enabled = xref != null, onClick = onClick).padding(start = 8.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mitRegel) Text(t.regel, Modifier.width(40.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
            color = (if (schwere == Schwere.Fehler) MaterialTheme.colorScheme.error else WARN_FARBE).copy(alpha = blass))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p?.let(::registerName) ?: t.familie.orEmpty(), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = blass), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                p?.lifespan?.takeIf(String::isNotBlank)?.let { Text("  ($it)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = blass)) }
                Text("  " + listOfNotNull(xref, t.familie).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = blass))
            }
            Text(t.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = blass), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onAbhaken, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Check, stringResource(if (abgehakt) Res.string.desk_check_untick else Res.string.desk_check_tick), Modifier.size(18.dp),
                tint = if (abgehakt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
        }
    }
}

/** Die Person zum gewaehlten Treffer: Ereignisse, Eltern, Partner und Kinder - aus der Datei, ohne Server. */
@Composable
private fun PersonKarte(b: TreeExport, xref: String?) {
    val d = xref?.let { b.detail(it) }
    if (d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(Res.string.ga_no_person), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    val rollen = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rollen).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(d.person.name.ifBlank { "?" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                d.person.lifespan.takeIf(String::isNotBlank)?.let { Text("  ($it)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("  ${d.person.xref}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Ueberschrift(stringResource(Res.string.ga_person_facts))
            d.facts.filter { it.tag != "NAME" }.forEach { f ->
                Row(Modifier.fillMaxWidth()) {
                    Text(f.label.ifBlank { f.tag }, Modifier.width(150.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(f.date?.text.orEmpty(), Modifier.width(170.dp), style = MaterialTheme.typography.bodySmall)
                    Text(listOfNotNull(f.place?.name?.takeIf(String::isNotBlank), f.value.takeIf(String::isNotBlank)).joinToString(" · "), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (f.sources.isNotEmpty()) Text(stringResource(Res.string.ga_person_sources, f.sources.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (d.parentFamilies.isNotEmpty()) {
                Ueberschrift(stringResource(Res.string.ga_person_parents))
                d.parentFamilies.forEach { fam -> Text(listOfNotNull(fam.husband, fam.wife).joinToString(" ⚭ ") { personText(it) } + "  (${fam.xref})", style = MaterialTheme.typography.bodySmall) }
            }
            if (d.spouseFamilies.isNotEmpty()) {
                Ueberschrift(stringResource(Res.string.ga_person_families))
                d.spouseFamilies.forEach { fam ->
                    Text("⚭ " + (fam.spouse?.let(::personText) ?: "?") + (fam.marriage?.date?.text?.takeIf(String::isNotBlank)?.let { "  $it" } ?: "") + "  (${fam.xref})",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    fam.children.forEach { k -> Text("     " + personText(k), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
        SenkrechteLeiste(rollen)
    }
}

private fun personText(p: Person) = p.name.ifBlank { "?" } + p.lifespan.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty() + "  ${p.xref}"

@Composable
private fun Ueberschrift(text: String) =
    Text(text, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun GedauditUeber(appName: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Markenkopf(appName, mitClaim = false, groesse = 26) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val version = (System.getProperty("gedaudit.versionName") ?: "dev") + (System.getProperty("gedaudit.build")?.let { ".$it" } ?: "")
                Text(stringResource(Res.string.ga_about_text, appName, version))
                Text(stringResource(Res.string.app_author), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(Res.string.ga_about_unofficial), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(Res.string.action_close)) } },
    )
}
