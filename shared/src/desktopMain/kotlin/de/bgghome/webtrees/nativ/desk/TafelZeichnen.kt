package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.EventJson
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.res.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.awt.image.BufferedImage

/*
 * Zeichnen einer Tafel (aus Stammtafel.kt herausgeloest, 28.09.2026): in drei Stufen, damit neue Gestaltungs-
 * optionen je an einer Stelle landen.
 *  1. TafelAnordnung - reine Geometrie: die Layouts der Teile, Geschwister, Reihen, Gitterpositionen (ohne PDF).
 *  2. TafelBlatt - die Masse des Blatts: Rand, Titel, Gitterrand, Verkleinerung, Umrechnung in PDF-Koordinaten.
 *  3. TafelZeichner - je Schritt eine Methode: Hintergrund, Titel, Gitter, Kurven, Linien, Karten, Fusszeile.
 * Die Reihenfolge der Schritte ist die Stapelfolge auf dem Blatt (Kurven unter den Linien, Karten obenauf).
 */

/**
 * Ein Teil der Tafel: Layout, Richtung (Vorfahren wachsen nach oben) und waagerechter Versatz auf dem Blatt.
 * [ohneWurzel]: die Wurzel haelt nur mehrere Kinder zusammen und wird weder gezeichnet noch verbunden.
 */
internal class TafelTeil(val layout: TafelLayout, val aufwaerts: Boolean, var dx: Float = 0f, val ohneWurzel: Boolean = false, val versatz: Int = 0) {
    fun istHalter(pl: TafelPlatz) = ohneWurzel && pl === layout.plaetze.first()
}

/** Ein Paar auf der Tafel; Vater, Mutter und Kinder koennen in verschiedenen Teilen stehen. */
internal class PaarLage(
    val vaterTeil: TafelTeil, val vater: TafelPlatz, val mutterTeil: TafelTeil, val mutter: TafelPlatz,
    val kinderTeil: TafelTeil?, val kinder: List<TafelPlatz>,
)

/**
 * Wo was auf der Tafel steht, in Punkt entlang zweier Achsen: g (Generationen, Reihe fuer Reihe) und q (quer dazu,
 * die Geschwister nebeneinander). Senkrecht ist g nach unten und q nach rechts, waagerecht umgekehrt.
 */
internal class TafelAnordnung(inhalt: TafelInhalt, val o: TafelOptionen) {
    val masse: TafelMasse
    val teile: List<TafelTeil>
    /** Geschwister: eigene Plaetze in der Reihe der Person, nur wenn deren Eltern auf der Tafel stehen. */
    val geschwisterVon = HashMap<TafelPlatz, List<TafelPlatz>>()
    /** Reihen oberhalb und unterhalb der Ausgangsperson. */
    val oben: Int
    val unten: Int
    val breite: Float
    val hoehe: Float
    /** Jeder Platz einmal: bei der Sanduhr steht die Ausgangsperson nur im unteren Teil. */
    val gezeichnet: List<Pair<TafelTeil, TafelPlatz>>
    /** Nummern fuer Personen, die mehrfach vorkommen. */
    val nummern: Map<String, Int>
    /** Paare mit Heiratslinie, gemeinsame Kinder an einer Linie aus ihrer Mitte. */
    val paare: List<PaarLage>
    /** Vor- und Nachfahren auf einem Blatt: die Generationen werden von oben durchgezaehlt. */
    private val beideRichtungen: Boolean

