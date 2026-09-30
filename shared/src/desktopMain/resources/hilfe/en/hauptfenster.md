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

**View › Sources** (Ctrl+5) or the **Sources** icon opens the source manager: all sources of the tree on the left with search and the number of citations, the selected source on the right with author, publication, repository and call number, text, notes, media and **Cited by** – every individual and family with the facts that carry the citation. A click selects the person, a double-click makes them the central person. Adding and changing sources is done via **Open in webtrees** for now.

In the person sheet, the detail area below the event table (tab **Sources**) shows each citation with page, quality, date, quotation, notes and media; a click on the title opens the source here. A source without its own record ("according to Martha Meier") is shown in italics.

In the layouts **Tree in the centre** and **Family view** the **Sources** button is in the bar at the top; in the person panel on the right (tab Events) every citation is clickable and opens the source.

Needs api4webtrees with API level 18; with older servers the icon opens the webtrees source list as before.

## Sections: Home, Tree, Photos

- **Home** (Ctrl+1): greeting, upcoming anniversaries, recent changes in the tree, the start person. Moderators see pending changes here and accept or reject them.
- **Tree** (Ctrl+2): navigator, tree view or family.
- **Photos** (Ctrl+3): all pictures of the tree; a click opens the viewer. If the Collections module runs on the server, its archive appears here too.

## View

- **Appearance:** light, dark or like the system.
- **Colour coding:** colours the start person's ancestors by the four grandparent lines and their descendants in a fifth colour (after Mary Hill). Needs a start person.
- **Show siblings and partners**, **Show cousins**: for the tree view.
- **Generations:** how many ancestor generations the tree loads.

## Back, forward, history

Every change of the central person goes into the history. **Alt+Left** goes back, **Alt+Right** forward again; the **History** icon lists the last persons. **Alt+Home** jumps to the start person webtrees knows for your account.

## Status bar

At the bottom: server, account (or "guest") and program version. **F5** reloads the tree, e.g. after changes in the browser.
