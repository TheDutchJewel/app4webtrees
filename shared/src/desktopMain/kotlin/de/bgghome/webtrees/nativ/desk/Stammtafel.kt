package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.api.Person
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSBoolean
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSFloat
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.multipdf.LayerUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.awt.color.ColorSpace

/*
 * Stammtafel und Ahnentafel: alle Nachfahren oder
 * Vorfahren einer Person als ein grosses Blatt - jede Generation eine Reihe, ueber jedem Kasten Portraet oder
 * Silhouette. Die Ahnentafel ist dieselbe Zeichnung, gespiegelt: Ausgangsperson unten. Ein Zeichenkern fuer Vorschau und Ausgabe: das Layout rechnet in Punkt (1/72 Zoll), daraus
 * entsteht EIN PDF-Blatt; die Vorschau im Fenster ist dieses Blatt, gedruckt wird es verkleinert auf ein
 * Blatt oder in Originalgroesse auf mehrere A4-Blaetter zum Zusammenkleben.
 */

/**
 * Eine Person der Tafel mit den Personen der naechsten Reihe: bei der Stammtafel ihre Kinder (aller
 * Partnerschaften, in Familienfolge), bei der Ahnentafel Vater und Mutter. [nummer]: Kekule-Nummer (Ahnentafel).
 * [partner]: Ehepartner, im Kasten genannt. [verweis]: Ahnenschwund - die Person steht schon unter dieser Nummer,
 * ihre Vorfahren dort. [hinweis]: kleiner Text ueber dem Kasten ("→ S. 3"). [aufLinie]: gehoert zur Linie
 * (Stammlinie, Mutterstamm, aeltester Vorfahr).
 */
class TafelPerson(
    val person: Person, val kinder: List<TafelPerson>, val nummer: Long? = null,
    val partner: List<Person> = emptyList(), val verweis: Long? = null, val hinweis: String? = null, val aufLinie: Boolean = false,
    /** Ahnentafel: Geschwister neben der Person, an derselben Elternlinie - beim Vater links, sonst rechts. */
    val geschwister: List<Person> = emptyList(), val geschwisterLinks: Boolean = false,
)

/**
 * Stammtafel: Ausgangsperson oben, Nachfahren darunter. Ahnentafel: Ausgangsperson unten, Vorfahren darueber.
 * Sanduhr: beides. Paar: die Vorfahren beider Partner oben nebeneinander, die gemeinsamen Nachfahren darunter. Linien (Stammlinie, Mutterstamm, aeltester Vorfahr): eine Folge von Elternpaaren.
 * AhnenSeiten: die Ahnentafel in Stuecken zu vier Generationen je A4-Seite. Faecher und Kreis: Ringe um den
 * Probanden (Faechertafel.kt).
 */
enum class TafelArt { Ahnen, AhnenSeiten, Faecher, Kreis, Stammlinie, Mutterstamm, Aeltester, Stamm, Cousins, Sanduhr, Paar }

enum class TafelStil { Pergament, Klassisch, Farbig, Schwarzweiss }

data class TafelOptionen(
    val generationen: Int = 6,
    /** Kekule-Nummern an den Kaesten (Tafeln mit Vorfahren). */
    val nummern: Boolean = true,
    val stil: TafelStil = TafelStil.Pergament,
    val rahmenMm: Int = 30,
    val bilder: Boolean = true,
    val titel: String = "",
    /** Nur Ahnentafel: Ausgangsperson oben, die Vorfahren darunter. */
    val ausgangOben: Boolean = false,
    /** Nur Sanduhr und Paar: Generationen der Nachfahren ([generationen] zaehlt dort die Vorfahren). */
    val nachfahren: Int = 3,
    /** Stammtafel, Sanduhr: nur die Kinder der Soehne weiterverfolgen. */
    val namenstraeger: Boolean = false,
    /** Stammtafel, Sanduhr: Ehepartner im Kasten. Linien: beide Eltern statt nur der Linie. */
    val partner: Boolean = true,
    val orte: Boolean = false,
    val volleDaten: Boolean = false,
    /** Generationen als Spalten von links nach rechts statt als Reihen. */
    val waagerecht: Boolean = false,
    /** Nur Ahnentafel: 0 keine Geschwister, 1 die des Probanden, 2 die aller Vorfahren. */
    val geschwister: Int = 0,
    /** Grosse Tafeln lesbar machen: Gitter am Rand (Spalten A, B ... / Generationen I, II ...), Personenverzeichnis
     * mit Gitterposition auf eigenen A4-Seiten, doppelte Personen mit farbiger Kurve verbinden. */
    val gitter: Boolean = false,
    val verzeichnis: Boolean = false,
    val kurven: Boolean = false,
)

