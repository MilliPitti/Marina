#!/bin/bash
# Cleanup script to remove compiled classes and generated JAXB sources from source directories

echo "Cleaning up .class files from source directories..."
find de bijava -name "*.class" -delete

echo "Cleaning up generated JAXB sources from generated-src..."
rm -rf generated-src/*.java
rm -rf generated-src/de/smile/xml/marina/*.java
rm -rf generated-src/de/smile/xml/marina/weirs/*.java

echo "Cleanup complete."
