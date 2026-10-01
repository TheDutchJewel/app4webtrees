package de.bgghome.webtrees.nativ.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.data.Heiratsart
import de.bgghome.webtrees.nativ.data.PatenEintrag
import de.bgghome.webtrees.nativ.data.PatenRolle
import de.bgghome.webtrees.nativ.data.artZusatz
import de.bgghome.webtrees.nativ.data.patenGruppen
import de.bgghome.webtrees.nativ.res.*
import org.jetbrains.compose.resources.stringResource

/*
 * Die Paten-/Trauzeugen-Zeile unter einem Ereignis, fuer Lebenslauf, Personenblatt und Familienansicht:
 *   Paten:  Louise Falkenrath (Patin) · Ernst Falkenrath (Pate) ⓘ ⧉ · Friedrich Plate, Anbauer zu Celle
 * Verlinkte Paten sind anklickbar (oeffnen die Person), freie stehen als Text, private nur als "privat".
 * ⓘ klappt die Notiz am Paten auf, ⧉ oeffnet die Quelle (am Desktop) oder klappt sie als Text auf.
 */

/** "Paten", "Trauzeugen" oder "Beteiligte" als Ueberschrift der Gruppe. */
@Composable
fun patenUeberschrift(rolle: PatenRolle): String = stringResource(
    when (rolle) {
        PatenRolle.Paten -> Res.string.assoc_godparents
        PatenRolle.Trauzeugen -> Res.string.assoc_witnesses
        PatenRolle.Beteiligte -> Res.string.assoc_others
    }
)

/** Uebersetzung der Heiratsart fuer Module ohne typeLabel (vor Stufe 19). */
@Composable
fun heiratsartText(art: Heiratsart): String = stringResource(
    when (art) {
        Heiratsart.Standesamtlich -> Res.string.marr_civil
        Heiratsart.Kirchlich -> Res.string.marr_religious
        Heiratsart.Partnerschaft -> Res.string.marr_partners
        Heiratsart.OhneTrauschein -> Res.string.marr_common
    }
)

/** Ereignisname mit Art: "Heirat · kirchlich" - oder nur der Name, wenn er die Art schon enthaelt. */
@Composable
fun faktLabelMitArt(fact: FactJson): String {
    val civil = stringResource(Res.string.marr_civil); val reli = stringResource(Res.string.marr_religious)
    val partners = stringResource(Res.string.marr_partners); val common = stringResource(Res.string.marr_common)
    val zusatz = fact.artZusatz { when (it) { Heiratsart.Standesamtlich -> civil; Heiratsart.Kirchlich -> reli; Heiratsart.Partnerschaft -> partners; Heiratsart.OhneTrauschein -> common } }
    return if (zusatz == null) fact.label else fact.label + " · " + zusatz
}

/**
 * Alle Gruppen (Paten, Trauzeugen, Beteiligte) des Ereignisses als Zeilen; nichts ohne Paten.
 * [onPerson] oeffnet einen verlinkten Paten; [zeigeLevel1] haengt "an der Person erfasst" an (Desktop, Redakteure).
 */
@Composable
fun PatenZeilen(fact: FactJson, onPerson: (String) -> Unit, zeigeLevel1: Boolean = false, modifier: Modifier = Modifier) {
    val gruppen = fact.patenGruppen()
    if (gruppen.isEmpty()) return
    Column(modifier) {
        gruppen.forEach { g -> PatenZeile(patenUeberschrift(g.rolle), g.eintraege, onPerson, zeigeLevel1) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatenZeile(titel: String, eintraege: List<PatenEintrag>, onPerson: (String) -> Unit, zeigeLevel1: Boolean) {
    val colors = MaterialTheme.colorScheme
    val stil = MaterialTheme.typography.bodySmall
    val privat = stringResource(Res.string.person_private)
    // Welche Notizen/Quellen gerade aufgeklappt sind (je Eintrag, nach Position)
    var offen by remember(eintraege) { mutableStateOf(setOf<Int>()) }
    val oeffnen = LocalSourceOpener.current
    FlowRow(Modifier.fillMaxWidth(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        Text("$titel: ", style = stil, color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
        eintraege.forEachIndexed { i, e ->
            if (i > 0) Text(" · ", style = stil, color = colors.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.anzeige(privat),
                    modifier = if (e.verlinkt) Modifier.clickable { onPerson(e.xref!!) } else Modifier,
                    style = stil,
                    color = if (e.verlinkt) colors.primary else if (e.privat) colors.onSurfaceVariant else colors.onSurface,
                    textDecoration = if (e.verlinkt) TextDecoration.Underline else null,
                )
                if (e.notes.isNotEmpty()) Icon(
                    Icons.Outlined.Info, contentDescription = stringResource(Res.string.assoc_note),
                    Modifier.padding(start = 2.dp).size(14.dp).clickable { offen = if (i in offen) offen - i else offen + i },
                    tint = colors.onSurfaceVariant,
                )
                if (e.sources.isNotEmpty()) Icon(
                    SourceIcon, contentDescription = stringResource(Res.string.desk_tab_sources),
                    Modifier.padding(start = 2.dp).size(14.dp).clickable {
                        val q = e.sources.firstOrNull { !it.istText }
                        if (oeffnen != null && q != null) oeffnen(q.xref) else offen = if (i in offen) offen - i else offen + i
                    },
                    tint = colors.onSurfaceVariant,
                )
                if (zeigeLevel1 && e.level1) Text(" (" + stringResource(Res.string.assoc_level1) + ")", style = stil, color = colors.onSurfaceVariant)
            }
        }
    }
    // Aufgeklappte Notizen und Quellen, eingerueckt unter der Zeile
    eintraege.forEachIndexed { i, e ->
        if (i !in offen) return@forEachIndexed
        Column(Modifier.padding(start = 16.dp)) {
            e.notes.forEach { Text(e.anzeige(privat) + ": " + it, style = stil, color = colors.onSurfaceVariant) }
            e.sources.forEach { q ->
                val klickbar = oeffnen != null && !q.istText
                Text(
                    stringResource(Res.string.fact_sources, q.mitSeite()),
                    modifier = if (klickbar) Modifier.clickable { oeffnen(q.xref) } else Modifier,
                    style = MaterialTheme.typography.labelSmall, color = if (klickbar) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}
