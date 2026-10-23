# Contributing to EmoJet

EmoJet is the Android app. **Which emoji gets suggested** is decided by the Emojet model, which lives in
[subhadeeproy3902/emojet](https://github.com/subhadeeproy3902/emojet) — bad suggestions belong there, and you can
retrain the model yourself on free hardware.

Everything else — the strip, settings, onboarding, permissions, performance, battery — belongs here.

## Build it

You need **JDK 17+** and the **Android SDK** (Android Studio installs both). Works on Windows, macOS and Linux.

```bash
git clone https://github.com/subhadeeproy3902/emojet-ai && cd emojet-ai
bash scripts/build-android.sh debug          # macOS / Linux
cd android && gradlew.bat assembleDebug      # Windows
```

The first build also compiles the llama.cpp engine for arm64 (a few minutes). Install with:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

To put a model on the phone without downloading it there:

```bash
bash scripts/phone-install.sh path/to/Emojet-v1-Q4_K_M.gguf
```

## How the app is put together

| File | What it does |
|---|---|
| `EmojetService.kt` | the accessibility service: reads the text box, decides when to suggest, draws the strip |
| `Ranker.kt` | turns the model's 2,002 scores into six emojis (reply blending, tone boost, topic guarantee, offensive-emoji guard) |
| `Engine.kt` | runs llama.cpp inside the app on a local socket |
| `ModelManager.kt` | download, checksum, import, upgrade |
| `MainActivity.kt` / `OnboardingActivity.kt` / `HelpActivity.kt` | home + setup, first run, troubleshooting |
| `Theme.kt` | all colours, shapes and text styles |

There is **no XML layout and no UI framework** — screens are built in Kotlin with the helpers in `Theme.kt`.
Please keep it that way; it is what keeps the APK at 8 MB.

## Things to be careful about

- **The accessibility service is the sensitive part.** It must keep reading only the focused text box and the
  message being replied to — never password fields, never anything else. Any PR that widens that will be asked
  hard questions.
- **No analytics, no crash reporting, no network calls** beyond the model download and the explicit update check.
- **Phones kill background apps.** Anything that stops the service from staying alive (or stops it restarting)
  is a serious bug on Xiaomi/Oppo/Vivo devices.
- **Test on a real phone.** The strip is an overlay over other apps; an emulator will not show you the problems.

## Reporting

- App bug: [bug template](../../issues/new?template=bug.yml) — include phone, Android version and app version.
- Idea: [feature template](../../issues/new?template=feature.yml).
- Wrong emoji: [model repo](https://github.com/subhadeeproy3902/emojet/issues/new?template=bad_suggestion.yml).

## Licence

App code is Apache-2.0 and contributions are taken under the same licence. The model weights are CC BY-NC-SA 4.0.
