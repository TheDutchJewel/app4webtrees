# Personenblatt und Bearbeiten

Das Personenblatt öffnet sich mit **Doppelklick** auf eine Person, mit **Strg+E** oder über das Symbol **Bearbeiten**. Es zeigt alles zu einer Person und ist zugleich der Ort zum Bearbeiten.

## Aufbau

Oben Name, Lebensdaten und Bild. Darunter die Reiter:

- **Daten:** alle Ereignisse als Tabelle (Ereignis, Datum, Ort / Beschreibung), dazu Alter beim Tod.
- **Lebenslauf:** die Zeitleiste mit Heirat, Geburten der Kinder und dem jeweiligen Alter. Dazu die Ereignisse, bei denen die Person selbst Pate oder Trauzeuge war („Patin bei: Taufe von …“); ein Klick führt zum Täufling bzw. zum Paar.
- **Paten und Trauzeugen:** unter Taufe und Heirat steht die Zeile „Paten:“ bzw. „Trauzeugen:“ – im Lebenslauf, im Detailbereich der Datentabelle, in der Familienansicht und auf Karteikarten. Paten mit eigenem Datensatz sind unterstrichen und anklickbar, Paten ohne Datensatz (aus einer Notiz „Paten: …“ oder dem GEDCOM-L-Feld _GODP) stehen als Text. ⓘ klappt eine Notiz zum Paten auf, das Quellensymbol öffnet die Quelle. Lebende Paten, die du nicht sehen darfst, erscheinen nur als „Privat“. Unter der Datentabelle listet der Abschnitt **Patenschaften und Trauzeugenschaften** alle Taufen und Heiraten auf, bei denen die Person Pate oder Zeuge war; ein Klick auf die Überschrift klappt ihn zu. Beides braucht api4webtrees ab 1.11; mit einem älteren Modul bleibt die Patennotiz einfach eine Notiz.
- **Heiratsart:** standesamtliche und kirchliche Trauung erscheinen als getrennte Ereignisse mit ihrer Art; Listen und Prüfung nehmen die standesamtliche, wenn es beide gibt.
- **Eltern/Geschwister**: Eltern und Geschwister, auch Halbgeschwister. Ein Klick wechselt zur Person.
- **Partner/Kinder**: links die Partnerschaften, rechts die Kinder der gewählten, darunter ihre Ereignisse (Heirat, Scheidung, Wohnort …) zum Anlegen, Bearbeiten und Löschen. Doppelklick auf Partner oder Kind zeigt dessen Blatt; **+** legt einen Partner bzw. ein Kind dieser Partnerschaft an.
- **Name**: Vornamen, Familienname und Namenszusatz stehen beim Bearbeiten in eigenen Feldern.
- **Einfach / Vollständig** (unten im Blatt): „Einfach“ zeigt im Reiter Daten ein Formular mit Name, Geburt, Taufe, Religion, Beruf, Heirat je Partnerschaft, Tod und Begräbnis zum direkten Eintippen; „Vollständig“ die Tabelle aller Ereignisse mit Alter und Merkern für Notiz und Quelle (Klick auf den Spaltenkopf sortiert).
  Gespeichert wird nur, was du geändert hast; Quellen, Notizen und weitere Angaben am Ereignis bleiben erhalten. Gibt es ein Ereignis mehrfach, bearbeitet das Formular das erste. Ein Datum, das das Programm nicht deuten kann („Frühjahr 1850“), wird als Datumstext gespeichert. Gespeichert wird beim Verlassen eines Feldes (Tab oder Klick woandershin), wie überall im Programm. Ein ungültiges Datum bleibt rot stehen und geht nicht an den Server, bis du es korrigierst oder „Trotzdem als Text speichern“ wählst; nur dann fragt das Programm beim Schließen und Beenden nach.
- **Notizen**, **Quellen**, **Medien**.
- **Karte:** die Lebensstationen als Liste mit Links zu OpenStreetMap.

## Blättern

**Bild auf** und **Bild ab** wechseln zur vorigen bzw. nächsten Person der Liste, **Strg+Pos1** und **Strg+Ende** zur ersten und letzten. **Esc** schließt das Fenster.

## Bearbeiten

Bearbeiten geht nur mit Bearbeitungsrecht in webtrees. Änderungen landen sofort in webtrees; je nach Einstellung des Stammbaums gelten sie gleich oder warten auf die Freigabe durch einen Moderator.

- **Ereignis ändern:** Doppelklick auf die Zeile (oder Bearbeiten). Datum und Ort werden ausgewählt: genau, um, vor, nach, zwischen; Tag, Monat, Jahr. Orte schlägt das Programm beim Tippen aus dem Stammbaum vor.
- **Ereignis hinzufügen**, auch Familienereignisse wie Heirat.
- **Verwandte hinzufügen** (Strg+N): Eltern, Partner, Kind oder Geschwister anlegen, mit Namen und den ersten Ereignissen. Im Navigator geht das auch über das Menü der rechten Maustaste.
- **Löschen:** ein Ereignis oder die ganze Person, nach Rückfrage.
- **In webtrees bearbeiten:** alles, was das Programm nicht selbst kann (Namen, neue Quellen, Medien verknüpfen), erledigst du auf der Personenseite im Browser. Siehe [webtrees im Browser](hilfe:webtrees).

## Fotos

**Foto hinzufügen** wählt eine Datei vom PC und hängt sie an die Person; das Bild wird passend zum Upload-Limit des Servers verkleinert. Ein Klick auf ein Bild öffnet den Betrachter.

## Drucken und Weitergeben

- **Datei › Personenblatt drucken …** (Strg+P) und **Personenblatt als PDF …**
- **Person › Personentext kopieren** (Strg+Umschalt+C) legt alle Angaben als Text in die Zwischenablage, etwa für eine E-Mail oder die Textverarbeitung.
- Mehr Ausgaben: [Listen](hilfe:listen) (Personenblatt als Liste), [Tafeln](hilfe:tafeln), [Bücher](hilfe:buecher).
