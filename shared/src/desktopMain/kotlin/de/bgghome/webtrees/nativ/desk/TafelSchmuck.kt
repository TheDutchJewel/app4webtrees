package de.bgghome.webtrees.nativ.desk

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.util.Matrix
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random

/*
 * Hintergrund und Schmuckrahmen der Tafel (C3, 28.09.2026): mitgelieferte Hintergruende (Pergament-Verlauf,
 * Papierstruktur, heller Verlauf) oder ein eigenes Bild blass unterlegt (Wappen, Landkarte), dazu gezeichnete Rahmen
 * (doppelte Linie, Ornament-Ecken, Blattranken). Alles in Vektor, die Papierstruktur als kleine gekachelte Grafik.
 */

enum class TafelHintergrund { Stil, Weiss, Pergament, Papier, Verlauf, Bild }

enum class Schmuckrahmen { Keiner, Doppelt, Ecken, Ranken }

/** Eine Kachel Papier: warmer Grundton, feines Rauschen und einzelne Fasern (immer gleich, damit Vorschau und Druck passen). */
private val papierKachel: BufferedImage by lazy {
    val n = 256
    val r = Random(1808)
    BufferedImage(n, n, BufferedImage.TYPE_INT_RGB).also { b ->
        for (y in 0 until n) for (x in 0 until n) {
            val d = r.nextInt(-7, 8)
            b.setRGB(x, y, Color((0xF6 + d).coerceIn(0, 255), (0xF0 + d).coerceIn(0, 255), (0xE1 + d).coerceIn(0, 255)).rgb)
        }
        val g = b.createGraphics()
        repeat(60) {
            g.color = Color(0xD8 + r.nextInt(-10, 10), 0xCC + r.nextInt(-10, 10), 0xB0, 90)
            val x = r.nextInt(n); val y = r.nextInt(n)
            g.drawLine(x, y, x + r.nextInt(-14, 14), y + r.nextInt(-5, 5))
        }
        g.dispose()
    }
}

/**
 * Hintergrund ueber das ganze Blatt [b] x [h]. [stilOben]/[stilUnten]: der Verlauf des Stils (bei "wie der Stil").
 * [bild]: Datei fuer "eigenes Bild", deckend skaliert und blass.
 */
internal fun PDPageContentStream.tafelHintergrund(
    doc: PDDocument, art: TafelHintergrund, b: Float, h: Float, stilOben: Color, stilUnten: Color, bild: String,
) {
    when (art) {
        TafelHintergrund.Stil -> if (stilOben != stilUnten) verlauf(b, h, stilOben, stilUnten)
        TafelHintergrund.Weiss -> {}
        TafelHintergrund.Pergament -> verlauf(b, h, Color(0xFF, 0xFF, 0xFF), Color(0xFD, 0xEE, 0xBE))
        TafelHintergrund.Verlauf -> verlauf(b, h, Color(0xFF, 0xFF, 0xFF), Color(0xE3, 0xEC, 0xF4))
        TafelHintergrund.Papier -> {
            val img = LosslessFactory.createFromImage(doc, papierKachel)
            val k = 180f
            saveGraphicsState(); addRect(0f, 0f, b, h); clip()
            var y = 0f
            while (y < h) { var x = 0f; while (x < b) { drawImage(img, x, y, k, k); x += k }; y += k }
            restoreGraphicsState()
        }
        TafelHintergrund.Bild -> {
            val quelle = runCatching { ImageIO.read(File(bild)) }.getOrNull() ?: return
            // In RGB (JPEG kennt keine Transparenz), deckend ueber das Blatt, blass
            val rgb = BufferedImage(quelle.width, quelle.height, BufferedImage.TYPE_INT_RGB).also { it.createGraphics().apply { color = Color.WHITE; fillRect(0, 0, quelle.width, quelle.height); drawImage(quelle, 0, 0, null); dispose() } }
            val img = JPEGFactory.createFromImage(doc, rgb, 0.85f)
            val k = maxOf(b / img.width, h / img.height)
            val iw = img.width * k; val ih = img.height * k
            saveGraphicsState(); addRect(0f, 0f, b, h); clip()
            setGraphicsStateParameters(PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = 0.18f })
            drawImage(img, (b - iw) / 2, (h - ih) / 2, iw, ih)
            restoreGraphicsState()
        }
    }
}

/** Platz, den ein Schmuckrahmen am Rand braucht, als Vielfaches des normalen Rands. */
internal fun schmuckRand(art: Schmuckrahmen) = when (art) { Schmuckrahmen.Keiner -> 1f; Schmuckrahmen.Doppelt -> 1.5f; else -> 2.2f }

