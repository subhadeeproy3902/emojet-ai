#!/usr/bin/env bash
# Build the EmoJet APK. Run with: bash scripts/build-android.sh [debug|release]
# Needs: JDK 21, ~/Android/Sdk (+ NDK r30), llama.cpp source in ~/build/llama.cpp.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TYPE="${1:-debug}"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
NDK="${NDK:-$SDK/ndk/30.0.16248370}"
LLAMA="${LLAMA:-$HOME/build/llama.cpp}"
MODEL_REPO="${MODEL_REPO:-$ROOT/../emojet}"   # the emojet model repo (engine patch lives there)
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"

# 1. Laya engine for arm64 phones (pinned llama.cpp + EmoJet patches, KleidiAI).
bash "$MODEL_REPO/scripts/build-engine.sh" android
JNI="$ROOT/android/app/src/main/jniLibs/arm64-v8a"
mkdir -p "$JNI"
"$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" -o "$JNI/libllama_server.so" "$LLAMA/build-android/bin/llama-server"

# 2. Gradle setup (local.properties is per machine, git-ignored).
echo "sdk.dir=$SDK" > "$ROOT/android/local.properties"
cd "$ROOT/android"
if [[ ! -f gradlew ]]; then
  "$HOME/build/gradle-9.8.0/bin/gradle" wrapper --gradle-version 9.8.0 --distribution-type bin
fi

# 3. Build (gradlew via bash: E: is mounted without exec permission).
bash gradlew --no-daemon clean "assemble${TYPE^}"   # clean: incremental packaging leaves slack in the APK
ls -lh app/build/outputs/apk/"$TYPE"/*.apk
