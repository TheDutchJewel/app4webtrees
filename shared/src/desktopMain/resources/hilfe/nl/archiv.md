# Archief

Draait op de server de module **Sammlungen**, dan opent **Beeld › Archief** (Ctrl+9) of het pictogram **Archief** het archief in een eigen venster. De [stamboom op deze pc](hilfe:lokal) bevat de module al. Het archief bevat meer dan de stamboom: foto’s en documenten hoeven aan niemand gekoppeld te zijn. Wie wat mag zien en wijzigen, regelt webtrees met zijn gebruikersrechten.

## Opbouw

- **Links** de verzamelingen: archiefmappen, thematische verzamelingen, verzamelingen per mediatype en de media die aan geen persoon gekoppeld zijn. Daaronder staat hoeveel bestanden tot het vrije bestand horen, waar in de stamboom dus niets naar verwijst. Dat is zo bedoeld en geen gebrek.
- **In het midden** de bestanden van de gekozen verzameling als raster, bovenaan in drie groottes te kiezen. Het label op de afbeelding geeft het aantal gekoppelde personen, **GEDCOM** betekent mediaobject zonder persoon, **vrij** betekent alleen bestand.
- **Rechts** de gegevens van het gekozen bestand: beschrijving, datum, personen en trefwoorden uit het bestand, verzamelingen, grootte, pad en wie er in de stamboom aan gekoppeld is. Een klik op een naam maakt die persoon tot proband.

De breedte van de kolommen stel je in met de scheidingslijnen; wtWin onthoudt ze.

## Bediening

Een klik kiest een bestand, **Ctrl+klik** voegt er meer toe, **Shift+klik** een heel bereik, **Ctrl+A** alle. Met de pijltoetsen, **Home** en **End** ga je door het raster; **Enter** of dubbelklik opent: afbeeldingen in de viewer, pdf’s in de pdf-viewer, al het andere in de browser.

De **rechtermuisklik** biedt: Openen, Gegevens bewerken, Pad kopiëren (ook Ctrl+C), Kopieën in een map opslaan en Openen in webtrees. Kopieën opslaan haalt de originelen naar deze pc; bestaande bestanden worden niet overschreven.

## Bestanden toevoegen

Wie in webtrees media mag uploaden, voegt bestanden toe met **Bestanden toevoegen …** of sleept ze vanuit de bestandsbeheerder naar het venster. Map en thematische verzameling zijn vooringesteld op de open verzameling. Beschrijving, datum, personen en trefwoorden zijn vrijwillig en worden alleen in afbeeldingen geschreven (XMP).

wtWin uploadt het origineel en verkleint niets. Een bestand dat groter is dan de server aanneemt, wordt overgeslagen en genoemd. De grens stelt de server in (PHP `upload_max_filesize`). Daarna zijn de bestanden aan niemand gekoppeld. Aan een persoon, bron of plaats koppel je ze op het tabblad **Media** met **Bestaand medium › Uit het archief**.

## Gegevens bewerken

Beheerders van de stamboom wijzigen beschrijving, datum, personen en trefwoorden van een afbeelding via de rechtermuisklik, de knop rechts of het potlood in de viewer. De module schrijft de gegevens in het bestand zelf.

## Back-up

In de stamboom op deze pc slaat **Bestand › Stamboom en archief back-uppen …** alles op in één zip-bestand: de database met stambomen en verzamelingen en de hele mediamap inclusief het vrije bestand. Een GEDCOM- of GEDZIP-bestand neemt alleen de bestanden mee waar de stamboom naar verwijst. Om terug te zetten wtWin afsluiten en de map `data` uit het zip-bestand in de webtrees-map leggen (waar die staat, lees je onder [stamboom op deze pc](hilfe:lokal)).

Op een server maakt de server zelf de back-up, bijvoorbeeld nas4webtrees of je webhoster.
