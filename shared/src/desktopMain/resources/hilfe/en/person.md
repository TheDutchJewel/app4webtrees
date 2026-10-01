# Person sheet and editing

The person sheet opens with a **double-click** on a person, with **Ctrl+E** or via the **Edit** icon. It shows everything about a person and is also the place for editing.

## Layout

Name, dates and picture on top. Below, the tabs:

- **Data:** all events as a table (event, date, place / description), plus age at death.
- **Life:** the timeline with marriage, births of the children and the age at each event. It also lists the events where the person was a godparent or witness (“Godmother at: Christening of …”); a click leads to the child or the couple.
- **Godparents and witnesses:** below baptism and marriage a line “Godparents:” or “Witnesses:” appears – in the timeline, in the detail area of the data table, in the family view and on index cards. Godparents with their own record are underlined and clickable, those without (from a note “Paten: …” or the GEDCOM-L field _GODP) appear as text. ⓘ unfolds a note about the godparent, the source icon opens the source. Living godparents you are not allowed to see show only as “Private”. Below the data table the section **Godparent and witness roles** lists all baptisms and marriages where the person was a godparent or witness; a click on the heading folds it away. Both need api4webtrees 1.11 or newer; with an older module the godparent note simply stays a note.
- **Type of marriage:** civil and religious marriage appear as separate events with their type; lists and the check use the civil one when both exist.
- **Parents/Siblings**: parents and siblings, including half-siblings. A click switches to that person.
- **Partners/Children**: the partnerships on the left, the children of the selected one on the right, below its events (marriage, divorce, residence …) to add, edit and delete. Double-click a partner or child to show their sheet; **+** adds a partner or a child of this partnership.
- **Name**: given names, surname and name suffix have their own fields when editing.
- **Simple / Full** (bottom of the sheet): “Simple” shows a form in the Data tab with name, birth, baptism, religion, occupation, marriage per partnership, death and burial to type into directly; “Full” shows the table of all events with age and markers for note and source (click a column header to sort).
  Only what you changed is saved; sources, notes and other details of the event are kept. If an event exists more than once, the form edits the first. A date the program cannot interpret (“spring 1850”) is saved as date text. Saving happens when you leave a field (Tab or click elsewhere), as everywhere in the program. An invalid date stays red and is not sent until you correct it or choose “Save as text anyway”; only then does closing or quitting ask first.
- **Notes**, **Sources**, **Media**.
- **Map:** the places of life as a list with links to OpenStreetMap.

## Paging

**Page Up** and **Page Down** move to the previous or next person in the list, **Ctrl+Home** and **Ctrl+End** to the first and last. **Esc** closes the window.

## Editing

Editing needs edit rights in webtrees. Changes go to webtrees immediately; depending on the tree's settings they apply at once or wait for a moderator's approval.

- **Change an event:** double-click the row (or Edit). Date and place are selected, not typed: exact, about, before, after, between; day, month, year. Places are suggested from the tree as you type.
- **Add event**, including family events such as marriage.
- **Add relative** (Ctrl+N): create parents, partner, child or sibling with name and first events. In the navigator this is also in the right-click menu.
- **Delete:** an event or the whole person, after confirmation.
- **Edit in webtrees:** everything the program cannot do itself (names, new sources, linking media) you do on the person page in the browser. See [webtrees in the browser](hilfe:webtrees).

## Photos

**Add photo** picks a file from the PC and attaches it to the person; the picture is scaled to the server's upload limit. A click on a picture opens the viewer.

## Printing and sharing

- **File › Print person sheet …** (Ctrl+P) and **Person sheet as PDF …**
- **Person › Copy person text** (Ctrl+Shift+C) puts all details as text on the clipboard, e.g. for an e-mail or a word processor.
- More output: [Lists](hilfe:listen) (person sheet as a list), [Charts](hilfe:tafeln), [Books](hilfe:buecher).
