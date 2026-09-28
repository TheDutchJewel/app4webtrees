package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.res.*
import org.apache.pdfbox.pdmodel.font.PDFont

/*
 * Legende der Tafel (C4, 28.09.2026): unten rechts, nur mit dem, was auf dieser Tafel vorkommt - die Farben (Linien,
 * Regeln, gefaerbte Zweige) und die Zeichen (* † ⚭, Kekule-Nummer, Doppelte, Gitter- und Seitenverweise).
 */

/** Wie ein Eintrag links gezeichnet wird: Farbfeld, Zeichen als Text, gelbes Schild (wie an doppelten Personen). */
internal enum class LegendenArt { Farbe, Zeichen, Schild, Nummer }

internal class LegendenEintrag(val art: LegendenArt, val zeichen: String, val text: String, val farbe: Int = 0)

internal fun legendenEintraege(a: TafelAnordnung): List<LegendenEintrag> = buildList {
    a.farbBedeutung.forEach { (f, t) -> add(LegendenEintrag(LegendenArt.Farbe, "", t, f)) }
    val knoten = a.gezeichnet.map { it.second.knoten }
    fun z(zeichen: String, text: StringResource) = add(LegendenEintrag(LegendenArt.Zeichen, zeichen, Texte.t(text)))
    z("*", Res.string.desk_legend_born)
    z("†", Res.string.desk_legend_died)
    if (knoten.any { it.partner.isNotEmpty() }) add(LegendenEintrag(LegendenArt.Zeichen, "⚭", Texte.t(Res.string.desk_legend_married)))
    if (a.o.nummern && knoten.any { it.nummer != null }) add(LegendenEintrag(LegendenArt.Nummer, "4", Texte.t(Res.string.desk_legend_kekule)))
    if (a.nummern.isNotEmpty()) add(LegendenEintrag(LegendenArt.Schild, "1", Texte.t(Res.string.desk_legend_double)))
    if (knoten.any { it.verweis != null }) add(LegendenEintrag(LegendenArt.Schild, "= 8", Texte.t(Res.string.desk_legend_ref)))
    if (a.mitGitter && a.positionen.values.any { it.distinct().size > 1 }) add(LegendenEintrag(LegendenArt.Zeichen, "= C III", Texte.t(Res.string.desk_legend_grid)))
    if (knoten.any { it.hinweis?.startsWith("→") == true }) add(LegendenEintrag(LegendenArt.Zeichen, "→ S. 5", Texte.t(Res.string.desk_legend_page)))
}

private typealias StringResource = org.jetbrains.compose.resources.StringResource

/** Masse der Legende: Schrift, Spalten, Zeilen; [b]/[h] in Punkt (0, wenn keine Legende). */
internal class LegendenMass(eintraege: List<LegendenEintrag>, schrift: PDFont, fett: PDFont, rahmen: Float, verfuegbar: Float) {
    val g = maxOf(9f, rahmen * 0.09f)
    val zeileH = g * 1.7f
    val feldB = g * 3.4f
    val spaltenB = feldB + (eintraege.maxOfOrNull { schrift.breite(it.text, g) } ?: 0f) + g * 2f
    /** Hoechstens vier Spalten: die Legende bleibt ein kompakter Block, auch unter sehr breiten Tafeln. */
    val spalten = if (eintraege.isEmpty()) 0 else (verfuegbar / spaltenB).toInt().coerceIn(1, minOf(4, eintraege.size))
    val zeilen = if (spalten == 0) 0 else (eintraege.size + spalten - 1) / spalten
    val kopfH = g * 2.2f
    val b = maxOf(spalten * spaltenB, if (eintraege.isEmpty()) 0f else fett.breite(Texte.t(Res.string.desk_legend_title), g * 1.1f))
    val h = if (eintraege.isEmpty()) 0f else kopfH + zeilen * zeileH + g
}