/** Was eine Tafel zeichnet: Vorfahren nach oben, Nachfahren nach unten (je nach Art einer oder beide Teile). */
class TafelInhalt(
    val vorfahren: TafelPerson? = null, val nachfahren: TafelPerson? = null, val linie: Boolean = false, val cousins: CousinTafel? = null,
    val paar: PaarTafel? = null,
)

/**
 * Sanduhr eines Paares (A4, 28.09.2026): die Ahnentafeln von [mann] und [frau] (Kekule-Nummern je Partner ab 1)
 * nach oben, nebeneinander; das Paar in einer Reihe mit Heiratslinie, darunter die gemeinsamen [kinder].
 */
class PaarTafel(val mann: TafelPerson, val frau: TafelPerson, val kinder: List<TafelPerson>) {
    fun knoten(): List<TafelPerson> = alleKnoten(mann) + alleKnoten(frau) + kinder.flatMap(::alleKnoten)
}

/**
 * Nachfahren der Grosseltern (A1, 28.09.2026): links die Grosseltern vaeterlicherseits mit ihren Nachfahren, rechts
 * die muetterlicherseits. [vater] steht als letztes Kind links, [mutter] als erstes rechts - beide innen nebeneinander,
 * ihre gemeinsamen [kinder] (Proband und Geschwister) haengen mittig unter dem Paar. Fehlen die Grosseltern einer
 * Seite, ist der Elternteil selbst die Wurzel ([links] === [vater]); fehlt ein Elternteil, haengen die Kinder in
 * dessen Baum und [vater]/[mutter] sind null.
 */
class CousinTafel(val links: TafelPerson?, val rechts: TafelPerson?, val vater: TafelPerson?, val mutter: TafelPerson?, val kinder: List<TafelPerson>) {
    fun knoten(): List<TafelPerson> = listOfNotNull(links, rechts).flatMap(::alleKnoten) + kinder.flatMap(::alleKnoten)
}

/** Ein Paar nebeneinander in einer Reihe, dessen gemeinsame Kinder an einer Linie aus der Mitte haengen. */
class TafelPaar(val vater: TafelPlatz, val mutter: TafelPlatz, val kinder: List<TafelPlatz>)

/** Ein platzierter Kasten: Mitte waagerecht, Oberkante des Bildes, Ebene (0 = Ausgangsperson). */
class TafelPlatz(val knoten: TafelPerson, val mitteX: Float, val obenY: Float, val ebene: Int, val eltern: TafelPlatz?)

/**
 * Masse eines Kastens in Punkt, alle aus der Rahmenbreite abgeleitet. [zusatz]: Zeilen fuer Orte und Partner.
 * Das Layout rechnet in zwei Achsen: Generation (Reihe) und Geschwister (nebeneinander). [waagerecht]: die
 * Generationen liegen als Spalten nebeneinander, das Bild steht links neben dem Text statt darueber.
 */
