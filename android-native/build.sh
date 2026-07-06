#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/android-native"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/c/Users/wayne/AppData/Local/Android/Sdk}}"
if command -v cygpath >/dev/null 2>&1; then
  SDK="$(cygpath -u "$SDK" 2>/dev/null || printf '%s' "$SDK")"
fi
if [ ! -d "$SDK" ]; then echo "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT." >&2; exit 1; fi
PLATFORM="$(ls -d "$SDK"/platforms/android-* | sort -V | tail -1)"
BUILD_TOOLS="$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)"
AAPT2="$BUILD_TOOLS/aapt2"; [ -x "$AAPT2.exe" ] && AAPT2="$AAPT2.exe"
D8="$BUILD_TOOLS/d8"; [ -f "$D8.bat" ] && D8="$D8.bat"
ZIPALIGN="$BUILD_TOOLS/zipalign"; [ -x "$ZIPALIGN.exe" ] && ZIPALIGN="$ZIPALIGN.exe"
APKSIGNER="$BUILD_TOOLS/apksigner"; [ -f "$APKSIGNER.bat" ] && APKSIGNER="$APKSIGNER.bat"
ANDROID_JAR="$PLATFORM/android.jar"
OUT="$APP/build"
DIST="$APP/dist"
mkdir -p "$OUT/res" "$OUT/classes" "$OUT/dex" "$DIST"
rm -rf "$OUT/res"/* "$OUT/classes"/* "$OUT/dex"/*
"$AAPT2" compile --dir "$APP/src/main/res" -o "$OUT/res.zip"
"$AAPT2" link -o "$OUT/unsigned.apk" -I "$ANDROID_JAR" --manifest "$APP/src/main/AndroidManifest.xml" --java "$OUT" --min-sdk-version 23 --target-sdk-version 36 --version-code 1 --version-name 0.1.0 "$OUT/res.zip"
R_DIR="$OUT/com/townsendprecision/jcloisterzoneandroid"
SRC_FILES=$(find "$APP/src/main/java" "$R_DIR" -name '*.java' -print | tr '\n' ' ')
javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$ANDROID_JAR" -d "$OUT/classes" $SRC_FILES
"$D8" --min-api 23 --lib "$ANDROID_JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class' -print)
JAR_TOOL="$(command -v jar || true)"
if [ -z "$JAR_TOOL" ] && [ -n "${JAVA_HOME:-}" ]; then
  JAR_TOOL="$JAVA_HOME/bin/jar"
  [ -x "$JAR_TOOL.exe" ] && JAR_TOOL="$JAR_TOOL.exe"
fi
if [ -z "$JAR_TOOL" ]; then
  for candidate in "/c/Program Files/Common Files/Oracle/Java/javapath/jar.exe" /c/Program\ Files/Java/*/bin/jar.exe /c/Program\ Files/Eclipse\ Adoptium/*/bin/jar.exe; do
    if [ -f "$candidate" ]; then JAR_TOOL="$candidate"; break; fi
  done
fi
if [ -z "$JAR_TOOL" ]; then echo "jar tool not found" >&2; exit 1; fi
(cd "$OUT/dex" && "$JAR_TOOL" uf "$OUT/unsigned.apk" classes.dex)
KEYTOOL="$(command -v keytool || true)"
if [ -z "$KEYTOOL" ]; then
  for candidate in "/c/Program Files/Common Files/Oracle/Java/javapath/keytool.exe" /c/Program\ Files/Java/*/bin/keytool.exe /c/Program\ Files/Eclipse\ Adoptium/*/bin/keytool.exe; do
    if [ -f "$candidate" ]; then KEYTOOL="$candidate"; break; fi
  done
fi
if [ -z "$KEYTOOL" ]; then echo "keytool not found" >&2; exit 1; fi
if [ ! -f "$APP/debug.keystore" ]; then "$KEYTOOL" -genkeypair -v -keystore "$APP/debug.keystore" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null; fi
"$ZIPALIGN" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
"$APKSIGNER" sign --ks "$APP/debug.keystore" --ks-pass pass:android --key-pass pass:android --out "$DIST/JCloisterZone-Android-v0.1.0-debug.apk" "$OUT/aligned.apk"
"$APKSIGNER" verify --verbose "$DIST/JCloisterZone-Android-v0.1.0-debug.apk"
"$AAPT2" dump badging "$DIST/JCloisterZone-Android-v0.1.0-debug.apk" | sed -n '1,14p'
printf '\nBuilt %s\n' "$DIST/JCloisterZone-Android-v0.1.0-debug.apk"
