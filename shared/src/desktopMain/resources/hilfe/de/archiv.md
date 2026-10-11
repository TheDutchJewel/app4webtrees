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

## Gesichter auf Gruppenbildern

Rechtsklick auf ein Bild › **Gesichter …** zeigt das Bild groß mit allen markierten Gesichtern. Beim Darüberfahren steht der Name am Rahmen, rechts die Liste Reihe für Reihe von links; ein Klick auf einen Namen macht die Person zum Probanden. Verwalter setzen den Haken **Gesicht markieren**, ziehen einen Rahmen um ein Gesicht und ordnen ihn zu: einer Person aus dem Stammbaum, nur einem Namen (wer bekannt ist, aber keinen Datensatz hat) oder „unbekannt“. Einen gewählten Rahmen verschiebst du, am gelben Griff änderst du die Größe, **Entf** entfernt ihn. Die Rahmen stehen in der Bilddatei selbst (XMP), andere Programme wie digiKam lesen sie mit. Das Bild hängt dadurch nicht an der Person – außer du setzt im Zuordnen-Dialog den Haken **Bild auch an die gewählte Person hängen**. Im Personenblatt zeigt der Reiter **Medien** unter **Auf Gruppenbildern** alle Bilder, auf denen die Person markiert ist.

## Postkarten

In einer Postkarten-Sammlung sind Vorder- und Rückseite ein Eintrag (Zeichen ↻). Rechts **Wenden**, darunter Motiv, Transkription und Notiz; ein erschlossenes Datum steht als „um 1916“. Doppelklick zeigt beide Seiten im Betrachter. **Postkarte bearbeiten …** ändert Motiv, Transkription, Notiz und Datum; das Modul schreibt in beide Seiten und lässt unveränderte Felder stehen.

## Sammlungen verwalten

Administratoren legen mit **+ Neue Sammlung** eine Sammlung an: Name, Beschreibung, Medienordner (oder keiner – dann ist es ein Album, in das du Bilder einzeln legst), Anzeigetyp, Symbol, Farbe, Reihenfolge, sichtbar. Rechtsklick auf eine Sammlung: **Sammlung bearbeiten …**, **Ausblenden**/**Einblenden**, **Sammlung löschen …** (die Dateien bleiben). Ausgeblendete Sammlungen stehen blass unter „ausgeblendet“. Rechtsklick auf Bilder: **Thematische Sammlungen …** (Haken je Album, wirkt sofort, auch für mehrere gewählte Bilder), **Aus dieser Sammlung entfernen** und **Umbenennen …** (gleicher Ordner, gleiche Endung; ein Medienobjekt im Stammbaum zieht mit). **Einstellungen des Archivs …** setzt Zwischenspeicher-Dauer und Seitengröße und leert den Zwischenspeicher. Was wer darf, legt das Modul fest.

## Abgleich mit webtrees

Bei Bildern, die als Medienobjekt im Stammbaum stehen, zeigt die rechte Spalte, wo Datei und webtrees auseinanderliegen: Beschreibung gegen Titel, Personen in der Datei gegen die verknüpften. **→ in die Datei übernehmen** schreibt den webtrees-Wert in die Datei; am Stammbaum ändert sich nichts.

## Sichern

Im Stammbaum auf diesem PC sichert **Datei › Stammbaum und Archiv sichern …** alles in eine ZIP-Datei: die Datenbank mit Stammbäumen und Sammlungen und den ganzen Medienordner samt freiem Bestand. Eine GEDCOM- oder GEDZIP-Datei nimmt nur die Dateien mit, auf die der Stammbaum zeigt. Zum Zurückholen wtWin beenden und den Ordner `data` aus der ZIP-Datei in den webtrees-Ordner legen (wo der liegt, steht unter [Stammbaum auf diesem PC](hilfe:lokal)).

Auf einem Server sichert der Server selbst, etwa nas4webtrees oder dein Webhoster.
