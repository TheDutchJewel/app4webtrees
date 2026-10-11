package de.bgghome.webtrees.nativ.api

import kotlinx.serialization.Serializable

// JSON-Formen des webtrees-Moduls "Sammlungen" (github.com/thobgg/webtrees-sammlungen, Abschnitt "Schnittstelle fuer
// Apps"): das Archiv eines Baums - Fotos und Dokumente, die nicht unbedingt an einer Person haengen. Die Feldnamen sind
// die des Moduls, deshalb deutsch.

/** Antwort von /archiv/api/sammlungen: die Uebersicht des Archivs. */
@Serializable
data class ArchiveOverview(
    val api: Int = 0,
    val modul: String = "",
    val baum: String = "",
    val proSeite: Int = 50,
    /** Die Galerie des Moduls im Browser */
    val galerie: String = "",
    /** Ab Stufe 2: ob dieser Nutzer ins Archiv hochladen bzw. EXIF schreiben darf, und die Zielordner */
    val darfHochladen: Boolean = false,
    val darfExif: Boolean = false,
    /** Ab Stufe 5: ob dieser Nutzer Gesichter auf Bildern markieren darf */
    val darfBereiche: Boolean = false,
    /** Ab Stufe 5: Sammlung "Bilder einer Person" ueber den Filter person (Gesichter mit dieser Kennung) */
    val personenFilter: Boolean = false,
    /** Ab Stufe 6: umbenennen, zuordnen, sichtbar schalten (Verwalter); anlegen/aendern/loeschen und Einstellungen (Administrator) */
    val darfOrdnen: Boolean = false,
    val darfVerwalten: Boolean = false,
    val darfEinstellungen: Boolean = false,
    /** Nur mit ?alle=1 fuer Verwalter: alle Sammlungen samt inaktiven und die Auswahllisten der Maske */
    val verwaltung: Verwaltung? = null,
    val ordnerListe: List<String> = emptyList(),
    val sammlungen: List<ArchiveCollection> = emptyList(),
    /** Medienobjekte, an denen keine Person und keine Familie haengt - nach Medientyp */
    val unverknuepft: List<MediaTypeCount> = emptyList(),
    val frei: FreeFiles = FreeFiles(),
)

/** Eine Sammlung auf der Uebersicht. art: "ordner", "thematisch" oder "medientyp". */
@Serializable
data class ArchiveCollection(
    val slug: String,
    val art: String = "",
    val name: String = "",
    val beschreibung: String = "",
    /** "#rrggbb" bei Sammlungen aus der Verwaltung, sonst null */
    val farbe: String? = null,
    val icon: String = "",
    /** "foto", "raster", "gemischt" oder "dokument" */
    val ansicht: String = "foto",
    val ordner: String? = null,
    val anzahl: Int = 0,
    /** Bis zu drei Vorschaubilder */
    val vorschau: List<String> = emptyList(),
    /** Ab Stufe 6: Sammlung ohne Ordner, nimmt einzelne Bilder auf */
    val album: Boolean = false,
    /** Ab Stufe 6: sichtbare Ordner-Sammlung direkt unter dem Medienordner (Quelle der Bildauswahl im Browser) */
    val bildquelle: Boolean = false,
)

@Serializable
data class MediaTypeCount(val typ: String, val name: String = "", val anzahl: Int = 0)

/** Dateien im Medienordner, die im Stammbaum nirgends auftauchen - nur gezaehlt, je Ordner. */
@Serializable
data class FreeFiles(val gesamt: Int = 0, val dateien: Int = 0, val jeOrdner: List<FolderCount> = emptyList())

@Serializable
data class FolderCount(val ordner: String = "", val anzahl: Int = 0)

