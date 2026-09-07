#!/usr/bin/env bash
# Prueba la lógica del juego (palabras y estado de partida) en la JVM, sin emulador.
# Necesita tools/android-34.jar y tools/json.jar (Maven Central: org.json:json:20240303).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${IMPOSTOR_TOOLS:-$ROOT/tools}"
J="$ROOT/app/src/main/java/com/impostor/juego"
OUT="$ROOT/build/test"
rm -rf "$OUT" && mkdir -p "$OUT"
javac -source 8 -target 8 -Xlint:-options -encoding UTF-8 -bootclasspath "$TOOLS/android-34.jar" -d "$OUT" \
  "$J/WordRepository.java" "$J/WordPick.java" "$J/GameState.java"
javac --release 11 -cp "$TOOLS/json.jar:$TOOLS/android-34.jar:$OUT" -d "$OUT" "$ROOT/scripts/LogicTest.java"
java -cp "$TOOLS/json.jar:$OUT:$TOOLS/android-34.jar" LogicTest
