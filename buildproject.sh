#!/bin/bash
set -euo pipefail

# Generate JAXB classes before compilation so MarinaXML bindings are up to date.
./generate_jaxb.sh

javac -Xlint -cp "lib/*:bin" -d bin $(find . -name "*.java")
