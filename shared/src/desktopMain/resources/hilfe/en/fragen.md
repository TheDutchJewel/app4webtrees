# Questions, errors, privacy

## Reporting questions and errors

wtWin, wtTux, wtMac and wtAnd are not official webtrees products. Please do not ask in the webtrees forum, but open an issue at **github.com/thobgg/app4webtrees/issues**. Helpful: the version from **Help › About wtWin** (with build number), Windows or Linux, and what you were doing when it happened. For the family tree on this PC also the file `php.log` (see [Family tree on this PC](hilfe:lokal)).

New versions appear at **github.com/thobgg/app4webtrees/releases**. The Windows file is not signed, so Windows warns about an unknown publisher on first start ("More info", then "Run anyway").

## What the program stores

- Server address, user name and the session cookie, **never the webtrees password**. Only the credentials of a directory protection (if entered) stay on the PC, they have to go with every request.
- Your settings (layout, appearance, chart and list settings, colour rules, tick list of the check) on this PC.
- For the family tree on this PC: the whole tree in the folder `app4webtrees` including the login to the local webtrees.
- No analytics, no advertising, no sharing.

## Privacy in the tree

The program signs in with your webtrees account; every request runs as this user. The same rules apply as on the website: living persons, locked records and private trees are visible only if webtrees allows it. Output (charts, lists, books, PDF) contains only what you may see.

## Unencrypted on the home network

An `http://` address is accepted only on the home network (private addresses like 192.168…, names like `diskstation` or `.local`). The connection is then unencrypted, which is fine at home. To reach the tree while away, the server needs HTTPS; nas4webtrees provides the instructions.

## Phone and tablet

For Android there is **wtAnd**, the same family with the same data: tree, photos, editing, anniversaries. A QR code on the webtrees **App** page connects the phone without typing. wtAnd needs a server; the family tree on this PC is reachable only on the PC.

## Licence

wtWin is licensed under the GPL-3, like webtrees. The family tree on this PC contains webtrees unchanged from the official release (webtrees.net).

## Full manual

With pictures and current notes (in German): **bgg-home.de/me/genealogie/wtwin/** (button at the bottom of this window).
