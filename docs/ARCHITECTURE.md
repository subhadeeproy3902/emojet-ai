# Architecture

## The one idea everything is built on

Laya is not asked "which emoji?". It is asked **"what is the feeling or topic of this message?"** and picks from ~25 short word labels (`laughing`, `grateful`, `tired`, `food` ...). EmoJet maps the top labels to rows of emojis on its own side ([`shared/moods.json`](../shared/moods.json)).

Why: Laya fits the question **and all the options** into a budget of **192 tokens** (the `max_head_tokens` value stored in the model). One emoji costs 3–6 tokens (❤️ alone is 6). With 64 emoji options every option was cut to ~3 tokens of broken bytes, and the picks were close to random. Word labels cost ~2–3 tokens each, fit easily, and Laya is very good at them (see [RESEARCH.md](RESEARCH.md) for the numbers).

```
your draft text  ─┐
replied-to msg   ─┼─► Laya (1 forward pass, ~25 mood labels) ─► mood probabilities
                  │                                                    │
                  │                         shared/moods.json ◄────────┘
                  │                                │
                  │                       blend top 3 moods' emoji rows
                  │                                │
                  │                    + your personal pick history (local)
                  │                                ▼
                  └──────────────────────► 6 emoji suggestions in the overlay
```

The blend (same in every client): each of the top 3 moods adds `p(mood) / (1 + 0.35 × rank)` to each emoji in its row; the 6 highest scores win. Reference implementation: `suggest()` in [`scripts/test-prompts.py`](../scripts/test-prompts.py).

## Shared pieces (one source of truth)

| File | Used by | What |
|---|---|---|
| `shared/moods.json` | Android, extension, test script | question text, mood labels (+ optional short descriptions), emoji row per mood |
| `scripts/test-prompts.py` | dev + CI-style check | the labelled sample set and the accuracy score; any change to `moods.json` must not lower it |

## Where the Laya logic lives (important)

In llama.cpp, the part that turns `{state, questions}` into Laya prompts and turns the model output into probabilities is **inside `llama-server`** (`tools/server/server-decision.cpp`, ~800 lines), not in the reusable `libllama`. So each client either runs `llama-server` itself or carries that logic:

| Client | How it runs Laya | Why |
|---|---|---|
| Dev / desktop | `llama-server` on `127.0.0.1` | exact upstream behaviour, zero porting |
| Android (Phase 1–2) | `llama-server` cross-compiled for arm64, shipped inside the APK as `libllama_server.so` (native-lib folder = the one place Android lets an app execute its own binary), started by the app on a random localhost port with a random API key | reuses the upstream code verbatim; model fixes come with a rebuild |
| Android (later, optional) | JNI library "emojet-core" = libllama + the decision code extracted from the server | less memory, no HTTP hop; only if Phase 1 shows the process approach costs too much |
| Browser extension | "emojet-core" compiled to WebAssembly (emscripten), run in an offscreen document / worker | browsers can't run a server process; same C++ as Android-later, so only one port |
| Browser, optional fast path | the extension talks to a local `llama-server` if you run one | native speed for power users; off by default, never required |

Laya is an **encoder** (ModernBERT): every token looks at every other token, so a cached "question + options" prefix cannot be reused across messages. Cost per suggestion grows linearly with prompt length — this is why the prompt is kept short.

## Android app

- **AccessibilityService** — reads the focused editable field in any app (text + cursor position). Skips password fields (`isPassword`) and any app on the user's deny list. Optional per-app allow list (default: messaging apps).
- **Overlay** (`TYPE_APPLICATION_OVERLAY`, "Display over other apps") — a small floating strip anchored just above the focused field / Send button.
- **Insert** — tap = insert emoji at the cursor through the accessibility API (`ACTION_SET_TEXT` with the new text + `ACTION_SET_SELECTION`), no clipboard.
- **Engine service** — foreground service that owns the `llama-server` child process, loads the model once and keeps it warm; stops it when EmoJet is toggled off or after N minutes without typing (battery).
- **Reply context** — generic: the last incoming message bubble visible above the input; per-app adapters (WhatsApp first) can read the quoted-reply view when present.
- **Toggle** — quick-settings tile + notification action + in-app switch.

## Browser extension (Manifest V3)

- **Content script** on all sites: listens to `focusin`/`input` on `textarea`, `input[type=text]` and `contenteditable`; draws the suggestion strip in a Shadow DOM so site CSS can't break it.
- **Insert** — `document.execCommand('insertText')` / `InputEvent`, because editors like WhatsApp Web's (Lexical) and Instagram's ignore direct `.value`/`textContent` changes.
- **Engine** — WASM emojet-core in an offscreen document (Chrome) / background page (Firefox); model stored in the Origin Private File System after the first download (needs the `unlimitedStorage` permission).
- **Reply context** — per-site adapters (WhatsApp Web, Instagram) read the quoted / last incoming message; generic sites use draft only.
- **Popup** — on/off, per-site disable, model status, "0 network calls since setup" counter.

## Speed strategy

1. **Short prompts**: only the current/last two sentences of the draft + at most ~30 words of reply context.
2. **Debounce**: run Laya ~350 ms after typing pauses, on a word/sentence boundary; drop stale results if the text changed meanwhile.
3. **Cache**: identical normalised text → cached answer (backspace-then-retype is instant).
4. **Dedup, not suppression**: if the clause is unchanged since the last suggestion, don't re-run. Never stop suggesting just because the last character is an emoji — multi-emoji typing keeps working.
5. **Optional instant layer (owner to decide)**: a plain keyword table (`haha` → laughing, `bday` → celebration) shows a first row at 0 ms; Laya's answer replaces it when ready. Not a second AI model.

## Privacy rules (non-negotiable)

- No network access except the one-time model download (and checking for a newer model only when the user taps "check").
- Nothing typed is stored, except the optional local pick history (emoji + mood, never the text).
- Password fields and deny-listed apps are never read.
