# Persoonsblad en bewerken

Het persoonsblad opent met een **dubbelklik** op een persoon, met **Ctrl+E** of via het symbool **Bewerken**. Het toont alles over een persoon en is ook de plek om te bewerken.

## Opbouw

Bovenaan naam, data en foto. Daaronder de tabbladen:

- **Gegevens:** alle gebeurtenissen als tabel (gebeurtenis, datum, plaats / beschrijving), plus de leeftijd bij overlijden.
- **Levensloop:** de tijdlijn met huwelijk, geboorten van de kinderen en de leeftijd bij elke gebeurtenis. Ook de gebeurtenissen waarbij de persoon doopgetuige of getuige was, staan erin („Doopgetuige bij: doop van …“); een klik leidt naar het kind of het paar.
- **Doopgetuigen en huwelijksgetuigen:** onder doop en huwelijk staat de regel „Doopgetuigen:“ of „Huwelijksgetuigen:“ – in de levensloop, in het detailgebied van de gegevenstabel, in de gezinsweergave en op persoonskaarten. Doopgetuigen met een eigen record zijn onderstreept en aanklikbaar, die zonder record (uit een notitie „Paten: …“ of uit het doopgetuigenveld van andere stamboomprogramma's) staan als tekst. ⓘ klapt een notitie over de doopgetuige open, het bronsymbool opent de bron. Levende doopgetuigen die u niet mag zien, verschijnen alleen als „Privé“. Onder de gegevenstabel somt het onderdeel **Doopgetuige- en huwelijksgetuigerollen** alle dopen en huwelijken op waarbij de persoon doopgetuige of getuige was; een klik op de kop klapt het dicht. Beide vereisen api4webtrees 1.11 of nieuwer; met een oudere module blijft de doopgetuigennotitie gewoon een notitie.
- **Soort huwelijk:** burgerlijk en kerkelijk huwelijk verschijnen als aparte gebeurtenissen met hun soort; lijsten en de controle gebruiken het burgerlijke als beide bestaan.
- **Ouders/broers/zussen**: ouders en broers en zussen, ook halfbroers en -zussen. Een klik wisselt naar die persoon.
- **Partners/kinderen**: links de relaties, rechts de kinderen van de geselecteerde relatie, daaronder haar gebeurtenissen (huwelijk, scheiding, woonplaats …) om toe te voegen, te bewerken en te verwijderen. Dubbelklik op een partner of kind toont diens blad; **+** voegt een partner of een kind van deze relatie toe.
- **Naam**: voornamen, achternaam en naamtoevoeging hebben bij het bewerken elk een eigen veld.
- **Eenvoudig / Volledig** (onderaan het blad): „Eenvoudig“ toont in het tabblad Gegevens een formulier met naam, geboorte, doop, religie, beroep, huwelijk per relatie, overlijden en begrafenis om direct in te typen; „Volledig“ toont de tabel met alle gebeurtenissen, met leeftijd en markeringen voor notitie en bron (klik op een kolomkop om te sorteren).
  Alleen wat u gewijzigd hebt, wordt opgeslagen; bronnen, notities en andere details van de gebeurtenis blijven bewaard. Komt een gebeurtenis meer dan eens voor, dan bewerkt het formulier de eerste. Een datum die het programma niet kan interpreteren („voorjaar 1850“), wordt als datumtekst opgeslagen. Opslaan gebeurt bij het verlaten van een veld (Tab of ergens anders klikken), zoals overal in het programma. Een ongeldige datum blijft rood en wordt pas verzonden als u hem corrigeert of „Toch als tekst opslaan“ kiest; alleen dan vraagt het programma bij sluiten of afsluiten eerst om bevestiging.
- **Notities**, **Bronnen**, **Media**.
- **Kaart:** de levensplaatsen als lijst met links naar OpenStreetMap.

## Bladeren

**Page Up** en **Page Down** gaan naar de vorige of volgende persoon in de lijst, **Ctrl+Home** en **Ctrl+End** naar de eerste en laatste. **Esc** sluit het venster.

## Bewerken

Bewerken vereist bewerkingsrechten in webtrees. Wijzigingen gaan direct naar webtrees; afhankelijk van de instellingen van de stamboom gelden ze meteen of wachten ze op goedkeuring door een moderator.

- **Gebeurtenis wijzigen:** dubbelklik op de rij (of Bewerken). Datum en plaats worden gekozen, niet getypt: exact, omstreeks, voor, na, tussen; dag, maand, jaar. Plaatsen worden tijdens het typen uit de stamboom voorgesteld.
- **Gebeurtenis toevoegen**, ook gezinsgebeurtenissen zoals een huwelijk.
- **Verwant toevoegen** (Ctrl+N): ouders, partner, kind of broer/zus aanmaken met naam en eerste gebeurtenissen. In de navigator staat dit ook in het menu van de rechtermuisknop.
- **Doopgetuigen en huwelijksgetuigen invoeren:** selecteer de doop of het huwelijk in de gegevenstabel en klik in het detailgebied eronder op **Doopgetuigen bewerken …** of **Huwelijksgetuigen bewerken …** (huwelijken ook in het tabblad Partners/kinderen). De lijst ordent u met de pijlen, ✕ verwijdert een vermelding. **Persoon uit de stamboom …** zoekt op naam en koppelt de persoon; **Zonder record …** voegt iemand toe die geen eigen vermelding heeft – zoals in het kerkboek staat: „Friedrich Plate, landbouwer te Celle“. De rol (doopgetuige, getuige of een andere zoals „vroedvrouw“) en een notitie over de persoon staan onder de lijst. Gekoppelde doopgetuigen worden opgeslagen zoals webtrees dat doet, personen zonder record in het gangbare doopgetuigenveld van andere stamboomprogramma's; bronnen bij een doopgetuige blijven bewaard. Doopgetuigen die alleen bij de persoon zijn vastgelegd (oudere exports), verplaatst een vinkje naar de doop. Vereist api4webtrees 1.12 of nieuwer.
- **Soort huwelijk:** in het gebeurtenisvenster van een huwelijk kiest **Soort** tussen niet opgegeven, burgerlijk, kerkelijk, geregistreerd partnerschap en samenwonend. Een tweede plechtigheid (zoals de kerkelijke na de burgerlijke) voegt u toe als nog een gebeurtenis „Huwelijk“ van de relatie.
- **Verwijderen:** een gebeurtenis of de hele persoon, na bevestiging.
- **Bewerken in webtrees:** alles wat het programma zelf niet kan (namen, nieuwe bronnen, media koppelen), doet u op de persoonspagina in de browser. Zie [webtrees in de browser](hilfe:webtrees).

## Foto's

**Foto toevoegen** kiest een bestand op de pc en hangt het aan de persoon; de afbeelding wordt verkleind tot de uploadlimiet van de server. Een klik op een afbeelding opent de viewer.

## Afdrukken en delen

- **Bestand › Persoonsblad afdrukken …** (Ctrl+P) en **Persoonsblad als PDF …**
- **Persoon › Persoonstekst kopiëren** (Ctrl+Shift+C) zet alle gegevens als tekst op het klembord, bijv. voor een e-mail of een tekstverwerker.
- Meer uitvoer: [Lijsten](hilfe:listen) (persoonsblad als lijst), [Schema's](hilfe:tafeln), [Boeken](hilfe:buecher).
