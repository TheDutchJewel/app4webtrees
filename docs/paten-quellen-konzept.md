# Paten, Trauzeugen, Heiratsart und Quellen: Konzept für die Apps

Stand 01.10.2026, nur Konzept – kein Code. Setzt **api4webtrees 1.11 / API-Stufe 19** voraus
(Spec: `webtrees/api4webtrees/docs/spec-paten-quellen.md`). Testdaten: `demo-tree/falkenrath.ged` **Version 1.2**,
Fundstellen in `demo-tree/README.md`. Gilt für wtAnd, wtWin und wtTux (gemeinsamer Code in `shared/`).

## Arbeitsteilung

Die **API** liest das GEDCOM, normalisiert und schützt; die **Apps** zeigen und bearbeiten nur JSON. Die Apps
parsen **kein** GEDCOM und keine Notiztexte selbst – auch nicht `Paten: …`.

| Aufgabe | API | App |
| - | - | - |
| `_ASSO`, `1 ASSO`, `RELA` in jeder Schreibweise erkennen | ✔ (`role` normalisiert) | nutzt `role`, nie `rela` |
| `Paten:`/`Trauzeugen:`-Notiz zerlegen | ✔ (`freeAssociates`) | zeigt an |
| „Pate bei …“ ermitteln | ✔ (`associatedIn`) | zeigt an, verlinkt |
| Pate/Patin, Heiratsart übersetzen | ✔ (`label`, `typeLabel`) | Fallback bei alter API |
| Datenschutz | ✔ (`private`) | zeigt „privat“, nie Namen |
| webtrees-Form schreiben (`godparent` klein) | ✔ | schickt nur `role` |

**Ältere API (Stufe < 19):** Felder fehlen → Apps verhalten sich wie heute (Paten nur aus der Notiz). Kein Fehler.

## Abweichungen in der Umsetzung (01.10.2026, API 1.11.0 fc532bb/bc92a89, App-Schritt 2)

1. `1 ASSO` an der Person erscheint in `associates` der Taufe mit `level1: true` **und** bleibt als eigener Fakt `ASSO`
   in `facts[]` (Abwärtskompatibilität). Die App blendet den ASSO-Fakt aus, wenn seine Person schon an einer Taufe
   steht (`ohneDoppelteAsso()` in `data/Paten.kt`) – in Lebenslauf, Datentabelle, Karteikarte, Druck.
2. `type` kommt in webtrees-Kanonform groß (`CIVIL`, `RELIGIOUS`, leer ohne TYPE). Das `label` des Fakts enthält die
   Art bereits („Standesamtliche Heirat“), `typeLabel` ist dieselbe Übersetzung. Die App hängt die Art nur an, wenn
   das Label sie nicht schon enthält (`artZusatz()`), Fallback-Übersetzung ohne Rücksicht auf Groß-/Kleinschreibung
   (`Heiratsart.aus()`); Kurzform „(standesamtlich)“ nur bei mehreren Heiraten einer Familie (Buch, Karteikarte).
3. Datenschutz: Gäste bekommen lebende Paten **gar nicht** (Eintrag fehlt), Mitglieder sehen Vertrauliche als
   `private: true` ohne Namen → „Privat“. Die App rät nie Namen.
4. `noteKinds[]` parallel zu `notes[]`; die App zeigt Notizen mit Kind `associates` nicht mehr unter Notizen
   (`notizenOhnePaten()`); fehlt `noteKinds` (alte API), bleiben alle Notizen stehen.
5. **Verbreitete Programme** schreiben freie Paten nicht als NOTE, sondern als `2 _GODP <Text>` unter CHR/BAPM und Trauzeugen als
   `2 _WITN` unter MARR (GEDCOM-L, webtrees kennt beide). Die API liefert sie seit bc92a89 ebenfalls in
   `freeAssociates` (zuerst `_GODP`/`_WITN`, dann NOTE-Einträge). Programme, die nur `_GODP` lesen,
   zeigen bei Falkenrath 1.2 (Notiz-Form) keine freien Paten.
