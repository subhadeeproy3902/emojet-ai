# EmoJet — Plan

**Goal:** free, fully on-device emoji suggestions while you type — an overlay on Android (WhatsApp, Instagram, SMS, any app) and a browser extension (WhatsApp Web, Instagram, Snapchat, any site). The only AI is **Laya**. Distributed as an APK and an unpacked extension zip on GitHub Releases. **$0** to build, host and run.

Related docs: [ARCHITECTURE](ARCHITECTURE.md) · [TRAINING](TRAINING.md) · [MANUAL-WORK](MANUAL-WORK.md) (your checklist) · [TESTING](TESTING.md) · [HUGGINGFACE](HUGGINGFACE.md) · [RESEARCH](RESEARCH.md)

## Decisions log

| Date | Decision |
|---|---|
| 2026-10-04 | Laya only; $0; APK + unpacked extension via GitHub Releases, no stores |
| 2026-10-04 | Mood labels instead of direct emoji choice (192-token budget); Q4_0 model file |
| 2026-10-04 | No keyword layer — whole sentence only |
| 2026-10-04 | Target all Unicode emojis + tone of voice via fine-tuned Laya-Emoji |
| 2026-10-05 | **Training on Kaggle** (free GPU); laptop for data prep, conversion and tests |
| 2026-10-05 | **Small APK (~15 MB) + model downloaded on first launch** (Option A) |
| 2026-10-05 | **Updates from inside the app / extension** — app, model and data; never uninstall + reinstall ([WORKFLOWS.md](WORKFLOWS.md) §4) |

## Design rules (owner's requirements)

