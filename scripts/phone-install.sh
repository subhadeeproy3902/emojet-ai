#!/usr/bin/env bash
# Install / update EmoJet on the USB-connected phone and copy the model(s) over the cable.
# Run with: bash scripts/phone-install.sh [model.gguf ...]   (default: every models/Laya*.gguf the app knows)
# Phone safety (owner's phone is new): only installs our own app and writes to the app's own folder.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODELS="${MODELS:-$ROOT/../emojet/models}"   # model files live in the emojet model repo (git-ignored)
PKG=io.github.subhadeeproy3902.emojet
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
DEST="/sdcard/Android/data/$PKG/files"

adb get-state >/dev/null
temp=$(adb shell dumpsys battery | awk '/temperature:/ {printf "%.1f", $2/10}')
echo "phone: $(adb shell getprop ro.product.marketname | tr -d '\r'), battery $temp °C"
awk "BEGIN {exit !($temp > 40.0)}" && { echo "Phone warmer than 40 °C, not continuing." >&2; exit 1; }

[[ -f "$APK" ]] || bash "$ROOT/scripts/build-android.sh" debug
echo "installing $(basename "$APK") ($(du -h "$APK" | cut -f1))"
adb install -r "$APK" | tail -1

# The app folder exists once the app has run; create it the same way Android does.
adb shell mkdir -p "$DEST"
files=("$@")
if [[ ${#files[@]} -eq 0 ]]; then
  for f in "$MODELS"/Laya-Emoji-Q4_0.gguf "$MODELS"/Laya-Q4_0.gguf; do [[ -f "$f" ]] && files+=("$f"); done
fi
for f in "${files[@]}"; do
  name=$(basename "$f")
  if [[ "$(adb shell stat -c %s "$DEST/$name" 2>/dev/null | tr -d '\r')" == "$(stat -c %s "$f")" ]]; then
    echo "model already on the phone: $name"
  else
    echo "copying $name ($(du -h "$f" | cut -f1)) over USB…"
    adb push "$f" "$DEST/$name" | tail -1
  fi
done
adb shell am start -n "$PKG/.MainActivity" >/dev/null && echo "EmoJet opened on the phone."
