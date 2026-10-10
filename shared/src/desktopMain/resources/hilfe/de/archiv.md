# Archiv

Läuft auf dem Server das Modul **Sammlungen**, öffnet **Ansicht › Archiv** (Strg+9) oder das Symbol **Archiv** dessen Archiv in einem eigenen Fenster. Im [Stammbaum auf diesem PC](hilfe:lokal) ist das Modul schon dabei. Das Archiv hält mehr als der Stammbaum: Fotos und Dokumente müssen an niemandem hängen. Wer was sehen und ändern darf, regelt webtrees mit seinen Benutzerrechten.

## Aufbau

- **Links** die Sammlungen: Archivordner, thematische Sammlungen, Sammlungen nach Medientyp und die Medien, die an keiner Person hängen. Darunter steht, wie viele Dateien zum freien Bestand gehören, auf die im Stammbaum also nichts zeigt. Das ist so gewollt und kein Mangel.
- **In der Mitte** die Dateien der gewählten Sammlung als Raster, oben in drei Größen umschaltbar. Das Kennzeichen am Bild nennt die Zahl der verknüpften Personen, **GEDCOM** heißt Medienobjekt ohne Person, **frei** heißt nur Datei.
- **Rechts** die Angaben zur gewählten Datei: Beschreibung, Datum, Personen und Schlagwörter aus der Datei, Sammlungen, Größe, Pfad und wer im Stammbaum damit verknüpft ist. Ein Klick auf einen Namen macht die Person zum Probanden.

Die Breite der Spalten stellst du an den Trennstrichen ein; wtWin merkt sie sich.

## Bedienen

Klick wählt eine Datei, **Strg+Klick** nimmt weitere dazu, **Umschalt+Klick** einen ganzen Bereich, **Strg+A** alle. Mit den Pfeiltasten, **Pos1** und **Ende** wanderst du durchs Raster, **Eingabe** oder Doppelklick öffnet: Bilder im Betrachter, PDFs im PDF-Betrachter, alles andere im Browser.

Der **Rechtsklick** bietet: Öffnen, Beschriftung ändern, Pfad kopieren (auch Strg+C), Kopien in einen Ordner speichern und In webtrees öffnen. Kopien speichern holt die Originale auf diesen PC; vorhandene Dateien werden nicht überschrieben.

## Dateien hinzufügen

Wer in webtrees Medien hochladen darf, legt Dateien mit **Dateien hinzufügen …** ins Archiv oder zieht sie aus dem Dateimanager ins Fenster. Ordner und thematische Sammlung sind mit der offenen Sammlung vorbelegt. Beschreibung, Datum, Personen und Schlagwörter sind freiwillig und kommen nur in Bilder (XMP).

wtWin lädt das Original hoch und verkleinert nichts. Ist eine Datei größer, als der Server annimmt, wird sie übersprungen und genannt. Die Grenze setzt der Server (PHP `upload_max_filesize`). Die Dateien hängen danach an niemandem. An eine Person, eine Quelle oder einen Ort hängst du sie im Reiter **Medien** mit **Vorhandenes Medium › Aus dem Archiv**.

## Beschriftung ändern

Verwalter des Stammbaums ändern Beschreibung, Datum, Personen und Schlagwörter eines Bildes über den Rechtsklick, den Knopf rechts oder den Stift im Betrachter. Das Modul schreibt die Angaben in die Datei selbst.

## Sichern

Im Stammbaum auf diesem PC sichert **Datei › Stammbaum und Archiv sichern …** alles in eine ZIP-Datei: die Datenbank mit Stammbäumen und Sammlungen und den ganzen Medienordner samt freiem Bestand. Eine GEDCOM- oder GEDZIP-Datei nimmt nur die Dateien mit, auf die der Stammbaum zeigt. Zum Zurückholen wtWin beenden und den Ordner `data` aus der ZIP-Datei in den webtrees-Ordner legen (wo der liegt, steht unter [Stammbaum auf diesem PC](hilfe:lokal)).

Auf einem Server sichert der Server selbst, etwa nas4webtrees oder dein Webhoster.
