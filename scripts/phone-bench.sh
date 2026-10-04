#!/usr/bin/env bash
# Run Laya on a USB-connected Android phone and score it with the same tests as the laptop.
# Run with: bash scripts/phone-bench.sh [threads]   (default 4 = the big cores on most phones)
# Needs: adb, the Android llama-server build (see docs/PLAN.md Phase 1), models/Laya-Q4_0.gguf.
# Everything goes to /data/local/tmp/emojet on the phone (no app installed; remove with
#   adb shell rm -r /data/local/tmp/emojet).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
THREADS="${1:-4}"
NDK="${NDK:-$HOME/Android/Sdk/ndk/30.0.16248370}"
BIN="${BIN:-$HOME/build/llama.cpp/build-android/bin/llama-server}"
MODEL="${MODEL:-$ROOT/models/Laya-Q4_0.gguf}"
DIR=/data/local/tmp/emojet

adb get-state >/dev/null
echo "phone: $(adb shell getprop ro.product.marketname | tr -d '\r'), Android $(adb shell getprop ro.build.version.release | tr -d '\r'), SoC $(adb shell getprop ro.soc.model | tr -d '\r')"

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
trap 'adb shell "kill \$(pidof llama-server)" 2>/dev/null; wait $SERVER 2>/dev/null' EXIT

for _ in $(seq 1 60); do curl -s 127.0.0.1:8085/health | grep -q ok && break; sleep 0.5; done
echo "threads: $THREADS, real memory: $(adb shell "grep RssAnon /proc/\$(pidof llama-server)/status" | tr -s ' ' | cut -d' ' -f2-)"
python3 "$ROOT/scripts/test-prompts.py" --url http://127.0.0.1:8085/v1/systemone
