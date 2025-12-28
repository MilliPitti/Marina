#!/bin/bash
# use java from environment
# JAVA_HOME usually set on Linux, otherwise use system java
if [ -z "$JAVA_HOME" ]; then
    JAVA=$(which java)
else
    JAVA=$JAVA_HOME/bin/java
fi

# Libraries
LIB_DIR=lib
JARS="$LIB_DIR/jaxb-xjc.jar:$LIB_DIR/jaxb-impl.jar:$LIB_DIR/jaxb-core.jar:$LIB_DIR/jaxb-api.jar:$LIB_DIR/activation.jar:$LIB_DIR/vecmath.jar"

# Check if jars exist (checking one is enough usually)
if [ ! -f "$LIB_DIR/jaxb-api.jar" ]; then
    echo "Error: JAXB jars not found in $LIB_DIR"
    exit 1
fi

echo "Using Java: $JAVA"
echo "Classpath: $JARS"

# Generate Marina classes
echo "Generating Marina classes..."
"$JAVA" -cp "$JARS" com.sun.tools.xjc.XJCFacade -d . -p de.smile.xml.marina jaxb/MarinaXML/Marina.xsd -extension

# Generate Weirs classes
echo "Generating Weirs classes..."
"$JAVA" -cp "$JARS" com.sun.tools.xjc.XJCFacade -d . -p de.smile.xml.marina.weirs jaxb/WeirsXML/Weirs.xsd -extension

echo "Done."
