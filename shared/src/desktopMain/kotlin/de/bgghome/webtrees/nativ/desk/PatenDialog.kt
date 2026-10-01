package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.api.AssociationRequest
import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.FreeAssociateRequest
import de.bgghome.webtrees.nativ.api.LinkedAssociateRequest
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.Field
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import de.bgghome.webtrees.nativ.ui.saveAssociation
import org.jetbrains.compose.resources.stringResource

/*
 * Paten, Trauzeugen und andere Beteiligte eines Ereignisses bearbeiten (API-Stufe 20, Route Association).
 * Eine Liste in Dateireihenfolge: verknuepfte Personen (anklickbar im Baum) und Personen ohne Datensatz (Text wie im
 * Kirchenbuch, gespeichert als GEDCOM-L _GODP/_WITN). Gesendet wird nur, was sich geaendert hat - unveraenderte
 * Teile (etwa eine alte Notiz "Paten: ...") bleiben in ihrer Form; Quellen an einem Paten bleiben immer.
 */

/** Ein Eintrag im Dialog. [xref] null = ohne Datensatz ([text]); [level1] = "1 ASSO" an der Person (nur mit Uebernahme aenderbar). */
private data class PatenEingabe(
    val xref: String?,
    val name: String,
    val privat: Boolean = false,
    val role: String,
    val rela: String = "",
    val note: String = "",
    val noteAlt: String = "",
    val text: String = "",
    val level1: Boolean = false,
)

/** Welche Rolle ein neuer Eintrag bekommt: Taufe Pate, Heirat Zeuge, sonst Andere (frei: Zeuge). */
private fun vorgabeRolle(tag: String, frei: Boolean): String = when (tag) {
    "CHR", "BAPM" -> "godparent"
    "MARR" -> "witness"
    else -> if (frei) "witness" else "other"
}

/** Titel des Knopfs im Detailbereich, je nach Ereignis. */
@Composable
internal fun patenKnopfText(tag: String): String = stringResource(
    when (tag) {
        "CHR", "BAPM" -> Res.string.assoc_edit_godparents
        "MARR" -> Res.string.assoc_edit_witnesses
        else -> Res.string.assoc_edit_others
    }
)

@Composable
private fun rolleText(role: String): String = stringResource(
    when (role) {
        "godparent" -> Res.string.assoc_role_godparent
        "witness" -> Res.string.assoc_role_witness
        else -> Res.string.assoc_role_other
    }
)

/** Der Dialog aus dem Personenblatt: sucht ueber den Server und speichert ueber das ViewModel. */
@Composable
internal fun PatenDialog(fact: FactJson, record: String, titel: String, tree: String, viewModel: AppViewModel, onDismiss: () -> Unit) {
    PatenDialog(
        fact, record, titel,
        suche = { q -> runCatching { viewModel.client.individuals(tree, q, 1).data }.getOrNull().orEmpty() },
        onSpeichern = { a, fertig -> viewModel.saveAssociation(record, a) { fehler -> fertig(!fehler) } },
        onDismiss = onDismiss,
    )
}

/**
 * Der Dialog. [record]: Person oder Familie, an der das Ereignis haengt; [titel]: Ereignis wie im Blatt ("Taufe").
 * [suche] findet Personen nach Namen, [onSpeichern] schreibt und meldet, ob es geklappt hat.
 */
