#!/bin/bash
set -euo pipefail

# Generate JAXB classes before compilation so MarinaXML bindings are up to date.
./generate_jaxb.sh

mapfile -t SRC_FILES < <(find bijava de -name "*.java" \
  ! -path "de/smile/xml/marina/*")
mapfile -t JAXB_FILES < <(find bin/generated-src -name "*.java")

javac -Xlint -cp "lib/*:bin" -d bin "${SRC_FILES[@]}" "${JAXB_FILES[@]}"