    init {
        val knoten = listOfNotNull(inhalt.vorfahren, inhalt.nachfahren).flatMap(::alleKnoten) + inhalt.cousins?.knoten().orEmpty() + inhalt.paar?.knoten().orEmpty() +
            inhalt.wald.flatMap(::alleKnoten)
        masse = TafelMasse(o.rahmenMm * 72f / 25.4f, o.bilder, zusatzZeilen(knoten, o), o.waagerecht)
        teile = buildList {
            inhalt.vorfahren?.let { add(TafelTeil(if (inhalt.linie) linienLayout(it, masse) else stammtafelLayout(it, masse), true)) }
            inhalt.nachfahren?.let { add(TafelTeil(stammtafelLayout(it, masse), false)) }
            inhalt.cousins?.let { add(TafelTeil(cousinLayout(it, masse), false)) }
            // Mehrere Baeume nebeneinander (Verwandtschaftstafel): der Halter ist unsichtbar und nimmt keine Reihe ein
            inhalt.wald.firstOrNull()?.let { add(TafelTeil(stammtafelLayout(TafelPerson(it.person, inhalt.wald), masse), false, ohneWurzel = true, versatz = 1)) }
            inhalt.paar?.let { p ->
                add(TafelTeil(stammtafelLayout(p.mann, masse), true)); add(TafelTeil(stammtafelLayout(p.frau, masse), true))
                p.kinder.firstOrNull()?.let { add(TafelTeil(stammtafelLayout(TafelPerson(it.person, p.kinder), masse), false, ohneWurzel = true)) }
            }
        }
        teile.forEach { t ->
            t.layout.plaetze.filter { it.knoten.geschwister.isNotEmpty() && it.knoten.kinder.isNotEmpty() }.forEach { pl ->
                val richtung = if (pl.knoten.geschwisterLinks) -1 else 1
                geschwisterVon[pl] = pl.knoten.geschwister.mapIndexed { i, g ->
                    TafelPlatz(TafelPerson(g, emptyList()), pl.mitteX + richtung * (i + 1) * masse.slot, pl.obenY, pl.ebene, null)
                }
            }
        }
        // Ausgangspersonen uebereinander (Paar: nebeneinander, die Kinder mittig darunter), dann alles an den linken Rand
        if (inhalt.paar != null) paarRuecken(teile, masse)
        else if (teile.size == 2) teile[0].dx = teile[1].layout.plaetze.first().mitteX - teile[0].layout.plaetze.first().mitteX
        val minX = teile.minOf { t -> t.alle().minOf { it.mitteX } + t.dx } - masse.slot / 2
        teile.forEach { it.dx -= minX }
        breite = teile.maxOf { t -> t.alle().maxOf { it.mitteX } + t.dx } + masse.slot / 2
        oben = teile.filter { it.aufwaerts }.maxOfOrNull { t -> t.layout.plaetze.maxOf { it.ebene } } ?: 0
        unten = teile.filter { !it.aufwaerts }.maxOfOrNull { t -> t.layout.plaetze.maxOf { it.ebene } - t.versatz } ?: 0
        beideRichtungen = teile.any { it.aufwaerts } && teile.any { !it.aufwaerts }
        hoehe = (oben + unten + 1) * masse.ebeneH - masse.verbinder
        // Sanduhr: die Ausgangsperson steht in beiden Teilen und wird nur unten gezeichnet
        val doppelteWurzel = teile.any { !it.aufwaerts && !it.ohneWurzel } && teile.any { it.aufwaerts }
        gezeichnet = teile.flatMap { t ->
            t.layout.plaetze.filter { !(doppelteWurzel && t.aufwaerts && it.ebene == 0) && !t.istHalter(it) }
                .flatMap { pl -> listOf(t to pl) + geschwisterVon[pl].orEmpty().map { t to it } }
        }
        nummern = doppelteNummern(gezeichnet.map { it.second })
        paare = teile.flatMap { t -> t.layout.paare.map { PaarLage(t, it.vater, t, it.mutter, t, it.kinder) } } +
            (if (inhalt.paar == null) emptyList() else {
                val k = teile.getOrNull(2)
                listOf(PaarLage(teile[0], teile[0].layout.plaetze.first(), teile[1], teile[1].layout.plaetze.first(), k, k?.layout?.plaetze?.filter { it.eltern === k.layout.plaetze.first() }.orEmpty()))
            })
    }

    fun TafelTeil.alle() = layout.plaetze + layout.plaetze.flatMap { geschwisterVon[it].orEmpty() }
    fun TafelTeil.reihe(pl: TafelPlatz) = if (aufwaerts) oben - pl.ebene else oben + pl.ebene - versatz
    fun TafelTeil.oberkante(pl: TafelPlatz) = reihe(pl) * masse.ebeneH
    fun TafelTeil.x(pl: TafelPlatz) = pl.mitteX + dx

    // Gitter: Spalten zu drei Kastenbreiten entlang der Geschwister, Zeilen = Generationen vom Probanden aus
    // (Sanduhr: von oben durchgezaehlt, weil Vor- und Nachfahren sonst dieselbe Nummer haetten)
    val mitGitter = o.gitter || o.verzeichnis
    val zelle = masse.slot * 3
    val zeilenZahl = oben + unten + 1
    val zeilenName = HashMap<Int, String>().also { m ->
        gezeichnet.forEach { (t, pl) -> m.putIfAbsent(t.reihe(pl), roemisch(if (beideRichtungen || !t.aufwaerts) t.reihe(pl) + 1 else pl.ebene + 1)) }
    }
    /** Farbe je Platz (TafelFarben.kt); fehlt ein Platz, gilt die Farbe des Stils. */
    val farben: Map<TafelPlatz, Int> = kastenFarben(this)

    fun position(t: TafelTeil, pl: TafelPlatz) = "${spalteName((t.x(pl) / zelle).toInt())} ${zeilenName[t.reihe(pl)].orEmpty()}"
    /** Alle Gitterpositionen je Person. */
    val positionen = gezeichnet.filter { it.second.knoten.person.xref.isNotEmpty() }.groupBy({ it.second.knoten.person.xref }, { position(it.first, it.second) })
}

