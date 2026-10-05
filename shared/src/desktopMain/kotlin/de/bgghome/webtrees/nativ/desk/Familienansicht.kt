package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.Ancestor
import de.bgghome.webtrees.nativ.api.DescendantNode
import de.bgghome.webtrees.nativ.api.EventJson
import de.bgghome.webtrees.nativ.api.FamilyJson
import de.bgghome.webtrees.nativ.api.IndividualDetail
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.halfSiblings
import de.bgghome.webtrees.nativ.data.hatPaten
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.Avatar
import de.bgghome.webtrees.nativ.ui.PatenZeilen
import de.bgghome.webtrees.nativ.ui.Section
import de.bgghome.webtrees.nativ.ui.UiState
import de.bgghome.webtrees.nativ.ui.faktLabelMitArt
import de.bgghome.webtrees.nativ.ui.forSex
import de.bgghome.webtrees.nativ.ui.requestAddRelative
import de.bgghome.webtrees.nativ.ui.select
import de.bgghome.webtrees.nativ.ui.setRoot
import de.bgghome.webtrees.nativ.ui.setSection
import de.bgghome.webtrees.nativ.ui.treeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Familienansicht: das Paar als grosse Karten in der Mitte, darueber die Vorfahren beider Partner in waehlbarer Tiefe
 * (2 bis 4 Generationen, oben die aeltesten als kleine Kaesten), darunter die Statuszeile der Partnerschaft (Heiraten,
 * Zahl der Kinder, Kind hinzufuegen) und die Kinder als Kaesten nebeneinander mit ihren Partnern und Enkeln. Hat der
 * Proband mehrere Ehen, steht jede auf einem eigenen Reiter. Ein Klick waehlt eine Person (Personentafel rechts), ein
 * Doppelklick macht sie zum Probanden - so geht man durch die Familien nach oben und unten.
 * Die Daten kommen aus der Personenabfrage des Probanden und seines Partners, den Ahnentafeln beider (Vorfahren) und
 * der Nachfahrenabfrage des Probanden (Partner und Kinder der Kinder) - nicht aus dem Baum-Export.
 */

/** Vorauswahl fuer den Dialog "Verwandte hinzufuegen", gesetzt vom leeren Feld "Vater/Mutter unbekannt". */
object Verwandtenwahl {
    var vorwahl: String? by mutableStateOf(null)
    var familie: String? by mutableStateOf(null)
    fun leeren() { vorwahl = null; familie = null }
}

/** Die Familie des Probanden auf dem gewaehlten Reiter; Eltern je Partner aus dessen erster Herkunftsfamilie. */
private class PaarDaten(
    val proband: IndividualDetail, val partner: IndividualDetail?,
    /** Vorfahren je Seite (Kekule-Nummern ab 2), Schluessel = XREF der Person unten. */
    val ahnen: Map<String, List<Ancestor>>,
    /** Die Kinder mit ihren Familien (Partner, Enkel), Schluessel = XREF des Kindes. */
    val nachfahren: Map<String, DescendantNode>,
)

private fun ereignisZeile(zeichen: String, e: EventJson?): String? {
    val datum = e?.date?.text.orEmpty(); val ort = e?.place?.name.orEmpty()
    return if (datum.isBlank() && ort.isBlank()) null else "$zeichen " + listOf(datum, ort).filter(String::isNotBlank).joinToString(", ")
}

private const val GEN_MIN = 2
private const val GEN_MAX = 4

