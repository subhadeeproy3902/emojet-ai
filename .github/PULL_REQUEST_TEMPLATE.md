## What this changes

<!-- One or two sentences. "Fixes #123" if it closes an issue. -->

## Type

- [ ] Bug fix
- [ ] New feature or UI change
- [ ] Performance
- [ ] Documentation
- [ ] Build / tooling

## Tested on

| | |
|---|---|
| Phone and Android version | |
| Fresh install or upgrade | |
| Apps tried (WhatsApp, Instagram, …) | |

- [ ] The app builds: `bash scripts/build-android.sh debug`
- [ ] I installed it on a real phone and the strip still appears
- [ ] No crash in `adb logcat -s EmoJet AndroidRuntime`

## If it touches the suggestion strip or the service

- [ ] Empty text box shows nothing
- [ ] The strip still follows the text box when the keyboard opens and closes
- [ ] It still works after turning Accessibility off and on again

## Checklist

- [ ] No model files, keystores or `.env` committed
- [ ] No analytics, trackers or network calls added (the app only talks to the model on the device)
