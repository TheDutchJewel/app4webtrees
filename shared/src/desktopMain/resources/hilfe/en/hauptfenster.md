# The main window

Under **View › Layout** you choose between three layouts. All show the same tree, arranged differently.

## Navigator (default)

The toolbar on top, below it the **central person** with partners and children, to the right their ancestors, generation by generation.

- **One click** on a person makes them the central person.
- **Double-click** opens the [person sheet](hilfe:person).
- **Right mouse button**: show as central person, edit, bookmark, open in webtrees.
- **Siblings** are listed in the info box at the top left (half-siblings with ½); a click makes them the central person.
- **Keyboard:** right arrow to the father (with Shift to the mother), left to the child, up/down through the siblings, Enter opens the sheet (see [Keyboard shortcuts](hilfe:tasten)).
- **Generations** (2 to 7) and **zoom** (−, +, fit) are in the navigator itself; the choice is remembered.
- A missing parent shows as "father unknown" or "mother unknown". With edit rights, **Add relative** creates the missing person.

**Toolbar:** Go to, Edit, Add relative, Bookmarks, Back, Forward, History, Start person, List, Chart, Print, Home, Photos, Check, Places, Sources, Help, Quit. If the width is not enough, it shows icons only (name on hover), finally the rest moves into the **More** menu. Switch the captions off under **View › Toolbar labels**.

## Tree in the centre

Three columns: the **person list** with a search field on the left (Ctrl+F jumps into it), the **tree** as an hourglass around the central person in the middle (drag, zoom with the mouse wheel, expand branches upwards), the **person panel** with the selected person's details on the right.

When zooming out a card shows less rather than smaller: first without picture and years, then only the given name, finally a box in the sex colour. Right-click in the list: as central person, profile, bookmark, open in webtrees.

## Family view

Like "Tree in the centre", but the middle shows the central person's **family**: at the top the parents of both partners, in the middle the couple with their marriage, below the children with their dates and marriages. If the person married more than once, each marriage has its own **tab**.

- **A click** selects a person; the person panel on the right shows them.
- **Double-click** makes them the central person – this is how you move through the families up (parents) and down (children).
- **Right mouse button**: Show as focus, Add relative, Open in webtrees.
- A missing parent shows "Father unknown" or "Mother unknown"; with edit rights, **+ add** creates them.
- The central person's **siblings** are shown above the couple. **Keyboard:** up arrow to the father (with Shift to the mother), down to the child, left/right through the siblings, Tab to the next tab.

The [person table](hilfe:tabelle) (Ctrl+4) shows everyone as a table.

## Sources

**View › Sources** (Ctrl+5) or the **Sources** icon opens the source manager: all sources of the tree on the left with search and the number of citations, the selected source on the right with author, publication, repository and call number, text, notes, media and **Cited by** – every individual and family with the facts that carry the citation. A click selects the person, a double-click makes them the central person.

With edit rights: **+ New source** (title, author, publication, abbreviation, repository with call number – also a new repository –, text, note), **Edit**, **Add scan or file …** (attaches the file to the source as a document) and **Delete** (with a warning if the source is still cited). **Remove unused …** lists all sources nobody cites, to tick and delete. In the citation dialog, **New source …** creates a source directly and **Source from file …** creates one from a scan: title from the file name, the file attached, the source selected right away.

**Documents** are attached in two places, as in webtrees: to the **source** (the digitised parish register) or to the single **citation** (the scan of exactly this baptism). In both places you can **upload a file** or choose **existing media** of the tree; “unlink” or ✕ only removes the link, media object and file are kept.

In the person sheet, the detail area below the event table (tab **Sources**) shows each citation with page, quality, date, quotation, notes and media; a click on the title opens the source here. A source without its own record ("according to Martha Meier") is shown in italics.

With edit rights, the buttons below are **+ Cite source** (search a source in the manager or enter it as text, plus page, quality, date, quotation and note), **Edit**, **Remove**, **▲ ▼** (order) and **Copy to …** – the same citation for further events of this person, for parents, partners and children (as a general citation on the record) or for the partnership. Only what you enter is changed; everything else on the citation and the event is kept.

In the layouts **Tree in the centre** and **Family view** the **Sources** button is in the bar at the top; in the person panel on the right (tab Events) every citation is clickable and opens the source.

Needs api4webtrees with API level 18; with older servers the icon opens the webtrees source list as before.


## Places

