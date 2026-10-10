package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.Bereich

/*
 * Gesichter auf Gruppenbildern (Modul Sammlungen ab Stufe 5): Rechnen zwischen Bildschirm und Bild. Die Bereiche sind
 * normiert (0-1, links oben); gezeigt wird das Bild eingepasst (wie ContentScale.Fit), also mit Rand links/rechts oder
 * oben/unten. Ohne Compose-Typen, damit Desktop und Handy dasselbe rechnen und es sich ohne Oberflaeche testen laesst.
 */
object Gesichter {
    /** Wo das eingepasste Bild in einem Feld [feldB]×[feldH] liegt: links, oben, Breite, Hoehe (in Feld-Einheiten). */
    data class Lage(val links: Float, val oben: Float, val breite: Float, val hoehe: Float)

    fun eingepasst(feldB: Float, feldH: Float, bildB: Int, bildH: Int): Lage {
        if (bildB <= 0 || bildH <= 0 || feldB <= 0f || feldH <= 0f) return Lage(0f, 0f, feldB, feldH)
        val massstab = minOf(feldB / bildB, feldH / bildH)
        val b = bildB * massstab
        val h = bildH * massstab
        return Lage((feldB - b) / 2, (feldH - h) / 2, b, h)
    }

    /** Punkt im Feld → normierter Punkt aufs Bild (kann ausserhalb 0-1 liegen, wenn er im Rand liegt). */
    fun normiert(lage: Lage, x: Float, y: Float): Pair<Double, Double> =
        ((x - lage.links) / lage.breite).toDouble() to ((y - lage.oben) / lage.hoehe).toDouble()

    /** Welcher Bereich unter dem normierten Punkt liegt; bei Ueberlappung der kleinste (das Kind vor der Mutter). */
    fun treffer(bereiche: List<Bereich>, nx: Double, ny: Double): Int? =
        bereiche.indices
            .filter { val b = bereiche[it]; nx >= b.x && nx <= b.x + b.w && ny >= b.y && ny <= b.y + b.h }
            .minByOrNull { bereiche[it].w * bereiche[it].h }

    /** Kleinste Kantenlaenge eines gezogenen Rahmens (normiert) - kuerzere Zuege sind Klicks, kein Rahmen. */
    const val MINDEST = 0.01

    /** Rahmen aus zwei gezogenen Ecken (normiert, beliebige Richtung), aufs Bild beschnitten; null, wenn zu klein. */
    fun gezogen(ax: Double, ay: Double, bx: Double, by: Double): Bereich? {
        val x1 = minOf(ax, bx).coerceIn(0.0, 1.0)
        val x2 = maxOf(ax, bx).coerceIn(0.0, 1.0)
        val y1 = minOf(ay, by).coerceIn(0.0, 1.0)
        val y2 = maxOf(ay, by).coerceIn(0.0, 1.0)
        if (x2 - x1 < MINDEST || y2 - y1 < MINDEST) return null
        return Bereich(x = x1, y = y1, w = x2 - x1, h = y2 - y1)
    }

    /**
     * Lesereihenfolge wie auf der Rueckseite alter Fotos: Reihe fuer Reihe von oben, darin von links nach rechts. Zur
     * selben Reihe gehoert, wessen Mitte hoeher liegt als die halbe Rahmenhoehe unter der Mitte des ersten der Reihe.
     */
    fun reihenfolge(bereiche: List<Bereich>): List<Int> {
        val nachOben = bereiche.indices.sortedBy { bereiche[it].y + bereiche[it].h / 2 }
        val reihen = mutableListOf<MutableList<Int>>()
        nachOben.forEach { i ->
            val b = bereiche[i]
            val mitte = b.y + b.h / 2
            val reihe = reihen.lastOrNull()
            val erster = reihe?.first()?.let { bereiche[it] }
            if (erster != null && mitte - (erster.y + erster.h / 2) < erster.h / 2) reihe.add(i) else reihen.add(mutableListOf(i))
        }
        return reihen.flatMap { r -> r.sortedBy { bereiche[it].x } }
    }

    /** Pixel-Rahmen (links oben, Breite, Hoehe) eines Bildes [bildB]×[bildH] → normierter Bereich. */
    fun ausPixeln(x: Int, y: Int, w: Int, h: Int, bildB: Int, bildH: Int, name: String = "", xref: String? = null) =
        Bereich(x = x.toDouble() / bildB, y = y.toDouble() / bildH, w = w.toDouble() / bildB, h = h.toDouble() / bildH, name = name, xref = xref)
}
