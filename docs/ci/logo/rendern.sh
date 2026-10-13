#!/bin/sh
# Rendert die SVG-Vorlagen als PNG (Vorschau, Icon-Kandidaten). Schrift-SVGs werden dabei in Pfade gewandelt.
cd "$(dirname "$0")" || exit 1
for s in 16 24 32; do inkscape baum-klein.svg --export-type=png --export-width=$s --export-filename="../build/klein-$s.png" 2>/dev/null; done
for f in ahnenzeichen ahnenzeichen-hell ahnenzeichen-einfarbig monogramm sanduhr sanduhr-hell faecher faecher-hell kreis kreis-hell; do
  inkscape "$f.svg" --export-type=png --export-width=512 --export-filename="$f.png" 2>/dev/null
done
for z in wtwin wttux wtmac wtand; do
  inkscape wortmarke-$z.svg --export-type=png --export-width=800 --export-filename=wortmarke-$z.png 2>/dev/null
  inkscape logo-quer-$z.svg --export-type=png --export-width=1000 --export-filename=logo-quer-$z.png 2>/dev/null
  inkscape wortmarke-$z.svg --export-text-to-path --export-plain-svg --export-filename=wortmarke-$z-pfade.svg 2>/dev/null
  inkscape logo-quer-$z.svg --export-text-to-path --export-plain-svg --export-filename=logo-quer-$z-pfade.svg 2>/dev/null
done
# Textfreie Fassungen (Pfade) fuer Programme ohne Inter
inkscape monogramm.svg --export-text-to-path --export-plain-svg --export-filename=monogramm-pfade.svg 2>/dev/null
# Kleinstgroessen zur Probe (nach ../build, nicht ins Repo)
mkdir -p ../build
for z in ahnenzeichen sanduhr faecher kreis; do for s in 16 32 48; do inkscape $z.svg --export-type=png --export-width=$s --export-filename="../build/probe-$z-$s.png" 2>/dev/null; done; done