class TafelMasse(val rahmen: Float, val bilder: Boolean, zusatz: Int = 0, val waagerecht: Boolean = false) {
    val bild = if (bilder) rahmen * 0.78f else 0f
    val bildAbstand = if (bilder) rahmen * 0.06f else 0f
    val schriftKlein = rahmen * 0.085f
    val schriftName = rahmen * 0.12f
    val kastenH = schriftKlein * (3 + zusatz) * 1.3f + schriftName * 1.3f + rahmen * 0.12f
    val spalt = maxOf(rahmen * 0.12f, 6f)
    val verbinder = maxOf(rahmen * 0.36f, 18f)
    /** Eine Karte (Bild und Kasten) in Blattrichtung. */
    val karteB = if (waagerecht) bild + bildAbstand + rahmen else rahmen
    val karteH = if (waagerecht) maxOf(bild, kastenH) else bild + bildAbstand + kastenH
    /** Laenge der Karte entlang der Generationen und entlang der Geschwister. */
    val laengeG = if (waagerecht) karteB else karteH
    val laengeQ = if (waagerecht) karteH else karteB
    val ebeneH = laengeG + verbinder
    val slot = laengeQ + spalt
}

class TafelLayout(val plaetze: List<TafelPlatz>, val breite: Float, val hoehe: Float, val masse: TafelMasse, val paare: List<TafelPaar> = emptyList())

/**
 * Baumlayout nach Konturen (Art Reingold-Tilford): Geschwister-Teilbaeume ruecken so eng zusammen, wie es ihre
 * Umrisse in JEDER Reihe erlauben - ein tiefer Zweig darf unter kinderlose Geschwister reichen. Jede Person steht
 * mittig ueber ihrem ersten und letzten Kind. So wird die Tafel nicht breiter als noetig (Vorbild: 78 statt 140 cm).
 */
fun stammtafelLayout(wurzel: TafelPerson, masse: TafelMasse): TafelLayout {
    // Ein Teilbaum: Versatz jedes Kindes zur Mitte der Person, Umriss je Tiefe (linkeste und rechteste Mitte).
    class Teil(val kinderVersatz: List<Float>, val links: MutableList<Float>, val rechts: MutableList<Float>)
    val teile = HashMap<TafelPerson, Teil>()
    // Geschwister verbreitern die Person nach einer Seite (Mitte der aeussersten Geschwisterkarte)
    fun lw(k: TafelPerson) = if (k.geschwisterLinks) k.geschwister.size * masse.slot else 0f
    fun rw(k: TafelPerson) = if (k.geschwisterLinks) 0f else k.geschwister.size * masse.slot
    fun rechnen(k: TafelPerson): Teil {
        val kinder = k.kinder.map(::rechnen)
        if (kinder.isEmpty()) return Teil(emptyList(), mutableListOf(-lw(k)), mutableListOf(rw(k))).also { teile[k] = it }
        // Kinder von links nach rechts ansetzen; jedes so weit rechts, dass es in keiner Tiefe den bisherigen Umriss beruehrt
        val versatz = ArrayList<Float>()
        val accL = ArrayList<Float>(); val accR = ArrayList<Float>()
        kinder.forEach { t ->
            var x = 0f
            if (versatz.isNotEmpty()) {
                x = Float.NEGATIVE_INFINITY
                for (d in 0 until minOf(accR.size, t.links.size)) x = maxOf(x, accR[d] + masse.slot - t.links[d])
            }
            versatz += x
            t.links.indices.forEach { d ->
                val l = t.links[d] + x; val r = t.rechts[d] + x
                if (d < accL.size) { accL[d] = minOf(accL[d], l); accR[d] = maxOf(accR[d], r) } else { accL += l; accR += r }
            }
        }
        val mitte = (versatz.first() + versatz.last()) / 2
        val teil = Teil(versatz.map { it - mitte }, mutableListOf(-lw(k)).apply { addAll(accL.map { it - mitte }) }, mutableListOf(rw(k)).apply { addAll(accR.map { it - mitte }) })
        teile[k] = teil
        return teil
    }
    val ganz = rechnen(wurzel)
    val links = ganz.links.min()
    val plaetze = mutableListOf<TafelPlatz>()
    fun setzen(k: TafelPerson, mitte: Float, ebene: Int, eltern: TafelPlatz?) {
        val platz = TafelPlatz(k, mitte, ebene * masse.ebeneH, ebene, eltern)
        plaetze += platz
        val t = teile.getValue(k)
        k.kinder.forEachIndexed { i, kind -> setzen(kind, mitte + t.kinderVersatz[i], ebene + 1, platz) }
    }
    // Die linkeste Kastenmitte liegt eine halbe Slotbreite vom linken Rand
    setzen(wurzel, -links + masse.slot / 2, 0, null)
    val breite = ganz.rechts.max() - links + masse.slot
    val hoehe = ganz.links.size * masse.ebeneH - masse.verbinder
    return TafelLayout(plaetze, breite, hoehe, masse)
}

