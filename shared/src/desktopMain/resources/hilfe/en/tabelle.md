# Person table

The **person table** shows everyone in the family tree in columns: ID, name, sex, birth, birth place, death, death place and occupation. Open it via **View › Person table** (Ctrl+4) or the **List** icon. The window can stay open next to the main window.

## Sorting

Click a column header to sort by that column, click again to reverse the order. Dates are sorted by the calendar, not by their text; people without a value always come last.

## Filtering

Below each column header there is a filter field. Several filters apply together.

- **Text:** finds parts, ignoring upper and lower case – "hann" finds Hannover.
- **1800-1850** under birth or death: years from–to; also **-1850** or **1800-**.
- `!` – the field is empty. This is how you find gaps, for example everyone without a birth place.
- `*` – the field is filled.

**Clear filters** resets all fields. The top line shows how many people match.

If the birth is missing, the baptism is shown (with ~); if the death is missing, the burial (with □). Private people are not listed.

## Working with the table

- **Click:** selects the person; the person panel in the main window shows them.
- **Double-click** or **Enter:** show the person as focus.
- **Arrow keys, Page up/down:** move through the table.
- **Right-click:** Show as focus, Edit person, Open in webtrees.
- **Save as CSV …** writes the rows currently shown to a file for spreadsheet programs.

The table needs the whole tree at once; the server must run api4webtrees 1.9 or later (see [Lists](hilfe:listen)).
