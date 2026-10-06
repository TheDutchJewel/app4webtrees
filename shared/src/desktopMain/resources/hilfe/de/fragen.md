# Fragen, Fehler, Datenschutz

## Fragen und Fehler melden

wtWin, wtTux, wtMac und wtAnd sind keine offiziellen webtrees-Produkte. Fragen und Fehler bitte nicht im webtrees-Forum, sondern als Issue unter **github.com/thobgg/app4webtrees/issues**. Hilfreich sind: die Version aus **Hilfe › Über wtWin** (mit Build-Nummer), Windows oder Linux, und was du getan hast, als es passierte. Beim Stammbaum auf dem PC zusätzlich die Datei `php.log` (siehe [Stammbaum auf diesem PC](hilfe:lokal)).

Neue Fassungen erscheinen unter **github.com/thobgg/app4webtrees/releases**. Die Windows-Datei ist nicht signiert; Windows warnt darum beim ersten Start vor einem unbekannten Herausgeber („Weitere Informationen“, dann „Trotzdem ausführen“).

## Was das Programm speichert

- Serveradresse, Benutzername und das Sitzungs-Cookie, **nie das webtrees-Passwort**. Nur die Zugangsdaten eines Verzeichnisschutzes (falls eingetragen) bleiben auf dem PC, sie müssen bei jeder Anfrage mit.
- Deine Einstellungen (Aufbau, Erscheinungsbild, Tafel- und Listeneinstellungen, Farbregeln, Abhakliste der Prüfung) auf diesem PC.
- Beim Stammbaum auf dem PC: den ganzen Stammbaum im Ordner `app4webtrees` samt Zugang zum lokalen webtrees.
- Keine Analyse, keine Werbung, keine Weitergabe.

## Datenschutz im Stammbaum

Das Programm meldet sich mit deinem webtrees-Konto an; jede Anfrage läuft als dieser Benutzer. Es gelten dieselben Regeln wie auf der Website: lebende Personen, gesperrte Einträge und private Stammbäume siehst du nur, wenn webtrees es dir erlaubt. Ausgaben (Tafeln, Listen, Bücher, PDF) enthalten nur, was du sehen darfst.

## Unverschlüsselt im Heimnetz

Eine `http://`-Adresse nimmt das Programm nur im Heimnetz an (private Adressen wie 192.168…, Namen wie `diskstation` oder `.local`). Dann läuft die Verbindung unverschlüsselt, was zu Hause in Ordnung ist. Soll der Stammbaum von unterwegs erreichbar sein, braucht der Server HTTPS; die Anleitung dazu liefert nas4webtrees.

## Handy und Tablet

Für Android gibt es **wtAnd**, dieselbe Familie mit denselben Daten: Stammbaum, Fotos, Bearbeiten, Jahrestage. Ein QR-Code auf der webtrees-Seite **App** verbindet das Handy ohne Tippen. wtAnd braucht einen Server; der Stammbaum auf dem PC ist nur am PC erreichbar.

## Lizenz

wtWin steht unter der GPL-3, wie webtrees. Der Stammbaum auf dem PC enthält webtrees unverändert aus dem offiziellen Release (webtrees.net).

## Ausführliche Anleitung

Mit Bildern und aktuellen Hinweisen: **bgg-home.de/me/genealogie/wtwin/** (Knopf unten in diesem Fenster).
