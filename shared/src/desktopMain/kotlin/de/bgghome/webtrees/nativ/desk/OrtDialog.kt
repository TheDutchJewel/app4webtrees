package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.bgghome.webtrees.nativ.api.PlaceDetail
import de.bgghome.webtrees.nativ.api.PlaceRequest
import de.bgghome.webtrees.nativ.api.WriteResult
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.Field
import de.bgghome.webtrees.nativ.ui.WtAlertDialog
import de.bgghome.webtrees.nativ.ui.karte.GeoPunkt
import de.bgghome.webtrees.nativ.ui.karte.KachelEbene
import de.bgghome.webtrees.nativ.ui.karte.KachelKarte
import de.bgghome.webtrees.nativ.ui.karte.KartenPin
import de.bgghome.webtrees.nativ.ui.karte.KartenZustand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import java.util.concurrent.TimeUnit

/*
 * Ortsdaten bearbeiten (ab API-Stufe 22): GOV-Kennung, Notiz und Koordinaten im Ortsdatensatz (_LOC). Die Koordinaten
 * lassen sich ueber OpenStreetMap (Nominatim) suchen - ein Klick auf einen Treffer uebernimmt Breite und Laenge.
 * Administratoren koennen sie zusaetzlich in die Geografischen Daten von webtrees schreiben, die die Karten im Browser
 * lesen.
 */

/** Ein Treffer der Ortssuche. */
internal data class OrtTreffer(val name: String, val lat: Double, val lng: Double)

