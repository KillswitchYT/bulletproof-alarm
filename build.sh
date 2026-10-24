#!/usr/bin/env bash
# Builds BulletproofAlarm.apk without Gradle. Works in Termux and on Linux (GitHub Actions).
# Termux needs: pkg install openjdk-17 aapt2 d8 apksigner zipalign zip unzip curl
set -e
cd "$(dirname "$0")"
JAR="${ANDROID_JAR:-$HOME/.android/android.jar}"
VC="${VERSION_CODE:-5}"
VN="${VERSION_NAME:-2.1.0}"
OUT=BulletproofAlarm.apk
KS_GIVEN="${KS:-}"
KS="${KS:-$HOME/.android/debug.keystore}"
KS_PASS="${KS_PASS:-android}"
KEY_ALIAS="${KEY_ALIAS:-androiddebugkey}"

[ -f "$JAR" ] || { echo "No android.jar found. Run: bash get-jar.sh (or set ANDROID_JAR)"; exit 1; }
unzip -l "$JAR" | grep -q 'android/credentials/CredentialManager.class' || { echo "android.jar is older than API 34. Run: bash get-jar.sh"; exit 1; }
if [ ! -f "$KS" ]; then
  [ -n "$KS_GIVEN" ] && { echo "Keystore $KS not found"; exit 1; }
  mkdir -p "$(dirname "$KS")"
  keytool -genkeypair -keystore "$KS" -storepass "$KS_PASS" -keypass "$KS_PASS" -alias "$KEY_ALIAS" \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

rm -rf build; mkdir -p build/gen build/obj build/dex
echo "[1/6] compile resources"; aapt2 compile --dir res -o build/res.zip
echo "[2/6] link manifest, assets, R.java"
aapt2 link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml -A assets --java build/gen \
  --min-sdk-version 26 --target-sdk-version 34 --version-code "$VC" --version-name "$VN" build/res.zip
echo "[3/6] compile java"
javac -nowarn -source 8 -target 8 -bootclasspath "$JAR" -d build/obj $(find src build/gen -name '*.java')
echo "[4/6] dex"
d8 --release --min-api 26 --lib "$JAR" --output build/dex $(find build/obj -name '*.class')
echo "[5/6] package and align"
cp build/base.apk build/unaligned.apk
(cd build/dex && zip -q ../unaligned.apk classes.dex)
zipalign -f -p 4 build/unaligned.apk build/aligned.apk
echo "[6/6] sign"
apksigner sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" --ks-key-alias "$KEY_ALIAS" --out "$OUT" build/aligned.apk
echo "Done: $PWD/$OUT (version $VN, code $VC)"
echo "Install: termux-open $OUT   (allow Termux to install unknown apps if asked)"
