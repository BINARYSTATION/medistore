#!/bin/bash
# Builds the portable Windows bundle:  submission/MediStore-Portable/  and  .zip
#
# The bundle carries its own trimmed-down Windows Java runtime, so the college PC
# needs nothing installed: unzip, then double-click MediStore.bat.
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
MAC_JDK="$(find "$DIR/tools" -maxdepth 3 -type d -name Home | head -1)"
WIN_JDK="$(find "$DIR/tools/jdk-win" -maxdepth 1 -mindepth 1 -type d | head -1)"
WIN32_JDK="$(find "$DIR/tools/jdk-win32" -maxdepth 1 -mindepth 1 -type d | head -1)"
OUT="$DIR/submission/MediStore-Portable"
MODULES=java.base,java.desktop,java.logging,java.management,java.naming,java.prefs,java.sql,java.xml,jdk.unsupported

[ -d "$WIN_JDK/jmods" ] || { echo "Windows JDK missing in tools/jdk-win"; exit 1; }
[ -d "$WIN32_JDK/jmods" ] || { echo "32-bit Windows JDK (BellSoft Liberica) missing in tools/jdk-win32"; exit 1; }

"$DIR/build.sh"
rm -rf "$OUT" "$DIR/submission/MediStore-Portable.zip"
mkdir -p "$OUT/app" "$OUT/lib/native/x64" "$OUT/lib/native/x86" "$OUT/data"

# 1. the program itself
"$MAC_JDK/bin/jar" --create --file "$OUT/app/medistore.jar" --main-class medistore.Main -C "$DIR/build/classes" .

# 2. libraries
cp "$DIR/tools/sqlite-jdbc.jar" "$DIR/tools/flatlaf.jar" "$DIR/tools/slf4j-api.jar" "$DIR/tools/slf4j-nop.jar" "$OUT/lib/"
# SQLite's native DLLs, shipped ready to load: otherwise the driver unpacks them into
# %TEMP% at start-up, which locked-down lab PCs and antivirus tools often block.
unzip -q -j "$DIR/tools/sqlite-jdbc.jar" "org/sqlite/native/Windows/x86_64/sqlitejdbc.dll" -d "$OUT/lib/native/x64"
unzip -q -j "$DIR/tools/sqlite-jdbc.jar" "org/sqlite/native/Windows/x86/sqlitejdbc.dll" -d "$OUT/lib/native/x86"

# 3. Windows Java runtimes, cut down to the modules the program uses:
#    jre = 64-bit, jre32 = 32-bit (the launchers pick one automatically)
for pair in "$WIN_JDK:jre" "$WIN32_JDK:jre32"; do
    "$MAC_JDK/bin/jlink" --module-path "${pair%%:*}/jmods" --add-modules "$MODULES" \
        --strip-debug --no-header-files --no-man-pages --compress zip-6 \
        --output "$OUT/${pair##*:}"
done

# 4. demonstration data: a freshly seeded database
SEED_DB="$DIR/data/medistore.db"
[ -f "$SEED_DB" ] && cp "$SEED_DB" "$OUT/data/medistore.db"

# 5. launchers (Windows line endings, so cmd.exe reads them properly)
# Picks the 32-bit runtime only on 32-bit Windows. %~dp0 stays outside the ( ) block
# because a folder name containing ")" would end the block early.
PICK_RUNTIME='cd /d "%~dp0"
set "JRE=jre"
set "NATIVE=x64"
if /i "%PROCESSOR_ARCHITECTURE%"=="x86" if not defined PROCESSOR_ARCHITEW6432 (
    set "JRE=jre32"
    set "NATIVE=x86"
)
set "SQLITE=-Dorg.sqlite.lib.path=%~dp0lib\native\%NATIVE%"'

cat > "$OUT/MediStore.bat" <<BAT
@echo off
rem Starts the Medical Store Management System. No Java installation is needed.
$PICK_RUNTIME
start "" "%JRE%\bin\javaw.exe" "%SQLITE%" -cp "app\medistore.jar;lib\*" medistore.Main
BAT

cat > "$OUT/Reset-Demo-Data.bat" <<BAT
@echo off
rem Replaces the database with fresh demonstration data. All entered data is lost.
$PICK_RUNTIME
echo This will DELETE all current data and load the demonstration data.
choice /M "Continue"
if errorlevel 2 exit /b
"%JRE%\bin\java.exe" "%SQLITE%" -cp "app\medistore.jar;lib\*" medistore.tools.SampleData
echo.
pause
BAT

cat > "$OUT/MediStore-Debug.bat" <<BAT
@echo off
rem Same as MediStore.bat, but this window stays open and shows any error message.
rem Use it when MediStore.bat seems to do nothing.
$PICK_RUNTIME
echo Folder  : %CD%
echo Windows : %PROCESSOR_ARCHITECTURE%   (using %JRE%)
echo.
echo Java runtime inside this folder:
"%JRE%\bin\java.exe" -version
echo.
echo Starting MediStore ... (close the application window to come back here)
"%JRE%\bin\java.exe" "%SQLITE%" -cp "app\medistore.jar;lib\*" medistore.Main
echo.
echo MediStore closed. Exit code: %errorlevel%
echo If you see an error above, take a photo of this window.
pause
BAT

cat > "$OUT/READ-ME-FIRST.txt" <<'TXT'
MEDICAL STORE MANAGEMENT SYSTEM  (MediStore)

HOW TO RUN
  1. Extract the zip first (right-click > Extract All). Do not run it from inside the zip.
  2. Keep the folder on the Desktop or in C:\MediStore - not inside C:\Program Files.
  3. Double-click  MediStore.bat
  4. Sign in with   user name: admin     password: admin123

Nothing needs to be installed. The Java runtime is inside this folder, for both
64-bit Windows ("jre") and 32-bit Windows ("jre32"); MediStore.bat picks the right one.

FILES
  MediStore.bat          starts the application
  MediStore-Debug.bat    same, but shows any error (use it if MediStore.bat does nothing)
  Reset-Demo-Data.bat    reloads the demonstration data (deletes current data)
  data\medistore.db      the database (copy this file to back up everything)

If Windows shows a "protected your PC" notice, choose "More info" and then "Run anyway".
TXT
for f in MediStore.bat MediStore-Debug.bat Reset-Demo-Data.bat READ-ME-FIRST.txt; do sed -i '' 's/$/\r/' "$OUT/$f"; done

# 6. zip it
(cd "$DIR/submission" && zip -qr MediStore-Portable.zip MediStore-Portable -x "*.DS_Store")

echo "Bundle : $OUT"
echo "Zip    : $DIR/submission/MediStore-Portable.zip  ($(du -h "$DIR/submission/MediStore-Portable.zip" | cut -f1))"
