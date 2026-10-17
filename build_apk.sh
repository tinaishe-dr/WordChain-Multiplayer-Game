#!/usr/bin/env bash
# Optional command-line build (no Gradle) using the Android SDK build-tools.
# Usage: ANDROID_JAR=/path/to/android.jar BUILD_TOOLS=/path/to/build-tools/<ver> ./build_apk.sh
set -euo pipefail
ANDROID_JAR=${ANDROID_JAR:?set ANDROID_JAR to an android.jar (API 26+ recommended)}
BT=${BUILD_TOOLS:?set BUILD_TOOLS to your SDK build-tools directory}
rm -rf build && mkdir -p build/classes
"$BT/aapt" package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -A app/src/main/assets \
  -I "$ANDROID_JAR" --min-sdk-version 26 --target-sdk-version 34 -F build/unsigned.apk
javac --release 8 -Xlint:-options -cp "$ANDROID_JAR" -d build/classes $(find app/src/main/java -name '*.java')
"$BT/dx" --dex --min-sdk-version=26 --output=build/classes.dex build/classes
(cd build && "$BT/aapt" add unsigned.apk classes.dex >/dev/null)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
mkdir -p keystore
[ -f keystore/debug.keystore ] || keytool -genkeypair -keystore keystore/debug.keystore -storepass android -keypass android \
  -alias debug -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=WordChain Debug"
"$BT/apksigner" sign --ks keystore/debug.keystore --ks-pass pass:android --key-pass pass:android --out build/WordChain.apk build/aligned.apk
echo "Built build/WordChain.apk"
