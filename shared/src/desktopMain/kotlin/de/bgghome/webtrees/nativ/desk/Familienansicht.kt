package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.EventJson
import de.bgghome.webtrees.nativ.api.FamilyJson
import de.bgghome.webtrees.nativ.api.IndividualDetail
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.Avatar
import de.bgghome.webtrees.nativ.ui.UiState
import de.bgghome.webtrees.nativ.ui.forSex
import de.bgghome.webtrees.nativ.ui.requestAddRelative
import de.bgghome.webtrees.nativ.ui.select
import de.bgghome.webtrees.nativ.ui.setRoot
import de.bgghome.webtrees.nativ.ui.treeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/*
 * Familienansicht: das Paar in der Mitte, darueber die Eltern beider Partner, darunter die Kinder. Hat der Proband
 * mehrere Ehen, steht jede auf einem eigenen Reiter. Ein Klick waehlt eine Person (Personentafel rechts), ein
 * Doppelklick macht sie zum Probanden - so geht man durch die Familien nach oben und unten.
 * Die Daten kommen aus der Personenabfrage des Probanden und seines Partners, nicht aus dem Baum-Export.
 */

/** Vorauswahl fuer den Dialog "Verwandte hinzufuegen", gesetzt vom leeren Feld "Vater/Mutter unbekannt". */
object Verwandtenwahl {
    var vorwahl: String? by mutableStateOf(null)
    var familie: String? by mutableStateOf(null)
    fun leeren() { vorwahl = null; familie = null }
}

/** Die Familie des Probanden auf dem gewaehlten Reiter; Eltern je Partner aus dessen erster Herkunftsfamilie. */
private class PaarDaten(val proband: IndividualDetail, val partner: IndividualDetail?)

private fun ereignisZeile(zeichen: String, e: EventJson?): String? {
    val datum = e?.date?.text.orEmpty(); val ort = e?.place?.name.orEmpty()
    return if (datum.isBlank() && ort.isBlank()) null else "$zeichen " + listOf(datum, ort).filter(String::isNotBlank).joinToString(", ")
}

@Composable
fun DeskFamilie(state: UiState, viewModel: AppViewModel, openWeb: (String) -> Unit) {
    val root = state.root
    val tree = state.tree?.name
    var neu by remember { mutableIntStateOf(0) }
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

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val fehler = proband?.exceptionOrNull()
        when {
            root == null -> {}
            fehler != null -> Text(fehler.message ?: "?", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
            p == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> Familie(state, viewModel, openWeb, PaarDaten(p, partner?.takeIf { it.person.xref == partnerXref }), familien, familie, reiter) { reiter = it }
        }
    }
}