/**
 * Paar: die Ahnentafel der Frau so weit rechts neben die des Mannes, dass sich in keiner Reihe Kaesten beruehren und
 * zwischen den Partnern Platz fuer die Heiratslinie bleibt; die Kinder mittig unter dem Paar.
 */
private fun paarRuecken(teile: List<TafelTeil>, masse: TafelMasse) {
    val (mann, frau) = teile
    fun umriss(t: TafelTeil, rechts: Boolean) = t.layout.plaetze.groupBy { it.ebene }.mapValues { e -> if (rechts) e.value.maxOf { it.mitteX } else e.value.minOf { it.mitteX } }
    val r = umriss(mann, true); val l = umriss(frau, false)
    val xm = mann.layout.plaetze.first().mitteX; val xf = frau.layout.plaetze.first().mitteX
    var d = xm + masse.slot * 1.3f - xf
    r.forEach { (e, x) -> l[e]?.let { d = maxOf(d, x + masse.slot - it) } }
    frau.dx = d
    teile.getOrNull(2)?.let { k -> k.dx = (xm + xf + d) / 2 - k.layout.plaetze.first().mitteX }
}

internal fun spalteName(i: Int): String = if (i < 26) "${'A' + i}" else spalteName(i / 26 - 1) + ('A' + i % 26)

internal fun roemisch(n: Int): String {
    val werte = listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
    var r = n; val sb = StringBuilder()
    werte.forEach { (v, z) -> while (r >= v) { sb.append(z); r -= v } }
    return sb.toString()
}

/**
 * Masse des Blatts um die Anordnung: Rand, Titel oben, Gitterrand, Fusszeile. Senkrecht liegen die Geschwister
 * nebeneinander, waagerecht die Generationen. [skala] < 1: das Blatt waere groesser als PDF_MAX.
 */
internal class TafelBlatt(a: TafelAnordnung, titelSchrift: PDFont) {
    private val w = a.o.waagerecht
    val rand = maxOf(a.masse.rahmen * 0.5f, 28f)
    val titelGroesse = (a.masse.rahmen * 0.55f).coerceIn(22f, 72f)
    val titelBreite = if (a.o.titel.isBlank()) 0f else titelSchrift.breite(a.o.titel, titelGroesse)
    val titelH = if (a.o.titel.isBlank()) 0f else titelGroesse * 1.9f
    val fussH = 18f
    val gitterRand = if (a.mitGitter) maxOf(18f, a.masse.rahmen * 0.2f) else 0f
    val blattB = if (w) a.hoehe else a.breite
    val blattH = if (w) a.breite else a.hoehe
    val inhaltB = maxOf(blattB, titelBreite)
    val b = inhaltB + 2 * rand + 2 * gitterRand
    val h = rand + titelH + blattH + fussH + rand + 2 * gitterRand
    val skala = minOf(1f, PDF_MAX / b, PDF_MAX / h)
    /** Oben links des Inhalts, y von oben gezaehlt. */
    val x0 = rand + gitterRand + (inhaltB - blattB) / 2
    val y0 = rand + titelH + gitterRand
    /** PDF zaehlt y von unten. */
    fun py(y: Float) = h - y
    /** Linke obere Ecke einer Karte, y von oben (vor der Verkleinerung). */
    fun karteEcke(a: TafelAnordnung, t: TafelTeil, pl: TafelPlatz): Pair<Float, Float> = with(a) {
        if (w) (x0 + t.oberkante(pl)) to (y0 + t.x(pl) - masse.karteH / 2) else (x0 + t.x(pl) - masse.karteB / 2) to (y0 + t.oberkante(pl))
    }
    /** Punkt aus Generations- und Querachse in PDF-Koordinaten (vor der Verkleinerung). */
    fun px(g: Float, q: Float) = x0 + if (w) g else q
    fun pyv(g: Float, q: Float) = py(y0 + if (w) q else g)
}

/** Eine Zeile im Kasten. */
private class KastenZeile(val text: String, val schrift: PDFont, val groesse: Float)

/** Ort gekuerzt auf den ersten Teil ("Celle, Niedersachsen, Deutschland" -> "Celle"). */
private fun ort(e: EventJson?): String =
    e?.place?.name?.substringBefore(',')?.trim().orEmpty()

private fun datum(e: EventJson?, voll: Boolean): String =
    e?.date?.let { d -> if (voll) d.text.ifBlank { d.year.takeIf { it > 0 }?.toString().orEmpty() } else d.year.takeIf { it > 0 }?.toString().orEmpty() }.orEmpty()

/** Zusatzzeilen, die die Kaesten dieser Tafel brauchen: je ein Ort unter Geburt und Tod, bis zu zwei Partner. */
private fun zusatzZeilen(knoten: List<TafelPerson>, o: TafelOptionen): Int =
    (if (o.orte) 2 else 0) + knoten.maxOf { it.partner.size }.coerceAtMost(2)

