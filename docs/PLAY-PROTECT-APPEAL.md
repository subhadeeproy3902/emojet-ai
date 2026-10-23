# Play Protect appeal — EmoJet

Google Play Protect blocks every sideloaded app that declares `BIND_ACCESSIBILITY_SERVICE`, in the ~10 countries
where its [fraud-protection pilot](https://blog.google/security/how-we-kept-google-play-android-app-ecosystem-safe-2024/)
runs (India, Brazil, Singapore, Thailand, Vietnam, the Philippines, Hong Kong, Kenya, Nigeria, South Africa).
There is no "install anyway" button and no packaging change that avoids it.

The only official route to have the block lifted for this app is Google's
[Play Protect appeal form](https://support.google.com/googleplay/android-developer/contact/protectappeals).
Note before spending time on it: the form states **"All appeal decisions are final and you will not receive a
response."** You find out by re-testing an install later.

## How to submit (about 5 minutes)

**Step 1 — put the APK on VirusTotal** (the form requires a hash that exists there).

Go to <https://www.virustotal.com/gui/home/upload>, drag in `emojet-v1.0.0-arm64.apk` from the
[release](https://github.com/subhadeeproy3902/emojet-ai/releases/tag/v1.0.0), and wait for the scan.
No account is needed. The APK is already public, so this discloses nothing new.

**Step 2 — fill in the form** at <https://support.google.com/googleplay/android-developer/contact/protectappeals>

| Field | Value |
|---|---|
| Email address | your email |
| Developer name | Subhadeep Roy |
| Application package name | `io.github.subhadeeproy3902.emojet` |
| SHA256 hash of the apk ... on Virus Total | `437c07d8295967ec3c6886997f88c92f57ef37e7a62dd235e8e6c392d8bd5fbf` |
| Additional information | the text below |

## Text for "Additional information"

> EmoJet is an open-source Android app that suggests emojis for the message the user is currently typing. It is
> published at https://github.com/subhadeeproy3902/emojet-ai and the full source of the accessibility service is
> readable at
> https://github.com/subhadeeproy3902/emojet-ai/blob/main/android/app/src/main/java/io/github/subhadeeproy3902/emojet/EmojetService.kt
>
> Why AccessibilityService is used: it is the only Android API that exposes the text field the user is typing in
> inside another app. There is no alternative API for this feature.
>
> What the service reads:
> 1. the text of the currently focused editable field, and
> 2. the message being replied to, found as the nearest readable text above that field in the same window.
>
> It checks `isEditable` and skips any node where `isPassword` is true, so password fields are never read. Nothing
> is read from other apps, other screens, or the background.
>
> What it does NOT do: it performs no autonomous actions. It never clicks, types, navigates, scrolls or dismisses
> anything. Its only write is inserting an emoji into the focused field after the user taps that emoji in the app's
> own suggestion strip.
>
> Where the data goes: nowhere. Inference runs on the device with llama.cpp bundled in the APK, against a model
> file in the app's own external files directory. Nothing typed is stored, logged or transmitted. There is no
> analytics SDK, no crash reporting, no advertising SDK, and the developer operates no server. The INTERNET
> permission is used only to download the model file once from Hugging Face
> (https://huggingface.co/subhadeep3902/emojet) and to read a version manifest when the user taps
> "Check for updates".
>
> Disclosure: on first run the app shows a screen stating exactly what it reads and what it never reads, before the
> user is asked to enable accessibility.
>
> The app is a text-input helper, not a financial or banking-adjacent app, and it requests no SMS, call-log,
> contacts, notification-listener or location permissions.

## Reducing the risk profile (recommended, independent of the appeal)

The APK currently also declares **`REQUEST_INSTALL_PACKAGES`**, used by the in-app "Update now" button to hand a
downloaded APK to Android's installer. Accessibility + install-packages together is the permission pair that
dropper malware uses, so it makes the app look worse to any scanner than it is.

Dropping it costs almost nothing: "Update now" can open the GitHub release page in the browser instead, and the
user installs from there. This does not remove the Play Protect block (that is triggered by accessibility alone),
but it is the right call for an app asking for this much trust.