/**
 * Linie (Stammlinie, Mutterstamm, aeltester Vorfahr): die Personen der Linie senkrecht uebereinander, der andere
 * Elternteil daneben - der Vater links, die Mutter rechts. Kein Schraegwandern wie bei zentrierten Paaren.
 */
fun linienLayout(wurzel: TafelPerson, masse: TafelMasse): TafelLayout {
    class Roh(val k: TafelPerson, val x: Float, val ebene: Int, val eltern: Int?)
    val roh = mutableListOf<Roh>()
    var k: TafelPerson? = wurzel; var ebene = 0; var eltern: Int? = null
    while (k != null) {
        roh += Roh(k, 0f, ebene, eltern)
        val hier = roh.size - 1
        val linie = k.kinder.firstOrNull { it.aufLinie }
        k.kinder.forEachIndexed { i, andere ->
            if (andere === linie) return@forEachIndexed
            val links = if (linie != null) i < k.kinder.indexOf(linie) else andere.person.sex == "M"
            roh += Roh(andere, if (links) -masse.slot else masse.slot, ebene + 1, hier)
        }
        k = linie; ebene++; eltern = hier
    }
    val links = roh.minOf { it.x }
    val plaetze = ArrayList<TafelPlatz>()
    roh.forEach { r -> plaetze += TafelPlatz(r.k, r.x - links + masse.slot / 2, r.ebene * masse.ebeneH, r.ebene, r.eltern?.let { plaetze[it] }) }
    val breite = roh.maxOf { it.x } - links + masse.slot
    return TafelLayout(plaetze, breite, (roh.maxOf { it.ebene } + 1) * masse.ebeneH - masse.verbinder, masse)
}

/**
 * Nachfahren der Grosseltern: die beiden Seiten und die Kinder des Paares je fuer sich nach Konturen gelegt, dann
 * nebeneinander gerueckt. Die Kinder stehen mittig unter Vater und Mutter; die rechte Seite rueckt so weit ab, dass
 * sich in keiner Reihe etwas beruehrt - weder die Seiten untereinander noch mit den Kindern dazwischen.
 * Reihe 0 sind die Grosseltern, Reihe 1 die Eltern mit Onkeln und Tanten.
 */
