package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.FactJson
import de.bgghome.webtrees.nativ.api.SourceRef

/*
 * Paten, Trauzeugen und Heiratsart - die gemeinsame Logik fuer alle Ansichten (Lebenslauf, Personenblatt,
 * Karteikarte, Familienansicht, Listen, Buecher, Pruefung). Die Daten kommen fertig von api4webtrees ab
 * API-Stufe 19 (associates, freeAssociates, noteKinds, typeLabel); die App parst keine Notizen mehr, nur
 * als Rueckfall fuer aeltere Module bleibt die Notiz "Paten: ..." lesbar.
 */

/** Rolle eines Beteiligten, aus `role` der API (normalisiert: godparent, witness, sonst other). */
enum class PatenRolle {
    Paten, Trauzeugen, Beteiligte;

    companion object {
        fun aus(role: String): PatenRolle = when (role.lowercase()) {
            "godparent" -> Paten
            "witness" -> Trauzeugen
            else -> Beteiligte
        }
    }
}

/**
 * Ein Eintrag der Paten-Zeile: verlinkt ([xref] gesetzt, Name aus dem Datensatz) oder frei ([xref] null, nur Text).
 * Bei [privat] fehlt der Name - die Anzeige sagt nur "privat", nie einen geratenen Namen.
 */
data class PatenEintrag(
    val xref: String?,
    val text: String,
    /** Die Rolle uebersetzt ("Patin", "Zeuge"); leer bei freien Eintraegen. */
    val label: String = "",
    val privat: Boolean = false,
    val notes: List<String> = emptyList(),
    val sources: List<SourceRef> = emptyList(),
    /** `1 ASSO` an der Person statt `2 _ASSO` an der Taufe. */
    val level1: Boolean = false,
) {
    val verlinkt: Boolean get() = xref != null && !privat

    /** "Louise Falkenrath (Patin)", frei "Friedrich Plate, Anbauer zu Celle", privat nur [privatText]. */
    fun anzeige(privatText: String): String = when {
        privat -> privatText
        label.isNotBlank() && xref != null -> "$text ($label)"
        else -> text
    }
}

data class PatenGruppe(val rolle: PatenRolle, val eintraege: List<PatenEintrag>)

/** Hat das Ereignis Paten/Zeugen aus der API (Stufe 19)? Aeltere Module: false, dann greift [patenNotiz]. */
val FactJson.hatPaten: Boolean get() = associates.isNotEmpty() || freeAssociates.isNotEmpty()

/**
 * Paten/Zeugen nach Rolle gruppiert (Paten, Trauzeugen, Beteiligte), innerhalb der Gruppe erst die verlinkten,
 * dann die freien - jeweils in der Reihenfolge der Datei.
 */
fun FactJson.patenGruppen(): List<PatenGruppe> {
    if (!hatPaten) return emptyList()
    val verlinkt = associates.map {
        PatenEintrag(it.xref.ifBlank { null } ?: "?", it.name.orEmpty(), it.label, it.isPrivate, it.notes, it.sources, it.level1)
    }
    val frei = freeAssociates.map { PatenEintrag(null, it.text.ifBlank { listOfNotNull(it.name, it.detail).joinToString(", ") }) }
    val nachRolle = (associates.map { PatenRolle.aus(it.role) } zip verlinkt) + (freeAssociates.map { PatenRolle.aus(it.role) } zip frei)
    return PatenRolle.entries.mapNotNull { rolle ->
        val e = nachRolle.filter { it.first == rolle }.map { it.second }
        if (e.isEmpty()) null else PatenGruppe(rolle, e)
    }
}

/**
 * Alle Paten/Zeugen als eine Textzeile fuer Druck, Listen und Buecher: "Louise Falkenrath (Patin); Ernst Falkenrath (Pate);
 * Friedrich Plate, Anbauer zu Celle". Private nur als [privat]. Leer ohne Paten.
 */
fun FactJson.patenText(privat: String, trenner: String = "; "): String =
    patenGruppen().flatMap { it.eintraege }.joinToString(trenner) { it.anzeige(privat) }

/**
 * Je Rolle eine Zeile fuer Druck, Karteikarte und Buch: "Paten: Louise Falkenrath (Patin); Friedrich Plate, Anbauer zu Celle".
 * [titel] uebersetzt die Rolle. Leer ohne Paten aus der API (die alte Notiz bleibt dann unter den Notizen stehen).
 */
