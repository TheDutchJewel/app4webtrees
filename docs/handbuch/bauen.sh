#!/bin/sh
# Setzt das Handbuch: bauen.sh [sprache ...]  (Vorgabe: alle fuenf). Ergebnis: build/wtwin-handbuch-<sprache>.pdf
cd "$(dirname "$0")" || exit 1
sprachen="${*:-de en fr nl es}"
mkdir -p build
for s in $sprachen; do
  python3 md2tex.py "$s" "build/$s" || exit 1
  (cd "build/$s" && TEXINPUTS="../../../ci:" latexmk -lualatex -interaction=nonstopmode -halt-on-error handbuch.tex >/dev/null) \
    && cp "build/$s/handbuch.pdf" "build/wtwin-handbuch-$s.pdf" && echo "fertig: build/wtwin-handbuch-$s.pdf" \
    || { echo "FEHLER in $s – siehe build/$s/handbuch.log"; grep -n -A4 '^!' "build/$s/handbuch.log" | head -20; exit 1; }
done