6. `associatedIn[]` hat für Familien zusätzlich `husband`/`wife` (XREF, nur wenn sichtbar, sonst null), damit
   „Trauzeuge bei Heirat A & B“ zu einer Person führt (die App öffnet den Mann, sonst die Frau).
7. Android: `associatedIn` erscheint als Zeilen im Lebenslauf („Patin bei: Taufe von …“, anklickbar) – keine eigene
   Karte; am Desktop zusätzlich der Abschnitt unter der Datentabelle (standardmäßig ausgeklappt, Zustand gemerkt).
8. Nicht in Schritt 2: die sechs neuen Prüfregeln aus Abschnitt 4 (nur Regeln 024/126 umgestellt) und der Hinweis
   „an der Person erfasst“ im Lebenslauf (nur im Detailbereich des Personenblatts, Redakteure).

## 1. Modelle (`shared/…/api/Models.kt`)

| Neu/geändert | Felder |
| - | - |
| `Associate` | `xref`, `name?`, `sex?`, `rela`, `role` (`godparent`/`witness`/`other`), `label`, `private`, `level1`, `notes`, `sources: List<SourceRef>` |
| `FreeAssociate` | `role`, `name?`, `detail?`, `text` |
| `AssociatedIn` | `record`, `recordType`, `name`, `tag`, `label`, `factId`, `date`, `place`, `role`, `label2` |
| `FactJson` | + `associates`, `freeAssociates`, `typeLabel`, `noteKinds` |
| `Person` (Individual) | + `associatedIn` |
| `SourceRef` (Zitat) | + `event`, `eventLabel`, `role` |
| `SourceDetail` | + `data` (`events[]`, `agency`, `notes`), `texts` |
| `RepositoryRef` | + `medium` |
| `RepositorySummary` | + `address`, `phone`, `email`, `www`, `notes` |
| Requests | `FactRequest.type`; neu `AssociationRequest`; `CitationRequest.event/role`; `SourceRequest.medium`; `RepositoryRequest` + Kontakt |

Alle neuen Felder mit Standardwerten (leer/null), damit alte Antworten weiter lesbar sind.

## 2. Anzeige

### 2.1 Am Ereignis (PersonSheet-Detail, Timeline, Karteikarte, Familienansicht)

```
Taufe  1. Mai 1897, Celle
  Paten:  Louise Falkenrath (Patin) · Ernst Falkenrath (Pate) ⓘ ⧉ · Friedrich Plate, Anbauer zu Celle
  Quelle: KB Celle ev. Taufen 1863-1905, Taufe 1897/77
```

- Verlinkte Paten **anklickbar** (öffnet die Person); freie als Text, gleiche Zeile, in Dateireihenfolge: erst
  verlinkt, dann frei.
- Überschrift aus `role`: „Paten“ / „Trauzeugen“ / „Beteiligte“.
- `label` hinter dem Namen (Pate/Patin, Zeuge/Zeugin); bei `private` nur „privat“, nicht anklickbar.
- `notes` am Paten als ⓘ (Tooltip/Antippen), `sources` als Quellen-Symbol ⧉ → öffnet das Zitat.
- Die Notiz mit `noteKinds = associates` **nicht** zusätzlich unter Notizen zeigen.
- `level1: true`: kleiner Hinweis „an der Person erfasst“ (nur Desktop, nur für Redakteure).

### 2.2 An der Person: „Patenschaften und Trauzeugenschaften“

Neuer Abschnitt (Desktop: im PersonSheet unter den Ereignissen; Android: eigene Karte), aus `associatedIn`:

```
Pate bei
  1897  Taufe von Heinrich Falkenrath, Celle
  1902  Taufe von …
Trauzeuge bei
  1893  Heirat Carl Falkenrath & Dorothea Wichmann, Celle
```

