package de.bgghome.webtrees.nativ.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import de.bgghome.webtrees.nativ.res.*
import de.bgghome.webtrees.nativ.ui.LocalAppName
import de.bgghome.webtrees.nativ.ui.openBrowser
import org.jetbrains.compose.resources.stringResource

/*
 * Hilfe (F1): ein eigenes Fenster mit Inhaltsverzeichnis, Suche, Zurueck/Vor und dem Kapiteltext. Die Kapitel liegen als
 * Markdown in resources/hilfe/<sprache>/<id>.md (Deutsch vollstaendig, Englisch als Rueckfall) und werden hier ohne fremde
 * Bibliothek gelesen: Ueberschriften, Absaetze, Aufzaehlungen, Tabellen, Hinweise, fett, kursiv, Tasten und Links.
 * Jedes Fenster oeffnet mit F1 sein Kapitel ueber Hilfe.oeffnen("tafeln"); Links der Form hilfe:<id> springen im Fenster.
 */

/** Zustand des Hilfefensters: ein Objekt, damit jedes Fenster es oeffnen kann, ohne Rueckrufe durchzureichen. */
object Hilfe {
    var kapitel by mutableStateOf<String?>(null)
        private set
    /** Zaehlt die Aufrufe, damit ein zweites F1 das offene Fenster nach vorn holt. */
    var anfrage by mutableStateOf(0)
        private set

    fun oeffnen(id: String = HilfeTexte.START) {
        kapitel = if (id in HilfeTexte.REIHENFOLGE) id else HilfeTexte.START
        anfrage++
    }

    fun schliessen() { kapitel = null }
}

// ── Kapitel und Markdown ─────────────────────────────────────────────

sealed interface HilfeBlock
class HUeberschrift(val stufe: Int, val text: String) : HilfeBlock
class HAbsatz(val text: String) : HilfeBlock
class HAufzaehlung(val punkte: List<String>, val nummeriert: Boolean) : HilfeBlock
class HTabelle(val kopf: List<String>, val zeilen: List<List<String>>) : HilfeBlock
class HHinweis(val text: String) : HilfeBlock

class HilfeKapitel(val id: String, val titel: String, val bloecke: List<HilfeBlock>) {
    /** Reiner Text ohne Auszeichnung, fuer Suche und Ausschnitt. */
    val text: String = bloecke.joinToString("\n") { b ->
        when (b) {
            is HUeberschrift -> b.text
            is HAbsatz -> b.text
            is HAufzaehlung -> b.punkte.joinToString("\n")
            is HTabelle -> (listOf(b.kopf) + b.zeilen).joinToString("\n") { it.joinToString(" – ") }
            is HHinweis -> b.text
        }
    }.let(::ohneAuszeichnung)

    fun treffer(suche: String): Int {
        if (suche.isBlank()) return 0
        var n = 0; var i = text.indexOf(suche, ignoreCase = true)
        while (i >= 0) { n++; i = text.indexOf(suche, i + suche.length, ignoreCase = true) }
        return n
    }

    fun ausschnitt(suche: String, umfang: Int = 45): String {
        val i = text.indexOf(suche, ignoreCase = true).takeIf { it >= 0 } ?: return ""
        val von = (i - umfang).coerceAtLeast(0); val bis = (i + suche.length + umfang).coerceAtMost(text.length)
        return (if (von > 0) "…" else "") + text.substring(von, bis).replace('\n', ' ') + (if (bis < text.length) "…" else "")
    }
}

object HilfeTexte {
    const val START = "start"
    val REIHENFOLGE = listOf("start", "hauptfenster", "person", "suche", "tafeln", "listen", "buecher", "pruefung", "webtrees", "lokal", "tasten", "fragen")

    /** Alle Kapitel in der Sprache des Systems; fehlt eines, kommt es englisch, zuletzt deutsch. */
    fun laden(sprache: String = java.util.Locale.getDefault().language): List<HilfeKapitel> = REIHENFOLGE.mapNotNull { id ->
        (lesen("hilfe/$sprache/$id.md") ?: lesen("hilfe/en/$id.md") ?: lesen("hilfe/de/$id.md"))?.let { kapitel(id, it) }
    }

    fun kapitel(id: String, markdown: String): HilfeKapitel {
        val bloecke = markdownBloecke(markdown)
        val titel = bloecke.filterIsInstance<HUeberschrift>().firstOrNull { it.stufe == 1 }?.text ?: id
        return HilfeKapitel(id, titel, bloecke)
    }

