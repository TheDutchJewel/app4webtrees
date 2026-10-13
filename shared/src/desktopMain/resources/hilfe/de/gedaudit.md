# gedaudit

**gedaudit** prüft eine GEDCOM-Datei auf Widersprüche und Auffälligkeiten, mit denselben Regeln wie die Plausibilitätsprüfung in wtWin, wtTux und wtMac. Es braucht kein webtrees und keinen Server: Die Datei wird nur gelesen, nichts wird verändert, nichts verlässt den Rechner.

## Datei öffnen

**Datei › GEDCOM-Datei öffnen …** (Strg+O) oder der Knopf auf der Startseite. Zuletzt geöffnete Dateien stehen auf der Startseite und unter **Datei › Zuletzt geöffnet**. Eine `.ged`-Datei lässt sich auch per Doppelklick im Dateimanager mit gedaudit öffnen.

Gelesen werden GEDCOM 5.5, 5.5.1 und 7. Der Zeichensatz kommt aus der Datei (UTF-8, UTF-16, ANSEL, ANSI); welcher erkannt wurde, steht in der Zeile über den Treffern. Wurde die Datei im anderen Programm geändert, holt **Datei neu laden** (F5) den neuen Stand.

## Prüfen

Links stehen die Regeln in Gruppen, rechts die Treffer. Wie die Regeln, Voreinstellungen, Grenzwerte und das Abhaken funktionieren, steht im Kapitel [Plausibilitätsprüfung](hilfe:pruefung). Der Unterschied zu wtWin: Es gibt kein Personenblatt und kein Bearbeiten, denn gedaudit schreibt nicht in die Datei. Korrigiert wird im Genealogieprogramm, aus dem die Datei stammt; danach die Datei neu exportieren und in gedaudit neu laden.

## Person ansehen

Ein Klick auf einen Treffer zeigt unten die **Person**: Ereignisse mit Datum, Ort und Zahl der Quellen, die Eltern, die Partner mit Heirat und die Kinder. Die Kennungen (I123, F45) sind die aus der Datei, so findest du die Person im Herkunftsprogramm wieder. Die Trennlinie über der Karte lässt sich ziehen.

## Abhaken

Treffer, die du geprüft hast und die so bleiben, hakst du ab; sie verschwinden aus der Liste und kommen erst wieder, wenn sich an der Stelle etwas ändert. Die Abhakliste liegt auf diesem PC, getrennt je Datei (nach ihrem Pfad). **Abgehakte sichern …** und **Abgehakte laden …** tauschen sie als Datei aus, auch mit wtWin.

## Drucken

**Datei › Drucken** (Strg+P) und **Als PDF speichern …** geben die aktuelle Trefferliste mit Zusammenfassung aus.

## Sprache und Aussehen

**Ansicht › Sprache** schaltet die Oberfläche um (Deutsch, Englisch, Französisch, Niederländisch, Spanisch), **Ansicht › Erscheinungsbild** zwischen hell und dunkel.