@Composable
private fun Familie(
    state: UiState, viewModel: AppViewModel, openWeb: (String) -> Unit, daten: PaarDaten,
    familien: List<FamilyJson>, familie: FamilyJson?, reiter: Int, onReiter: (Int) -> Unit,
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
    val karte = KartenAktionen(state, viewModel, openWeb, canEdit)
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 24.dp, vertical = 16.dp)) {
            // ── Eltern beider Partner ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                listOf(mann, frau).forEach { kind ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (kind != null && !kind.isPrivate) {
                            val eltern = elternVon(kind)
                            Text(stringResource(Res.string.desk_family_parents_of, klarName(kind)), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.weight(1f)) { PersonKarte(eltern?.husband, stringResource(Res.string.desk_unknown_father), false, karte, kind.xref, "father") }
                                Box(Modifier.weight(1f)) { PersonKarte(eltern?.wife, stringResource(Res.string.desk_unknown_mother), false, karte, kind.xref, "mother") }
                            }
                            Linie()
                        }
                    }
                }
            }

            // ── Reiter: eine Ehe je Reiter ──
            if (familien.size > 1) {
                PrimaryScrollableTabRow(selectedTabIndex = reiter.coerceAtMost(familien.lastIndex), edgePadding = 0.dp, containerColor = MaterialTheme.colorScheme.background) {
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

            // ── Das Paar ──
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { PersonKarte(mann, unbekannterPartner("F"), true, karte, if (familie == null) p.xref else null, "spouse") }
                Column(Modifier.width(190.dp).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (familie != null) {
                        Text("⚭", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val datum = familie.marriage?.date?.text.orEmpty(); val ort = familie.marriage?.place?.name.orEmpty()
                        if (datum.isNotBlank()) Text(datum, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        if (ort.isNotBlank()) Text(ort, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    } else {
                        Text(stringResource(Res.string.desk_family_no_partner), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                Box(Modifier.weight(1f)) { PersonKarte(frau, unbekannterPartner("M"), true, karte, if (familie == null) p.xref else null, "spouse") }
            }

            // ── Kinder ──
            val kinder = familie?.children.orEmpty()
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.desk_family_children, kinder.size), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                if (canEdit) TextButton(onClick = { Verwandtenwahl.vorwahl = "child"; Verwandtenwahl.familie = familie?.xref; viewModel.requestAddRelative(p.xref) }) { Text("+ " + stringResource(Res.string.action_add_relative)) }
            }
            if (kinder.isEmpty()) {
                Text(stringResource(Res.string.desk_family_no_children), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                kinder.forEach { k -> KindZeile(k, karte) }
            }
        }
        SenkrechteLeiste(scroll)
    }
}

/** Was eine Karte beim Klicken, Doppelklicken und im Kontextmenue tut. */
private class KartenAktionen(val state: UiState, val viewModel: AppViewModel, val openWeb: (String) -> Unit, val canEdit: Boolean)

@Composable
private fun Linie() {
    Box(Modifier.padding(top = 4.dp).width(2.dp).height(14.dp).background(treeColors.connector))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Anklickbar(person: Person, a: KartenAktionen, inhalt: @Composable (Modifier) -> Unit) {
    ContextMenuArea(items = {
        buildList {
            add(ContextMenuItem(Texte.t(Res.string.action_make_root)) { a.viewModel.setRoot(person.xref) })
            if (a.canEdit) add(ContextMenuItem(Texte.t(Res.string.action_add_relative)) { a.viewModel.requestAddRelative(person.xref) })
            add(ContextMenuItem(Texte.t(Res.string.chip_open_web)) { a.openWeb(person.url) })
        }
    }) {
        inhalt(Modifier.fokusRahmen().combinedClickable(onDoubleClick = { a.viewModel.setRoot(person.xref) }) { a.viewModel.select(person.xref) })
    }
}

/**
 * Eine Person als Karte; gross fuer das Paar (mit Geburt, Tod und Beruf), klein fuer die Eltern. Fehlt sie, steht dort
 * [leer] ("Vater unbekannt") - mit Bearbeitungsrecht und [ergaenzenFuer] als Knopf, der Verwandte hinzufuegen oeffnet.
 */
@Composable
private fun PersonKarte(person: Person?, leer: String, gross: Boolean, a: KartenAktionen, ergaenzenFuer: String?, beziehung: String? = null) {
    val shape = MaterialTheme.shapes.medium
    val hoehe: Dp = if (gross) 128.dp else 64.dp
    if (person == null) {
        Box(Modifier.fillMaxWidth().height(hoehe).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(leer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (a.canEdit && ergaenzenFuer != null) TextButton(onClick = { Verwandtenwahl.vorwahl = beziehung; a.viewModel.requestAddRelative(ergaenzenFuer) }, modifier = Modifier.height(30.dp)) {
                    Text("+ " + stringResource(Res.string.desk_family_add), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        return
    }
    val farbe = treeColors.forSex(person.sex)
    val proband = person.xref == a.state.root
    val gewaehlt = person.xref == a.state.selected
    Anklickbar(person, a) { klick ->
        Row(
            Modifier.fillMaxWidth().height(hoehe)
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(if (gewaehlt) 2.5.dp else if (proband) 2.dp else 1.dp, if (gewaehlt) MaterialTheme.colorScheme.primary else farbe, shape)
                .then(klick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(person, if (gross) 72.dp else 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                val name = if (person.isPrivate) stringResource(Res.string.person_private) else person.name
                Text(name, style = if (gross) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    fontWeight = if (proband) FontWeight.Bold else FontWeight.SemiBold, maxLines = if (gross) 2 else 1, overflow = TextOverflow.Ellipsis)
                if (!person.isPrivate) {
                    if (gross) {
                        listOfNotNull(ereignisZeile("*", person.birth ?: person.chr), ereignisZeile("†", person.death ?: person.buri), person.occupation?.takeIf { it.isNotBlank() })
                            .forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    } else if (jahre(person).isNotEmpty()) {
                        Text(jahre(person), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Ein Kind: Bild, Name, Geburt und Tod, dahinter seine Ehen - Doppelklick fuehrt in seine Familie. */
@Composable
private fun KindZeile(k: Person, a: KartenAktionen) {
    val shape = MaterialTheme.shapes.small
    val gewaehlt = k.xref == a.state.selected
    Anklickbar(k, a) { klick ->
        Row(
            Modifier.fillMaxWidth().widthIn(min = 400.dp)
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(if (gewaehlt) 2.dp else 1.dp, if (gewaehlt) MaterialTheme.colorScheme.primary else treeColors.forSex(k.sex).copy(alpha = 0.6f), shape)
                .then(klick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(k, 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(if (k.isPrivate) stringResource(Res.string.person_private) else k.name, Modifier.weight(1.3f),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!k.isPrivate) {
                Text(ereignisZeile("*", k.birth ?: k.chr).orEmpty(), Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ereignisZeile("†", k.death ?: k.buri).orEmpty(), Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val ehen = k.marriages.map { m -> "oo " + m.spouse.ifBlank { "?" } + (m.date?.year?.takeIf { it > 0 }?.let { " ($it)" }.orEmpty()) }
                Text(ehen.joinToString("  "), Modifier.weight(1.4f), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
