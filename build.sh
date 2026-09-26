#!/bin/bash
# Compiles every source file into build/classes.
# Uses only the JDK that ships inside tools/ - nothing has to be installed.
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="$(find "$DIR/tools" -maxdepth 3 -type d -name Home | head -1)"
if [ -z "$JAVA_HOME" ]; then
    echo "No JDK found in tools/. Run tools/get-jdk.sh first."
    exit 1
fi

CLASSPATH="$DIR/tools/sqlite-jdbc.jar:$DIR/tools/flatlaf.jar:$DIR/tools/slf4j-api.jar:$DIR/tools/slf4j-nop.jar"

rm -rf "$DIR/build/classes"
mkdir -p "$DIR/build/classes"

# Each path is quoted because javac splits an argument file on spaces,
# which breaks the moment the project sits in a folder with a space in it.
find "$DIR/src" -name "*.java" | sed 's/.*/"&"/' > "$DIR/build/sources.txt"
"$JAVA_HOME/bin/javac" -encoding UTF-8 -Xlint:-this-escape \
    -d "$DIR/build/classes" -cp "$CLASSPATH" "@$DIR/build/sources.txt"

echo "Build OK  ->  $(find "$DIR/build/classes" -name '*.class' | wc -l | tr -d ' ') classes"