/** Nominatim (OpenStreetMap): hoechstens 10 Treffer, wahrscheinlichste zuerst. Ehrlicher User-Agent ist Pflicht. */
internal fun nominatim(frage: String, userAgent: String, sprache: String = Locale.getDefault().language): List<OrtTreffer> {
    val url = "https://nominatim.openstreetmap.org/search".toHttpUrl().newBuilder()
        .addQueryParameter("q", frage).addQueryParameter("format", "jsonv2").addQueryParameter("limit", "10")
        .addQueryParameter("accept-language", sprache).build()
    val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    client.newCall(Request.Builder().url(url).header("User-Agent", userAgent).build()).execute().use { antwort ->
        if (!antwort.isSuccessful) error("OpenStreetMap: HTTP ${antwort.code}")
        return Json.parseToJsonElement(antwort.body?.string().orEmpty().ifEmpty { "[]" }).jsonArray.mapNotNull { e ->
            val o = e.jsonObject
            val lat = o["lat"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val lng = o["lon"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
            val name = o["display_name"]?.jsonPrimitive?.contentOrNull
            if (lat == null || lng == null || name == null) null else OrtTreffer(name, lat, lng)
        }
    }
}

/** "53,778417" oder "53.778417" -> 53.778417; leer -> null */
internal fun grad(text: String): Double? = text.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()

@Composable
fun OrtDialog(tree: String, ort: PlaceDetail, client: WtClient, istAdmin: Boolean, openWeb: (String) -> Unit,
              onDismiss: () -> Unit, onSaved: (WriteResult) -> Unit) {
    val loc = ort.location
    val altGov = loc?.gov.orEmpty()
    val altNotiz = loc?.notes?.firstOrNull().orEmpty()
    // Vorbelegt mit den Koordinaten des _LOC - fehlen sie dort, mit den gezeigten (Geografische Daten oder Ereignis)
    val altLat = loc?.lat ?: ort.lat
    val altLng = loc?.lng ?: ort.lng
    var gov by remember { mutableStateOf(altGov) }
    var notiz by remember { mutableStateOf(altNotiz) }
    var lat by remember { mutableStateOf(altLat?.let { dezimalEinzeln(it) }.orEmpty()) }
    var lng by remember { mutableStateOf(altLng?.let { dezimalEinzeln(it) }.orEmpty()) }
    var mapData by remember { mutableStateOf(istAdmin) }
    var suche by remember { mutableStateOf<String?>(null) }
    var treffer by remember { mutableStateOf<List<OrtTreffer>?>(null) }
    var sucht by remember { mutableStateOf(false) }
    var speichert by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val b = grad(lat); val l = grad(lng)
    val koordOk = (lat.isBlank() && lng.isBlank()) || (b != null && l != null && b in -90.0..90.0 && l in -180.0..180.0)
    // Koordinaten zaehlen als geaendert, wenn sie vom _LOC abweichen (auch wenn sie bisher nur woanders standen)
    val koordGeaendert = koordOk && (b != loc?.lat || l != loc?.lng)

    fun sucheStarten(frage: String) {
        sucht = true; fehler = null
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { nominatim(frage, client.userAgent) } }
                .onSuccess { treffer = it }.onFailure { fehler = it.message ?: "?" }
            sucht = false
        }
    }

    WtAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.desk_place_edit) + " – " + ort.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.width(620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { Field(gov, { gov = it.trim() }, Res.string.desk_place_gov, hint = Res.string.desk_place_gov_hint) }
                    TextButton(onClick = { openWeb("https://gov.genealogy.net/search/name?name=" + java.net.URLEncoder.encode(ort.levels.firstOrNull() ?: ort.name, "UTF-8")) }) {
                        Text(stringResource(Res.string.desk_place_gov_search))
                    }
                }
                Field(notiz, { notiz = it }, Res.string.fact_note, minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(lat, { lat = it }, Modifier.weight(1f), label = { Text(stringResource(Res.string.desk_place_lat)) }, singleLine = true, isError = !koordOk)
                    OutlinedTextField(lng, { lng = it }, Modifier.weight(1f), label = { Text(stringResource(Res.string.desk_place_lng)) }, singleLine = true, isError = !koordOk)
                }
                Text(stringResource(if (koordOk) Res.string.desk_place_coords_hint else Res.string.desk_place_invalid_coords), style = MaterialTheme.typography.bodySmall,
                    color = if (koordOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                if (b != null && l != null && koordOk) {
                    val zustand = remember { KartenZustand() }
                    BoxWithConstraints(Modifier.fillMaxWidth().height(180.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        val w = constraints.maxWidth; val h = constraints.maxHeight
                        LaunchedEffect(b, l, w, h) { zustand.passeEin(listOf(GeoPunkt(b, l)), w, h, einzelZoom = 11, maxZoom = 15) }
                        KachelKarte(zustand, KachelEbene.STANDARD, Modifier.fillMaxSize(), pins = listOf(KartenPin(b, l, farbe = MaterialTheme.colorScheme.primary, radiusDp = 8f)))
                    }
                }

                // ── Koordinaten suchen (OpenStreetMap) ──
                if (suche == null) {
                    OutlinedButton(onClick = { suche = ort.name; sucheStarten(ort.name) }, shape = MaterialTheme.shapes.small) { Text(stringResource(Res.string.desk_place_search_coords)) }
                    Text(stringResource(Res.string.desk_place_search_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(suche.orEmpty(), { suche = it }, Modifier.weight(1f), singleLine = true,
                            label = { Text(stringResource(Res.string.desk_place_search_title, ort.levels.firstOrNull() ?: ort.name)) })
                        OutlinedButton(onClick = { suche?.takeIf(String::isNotBlank)?.let(::sucheStarten) }, enabled = !sucht, shape = MaterialTheme.shapes.small) {
                            Text(stringResource(Res.string.desk_place_search_button))
                        }
                    }
                    when {
                        sucht -> Text("…")
                        treffer?.isEmpty() == true -> Text(stringResource(Res.string.desk_place_search_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Column(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            treffer.orEmpty().forEach { t ->
                                val aktiv = grad(lat) == t.lat && grad(lng) == t.lng
                                Row(Modifier.fillMaxWidth().background(if (aktiv) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                                    .fokusRahmen().clickable { lat = dezimalEinzeln(t.lat); lng = dezimalEinzeln(t.lng) }.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                    Text(t.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(stringResource(Res.string.desk_place_take), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                if (istAdmin) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(mapData, { mapData = it })
                    Text(stringResource(Res.string.desk_place_mapdata), style = MaterialTheme.typography.bodyMedium)
                }
                fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            val geaendert = gov != altGov || notiz.trim() != altNotiz.trim() || koordGeaendert
            TextButton(enabled = geaendert && koordOk && !speichert, onClick = {
                speichert = true; fehler = null
                val anfrage = PlaceRequest(
                    name = ort.name,
                    gov = gov.takeIf { it != altGov },
                    note = notiz.takeIf { it.trim() != altNotiz.trim() },
                    koordinatenAendern = koordGeaendert, lat = b, lng = l,
                    mapData = mapData && istAdmin && koordGeaendert,
                )
                scope.launch {
                    runCatching { withContext(Dispatchers.IO) { client.savePlace(tree, anfrage) } }
                        .onSuccess { onSaved(it); onDismiss() }
                        .onFailure { fehler = it.message ?: "?"; speichert = false }
                }
            }) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !speichert) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

/** 53.7784170 -> "53.778417" */
internal fun dezimalEinzeln(wert: Double): String = "%.6f".format(Locale.ROOT, wert).trimEnd('0').trimEnd('.')
