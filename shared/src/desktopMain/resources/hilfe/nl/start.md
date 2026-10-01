# Aan de slag

wtWin (onder Linux wtTux) toont en bewerkt een webtrees-stamboom zoals een klassiek genealogieprogramma op de pc: menubalk, navigator, persoonsblad, schema's, lijsten en boeken. De gegevens staan op een webtrees-server (NAS of webhoster) of direct op deze pc.

Bij de eerste start staan beide wegen naast elkaar.

## Verbinden met webtrees

Voorwaarde: webtrees 2.2 met de module **api4webtrees**. Wie de server beheert, installeert de module; het pakket nas4webtrees bevat hem al.

**Het eenvoudigst, zonder typen:** meld u in de browser aan bij webtrees, open het menu **App** en klik op **Verbinden met wtWin**. Het programma neemt de verbindingslink over (uit het klembord of direct uit de browser), vraagt één keer en opent de stamboom. Geen adres, geen wachtwoord. De link is tien minuten en precies één keer geldig.

**Handmatig:**

1. Voer het adres van uw webtrees-site in zoals het in de browser staat. De rest knipt het programma zelf af. Staat er een geschikt adres op het klembord, dan wordt het voorgesteld.
2. Klik op **Verbinden**.
3. Meld u aan met gebruikersnaam (of e-mailadres) en wachtwoord. **Bekijken zonder aan te melden** toont alleen wat bezoekers op de website zien.

U meldt zich aan met uw gewone webtrees-account. Dezelfde rechten gelden als op de website: wat u daar niet kunt zien, ziet u hier ook niet; wat u daar mag bewerken, mag u hier ook bewerken.

> Onversleutelde adressen (`http://`) worden alleen in het thuisnetwerk geaccepteerd, bijv. `http://192.168.178.73:8095`. Buitenshuis heeft de server HTTPS nodig.

## Stamboom op deze pc

Rechts in het startscherm: voer een naam in en klik op **Stamboom aanmaken**, of op **Uit een GEDCOM-bestand overnemen …** om over te stappen vanuit een ander programma. Vanaf dan werkt u zonder server, wachtwoord of internet. Zie [Stamboom op deze pc](hilfe:lokal).

## Wisselen van stamboom of server

- Heeft de server meerdere stambomen, wissel dan via **Bestand › Andere stamboom**.
- Naar een andere server: **Bestand › Afmelden**, daarna **Ander adres**. Of klik op de pagina **App** van de andere server op **Verbinden met wtWin**; dat werkt ook terwijl u aangemeld bent.

## Taal

Het programma volgt de systeemtaal (Duits, Engels, Frans, Nederlands of Spaans, anders Engels). Kies een vaste taal onder **Beeld › Taal**. Teksten die van de server komen (namen van gebeurtenissen, verwantschappen) verschijnen in de taal van het programma.

## Verder

- [Het hoofdvenster](hilfe:hauptfenster): navigator, werkbalk, weergave
- [Persoonsblad en bewerken](hilfe:person)
- [Schema's](hilfe:tafeln), [Lijsten](hilfe:listen), [Boeken](hilfe:buecher)
- [Sneltoetsen](hilfe:tasten)