/** Zeichnet die Anordnung auf das Blatt, Schritt fuer Schritt. */
private class TafelZeichner(
    private val a: TafelAnordnung, private val bl: TafelBlatt, private val cs: PDPageContentStream, private val doc: PDDocument,
    private val s: TafelSchriften, private val bilder: (Person) -> BufferedImage?, private val privat: String,
) {
    private val o = a.o
    private val m = a.masse
    private val w = o.waagerecht
    private val f = farben(o.stil)
    // Heiratszeichen: nicht jede Schrift hat ⚭ - dann das uebliche "oo"
    private val heirat = if (runCatching { s.normal.encode("⚭") }.isSuccess) "⚭" else "oo"
    private val bildCache = HashMap<String, PDImageXObject>()

    private fun text(t: String, schrift: PDFont, groesse: Float, x: Float, y: Float) {
        cs.beginText(); cs.setFont(schrift, groesse); cs.newLineAtOffset(x, y); cs.showText(schrift.sicher(t)); cs.endText()
    }

    /** Eine Linie zwischen zwei Punkten in Generations- und Querachse. */
    private fun linie(g1: Float, q1: Float, g2: Float, q2: Float) {
        cs.moveTo(bl.px(g1, q1), bl.pyv(g1, q1)); cs.lineTo(bl.px(g2, q2), bl.pyv(g2, q2))
    }

    fun hintergrund() {
        if (bl.skala < 1f) cs.transform(Matrix.getScaleInstance(bl.skala, bl.skala))
        if (f.hintergrundOben != f.hintergrundUnten) cs.verlauf(bl.b, bl.h, f.hintergrundOben, f.hintergrundUnten)
    }

    fun titel() {
        if (o.titel.isBlank()) return
        cs.setNonStrokingColor(f.titel)
        text(o.titel, s.titel, bl.titelGroesse, (bl.b - bl.titelBreite) / 2, bl.py(bl.rand + bl.titelGroesse * 1.05f))
    }

    /** Gitter am Rand: Buchstaben fuer die Spalten, roemische Zahlen fuer die Generationen, dazwischen kleine Striche. */
    fun gitter() {
        if (!a.mitGitter) return
        val g = maxOf(8f, m.rahmen * 0.09f)
        val gr = bl.gitterRand; val x0 = bl.x0; val y0 = bl.y0
        cs.setNonStrokingColor(f.linie); cs.setStrokingColor(f.linie); cs.setLineWidth(0.5f)
        fun beschriftung(t: String, x: Float, yVonOben: Float) = text(t, s.normal, g, x - s.normal.breite(t, g) / 2, bl.py(yVonOben + g * 0.35f))
        val zellen = Math.ceil((a.breite / a.zelle).toDouble()).toInt().coerceAtLeast(1)
        for (i in 0 until zellen) {
            val q = i * a.zelle + a.zelle / 2
            val name = spalteName(i)
            if (!w) { beschriftung(name, x0 + q, y0 - gr / 2); beschriftung(name, x0 + q, y0 + bl.blattH + gr / 2) }
            else { beschriftung(name, x0 - gr / 2, y0 + q); beschriftung(name, x0 + bl.blattB + gr / 2, y0 + q) }
            if (i > 0) {
                val grenze = i * a.zelle
                if (!w) listOf(y0 - gr * 0.8f to y0 - gr * 0.2f, y0 + bl.blattH + gr * 0.2f to y0 + bl.blattH + gr * 0.8f)
                    .forEach { (von, bis) -> cs.moveTo(x0 + grenze, bl.py(von)); cs.lineTo(x0 + grenze, bl.py(bis)) }
                else listOf(x0 - gr * 0.8f to x0 - gr * 0.2f, x0 + bl.blattB + gr * 0.2f to x0 + bl.blattB + gr * 0.8f)
                    .forEach { (von, bis) -> cs.moveTo(von, bl.py(y0 + grenze)); cs.lineTo(bis, bl.py(y0 + grenze)) }
            }
        }
        cs.stroke()
        for (r in 0 until a.zeilenZahl) {
            val name = a.zeilenName[r] ?: continue
            val gm = r * m.ebeneH + m.laengeG / 2
            if (!w) { beschriftung(name, x0 - gr / 2, y0 + gm); beschriftung(name, x0 + bl.blattB + gr / 2, y0 + gm) }
            else { beschriftung(name, x0 + gm, y0 - gr / 2); beschriftung(name, x0 + gm, y0 + bl.blattH + gr / 2) }
        }
    }

    /** Doppelte Personen: farbige Kurven zwischen den Vorkommen, halbtransparent unter allem anderen. */
    fun kurven() = with(a) {
        if (!o.kurven) return@with
        val palette = listOf(Color(0xD9, 0x4F, 0x4F), Color(0x3F, 0x7F, 0xD0), Color(0x4C, 0xA6, 0x4C), Color(0xE0, 0x9A, 0x2B), Color(0x8E, 0x5C, 0xC4), Color(0x2B, 0xA3, 0xA3))
        cs.saveGraphicsState(); cs.setGraphicsStateParameters(PDExtendedGraphicsState().apply { strokingAlphaConstant = 0.45f })
        cs.setLineWidth(maxOf(2.5f, m.rahmen * 0.03f))
        gezeichnet.filter { it.second.knoten.person.xref.isNotEmpty() }.groupBy { it.second.knoten.person.xref }.values.filter { it.size > 1 }
            .forEachIndexed { i, vorkommen ->
                cs.setStrokingColor(palette[i % palette.size])
                val punkte = vorkommen.map { (t, pl) -> val gm = t.oberkante(pl) + m.laengeG / 2; bl.px(gm, t.x(pl)) to bl.pyv(gm, t.x(pl)) }.sortedBy { it.first }
                punkte.zipWithNext().forEach { (von, bis) ->
                    val d = Math.hypot((bis.first - von.first).toDouble(), (bis.second - von.second).toDouble()).toFloat()
                    if (d < m.slot * 2) return@forEach
                    val bogen = d * 0.3f
                    cs.moveTo(von.first, von.second)
                    cs.curveTo(von.first, von.second + bogen, bis.first, bis.second + bogen, bis.first, bis.second)
                }
                cs.stroke()
            }
        cs.restoreGraphicsState()
    }

    /**
     * Verbindungen: von der Unterkante der Eltern senkrecht auf halbe Hoehe, waagerecht ueber alle Kinder, senkrecht
     * hinunter zu jedem Kind (Vorfahren spiegelbildlich: vom Bild nach oben zu Vater und Mutter). Danach die Paare.
     */
    fun linien() = with(a) {
        cs.setStrokingColor(f.linie); cs.setLineWidth(maxOf(0.6f, m.rahmen * 0.009f))
        teile.forEach { t ->
            t.layout.plaetze.groupBy { it.eltern }.forEach { (eltern, kinder) ->
                if (eltern == null || t.istHalter(eltern)) return@forEach
                val start = if (t.aufwaerts) t.oberkante(eltern) else t.oberkante(eltern) + m.laengeG
                val mitte = if (t.aufwaerts) start - m.verbinder / 2 else start + m.verbinder / 2
                val ex = t.x(eltern)
                linie(start, ex, mitte, ex)
                val xs = kinder.map { t.x(it) }
                // Geschwister haengen an derselben Linie wie die Person
                val gs = geschwisterVon[eltern].orEmpty().map { t.x(it) }
                gs.forEach { q -> linie(start, q, mitte, q) }
                val q1 = minOf(xs.min(), ex, gs.minOrNull() ?: ex); val q2 = maxOf(xs.max(), ex, gs.maxOrNull() ?: ex)
                linie(mitte, q1, mitte, q2)
                kinder.forEach { k ->
                    val ende = if (t.aufwaerts) t.oberkante(k) + m.laengeG else t.oberkante(k)
                    linie(mitte, t.x(k), ende, t.x(k))
                }
                cs.stroke()
            }
        }
        paare.forEach { paar(it) }
    }

    /** Heiratslinie zwischen den Kaesten eines Paares, aus ihrer Mitte hinunter zu den gemeinsamen Kindern. */
    private fun paar(p: PaarLage) = with(a) {
        val oberkante = p.vaterTeil.oberkante(p.vater)
        val g = oberkante + if (w) m.laengeG / 2 else m.bild + m.bildAbstand + m.kastenH / 2
        val q1 = p.vaterTeil.x(p.vater) + m.laengeQ / 2; val q2 = p.mutterTeil.x(p.mutter) - m.laengeQ / 2
        linie(g, q1, g, q2)
        val kt = p.kinderTeil
        if (kt != null && p.kinder.isNotEmpty()) {
            val qm = (q1 + q2) / 2
            val mitte = oberkante + m.laengeG + m.verbinder / 2
            linie(g, qm, mitte, qm)
            val xs = p.kinder.map { kt.x(it) }
            linie(mitte, minOf(xs.min(), qm), mitte, maxOf(xs.max(), qm))
            p.kinder.forEach { kind -> linie(mitte, kt.x(kind), kt.oberkante(kind), kt.x(kind)) }
        }
        cs.stroke()
    }

    private fun bildFuer(p: Person): PDImageXObject? {
        if (!o.bilder) return null
        val schluessel = if (p.isPrivate) "privat" else p.xref
        return bildCache.getOrPut(schluessel) {
            val foto = if (p.isPrivate) null else bilder(p)
            if (foto != null) JPEGFactory.createFromImage(doc, if (f.grau) grau(foto) else foto, 0.88f)
            else {
                val sil = silhouetteBild(p.sex, jahrhundert(p), 160)
                LosslessFactory.createFromImage(doc, if (f.grau) grau(sil) else sil)
            }
        }
    }

    /** Kleines Schild an der rechten oberen Bildecke (Nummer fuer Doppelte, "= 8" fuer Ahnenschwund). */
    private fun schild(text: String, cx: Float, cy: Float) {
        val r = m.rahmen * 0.075f; val g = r * 1.3f
        val sw = maxOf(2 * r, s.fett.breite(text, g) + r)
        cs.setNonStrokingColor(Color(0xFF, 0xF3, 0x9A)); cs.setStrokingColor(f.linie); cs.setLineWidth(0.5f)
        cs.rechteck(cx - sw / 2, cy - r, sw, 2 * r, r); cs.fillAndStroke()
        cs.setNonStrokingColor(f.text)
        text(text, s.fett, g, cx - s.fett.breite(text, g) / 2, cy - g * 0.35f)
    }

    /** Zeilen im Kasten: Vorname klein, Nachname fett, Geburt (und Ort), Tod (und Ort), Partner. */
    private fun zeilen(k: TafelPerson): List<KastenZeile> = buildList {
        val p = k.person
        if (p.isPrivate) { add(KastenZeile(privat, s.normal, m.schriftKlein)); return@buildList }
        val vor = p.given.ifBlank { if (p.surname.isBlank()) p.name else "" }
        add(KastenZeile(vor, s.fett, m.schriftKlein))
        add(KastenZeile(p.surname, s.fett, m.schriftName))
        listOf("*" to p.birth, "†" to p.death).forEach { (zeichen, e) ->
            val d = datum(e, o.volleDaten)
            if (d.isNotBlank()) add(KastenZeile("$zeichen $d", s.normal, m.schriftKlein))
            if (o.orte) ort(e).takeIf(String::isNotBlank)?.let { add(KastenZeile(it, s.normal, m.schriftKlein * 0.92f)) }
        }
        k.partner.take(2).forEach { add(KastenZeile("$heirat ${it.name.ifBlank { "?" }}", s.normal, m.schriftKlein * 0.92f)) }
    }

    /** Eine Karte: Hinweis, Gitterverweis, Bild mit Schild, Kasten mit Nummer und Zeilen. */
    fun karte(t: TafelTeil, platz: TafelPlatz) = with(a) {
        val k = platz.knoten
        val p = k.person
        val (karteL, karteO) = bl.karteEcke(a, t, platz)
        // Senkrecht: Bild oben mittig, Kasten darunter. Waagerecht: Bild links, Kasten rechts daneben, beide mittig.
        val bildL = if (w) karteL else karteL + (m.karteB - m.bild) / 2
        val oben = if (w) karteO + (m.karteH - m.bild) / 2 else karteO
        val links = if (w) karteL + m.bild + m.bildAbstand else karteL
        k.hinweis?.let { hw ->
            // Auf der Seite, wo die Vorfahren weitergehen
            val g = m.schriftKlein
            val tb = s.fett.breite(hw, g)
            val (hx, hy) = when {
                !w -> (karteL + m.karteB / 2 - tb / 2) to (if (t.aufwaerts) karteO - g * 0.6f else karteO + m.karteH + g * 1.3f)
                t.aufwaerts -> (karteL - tb - g * 0.5f) to (karteO + m.karteH / 2 + g * 0.35f)
                else -> (karteL + m.karteB + g * 0.5f) to (karteO + m.karteH / 2 + g * 0.35f)
            }
            cs.setNonStrokingColor(f.linie)
            text(hw, s.fett, g, hx, bl.py(hy))
        }
        // Mit Gitter: unter der Karte (waagerecht rechts daneben), wo die Person noch steht
        if (mitGitter) positionen[p.xref]?.takeIf { it.size > 1 }?.let { alle ->
            val andere = alle - position(t, platz)
            if (andere.isNotEmpty()) {
                val g = m.schriftKlein * 0.9f
                val (hx, hy) = if (!w) (karteL + m.karteB / 2 + g * 0.4f) to (karteO + m.karteH + g * 1.2f)
                    else (karteL + m.karteB + g * 0.4f) to (karteO + m.karteH - g * 0.2f)
                cs.setNonStrokingColor(f.linie)
                text("= " + andere.distinct().joinToString(", "), s.normal, g, hx, bl.py(hy))
            }
        }
        val schildText = k.verweis?.let { "= $it" } ?: nummern[p.xref]?.toString()
        // Bild mit feinem Rand
        bildFuer(p)?.let { img ->
            cs.drawImage(img, bildL, bl.py(oben + m.bild), m.bild, m.bild)
            cs.setStrokingColor(f.linie); cs.setLineWidth(0.5f); cs.addRect(bildL, bl.py(oben + m.bild), m.bild, m.bild); cs.stroke()
            schildText?.let { schild(it, bildL + m.bild, bl.py(oben)) }
        }
        // Kasten
        val ky = if (w) karteO + (m.karteH - m.kastenH) / 2 else oben + m.bild + m.bildAbstand
        val eigen = farben[platz]?.let { KASTEN_FARBEN[it.coerceIn(0, KASTEN_FARBEN.size - 1)] }
        cs.setNonStrokingColor(eigen?.fuellung ?: f.fuellung(p.sex)); cs.setStrokingColor(eigen?.rahmen ?: f.rahmen(p.sex)); cs.setLineWidth(f.rahmenBreite)
        cs.rechteck(links + f.rahmenBreite / 2, bl.py(ky + m.kastenH), m.rahmen - f.rahmenBreite, m.kastenH, if (f.rund) m.rahmen * 0.05f else 0f); cs.fillAndStroke()
        if (!o.bilder) schildText?.let { schild(it, links + m.rahmen, bl.py(ky)) }
        // Kekule-Nummer klein oben links im Kasten; die erste Zeile weicht ihr beidseitig aus
        val nummer = k.nummer?.takeIf { o.nummern }?.toString()
        val nummerG = m.schriftKlein * 0.85f
        val nummerB = nummer?.let { s.normal.breite(it, nummerG) + m.rahmen * 0.04f } ?: 0f
        if (nummer != null) {
            cs.setNonStrokingColor(f.linie)
            text(nummer, s.normal, nummerG, links + m.rahmen * 0.05f, bl.py(ky + nummerG * 1.25f))
        }
        cs.setNonStrokingColor(f.text)
        val innen = m.rahmen * 0.84f
        val mx = links + m.rahmen / 2
        var y = ky + m.rahmen * 0.06f
        zeilen(k).forEachIndexed { i, z ->
            y += z.groesse * 1.3f
            if (z.text.isNotBlank()) {
                val (tx, g) = passend(z.schrift, z.text, z.groesse, if (i == 0) innen - 2 * nummerB else innen)
                text(tx, z.schrift, g, mx - z.schrift.breite(tx, g) / 2, bl.py(y - z.groesse * 0.25f))
            }
        }
    }

    fun fuss(fuss: String) {
        cs.setNonStrokingColor(Color(0x66, 0x66, 0x66))
        text(fuss, s.normal, 7f, bl.rand, bl.rand * 0.6f)
    }
}

