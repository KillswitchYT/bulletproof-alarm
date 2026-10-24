#!/data/data/com.termux/files/usr/bin/bash
# Downloads the Android API 34 android.jar used to compile the app.
mkdir -p "$HOME/.android"
curl -L --fail -o "$HOME/.android/android.jar" \
  https://raw.githubusercontent.com/Reginer/aosp-android-jar/main/android-34/android.jar \
  && echo "android.jar saved to ~/.android/android.jar"
