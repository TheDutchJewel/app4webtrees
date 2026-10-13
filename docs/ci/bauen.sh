#!/bin/sh
# Setzt das CI-Dokument: build/wtwin-ci.pdf
cd "$(dirname "$0")" || exit 1
mkdir -p build
latexmk -lualatex -interaction=nonstopmode -halt-on-error -output-directory=build wtwin-ci.tex >/dev/null && echo "fertig: $(pwd)/build/wtwin-ci.pdf"
