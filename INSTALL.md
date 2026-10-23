# Installing EmoJet

**Android 8.0 or newer. About 282 MB free (8 MB app + 274 MB model). No iPhone version is possible.**

---

## Step 1 — Download the app

Open the [latest release](../../releases/latest) on your phone and tap **`emojet-v1.0.0-arm64.apk`**.

## Step 2 — If Android says "App blocked to protect your device"

<img src="https://img.shields.io/badge/only_in-India_·_Brazil_·_Singapore_·_Thailand_·_Vietnam_·_Philippines_·_Hong_Kong_·_Kenya_·_Nigeria_·_South_Africa-orange" alt="affected countries">

**In most of the world this never happens — skip to step 3.** In the countries above, Google blocks *every*
sideloaded app that asks for the accessibility permission. You can still install it, in four taps:

1. Open the **Play Store**
2. Tap your **profile picture**, top right → **Play Protect**
3. Tap the **⚙ gear**, top right → turn **off** *"Scan apps with Play Protect"*
4. Install the APK → then **turn the scan back on** (EmoJet keeps working)

**Why does this happen?** EmoJet needs Android's accessibility permission — it is the only way an app can read
the text box you are typing in. Banking trojans abuse that same permission to read one-time codes off your screen,
so Google blocks the entire category when an app comes from a browser instead of the Play Store. The scary wording
about "identity theft or financial fraud" is shown for **every** app with that permission; Play Protect has not
looked at EmoJet and has not found anything in it. What EmoJet does with the permission is
[readable line by line](android/app/src/main/java/io/github/subhadeeproy3902/emojet/EmojetService.kt): it reads the
box you are typing in and the message above it, never password fields, and the AI runs entirely on your phone.

*Prefer not to touch Play Protect?* Install from a computer instead — `adb install emojet-v1.0.0-arm64.apk` is
never blocked.

## Step 3 — Allow the install

Android asks whether to allow installing apps from your browser or file manager. Allow it.
On Xiaomi, Redmi, POCO, Oppo and Vivo phones a second confirmation appears from the phone maker — accept that too.

## Step 4 — Open EmoJet and follow the three steps

| | | |
|---|---|---|
| **1** | **Download the model** | 274 MB, once. Wi-Fi or mobile data — your choice. After this EmoJet works offline forever. |
| **2** | **Turn on Accessibility** | See the box below first — Android blocks this switch for sideloaded apps. |
| **3** | **Keep it running** | A silent notification plus "no battery restrictions", so your phone does not freeze EmoJet after a few days. |

### "App was denied access" / "Controlled by restricted setting"

**Every app installed outside the Play Store hits this on Android 13 and newer.** Android hides the accessibility
switch until you unlock it, once, per app:

1. Settings → **Apps** → **Manage apps** → **EmoJet**  *(or use the "Open App info" button inside EmoJet)*
2. Tap the **⋮** in the top-right corner
3. Tap **"Allow restricted settings"**
4. Go back to Accessibility → EmoJet → the switch works now

This exists because accessibility is a powerful permission, and Android wants you to have walked to the app's own
settings page deliberately before granting it to something that did not come from a store. It is a one-time step.

## Step 5 — Xiaomi, Redmi and POCO only

Also turn on **Autostart**: App info → **Autostart** → on. Without it HyperOS stops EmoJet in the background
after a few days and the suggestions quietly disappear. It also helps to lock EmoJet in Recents (open Recents,
hold its card, tap the lock).

---

## Then just type

Open WhatsApp, Instagram, Messages — anything. Type a sentence, pause for a moment, and six emojis appear above
your keyboard. Tap one.

- **Hold** a person emoji (👋 🙏 👍) to pick a skin tone — it is remembered.
- **Drag ⠿** to move the strip, **tap ✕** to hide it for one message.
- **Hold ⠿** to switch EmoJet off inside one particular app.

## If something does not work

| Problem | Fix |
|---|---|
| Suggestions stopped after a few days | The phone froze it. Open EmoJet once; then App info → Battery → *No restrictions*, and Autostart on (Xiaomi). |
| Accessibility switch is greyed out | App info → **⋮** → *Allow restricted settings*. |
| No strip anywhere | Check Suggestions is on, the app is not in the "Off in …" list, and the model finished downloading. |
| No Wi-Fi and you do not want to use data | Download [the model file](https://huggingface.co/subhadeep3902/emojet/blob/main/Emojet-v1-Q4_K_M.gguf) on a computer, copy it to the phone, then **Advanced → Import a model file**. |

The app's own **Help** page (the **?** button, top right) shows the live state of every permission.