    private fun lesen(pfad: String): String? =
        HilfeTexte::class.java.classLoader?.getResourceAsStream(pfad)?.use { it.readBytes().toString(Charsets.UTF_8) }
}

private val UEBERSCHRIFT = Regex("^(#{1,3})\\s+(.*)$")
private val NUMMER = Regex("^\\d+\\.\\s+(.*)$")
private val TRENNZEILE = Regex("^:?-+:?$")

/** Zerlegt Markdown in Bloecke: HUeberschrift, HAbsatz, HAufzaehlung (- / 1.), HTabelle (|), HHinweis (>). */
fun markdownBloecke(text: String): List<HilfeBlock> {
    val zeilen = text.lines()
    val bloecke = mutableListOf<HilfeBlock>()
    var i = 0
    fun punkt(z: String) = z.startsWith("- ") || z.startsWith("* ")
    while (i < zeilen.size) {
        val z = zeilen[i].trimEnd()
        when {
            z.isBlank() -> i++
            UEBERSCHRIFT.matches(z) -> { val m = UEBERSCHRIFT.find(z)!!; bloecke += HUeberschrift(m.groupValues[1].length, m.groupValues[2].trim()); i++ }
            z.startsWith(">") -> {
                val teile = mutableListOf<String>()
                while (i < zeilen.size && zeilen[i].trimEnd().startsWith(">")) { teile += zeilen[i].trimEnd().removePrefix(">").trim(); i++ }
                bloecke += HHinweis(teile.joinToString(" "))
            }
            punkt(z) || NUMMER.matches(z) -> {
                val nummeriert = !punkt(z)
                val punkte = mutableListOf<String>()
                while (i < zeilen.size) {
                    val p = zeilen[i].trimEnd()
                    when {
                        (!nummeriert && punkt(p)) -> punkte += p.substring(2).trim()
                        (nummeriert && NUMMER.matches(p)) -> punkte += NUMMER.find(p)!!.groupValues[1].trim()
                        p.startsWith("  ") && p.isNotBlank() && punkte.isNotEmpty() -> punkte[punkte.lastIndex] = punkte.last() + " " + p.trim()
                        else -> break
                    }
                    i++
                }
                bloecke += HAufzaehlung(punkte, nummeriert)
            }
            z.startsWith("|") -> {
                val reihen = mutableListOf<List<String>>()
                while (i < zeilen.size && zeilen[i].trimEnd().startsWith("|")) {
                    val zellen = zeilen[i].trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                    if (!zellen.all { TRENNZEILE.matches(it) }) reihen += zellen
                    i++
                }
                if (reihen.isNotEmpty()) bloecke += HTabelle(reihen.first(), reihen.drop(1))
            }
            else -> {
                val teile = mutableListOf<String>()
                while (i < zeilen.size) {
                    val p = zeilen[i].trimEnd()
                    if (p.isBlank() || UEBERSCHRIFT.matches(p) || p.startsWith(">") || punkt(p) || NUMMER.matches(p) || p.startsWith("|")) break
                    teile += p.trim(); i++
                }
                bloecke += HAbsatz(teile.joinToString(" "))
            }
        }
    }
    return bloecke
}

private val INLINE = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)]\\(([^)]+)\\)|\\*(.+?)\\*")

/** Text ohne Auszeichnung (fett, Tasten, Links als ihr Text). */
fun ohneAuszeichnung(text: String): String = INLINE.replace(text) { m ->
    m.groupValues[1].ifEmpty { m.groupValues[2] }.ifEmpty { m.groupValues[3] }.ifEmpty { m.groupValues[5] }
}

/** Baut den Absatztext mit fett, kursiv, Tasten (Code) und Links; Suchtreffer werden hervorgehoben. */
private fun inline(text: String, suche: String, taste: SpanStyle, link: TextLinkStyles, hervor: SpanStyle, onLink: (String) -> Unit): AnnotatedString {
    val roh = buildAnnotatedString {
        var pos = 0
        for (m in INLINE.findAll(text)) {
            append(text, pos, m.range.first)
            when {
                m.groupValues[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(m.groupValues[1]) }
                m.groupValues[2].isNotEmpty() -> withStyle(taste) { append(m.groupValues[2]) }
                m.groupValues[3].isNotEmpty() -> {
                    val ziel = m.groupValues[4]
                    withLink(LinkAnnotation.Clickable(ziel, link) { onLink(ziel) }) { append(m.groupValues[3]) }
                }
                else -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(m.groupValues[5]) }
            }
            pos = m.range.last + 1
        }
        append(text, pos, text.length)
    }
    if (suche.isBlank()) return roh
    return buildAnnotatedString {
        append(roh)
        var i = roh.text.indexOf(suche, ignoreCase = true)
        while (i >= 0) { addStyle(hervor, i, i + suche.length); i = roh.text.indexOf(suche, i + suche.length, ignoreCase = true) }
    }
}