fun cousinLayout(c: CousinTafel, masse: TafelMasse): TafelLayout {
    // Teil-Layout mit Reihenversatz; [ohneWurzel]: die Wurzel ist nur ein Halter fuer mehrere Kinder
    class Stueck(val layout: TafelLayout, val versatz: Int, ohneWurzel: Boolean) {
        val plaetze = if (ohneWurzel) layout.plaetze.drop(1) else layout.plaetze
        fun ebene(p: TafelPlatz) = p.ebene + versatz
        val links = plaetze.groupBy(::ebene).mapValues { e -> e.value.minOf { it.mitteX } }
        val rechts = plaetze.groupBy(::ebene).mapValues { e -> e.value.maxOf { it.mitteX } }
        fun x(k: TafelPerson?) = plaetze.firstOrNull { it.knoten === k }?.mitteX
    }
    val l = c.links?.let { Stueck(stammtafelLayout(it, masse), if (it === c.vater) 1 else 0, false) }
    val r = c.rechts?.let { Stueck(stammtafelLayout(it, masse), if (it === c.mutter) 1 else 0, false) }
    val halter = c.kinder.firstOrNull()?.let { TafelPerson(it.person, c.kinder) }
    val k = halter?.let { Stueck(stammtafelLayout(it, masse), 1, true) }
    // Abstand zwischen zwei Stuecken, damit sich in keiner Reihe Kaesten beruehren
    fun abstand(a: Stueck, b: Stueck): Float =
        a.rechts.keys.intersect(b.links.keys).maxOfOrNull { e -> a.rechts.getValue(e) + masse.slot - b.links.getValue(e) } ?: Float.NEGATIVE_INFINITY
    val fx = l?.x(c.vater); val mx = r?.x(c.mutter)
    var dr = 0f; var dk = 0f
    if (l != null && r != null) {
        dr = abstand(l, r)
        if (fx != null && mx != null) {
            // Das Paar etwas weiter auseinander als Geschwister: dazwischen die Heiratslinie
            dr = maxOf(dr, fx + masse.slot * 1.3f - mx)
            if (k != null) {
                // Kinder mittig: dk = (fx + mx + dr) / 2 - kc; links an l, rechts an r vorbei
                val kc = k.layout.plaetze.first().mitteX
                k.links.forEach { (e, kl) -> l.rechts[e]?.let { lr -> dr = maxOf(dr, 2 * (lr + masse.slot - kl + kc) - fx - mx) } }
                k.rechts.forEach { (e, kr) -> r.links[e]?.let { rl -> dr = maxOf(dr, 2 * (kr + masse.slot - kc - rl) + fx + mx) } }
                dk = (fx + mx + dr) / 2 - kc
            }
        }
    } else if (k != null) dk = (fx ?: mx ?: 0f) - k.layout.plaetze.first().mitteX
    // Zusammensetzen: neue Plaetze mit den neuen Eltern-Verweisen
    val alle = mutableListOf<TafelPlatz>()
    val neu = HashMap<TafelPlatz, TafelPlatz>()
    listOfNotNull(l?.let { it to 0f }, r?.let { it to dr }, k?.let { it to dk }).forEach { (st, dx) ->
        st.plaetze.forEach { p ->
            val n = TafelPlatz(p.knoten, p.mitteX + dx, 0f, st.ebene(p), p.eltern?.let { neu[it] })
            neu[p] = n; alle += n
        }
    }
    if (alle.isEmpty()) return TafelLayout(emptyList(), 0f, 0f, masse)
    val e0 = alle.minOf { it.ebene }
    val x0 = alle.minOf { it.mitteX } - masse.slot / 2
    val fertig = HashMap<TafelPlatz, TafelPlatz>()
    // Reihenfolge bleibt: Eltern stehen vor ihren Kindern
    val plaetze = alle.map { p -> TafelPlatz(p.knoten, p.mitteX - x0, (p.ebene - e0) * masse.ebeneH, p.ebene - e0, p.eltern?.let { fertig[it] }).also { fertig[p] = it } }
    val paare = if (c.vater == null || c.mutter == null) emptyList() else {
        val v = plaetze.first { it.knoten === c.vater }; val m = plaetze.first { it.knoten === c.mutter }
        listOf(TafelPaar(v, m, plaetze.filter { p -> p.eltern == null && c.kinder.any { it === p.knoten } }))
    }
    return TafelLayout(plaetze, alle.maxOf { it.mitteX } - x0 + masse.slot / 2, (plaetze.maxOf { it.ebene } + 1) * masse.ebeneH - masse.verbinder, masse, paare)
}

/** Wer mehrfach vorkommt (Nachfahren, die untereinander geheiratet haben), bekommt auf der Tafel eine Nummer. */
fun doppelteNummern(plaetze: List<TafelPlatz>): Map<String, Int> =
    plaetze.filter { it.knoten.verweis == null }.groupBy { it.knoten.person.xref }.filter { it.value.size > 1 && it.key.isNotEmpty() }.keys.withIndex().associate { (i, x) -> x to i + 1 }

