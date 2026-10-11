package de.bgghome.webtrees.nativ.data

import de.bgghome.webtrees.nativ.api.ArchiveEntry
import de.bgghome.webtrees.nativ.api.ExifRequest

/*
 * Abgleich Datei ↔ webtrees wie in der Lightbox des Moduls Sammlungen: Beschreibung in der Datei gegen den Titel des
 * Medienobjekts, Personen in der Datei gegen die in webtrees verknuepften. Uebernommen wird nur in eine Richtung, aus
 * webtrees in die Datei; am Stammbaum aendert sich nichts.
 */
object ArchivAbgleich {
    enum class Feld { Beschreibung, Personen }

    /** Ein Unterschied; [uebernahme] null heisst: nichts anzubieten (webtrees leer oder nur ein Ausschnitt der Personen). */
    data class Unterschied(val feld: Feld, val datei: String, val webtrees: String, val gekuerzt: Boolean, val uebernahme: String?)

    fun unterschiede(e: ArchiveEntry): List<Unterschied> {
        // Nur Medienobjekte haben eine webtrees-Seite; eine freie Datei hat nichts zum Abgleichen
        if (e.xref == null) return emptyList()
        val liste = mutableListOf<Unterschied>()
        if (e.beschreibung != e.titel && (e.beschreibung.isNotBlank() || e.titel.isNotBlank()))
            liste += Unterschied(Feld.Beschreibung, e.beschreibung, e.titel, false, e.titel.takeIf { it.isNotBlank() })
        val wt = e.personen.map { it.name }.sorted().joinToString(", ")
        val datei = e.exifPersonen.sorted().joinToString(", ")
        // Bei sehr vielen Verknuepfungen liefert das Modul nur einen Ausschnitt - dann keine Uebernahme anbieten
        val gekuerzt = e.personenGesamt > e.personen.size
        if (wt != datei && (wt.isNotBlank() || datei.isNotBlank()))
            liste += Unterschied(Feld.Personen, datei, wt, gekuerzt, wt.takeIf { it.isNotBlank() && !gekuerzt })
        return liste
    }

    /** Die Anfrage an die Route exif: das uebernommene Feld neu, alles andere wie in der Datei. */
    fun anfrage(e: ArchiveEntry, u: Unterschied): ExifRequest = ExifRequest(
        beschreibung = if (u.feld == Feld.Beschreibung) u.uebernahme.orEmpty() else e.beschreibung,
        datum = e.datumIso.ifEmpty { e.datum },
        personen = if (u.feld == Feld.Personen) e.personen.map { it.name }.sorted() else e.exifPersonen,
        keywords = e.keywords,
    )
}
