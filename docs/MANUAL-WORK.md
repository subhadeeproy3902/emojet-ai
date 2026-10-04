# Manual work — what only you can do

Claude does all the coding, building, local installs in your home folder, testing on the laptop, commits and pushes. What's left is in this file: things that need your **password (sudo)**, your **phone**, your **accounts**, or **your taste**. Tick them off as you go and tell Claude.

## ★ Before the next build session — do these, in this order

1. ✅ **Phone ready** (done 2026-10-05: USB debugging + Install via USB on, signed in). (§A step 3): OS version ×7 → Developer options → USB debugging **on** → plug in → "Allow USB debugging?" → Always allow. Leave it plugged in when the session starts.
   - **Install via USB** is optional. It needs a Xiaomi account: check Settings (top of the list) — if already signed in, turn it on; if not, either create a free one (Settings → Xiaomi Account → Create account, phone number + SMS) or skip it.
   - Without it, Claude copies the APK to the phone over the cable (`adb push`) and you tap it in Files → Downloads → Install. Same path real users take.
   - ⚠️ Never sign out of / reset a phone signed in to a Xiaomi account whose password you don't know (Find Device lock).
2. ✅ **Kaggle account** (training, $0) — done 2026-10-05: account `subhadeep3902`, token in `.env` (`KAGGLE_API_TOKEN=KGAT_…`, `KAGGLE_USERNAME`), git-ignored, login verified.
   1. Sign up at kaggle.com (Google sign-in or email).
   2. Settings → **Phone verification** (Kaggle only gives free GPUs to verified accounts).
   3. Settings → **API** → **Create New Token** — your browser downloads `kaggle.json`. Leave it in `~/Downloads`; Claude moves it to `~/.config/kaggle/` and locks it (it's a password — never share it in chat, never in git).
3. **Your messages file** — create `/mnt/shared/EndeavourOS/emojet/data/my-messages.txt` (the `data/` folder is never uploaded, so it stays private). One message per line, `|` between fields:
   ```
   bhai kya scene hai aaj party karte hain | 🎉🥳🍻 | enthusiastic, playful
   it's fine. really. I'm fine. | 🙂🙃 | bitter | passive-aggressive
   > do you want to come to the concert with me?
   yes!! | 🎉🤩 | enthusiastic
   ```
   Fields: message | emojis you'd add | tones (optional) | style (optional). A line starting with `>` is the message you're replying to (applies to the next line). Aim for ~50 everyday messages + 5–10 per style and per hard tone (sarcastic, passive-aggressive, condescending, defensive, cynical, accusatory). Partial is fine — start with 20.
4. **Quick answers** (one line each): tone chip yes/no/later · tone check before sending yes/no/later · their-tone yes/no/later · moods.json rows OK or changes.
5. **Start the session**: open Claude Code in `/mnt/shared/EndeavourOS/emojet` (or your home folder) and say: *"Continue EmoJet — start Phase 1 and Phase T."* Claude reads `CLAUDE.md` + `docs/PLAN.md` and picks up from there.

Nothing to install with sudo for this step. (Kaggle's command-line tool is only in the AUR; Claude will review its PKGBUILD or use a small script instead — decided next session.)

## A. Right now (unblocks Phase 1)

- [x] **1. Tell Claude your phone** — Redmi Note 17 Pro (Claude reads chip/RAM/Android version over adb once it's connected). The APK itself targets *any* Android 8+ phone.
- [x] **2. Install the Android basics** (done 2026-10-04):
  ```
  ! sudo pacman -S --needed jdk21-openjdk android-tools android-udev
  ```
  - `jdk21-openjdk` — Java, needed by the Android build tools (Gradle, sdkmanager).
  - `android-tools` — `adb`, to install the test app on your phone over USB.
  - `android-udev` — lets your normal user talk to the phone over USB (no root).

  Done: Claude installed the Android SDK 36 + NDK r30 into `~/Android/Sdk` (2.7 GB, no sudo).
- [x] **3. Phone → developer mode (Redmi / Xiaomi HyperOS)** — done 2026-10-05:
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
- [x] **Where Laya-Emoji gets trained** — decided 2026-10-05: **Kaggle** (free GPU). Needs the account + token from ★ step 2.
- [x] **Model delivery** — decided 2026-10-05: small APK, model downloaded on first launch.
- [x] **Updates** — decided 2026-10-05: from inside the app / extension, no uninstall ([WORKFLOWS.md](WORKFLOWS.md) §4).
- [ ] **Model size** after Phase 1: Q4_0 (252 MB) is the current pick; confirm after the phone test.
- [x] **Icon** — owner's logo + favicons (`assets/brand/`).

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
