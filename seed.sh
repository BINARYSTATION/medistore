#!/bin/bash
# Rebuilds the database and fills it with the demonstration data.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="$(find "$DIR/tools" -maxdepth 3 -type d -name Home | head -1)"
CLASSPATH="$DIR/build/classes:$DIR/tools/sqlite-jdbc.jar:$DIR/tools/flatlaf.jar:$DIR/tools/slf4j-api.jar:$DIR/tools/slf4j-nop.jar"
cd "$DIR"
exec "$JAVA_HOME/bin/java" -cp "$CLASSPATH" medistore.tools.SampleData
