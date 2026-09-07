#!/usr/bin/env bash
# Construye y firma el APK sin Android Studio ni el SDK completo.
#
# Herramientas necesarias (carpeta $IMPOSTOR_TOOLS, por defecto ./tools):
#   aapt2              binario de aapt2 (p. ej. el que incluye Apktool en prebuilt/linux/aapt2)
#   android-34.jar     android.jar de la plataforma 34
#   r8lib.jar          R8/D8 (https://storage.googleapis.com/r8-releases/raw/<version>/r8lib.jar)
#   apksig-2.3.0.jar   librería apksig (Maven Central: com.android.tools.build:apksig)
# Además: JDK 11+ y python3.
#
# Uso: scripts/build-apk.sh [salida.apk]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${IMPOSTOR_TOOLS:-$ROOT/tools}"
AAPT2="$TOOLS/aapt2"
ANDROID_JAR="$TOOLS/android-34.jar"
R8="$TOOLS/r8lib.jar"
APKSIG="$TOOLS/apksig-2.3.0.jar"

KEYSTORE="${KEYSTORE:-$ROOT/keystore/impostor.jks}"
KEY_ALIAS="${KEY_ALIAS:-impostor}"
STORE_PASS="${STORE_PASS:-impostor123}"
KEY_PASS="${KEY_PASS:-$STORE_PASS}"

# apksig 2.3.0 necesita un JDK <= 17 para la firma v1 (usa APIs internas que cambiaron en JDK 21).
if [ -z "${SIGN_JAVA:-}" ]; then
  for cand in /usr/lib/jvm/java-17-openjdk-amd64/bin/java /usr/lib/jvm/java-11-openjdk-amd64/bin/java; do
    [ -x "$cand" ] && { SIGN_JAVA="$cand"; break; }
  done
  SIGN_JAVA="${SIGN_JAVA:-java}"
fi

OUT="${1:-$ROOT/release/impostor.apk}"
SRC="$ROOT/app/src/main"
BUILD="$ROOT/build"

for f in "$AAPT2" "$ANDROID_JAR" "$R8" "$APKSIG" "$KEYSTORE"; do
  [ -f "$f" ] || { echo "Falta: $f" >&2; exit 1; }
done

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$BUILD/signer" "$(dirname "$OUT")"

echo "==> Iconos"
python3 "$ROOT/scripts/gen_icons.py" "$SRC/res" > /dev/null

echo "==> aapt2 compile"
"$AAPT2" compile --dir "$SRC/res" -o "$BUILD/res.zip"

echo "==> aapt2 link"
# El manifest no lleva package= (AGP 8 lo toma del namespace); aapt2 sí lo necesita.
sed 's#<manifest #<manifest package="com.impostor.juego" #' "$SRC/AndroidManifest.xml" > "$BUILD/AndroidManifest.xml"
"$AAPT2" link -o "$BUILD/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$BUILD/AndroidManifest.xml" \
  -R "$BUILD/res.zip" \
  --java "$BUILD/gen" \
  --auto-add-overlay

echo "==> javac"
find "$SRC/java" "$BUILD/gen" -name '*.java' > "$BUILD/sources.txt"
javac -source 8 -target 8 -Xlint:-options -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "==> d8"
find "$BUILD/classes" -name '*.class' > "$BUILD/classes.txt"
java -cp "$R8" com.android.tools.r8.D8 --release --min-api 21 \
  --lib "$ANDROID_JAR" --output "$BUILD/dex" @"$BUILD/classes.txt"

echo "==> empaquetar + alinear"
python3 "$ROOT/scripts/package.py" "$BUILD/base.apk" "$BUILD/dex/classes.dex" "$BUILD/unsigned.apk"

echo "==> firmar"
javac --release 11 -cp "$APKSIG" -d "$BUILD/signer" "$ROOT/scripts/SignApk.java"
"$SIGN_JAVA" --add-exports java.base/sun.security.x509=ALL-UNNAMED \
  --add-exports java.base/sun.security.pkcs=ALL-UNNAMED \
  --add-exports java.base/sun.security.util=ALL-UNNAMED \
  -cp "$APKSIG:$BUILD/signer" SignApk "$BUILD/unsigned.apk" "$OUT" \
  "$KEYSTORE" "$STORE_PASS" "$KEY_ALIAS" "$KEY_PASS"

echo "==> listo: $OUT ($(du -h "$OUT" | cut -f1))"
