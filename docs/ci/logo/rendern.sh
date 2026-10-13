#!/bin/sh
# Rendert die SVG-Vorlagen als PNG (Vorschau, Icon-Kandidaten). Schrift-SVGs werden dabei in Pfade gewandelt.
cd "$(dirname "$0")" || exit 1
for f in ahnenzeichen ahnenzeichen-hell ahnenzeichen-einfarbig monogramm; do
  inkscape "$f.svg" --export-type=png --export-width=512 --export-filename="$f.png" 2>/dev/null
done
inkscape wortmarke.svg --export-type=png --export-width=800 --export-filename=wortmarke.png 2>/dev/null
inkscape logo-quer.svg --export-type=png --export-width=1000 --export-filename=logo-quer.png 2>/dev/null
# Textfreie Fassungen (Pfade) fuer Programme ohne Inter
inkscape monogramm.svg --export-text-to-path --export-plain-svg --export-filename=monogramm-pfade.svg 2>/dev/null
inkscape wortmarke.svg --export-text-to-path --export-plain-svg --export-filename=wortmarke-pfade.svg 2>/dev/null
inkscape logo-quer.svg --export-text-to-path --export-plain-svg --export-filename=logo-quer-pfade.svg 2>/dev/null
# Kleinstgroessen zur Probe (nach ../build, nicht ins Repo)
mkdir -p ../build
for s in 16 32 48; do inkscape ahnenzeichen.svg --export-type=png --export-width=$s --export-filename="../build/probe-ahnenzeichen-$s.png" 2>/dev/null; inkscape monogramm.svg --export-type=png --export-width=$s --export-filename="../build/probe-monogramm-$s.png" 2>/dev/null; done
