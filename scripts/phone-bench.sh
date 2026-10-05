#!/usr/bin/env bash
# Run Laya on a USB-connected Android phone and score it with the same tests as the laptop.
# Run with: bash scripts/phone-bench.sh [threads]   (default 4 = the big cores on most phones)
# Needs: adb, the Android llama-server build (see docs/PLAN.md Phase 1), models/Laya-Q4_0.gguf.
# Everything goes to /data/local/tmp/emojet on the phone (no app installed) and is deleted
# again at the end (KEEP=1 keeps it). Refuses to start if the battery is above 40 °C.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODELS="${MODELS:-$ROOT/../emojet/models}"   # model files live in the emojet model repo (git-ignored)
THREADS="${1:-4}"
NDK="${NDK:-$HOME/Android/Sdk/ndk/30.0.16248370}"
BIN="${BIN:-$HOME/build/llama.cpp/build-android/bin/llama-server}"
MODEL="${MODEL:-$MODELS/Laya-Q4_0.gguf}"
DIR=/data/local/tmp/emojet

adb get-state >/dev/null
echo "phone: $(adb shell getprop ro.product.marketname | tr -d '\r'), Android $(adb shell getprop ro.build.version.release | tr -d '\r'), SoC $(adb shell getprop ro.soc.model | tr -d '\r')"

# Phone safety: owner's phone is new. Don't start a CPU-heavy test on a warm phone.
temp() { adb shell dumpsys battery | awk '/temperature:/ {printf "%.1f", $2/10}'; }
T0="$(temp)"
echo "battery temperature before: ${T0} °C"
if awk "BEGIN {exit !($T0 > 40.0)}"; then
  echo "Phone is warmer than 40 °C — not starting. Let it cool down first." >&2
  exit 1
fi

adb shell mkdir -p "$DIR"
STRIPPED="$(mktemp)"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" -o "$STRIPPED" "$BIN"
adb push "$STRIPPED" "$DIR/llama-server" >/dev/null && rm -f "$STRIPPED"
adb shell chmod 755 "$DIR/llama-server"
if [[ "$(adb shell stat -c %s "$DIR/$(basename "$MODEL")" 2>/dev/null | tr -d '\r')" != "$(stat -c %s "$MODEL")" ]]; then
  adb push "$MODEL" "$DIR/" >/dev/null
fi

adb forward tcp:8085 tcp:8080 >/dev/null
adb shell "cd $DIR && exec ./llama-server -m $(basename "$MODEL") --host 127.0.0.1 --port 8080 -t $THREADS -c 512 -b 512 -ub 512 -np 1 --cache-ram 0 --no-webui" >/dev/null 2>&1 &
SERVER=$!
cleanup() {
  adb shell "kill \$(pidof llama-server)" 2>/dev/null || true
  wait "$SERVER" 2>/dev/null || true
  adb forward --remove tcp:8085 2>/dev/null || true
  echo "battery temperature after: $(temp) °C"
  if [[ "${KEEP:-0}" != 1 ]]; then
    adb shell rm -r "$DIR" && echo "test files removed from the phone"
  fi
}
trap cleanup EXIT

for _ in $(seq 1 60); do curl -s 127.0.0.1:8085/health | grep -q ok && break; sleep 0.5; done
echo "threads: $THREADS, real memory: $(adb shell "grep RssAnon /proc/\$(pidof llama-server)/status" | tr -s ' ' | cut -d' ' -f2-)"
python3 "$ROOT/../emojet/scripts/test-prompts.py" --url http://127.0.0.1:8085/v1/systemone
