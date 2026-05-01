#!/bin/bash
# Build script for Island Android app using SDK command-line tools
# This bypasses Gradle entirely, using only locally available tools.

set -e

PROJ_ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJ_ROOT/build_output"
ANDROID_SDK="${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}"
BUILD_TOOLS="$ANDROID_SDK/build-tools/34.0.0"
PLATFORM="$ANDROID_SDK/platforms/android-34"
ANDROID_JAR="$PLATFORM/android.jar"

AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
APKSIGNER="$BUILD_TOOLS/apksigner"
ZIPALIGN="$BUILD_TOOLS/zipalign"

APP_DIR="$PROJ_ROOT/app/src/main"
MANIFEST="$APP_DIR/AndroidManifest.xml"
RES_DIR="$APP_DIR/res"
SRC_DIR="$APP_DIR/java"

# Clean and create build directories
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/compiled_res"
mkdir -p "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/dex"
mkdir -p "$BUILD_DIR/apk_contents"

echo "=== Step 1: Compile resources with aapt2 ==="
find "$RES_DIR" -name "*.xml" -o -name "*.png" -o -name "*.jpg" | while read -r f; do
    "$AAPT2" compile "$f" --dir "$RES_DIR" -o "$BUILD_DIR/compiled_res/" 2>/dev/null || true
done
"$AAPT2" compile --dir "$RES_DIR" -o "$BUILD_DIR/compiled_res/"

echo "=== Step 2: Link resources ==="
"$AAPT2" link \
    --proto-format \
    -o "$BUILD_DIR/resources.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$MANIFEST" \
    --java "$BUILD_DIR/classes" \
    --min-sdk-version 31 \
    --target-sdk-version 34 \
    --version-code 1 \
    --version-name "1.0" \
    "$BUILD_DIR/compiled_res/"*.flat 2>&1 || {
    # Try without --proto-format
    "$AAPT2" link \
        -o "$BUILD_DIR/resources.apk" \
        -I "$ANDROID_JAR" \
        --manifest "$MANIFEST" \
        --java "$BUILD_DIR/classes" \
        --min-sdk-version 31 \
        --target-sdk-version 34 \
        --version-code 1 \
        --version-name "1.0" \
        "$BUILD_DIR/compiled_res/"*.flat
}

echo "=== Step 3: Compile Kotlin sources ==="
find "$SRC_DIR" -name "*.kt" > "$BUILD_DIR/sources.txt"
kotlinc \
    @"$BUILD_DIR/sources.txt" \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    -jvm-target 1.8 \
    -api-version 1.8 \
    2>&1

echo "=== Step 4: Convert to DEX ==="
find "$BUILD_DIR/classes" -name "*.class" > "$BUILD_DIR/class_files.txt"
"$D8" \
    --classpath "$ANDROID_JAR" \
    --min-api 31 \
    --output "$BUILD_DIR/dex" \
    @"$BUILD_DIR/class_files.txt" 2>&1 || \
"$D8" \
    --classpath "$ANDROID_JAR" \
    --min-api 31 \
    --output "$BUILD_DIR/dex" \
    $(cat "$BUILD_DIR/class_files.txt" | tr '\n' ' ')

echo "=== Step 5: Package APK ==="
cp "$BUILD_DIR/resources.apk" "$BUILD_DIR/island-unsigned.apk"
# Add classes.dex to the APK
cd "$BUILD_DIR/dex"
zip -u "$BUILD_DIR/island-unsigned.apk" classes*.dex 2>&1 || true

echo "=== Step 6: Create debug keystore ==="
if [ ! -f "$BUILD_DIR/debug.keystore" ]; then
    keytool -genkey -v \
        -keystore "$BUILD_DIR/debug.keystore" \
        -alias androiddebugkey \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass android \
        -keypass android \
        -dname "CN=Android Debug,O=Android,C=US" 2>&1
fi

echo "=== Step 7: Align APK ==="
"$ZIPALIGN" -f 4 "$BUILD_DIR/island-unsigned.apk" "$BUILD_DIR/island-aligned.apk"

echo "=== Step 8: Sign APK ==="
"$APKSIGNER" sign \
    --ks "$BUILD_DIR/debug.keystore" \
    --ks-pass pass:android \
    --key-pass pass:android \
    --ks-key-alias androiddebugkey \
    --out "$BUILD_DIR/island-debug.apk" \
    "$BUILD_DIR/island-aligned.apk"

echo ""
echo "=== BUILD SUCCESS ==="
echo "APK: $BUILD_DIR/island-debug.apk"
ls -la "$BUILD_DIR/island-debug.apk"