@Composable
internal fun PatenDialog(
    fact: FactJson, record: String, titel: String,
    suche: suspend (String) -> List<de.bgghome.webtrees.nativ.api.Person>,
    onSpeichern: (AssociationRequest, fertig: (ok: Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    val privatText = stringResource(Res.string.person_private)
    val original = remember(fact.id) {
        fact.associates.map {
            val notiz = it.notes.joinToString("\n")
            PatenEingabe(it.xref, it.name ?: privatText, it.isPrivate, it.role, if (it.role == "other") it.rela else "", notiz, notiz, level1 = it.level1)
        } + fact.freeAssociates.map { PatenEingabe(null, "", role = if (it.role == "witness") "witness" else "godparent", text = it.text) }
    }
    val zeilen = remember(fact.id) { mutableStateListOf<PatenEingabe>().also { it.addAll(original) } }
    var gewaehlt by remember(fact.id) { mutableStateOf(if (original.isEmpty()) -1 else 0) }
    var suchen by remember { mutableStateOf(false) }
    val level1Anzahl = zeilen.count { it.level1 }
    var uebernehmen by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    fun verschieben(i: Int, ziel: Int) {
        if (ziel !in zeilen.indices) return
        val z = zeilen.removeAt(i); zeilen.add(ziel, z); gewaehlt = ziel
    }

    fun anfrage(): AssociationRequest? {
        fun verknuepft(liste: List<PatenEingabe>, mitLevel1: Boolean) = liste.filter { it.xref != null && (!it.level1 || mitLevel1) }
        val linkedNeu = verknuepft(zeilen, uebernehmen)
        val linkedAlt = verknuepft(original, false)
        val linkedGeaendert = uebernehmen || linkedNeu.map { listOf(it.xref, it.role, it.rela.trim(), it.note.trim()) } !=
            linkedAlt.map { listOf(it.xref, it.role, it.rela.trim(), it.note.trim()) }
        val freiNeu = zeilen.filter { it.xref == null && it.text.isNotBlank() }.map { it.text.trim() to it.role }
        val freiAlt = original.filter { it.xref == null }.map { it.text.trim() to it.role }
        if (!linkedGeaendert && freiNeu == freiAlt) return null
        return AssociationRequest(
            factId = fact.id,
            linked = linkedNeu.takeIf { linkedGeaendert }?.map {
                LinkedAssociateRequest(it.xref!!, it.role, it.rela.trim().takeIf { r -> it.role == "other" && r.isNotEmpty() },
                    it.note.trim().takeIf { n -> n != it.noteAlt.trim() })
            },
            free = freiNeu.takeIf { it != freiAlt }?.map { (text, role) -> FreeAssociateRequest(text, role) },
            convertLevel1 = true.takeIf { uebernehmen },
        )
    }
    // "Andere" braucht eine Rolle als Text, sonst nimmt der Server den Eintrag nicht an
    val gueltig = zeilen.none { it.xref != null && it.role == "other" && it.rela.isBlank() && (!it.level1 || uebernehmen) }

    WtAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.assoc_dialog_title, patenKnopfText(fact.tag).trimEnd(' ', '…'), titel)) },
        text = {
            Column(Modifier.width(620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // ── Liste ──
                Column(Modifier.fillMaxWidth().heightIn(min = 60.dp).border(1.dp, colors.outlineVariant, MaterialTheme.shapes.small)) {
                    if (zeilen.isEmpty()) Text(stringResource(Res.string.assoc_none), Modifier.padding(10.dp), color = colors.onSurfaceVariant)
                    zeilen.forEachIndexed { i, z ->
                        val aktiv = i == gewaehlt
                        Row(
                            Modifier.fillMaxWidth().background(if (aktiv) colors.secondaryContainer else colors.surface).clickable { gewaehlt = i }
                                .padding(start = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                                Text(
                                    if (z.xref == null) z.text.ifBlank { "…" } else z.name,
                                    style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal,
                                    fontStyle = if (z.privat) FontStyle.Italic else null,
                                )
                                val zusatz = listOfNotNull(
                                    if (z.role == "other" && z.rela.isNotBlank()) z.rela else rolleText(z.role),
                                    if (z.xref == null) stringResource(Res.string.assoc_free_label) else null,
                                    if (z.level1) stringResource(Res.string.assoc_level1) else null,
                                    z.note.lineSequence().firstOrNull()?.takeIf(String::isNotBlank),
                                ).joinToString(" · ")
                                Text(zusatz, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            // Eintraege an der Person (1 ASSO) bleiben dort, bis sie in die Taufe uebernommen werden
                            val beweglich = !z.level1 || uebernehmen
                            Tipp(stringResource(Res.string.assoc_up)) { IconButton(onClick = { verschieben(i, i - 1) }, enabled = beweglich && i > 0) { Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(18.dp)) } }
                            Tipp(stringResource(Res.string.assoc_down)) { IconButton(onClick = { verschieben(i, i + 1) }, enabled = beweglich && i < zeilen.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp)) } }
                            Tipp(stringResource(Res.string.assoc_remove)) {
                                IconButton(onClick = { zeilen.removeAt(i); gewaehlt = (gewaehlt.coerceAtMost(zeilen.lastIndex)) }, enabled = beweglich) {
                                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // ── Der gewaehlte Eintrag ──
                zeilen.getOrNull(gewaehlt)?.let { z ->
                    val i = gewaehlt
                    fun setzen(neu: PatenEingabe) { zeilen[i] = neu }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.assoc_role) + ":", style = MaterialTheme.typography.labelMedium)
                        var menue by remember(i) { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { menue = true }, enabled = !z.level1 || uebernehmen, shape = MaterialTheme.shapes.small) {
                                Text(if (z.role == "other" && z.rela.isNotBlank()) z.rela else rolleText(z.role))
                            }
                            DropdownMenu(expanded = menue, onDismissRequest = { menue = false }) {
                                (if (z.xref == null) listOf("godparent", "witness") else listOf("godparent", "witness", "other")).forEach { r ->
                                    DropdownMenuItem(text = { Text(rolleText(r)) }, onClick = { menue = false; setzen(z.copy(role = r)) })
                                }
                            }
                        }
                    }
                    if (z.xref == null) {
                        Field(z.text, { setzen(z.copy(text = it)) }, Res.string.assoc_add_free, hint = Res.string.assoc_free_hint)
                    } else if (!z.privat && (!z.level1 || uebernehmen)) {
                        if (z.role == "other") Field(z.rela, { setzen(z.copy(rela = it)) }, Res.string.assoc_rela_hint)
                        Field(z.note, { setzen(z.copy(note = it)) }, Res.string.assoc_note_hint, minLines = 2)
                    }
                }

                // ── Hinzufuegen ──
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { suchen = !suchen }, shape = MaterialTheme.shapes.small) { Text(stringResource(Res.string.assoc_add_person)) }
                    OutlinedButton(onClick = {
                        zeilen.add(PatenEingabe(null, "", role = vorgabeRolle(fact.tag, true))); gewaehlt = zeilen.lastIndex; suchen = false
                    }, shape = MaterialTheme.shapes.small) { Text(stringResource(Res.string.assoc_add_free)) }
                }
                if (suchen) {
                    PersonSuche(null, { p ->
                        zeilen.add(PatenEingabe(p.xref, p.name, p.isPrivate, vorgabeRolle(fact.tag, false))); gewaehlt = zeilen.lastIndex; suchen = false
                    }) { q -> suche(q).filter { it.xref != record } }
                }

                if (level1Anzahl > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = uebernehmen, onCheckedChange = { uebernehmen = it })
                        Text(stringResource(Res.string.assoc_convert, level1Anzahl), Modifier.clickable { uebernehmen = !uebernehmen }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = gueltig, onClick = {
                val a = anfrage()
                if (a == null) onDismiss() else onSpeichern(a) { ok -> if (ok) onDismiss() }
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