internal class StilFarben(
    val hintergrundOben: Color, val hintergrundUnten: Color, val titel: Color, val linie: Color, val text: Color,
    val rahmenBreite: Float, val rund: Boolean, val grau: Boolean,
    val fuellung: (String) -> Color, val rahmen: (String) -> Color,
)

internal fun farben(stil: TafelStil): StilFarben = when (stil) {
    TafelStil.Pergament -> StilFarben(Color(0xFF, 0xFF, 0xFF), Color(0xFD, 0xEE, 0xBE), Color(0x1E, 0x14, 0x0A), Color(0x3A, 0x2E, 0x22), Color(0x1E, 0x14, 0x0A),
        2.2f, true, false, { Color.WHITE }, { Color(0x1E, 0x14, 0x0A) })
    TafelStil.Klassisch -> StilFarben(Color.WHITE, Color.WHITE, Color.BLACK, Color(0x44, 0x44, 0x44), Color.BLACK,
        1f, false, false, { Color.WHITE }, { Color.BLACK })
    TafelStil.Farbig -> StilFarben(Color.WHITE, Color.WHITE, Color(0x1F, 0x3A, 0x6B), Color(0x6A, 0x74, 0x73), Color(0x24, 0x30, 0x2F),
        1.2f, true, false,
        { s -> when (s) { "M" -> Color(0xB9, 0xD0, 0xE8); "F" -> Color(0xF3, 0xC4, 0xBE); else -> Color(0xDD, 0xE2, 0xE2) } },
        { s -> when (s) { "M" -> Color(0x4F, 0x7F, 0xAE); "F" -> Color(0xC2, 0x70, 0x6A); else -> Color(0x8A, 0x94, 0x93) } })
    TafelStil.Schwarzweiss -> StilFarben(Color.WHITE, Color.WHITE, Color.BLACK, Color.BLACK, Color.BLACK,
        0.8f, false, true, { Color.WHITE }, { Color.BLACK })
}

/** Schriften der Tafel: Serifenschrift fuer Pergament und Klassisch, Titel in Schreibschrift (Great Vibes, OFL). */
internal class TafelSchriften(doc: PDDocument, stil: TafelStil) {
    private val basis = Schriften(doc)
    private val serif = stil == TafelStil.Pergament || stil == TafelStil.Klassisch
    val normal: PDFont = (if (serif) basis.ladenAus(listOf(
        "C:/Windows/Fonts/times.ttf", "/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
        "/usr/share/fonts/truetype/noto/NotoSerif-Regular.ttf",
    )) else null) ?: basis.normal
    val fett: PDFont = (if (serif) basis.ladenAus(listOf(
        "C:/Windows/Fonts/timesbd.ttf", "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf",
        "/usr/share/fonts/truetype/noto/NotoSerif-Bold.ttf",
    )) else null) ?: basis.fett
    val titel: PDFont = if (stil == TafelStil.Pergament) {
        runCatching { TafelSchriften::class.java.getResourceAsStream("/fonts/GreatVibes-Regular.ttf")!!.use { PDType0Font.load(doc, it) } }.getOrNull() ?: fett
    } else fett
}

private fun COSArray.zahlen(vararg z: Float) = apply { z.forEach { add(COSFloat(it)) } }

