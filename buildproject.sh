#!/bin/bash
set -euo pipefail

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$PROJECT_DIR"

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
done < <(find generated-src -name "*.java" -print0)

javac --release 17 -Xlint -cp "lib/*:bin" -d bin "${SRC_FILES[@]}" "${JAXB_FILES[@]}"

mkdir -p dist
jar --create --file dist/Marina.jar -C bin .
echo "Created JAR: $PROJECT_DIR/dist/Marina.jar"
