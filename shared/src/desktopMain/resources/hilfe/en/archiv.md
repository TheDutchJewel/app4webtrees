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

## Backing up

In the family tree on this PC, **File › Back up family tree and archive …** saves everything into one ZIP file: the database with family trees and collections, and the whole media folder including the free holdings. A GEDCOM or GEDZIP file only takes the files the family tree points to. To restore, quit wtWin and put the `data` folder from the ZIP file into the webtrees folder (where that is, see [family tree on this PC](hilfe:lokal)).

On a server, the server does the backup, for example nas4webtrees or your web host.