1. **Whole sentence, not single words.** The model always reads the full draft (+ the message being replied to). No suggestion is decided by one keyword.
2. **All emojis.** Target: every emoji in Unicode 18.0 — 3,963 = 1,923 distinct emojis + skin-tone variants (applied from the user's preference). Phase 0's 25 moods are the stepping stone; Laya-Emoji (Phase T) is the goal.
3. **Real time.** Suggestions within a typing pause; never blocks typing.
4. **Laya only, $0.** No other AI model at runtime, no paid service, no store.
5. **Any Android phone.** APK for `arm64-v8a` (all modern phones) + `x86_64` (emulators, Chromebooks), Android 8.0+ (`minSdk 26`). Owner's test phone: Redmi Note 17 Pro.
6. **Multi-emoji friendly, reply-aware, privacy-first** (see ARCHITECTURE).
7. **Tone-aware.** The model also reads *how* something is said: 4 communication styles, 46 tones, 4 tone dimensions (`shared/tones.json`), and emojis follow the tone (sarcastic "great job" → 🙃👏, not 🥳).

Legend: **[Claude]** done by Claude · **[You]** needs your phone / password / accounts / taste · ✅ done · 🔄 in progress · ⬜ not started

---

## Prerequisites

| What | Why | Status |
|---|---|---|
| llama.cpp (built from source in `~/build/llama.cpp`) | runs Laya; Laya support is newer than the Arch package | ✅ [Claude] |
| Laya GGUF (`models/`, gitignored) | the model | ✅ [Claude] — `bash scripts/get-model.sh` |
| Python 3, Node 22, pnpm, cmake, ninja, gcc | scripts, extension build, native builds | ✅ already installed |
| JDK 21, adb, udev rules | Android build + install to phone | ✅ **[You]** installed 2026-10-04 |
| Android SDK 36 + build-tools 36 + NDK r30 in `~/Android/Sdk` | cross-compile llama.cpp for phones, build the APK | ✅ [Claude] |
| Full emoji catalog (`shared/emoji-catalog.json`) | all 1,923 base emojis + names + CLDR keywords | ✅ [Claude] — `python3 scripts/build-emoji-catalog.py` |
| Brand assets (`assets/brand/`) | logo, favicons, extension icon sizes | ✅ from the owner's files |
| A phone with USB debugging | the only real latency test | ⬜ **[You]** Redmi: see MANUAL-WORK §A (Xiaomi has extra switches) |
| PyTorch + transformers, or a Kaggle account | training Laya-Emoji | ⬜ **[You]** decide A or B in TRAINING.md |
| emscripten | WebAssembly engine for the extension | ⬜ **[You]** sudo, Phase 3 |
| Hugging Face account | **not needed** (models are public) | — see HUGGINGFACE.md |

---

## Phase 0 — Prove Laya can do this (laptop) — ✅ mostly done

- ✅ [Claude] Build llama.cpp, download + checksum the model, `scripts/dev-server.sh`, `scripts/test-prompts.py`.
- ✅ [Claude] Found that asking Laya to choose among emojis directly fails (192-token option budget → emojis truncated to byte fragments) and switched to the **mood-label design** (see ARCHITECTURE).
- ✅ [Claude] Compared model files: **Q4_0 (252 MB, made locally) is the pick** — smallest, fastest, no accuracy loss vs the official 8-bit file.
- ✅ [Claude] Result on 23 labelled samples (Q4_0, 4 threads): **18/23 correct first mood, 22/23 in the top 3, ~183 ms per suggestion, ~265 MB RAM** on this laptop's CPU.
- ✅ [Claude] Reply awareness measured: 3/3 reply samples right with the replied-to message, 0/3 without.
- ⬜ **[You]** ~50 messages in your own texting style (MANUAL-WORK §B) → [Claude] turns them into the test set and tunes `moods.json` against it (target ≥ 85% top-3).
- ✅ Instant keyword layer: dropped (owner: whole sentence only).
- ✅ [Claude] Full Unicode 18.0 catalog: 3,963 emojis → 1,923 distinct + skin tones (`shared/emoji-catalog.json`).

**Exit:** test-set score ≥ target, mood list approved by you.

## Phase 1 — Is it fast enough on your phone? — ✅ yes (2026-10-05)

**Result:** REDMI Note 17 Pro 5G, 4 threads: **~306–356 ms per suggestion**, same accuracy as the laptop, ~230 MB RAM (RESEARCH.md §4). Done with a command-line build over adb — the "EmoJet Lab" app was not needed for the measurement; its pieces move into Phase 2.

The biggest unknown. Laptop: ~390 ms. A phone CPU is typically 2–4× slower, so expect ~0.8–1.5 s unless a GPU backend or fewer tokens help. Measure before building UI.

1. ✅ [Claude] Install Android SDK + NDK.
2. ✅ [Claude] Cross-compile `llama-server` for `arm64-v8a` with KleidiAI (portable, fast Q4_0) — `~/build/llama.cpp/build-android`.
3. [Claude] "EmoJet Lab": a bare test app — text box, Run button, shows mood + emoji row + milliseconds, runs the full 23-sample benchmark with one tap. Model copied with `adb push` (no download code yet).
4. ✅ Measured over adb with `bash scripts/phone-bench.sh` (no app needed).
5. [Claude] Experiments on your phone: 8-bit vs 4-bit, 2/4/6 threads, OpenCL (Adreno) / Vulkan GPU backends if your chip has them, fewer moods (e.g. 16) to shorten the prompt.
6. ✅ **Go**: Q4_0, 4 threads, CPU. Target ≤ 600 ms met.

## Phase T — Train Laya-Emoji (all 1,923 emojis, whole sentence) — ⬜

Runs in parallel with Phase 1 (same encoder, so phone speed results carry over). Full plan: [TRAINING.md](TRAINING.md).

1. ✅ [Claude] Laya's encoder loads in PyTorch and matches llama.cpp exactly; Kaggle T4 fits full training (~31–48 min/epoch of 200K) — TRAINING.md step 1.
2. [Claude] Build the dataset (emojinize 108K sentences + GoEmotions 58K + Laya-teacher + CLDR coverage; tone: GoEmotions map, SARC sarcasm, Stanford politeness, Pavlick formality, Jigsaw toxicity, hand-written styles); your messages = test set.
   - ✅ Tone taxonomy researched (`shared/tones.json`) and untrained-Laya tone baseline measured: tone 18/25 top-3, style 6/13 (`scripts/test-tones.py`).
3. [Claude] Laptop pilot: frozen encoder + emoji head.
4. **[You]** Choose where the full training runs (laptop GPU vs free Kaggle GPU); [Claude] runs or prepares it.
5. [Claude] Convert to GGUF Q4_0, verify, A/B against the mood model. Laya-Emoji replaces moods only if it wins on your test set.

## Phase 2 — Android overlay app — ⬜

1. [Claude] Engine service: starts `llama-server` from the APK's native-lib folder on a random localhost port + random API key, loads the model once, stops after idle.
2. [Claude] Model manager: first-run download (pinned revision, sha256, resumable, Wi-Fi only), GitHub mirror fallback, "import .gguf" button.
3. [Claude] AccessibilityService: focused field text + cursor, skip password fields, per-app allow/deny list.
4. [Claude] Overlay strip near the field, tap to insert at cursor, follows keyboard.
5. [Claude] Debounce + cache + dedup rule (never block multi-emoji typing).
6. [Claude] Reply awareness: generic "last incoming bubble", WhatsApp quoted-reply adapter.
7. [Claude] Toggle: quick-settings tile, notification action, in-app switch; "pause for 1 hour".
8. [Claude] Settings: per-app list, battery saver, model status, privacy page ("0 network calls since setup").
9. [Claude] **Updates screen**: reads `updates.json` from GitHub Releases on "Check for updates" (optional weekly Wi-Fi check, off by default); model/data updates in-app with sha256 + rollback; app updates via Android's installer (one "Update" tap, same signing key, no uninstall); re-enable shortcut if the phone turned accessibility off after an update.
9. **[You]** Real-world day of use; report what feels wrong.
10. [Claude] Fixes, then a signed release APK (you back up the key).

## Phase 3 — Browser extension — ⬜

1. [Claude] Extract the Laya decision code from `llama-server` into "emojet-core" (C++), with a tiny C API: `load(model)`, `decide(json) → json`. Unit-tested on the laptop against `llama-server`'s answers (must match).
2. [Claude] Compile emojet-core to WebAssembly (emscripten, SIMD + threads). **Measure speed first** — WASM is usually 2–3× slower than native. Go/no-go:
   - fast enough → it's the default engine;
   - too slow → default to the optional local fast path below and keep WASM as the zero-setup fallback.
3. [Claude] MV3 extension: content script (Shadow-DOM strip), `insertText`-based insertion (works with WhatsApp Web's / Instagram's editors), offscreen-document engine, OPFS model storage, popup (on/off, per-site, status).
4. [Claude] Site adapters for reply context: WhatsApp Web, Instagram; generic for the rest.
5. [Claude] Optional fast path: if a local `llama-server` is running (you already have one), use it. Off by default, never required.
5b. [Claude] **Updates page**: fixed extension ID (`key` in manifest) so settings + model survive updates; model/data updates in-extension; code updates one-click via a folder you allow once (verify it works), fallback "unzip over + Reload EmoJet"; Firefox build with automatic updates.
6. **[You]** Real-world use on the sites; Firefox signing account if you want Firefox permanently.
7. [Claude] Release zip.

## Phase 4 — Enhancements — ⬜

Confirmed by you:
- **Reply-aware suggestions** (Phases 2/3).
- **Multi-emoji friendly**: suppression only de-duplicates unchanged text; chains of emojis keep getting suggestions.

Confirmed by you (2026-10-04):
- **Tone of voice** in the model (Phase T outputs).

Proposed (pick what you like):
- **Tone chip** — a tiny label on the strip ("playful", "sounds passive-aggressive"), like Grammarly.
- **Tone check before sending** — off by default; warns gently if the draft reads aggressive / condescending / passive-aggressive.
- **Their tone** — tone of the message you're replying to ("they sound upset" → 🫂 rises).
- **Personal boost** — emojis you pick often rise in their mood row (stored locally: emoji + mood only, never text).
- **Combo chip** — one tap inserts a 2–3 emoji combo (😂💀, 🥳🎉🎂) — made for heavy emoji users.
- **Skin tone preference** applied to hand/people emojis.
- **Swipe the strip** to see the next mood's row; long-press for the full row.
- **Position / size / opacity** of the strip; haptic tick on insert; Material You colours.
- **Emoji Wrapped** — a local-only stats card (your top emojis and moods this month), screenshot-ready.
- **Kaomoji mode** ¯\\_(ツ)_/¯ for places where emojis look wrong (work chats, terminals).
- **Hinglish / multilingual** — convert Laya's multilingual checkpoint to GGUF if Hinglish scores stay low.
- **Sarcasm wink** — when idle, one extra Laya yes/no pass; if sarcastic, add 🙃.
- **TalkBack labels** so the overlay is usable with a screen reader.

## Phase 5 — Release — ⬜

- [Claude] `docs/INSTALL.md` for users (APK steps incl. restricted settings, extension "load unpacked").
- [Claude] GitHub Release: APK + extension zip + model + checksums + notices + **`updates.json`** (latest version, URL and sha256 of app, extension, model, data) — what the in-app Updates screens read.
- **[You]** OK each public release; back up the signing key; check Google's developer-verification status (MANUAL-WORK §G).

---

## Working agreement

- Claude commits and pushes every meaningful change (owner's instruction), with a Co-Authored-By trailer.
- Model files, keys, tokens and build outputs never go into git (`.gitignore`).
- Every change to `moods.json` or prompt wording is re-scored with `scripts/test-prompts.py`; the score must not drop.