// ── Fenster ──────────────────────────────────────────────────────────

/** Das Hilfefenster; offen, solange [Hilfe.kapitel] gesetzt ist. Groesse bleibt in den Desktop-Einstellungen. */
@Composable
fun HilfeFenster() {
    val angefordert = Hilfe.kapitel ?: return
    val appName = LocalAppName.current
    val prefs = DeskLayout.prefs
    val fenster = rememberWindowState(
        width = (prefs.getString("hilfe_breite", null)?.toFloatOrNull() ?: 960f).dp,
        height = (prefs.getString("hilfe_hoehe", null)?.toFloatOrNull() ?: 700f).dp,
    )
    val kapitel = remember { HilfeTexte.laden() }
    var aktuell by remember { mutableStateOf(angefordert) }
    val zurueck = remember { mutableStateListOf<String>() }
    val vor = remember { mutableStateListOf<String>() }
    var suche by remember { mutableStateOf("") }
    fun gehe(ziel: String) { if (ziel != aktuell && kapitel.any { it.id == ziel }) { zurueck.add(aktuell); vor.clear(); aktuell = ziel } }
    fun schliessen() {
        prefs.putStrings(mapOf("hilfe_breite" to fenster.size.width.value.toString(), "hilfe_hoehe" to fenster.size.height.value.toString()))
        Hilfe.schliessen()
    }
    val onLink: (String) -> Unit = { ziel -> if (ziel.startsWith("hilfe:")) gehe(ziel.removePrefix("hilfe:")) else openBrowser(ziel) }

    Window(
        onCloseRequest = ::schliessen, title = "${stringResource(Res.string.desk_help)} – $appName", state = fenster,
        onPreviewKeyEvent = { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) { schliessen(); true } else false },
    ) {
        // F1 aus einem anderen Fenster, waehrend die Hilfe offen ist: Kapitel wechseln und nach vorn holen.
        LaunchedEffect(Hilfe.anfrage) { gehe(angefordert); window.toFront() }
        DeskTheme {
            val farben = MaterialTheme.colorScheme
            Column(Modifier.fillMaxSize().background(farben.background)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = zurueck.isNotEmpty(), onClick = { vor.add(aktuell); aktuell = zurueck.removeAt(zurueck.lastIndex) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.action_back))
                    }
                    IconButton(enabled = vor.isNotEmpty(), onClick = { zurueck.add(aktuell); aktuell = vor.removeAt(vor.lastIndex) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(Res.string.desk_forward))
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        suche, onValueChange = { suche = it }, singleLine = true, modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = { Text(stringResource(Res.string.desk_help_search), style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { if (suche.isNotEmpty()) IconButton(onClick = { suche = "" }) { Icon(Icons.Default.Clear, stringResource(Res.string.search_clear)) } },
                    )
                }
                HorizontalDivider(color = farben.outlineVariant)
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Inhalt(kapitel, aktuell, suche, Modifier.width(260.dp).fillMaxHeight(), onWahl = ::gehe)
                    VerticalDivider(color = farben.outlineVariant)
                    val k = kapitel.firstOrNull { it.id == aktuell } ?: kapitel.firstOrNull()
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        val rollen = rememberScrollState()
                        LaunchedEffect(aktuell) { rollen.scrollTo(0) }
                        Column(Modifier.fillMaxSize().verticalScroll(rollen).padding(start = 28.dp, end = 32.dp, top = 18.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            k?.bloecke?.forEach { BlockAnsicht(it, suche, onLink) }
                        }
                        SenkrechteLeiste(rollen)
                    }
                }
                HorizontalDivider(color = farben.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { openBrowser("https://bgg-home.de/me/genealogie/wtwin/") }) { Text(stringResource(Res.string.desk_help_online)) }
                    TextButton(onClick = { openBrowser("https://github.com/thobgg/app4webtrees/issues") }) { Text(stringResource(Res.string.desk_help_issues)) }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(shape = MaterialTheme.shapes.small, onClick = ::schliessen) { Text(stringResource(Res.string.action_close)) }
                }
            }
        }
    }
}

