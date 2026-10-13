# Handbuch

Das Handbuch entsteht aus der Programmhilfe (`shared/src/desktopMain/resources/hilfe/<sprache>/*.md`) und hat immer deren Stand.

| Befehl | Ergebnis |
|---|---|
| `docs/handbuch/bauen.sh` | `build/wtwin-handbuch-{de,en,fr,nl,es}.pdf` |
| `docs/handbuch/bauen.sh de` | nur eine Sprache |

Braucht TeX Live mit LuaLaTeX, latexmk, KOMA-Script, Inter, Source Serif Pro, Source Code Pro. Erscheinungsbild: `docs/ci/wtwinci.sty`.