/** Antwort von /archiv/api/sammlung: eine Seite einer Sammlung. */
@Serializable
data class CollectionPage(
    val slug: String = "",
    val art: String = "",
    /** Nur bei nicht eingebundenen Medien: der Medientyp dieser Seite */
    val typ: String? = null,
    val name: String = "",
    val beschreibung: String = "",
    val farbe: String? = null,
    val icon: String = "",
    val ansicht: String = "foto",
    /** Bei Ordner-Sammlungen der Ordner unter dem Medienordner - Vorgabe fuer "Festhalten" */
    val ordner: String? = null,
    val anzahl: Int = 0,
    val dateien: Int = 0,
    val seite: Int = 1,
    val seiten: Int = 1,
    val proSeite: Int = 0,
    val typen: List<MediaTypeCount> = emptyList(),
    val eintraege: List<ArchiveEntry> = emptyList(),
    /** Nur beim Filter person (Stufe 5): false, solange das Modul seinen Index noch aufbaut - spaeter erneut fragen */
    val vollstaendig: Boolean = true,
    /** Foto-Ordner: Video, Audio und Dokumente als Liste unter den Bildern (nicht seitenweise) */
    val weitere: List<ArchiveEntry> = emptyList(),
)

/** Eine Datei im Archiv - mit oder ohne Medienobjekt (xref). */
@Serializable
data class ArchiveEntry(
    val pfad: String? = null,
    val datei: String = "",
    val format: String = "",
    val istBild: Boolean = false,
    val xref: String? = null,
    val titel: String = "",
    val notiz: String = "",
    /** EXIF-Beschreibung aus der Datei */
    val beschreibung: String = "",
    /** Beschreibung, sonst Titel, sonst Dateiname - was unter das Bild gehoert */
    val bildunterschrift: String = "",
    val datum: String = "",
    val datumIso: String = "",
    val exifPersonen: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val breite: Int = 0,
    val hoehe: Int = 0,
    val groesseKb: Int = 0,
    /** In webtrees verknuepfte Personen (hoechstens 25) */
    val personen: List<PersonRef> = emptyList(),
    val personenGesamt: Int = 0,
    val inSammlungen: List<String> = emptyList(),
    /** 400 Pixel breit - nur bei Bildern */
    val kachel: String? = null,
    /** 1600 Pixel breit - nur bei Bildern */
    val vollbild: String? = null,
    val original: String = "",
    /** Die Medienseite in webtrees - nur bei Medienobjekten */
    val seite: String? = null,
    /** Ab Stufe 5: markierte Gesichter (XMP MWG Regions aus der Datei) */
    val bereiche: List<Bereich> = emptyList(),
    /** Ab Stufe 4: das Datum ist erschlossen (Poststempel unleserlich) - "um 12.05.1912" */
    val datumUnsicher: Boolean = false,
    /** Ab Stufe 4, nur in Postkarten-Sammlungen: die Rueckseite als eigener Eintrag (null = Einzelkarte) */
    val rueckseite: ArchiveEntry? = null,
    val motiv: String = "",
    /** api/eintrag und api/exif bei Postkarten (Stufe 6): die andere Seite der Karte */
    val gegenseite: ArchiveEntry? = null,
    /** In Postkarten-Sammlungen steht in [notiz] der Abschnitt "Notiz:" der Karte statt der Notiz des Medienobjekts. */
    val transkription: String = "",
) {
    /** Eine Postkarte (Stufe 4): hat eine Rueckseite oder die Abschnitte des Postkarten-Editors. */
    val istPostkarte: Boolean get() = rueckseite != null || motiv.isNotBlank() || transkription.isNotBlank()
}

/**
 * Ein markiertes Gesicht auf einem Bild (Stufe 5): Rahmen links oben, normiert 0-1 aufs Bild. [xref] nur, wenn die Person
 * im Stammbaum steht; [name] leer heisst unbekannt. Im Modul liegt es als MWG-Region in der Datei.
 */
@Serializable
data class Bereich(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val w: Double = 0.0,
    val h: Double = 0.0,
    val name: String = "",
    val xref: String? = null,
    val typ: String = "Face",
)

/** Was die App zu einem Foto mitgibt, das sie ins Archiv legt (Stufe 2 der Schnittstelle). */
data class ArchiveUploadRequest(
    /** Unterordner des Medienordners, "" = Hauptordner */
    val ordner: String = "",
    val beschreibung: String = "",
    /** YYYY, YYYY-MM oder YYYY-MM-DD */
    val datum: String = "",
    val personen: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    /** Slug einer thematischen Sammlung, "" = keine */
    val sammlung: String = "",
)

