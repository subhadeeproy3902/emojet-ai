<div align="center">

<img src="assets/brand/emojet-logo-256.png" width="140" alt="EmoJet">

# EmoJet

**The right emoji, every time — while you type, in any app, with nothing leaving your phone.**

[![Download APK](https://img.shields.io/badge/download-APK_(arm64)-ffcc4d?logo=android&logoColor=white)](../../releases/latest)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3ddc84?logo=android&logoColor=white)](#install-on-android)
[![Size](https://img.shields.io/badge/app_8_MB_%2B_model_274_MB-282_MB_total-blue)](#how-much-space-it-takes)
[![Model](https://img.shields.io/badge/%F0%9F%A4%97_model-Emojet_v1-ffcc4d)](https://huggingface.co/subhadeep3902/emojet)
[![Licence](https://img.shields.io/badge/code-Apache--2.0-green)](LICENSE)
[![Offline](https://img.shields.io/badge/works-fully_offline-lightgrey)](#privacy)

### ⚠️ Installing in India, Brazil, Singapore, Thailand, Vietnam, the Philippines, Hong Kong, Kenya, Nigeria or South Africa?

Android will block the install with *"App blocked to protect your device"*. It does that to **every** sideloaded
app that uses the accessibility permission. Four taps get past it — **[step-by-step guide →](INSTALL.md)**

[Install](#install-on-android) · [How it works](#how-it-works) · [Space](#how-much-space-it-takes) · [Privacy](#privacy) · [Desktop](#use-emojet-on-a-computer) · [Build it](#build-from-source-windows-macos-linux) · [Troubleshooting](#troubleshooting)

</div>

---

You type in WhatsApp, Instagram, Messages — any app. You pause. A row of six emojis appears right above the
keyboard, chosen from the **whole sentence**, not the last word. Tap one and it goes in.

```
┌─────────────────────────────────────────────┐
│  kal paper hai aur kuch nahi padha          │   ← what you typed (Hinglish)
├─────────────────────────────────────────────┤
│  ⠿   📚   😭   😩   😰   🤦   📖        ✕   │   ← EmoJet, right above the keyboard
└─────────────────────────────────────────────┘
```

It understands **English, Hindi and Bengali**, including the way people really type — Hinglish and Banglish in
English letters, native script, and the spelling variations in between.

Everything runs on the phone. No account, no server, no subscription, no ads, and after the one-time model
download, no internet at all.

## Install on Android

**Requirements:** Android 8.0 or newer, an arm64 phone (every phone since ~2017), ~282 MB free.

1. **Download the APK** from the [latest release](../../releases/latest) — `emojet-v1.0.0-arm64.apk`.
2. **In some countries, let Play Protect through first.** In India, Brazil, Singapore, Thailand, Vietnam, the
   Philippines, Indonesia-region pilots, Hong Kong, Kenya, Nigeria and South Africa, Android blocks *every*
   sideloaded app that uses the accessibility permission with an "App blocked to protect your device" dialog.
   Everywhere else it installs normally — skip to step 3. If you see the dialog: Play Store → your profile picture →
   **Play Protect** → **⚙** → turn **off** "Scan apps with Play Protect", install, then turn it back on.
   ([Why?](#why-android-blocks-the-install))
3. **Open it.** Android asks to allow installing from this source; allow it for your browser or file manager.
   (Chinese phones usually ask again inside MIUI/ColorOS — accept there too.)
4. **Open EmoJet.** It explains what it does and what it needs, then walks you through three steps:
   - **Download the model** — 274 MB once, over Wi-Fi, then it works offline forever.
   - **Turn on Accessibility** for EmoJet — the only way Android lets an app read the box you are typing in.
   - **Allow it to keep running** — a silent notification plus "no battery restrictions", so the phone does not
     freeze it after a few days.
5. **Type in any app**, pause, and tap an emoji.

### Why Android blocks the install

EmoJet needs Android's **accessibility permission**, because it is the only way Android lets an app read the text
box you are typing in. That same permission is what banking trojans abuse — they use it to read one-time codes off
the screen and tap "confirm" on transfers — so Google Play Protect
[automatically blocks any app that declares it](https://developers.google.com/android/play-protect/warning-dev-guidance)
when it is installed from a browser, messaging app or file manager.

The dialog mentions "identity theft or financial fraud" because **that text is shown for every app with this
permission**. Play Protect only reads the permission list before installing; it has not examined what EmoJet does
with it, and has not found anything in this app. The warning is about what the permission *could* allow, which is a
fair thing to warn about — EmoJet's answer is that the code using it is public and the model never leaves your phone.

This is a [pilot Google runs in about ten countries](https://blog.google/security/how-we-kept-google-play-android-app-ecosystem-safe-2024/)
(India, Brazil, Singapore, Thailand, Vietnam, the Philippines, Hong Kong, Kenya, Nigeria, South Africa). In the rest
of the world the APK installs with no block.

There is no "Install anyway" button, and installing from F-Droid or Obtainium does not avoid it either. Your options:

| | |
|---|---|
| **Pause Play Protect** (easiest) | Play Store → profile picture → Play Protect → **⚙** → turn off "Scan apps with Play Protect" → install → **turn it back on**. The app keeps working. |
| **Install over USB** | `adb install emojet-v1.0.0-arm64.apk` from a computer — ADB installs are not blocked. |

If you would rather not do either, that is a completely reasonable decision — an app that reads your text boxes
deserves that scepticism. Every line of what it does with that permission is
[readable here](android/app/src/main/java/io/github/subhadeeproy3902/emojet/EmojetService.kt).

> **Xiaomi, Redmi and POCO owners:** also turn on **Autostart** for EmoJet (App info → Autostart). Without it HyperOS
> stops EmoJet in the background after a few days and suggestions silently stop. The app's Help page has the steps.

### Using the strip

| Action | What happens |
|---|---|
| Tap an emoji | Inserted at the cursor |
| **Hold** a person emoji (👋 🙏 👍) | Pick a skin tone — remembered from then on |
| Empty box, after swipe-to-reply | You get emojis to react to *that* message |
| Drag **⠿** | Move the strip if it covers something |
| Tap **✕** | Hide it for this message; it returns with your next word |
| **Hold ⠿** | Turn EmoJet off inside that one app |

## How much space it takes

Worth knowing before you start, because the model is the big part:

| | Size | When |
|---|---|---|
| The app (APK) | **8 MB** | at install |
| The Emojet model, 4-bit | **274 MB** | one download, over Wi-Fi |
| **Total on the phone** | **~282 MB** | |
| Memory while suggesting | ~300 MB | only while you are typing |

The model is downloaded rather than bundled, so the APK stays small and the model can be updated without a new app
version. It is checksummed (SHA-256) after download, and can also be
[imported from a file](#no-wi-fi-or-want-to-move-the-model-by-usb) if you would rather not download it on the phone.

## How it works

EmoJet runs [**Emojet v1**](https://huggingface.co/subhadeep3902/emojet) — an open model (CC BY-NC-SA 4.0) built on
[Laya](https://huggingface.co/convaiinnovations/laya) and fine-tuned on millions of real chat messages. One pass over
your sentence scores every emoji in Unicode 18.0 plus the tone of voice, in about **0.4 seconds on a mid-range phone**.

The app reads the text box through Android's accessibility service, runs the model **inside the app**
(llama.cpp compiled for arm64), ranks the results, and draws the strip above your keyboard.

Details: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) · model and training:
[subhadeeproy3902/emojet](https://github.com/subhadeeproy3902/emojet).

## Privacy

- **Nothing you type is stored, uploaded or shared.** There is no server to send it to.
- EmoJet reads **only the text box you are typing in**, plus the message you are replying to.
- It **never** reads password fields, and nothing else on your screen.
- The internet is used **twice**: to download the model once, and when you tap "Check for updates".
- No analytics, no crash reporting, no identifiers, no ads.

Accessibility permission is powerful, which is exactly why the code that uses it is
[public and readable](android/app/src/main/java/io/github/subhadeeproy3902/emojet/EmojetService.kt).

## Use Emojet on a computer

The Android app is the product; on Windows, macOS and Linux you can run the **same model** directly. It needs
[llama.cpp](https://github.com/ggml-org/llama.cpp) and the 274 MB model file:

```bash
# any OS with llama.cpp installed
huggingface-cli download subhadeep3902/emojet Emojet-v1-Q4_K_M.gguf --local-dir .
llama-server -m Emojet-v1-Q4_K_M.gguf --embedding --pooling rank -c 512 --port 8080

curl -s localhost:8080/embedding -H 'Content-Type: application/json' \
  -d '{"content":"my message: kal paper hai aur kuch nahi padha","embd_normalize":-1}'
```

Or with Python on any OS — see the [model card](https://huggingface.co/subhadeep3902/emojet#quick-start).

A browser extension (WhatsApp Web, Instagram, Gmail) is planned: [`extension/`](extension/). **iOS is not possible** —
Apple does not allow an app to read other apps' text fields.

## Build from source (Windows, macOS, Linux)

```bash
git clone https://github.com/subhadeeproy3902/emojet-ai && cd emojet-ai
# needs JDK 17+ and the Android SDK (Android Studio installs both)
bash scripts/build-android.sh debug      # macOS / Linux
cd android && gradlew.bat assembleDebug  # Windows
```

The script also builds the llama.cpp engine for arm64 the first time, which takes a few minutes. The result lands in
`android/app/build/outputs/apk/`. Details: [`scripts/README.md`](scripts/README.md).

## Troubleshooting

<details>
<summary><b>Suggestions stopped appearing after a few days</b></summary>

The phone froze EmoJet in the background. This is the most common problem, especially on Xiaomi/Redmi/POCO:

1. Open EmoJet once — that alone restarts it.
2. App info → **Battery** → No restrictions.
3. Xiaomi/Redmi/POCO: App info → **Autostart** → on, and lock EmoJet in Recents (hold its card → lock).
4. Check **Accessibility** is still switched on for EmoJet.

The app's **Help** page shows the live state of all of these.
</details>

<details>
<summary><b>The Accessibility switch is greyed out</b></summary>

Android 13+ blocks accessibility for sideloaded apps until you allow it: App info → **⋮** (top right) →
**Allow restricted settings**, then try again.
</details>

<details>
<summary><b>No strip at all, in any app</b></summary>

Check that Suggestions is on, that the app is not in the "Off in …" list (you can switch an app off by holding ⠿),
and that the model finished downloading (Advanced names the model file it is using).
</details>

<details>
<summary><b id="no-wi-fi-or-want-to-move-the-model-by-usb">No Wi-Fi, or you want to copy the model by USB</b></summary>

Download [`Emojet-v1-Q4_K_M.gguf`](https://huggingface.co/subhadeep3902/emojet/blob/main/Emojet-v1-Q4_K_M.gguf) on a
computer, copy it to the phone, then in EmoJet: **Advanced → Import a model file**. The file is accepted only if its
checksum matches.
</details>

<details>
<summary><b>A suggestion is plain wrong</b></summary>

That is a model issue, and the most useful thing you can report — it becomes a permanent test case.
Open a [bad suggestion report](https://github.com/subhadeeproy3902/emojet/issues/new?template=bad_suggestion.yml)
with the message you typed and what a real person would have sent.
</details>

## Contributing

Issues and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). Anything about **which emoji is
suggested** belongs in the [model repo](https://github.com/subhadeeproy3902/emojet), where you can also retrain the
model yourself on free hardware.

## Licence

- **App code** (this repo): [Apache-2.0](LICENSE).
- **Emojet model weights**: CC BY-NC-SA 4.0 — free and open, non-commercial
  ([model card](https://huggingface.co/subhadeep3902/emojet)).
- Third-party components and their licences: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

<div align="center">

**[Download the APK →](../../releases/latest)** · [The model →](https://huggingface.co/subhadeep3902/emojet)

</div>
