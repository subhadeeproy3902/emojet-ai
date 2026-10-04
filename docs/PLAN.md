# EmoJet — Phase plan

Goal: a free, fully on-device emoji suggestion overlay (Android) + browser extension, built on Laya only, distributed as a raw APK + an unpacked extension folder — no app stores, no servers, no cost to build or to run, ever.

Legend: **[Claude]** = can be done by the assistant directly. **[You]** = needs your hardware, your accounts, or a judgment call only you can make.

---

## Phase 0 — Validate the model itself

Nothing else matters if Laya's suggestions aren't good or aren't fast enough. Do this before writing any app code.

1. **[You]** Install `llama.cpp` locally (not on this EndeavourOS box necessarily — wherever is convenient for you to iterate; could be this machine too).
2. **[Claude]** Write a small script (`scripts/dev-server.sh`) that starts `llama.cpp server` with the Laya `Q8_0` GGUF and exposes `/v1/systemone`.
3. **[Claude]** Write a test harness (`scripts/test-prompts.*`) with ~20 real-world sample sentences (happy, sarcastic, angry, flirty, plain statements) and the typed-decision schema (emoji candidate set, emotion class, mood/urgency/energy scores) to send against that endpoint.
4. **[You]** Run the server + harness, eyeball the suggestion quality against the 20 samples. Decide: good enough as-is, or does it need prompt/schema tweaks?
5. **[Claude]** If size/speed is a concern later, write the `llama-quantize` command to produce a `Q4_K_M` (~225MB) variant from the same checkpoint, as a documented fallback — not run unless Phase 1 shows it's needed.

**Exit criteria:** you're happy with suggestion quality on the local server, and we have a documented typed-decision schema (the exact "questions" sent to Laya) to reuse in both the Android and browser clients.

---

## Phase 1 — Android on-device latency prototype

The riskiest unknown: is a 421M-param single-forward-pass model actually fast enough on a phone CPU? Nobody's published that number for Laya. Find out before building any UI.

### Prerequisites (install once)

- **[You]** Android Studio + NDK (needed to compile `llama.cpp` for Android via JNI).
- **[You]** Your physical Android phone, USB debugging enabled (`adb` needs to see it) — an emulator can't meaningfully test real-world CPU latency or later, the AccessibilityService/overlay behavior against the real WhatsApp app.

### Steps

1. **[Claude]** Scaffold a minimal Kotlin app with a JNI binding to `llama.cpp` (reusing the wiring pattern from llama.cpp's own `examples/llama.android`, not copying a product).
2. **[Claude]** Wire it to load the Laya GGUF once at app start and keep it warm (reload cost would dominate perceived latency — load-once is non-negotiable).
3. **[You]** Push the model file + app to your phone (`adb push` / debug install), run it against the same 20 test sentences from Phase 0.
4. **[You]** Report back: real on-device ms per suggestion, RAM used, any visible battery/heat impact over a few minutes of sustained use.
5. **[Claude + You]** Go/no-go: if latency feels bad, the lever is quant level (`Q4_K_M`) and trimming context (feed only the last sentence/clause, not full chat history) — not switching models. We stay Laya-only per your requirement.

**Exit criteria:** a confirmed, real, on-device latency number from your own phone, and a decision on which quant level ships.

---

## Phase 2 — Android overlay app (the real product)

1. **[Claude]** `AccessibilityService` that reads the focused text field system-wide (any app — WhatsApp, Instagram, SMS).
2. **[Claude]** Explicitly skip password/sensitive input fields (`InputType` flags) — privacy non-negotiable, not just a nicety.
3. **[Claude]** `TYPE_APPLICATION_OVERLAY` window: floating suggestion strip anchored near the input/keyboard.
4. **[Claude]** Debounce logic: run inference on pause-in-typing, not every keystroke.
5. **[Claude]** Suppression logic (see "Design notes" below) for the "just typed an emoji" case, tuned to not punish heavy multi-emoji typers.
6. **[Claude]** Reply-aware context: if the message you're replying to is readable from the accessibility tree, feed it in alongside your own draft for sharper suggestions.
7. **[Claude]** Settings screen: on/off toggle, battery-saver mode, model download/update manager, plain-language privacy explanation.
8. **[You]** Grant the accessibility permission on your phone (one-time, interactive — can't be scripted) and live-test against real WhatsApp/Instagram/SMS conversations.
9. **[Claude]** Build the signed release APK once you're happy with it.

---

## Phase 3 — Browser extension

1. **[Claude]** Manifest V3 scaffold, generic content script that attaches to any focused editable element on any page (not per-site rules) — covers WhatsApp Web, Instagram, Snapchat, Gmail, anywhere.
2. **[Claude]** WASM build of `llama.cpp` (e.g. `wllama`) running the same Laya GGUF, inside a Web Worker so typing never stutters.
3. **[Claude]** Model downloaded once from Hugging Face on first use, cached in IndexedDB — fully offline after that.
4. **[Claude]** Popup toggle (on/off) + per-site quick-disable, using browser-local storage only (no accounts, no sync to any server).
5. **[You]** Load it unpacked in your browser(s) and live-test on the actual sites, same as the dev loop in Phase 0.
6. **[Claude]** Package a release zip (for "download and unpack" distribution, no store).

### Optional power-user fast path (not required for anyone)

Since you already have `llama.cpp server` running locally for Phase 0 dev-testing, that same local server can stay available as an opt-in "fast path" setting in the extension (native speed instead of WASM, no download needed if you already run it). Off by default — regular users never see or need it; this only exists because the dev tooling happens to double as a nice option for people like you who are comfortable running a local server.

---

## Phase 4 — Polish / enhancements

- Reply-aware suggestions (moved up into Phase 2/3 above — flagged here as a confirmed priority).
- Smart multi-emoji suppression (see design notes).
- Battery-saver throttling below 20% battery (Android).
- Long-press to expand into a mood-categorized grid, using Laya's emotion/score outputs you're already computing (not just the top row).
- Double-tap a suggestion to insert-and-send in one action.
- Visible "0 network calls" indicator in settings — the honest answer to "is this really free and private."
- Model-update channel pointed at the Hugging Face repo, independent of app/extension releases.

---

## Design notes

### Reply-aware suggestions
When the accessibility tree / page DOM exposes the message you're replying to, include it as extra context alongside your own draft when building the typed-decision prompt. This should noticeably improve relevance for short replies ("same place?" → 📍 vs just guessing from 3 words alone). Falls back gracefully to draft-only when no reply context is available (e.g., starting a new message).

### Suppressing re-suggestion without punishing heavy emoji users
The naive rule ("don't suggest right after an emoji") breaks for people who chain many emojis on purpose. The actual rule should be:

- Suppress re-running inference only when the **clause hasn't changed** since the last suggestion was shown (i.e. you tapped a suggestion or ignored it, and haven't typed any new word since) — this avoids wastefully re-suggesting on a sentence that's already been evaluated.
- Do **not** suppress just because the last character is an emoji. If you keep typing more words/clauses after an emoji, suggestions should keep coming, as many times as you want.
- This is a debounce-dedup rule (don't repeat work on unchanged input), not a "you've had enough emojis" rule.

### Distribution (confirmed)
No Play Store, no Chrome Web Store. Android ships as a signed APK for sideloading; the extension ships as a zip for "load unpacked" installs. This removes the $25 Play Store fee, the Chrome Web Store review, and any store policy risk entirely — fully consistent with the zero-cost goal.

### Open source
MIT license. Contributions welcome once Phase 1's prototype proves the latency story out.
