#!/bin/bash
set -euo pipefail

# Generate JAXB classes before compilation so MarinaXML bindings are up to date.
./generate_jaxb.sh

SRC_FILES=()
while IFS= read -r -d '' file; do
  SRC_FILES+=("$file")
done < <(find bijava de -name "*.java" \
  ! -path "de/smile/xml/marina/*" -print0)

JAXB_FILES=()
while IFS= read -r -d '' file; do
  JAXB_FILES+=("$file")
done < <(find bin/generated-src -name "*.java" -print0)

javac -Xlint -cp "lib/*:bin" -d bin "${SRC_FILES[@]}" "${JAXB_FILES[@]}"