/** Rahmen um das Blatt [b] x [h]; [rand]: der (schon verbreiterte) Blattrand, [farbe]: Farbe des Stils. */
internal fun PDPageContentStream.schmuckrahmen(art: Schmuckrahmen, b: Float, h: Float, rand: Float, farbe: Color) {
    if (art == Schmuckrahmen.Keiner) return
    setStrokingColor(farbe); setNonStrokingColor(farbe)
    val d = rand * 0.35f
    when (art) {
        Schmuckrahmen.Doppelt -> {
            setLineWidth(maxOf(1.5f, rand * 0.05f)); addRect(d, d, b - 2 * d, h - 2 * d); stroke()
            val i = d + rand * 0.1f
            setLineWidth(maxOf(0.5f, rand * 0.015f)); addRect(i, i, b - 2 * i, h - 2 * i); stroke()
        }
        Schmuckrahmen.Ecken -> {
            val i = d + rand * 0.12f
            setLineWidth(maxOf(0.6f, rand * 0.018f)); addRect(i, i, b - 2 * i, h - 2 * i); stroke()
            // Die Ecke unten links, fuer die anderen gespiegelt
            listOf(Triple(i, i, 1f to 1f), Triple(b - i, i, -1f to 1f), Triple(i, h - i, 1f to -1f), Triple(b - i, h - i, -1f to -1f)).forEach { (x, y, s) ->
                saveGraphicsState()
                transform(Matrix(s.first, 0f, 0f, s.second, x, y))
                ornamentEcke(rand * 0.9f)
                restoreGraphicsState()
            }
        }
        Schmuckrahmen.Ranken -> {
            val i = d + rand * 0.35f
            setLineWidth(maxOf(0.5f, rand * 0.012f)); addRect(i, i, b - 2 * i, h - 2 * i); stroke()
            val a = d + rand * 0.35f / 2
            val amp = rand * 0.08f
            ranke(a, a, b - a, a, amp); ranke(b - a, a, b - a, h - a, amp)
            ranke(b - a, h - a, a, h - a, amp); ranke(a, h - a, a, a, amp)
        }
        Schmuckrahmen.Keiner -> {}
    }
}

/** Eine verschnoerkelte Ecke im Ursprung, nach rechts oben (Groesse [g]): zwei Voluten und eine Raute. */
private fun PDPageContentStream.ornamentEcke(g: Float) {
    setLineWidth(maxOf(0.8f, g * 0.03f))
    // Voluten entlang beider Kanten
    for (spiegel in listOf(false, true)) {
        fun p(x: Float, y: Float) = if (spiegel) y to x else x to y
        val (x0, y0) = p(g * 0.15f, 0f)
        moveTo(x0, y0)
        val (c1x, c1y) = p(g * 0.55f, g * 0.02f); val (c2x, c2y) = p(g * 0.9f, g * 0.22f); val (ex, ey) = p(g * 0.8f, g * 0.38f)
        curveTo(c1x, c1y, c2x, c2y, ex, ey)
        val (d1x, d1y) = p(g * 0.7f, g * 0.5f); val (d2x, d2y) = p(g * 0.52f, g * 0.36f); val (fx, fy) = p(g * 0.62f, g * 0.28f)
        curveTo(d1x, d1y, d2x, d2y, fx, fy)
        stroke()
    }
    // Bogen in die Ecke und eine kleine Raute
    moveTo(g * 0.15f, 0f); curveTo(g * 0.18f, g * 0.18f, g * 0.18f, g * 0.18f, 0f, g * 0.15f); stroke()
    val m = g * 0.3f; val r = g * 0.07f
    moveTo(m, m - r); lineTo(m + r, m); lineTo(m, m + r); lineTo(m - r, m); closePath(); fill()
}

/** Eine Ranke von (x1, y1) nach (x2, y2): Wellenlinie mit Blaettern abwechselnd auf beiden Seiten. */
private fun PDPageContentStream.ranke(x1: Float, y1: Float, x2: Float, y2: Float, amp: Float) {
    val laenge = Math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
    val welle = amp * 7f
    val n = maxOf(2, (laenge / welle).toInt())
    val ux = (x2 - x1) / laenge; val uy = (y2 - y1) / laenge
    val nx = -uy; val ny = ux
    val schritt = laenge / n
    setLineWidth(maxOf(0.6f, amp * 0.12f))
    moveTo(x1, y1)
    for (k in 0 until n) {
        val s = if (k % 2 == 0) 1f else -1f
        val a = k * schritt; val e = (k + 1) * schritt
        curveTo(x1 + ux * (a + schritt * 0.3f) + nx * amp * s, y1 + uy * (a + schritt * 0.3f) + ny * amp * s,
            x1 + ux * (a + schritt * 0.7f) + nx * amp * s, y1 + uy * (a + schritt * 0.7f) + ny * amp * s,
            x1 + ux * e, y1 + uy * e)
    }
    stroke()
    // Blaetter: kleine Ellipsen an jedem Wellenberg, schraeg zur Linie
    for (k in 0 until n) {
        val s = if (k % 2 == 0) 1f else -1f
        val mx = x1 + ux * (k + 0.5f) * schritt + nx * amp * 0.75f * s
        val my = y1 + uy * (k + 0.5f) * schritt + ny * amp * 0.75f * s
        saveGraphicsState()
        val winkel = Math.atan2(uy.toDouble(), ux.toDouble()) + s * 0.8
        transform(Matrix.getRotateInstance(winkel, mx, my))
        ellipse(0f, -amp * 0.35f, amp * 1.6f, amp * 0.7f); fill()
        restoreGraphicsState()
    }
}
