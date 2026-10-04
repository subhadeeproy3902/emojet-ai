# Workflows

The big picture in plain words: how the model is made, how people install EmoJet, what happens when they type, and how updates arrive **without uninstalling anything**. Component details: [ARCHITECTURE.md](ARCHITECTURE.md).

## Making the model ≠ running the model

| | Making the model (training) | Running the model (suggestions) |
|---|---|---|
| When | a few times in total, once per model version | every time anyone types |
| Where | **Kaggle** free GPU (owner's decision 2026-10-05) + laptop for prep/tests | each user's **own** phone / browser |
| Cost | $0 | $0 — the user's device does the math |
| Result | one file `laya-emoji.gguf` (~250 MB) | 6 emoji suggestions + tone |

Kaggle is a kitchen used once per version; the model file is the dish everyone takes home. No server runs for users, ever — 1 user or 1 million costs the same: $0.

```
┌──────────── OUR SIDE (only when making a new version) ───────────────────────┐
│  datasets ──► train on Kaggle ──► convert to .gguf ──► test on laptop        │
└───────────────────────────────────────────────────────────────┬──────────────┘
                                                                ▼ publish
┌──────────── FREE FILE HOSTING (storage only) ────────────────────────────────┐
│  GitHub Releases: EmoJet.apk · emojet-extension.zip · laya-emoji.gguf        │
│                   updates.json (what's the latest version of each)           │
│  Hugging Face:    laya-emoji.gguf mirror (optional)                          │
└──────────────┬─────────────────────────────────────────┬─────────────────────┘
               ▼ download once, then only updates        ▼
┌───── USER'S PHONE (offline) ───────────┐   ┌──── USER'S BROWSER (offline) ────┐
│ Reader → Engine → Ranker → Overlay     │   │ Reader → Engine → Ranker → Strip │
│ Updates screen                         │   │ Updates page                     │
└────────────────────────────────────────┘   └──────────────────────────────────┘
```

Nothing goes up: no messages, no usage data, no accounts.

## 1. Making a model version (us)

```
build dataset → train → convert to .gguf → quantize Q4_0 → score on owner's messages → publish + updates.json
   (laptop)    (Kaggle)     (laptop)          (laptop)          (laptop)                  (GitHub Release)
```

Claude prepares the dataset and the training notebook, sends it to Kaggle with the owner's Kaggle API token, downloads the result, converts, tests, and publishes only if it beats the previous version.

## 2. Installing (each user, once)

```
download small APK (~15 MB) from GitHub → install → permissions (accessibility, overlay, battery)
→ app downloads the model once (~250 MB, Wi-Fi) → ready, works offline
```

Extension: download zip → unzip → Load unpacked → the extension downloads the model once.

## 3. Typing (every message, on the device, target ~0.2–0.5 s)

```
type "it's fine. really. I'm fine." → pause ~0.35 s
→ Reader: whole text + replied-to message
→ Engine: one Laya-Emoji pass → 1,923 emoji scores + tone + style
→ Ranker: tone boost, skin tone, personal history → 🙂 🙃 😑 😒 🫠 👍
→ strip near Send → tap inserts at the cursor → keep typing, suggestions keep coming
```

## 4. Updates — from inside the app / extension, no uninstall

Three kinds of updates, each with its own version number in `updates.json` (a small public file in the GitHub Release):

| What changes | How often | Size |
|---|---|---|
| **Model** (`laya-emoji.gguf`) | each new training | ~250 MB |
| **Data** (`emoji-catalog.json`, `tones.json`, ranking settings) | new Unicode emojis, tuning | < 1 MB |
| **App / extension code** | new features, fixes | APK ~15 MB / zip ~5 MB |

### Android app — Settings → Updates

- Shows installed vs latest for app, model and data, with release notes. **Check for updates** button; optional "check weekly on Wi-Fi" (off by default — privacy: the check only downloads `updates.json`, it sends nothing about you).
- **Model / data update:** downloaded inside the app, sha256 checked, swapped in only after it's complete; the old one keeps working until then; "roll back" keeps the previous model.
- **App update:** the app downloads the new APK, checks its sha256, and hands it to Android's installer. Android shows **"Do you want to update this app?" → Update**. One tap, installed over the old version, settings and model kept. No uninstall.
  - Works because every EmoJet APK is signed with the **same key** and has a higher version number. That's why the signing key must be backed up (MANUAL-WORK §E) — a lost key would force an uninstall.
  - First time only: Android asks to allow EmoJet to "install unknown apps".
  - Android never allows a sideloaded app to update itself with zero taps; the one "Update" tap is the minimum.
  - Some phones switch the accessibility service off after an app update; EmoJet detects it and shows a one-tap shortcut to turn it back on.

### Browser extension — popup → Updates

- Same screen: installed vs latest, release notes, **Check for updates**.
- **Model / data update:** downloaded inside the extension, no restart, nothing else to do.
- **Code update (Chrome, Edge, Brave, Opera):** browsers don't let an unpacked extension replace its own files on their own. Plan:
  - **One-click (to verify in Phase 3):** the first time, you pick your EmoJet folder once ("allow EmoJet to edit this folder"); after that, **Update** downloads the new version, checks it, writes the new files into that folder and reloads EmoJet. No uninstall.
  - **Fallback:** download the zip, unzip it over the old folder, click **Reload EmoJet** on the Updates page.
  - Either way the extension keeps a fixed ID (`key` in `manifest.json`), so settings and the downloaded model survive every update.
- **Firefox:** a free Mozilla-signed build with an update address updates itself automatically, like a store extension.

## 5. Our development loop

```
Claude codes → tests on the laptop → owner tests on phone/browser → commit + push → GitHub Release + updates.json
```