/** Senkrechter Farbverlauf ueber das ganze Blatt (Pergament). */
internal fun PDPageContentStream.verlauf(b: Float, h: Float, oben: Color, unten: Color) {
    val fn = COSDictionary().apply {
        setInt(COSName.FUNCTION_TYPE, 2)
        setItem(COSName.DOMAIN, COSArray().zahlen(0f, 1f))
        setItem(COSName.C0, COSArray().zahlen(unten.red / 255f, unten.green / 255f, unten.blue / 255f))
        setItem(COSName.C1, COSArray().zahlen(oben.red / 255f, oben.green / 255f, oben.blue / 255f))
        setInt(COSName.N, 1)
    }
    val shading = PDShadingType2(COSDictionary()).apply {
        shadingType = 2
        colorSpace = PDDeviceRGB.INSTANCE
        coords = COSArray().zahlen(0f, 0f, 0f, h)
        function = PDFunctionType2(fn)
        extend = COSArray().apply { add(COSBoolean.TRUE); add(COSBoolean.TRUE) }
    }
    saveGraphicsState(); addRect(0f, 0f, b, h); clip(); shadingFill(shading); restoreGraphicsState()
}

internal fun PDPageContentStream.rechteck(x: Float, y: Float, w: Float, h: Float, r: Float) {
    if (r <= 0f) { addRect(x, y, w, h); return }
    val k = 0.5523f * r
    moveTo(x + r, y); lineTo(x + w - r, y); curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r)
    lineTo(x + w, y + h - r); curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h)
    lineTo(x + r, y + h); curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r)
    lineTo(x, y + r); curveTo(x, y + r - k, x + r - k, y, x + r, y); closePath()
}

internal fun grau(b: BufferedImage): BufferedImage = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(b, null)

/** Text in eine Breite zwingen: erst kleiner (bis 70 %), dann kuerzen. */
internal fun passend(schrift: PDFont, text: String, groesse: Float, breite: Float): Pair<String, Float> {
    var g = groesse
    while (schrift.breite(text, g) > breite && g > groesse * 0.7f) g -= groesse * 0.05f
    var t = text
    while (schrift.breite(t, g) > breite && t.length > 3) t = t.dropLast(2) + "…"
    return t to g
}

/** Groesste Seitenlaenge eines PDF-Blatts (200 Zoll); groessere Tafeln werden verkleinert. */
internal const val PDF_MAX = 14400f

/** Masse des Blatts in Zentimetern und die Personenzahl, fuer die Anzeige im Fenster. [seiten] > 0: A4-Seiten. */
class TafelInfo(val personen: Int, val breiteCm: Int, val hoeheCm: Int, val seiten: Int = 0)

internal fun alleKnoten(k: TafelPerson): List<TafelPerson> = listOf(k) + k.kinder.flatMap(::alleKnoten)

/**
 * Das Blatt einmal gespeichert und neu geladen: PDFBox bettet die Schriften (Teilmengen) erst beim Speichern ein.
 * Wer die Seite vorher in ein anderes Dokument uebernimmt, bekommt leere Schriften - der Titel in Great Vibes
 * wurde dort zu Zeichensalat (26.09.2026). Das Original bleibt unveraendert nutzbar.
 */
private fun eingebettet(poster: PDDocument): PDDocument =
    org.apache.pdfbox.Loader.loadPDF(java.io.ByteArrayOutputStream().also { poster.save(it) }.toByteArray())

/** Das Blatt verkleinert auf eine A4-Seite (hoch oder quer, was besser passt). */
fun aufEinBlatt(poster: PDDocument): PDDocument = PDDocument().also { aufEinBlatt(poster, it) }

/** Wie [aufEinBlatt], haengt die Seite aber an [ziel] an (fuer mehrseitige Tafeln). */
fun aufEinBlatt(original: PDDocument, ziel: PDDocument, querErzwingen: Boolean = false) {
    val poster = eingebettet(original)
    try { aufEinBlattSeite(poster, ziel, querErzwingen) } finally { anhangSeiten(poster, ziel) }
}

/** Seiten hinter dem Blatt (Personenverzeichnis) unveraendert anhaengen, ohne Links auf das Poster. */
private fun anhangSeiten(poster: PDDocument, ziel: PDDocument) {
    for (i in 1 until poster.numberOfPages) ziel.importPage(poster.getPage(i)).annotations = emptyList()
}

