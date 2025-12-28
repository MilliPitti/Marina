#!/bin/bash

# ----- Globale Einstellungen -----
# Determine the absolute path of the script directory
MARINA_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CLASSPATH=$CLASSPATH:$MARINA_PATH
# Compile output path (if compiled via IDE/Command line previously)
CLASSPATH=$CLASSPATH:$MARINA_PATH/.

# ----- Libraries -----
# Using local lib directory where we copied the jars
CLASSPATH=$CLASSPATH:$MARINA_PATH/lib/vecmath.jar
CLASSPATH=$CLASSPATH:$MARINA_PATH/lib/jaxb-impl.jar
CLASSPATH=$CLASSPATH:$MARINA_PATH/lib/jaxb-api.jar
CLASSPATH=$CLASSPATH:$MARINA_PATH/lib/activation.jar
CLASSPATH=$CLASSPATH:$MARINA_PATH/lib/jaxb-core.jar
CLASSPATH=$CLASSPATH:$MARINA_PATH/lib/jaxb-xjc.jar
CLASSPATH=$CLASSPATH:$MARINA_PATH/lib/sgt_v30.jar

export CLASSPATH
export MARINA_PATH

if [ $# -eq 0 ]; then
    echo "usage: $0 <MarinaControl.xml>"
else
    # Run
    echo "Starting Marina..."
    # echo "CLASSPATH: $CLASSPATH"
    # Using simple 'nice' to avoid permission denied errors (default is usually +10 niceness)
    nice java -Xmx4G -cp "$CLASSPATH" de.smile.marina.MarinaXML "$1"
fi