/**
 * Eine Tafel als PDF mit einem Blatt. [bilder] liefert je Person ihr Portraet (null = Silhouette); [privat] ist
 * der Text fuer Personen, die der Server nicht zeigt. Vorfahren wachsen nach oben, Nachfahren nach unten; hat die
 * Tafel beides (Sanduhr), steht die Ausgangsperson einmal in der Mitte.
 */
fun tafelPdf(
    inhalt: TafelInhalt, o: TafelOptionen, bilder: (Person) -> BufferedImage?, privat: String, fuss: String,
): Pair<PDDocument, TafelInfo> {
    val a = TafelAnordnung(inhalt, o)
    val doc = PDDocument()
    val s = TafelSchriften(doc, o.stil)
    val bl = TafelBlatt(a, s.titel)
    val page = PDPage(PDRectangle(bl.b * bl.skala, bl.h * bl.skala))
    // Groesser als PDF_MAX: die Seite ist verkleinert gezeichnet, die UserUnit nennt das echte Mass
    if (bl.skala < 1f) page.userUnit = 1f / bl.skala
    doc.addPage(page)
    PDPageContentStream(doc, page).use { cs ->
        TafelZeichner(a, bl, cs, doc, s, bilder, privat).apply {
            hintergrund(); titel(); gitter(); kurven(); linien()
            a.gezeichnet.forEach { (t, platz) -> karte(t, platz) }
            fuss(fuss)
        }
    }
    if (o.verzeichnis) tafelVerzeichnis(doc, o.titel, verzeichnisEintraege(a, bl))
    val karten = a.gezeichnet.map { (t, pl) ->
        val (l, oben) = bl.karteEcke(a, t, pl)
        KartenOrt(pl.knoten.person, l * bl.skala, oben * bl.skala, a.masse.karteB * bl.skala, a.masse.karteH * bl.skala)
    }
    val info = TafelInfo(a.gezeichnet.map { it.second.knoten.person.xref }.distinct().size, (bl.b / 72f * 2.54f).toInt(), (bl.h / 72f * 2.54f).toInt(),
        seiteB = bl.b * bl.skala, seiteH = bl.h * bl.skala, einheit = 1f / bl.skala, karten = karten,
        // Unten bleibt die Fusszeile (sie steht bei 0,6 Rand)
        bereich = Bereich(bl.rand * bl.skala, bl.rand * 0.4f * bl.skala, (bl.b - 2 * bl.rand) * bl.skala, (bl.h - 1.4f * bl.rand) * bl.skala))
    return doc to info
}

