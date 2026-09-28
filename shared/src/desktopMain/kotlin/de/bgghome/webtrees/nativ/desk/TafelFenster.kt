package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import de.bgghome.webtrees.nativ.Desktop
import de.bgghome.webtrees.nativ.api.DescendantNode
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.api.WtClient
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.AppViewModel
import de.bgghome.webtrees.nativ.ui.LocalAppName
import de.bgghome.webtrees.nativ.ui.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.apache.pdfbox.rendering.PDFRenderer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/*
 * Fenster "Tafel erstellen" (25.09.2026): links die Tafelarten, in der Mitte die Einstellungen, rechts die
 * Vorschau - sie ist das fertige Blatt, gerendert aus demselben PDF, das gedruckt oder gespeichert wird.
 * Tafelarten (26.09.2026): Ahnentafel, seitenweise Ahnentafel, Stammlinie, Mutterstamm, aeltester Vorfahr,
 * Stammtafel, Sanduhr - alle auf demselben Zeichenkern (Stammtafel.kt), Daten in TafelDaten.kt.
 */

/** Portraets fuer Tafeln, einmal geladen (ueber die Sitzung der App) und fuer die Laufzeit behalten. */
private object TafelBilder {
    private val cache = ConcurrentHashMap<String, BufferedImage>()
    private val fehlt = ConcurrentHashMap.newKeySet<String>()
    fun bekannt(url: String): BufferedImage? = cache[url]
    fun laden(url: String): BufferedImage? {
        cache[url]?.let { return it }
        if (url in fehlt) return null
        val bild = runCatching {
            Desktop.plattform.client.http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.byteStream()?.use { ImageIO.read(it) }
            }
        }.getOrNull()
        // In RGB umkopieren: JPEG verlangt Bilder ohne Transparenz
        val rgb = bild?.let { b -> BufferedImage(b.width, b.height, BufferedImage.TYPE_INT_RGB).also { it.createGraphics().apply { color = java.awt.Color.WHITE; fillRect(0, 0, b.width, b.height); drawImage(b, 0, 0, null); dispose() } } }
        if (rgb != null) cache[url] = rgb else fehlt += url
        return rgb
    }
}

private fun nachfahrenPersonen(k: DescendantNode): List<Person> = listOf(k.person) + k.families.flatMap { it.children }.flatMap(::nachfahrenPersonen)

