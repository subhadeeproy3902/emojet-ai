# EmoJet — context for Claude Code

Free, offline emoji suggestions (Android overlay APK + browser extension) powered only by **Laya**. Owner: Subhadeep Roy (GitHub subhadeeproy3902), a Linux beginner — explain in plain words.

Start every session by reading `docs/PLAN.md` (phases, status, decisions) and `docs/MANUAL-WORK.md` (what the owner still has to do).

## Rules

- **Commit and push after every meaningful change** (owner's standing instruction). End commit messages with the Co-Authored-By Claude trailer. Never commit models, keys, tokens or build outputs (`.gitignore`).
- **Laya only** as AI at runtime. **$0**: no paid services, no Play Store, no Chrome Web Store. Distribution = GitHub Releases (APK, extension zip, model).
- Suggestions come from the **whole sentence** (+ replied-to message), never single keywords. Never stop suggesting because an emoji was just typed.
- Target all Unicode emojis (`shared/emoji-catalog.json`) and tone of voice (`shared/tones.json`) via the fine-tuned **Laya-Emoji** model (`docs/TRAINING.md`).
- Updates happen **inside** the app/extension, never uninstall + reinstall (`docs/WORKFLOWS.md` §4).
- Any Android 8+ phone; owner tests on a Redmi Note 17 Pro (HyperOS: "Install via USB" needed for adb installs).
- sudo needs the owner's password: show the exact command, the owner runs it with `!`.
- **The owner's phone is brand new — be careful with it.** Never root, unlock the bootloader, flash, factory-reset, or sign out of accounts. No `adb shell settings put`, no `pm uninstall`/`disable` of anything that isn't ours, no system tweaks. Only: read info (`getprop`, `dumpsys battery`), copy our own files to `/data/local/tmp/emojet` (deleted after each test), install/uninstall our own EmoJet APK. Keep load tests short; check battery temperature before (skip above 40 °C) and after. Say what will happen on the phone before doing it.
- **Secrets:** the Kaggle token is in `.env` (`KAGGLE_API_TOKEN`, git-ignored). Never print it, never commit it, never send it anywhere except Kaggle's API.

## Machine facts

- Repo lives on E: (`/mnt/shared/EndeavourOS/emojet`, NTFS, **no exec bit**) → run scripts with `bash`/`python3`; binaries and SDKs live in `/home`.
- llama.cpp built from source: `~/build/llama.cpp/build/bin/` (Laya `/v1/systemone` support, PR #29818). Android SDK + NDK r30: `~/Android/Sdk`. JDK 21 + adb installed.
- Android engine build: `~/build/llama.cpp/build-android` (NDK r30, arm64-v8a, KleidiAI). Phone test: `bash scripts/phone-bench.sh` (owner's REDMI Note 17 Pro 5G, adb serial 4b8ff91f; 4 threads best, ~306–356 ms).
- Models in `models/` (gitignored): `bash scripts/get-model.sh`, `bash scripts/quantize.sh Q4_0`.
- Dev server: `bash scripts/dev-server.sh &` → `python3 scripts/test-prompts.py` (moods) / `python3 scripts/test-tones.py` (tone). Stop with `pkill -x llama-server`.
- Machine changes are also logged in `~/CHANGELOG.md` (the owner's system log).