fun FactJson.patenZeilen(privat: String, titel: (PatenRolle) -> String, trenner: String = "; "): List<String> =
    patenGruppen().map { g -> titel(g.rolle) + ": " + g.eintraege.joinToString(trenner) { it.anzeige(privat) } }

/** Rueckfall fuer Module vor Stufe 19: der Text hinter "Paten:"/"Trauzeugen:" in einer Notiz des Ereignisses, sonst null. */
fun FactJson.patenNotiz(): String? =
    notes.firstNotNullOfOrNull { PATEN_NOTIZ.find(it.trim())?.groupValues?.get(2)?.trim() }

/** Patenzeile aus der API, sonst aus der Notiz (aeltere Module); leer, wenn beides fehlt. */
fun FactJson.patenTextOderNotiz(privat: String, trenner: String = "; "): String =
    if (hatPaten) patenText(privat, trenner) else patenNotiz().orEmpty()

private val PATEN_NOTIZ = Regex("(?i)^(paten|pate|patin|taufpaten|taufzeugen|gevattern|zeugen|trauzeugen|godparents?|sponsors?|witnesses?)\\s*:\\s*(.+)$", RegexOption.DOT_MATCHES_ALL)

/**
 * Die Notizen ohne die Patenliste: ab Stufe 19 steht sie in freeAssociates (noteKinds "associates"), bei aelteren
 * Modulen bleibt alles - sonst verschwaende die Notiz "Paten: ..." ohne Ersatz.
 */
fun FactJson.notizenOhnePaten(): List<String> =
    if (noteKinds.size != notes.size) notes else notes.filterIndexed { i, _ -> noteKinds[i] != "associates" }

/**
 * `1 ASSO` an der Person liefert die API doppelt: in den Paten der Taufe (level1) und als eigener Fakt ASSO.
 * Den Fakt ausblenden, wenn seine Person schon an einer Taufe (CHR/BAPM) dieser Liste steht.
 */
fun List<FactJson>.ohneDoppelteAsso(): List<FactJson> {
    val anTaufe = filter { it.tag == "CHR" || it.tag == "BAPM" }.flatMap { f -> f.associates.filter { it.level1 }.map { it.xref } }.toSet()
    if (anTaufe.isEmpty()) return this
    return filter { f -> !(f.tag == "ASSO" && f.associates.isNotEmpty() && f.associates.all { it.xref in anTaufe }) }
}

/** Heiratsart aus MARR:TYPE, ohne Ruecksicht auf Gross-/Kleinschreibung (webtrees: CIVIL, RELIGIOUS, PARTNERS, COMMON LAW). */
enum class Heiratsart {
    Standesamtlich, Kirchlich, Partnerschaft, OhneTrauschein;

    companion object {
        fun aus(type: String): Heiratsart? = when (type.trim().uppercase().replace('_', ' ')) {
            "CIVIL" -> Standesamtlich
            "RELIGIOUS", "RELI" -> Kirchlich
            "PARTNERS", "PARTNERSHIP" -> Partnerschaft
            "COMMON LAW", "COMMON" -> OhneTrauschein
            else -> null
        }
    }
}

/** Die Heiratsart dieses Fakts, null ohne TYPE oder bei unbekanntem Wert. */
val FactJson.heiratsart: Heiratsart? get() = if (tag == "MARR") Heiratsart.aus(type) else null

/**
 * Was hinter dem Ereignisnamen steht: die Art des Fakts (`typeLabel` der API, sonst die Uebersetzung aus [fallback],
 * sonst der Rohwert). Null, wenn es keine Art gibt oder das Label sie schon enthaelt ("Standesamtliche Heirat").
 */
fun FactJson.artZusatz(fallback: (Heiratsart) -> String): String? {
    if (type.isBlank()) return null
    val art = typeLabel?.takeIf { it.isNotBlank() } ?: heiratsart?.let(fallback) ?: type
    return if (label.contains(art, ignoreCase = true)) null else art
}

/** Die Heirat einer Familie fuer Listen und Pruefung: die standesamtliche bevorzugt, sonst die erste MARR. */
fun List<FactJson>.hauptHeirat(): FactJson? =
    firstOrNull { it.tag == "MARR" && it.heiratsart == Heiratsart.Standesamtlich } ?: firstOrNull { it.tag == "MARR" }