Einträge verlinkt. Fehlt `associatedIn` (alte API) → Abschnitt weglassen.

### 2.3 Heiratsart

- Bezeichnung aus `typeLabel`; Fallback-Übersetzung in der App für `civil` → „standesamtlich“, `religious` →
  „kirchlich“ (heute zeigen Timeline, Karteikarte und PersonSheet den Rohwert).
- Kurzform in Listen: „Heirat (standesamtlich)“ / „Heirat (kirchlich)“.
- **Alle** `MARR` einer Familie anzeigen, nach Datum; nicht nur die erste.

### 2.4 Quellen

- Quelle (QuellenFenster/QuelleDetail): Abschnitt „Erfasst“ aus `data` – Ereignisse übersetzt (Taufen, Begräbnisse
  …), Zeitraum, Ort, Stelle (`agency`); alle `texts`.
- Archiv: Adresse, Telefon (`tel:`), E-Mail (`mailto:`), Web (Link), Notizen; je Archivverweis Signatur **und**
  Medium (Buch, Mikrofilm, digital …).
- Zitat: „aus Taufeintrag“ (`eventLabel`), Rolle „Pate“ (`role`, Klammern entfernen), Qualität wie bisher.

## 3. Bearbeiten (Desktop zuerst, Android lesend)

### 3.1 Paten-/Zeugen-Dialog am Ereignis

- Liste der Paten mit ↑↓, ✕; „Person aus dem Baum…“ (Personensuche), „Ohne Datensatz…“ (Freitext
  `Name, Beruf zu Ort`).
- Rolle wählbar: Pate/Patin → `godparent`, Trauzeuge/-zeugin → `witness`, sonst frei (`other`). Die App schickt
  **nur** `role`; die API schreibt `godparent`/`witness` klein.
- Notiz am Paten editierbar; Quellen am Paten nur anzeigen (Bearbeiten später).
- Vorschlagsliste: Großeltern, Geschwister der Eltern und deren Partner, ältere Geschwister – lebend und mindestens
  14 zum Taufdatum (bzw. 21/18 für Trauzeugen).
- Bei `level1`-Paten: Angebot „In die Taufe übernehmen“ (`convertLevel1`), nur nach Bestätigung.

### 3.2 Heiratsart und zweite Heirat

- Feld „Art“ (standesamtlich / kirchlich / ohne Angabe / eingetragene Partnerschaft) im Ereignisdialog →
  `FactRequest.type`.
- `EinfachFormular`: statt einem MARR-Feld (heute `liste.firstOrNull()`) eine Zeile je Heirat plus „Kirchliche
  Trauung hinzufügen“.

### 3.3 Quellen

- QuelleDialog: Medium beim Archivverweis (erhält `MEDI`), Kontakt beim Archiv.
- ZitatDialog: „aus Ereignis“ + Rolle (optional, eingeklappt).

### 3.4 Unverändert lassen

Beim Speichern eines Ereignisses nie das ganze GEDCOM schicken (Roh-Modus `gedcom` nur als Notbehelf); sonst
verschwinden Paten, Abschriften, Scans. Abnahmetest: I39 Geburt und F8 Heirat bearbeiten → alle Unterstrukturen
bleiben.

## 4. Listen, Bücher, Prüfung

| Stelle | Heute | Neu |
| - | - | - |
| Liste „Taufpaten“ (`ListenBaum.kt:165`) | nur Notizen | `associates` + `freeAssociates`, Spalte „verlinkt“ |
| Buch (`Buch.kt:187/193`) | Notizen, ASSO ausgeschlossen | „Paten: …“-Zeile aus beiden Quellen, Patenschaften im Personenteil (optional) |
| Prüfung (`Pruefung.kt:232`, Regeln 024, 126) | Regex auf Notiz | auf `associates`/`freeAssociates` umstellen; Regex nur Fallback |
| Neue Prüfregeln | – | Pate bei Taufe tot oder unter 14 · Elternteil als Pate · Trauzeuge unter 21/18 · `RELA` unbekannt/abweichend geschrieben (Hinweis) · `1 ASSO` statt in der Taufe (Hinweis mit Umwandlung) · Standesamt-Heirat ohne zwei Zeugen (Hinweis) |
| Listen mit Heirat (`ListenBaum.kt:51/128`, `Pruefung.kt:129`) | erste MARR | standesamtliche bevorzugt, sonst erste |

