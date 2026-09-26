#!/bin/bash
# Builds if needed, then starts the application.
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_HOME="$(find "$DIR/tools" -maxdepth 3 -type d -name Home | head -1)"
CLASSPATH="$DIR/build/classes:$DIR/tools/sqlite-jdbc.jar:$DIR/tools/flatlaf.jar:$DIR/tools/slf4j-api.jar:$DIR/tools/slf4j-nop.jar"

[ -d "$DIR/build/classes" ] || "$DIR/build.sh"

cd "$DIR"
exec "$JAVA_HOME/bin/java" -cp "$CLASSPATH" \
    -Dapple.awt.application.appearance=system \
    medistore.Main "$@"