/** Bilddatei fuer den Tafelhintergrund waehlen (Wappen, Karte ...); null bei Abbruch. */
private fun bildWaehlen(): String? {
    val d = java.awt.FileDialog(null as java.awt.Frame?, de.bgghome.webtrees.nativ.Texte.t(Res.string.desk_chart_bg_choose), java.awt.FileDialog.LOAD).apply {
        setFilenameFilter { _, n -> n.lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") } }
        isVisible = true
    }
    return d.file?.let { java.io.File(d.directory, it).absolutePath }
}

/** Alle geladenen Personen einer Tafel (fuer Bilder und Karteikarten). */
private fun datenPersonen(d: TafelDaten): List<Person> =
    d.ahnen.values.map { it.person } + (d.nachfahren?.let(::nachfahrenPersonen) ?: emptyList()) + (d.mutterseite?.let(::nachfahrenPersonen) ?: emptyList()) +
        d.partnerAhnen.values.map { it.person } + d.stammpaare.flatMap { nachfahrenPersonen(it.baum) } + d.geschwister.values.flatten()

private val stilNamen: Map<TafelStil, StringResource> = mapOf(
    TafelStil.Pergament to Res.string.desk_style_parchment, TafelStil.Klassisch to Res.string.desk_style_classic,
    TafelStil.Farbig to Res.string.desk_style_colour, TafelStil.Schwarzweiss to Res.string.desk_style_bw,
)

/** Name und Erklaerung je Tafelart. */
private val artTexte: Map<TafelArt, Pair<StringResource, StringResource>> = mapOf(
    TafelArt.Ahnen to (Res.string.desk_chart_ancestors to Res.string.desk_chart_ancestors_hint),
    TafelArt.AhnenSeiten to (Res.string.desk_chart_ancestors_pages to Res.string.desk_chart_ancestors_pages_hint),
    TafelArt.Faecher to (Res.string.desk_chart_fan to Res.string.desk_chart_fan_hint),
    TafelArt.Kreis to (Res.string.desk_chart_circle to Res.string.desk_chart_circle_hint),
    TafelArt.Stammlinie to (Res.string.desk_chart_paternal to Res.string.desk_chart_paternal_hint),
    TafelArt.Mutterstamm to (Res.string.desk_chart_maternal to Res.string.desk_chart_maternal_hint),
    TafelArt.Aeltester to (Res.string.desk_chart_oldest to Res.string.desk_chart_oldest_hint),
    TafelArt.Stamm to (Res.string.desk_chart_descendants to Res.string.desk_chart_descendants_hint),
    TafelArt.StammSeiten to (Res.string.desk_chart_descendants_pages to Res.string.desk_chart_descendants_pages_hint),
    TafelArt.Cousins to (Res.string.desk_chart_cousins to Res.string.desk_chart_cousins_hint),
    TafelArt.Sanduhr to (Res.string.desk_chart_hourglass to Res.string.desk_chart_hourglass_hint),
    TafelArt.Paar to (Res.string.desk_chart_couple to Res.string.desk_chart_couple_hint),
    TafelArt.Verwandt to (Res.string.desk_chart_relatives to Res.string.desk_chart_relatives_hint),
)

private val linien = setOf(TafelArt.Stammlinie, TafelArt.Mutterstamm, TafelArt.Aeltester)

/** Faechertafel und Ahnenkreis: ohne Orte und volle Daten (dafuer ist in den Ringen kein Platz). */
private val kreise = setOf(TafelArt.Faecher, TafelArt.Kreis)

/** Tafeln mit Gitter, Personenverzeichnis und Kurven fuer Doppelte (nicht Kreise und die seitenweise Ahnentafel). */
private val mitGitterArten = setOf(TafelArt.Ahnen, TafelArt.Stamm, TafelArt.Cousins, TafelArt.Sanduhr, TafelArt.Paar, TafelArt.Verwandt, TafelArt.Stammlinie, TafelArt.Mutterstamm, TafelArt.Aeltester)

/** Tafeln mit Kastenfarben (Regeln, Zweige); [linienArten] kennen Kekule-Nummern, [zweigArten] Nachfahren. */
private val farbArten = mitGitterArten + TafelArt.AhnenSeiten + TafelArt.StammSeiten + TafelArt.Paar
private val linienArten = setOf(TafelArt.Verwandt, TafelArt.Ahnen, TafelArt.AhnenSeiten, TafelArt.Sanduhr, TafelArt.Paar, TafelArt.Stammlinie, TafelArt.Mutterstamm, TafelArt.Aeltester)
private val zweigArten = setOf(TafelArt.Verwandt, TafelArt.Stamm, TafelArt.StammSeiten, TafelArt.Cousins, TafelArt.Sanduhr, TafelArt.Paar)

/** Einblattige Tafeln aus dem Zeichenkern: sie koennen Karteikarten tragen. */
private val kartenArten = mitGitterArten + TafelArt.Paar

/** Tafeln, die auch waagerecht gehen (Linien bleiben senkrecht). */
private val waagerechtMoeglich = setOf(TafelArt.Ahnen, TafelArt.AhnenSeiten, TafelArt.Stamm, TafelArt.StammSeiten, TafelArt.Cousins, TafelArt.Sanduhr, TafelArt.Paar)

/** Die Einstellungen bleiben je Tafelart zwischen den Aufrufen erhalten (Desktop-Einstellungen). */
private object TafelWahl {
    private val prefs get() = DeskLayout.prefs
    private fun k(art: TafelArt, name: String) = when (art) {
        TafelArt.Stamm -> "tafel_$name"
        TafelArt.Ahnen -> "tafel_ahnen_$name"
        else -> "tafel_${art.name.lowercase()}_$name"
    }
    private fun vorgabe(art: TafelArt) = when (art) {
        TafelArt.Stamm, TafelArt.StammSeiten -> 6; TafelArt.Cousins -> 4; TafelArt.Ahnen, TafelArt.Faecher -> 5; TafelArt.Kreis -> 6; TafelArt.Sanduhr -> 3; TafelArt.Paar -> 4; TafelArt.Verwandt -> 3; TafelArt.AhnenSeiten -> 7; else -> 13
    }
    fun letzte(): TafelArt = TafelArt.entries.firstOrNull { it.name == prefs.getString("tafel_art", null) } ?: TafelArt.Stamm
    fun laden(art: TafelArt) = TafelOptionen(
        generationen = (prefs.getString(k(art, "gen"), null)?.toIntOrNull() ?: vorgabe(art)).coerceIn(minGen(art), maxGen(art)),
        stil = TafelStil.entries.firstOrNull { it.name == prefs.getString(k(art, "stil"), null) } ?: TafelStil.Pergament,
        rahmenMm = prefs.getString(k(art, "rahmen"), null)?.toIntOrNull() ?: 30,
        bilder = prefs.getBoolean(k(art, "bilder"), true),
        nummern = prefs.getBoolean(k(art, "nummern"), true),
        ausgangOben = prefs.getBoolean(k(art, "oben"), false),
        nachfahren = if (art == TafelArt.Verwandt) (prefs.getString(k(art, "nach"), null)?.toIntOrNull() ?: 1).coerceIn(0, 3)
            else (prefs.getString(k(art, "nach"), null)?.toIntOrNull() ?: 3).coerceIn(1, 9),
        namenstraeger = prefs.getBoolean(k(art, "namen"), false),
        partner = prefs.getBoolean(k(art, "partner"), art in linien || art == TafelArt.Cousins || art == TafelArt.Paar || art == TafelArt.Verwandt),
        orte = prefs.getBoolean(k(art, "orte"), false),
        volleDaten = prefs.getBoolean(k(art, "voll"), false),
        waagerecht = art in waagerechtMoeglich && prefs.getBoolean(k(art, "waagerecht"), art == TafelArt.Sanduhr),
        geschwister = (prefs.getString(k(art, "geschw"), null)?.toIntOrNull() ?: 0).coerceIn(0, 2),
        gitter = prefs.getBoolean(k(art, "gitter"), art == TafelArt.Verwandt), verzeichnis = prefs.getBoolean(k(art, "verz"), art == TafelArt.StammSeiten), kurven = prefs.getBoolean(k(art, "kurven"), false),
        farbe = FarbSchema.entries.firstOrNull { it.name == prefs.getString(k(art, "farbe"), null) } ?: FarbSchema.Geschlecht,
        jeSeite = (prefs.getString(k(art, "jeseite"), null)?.toIntOrNull() ?: 3).coerceIn(2, 5),
        karteikarten = prefs.getBoolean(k(art, "karten"), false),
        legende = art !in kreise && prefs.getBoolean(k(art, "legende"), true),
        form = KastenForm.entries.firstOrNull { it.name == prefs.getString(k(art, "form"), null) } ?: KastenForm.Stil,
        schatten = prefs.getBoolean(k(art, "schatten"), false), fotoLinks = prefs.getBoolean(k(art, "fotolinks"), false),
        hintergrund = TafelHintergrund.entries.firstOrNull { it.name == prefs.getString(k(art, "hg"), null) } ?: TafelHintergrund.Stil,
        hintergrundBild = prefs.getString("tafel_hg_bild", null).orEmpty(),
        schmuck = Schmuckrahmen.entries.firstOrNull { it.name == prefs.getString(k(art, "schmuck"), null) } ?: Schmuckrahmen.Keiner,
        ersteller = prefs.getString("tafel_ersteller", null).orEmpty(),
        uebersicht = prefs.getBoolean(k(art, "uebersicht"), true),
    )
    fun sichern(art: TafelArt, o: TafelOptionen) {
        prefs.putString("tafel_art", art.name)
        prefs.putString(k(art, "gen"), o.generationen.toString()); prefs.putString(k(art, "stil"), o.stil.name)
        prefs.putString(k(art, "rahmen"), o.rahmenMm.toString()); prefs.putBoolean(k(art, "bilder"), o.bilder)
        prefs.putBoolean(k(art, "nummern"), o.nummern); prefs.putBoolean(k(art, "oben"), o.ausgangOben)
        prefs.putString(k(art, "nach"), o.nachfahren.toString()); prefs.putBoolean(k(art, "namen"), o.namenstraeger)
        prefs.putBoolean(k(art, "partner"), o.partner); prefs.putBoolean(k(art, "orte"), o.orte); prefs.putBoolean(k(art, "voll"), o.volleDaten)
        prefs.putBoolean(k(art, "waagerecht"), o.waagerecht); prefs.putString(k(art, "geschw"), o.geschwister.toString())
        prefs.putBoolean(k(art, "gitter"), o.gitter); prefs.putBoolean(k(art, "verz"), o.verzeichnis); prefs.putBoolean(k(art, "kurven"), o.kurven)
        prefs.putString(k(art, "farbe"), o.farbe.name)
        prefs.putString(k(art, "jeseite"), o.jeSeite.toString()); prefs.putBoolean(k(art, "uebersicht"), o.uebersicht)
        prefs.putBoolean(k(art, "karten"), o.karteikarten); prefs.putBoolean(k(art, "legende"), o.legende)
        prefs.putString(k(art, "hg"), o.hintergrund.name); prefs.putString("tafel_hg_bild", o.hintergrundBild); prefs.putString(k(art, "schmuck"), o.schmuck.name)
        prefs.putString(k(art, "form"), o.form.name); prefs.putBoolean(k(art, "schatten"), o.schatten); prefs.putBoolean(k(art, "fotolinks"), o.fotoLinks)
        prefs.putString("tafel_ersteller", o.ersteller)
    }
}

@Composable
fun TafelFenster(state: UiState, viewModel: AppViewModel, start: TafelArt?, onClose: () -> Unit) {
    val appName = LocalAppName.current
    val tree = state.tree
    val root = state.root
    val wurzelName = state.detail?.takeIf { it.person.xref == root }?.person?.name ?: state.people.firstOrNull { it.xref == root }?.name.orEmpty()
    var art by remember { mutableStateOf(start ?: TafelWahl.letzte()) }
    // Paar: welcher Ehepartner (Familie der Ausgangsperson in Folge), gilt nur fuer diesen Aufruf
    var paarFamilie by remember(root) { mutableStateOf(0) }
    var partnerName by remember(root) { mutableStateOf("") }
    val titelVorgabe = remember(art, wurzelName, partnerName) { tafelTitel(art, wurzelName, partnerName) }
    var o by remember(art) { mutableStateOf(TafelWahl.laden(art).copy(titel = titelVorgabe, untertitel = tree?.title.orEmpty())) }
    // Der Titel nennt beim Paar beide Namen - er steht erst fest, wenn der Partner geladen ist
    LaunchedEffect(titelVorgabe) { if (art == TafelArt.Paar) o = o.copy(titel = titelVorgabe) }
    LaunchedEffect(art, o) { TafelWahl.sichern(art, o) }
    // Farbregeln und gefaerbte Zweige gelten fuer alle Tafeln des Stammbaums
    val baumName = tree?.name.orEmpty()
    var regeln by remember(baumName) { mutableStateOf(TafelFarbSpeicher.regeln(baumName)) }
    var zweige by remember(baumName) { mutableStateOf(TafelFarbSpeicher.zweige(baumName)) }
    LaunchedEffect(baumName, regeln, zweige) { if (baumName.isNotEmpty()) TafelFarbSpeicher.sichern(baumName, regeln, zweige) }
    // Verwandtschaftstafel: abgewaehlte Stammpaare (nur fuer diesen Aufruf)
    var ohneStamm by remember(root) { mutableStateOf(emptySet<String>()) }
    val oVoll = o.copy(regeln = regeln, zweige = zweige, ohneStamm = ohneStamm)
    var druck by remember { mutableStateOf(DruckWahl.laden()) }
    LaunchedEffect(druck) { DruckWahl.sichern(druck) }
    val privat = stringResource(Res.string.person_private)
    val fuss = remember(tree) { fusszeile(appName, tree?.title.orEmpty()) }

    // Daten: Nachfahren einmal in voller Tiefe (Umstellen der Generationen kuerzt nur); Vorfahren so tief wie
    // eingestellt, weil jede Generation ueber sieben weitere Anfragen kostet.
    val ladeTiefe = if (art == TafelArt.Stamm || art == TafelArt.StammSeiten || art == TafelArt.Cousins) maxGen(art) else o.generationen
    val geschwisterLaden = if (art == TafelArt.Ahnen) o.geschwister else 0
    val paarLaden = if (art == TafelArt.Paar) paarFamilie else 0
    val nachLaden = if (art == TafelArt.Verwandt) o.nachfahren else 0
    val daten by produceState<Result<TafelDaten>?>(null, tree?.name, root, art, ladeTiefe, geschwisterLaden, paarLaden, nachLaden) {
        value = null
        value = if (tree == null || root == null) null else withContext(Dispatchers.IO) {
            runCatching { tafelDatenLaden(viewModel.client, tree.name, root, art, ladeTiefe, geschwisterLaden, paarLaden, nachLaden) }
        }
    }
    LaunchedEffect(daten) { daten?.getOrNull()?.takeIf { art == TafelArt.Paar }?.let { d -> partnerName = d.partnerNamen.getOrNull(d.paarFamilie).orEmpty() } }
    // Bilder im Hintergrund laden; jedes fertige Buendel zaehlt hoch und zeichnet die Vorschau neu.
    var bilderStand by remember { mutableStateOf(0) }
    LaunchedEffect(daten, o.bilder) {
        val d = daten?.getOrNull() ?: return@LaunchedEffect
        if (!o.bilder) return@LaunchedEffect
        val urls = datenPersonen(d).mapNotNull { it.thumb }.distinct().filter { TafelBilder.bekannt(it) == null }
        urls.chunked(8).forEach { gruppe ->
            withContext(Dispatchers.IO) { gruppe.forEach { TafelBilder.laden(it) } }
            bilderStand++
        }
    }
    // Karteikarten: die ausfuehrlichen Daten aller geladenen Personen, im Hintergrund; nur fuer Druck und PDF, nicht fuer die Vorschau
    val mitKarten = o.karteikarten && art in kartenArten
    val details by produceState<Map<String, de.bgghome.webtrees.nativ.api.IndividualDetail>?>(null, daten, mitKarten) {
        value = null
        val d = daten?.getOrNull() ?: return@produceState
        if (!mitKarten || tree == null) return@produceState
        value = withContext(Dispatchers.IO) { runCatching { kartenLaden(viewModel.client, tree.name, datenPersonen(d).filter { !it.isPrivate }.map { it.xref }) }.getOrElse { emptyMap() } }
    }
    fun erzeugen(ausgabe: Boolean = false) = daten?.getOrNull()?.let { d ->
        tafelErzeugen(art, d, oVoll, { p -> p.thumb?.let(TafelBilder::bekannt) }, privat, fuss, if (ausgabe && mitKarten) details else null)
    }

    // Vorschau: das Blatt als Bild, kurz verzoegert, damit schnelles Umstellen nicht jedes Mal rendert. Das PDF wird
    // dafuer einmal gespeichert und neu geladen - erst beim Speichern bettet PDFBox die Schriften ein, vorher zeichnet
    // der Renderer den Titel in einer Ersatzschrift. Zoom (TafelVorschau.kt): die Aufloesung waechst in Stufen mit.
    var vorschauPx by remember { mutableStateOf(1000 to 800) }
    var zoom by remember(art) { mutableStateOf(1f) }
    var versatz by remember(art) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val stufe = zoomStufe(zoom)
    val vorschau by produceState<Result<VorschauBild?>?>(null, daten, oVoll, art, bilderStand, vorschauPx, stufe) {
        delay(150)
        if (daten?.getOrNull() == null) { value = null; return@produceState }
        value = withContext(Dispatchers.Default) {
            runCatching {
                val (doc, info) = erzeugen() ?: return@runCatching null
                val bytes = java.io.ByteArrayOutputStream().also { out -> doc.use { it.save(out) } }.toByteArray()
                org.apache.pdfbox.Loader.loadPDF(bytes).use {
                    val box = it.getPage(0).mediaBox
                    val passend = minOf(vorschauPx.first / box.width, vorschauPx.second / box.height)
                    // Je Zoomstufe doppelt so fein, das Bild aber hoechstens 9000 Pixel an der langen Seite
                    val skala = minOf(passend * 1.5f * (1 shl stufe), 9000f / maxOf(box.width, box.height)).coerceIn(0.05f, 4f)
                    VorschauBild(PDFRenderer(it).renderImage(0, skala).toComposeImageBitmap(), skala, info)
                }
            }
        }
    }

    DialogWindow(
        onCloseRequest = onClose, title = stringResource(Res.string.desk_chart_window),
        state = rememberDialogState(width = 1280.dp, height = 860.dp),
        onPreviewKeyEvent = { e -> if (e.key == Key.Escape) { onClose(); true } else false },
    ) {
        DeskTheme {
            Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                // Tafelarten
                Column(Modifier.width(210.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface).padding(vertical = 8.dp)) {
                    listOf(
                        Res.string.desk_chart_group_ancestors to listOf(TafelArt.Ahnen, TafelArt.AhnenSeiten, TafelArt.Faecher, TafelArt.Kreis, TafelArt.Stammlinie, TafelArt.Mutterstamm, TafelArt.Aeltester),
                        Res.string.desk_chart_group_descendants to listOf(TafelArt.Stamm, TafelArt.StammSeiten, TafelArt.Cousins),
                        Res.string.desk_chart_group_both to listOf(TafelArt.Sanduhr, TafelArt.Paar, TafelArt.Verwandt),
                    ).forEachIndexed { i, (gruppe, arten) ->
                        if (i > 0) Spacer(Modifier.height(8.dp))
                        ArtGruppe(stringResource(gruppe))
                        arten.forEach { a -> ArtEintrag(stringResource(artTexte.getValue(a).first), art == a) { art = a } }
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Einstellungen
                Column(Modifier.width(330.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(artTexte.getValue(art).first), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(artTexte.getValue(art).second), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Einstellung(stringResource(Res.string.desk_chart_person)) { Text(wurzelName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                    // Paar: mit mehreren Ehen waehlbar, welcher Partner neben der Person steht
                    val partnerNamen = daten?.getOrNull()?.takeIf { art == TafelArt.Paar }?.partnerNamen.orEmpty()
                    if (partnerNamen.size > 1) Einstellung(stringResource(Res.string.desk_chart_couple_partner)) {
                        val werte = partnerNamen.mapIndexed { i, n -> "${i + 1}. $n" }
                        Auswahl(werte[paarFamilie.coerceIn(0, werte.size - 1)], werte) { w -> paarFamilie = werte.indexOf(w) }
                    }
                    if (art == TafelArt.Verwandt) {
                        // Stammpaare aus Generation 1 (Eltern), 2 (Grosseltern) ...; die Namen helfen beim Waehlen
                        Einstellung(stringResource(Res.string.desk_chart_roots_generation)) {
                            val namen = listOf(Res.string.desk_chart_gen_parents, Res.string.desk_chart_gen_grandparents, Res.string.desk_chart_gen_great).map { stringResource(it) }
                            val werte = (minGen(art)..maxGen(art)).map { g -> namen.getOrNull(g - 1) ?: stringResource(Res.string.desk_chart_gen_nth, g - 2) }
                            Auswahl(werte[o.generationen - minGen(art)], werte) { w -> o = o.copy(generationen = werte.indexOf(w) + minGen(art)) }
                        }
                        Einstellung(stringResource(Res.string.desk_chart_below_root)) {
                            Auswahl(o.nachfahren.toString(), (0..3).map { it.toString() }) { o = o.copy(nachfahren = it.toInt()) }
                        }
                        daten?.getOrNull()?.stammpaare?.takeIf { it.isNotEmpty() }?.let { paare ->
                            Text(stringResource(Res.string.desk_chart_roots), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            paare.forEach { sp ->
                                val name = sp.wurzel.name + (sp.partner?.let { " ⚭ ${it.name}" } ?: "")
                                Haken(name, sp.wurzel.xref !in ohneStamm) { an -> ohneStamm = if (an) ohneStamm - sp.wurzel.xref else ohneStamm + sp.wurzel.xref }
                            }
                        }
                    } else Einstellung(stringResource(if (art == TafelArt.Sanduhr || art == TafelArt.Paar) Res.string.desk_chart_generations_anc else Res.string.desk_chart_generations)) {
                        Auswahl(o.generationen.toString(), (minGen(art)..maxGen(art)).map { it.toString() }) { o = o.copy(generationen = it.toInt()) }
                    }
                    if (art == TafelArt.StammSeiten) {
                        Einstellung(stringResource(Res.string.desk_chart_per_page)) {
                            Auswahl(o.jeSeite.toString(), (2..5).map { it.toString() }) { o = o.copy(jeSeite = it.toInt()) }
                        }
                        Haken(stringResource(Res.string.desk_chart_overview), o.uebersicht) { o = o.copy(uebersicht = it) }
                        Haken(stringResource(Res.string.desk_chart_index), o.verzeichnis) { o = o.copy(verzeichnis = it) }
                    }
                    if (art == TafelArt.Sanduhr || art == TafelArt.Paar) Einstellung(stringResource(Res.string.desk_chart_generations_desc)) {
                        Auswahl(o.nachfahren.toString(), (1..9).map { it.toString() }) { o = o.copy(nachfahren = it.toInt()) }
                    }
                    Einstellung(stringResource(Res.string.desk_chart_style)) {
                        val namen = TafelStil.entries.associateWith { stringResource(stilNamen.getValue(it)) }
                        Auswahl(namen.getValue(o.stil), namen.values.toList()) { w -> o = o.copy(stil = namen.entries.first { it.value == w }.key) }
                    }
                    Einstellung(stringResource(Res.string.desk_chart_box_width)) {
                        Auswahl("${o.rahmenMm} mm", (20..60 step 5).map { "$it mm" }) { o = o.copy(rahmenMm = it.substringBefore(' ').toInt()) }
                    }
                    if (art !in kreise) {
                        Einstellung(stringResource(Res.string.desk_chart_box_form)) {
                            val namen = listOf(Res.string.desk_chart_form_style, Res.string.desk_chart_form_square, Res.string.desk_chart_form_round,
                                Res.string.desk_chart_form_oval, Res.string.desk_chart_form_shield).map { stringResource(it) }
                            Auswahl(namen[o.form.ordinal], namen) { w -> o = o.copy(form = KastenForm.entries[namen.indexOf(w)]) }
                        }
                        Haken(stringResource(Res.string.desk_chart_shadow), o.schatten) { o = o.copy(schatten = it) }
                        Einstellung(stringResource(Res.string.desk_chart_background)) {
                            val namen = listOf(Res.string.desk_chart_form_style, Res.string.desk_chart_bg_white, Res.string.desk_chart_bg_parchment,
                                Res.string.desk_chart_bg_paper, Res.string.desk_chart_bg_gradient, Res.string.desk_chart_bg_image).map { stringResource(it) }
                            Auswahl(namen[o.hintergrund.ordinal], namen) { w ->
                                val hg = TafelHintergrund.entries[namen.indexOf(w)]
                                // Eigenes Bild: gleich die Datei waehlen lassen
                                val datei = if (hg == TafelHintergrund.Bild) bildWaehlen() ?: o.hintergrundBild else o.hintergrundBild
                                o = if (hg == TafelHintergrund.Bild && datei.isBlank()) o else o.copy(hintergrund = hg, hintergrundBild = datei)
                            }
                        }
                        if (o.hintergrund == TafelHintergrund.Bild) Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(java.io.File(o.hintergrundBild).name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            androidx.compose.material3.TextButton(onClick = { bildWaehlen()?.let { o = o.copy(hintergrundBild = it) } }) { Text(stringResource(Res.string.desk_chart_bg_choose)) }
                        }
                        Einstellung(stringResource(Res.string.desk_chart_frame)) {
                            val namen = listOf(Res.string.desk_chart_frame_none, Res.string.desk_chart_frame_double, Res.string.desk_chart_frame_corners,
                                Res.string.desk_chart_frame_vines).map { stringResource(it) }
                            Auswahl(namen[o.schmuck.ordinal], namen) { w -> o = o.copy(schmuck = Schmuckrahmen.entries[namen.indexOf(w)]) }
                        }
                        if (!o.waagerecht && o.bilder) Haken(stringResource(Res.string.desk_chart_photo_left), o.fotoLinks) { o = o.copy(fotoLinks = it) }
                    }
                    if (art in waagerechtMoeglich) Haken(stringResource(Res.string.desk_chart_horizontal), o.waagerecht) { o = o.copy(waagerecht = it) }
                    Haken(stringResource(Res.string.desk_chart_photos), o.bilder) { o = o.copy(bilder = it) }
                    if (art in mitGitterArten) {
                        Haken(stringResource(Res.string.desk_chart_grid), o.gitter) { o = o.copy(gitter = it) }
                        Haken(stringResource(Res.string.desk_chart_index), o.verzeichnis) { o = o.copy(verzeichnis = it) }
                        Haken(stringResource(Res.string.desk_chart_curves), o.kurven) { o = o.copy(kurven = it) }
                    }
                    if (art != TafelArt.Stamm && art != TafelArt.StammSeiten) Haken(stringResource(Res.string.desk_chart_numbers), o.nummern) { o = o.copy(nummern = it) }
                    if (art == TafelArt.Ahnen) Haken(stringResource(if (o.waagerecht) Res.string.desk_chart_root_right else Res.string.desk_chart_root_top), o.ausgangOben) { o = o.copy(ausgangOben = it) }
                    if (art == TafelArt.Ahnen) Einstellung(stringResource(Res.string.desk_chart_siblings)) {
                        val werte = listOf(Res.string.desk_chart_siblings_none, Res.string.desk_chart_siblings_root, Res.string.desk_chart_siblings_all).map { stringResource(it) }
                        Auswahl(werte[o.geschwister], werte) { w -> o = o.copy(geschwister = werte.indexOf(w)) }
                    }
                    if (art == TafelArt.Stamm || art == TafelArt.StammSeiten || art == TafelArt.Sanduhr) Haken(stringResource(Res.string.desk_chart_name_bearers), o.namenstraeger) { o = o.copy(namenstraeger = it) }
                    if (art == TafelArt.Stamm || art == TafelArt.StammSeiten || art == TafelArt.Sanduhr || art == TafelArt.Cousins || art == TafelArt.Paar || art == TafelArt.Verwandt) Haken(stringResource(Res.string.desk_chart_spouses), o.partner) { o = o.copy(partner = it) }
                    if (art in linien) Haken(stringResource(Res.string.desk_chart_both_parents), o.partner) { o = o.copy(partner = it) }
                    if (art !in kreise) {
                        Haken(stringResource(Res.string.desk_chart_places), o.orte) { o = o.copy(orte = it) }
                        Haken(stringResource(Res.string.desk_chart_full_dates), o.volleDaten) { o = o.copy(volleDaten = it) }
                    }
                    if (art in farbArten) FarbEinstellungen(
                        o, art in linienArten, art in zweigArten, { o = o.copy(farbe = it) },
                        regeln, { regeln = it }, zweige.size, { zweige = emptyMap() },
                    )
                    if (art in kartenArten) {
                        Haken(stringResource(Res.string.desk_chart_cards), o.karteikarten) { o = o.copy(karteikarten = it) }
                        if (mitKarten && details == null) Text(stringResource(Res.string.desk_chart_cards_loading), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(o.titel, { o = o.copy(titel = it) }, label = { Text(stringResource(Res.string.desk_chart_heading)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (art !in kreise) {
                        OutlinedTextField(o.untertitel, { o = o.copy(untertitel = it) }, label = { Text(stringResource(Res.string.desk_chart_subtitle)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(o.ersteller, { o = o.copy(ersteller = it) }, label = { Text(stringResource(Res.string.desk_chart_author)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Haken(stringResource(Res.string.desk_chart_legend), o.legende) { o = o.copy(legende = it) }
                    }
                    val info = vorschau?.getOrNull()?.info
                    info?.let {
                        Text(if (it.seiten > 0) stringResource(Res.string.desk_chart_size_pages, it.personen, it.seiten) else stringResource(Res.string.desk_chart_size, it.personen, it.breiteCm, it.hoeheCm),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.desk_chart_zoom_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val bereit = info != null && (!mitKarten || details != null)
                    val titel = o.titel.ifBlank { titelVorgabe }
                    // Beim Drucken bleibt das Blatt offen: der Druck laeuft im Hintergrund und greift auf seine Inhalte zu.
                    if (art == TafelArt.AhnenSeiten || art == TafelArt.StammSeiten) {
                        Knopf(stringResource(Res.string.desk_chart_print_pages), bereit) { erzeugen()?.first?.let { drucken(it, titel) } }
                        Knopf(stringResource(Res.string.desk_chart_pdf_pages), bereit) { erzeugen()?.first?.let { alsPdf(it, titel) } }
                    } else {
                        Knopf(stringResource(Res.string.desk_chart_print_one), bereit) { erzeugen(true)?.first?.let { drucken(aufEinBlatt(it), titel) } }
                        Knopf(stringResource(Res.string.desk_chart_pdf_poster), bereit) { erzeugen(true)?.first?.let { alsPdf(it, titel) } }
                        Knopf(stringResource(Res.string.desk_chart_png), bereit) { erzeugen()?.first?.let { alsPng(it, titel) } }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        GrossdruckWahl(druck, { druck = it }, info, bereit,
                            onBlaetterDrucken = { erzeugen(true)?.let { (p, i) -> p.use { drucken(aufBlaetter(it, druck.groesse(), i.bereich), titel) } } },
                            onBlaetterPdf = { erzeugen(true)?.let { (p, i) -> p.use { alsPdf(aufBlaetter(it, druck.groesse(), i.bereich), "$titel A4") } } },
                            onPlotter = {
                                val rolle = druck.rolleCm() ?: return@GrossdruckWahl
                                erzeugen()?.let { (p, i) -> p.use {
                                    val s = it.getPage(0); val ber = druckBereich(s, i.bereich)
                                    alsPdf(aufRolle(it, rollenPlan(ber.b, ber.h, s.userUnit, rolle, druck.groesse(), druck.einpassen), i.bereich), "$titel Plotter")
                                } }
                            })
                    }
                    Spacer(Modifier.height(4.dp))
                    Knopf(stringResource(Res.string.action_close), true, onClose)
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Vorschau
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().background(androidx.compose.ui.graphics.Color(0xFF8C8F8E)).padding(16.dp), contentAlignment = Alignment.Center) {
                    val dichte = LocalDensity.current
                    val px = with(dichte) { maxWidth.roundToPx() to maxHeight.roundToPx() }
                    LaunchedEffect(px) { vorschauPx = px }
                    val fehler = daten?.exceptionOrNull() ?: vorschau?.exceptionOrNull()
                    val vb = vorschau?.getOrNull()
                    val bild = vb?.bild
                    val info = vb?.info
                    // Rechtsklick auf einen Kasten: Punkt auf der Seite (in ihren Einheiten) -> Karte darunter
                    var menue by remember { mutableStateOf<Pair<KartenOrt, androidx.compose.ui.unit.DpOffset>?>(null) }
                    fun klick(seite: androidx.compose.ui.geometry.Offset, anker: androidx.compose.ui.geometry.Offset) {
                        if (info == null || info.karten.isEmpty() || art !in farbArten) return
                        val karte = info.karten.firstOrNull { it.enthaelt(seite.x, seite.y) && it.person.xref.isNotEmpty() && !it.person.isPrivate } ?: return
                        menue = karte to with(dichte) { androidx.compose.ui.unit.DpOffset(anker.x.toDp(), anker.y.toDp()) }
                    }
                    Box(Modifier.align(Alignment.TopStart)) {
                        ZweigMenue(menue?.first, menue?.second ?: androidx.compose.ui.unit.DpOffset.Zero, menue?.first?.person?.xref in zweige,
                            onWahl = { f -> menue?.first?.person?.xref?.let { x -> zweige = if (f == null) zweige - x else zweige + (x to f) }; menue = null },
                            onZu = { menue = null })
                    }
                    when {
                        fehler != null -> Text(fehler.message ?: "?", color = androidx.compose.ui.graphics.Color.White)
                        vorschau != null && bild == null -> Text(stringResource(Res.string.desk_chart_empty), color = androidx.compose.ui.graphics.Color.White)
                        bild == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
                            Text(stringResource(Res.string.desk_chart_loading_any), Modifier.padding(top = 8.dp), color = androidx.compose.ui.graphics.Color.White)
                        }
                        else -> ZoomVorschau(vb!!, zoom, versatz, { z, v -> zoom = z; versatz = v }, ::klick)
                    }
                }
            }
        }
    }
}

@Composable
internal fun Haken(text: String, wert: Boolean, onWechsel: (Boolean) -> Unit) {
    Row(Modifier.clickable { onWechsel(!wert) }, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = wert, onCheckedChange = onWechsel, modifier = Modifier.size(32.dp))
        Text(text, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ArtGruppe(text: String) {
    Text(text, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ArtEintrag(text: String, aktiv: Boolean, onClick: () -> Unit) {
    Text(text, Modifier.fillMaxWidth().background(if (aktiv) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
        .fokusRahmen().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 6.dp),
        style = MaterialTheme.typography.bodyMedium, fontWeight = if (aktiv) FontWeight.SemiBold else FontWeight.Normal)
}

@Composable
internal fun Einstellung(label: String, inhalt: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(150.dp), style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(1f)) { inhalt() }
    }
}

@Composable
internal fun Auswahl(wert: String, werte: List<String>, onWahl: (String) -> Unit) {
    var offen by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { offen = true }, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Text(wert, Modifier.weight(1f), maxLines = 1)
            Text("▾")
        }
        DropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
            werte.forEach { w -> DropdownMenuItem(text = { Text(w, fontWeight = if (w == wert) FontWeight.SemiBold else FontWeight.Normal) }, onClick = { offen = false; onWahl(w) }) }
        }
    }
}

@Composable
internal fun Knopf(text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