## 5. Datenschutz und Offline

- Private Paten: nur „privat“, kein Name, auch nicht im Tooltip, in Listen, Büchern, Exporten.
- Offline-Cache (`offline-konzept.md`): Antworten wie geliefert speichern – die API hat schon gefiltert.

## 6. Abnahme mit dem Demo-Baum 1.3

Seit 1.3 (01.10.2026) stehen freie Paten/Trauzeugen als `2 _GODP` / `2 _WITN` (eine Zeile je Person, GEDCOM-L);
die Notiz-Form `NOTE Paten: A; B` bleibt nur als Testfall (I140, I141, F60). Testsite ist mit 1.3 importiert.

| Fall | Fundstelle | Erwartung in der App |
| - | - | - |
| nur verlinkte Paten | I22 | 2 klickbare Namen |
| nur Text-Paten (`_GODP`) | I52, I144 | Eggers, Lüders – passend zum Scan M130 |
| gemischt + Notiz + Quelle am Paten | I21, I28 | I21: 2 verlinkt (I65 mit ⓘ „in Abwesenheit“ und ⧉) + 1 Text (`_GODP`) |
| alte Notiz-Form `NOTE Paten:` | I140, I141 (Trauzeugen F60) | wie `_GODP`; die Notiz erscheint nicht zusätzlich unter Notizen |
| lebende Patin | I1 / I10 | als Gast „privat“, angemeldet „Ute Falkenrath (Patin)“ |
| Gegenrichtung | I377 | Abschnitt „Pate bei“ mit allen Taufen |
| alte Schreibweisen | I38, I41, I57 | wie Hauptform, „Pate“/„Patin“ |
| `1 ASSO` | I58 | Paten an der Taufe, Hinweis „an der Person erfasst“ |
| Trauzeugen | F3, F6 (gemischt), F38 (nur `_WITN`) | Zeile „Trauzeugen“ |
| zwei Heiraten | F8 | „Heirat (standesamtlich)“ und „Heirat (kirchlich)“, beide bearbeitbar |
| ohne Art | F10 | „Heirat“ |
| Zitat aus Ereignis mit Rolle | Zitat an I62 | „aus Taufeintrag · Rolle Patin“ |
| Quelle mit Erfassungsdaten | S8 | Ereignisse, Zeitraum, Ort, Stelle |
| zwei Archive, Medium | S5 | Pfarrarchiv (Buch) + KirchenbuchDigital (digital) |
| Archiv mit Kontakt | R5 | Telefon, E-Mail, Web anklickbar |
| Bearbeiten ohne Verlust | I39 Geburt, F8 Heirat | Abschrift, Scan, Notiz, QUAY, Paten bleiben |

## 7. Reihenfolge

1. API 1.11: Lesen (1, 3, 4, 5) + Lecktest – Apps zeigen sofort mehr.
2. Apps: Modelle + Anzeige (2) + Listen/Prüfung (4).
3. API: `POST Association`, `type`, Quellen-Schreib-Bugfixes (MEDI, TEXT).
4. Apps: Bearbeiten (3).
5. Hilfe-Kapitel (`hilfe/de`, `hilfe/en`) und Galerie-Screenshots mit dem Demo-Baum.

## Offene Fragen

1. Android: Paten nur anzeigen oder auch bearbeiten?
2. Abschnitt „Patenschaften“ standardmäßig ein- oder ausgeklappt?
3. Buch: Patenschaften im Personenteil drucken (wird bei häufigen Paten lang)?