/** Personenverzeichnis: je Person ihre Gitterpositionen und das Linkziel auf ihre erste Karte, nach Namen sortiert. */
private fun verzeichnisEintraege(a: TafelAnordnung, bl: TafelBlatt): List<VerzeichnisEintrag> = with(a) {
    val m = masse; val w = o.waagerecht
    gezeichnet.filter { it.second.knoten.person.xref.isNotEmpty() && !it.second.knoten.person.isPrivate }
        .groupBy { it.second.knoten.person.xref }.map { (_, v) ->
            val (t, pl) = v.first()
            val gm = t.oberkante(pl); val q = t.x(pl)
            // Ziel im Blatt (PDF-Koordinaten, schon mit der Verkleinerung)
            val zx = (bl.x0 + if (w) gm else q - m.karteB / 2) * bl.skala
            val zy = (bl.h - (bl.y0 + if (w) q - m.karteH / 2 else gm)) * bl.skala
            VerzeichnisEintrag(pl.knoten.person, v.map { (tt, p2) -> position(tt, p2) }.distinct(), 0, zx, zy)
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { registerName(it.person) })
}

/** Ein Eintrag im Personenverzeichnis: wo die Person steht (Gitterpositionen oder Seiten) und wohin der Link fuehrt. */
internal class VerzeichnisEintrag(val person: Person, val stellen: List<String>, val seite: Int, val x: Float, val y: Float)

