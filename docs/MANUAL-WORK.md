# Manual work — what only you can do

Claude does all the coding, building, local installs in your home folder, testing on the laptop, commits and pushes. What's left is in this file: things that need your **password (sudo)**, your **phone**, your **accounts**, or **your taste**. Tick them off as you go and tell Claude.

## A. Right now (unblocks Phase 1)

- [x] **1. Tell Claude your phone** — Redmi Note 17 Pro (Claude reads chip/RAM/Android version over adb once it's connected). The APK itself targets *any* Android 8+ phone.
- [x] **2. Install the Android basics** (done 2026-10-04):
  ```
  ! sudo pacman -S --needed jdk21-openjdk android-tools android-udev
  ```
  - `jdk21-openjdk` — Java, needed by the Android build tools (Gradle, sdkmanager).
  - `android-tools` — `adb`, to install the test app on your phone over USB.
  - `android-udev` — lets your normal user talk to the phone over USB (no root).

  After this, Claude installs the Android SDK + NDK into `~/Android/Sdk` itself (Google's official SDK download into your home folder, ~4 GB, no sudo, nothing system-wide).
- [ ] **3. Phone → developer mode (Redmi / Xiaomi HyperOS)**:
  1. Settings → About phone → tap **OS version** 7 times (on Xiaomi it's "OS version", not "Build number").
  2. Settings → Additional settings → **Developer options**:
     - turn on **USB debugging**;
     - turn on **Install via USB** (Xiaomi-only; it asks you to be signed in to a Mi account and may need a SIM card inserted — without it `adb install` fails with `INSTALL_FAILED_USER_RESTRICTED`);
     - optional: **USB debugging (Security settings)** — only needed if Claude later automates taps for tests.
  3. Plug the phone in with a data-capable USB cable, unlock it, choose **File transfer** if asked, accept **"Allow USB debugging?"** (tick "Always allow from this computer").
  4. Tell Claude; Claude checks with `adb devices`.

  Other Android brands: Settings → About phone → tap **Build number** 7 times, then the same USB debugging switch.

### Xiaomi extras for the overlay (Phase 2, listed now so they're not a surprise)

On HyperOS, an overlay app needs more than stock Android: Settings → Apps → EmoJet → **Other permissions** → allow **Display pop-up windows while running in the background** and **Display pop-up window**; **Autostart** on; Battery → **No restrictions**. EmoJet's setup screen will link to each of these.

## B. Decisions only you can make (any time)

- [x] **Instant keyword layer** — decided 2026-10-04: **no**. Suggestions always come from the whole sentence, never from single words.
- [ ] **Review `shared/moods.json`**: are the 25 moods the right ones? Are the emoji rows the ones *you* use? (e.g. 💀 for laughing). Edit freely — the test score will tell us if a change hurt.
- [ ] **Write ~50 messages the way you text** (English + Hinglish, short and long, with the emoji you'd add). Only your own words — no screenshots of other people's chats. This becomes the real test set; 23 samples is too few to tune on.
- [ ] **Tone examples in your voice**: for each communication style (assertive, passive, aggressive, passive-aggressive) and the hard tones (sarcastic, condescending, defensive, cynical, accusatory), write 5–10 messages the way you or your friends would actually text them. No free dataset covers these well; your examples become test data and seeds. Template: [`shared/tones.json`](../shared/tones.json) has one example per style.
- [ ] **Tone chip / tone check** (Phase 4 ideas): yes / no / later.
- [ ] **Where Laya-Emoji gets trained** (Phase T): (A) this laptop's RTX 2050 — needs `sudo pacman -S python-pytorch-cuda python-transformers python-safetensors python-datasets` (~5 GB) and runs the GPU hot for 1–3 h per run; or (B) free Kaggle GPU — needs a free Kaggle account (phone-verified), you press Run on a notebook Claude writes. Recommended: B, with a small local pilot first. Details: [TRAINING.md](TRAINING.md).
- [ ] **Model size** after Phase 1: Claude will give you size / speed / accuracy for the 8-bit and 4-bit files; you pick.
- [ ] **Icon / colours** for the app and extension (or say "you choose").

## C. Hands-on tests (details in [TESTING.md](TESTING.md))

- [ ] Phase 1: run the "EmoJet Lab" test app on your phone, read out the numbers.
- [ ] Phase 2: a day of real use in WhatsApp / Instagram / SMS with the overlay.
- [ ] Phase 3: real use of the extension on WhatsApp Web / Instagram / Snapchat / Gmail.

## D. Later: Phase 3 (extension engine)

- [ ] ```
  ! sudo pacman -S --needed emscripten
  ```
  Compiler that turns the C++ Laya engine into WebAssembly for the browser.

## E. Release time

- [ ] **Back up the APK signing key.** Claude generates it (`keytool`); you copy the keystore file + its password somewhere off the laptop (password manager, USB stick). If it's lost, people who installed EmoJet must uninstall before they can update. It never goes into git.
- [ ] **Say "go" for each public GitHub Release** (APK + extension zip). Claude prepares it; publishing is a public action, so it waits for your OK.
- [ ] **Firefox only:** normal Firefox can't keep an unsigned extension after a restart. Free fix: Mozilla signs it as "unlisted" (not a store listing, still $0) — needs a free Mozilla account + API key from you. Chrome / Edge / Brave: "Load unpacked" stays installed, nothing needed.
- [ ] **Hugging Face account — only if** we publish our own model file. Steps in [HUGGINGFACE.md](HUGGINGFACE.md). Not needed otherwise.

## F. What every user (including you) does when installing the APK

Put this in the release notes / `docs/INSTALL.md` later.

1. Allow "Install unknown apps" for the browser / file manager used to open the APK.
2. Play Protect may warn → **More details → Install anyway**.
   - India note: since late 2024 Google has run an "enhanced fraud protection" pilot in India that can **block** sideloaded apps asking for Accessibility when they're installed from a browser, messaging app or file manager. Installing with `adb install` is not affected. If a user is blocked: install via a computer with adb, or temporarily turn off Play Protect scanning. **To verify on your phone in Phase 2.**
3. Android 13+: the Accessibility switch for a sideloaded app is greyed out ("Restricted setting"). Fix: Settings → Apps → EmoJet → ⋮ (top right) → **Allow restricted settings**, then turn EmoJet on under Accessibility.
4. Allow **Display over other apps**.
5. Battery → set EmoJet to **Unrestricted** (Xiaomi, Oppo, Vivo, OnePlus, Samsung kill background services aggressively; see dontkillmyapp.com).
6. First start: the model downloads once (~0.25–0.45 GB, Wi-Fi).

## G. Watch item: Google developer verification

Google announced (Aug 2025) that apps on certified Android phones must come from a **registered, verified developer**, even when sideloaded: first in Brazil, Indonesia, Singapore and Thailand from September 2026, other countries from 2027. `adb` installs are exempt, and Google said there would be a free option for students/hobbyists. When it reaches India it may add a (hopefully free) registration step for public releases. Claude's information here is from mid-2026 — check the current status before the first public release.
