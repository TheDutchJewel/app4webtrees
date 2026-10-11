# Archive

If the **Sammlungen** module runs on the server, **View › Archive** (Ctrl+9) or the **Archive** icon opens its archive in a window of its own. The [family tree on this PC](hilfe:lokal) already includes the module. The archive holds more than the family tree: photos and documents need not be attached to anyone. Who may see and change what is decided by webtrees and its user rights.

## Layout

- **On the left** the collections: archive folders, thematic collections, collections by media type and the media not attached to any person. Below that you see how many files belong to the free holdings, meaning nothing in the family tree points to them. That is intended, not a flaw.
- **In the middle** the files of the chosen collection as a grid, switchable between three sizes at the top. The badge on a picture gives the number of linked people, **GEDCOM** means a media object without a person, **free** means file only.
- **On the right** the details of the chosen file: description, date, people and keywords from the file, collections, size, path and who in the family tree is linked to it. A click on a name makes that person the central person.

Drag the dividers to set the column widths; wtWin remembers them.

## Working with it

A click selects a file, **Ctrl+click** adds more, **Shift+click** a whole range, **Ctrl+A** all of them. Use the arrow keys, **Home** and **End** to move through the grid; **Enter** or a double-click opens: pictures in the viewer, PDFs in the PDF viewer, everything else in the browser.

The **right-click** offers: Open, Edit details, Copy path (also Ctrl+C), Save copies to a folder and Open in webtrees. Saving copies fetches the originals to this PC; existing files are not overwritten.

## Adding files

Anyone allowed to upload media in webtrees adds files with **Add files …** or drags them from the file manager into the window. Folder and thematic collection are preset to the open collection. Description, date, people and keywords are optional and are written into images only (XMP).

wtWin uploads the original and shrinks nothing. A file larger than the server accepts is skipped and named. The limit is set by the server (PHP `upload_max_filesize`). Afterwards the files are attached to no one. To attach one to a person, a source or a place, use **Existing media › From the archive** on the **Media** tab.

## Editing details

Managers of the family tree change the description, date, people and keywords of a picture via the right-click, the button on the right or the pencil in the viewer. The module writes the details into the file itself.

## Faces on group photos

Right-click a picture › **Faces …** shows it large with all marked faces. Hovering shows the name at the frame; on the right the list goes row by row from the left, and a click on a name makes that person the central person. Managers tick **Mark a face**, drag a frame around a face and assign it: to a person in the family tree, to a name only (someone known but without a record) or to “unknown”. Move a selected frame by dragging it, resize it at the yellow handle, **Del** removes it. The frames are stored in the image file itself (XMP); other programs such as digiKam read them too. The picture is not attached to the person – unless you tick **Also attach the picture to the chosen person** in the assign dialog. In the person sheet, the **Media** tab lists under **On group photos** every picture on which the person is marked.

## Postcards

In a postcard collection, front and back are one entry (sign ↻). On the right **Turn over**, below that motif, transcription and note; an inferred date shows as “c. 1916”. A double-click shows both sides in the viewer; **R** or ↻ turns a picture written crosswise by 90 degrees for reading, **Shift+R** back (view only, nothing is saved). **Edit postcard …** changes motif, transcription, note and date; the module writes both sides and leaves unchanged fields as they are.

## Managing collections

Administrators create a collection with **+ New collection**: name, description, media folder (or none – then it is an album you fill with single pictures), display type, symbol, colour, order, visible. Right-click a collection: **Edit collection …**, **Hide**/**Show**, **Delete collection …** (the files stay). Hidden collections appear faded under “hidden”. Right-click pictures: **Thematic collections …** (a tick per album, takes effect at once, also for several selected pictures), **Remove from this collection** and **Rename …** (same folder, same extension; a media object in the family tree follows). **Archive settings …** sets the cache duration and page size and clears the cache. Who may do what is decided by the module.

## Comparison with webtrees

For pictures that are media objects in the family tree, the right column shows where file and webtrees differ: description against title, people in the file against the linked ones. **→ copy into the file** writes the webtrees value into the file; the family tree does not change.

## Backing up

In the family tree on this PC, **File › Back up family tree and archive …** saves everything into one ZIP file: the database with family trees and collections, and the whole media folder including the free holdings. A GEDCOM or GEDZIP file only takes the files the family tree points to. To restore, quit wtWin and put the `data` folder from the ZIP file into the webtrees folder (where that is, see [family tree on this PC](hilfe:lokal)).

On a server, the server does the backup, for example nas4webtrees or your web host.
