package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.res.*
import org.jetbrains.compose.resources.stringResource

/*
 * Einstellungen fuer den Grossdruck im Fenster "Tafel erstellen" (B5, 28.09.2026): Groesse (Original, Breite in cm,
 * auf Spalten x Zeilen Blaetter) fuer die Klebe-Blaetter, Rollenbreite fuer den Plotter. Gilt fuer alle Tafeln.
 */

/** [art]: 0 Originalgroesse, 1 Breite in cm, 2 auf Blaetter. [rolle]: Rollenbreite in cm als Text (auch frei). */
data class DruckEinstellung(
    val art: Int = 0, val breite: String = "100", val spalten: Int = 3, val zeilen: Int = 2,
    val rolle: String = "91.4", val einpassen: Boolean = true,
) {
    fun groesse(): DruckGroesse = when (art) {
        1 -> breite.replace(',', '.').toFloatOrNull()?.takeIf { it in 5f..5000f }?.let { DruckGroesse.Breite(it) } ?: DruckGroesse.Original
        2 -> DruckGroesse.Blaetter(spalten, zeilen)
        else -> DruckGroesse.Original
    }
    fun rolleCm(): Float? = rolle.replace(',', '.').toFloatOrNull()?.takeIf { it in 20f..500f }
}

object DruckWahl {
    private val prefs get() = DeskLayout.prefs
    fun laden() = DruckEinstellung(
        art = prefs.getString("druck_art", null)?.toIntOrNull()?.coerceIn(0, 2) ?: 0,
        breite = prefs.getString("druck_breite", null) ?: "100",
        spalten = prefs.getString("druck_spalten", null)?.toIntOrNull()?.coerceIn(1, 20) ?: 3,
        zeilen = prefs.getString("druck_zeilen", null)?.toIntOrNull()?.coerceIn(1, 20) ?: 2,
        rolle = prefs.getString("druck_rolle", null) ?: "91.4",
        einpassen = prefs.getBoolean("druck_einpassen", true),
    )
    fun sichern(d: DruckEinstellung) {
        prefs.putString("druck_art", d.art.toString()); prefs.putString("druck_breite", d.breite)
        prefs.putString("druck_spalten", d.spalten.toString()); prefs.putString("druck_zeilen", d.zeilen.toString())
        prefs.putString("druck_rolle", d.rolle); prefs.putBoolean("druck_einpassen", d.einpassen)
    }
}

/** Uebliche Rollenbreiten der Plotter in cm (24", 36", 42", 60"). */
private val ROLLEN = listOf("61", "91.4", "106.7", "152.4")

private fun zeigeCm(t: String) = t.replace(".", Texte.t(Res.string.desk_decimal_point))

@Composable
internal fun GrossdruckWahl(
    d: DruckEinstellung, onAendern: (DruckEinstellung) -> Unit, info: TafelInfo?, bereit: Boolean,
    onBlaetterDrucken: () -> Unit, onBlaetterPdf: () -> Unit, onPlotter: () -> Unit,
) {
    Text(stringResource(Res.string.desk_print_heading), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Einstellung(stringResource(Res.string.desk_print_size)) {
        val namen = listOf(Res.string.desk_print_size_original, Res.string.desk_print_size_width, Res.string.desk_print_size_sheets).map { stringResource(it) }
        Auswahl(namen[d.art], namen) { onAendern(d.copy(art = namen.indexOf(it))) }
    }
    if (d.art == 1) Einstellung(stringResource(Res.string.desk_print_width_cm)) {
        OutlinedTextField(d.breite, { onAendern(d.copy(breite = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6))) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    if (d.art == 2) Einstellung(stringResource(Res.string.desk_print_sheets)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            val zahlen = (1..20).map { it.toString() }
            Box(Modifier.weight(1f)) { Auswahl(d.spalten.toString(), zahlen) { onAendern(d.copy(spalten = it.toInt())) } }
            Text("×")
            Box(Modifier.weight(1f)) { Auswahl(d.zeilen.toString(), zahlen) { onAendern(d.copy(zeilen = it.toInt())) } }
        }
    }
    // Endmass und Blattzahl aus der Seite der Vorschau - ohne die Blaetter zu erzeugen
    val grund = info?.takeIf { it.seiteB > 0f }
    val ber = grund?.let { it.bereich ?: Bereich(0f, 0f, it.seiteB, it.seiteH) }
    if (grund != null && ber != null) {
        val p = kachelPlan(ber.b, ber.h, grund.einheit, d.groesse())
        Text(stringResource(Res.string.desk_print_sheets_info, p.endBCm, p.endHCm, p.spalten * p.zeilen, p.spalten, p.zeilen),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Knopf(stringResource(Res.string.desk_chart_print_tiles), bereit && grund != null, onBlaetterDrucken)
    Knopf(stringResource(Res.string.desk_chart_pdf_tiles), bereit && grund != null, onBlaetterPdf)
    Einstellung(stringResource(Res.string.desk_print_roll)) {
        val andere = stringResource(Res.string.desk_print_roll_other)
        val werte = ROLLEN.map { "${zeigeCm(it)} cm" } + andere
        val jetzt = if (d.rolle in ROLLEN) "${zeigeCm(d.rolle)} cm" else andere
        Auswahl(jetzt, werte) { w -> onAendern(d.copy(rolle = if (w == andere) "" else ROLLEN[werte.indexOf(w)])) }
    }
    if (d.rolle !in ROLLEN) Einstellung(stringResource(Res.string.desk_print_roll_cm)) {
        OutlinedTextField(zeigeCm(d.rolle), { onAendern(d.copy(rolle = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.replace(',', '.').take(6))) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
    Haken(stringResource(Res.string.desk_print_roll_fit), d.einpassen) { onAendern(d.copy(einpassen = it)) }
    val rolle = d.rolleCm()
    if (grund != null && ber != null && rolle != null) {
        val r = rollenPlan(ber.b, ber.h, grund.einheit, rolle, d.groesse(), d.einpassen)
        val laenge = dezimal(r.laenge / 72f * 0.0254f, 2)
        Text(if (r.bahnen > 1) stringResource(Res.string.desk_print_roll_info_strips, r.endBCm, r.endHCm, r.bahnen, laenge)
            else stringResource(Res.string.desk_print_roll_info, r.endBCm, r.endHCm, laenge),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Knopf(stringResource(Res.string.desk_print_roll_pdf), bereit && grund != null && rolle != null, onPlotter)
}