/** Linke Spalte: das Inhaltsverzeichnis, bei laufender Suche die Kapitel mit Treffern und einem Ausschnitt. */
@Composable
private fun Inhalt(kapitel: List<HilfeKapitel>, aktuell: String, suche: String, modifier: Modifier, onWahl: (String) -> Unit) {
    val farben = MaterialTheme.colorScheme
    Column(modifier.background(farben.surface)) {
        Text(stringResource(Res.string.desk_help_contents), Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelLarge, color = farben.onSurfaceVariant)
        val treffer = if (suche.isBlank()) emptyList() else kapitel.map { it to it.treffer(suche) }.filter { it.second > 0 }
        if (suche.isNotBlank() && treffer.isEmpty()) {
            Text(stringResource(Res.string.desk_help_none), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = farben.onSurfaceVariant)
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (suche.isBlank()) items(kapitel, key = { it.id }) { k ->
                val gewaehlt = k.id == aktuell
                Text(
                    k.titel, style = MaterialTheme.typography.bodyMedium, fontWeight = if (gewaehlt) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (gewaehlt) farben.onPrimaryContainer else farben.onSurface,
                    modifier = Modifier.fillMaxWidth().background(if (gewaehlt) farben.primaryContainer else Color.Transparent)
                        .clickable { onWahl(k.id) }.padding(horizontal = 16.dp, vertical = 7.dp),
                )
            } else items(treffer, key = { it.first.id }) { (k, n) ->
                val gewaehlt = k.id == aktuell
                Column(
                    Modifier.fillMaxWidth().background(if (gewaehlt) farben.primaryContainer else Color.Transparent)
                        .clickable { onWahl(k.id) }.padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text(k.titel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        color = if (gewaehlt) farben.onPrimaryContainer else farben.onSurface)
                    Text(stringResource(Res.string.desk_help_hits, n) + " · " + k.ausschnitt(suche), style = MaterialTheme.typography.bodySmall,
                        color = farben.onSurfaceVariant, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun BlockAnsicht(block: HilfeBlock, suche: String, onLink: (String) -> Unit) {
    val farben = MaterialTheme.colorScheme
    val typo = MaterialTheme.typography
    val taste = SpanStyle(fontFamily = FontFamily.Monospace, background = farben.surfaceVariant, fontSize = typo.bodyMedium.fontSize * 0.92f)
    val link = TextLinkStyles(SpanStyle(color = farben.primary, textDecoration = TextDecoration.Underline))
    val hervor = SpanStyle(background = farben.tertiaryContainer, color = farben.onTertiaryContainer)
    fun t(text: String) = inline(text, suche, taste, link, hervor, onLink)
    when (block) {
        is HUeberschrift -> when (block.stufe) {
            1 -> Text(t(block.text), style = typo.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
            2 -> Text(t(block.text), style = typo.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            else -> Text(t(block.text), style = typo.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        }
        is HAbsatz -> Text(t(block.text), style = typo.bodyMedium)
        is HAufzaehlung -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.punkte.forEachIndexed { i, p ->
                Row(Modifier.padding(start = 6.dp)) {
                    Text(if (block.nummeriert) "${i + 1}." else "•", style = typo.bodyMedium, modifier = Modifier.width(22.dp), color = farben.onSurfaceVariant)
                    Text(t(p), style = typo.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
        is HTabelle -> {
            val spalten = block.kopf.size.coerceAtLeast(1)
            // Zwei Spalten: die erste (Taste, System) schmaler als die Erklaerung.
            fun gewicht(s: Int) = if (spalten == 2 && s == 0) 1f else if (spalten == 2) 1.7f else 1f
            Column(Modifier.fillMaxWidth().border(1.dp, farben.outlineVariant, RoundedCornerShape(4.dp))) {
                Row(Modifier.fillMaxWidth().background(farben.surfaceVariant).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    block.kopf.forEachIndexed { s, z -> Text(t(z), style = typo.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(gewicht(s))) }
                }
                block.zeilen.forEachIndexed { r, zeile ->
                    HorizontalDivider(color = farben.outlineVariant)
                    Row(Modifier.fillMaxWidth().background(if (r % 2 == 1) farben.surface else Color.Transparent).padding(horizontal = 10.dp, vertical = 5.dp)) {
                        (0 until spalten).forEach { s -> Text(t(zeile.getOrElse(s) { "" }), style = typo.bodyMedium, modifier = Modifier.weight(gewicht(s))) }
                    }
                }
            }
        }
        is HHinweis -> Box(Modifier.fillMaxWidth().background(farben.secondaryContainer, RoundedCornerShape(6.dp)).padding(12.dp)) {
            Text(t(block.text), style = typo.bodyMedium, color = farben.onSecondaryContainer)
        }
    }
}