private fun aufEinBlattSeite(poster: PDDocument, ziel: PDDocument, querErzwingen: Boolean) {
    val quelle = poster.getPage(0).mediaBox
    val a4 = PDRectangle.A4
    val quer = querErzwingen || quelle.width > quelle.height
    val format = if (quer) PDRectangle(a4.height, a4.width) else a4
    val rand = 28f
    val form = LayerUtility(ziel).importPageAsForm(poster, 0)
    val page = PDPage(format); ziel.addPage(page)
    val f = minOf((format.width - 2 * rand) / quelle.width, (format.height - 2 * rand) / quelle.height, 1f)
    PDPageContentStream(ziel, page).use { cs ->
        cs.saveGraphicsState()
        cs.transform(Matrix.getTranslateInstance((format.width - quelle.width * f) / 2, (format.height - quelle.height * f) / 2))
        cs.transform(Matrix.getScaleInstance(f, f))
        cs.drawForm(form); cs.restoreGraphicsState()
    }
}

/**
 * Das Blatt in Originalgroesse auf A4-Seiten zum Zusammenkleben: 10 mm Rand, 10 mm Ueberlappung, jede Seite
 * mit Zeile/Spalte und Schnittmarken. Hoch- oder Querformat - was weniger Seiten braucht.
 */
fun aufA4Blaetter(original: PDDocument): PDDocument {
    val poster = eingebettet(original)
    val quelle = poster.getPage(0).mediaBox
    val mm = 72f / 25.4f
    val rand = 10 * mm; val ueber = 10 * mm
    fun zahl(format: PDRectangle): Pair<Int, Int> {
        val sx = format.width - 2 * rand - ueber; val sy = format.height - 2 * rand - ueber
        return Math.ceil(((quelle.width - ueber) / sx).toDouble()).toInt().coerceAtLeast(1) to Math.ceil(((quelle.height - ueber) / sy).toDouble()).toInt().coerceAtLeast(1)
    }
    val hoch = PDRectangle.A4; val quer = PDRectangle(hoch.height, hoch.width)
    val (hs, hz) = zahl(hoch); val (qs, qz) = zahl(quer)
    val format = if (qs * qz < hs * hz) quer else hoch
    val (spalten, zeilen) = zahl(format)
    val schrittX = format.width - 2 * rand - ueber; val schrittY = format.height - 2 * rand - ueber
    val doc = PDDocument()
    val form = LayerUtility(doc).importPageAsForm(poster, 0)
    val schrift = Schriften(doc).normal
    for (z in 0 until zeilen) for (sp in 0 until spalten) {
        val page = PDPage(format); doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            // Ausschnitt: Spalte sp von links, Zeile z von oben
            val qx = sp * schrittX
            val qyOben = quelle.height - z * schrittY
            cs.saveGraphicsState()
            cs.addRect(rand, rand, format.width - 2 * rand, format.height - 2 * rand); cs.clip()
            cs.transform(Matrix.getTranslateInstance(rand - qx, format.height - rand - qyOben))
            cs.drawForm(form)
            cs.restoreGraphicsState()
            // Schnittmarken an den Ecken des bedruckten Bereichs, Kennung der Seite
            cs.setStrokingColor(Color(0x99, 0x99, 0x99)); cs.setLineWidth(0.4f)
            val l = 4 * mm
            listOf(rand to rand, format.width - rand to rand, rand to format.height - rand, format.width - rand to format.height - rand).forEach { (x, y) ->
                cs.moveTo(x - l, y); cs.lineTo(x + l, y); cs.moveTo(x, y - l); cs.lineTo(x, y + l)
            }
            cs.stroke()
            cs.setNonStrokingColor(Color(0x88, 0x88, 0x88))
            cs.beginText(); cs.setFont(schrift, 7f); cs.newLineAtOffset(rand, rand * 0.4f)
            cs.showText(schrift.sicher("${z + 1}/${sp + 1}  ·  ${zeilen}×${spalten}")); cs.endText()
        }
    }
    anhangSeiten(poster, doc)
    return doc
}