@Composable
fun DeskFamilie(state: UiState, viewModel: AppViewModel, openWeb: (String) -> Unit, openSheet: ((String) -> Unit)? = null) {
    val root = state.root
    val tree = state.tree?.name
    var neu by remember { mutableIntStateOf(0) }
    var generationen by remember { mutableIntStateOf((DeskLayout.prefs.getString("familie_gen", null)?.toIntOrNull() ?: 3).coerceIn(GEN_MIN, GEN_MAX)) }
    // Nach dem Speichern (Verwandte hinzufuegen, Ereignis) neu laden
    var warBeschaeftigt by remember { mutableStateOf(false) }
    LaunchedEffect(state.busy) { if (warBeschaeftigt && !state.busy) neu++; warBeschaeftigt = state.busy }

    val proband by produceState<Result<IndividualDetail>?>(null, root, tree, neu) {
        if (root == null || tree == null) { value = null; return@produceState }
        if (value?.getOrNull()?.person?.xref != root) value = null
        value = withContext(Dispatchers.IO) { runCatching { viewModel.client.individual(tree, root) } }
    }
    var reiter by remember(root) { mutableIntStateOf(0) }
    val p = proband?.getOrNull()
    val familien = p?.spouseFamilies.orEmpty()
    val familie = familien.getOrNull(reiter.coerceAtMost((familien.size - 1).coerceAtLeast(0)))
    val partnerXref = familie?.let { f -> listOfNotNull(f.husband, f.wife).firstOrNull { it.xref != root }?.takeIf { !it.isPrivate }?.xref }
    val partner by produceState<IndividualDetail?>(null, partnerXref, tree, neu) {
        value = if (partnerXref == null || tree == null) null
        else withContext(Dispatchers.IO) { runCatching { viewModel.client.individual(tree, partnerXref) }.getOrNull() }
    }
    // Vorfahren beider Partner ab der Grosselterngeneration (die Eltern stehen schon in der Personenabfrage)
    val ahnen by produceState<Map<String, List<Ancestor>>>(emptyMap(), root, partnerXref, tree, neu, generationen) {
        if (root == null || tree == null || generationen < 3) { value = emptyMap(); return@produceState }
        value = withContext(Dispatchers.IO) {
            listOfNotNull(root, partnerXref).associateWith { x ->
                runCatching { viewModel.client.pedigree(tree, x, generationen).ancestors.filter { it.n >= 4 } }.getOrNull().orEmpty()
            }
        }
    }
    // Partner und Kinder der Kinder
    val nachfahren by produceState<Map<String, DescendantNode>>(emptyMap(), root, tree, neu) {
        if (root == null || tree == null) { value = emptyMap(); return@produceState }
        value = withContext(Dispatchers.IO) {
            runCatching { viewModel.client.descendants(tree, root, 2).tree.families.flatMap { it.children }.associateBy { it.person.xref } }.getOrNull().orEmpty()
        }
    }

    // Tastatur: hoch Vater (mit Umschalt Mutter), runter das erste Kind, links/rechts die Geschwister, Tab der naechste Reiter
    fun gehe(r: Richtung): Boolean { p?.let { zielPerson(it, familie, r) }?.let { viewModel.setRoot(it) }; return true }
    val tasten = Modifier.tastenBereich(root) { e ->
        if (e.type != KeyEventType.KeyDown || e.isAltPressed || e.isCtrlPressed) false else when (e.key) {
            Key.DirectionUp -> gehe(if (e.isShiftPressed) Richtung.Mutter else Richtung.Vater)
            Key.DirectionDown -> gehe(Richtung.Kind)
            Key.DirectionLeft -> gehe(Richtung.GeschwisterZurueck)
            Key.DirectionRight -> gehe(Richtung.GeschwisterVor)
            Key.Tab -> { if (familien.size > 1) reiter = (reiter + 1) % familien.size; familien.size > 1 }
            else -> false
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).then(tasten)) {
        val fehler = proband?.exceptionOrNull()
        when {
            root == null -> {}
            fehler != null -> Text(fehler.message ?: "?", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
            p == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> Familie(state, viewModel, openWeb, openSheet, PaarDaten(p, partner?.takeIf { it.person.xref == partnerXref }, ahnen, nachfahren),
                familien, familie, reiter, onReiter = { reiter = it }, generationen = generationen,
                onGenerationen = { generationen = it.coerceIn(GEN_MIN, GEN_MAX); DeskLayout.prefs.putString("familie_gen", generationen.toString()) })
        }
    }
}

@Composable
private fun Familie(
    state: UiState, viewModel: AppViewModel, openWeb: (String) -> Unit, openSheet: ((String) -> Unit)?, daten: PaarDaten,
    familien: List<FamilyJson>, familie: FamilyJson?, reiter: Int, onReiter: (Int) -> Unit, generationen: Int, onGenerationen: (Int) -> Unit,
) {
    val p = daten.proband.person
    val canEdit = state.tree?.canEdit == true
    // Mann links, Frau rechts; ohne Familie steht der Proband auf seiner Seite allein
    val mann = familie?.husband ?: p.takeIf { it.sex != "F" }
    val frau = familie?.wife ?: p.takeIf { it.sex == "F" }
    fun elternVon(x: Person?): FamilyJson? = when (x?.xref) {
        null -> null
        p.xref -> daten.proband.parentFamilies.firstOrNull()
        daten.partner?.person?.xref -> daten.partner.parentFamilies.firstOrNull()
        else -> null
    }
    val karte = KartenAktionen(state, viewModel, openWeb, openSheet, canEdit)
    val scroll = rememberScrollState()
    val farben = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 24.dp, vertical = 12.dp)) {
            // ── Generationen nach oben ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(stringResource(Res.string.tree_generations, generationen), style = MaterialTheme.typography.labelMedium, color = farben.onSurfaceVariant)
                TextButton(onClick = { onGenerationen(generationen - 1) }, enabled = generationen > GEN_MIN) { Text("−") }
                TextButton(onClick = { onGenerationen(generationen + 1) }, enabled = generationen < GEN_MAX) { Text("+") }
            }

            // ── Vorfahren ab den Grosseltern: oben die aelteste Generation, je Seite 2^(g-1) Plaetze ──
            for (g in generationen downTo 3) {
                val von = 1 shl (g - 1); val bis = (1 shl g) - 1
                val zeile = listOf(mann, frau).map { s -> s?.let { daten.ahnen[it.xref] }.orEmpty().filter { it.n in von..bis } }
                if (zeile.all { it.isEmpty() }) continue
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    zeile.forEach { seite ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (n in von..bis) {
                                    val a = seite.firstOrNull { it.n == n }
                                    Box(Modifier.weight(1f)) { if (a != null) AhnKarte(a.person, g, karte) else Spacer(Modifier.fillMaxWidth()) }
                                }
                            }
                            if (seite.isNotEmpty()) Linie()
                        }
                    }
                }
            }

            // ── Eltern beider Partner ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                listOf(mann, frau).forEach { kind ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (kind != null && !kind.isPrivate) {
                            val eltern = elternVon(kind)
                            Text(stringResource(Res.string.desk_family_parents_of, klarName(kind)), style = MaterialTheme.typography.labelMedium,
                                color = farben.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) { PersonKarte(eltern?.husband, stringResource(Res.string.desk_unknown_father), karte, kind.xref, "father") }
                                Box(Modifier.weight(1f)) { PersonKarte(eltern?.wife, stringResource(Res.string.desk_unknown_mother), karte, kind.xref, "mother") }
                            }
                            Linie()
                        }
                    }
                }
            }

            // ── Geschwister des Probanden (Halbgeschwister mit ½) ──
            val voll = daten.proband.parentFamilies.flatMap { it.children }.filter { it.xref != p.xref }.distinctBy { it.xref }
            val halb = daten.proband.halfSiblings().map { it.person }.filter { h -> voll.none { it.xref == h.xref } }
            if (voll.isNotEmpty() || halb.isNotEmpty()) {
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(Res.string.desk_family_siblings_of, klarName(p)), style = MaterialTheme.typography.labelMedium,
                        color = farben.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp, end = 4.dp))
                    (voll.map { it to false } + halb.map { it to true }).forEach { (g, istHalb) -> GeschwisterChip(g, istHalb, karte) }
                }
            }

            // ── Reiter: eine Ehe je Reiter ──
            if (familien.size > 1) {
                PrimaryScrollableTabRow(selectedTabIndex = reiter.coerceAtMost(familien.lastIndex), edgePadding = 0.dp, containerColor = farben.background) {
                    familien.forEachIndexed { i, f ->
                        val ander = f.spouse ?: listOfNotNull(f.husband, f.wife).firstOrNull { it.xref != p.xref }
                        val jahr = f.marriage?.date?.year?.takeIf { it > 0 }?.let { " oo $it" }.orEmpty()
                        Tab(selected = i == reiter, onClick = { onReiter(i) }, text = {
                            Text((ander?.let { if (it.isPrivate) stringResource(Res.string.person_private) else klarName(it) } ?: unbekannterPartner(p.sex)) + jahr, maxLines = 1)
                        })
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Das Paar als Karten ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) { PaarKarte(mann, unbekannterPartner("F"), karte, if (familie == null) p.xref else null) }
                Box(Modifier.width(56.dp).heightIn(min = 170.dp), contentAlignment = Alignment.Center) {
                    Text(if (familie != null) "⚭" else "", style = MaterialTheme.typography.headlineSmall, color = farben.onSurfaceVariant)
                }
                Box(Modifier.weight(1f)) { PaarKarte(frau, unbekannterPartner("M"), karte, if (familie == null) p.xref else null) }
            }

            // ── Statuszeile der Partnerschaft: Heiraten, Zahl der Kinder, Kind hinzufuegen ──
            val kinder = familie?.children.orEmpty()
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().background(farben.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    if (familie == null) Text(stringResource(Res.string.desk_family_no_partner), style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                    else {
                        // Alle Heiraten der Familie (standesamtlich und kirchlich), nicht nur die erste; ohne MARR-Fakt die Kurzform
                        val heiraten = familie.facts.filter { it.tag == "MARR" }
                        if (heiraten.isEmpty()) {
                            val datum = familie.marriage?.date?.text.orEmpty(); val ort = familie.marriage?.place?.name.orEmpty()
                            Text(if (datum.isBlank() && ort.isBlank()) stringResource(Res.string.desk_family_status_none)
                                else stringResource(Res.string.desk_family_status_married) + ": " + listOf(datum, ort).filter(String::isNotBlank).joinToString(", "),
                                style = MaterialTheme.typography.bodySmall)
                        } else heiraten.forEach { h ->
                            val datum = h.date?.text.orEmpty(); val ort = h.place?.name.orEmpty()
                            Text(faktLabelMitArt(h) + ": " + listOf(datum, ort).filter(String::isNotBlank).joinToString(", ").ifBlank { "–" },
                                style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Text(stringResource(Res.string.desk_family_children, kinder.size), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                if (canEdit) TextButton(onClick = { Verwandtenwahl.vorwahl = "child"; Verwandtenwahl.familie = familie?.xref; viewModel.requestAddRelative(p.xref) }, modifier = Modifier.height(30.dp)) {
                    Text("+ " + stringResource(Res.string.desk_family_add_child), style = MaterialTheme.typography.labelMedium)
                }
            }

            // ── Trauzeugen der Heiraten (ab API-Stufe 19), verlinkte anklickbar ──
            familie?.facts?.filter { it.tag == "MARR" && it.hatPaten }?.forEach { h ->
                PatenZeilen(h, onPerson = viewModel::select, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }

            // ── Kinder als Kaesten nebeneinander, darunter Partner und Enkel ──
            Spacer(Modifier.height(12.dp))
            if (kinder.isEmpty()) {
                Text(stringResource(Res.string.desk_family_no_children), style = MaterialTheme.typography.bodyMedium,
                    color = farben.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                val waagrecht = rememberScrollState()
                Row(Modifier.fillMaxWidth().horizontalScroll(waagrecht), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    kinder.forEach { k -> KindKarte(k, daten.nachfahren[k.xref], karte) }
                }
            }
        }
        SenkrechteLeiste(scroll)
    }
}

/** Was eine Karte beim Klicken, Doppelklicken und im Kontextmenue tut. */
private class KartenAktionen(val state: UiState, val viewModel: AppViewModel, val openWeb: (String) -> Unit, val openSheet: ((String) -> Unit)?, val canEdit: Boolean)

@Composable
private fun Linie() {
    Box(Modifier.padding(top = 4.dp, bottom = 4.dp).width(2.dp).height(12.dp).background(treeColors.connector))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Anklickbar(person: Person, a: KartenAktionen, inhalt: @Composable (Modifier) -> Unit) {
    ContextMenuArea(items = {
        buildList {
            add(ContextMenuItem(Texte.t(Res.string.action_make_root)) { a.viewModel.setRoot(person.xref) })
            a.openSheet?.let { s -> add(ContextMenuItem(Texte.t(Res.string.desk_sheet)) { s(person.xref) }) }
            if (a.canEdit) add(ContextMenuItem(Texte.t(Res.string.action_add_relative)) { a.viewModel.requestAddRelative(person.xref) })
            add(ContextMenuItem(Texte.t(Res.string.chip_open_web)) { a.openWeb(person.url) })
        }
    }) {
        inhalt(Modifier.fokusRahmen().combinedClickable(onDoubleClick = { a.viewModel.setRoot(person.xref) }) { a.viewModel.select(person.xref) })
    }
}

/** Rahmen einer Personenkarte: Geschlechtsfarbe, Proband dicker, gewaehlte Person in der Hauptfarbe. */
@Composable
private fun Modifier.kartenRahmen(person: Person, a: KartenAktionen, shape: androidx.compose.ui.graphics.Shape, dick: Boolean = true): Modifier {
    val proband = person.xref == a.state.root
    val gewaehlt = person.xref == a.state.selected
    return background(MaterialTheme.colorScheme.surface, shape)
        .border(if (gewaehlt) 2.5.dp else if (proband && dick) 2.dp else 1.dp, if (gewaehlt) MaterialTheme.colorScheme.primary else treeColors.forSex(person.sex), shape)
}

/** Ein Vorfahr ab der Grosselterngeneration: je hoeher, desto knapper (Urgrosseltern nur Name und Jahre). */
@Composable
private fun AhnKarte(person: Person, generation: Int, a: KartenAktionen) {
    val shape = MaterialTheme.shapes.small
    val knapp = generation >= 4
    Anklickbar(person, a) { klick ->
        Row(Modifier.fillMaxWidth().height(if (knapp) 40.dp else 52.dp).kartenRahmen(person, a, shape, dick = false).then(klick).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (!knapp) { Avatar(person, 32.dp); Spacer(Modifier.width(6.dp)) }
            Column(Modifier.weight(1f)) {
                Text(if (person.isPrivate) stringResource(Res.string.person_private) else klarName(person), style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!person.isPrivate && jahre(person).isNotEmpty()) Text(jahre(person), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/**
 * Ein Elternteil als kleine Karte. Fehlt er, steht dort [leer] ("Vater unbekannt") - mit Bearbeitungsrecht und
 * [ergaenzenFuer] als Knopf, der Verwandte hinzufuegen oeffnet.
 */
@Composable
private fun PersonKarte(person: Person?, leer: String, a: KartenAktionen, ergaenzenFuer: String?, beziehung: String? = null) {
    val shape = MaterialTheme.shapes.medium
    val hoehe: Dp = 64.dp
    if (person == null) {
        LeereKarte(hoehe, leer, a, ergaenzenFuer, beziehung)
        return
    }
    Anklickbar(person, a) { klick ->
        Row(Modifier.fillMaxWidth().height(hoehe).kartenRahmen(person, a, shape).then(klick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(person, 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (person.isPrivate) stringResource(Res.string.person_private) else person.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!person.isPrivate && jahre(person).isNotEmpty()) Text(jahre(person), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LeereKarte(hoehe: Dp, leer: String, a: KartenAktionen, ergaenzenFuer: String?, beziehung: String?) {
    Box(Modifier.fillMaxWidth().height(hoehe).border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(leer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (a.canEdit && ergaenzenFuer != null) TextButton(onClick = { Verwandtenwahl.vorwahl = beziehung; a.viewModel.requestAddRelative(ergaenzenFuer) }, modifier = Modifier.height(30.dp)) {
                Text("+ " + stringResource(Res.string.desk_family_add), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Ein Partner des Paars als grosse Karte: Portraet, Name, Zeilen Geboren / Gestorben / Beruf mit Beschriftung, darunter
 * die Knoepfe Personenblatt und Fotos. Fehlt er, der leere Kasten mit "hinzufuegen".
 */
@Composable
private fun PaarKarte(person: Person?, leer: String, a: KartenAktionen, ergaenzenFuer: String?) {
    val shape = MaterialTheme.shapes.medium
    if (person == null) { LeereKarte(170.dp, leer, a, ergaenzenFuer, "spouse"); return }
    val farben = MaterialTheme.colorScheme
    val proband = person.xref == a.state.root
    Anklickbar(person, a) { klick ->
        Column(Modifier.fillMaxWidth().heightIn(min = 170.dp).kartenRahmen(person, a, shape).then(klick).padding(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Avatar(person, 96.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (person.isPrivate) stringResource(Res.string.person_private) else person.name, style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (proband) FontWeight.Bold else FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!person.isPrivate) {
                        Spacer(Modifier.height(4.dp))
                        DatenZeile(stringResource(Res.string.desk_family_born), person.birth ?: person.chr)
                        DatenZeile(stringResource(Res.string.desk_family_died), person.death ?: person.buri)
                        person.occupation?.takeIf { it.isNotBlank() }?.let { DatenZeile(stringResource(Res.string.desk_list_fact_occupation), it) }
                    }
                }
            }
            if (!person.isPrivate) {
                Spacer(Modifier.weight(1f))
                HorizontalDivider(Modifier.padding(top = 6.dp), color = farben.outlineVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    a.openSheet?.let { s -> TextButton(onClick = { s(person.xref) }, modifier = Modifier.height(30.dp)) { Text(stringResource(Res.string.desk_sheet), style = MaterialTheme.typography.labelMedium) } }
                    TextButton(onClick = { a.viewModel.select(person.xref); a.viewModel.setSection(Section.Photos) }, modifier = Modifier.height(30.dp)) {
                        Text(stringResource(Res.string.nav_photos), style = MaterialTheme.typography.labelMedium)
                    }
                    if (a.canEdit) TextButton(onClick = { a.viewModel.requestAddRelative(person.xref) }, modifier = Modifier.height(30.dp)) {
                        Text(stringResource(Res.string.action_add_relative), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatenZeile(beschriftung: String, e: EventJson?) {
    val datum = e?.date?.text.orEmpty(); val ort = e?.place?.name.orEmpty()
    if (datum.isBlank() && ort.isBlank()) return
    DatenZeile(beschriftung, listOf(datum, ort).filter(String::isNotBlank).joinToString(", "))
}

@Composable
private fun DatenZeile(beschriftung: String, wert: String) {
    Row {
        Text(beschriftung, Modifier.width(78.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(wert, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Ein Geschwister als kleiner Reiter: Name und Jahre, Halbgeschwister mit ½. */
@Composable
private fun GeschwisterChip(g: Person, halb: Boolean, a: KartenAktionen) {
    val text = (if (g.isPrivate) stringResource(Res.string.person_private) else klarName(g)) + (if (halb) " ½" else "") + jahre(g).let { if (it.isNotEmpty()) "  $it" else "" }
    if (g.isPrivate) {
        Text(text, Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Anklickbar(g, a) { klick ->
        Text(text, Modifier.background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .border(1.dp, if (g.xref == a.state.selected) MaterialTheme.colorScheme.primary else treeColors.forSex(g.sex).copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .then(klick).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Ein Kind als Kasten: Bild, Name, Geburt und Tod; darunter je Partnerschaft der Partner (anklickbar) und die Enkel als
 * kleine Reiter. Ohne Nachfahrendaten nur die Ehen aus der Personenabfrage. Doppelklick fuehrt in die Familie des Kindes.
 */
@Composable
private fun KindKarte(k: Person, node: DescendantNode?, a: KartenAktionen) {
    val shape = MaterialTheme.shapes.small
    val farben = MaterialTheme.colorScheme
    Column(Modifier.width(230.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Anklickbar(k, a) { klick ->
            Column(Modifier.fillMaxWidth().kartenRahmen(k, a, shape, dick = false).then(klick).padding(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(k, 44.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(if (k.isPrivate) stringResource(Res.string.person_private) else k.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (!k.isPrivate) {
                    Spacer(Modifier.height(4.dp))
                    ereignisZeile("*", k.birth ?: k.chr)?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    ereignisZeile("†", k.death ?: k.buri)?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        if (k.isPrivate) return@Column
        // Partner und Enkel
        if (node != null && node.families.isNotEmpty()) node.families.forEach { f ->
            Column(Modifier.fillMaxWidth().padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val jahr = f.marriage?.date?.year?.takeIf { it > 0 }?.let { " ($it)" }.orEmpty()
                val sp = f.spouse
                if (sp != null && !sp.isPrivate) Anklickbar(sp, a) { klick ->
                    Text("⚭ " + klarName(sp) + jahr, Modifier.then(klick).padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.bodySmall,
                        color = farben.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else Text("⚭ " + (if (sp?.isPrivate == true) stringResource(Res.string.person_private) else "?") + jahr, style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant)
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    f.children.forEach { e -> GeschwisterChip(e.person, false, a) }
                }
            }
        } else k.marriages.forEach { m ->
            Text("⚭ " + m.spouse.ifBlank { "?" } + (m.date?.year?.takeIf { it > 0 }?.let { " ($it)" }.orEmpty()), Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.bodySmall, color = farben.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
