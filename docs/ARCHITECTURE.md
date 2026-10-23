# Architecture

How EmoJet turns "you are typing a sentence" into six emojis above your keyboard, without anything leaving the phone.

## The whole pipeline

```
 you type in WhatsApp
          │
          ▼
 ┌──────────────────────┐  accessibility event (text changed / focus / selection)
 │  EmojetService       │  throttled; fires 250 ms after you stop typing
 │  (AccessibilityService)
 └─────────┬────────────┘
           │  the focused text box + the message being replied to
           ▼
 ┌──────────────────────┐  "replying to: …\nmy message: …"
 │  Ranker.format()     │
 └─────────┬────────────┘
           ▼
 ┌──────────────────────┐  llama.cpp in-process, 127.0.0.1, random port + random API key
 │  Engine → Emojet v1  │  one forward pass → 2,002 scores
 └─────────┬────────────┘
           ▼
 ┌──────────────────────┐  reply blending · tone boost · topic guarantee · offensive guard
 │  Ranker              │
 └─────────┬────────────┘
           ▼
   six emojis drawn in an overlay window just above the text box
```

## The model

[Emojet v1](https://huggingface.co/subhadeep3902/emojet) — Laya's ModernBERT-large encoder with one
1024 → 2002 classification head. A single forward pass returns:

| Range | Group | Activation |
|---|---|---|
| 0 – 1922 | every emoji in Unicode 18.0 | softmax over the group |
| 1923 – 1987 | 65 tones of voice | sigmoid, independent |
| 1988 – 1995 | 8 communication styles | softmax over the group |
| 1996 – 2001 | 6 style axes | sigmoid, 0–1 |

The app ships the 4-bit GGUF (274 MB) and runs it with llama.cpp compiled for arm64. The label order is
`shared/emoji-catalog.json` followed by `shared/tones.json` — the same order the training notebook writes, which is
what lets the app and the model agree without a schema.

> **Historical note.** Before the model existed, EmoJet asked stock Laya *"what is the mood of this message?"* and
> mapped ~25 mood labels to rows of emojis (`shared/moods.json`). That path still exists in the code as the
> `"decision"` model kind and is used only if someone loads a plain Laya file. What ships is `"classifier"`.

## The service — `EmojetService.kt`

An `AccessibilityService` is the only way Android lets an app see the text box you are typing in inside another app.

It reads exactly two things:

- the **focused editable node** (`isEditable`, and never when `isPassword`), and
- the **message being replied to**, found by `ReplyFinder` as the nearest readable text above that input.

It does nothing else with the permission: no clicks, no navigation, no autofill, no reading other screens. Its only
write is inserting the emoji you tapped.

Behaviour worth knowing:

| | |
|---|---|
| Debounce | 250 ms after typing stops, so every keystroke does not start an inference |
| Long messages | clipped to 900 characters, head + `…` + tail, because the model reads 256 tokens |
| Empty box | no strip at all — unless you swiped to reply, then you get emojis for *that* message |
| Window changes | the strip hides when you leave the app or the field |
| Dedup | the same text is never asked twice, and a stale answer is dropped by a generation counter |

## The overlay

`TYPE_ACCESSIBILITY_OVERLAY`, which needs no "draw over other apps" permission. It is positioned from the text
box's `getBoundsInScreen`, and wherever you drag it is remembered per app (`Prefs.stripOffset`). Holding a person
emoji opens a skin-tone popup; the tone is applied by inserting the modifier after the first code point, but only
when `Paint.hasGlyph` confirms the phone can actually draw it.

## Ranking — `Ranker.kt`

Raw scores are not shown directly. In order:

1. **Reply blending.** With a reply in view the model is asked twice (draft alone, draft + reply) and the two score
   vectors are mixed by how confident the draft alone was. A specific draft ("Laptop") keeps its own emojis; a vague
   one ("yes!!") follows the conversation.
2. **Tone boost.** Tones above 0.5 lift the emojis listed for them in `shared/tones.json`.
3. **Topic guarantee.** Up to three "thing" emojis (food, animals, places, objects…) within a window of the best
   score get a bonus, so a message about pizza does not come back as six smileys.
4. **Offensive guard.** 🖕 🤬 🤮 🤢 🍆 🍑 👿 are dropped unless the message itself scores ≥ 0.5 on
   rude / aggressive / angry / derisive. This cannot rescue a *mis-read* tone — only stop an offensive emoji on a
   message the model correctly reads as calm.

`scripts/rank_sim.py` in the [model repo](https://github.com/subhadeeproy3902/emojet) is the same algorithm in
Python, so ranking changes can be measured on the full test sets before touching the app.

## The engine — `Engine.kt`

llama.cpp's server, shipped as `libllama_server.so` and started as a child process on **127.0.0.1 with a random
port and a random API key**, so no other app on the phone can reach it. One instance per app process, kept warm.
Threads = the number of big cores (2–6); the little cores only slow it down. About 0.4 s per suggestion on a
mid-range phone, 180 ms on a laptop.

## Model delivery — `ModelManager.kt`

The APK is 8 MB; the model is fetched once (274 MB) through `DownloadManager`, which resumes and survives reboots.
It is **checksummed (SHA-256) before use** and comes from a **pinned Hugging Face revision** listed in
`shared/models.json`. Free space is checked first, any connection is used by default (there is a "Wi-Fi only"
switch), and a file you already have can be imported instead.

## Staying alive

Phones aggressively freeze background apps. EmoJet runs as a foreground service with a silent `IMPORTANCE_MIN`
notification, asks for battery-optimisation exemption, and the Help page shows the live state of every permission.
On Xiaomi/HyperOS **Autostart** is additionally required — without it the service is killed after a few days and
suggestions silently stop, which is the single most common support issue.

## Two Android behaviours that surprise every sideloaded app

- **Restricted settings** (Android 13+): the accessibility switch is blocked until the user opens App info →
  ⋮ → *Allow restricted settings*. Step 2 of setup leads with this.
- **Play Protect**: in about ten countries it blocks installing any sideloaded app that declares
  `BIND_ACCESSIBILITY_SERVICE`. See [`../INSTALL.md`](../INSTALL.md).

Neither appears when installing over `adb`, which is why both have to be tested the way a real user installs.

## Files

| File | Responsibility |
|---|---|
| `EmojetService.kt` | accessibility events, when to suggest, the overlay |
| `ReplyFinder.kt` | find the message being replied to, with no app-specific code |
| `Ranker.kt` | 2,002 scores → six emojis |
| `Engine.kt` | the llama.cpp process and its HTTP calls |
| `ModelManager.kt` | download, verify, import, upgrade |
| `MainActivity.kt` | home screen and the three setup steps |
| `OnboardingActivity.kt` | first run: what it does, what it reads, what it costs |
| `HelpActivity.kt` | troubleshooting with live permission state |
| `Theme.kt` | every colour, shape and text style (there are no XML layouts) |
| `shared/*.json` | label order and model list, shared with the training repo |