**View › Places** (Ctrl+6) or the **Places** icon opens the place manager (api4webtrees 1.13 or later): all places as written at the events on the left, with search, the number of events and ◉ for “coordinates known”. On the right the selected place in tabs: **People** (every individual and family with their events there – a click selects the person, a double-click makes them the central person), **Details** (levels, the place above, places within, location record and GOV identifier), **Notes**, **Sources**, **Media** and **Coordinates** with a map. In webtrees a place’s note, GOV identifier and coordinates are kept in its location record (_LOC, GEDCOM-L); without one, wtWin takes the coordinates from webtrees’ geographic data or from an event.

The **Details** tab is the place page: at the top a picture (your own photo at the location record, otherwise a suggestion from Wikimedia Commons with credit), tiles for births, marriages, deaths and other events, the **hierarchy** from GOV (today and earlier, with years) and **Look up** with GOV, GenWiki, Wikipedia, Archion, Matricula, Archivportal-D and the German Digital Library. For this the place name and GOV identifier go to gov.genealogy.net and Wikimedia; the answers stay on this PC for 7 days. A Wikimedia picture is shown only if the match lies within 30 km of the place’s coordinates.

**Map** (switch above the list) shows every place with coordinates: size and colour by the number of events, nearby places as a grey group with their count – a click zooms in. A click on a place selects it, **Show** switches to its data. The search also filters the map; how many places still lack coordinates is shown top left.

With edit rights **Edit** opens the place data: GOV identifier (with **Search GOV**), note and coordinates. **Find coordinates …** asks OpenStreetMap for the place name; a click on a result takes latitude and longitude. Everything is saved in the location record (_LOC), which wtWin creates when needed; if the place name occurs more than once in the tree, the events at the place get a pointer to it. Administrators can also write the coordinates to webtrees’ geographic data – the maps in the browser read only those.

**Rename and merge:** the place name at the top is an input field. Change it and confirm with the tick (or Enter) – before that wtWin tells you how many events will be changed and which places below move along (“Kortau, Allenstein” → “Kortau, Olsztyn”). If the new name already exists, both places are merged: notes, sources and media of the location records are kept; if GOV identifier or coordinates differ, those of the target apply. Locked or confidential events you may not change keep the old name. Without automatic acceptance the changes wait for a moderator as usual. Tip: rename typing errors, but leave historical names (Allenstein/Olsztyn) as they are and connect them through the GOV identifier.

Place names in the person panel and on the person sheet can be clicked, and every filled place field has a place icon – both open the place directly in the place manager.

With edit rights you write the note directly in the **Notes** tab, attach photos and documents in the **Media** tab (upload a file, an existing media object or one from the archive; **unlink** only removes the link) and cite a source for the place in the **Sources** tab. Everything goes into the location record; if there is none yet, wtWin creates it. The first photo at the location record becomes the picture at the top of the place page.

Coordinates can be typed as decimals or in degrees, minutes, seconds; **From clipboard** takes a copied pair, for example from Wikipedia. With **Find coordinates …** two ticks decide whether a result supplies the coordinates and/or postal code, region and country. In the **Coordinates** tab buttons open the place in OpenStreetMap, Bing Maps or Google Maps; **Centre map** brings it back to the middle. In the **People** tab the mouse pointer shows life dates and events; a double-click opens the person sheet.

## Sections: Home, Tree, Photos

- **Home** (Ctrl+1): greeting, upcoming anniversaries, recent changes in the tree, the start person. Moderators see pending changes here and accept or reject them.
- **Tree** (Ctrl+2): navigator, tree view or family.
- **Photos** (Ctrl+3): all pictures of the tree; a click opens the viewer. If the Collections module runs on the server, its archive appears here too.

## View

- **Appearance:** light, dark or like the system.
- **Language:** like the system or fixed to German, English, French, Dutch or Spanish. Takes effect at once; labels from the server (event types, places) arrive in the new language with the next reload. Help and plausibility check switch as well.
- **Colour coding:** colours the start person's ancestors by the four grandparent lines and their descendants in a fifth colour (after Mary Hill). Needs a start person.
- **Show siblings and partners**, **Show cousins**: for the tree view.
- **Generations:** how many ancestor generations the tree loads.

## Back, forward, history

Every change of the central person goes into the history. **Alt+Left** goes back, **Alt+Right** forward again; the **History** icon lists the last persons. **Alt+Home** jumps to the start person webtrees knows for your account.

## Status bar

At the bottom: server, account (or "guest") and program version. **F5** reloads the tree, e.g. after changes in the browser.