/** Antwort von /archiv/api/hochladen. exif: true geschrieben, false fehlgeschlagen, null nichts zu schreiben. */
@Serializable
data class ArchiveUploadResult(
    val ok: Boolean = false,
    val pfad: String = "",
    val datei: String = "",
    val exif: Boolean? = null,
    /** "exif-failed", "not-image" oder "unknown-collection" - die Datei liegt trotzdem im Archiv */
    val hinweis: String? = null,
    val eintrag: ArchiveEntry? = null,
)

/** Die Beschriftung eines Bildes im Archiv - was die App in die Datei schreiben laesst (EXIF/XMP). */
data class ExifRequest(
    val beschreibung: String = "",
    /** YYYY, YYYY-MM oder YYYY-MM-DD */
    val datum: String = "",
    val personen: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
)

/** Antwort von /archiv/api/exif: der Eintrag mit der neuen Beschriftung. */
@Serializable
data class ExifWriteResult(val ok: Boolean = false, val eintrag: ArchiveEntry? = null)

/** Antwort von /archiv/api/eintrag: ein Eintrag zu einer Datei (Stufe 3). */
@Serializable
data class EntryResult(val ok: Boolean = false, val eintrag: ArchiveEntry? = null)

// ── Stufe 6: Verwaltung aus der App ──

/** Die Verwaltung der Sammlungen (api/sammlungen?alle=1): alle Sammlungen mit den Feldern der Maske und die Auswahllisten. */
@Serializable
data class Verwaltung(val sammlungen: List<SammlungDaten> = emptyList(), val auswahl: VerwaltungsAuswahl = VerwaltungsAuswahl())

/** Eine Sammlung, wie die Maske sie kennt. [symbol] ist der Formularwert, [icon] die Font-Awesome-Klasse. */
@Serializable
data class SammlungDaten(
    val slug: String = "",
    val art: String = "",
    val name: String = "",
    val beschreibung: String = "",
    val symbol: String = "",
    val icon: String = "",
    val farbe: String? = null,
    val ansicht: String = "foto",
    val ordner: String = "",
    val aktiv: Boolean = true,
    val reihenfolge: Int = 0,
    val album: Boolean = false,
    val bildquelle: Boolean = false,
    val anzahl: Int = 0,
)

@Serializable
data class VerwaltungsAuswahl(
    val symbole: List<AuswahlWert> = emptyList(),
    val ansichten: List<AuswahlWert> = emptyList(),
    val ordner: List<String> = emptyList(),
)

@Serializable
data class AuswahlWert(val wert: String = "", val name: String = "", val icon: String = "")

/** Antworten der schreibenden Routen von Stufe 6. */
@Serializable
data class UmbenennenErgebnis(val ok: Boolean = false, val alterPfad: String = "", val eintrag: ArchiveEntry? = null)

@Serializable
data class ZuordnenErgebnis(val ok: Boolean = false, val sammlung: String = "", val istDrin: Boolean = false, val eintrag: ArchiveEntry? = null)

@Serializable
data class SammlungErgebnis(val ok: Boolean = false, val neu: Boolean = false, val sammlung: SammlungDaten? = null)

@Serializable
data class Grenze(val min: Int = 0, val max: Int = 0, val vorgabe: Int = 0)

@Serializable
data class ArchivGrenzen(val cacheTtl: Grenze = Grenze(), val seitenGroesse: Grenze = Grenze())

@Serializable
data class ArchivEinstellungen(val ok: Boolean = false, val cacheTtl: Int = 0, val seitenGroesse: Int = 0, val grenzen: ArchivGrenzen = ArchivGrenzen(), val apcu: Boolean = false)

@Serializable
data class SchlichtesErgebnis(val ok: Boolean = false)

/** Postkarten-Abschnitte fuer api/exif (Stufe 6); null heisst: nicht senden, bleibt in der Datei. */
data class PostkartenAnfrage(
    val motiv: String? = null,
    val transkription: String? = null,
    val notiz: String? = null,
    val datum: String? = null,
    val datumUnsicher: Boolean? = null,
    val personen: List<String>? = null,
    val keywords: List<String>? = null,
)
