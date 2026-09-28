package de.bgghome.webtrees.nativ.desk

import de.bgghome.webtrees.nativ.Texte
import de.bgghome.webtrees.nativ.api.Person
import de.bgghome.webtrees.nativ.res.*
import org.apache.pdfbox.pdmodel.PDDocument
import java.awt.image.BufferedImage

/*
 * Seitenweise Stammtafel (B4, 28.09.2026): die Nachfahren auf A4-Seiten zu [TafelOptionen.jeSeite] Generationen.
 * Wer in der untersten Reihe einer Seite noch Nachfahren hat, beginnt eine eigene Seite und bekommt "-> S. n".
 * Vorn eine Seitenuebersicht (der Baum der Seiten), hinten auf Wunsch ein Personenverzeichnis mit Seitenzahlen.
 */

/** So breit (in Kaesten) darf eine Seite werden, bevor sie eine Generation weniger nimmt - A4 quer, auf ~65 % verkleinert. */
private const val MAX_BREITE_SENKRECHT = 12
private const val MAX_BREITE_WAAGERECHT = 8

/** Eine Seite: ihre Wurzel, wie viele Generationen sie zeigt, ob Kinder angehaengt sind, von welcher Seite sie weitergeht. */
private class StammSeite(val wurzel: TafelPerson, val tiefe: Int, val anhaengen: Boolean, val von: Int?)

/** Hat [k] nur noch Kinder ohne eigene Nachfahren? Dann kommen sie mit auf die Seite, statt eine eigene zu fuellen. */
private fun nurNochKinder(k: TafelPerson) = k.kinder.isNotEmpty() && k.kinder.all { it.kinder.isEmpty() }

/**
 * Die Person mit ihren Nachfahren, hoechstens [tiefe] Reihen; [hinweis] je Person der untersten Reihe, die weitergeht.
 * [anhaengen]: in der untersten Reihe haengen Kinder ohne eigene Nachfahren noch als eine Reihe darunter.
 */
private fun gekuerzt(k: TafelPerson, tiefe: Int, anhaengen: Boolean = false, hinweis: (TafelPerson) -> String? = { null }): TafelPerson = when {
    tiefe > 1 -> TafelPerson(k.person, k.kinder.map { gekuerzt(it, tiefe - 1, anhaengen, hinweis) }, k.nummer, k.partner)
    anhaengen && nurNochKinder(k) -> TafelPerson(k.person, k.kinder.map { TafelPerson(it.person, emptyList(), it.nummer, it.partner) }, k.nummer, k.partner)
    else -> TafelPerson(k.person, emptyList(), k.nummer, k.partner, hinweis = hinweis(k))
}

/** Die Personen der untersten Reihe einer Seite. */
private fun untersteReihe(k: TafelPerson, tiefe: Int): List<TafelPerson> = if (tiefe <= 1) listOf(k) else k.kinder.flatMap { untersteReihe(it, tiefe - 1) }

/** Wer von der untersten Reihe aus auf einer eigenen Seite weitergeht. */
private fun fortsetzungen(k: TafelPerson, tiefe: Int, anhaengen: Boolean) =
    untersteReihe(k, tiefe).filter { it.kinder.isNotEmpty() && !(anhaengen && nurNochKinder(it)) }

fun stammSeitenPdf(
    d: TafelDaten, o: TafelOptionen, bilder: (Person) -> BufferedImage?, privat: String, fuss: String,
): Pair<PDDocument, TafelInfo>? {
    val baum = d.nachfahren?.let { tafelBaum(it, o.generationen, o.namenstraeger, o.partner) } ?: return null
    val masse = TafelMasse(o.rahmenMm * 72f / 25.4f, o.bilder, 0, o.waagerecht, o.fotoLinks, o.form == KastenForm.Schild)
    val maxBreite = if (o.waagerecht) MAX_BREITE_WAAGERECHT else MAX_BREITE_SENKRECHT
    // Seiten der Reihe nach (Breitensuche): jede so tief wie eingestellt, aber nicht breiter als maxBreite Kaesten
    val seiten = ArrayList<StammSeite>()
    val warteschlange = ArrayDeque(listOf(baum to null as Int?))
    while (warteschlange.isNotEmpty()) {
        val (w, von) = warteschlange.removeFirst()
        fun zuBreit(t: Int, anhaengen: Boolean) = stammtafelLayout(gekuerzt(w, t, anhaengen), masse).breite / masse.slot > maxBreite
        var t = o.jeSeite.coerceAtLeast(2)
        while (t > 2 && zuBreit(t, false)) t--
        val anhaengen = !zuBreit(t, true)
        val nr = seiten.size
        seiten += StammSeite(w, t, anhaengen, von)
        fortsetzungen(w, t, anhaengen).forEach { warteschlange += it to nr }
    }
    // Seitenzahlen im fertigen Heft: die Uebersicht steht davor
    val vorn = if (o.uebersicht) 1 else 0
    val seiteVon = seiten.withIndex().associate { (i, s) -> s.wurzel to i + 1 + vorn }
    val ziel = PDDocument()
    val einzeln = o.copy(verzeichnis = false, gitter = false, kurven = false)
    if (o.uebersicht) {
        // Der Baum der Seiten: je Seite ihre Wurzel, darunter die Seiten, die von ihr weitergehen
        fun knoten(i: Int): TafelPerson = TafelPerson(seiten[i].wurzel.person, seiten.indices.filter { seiten[it].von == i }.map(::knoten),
            hinweis = Texte.t(Res.string.desk_chart_page_short, i + 1 + vorn))
        val (poster, _) = tafelPdf(TafelInhalt(nachfahren = knoten(0)), einzeln.copy(titel = Texte.t(Res.string.desk_chart_overview_title, o.titel), bilder = false,
            orte = false, volleDaten = false, partner = false, rahmenMm = 25, waagerecht = false), bilder, privat, fuss)
        aufEinBlatt(poster, ziel)
    }
    val stellen = LinkedHashMap<String, MutableList<Int>>()
    val personen = HashMap<String, Person>()
    seiten.forEachIndexed { i, s ->
        val inhalt = gekuerzt(s.wurzel, s.tiefe, s.anhaengen) { k -> seiteVon[k]?.takeIf { k !== s.wurzel }?.let { Texte.t(Res.string.desk_chart_page_ref, it) } }
        alleKnoten(inhalt).filter { it.person.xref.isNotEmpty() && !it.person.isPrivate }.forEach {
            stellen.getOrPut(it.person.xref) { mutableListOf() }.let { l -> if (i + 1 + vorn !in l) l += i + 1 + vorn }
            personen[it.person.xref] = it.person
        }
        val titel = if (s.von == null) o.titel else Texte.t(Res.string.desk_chart_desc_page_title, s.wurzel.person.name, s.von + 1 + vorn)
        val (poster, _) = tafelPdf(TafelInhalt(nachfahren = inhalt), einzeln.copy(titel = titel), bilder, privat, "$fuss · ${i + 1 + vorn}/${seiten.size + vorn}")
        aufEinBlatt(poster, ziel, querErzwingen = !o.waagerecht)
    }
    if (o.verzeichnis) {
        val eintraege = stellen.map { (x, s) ->
            val seite = ziel.getPage(s.first() - 1)
            VerzeichnisEintrag(personen.getValue(x), s.map { Texte.t(Res.string.desk_chart_page_short, it) }, s.first() - 1, 0f, seite.mediaBox.height)
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { registerName(it.person) })
        tafelVerzeichnis(ziel, o.titel, eintraege)
    }
    return ziel to TafelInfo(personen.size, 30, 21, ziel.numberOfPages)
}