/** Personenverzeichnis zur Tafel: zweispaltig auf A4, "Name (Lebensdaten) ..... C III", jede Zeile ein Link. */
internal fun tafelVerzeichnis(doc: PDDocument, titel: String, eintraege: List<VerzeichnisEintrag>) {
    val schrift = Schriften(doc)
    val a4 = PDRectangle.A4
    val rand = 50f; val abstand = 20f
    val sw = (a4.width - 2 * rand - abstand) / 2
    val g = 8.5f; val zh = g * 1.4f
    var cs: PDPageContentStream? = null
    var seite: PDPage? = null
    var y = 0f; var spalte = 2
    fun neueSpalte() {
        spalte++
        if (spalte >= 2) {
            cs?.close()
            seite = PDPage(a4).also { doc.addPage(it) }
            cs = PDPageContentStream(doc, seite)
            val kopf = Texte.t(Res.string.desk_chart_index_title) + if (titel.isNotBlank()) " – $titel" else ""
            cs!!.beginText(); cs!!.setFont(schrift.fett, 12f); cs!!.newLineAtOffset(rand, a4.height - rand); cs!!.showText(schrift.fett.sicher(kopf)); cs!!.endText()
            spalte = 0
        }
        y = a4.height - rand - 26f
    }
    neueSpalte()
    val ziele = eintraege.map { it.seite }.distinct().associateWith { doc.getPage(it) }
    eintraege.forEach { e ->
        val p = e.person; val pos = e.stellen
        if (y < rand) neueSpalte()
        val x = rand + spalte * (sw + abstand)
        val name = registerName(p) + p.lifespan.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()
        val rechts = pos.joinToString(", ")
        val rb = schrift.normal.breite(rechts, g)
        val (nt, ng) = passend(schrift.normal, name, g, sw - rb - 10f)
        val c = cs!!
        c.beginText(); c.setFont(schrift.normal, ng); c.newLineAtOffset(x, y); c.showText(schrift.normal.sicher(nt)); c.endText()
        val nb = schrift.normal.breite(nt, ng)
        val punkt = schrift.normal.breite(".", g)
        val sb = StringBuilder(); var px = x + nb + 3f
        while (px + punkt < x + sw - rb - 3f) { sb.append('.'); px += punkt }
        c.beginText(); c.setFont(schrift.normal, g); c.newLineAtOffset(x + nb + 3f, y); c.showText(sb.toString()); c.endText()
        c.beginText(); c.setFont(schrift.normal, g); c.newLineAtOffset(x + sw - rb, y); c.showText(schrift.normal.sicher(rechts)); c.endText()
        seite!!.annotations.add(org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink().apply {
            rectangle = PDRectangle(x, y - g * 0.3f, sw, zh)
            borderStyle = org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary().apply { width = 0f }
            action = org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo().apply {
                destination = org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination().apply {
                    page = ziele.getValue(e.seite); left = e.x.toInt(); top = e.y.toInt()
                }
            }
        })
        y -= zh
    }
    cs?.close()
}