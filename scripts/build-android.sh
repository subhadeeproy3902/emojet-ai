#!/usr/bin/env bash
# Build the EmoJet APK. Run with: bash scripts/build-android.sh [debug|release]
# Needs: JDK 21, ~/Android/Sdk (+ NDK r30), llama.cpp source in ~/build/llama.cpp.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TYPE="${1:-debug}"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
NDK="${NDK:-$SDK/ndk/30.0.16248370}"
LLAMA="${LLAMA:-$HOME/build/llama.cpp}"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk}"

# 1. Laya engine for arm64 phones (llama-server + KleidiAI), built once.
if [[ ! -x "$LLAMA/build-android/bin/llama-server" ]]; then
  cmake -S "$LLAMA" -B "$LLAMA/build-android" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 \
    -DGGML_CPU_KLEIDIAI=ON -DGGML_NATIVE=OFF -DGGML_OPENMP=OFF -DGGML_LLAMAFILE=OFF -DLLAMA_OPENSSL=OFF \
    -DLLAMA_BUILD_TESTS=OFF -DLLAMA_BUILD_EXAMPLES=OFF -DBUILD_SHARED_LIBS=OFF
  cmake --build "$LLAMA/build-android" -j "$(nproc)" --target llama-server
fi
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
bash gradlew --no-daemon "assemble${TYPE^}"
ls -lh app/build/outputs/apk/"$TYPE"/*.apk
