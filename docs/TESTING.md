# Test manual

Two kinds of tests: **automatic** ones Claude runs on the laptop after every change, and **hands-on** ones that need your phone, your browser and your judgement. Each phase ends only when both pass.

## 1. Automatic tests (Claude)

### 1.1 Model quality + speed — every change to `moods.json`, prompt wording or model file

```bash
bash scripts/dev-server.sh &              # Q4_0, 4 threads, 127.0.0.1:8080
python3 scripts/test-prompts.py           # score + latency
python3 scripts/test-prompts.py --no-reply   # proves reply context still helps
pkill -x llama-server
```

Pass criteria (current baseline in brackets, 2026-10-04):

| Check | Must be | Baseline |
|---|---|---|
| Top-3 mood correct | ≥ baseline, target ≥ 85% | 22/23 (96%) |
| Top-1 mood correct | ≥ baseline − 1 | 18/23 |
| Median latency, laptop, 4 threads | ≤ 250 ms | 183 ms |
| Reply samples with context | 3/3 top-1 | 3/3 |
| Real memory (RssAnon) of `llama-server` | ≤ 300 MB | 266 MB |

Tone/style check — `python3 scripts/test-tones.py` (same server). Baseline, untrained Laya: tone 14/25 first pick, 18/25 top 3; style 6/13. Laya-Emoji must beat it, especially on sarcastic / passive-aggressive / condescending / accusatory.

When your ~50 messages arrive they replace the 23 samples as the main score (the 23 stay as a regression set).

### 1.2 Checksums — every model download / new quant
- `sha256sum models/*.gguf` matches the table in `HUGGINGFACE.md` (official files) or the value recorded when we made the quant.

### 1.3 Android (from Phase 1)
- `./gradlew assembleDebug` succeeds; `./gradlew test` (unit tests) passes.
- Unit tests: suggestion blend (same output as `suggest()` in the Python script for fixed inputs), debounce/dedup rule (unchanged clause → no new request; new word after an emoji → new request), reply-context prompt building, password-field skip.
- `llama-server` arm64 binary: `file` says ARM aarch64; it starts under `qemu-aarch64` or on the phone and answers `/health`.

### 1.4 Extension (from Phase 3)
- emojet-core (native build) gives the **same probabilities** as `llama-server` for the sample set (tolerance 1e-3).
- WASM build gives the same answers as the native build.
- `pnpm build` + `web-ext lint` pass; the zip contains the licences.
- Insert test against a local HTML page with `textarea`, `input`, a plain `contenteditable` and a Lexical editor (WhatsApp Web uses Lexical).

## 2. Hands-on tests (you)

Tick each, and send Claude a one-line result per phase, e.g. *"Phase 1: 420 ms median, 4 threads, no heat, feels OK"*.

### Phase 0 — quality (laptop)
- [ ] Read the output of `python3 scripts/test-prompts.py`. For each line: would *you* use one of the 6 emojis shown? Note the ones where you wouldn't.
- [ ] Write your ~50 messages (MANUAL-WORK §B).

### Phase 1 — speed on your phone
- [ ] Phone plugged in with USB debugging on; Claude installs "EmoJet Lab".
- [ ] Tap **Benchmark**; screenshot the result (median ms, model, threads).
- [ ] Type a few of your own messages in the Lab text box; does the row feel right and quick?
- [ ] After ~5 minutes of use: phone noticeably warm? battery drop?

### Phase 2 — overlay in real apps
- [ ] Install the APK, go through every permission screen (restricted settings, accessibility, display over apps, battery unrestricted). Note any screen that was confusing.
- [ ] WhatsApp: type 10+ messages in 2 chats. Strip appears after a pause, near the field, never covering the Send button.
- [ ] Tap inserts at the cursor (also in the middle of the text), not at the end.
- [ ] Multi-emoji: type text, tap an emoji, tap another, keep typing → suggestions keep coming.
- [ ] Reply: swipe-reply to a message, type "yes!!" → row fits the replied message.
- [ ] Instagram DMs, SMS, one more app of your choice: same checks.
- [ ] Password field (any login screen): **no strip, ever**.
- [ ] Toggle off (tile / notification / app): strip disappears immediately; on again works.
- [ ] Leave it a full day: anything annoying? battery use in Settings → Battery?
- [ ] Airplane mode after model download: still works (proves no network needed).

### Phase 3 — extension
- [ ] Load unpacked (Chrome/Brave/Edge); model downloads once with progress.
- [ ] WhatsApp Web, Instagram web DMs, Snapchat web, Gmail compose: strip appears, insert works, the site's own editor doesn't break (no lost text, Enter still sends).
- [ ] Per-site disable works; global off works.
- [ ] Wi-Fi off after the first download: still works.
- [ ] DevTools → Network while typing: **no requests** from the extension.

### Every release
- [ ] Fresh install on a second phone / browser profile following `docs/INSTALL.md` exactly — any step that's unclear is a bug in the guide.
