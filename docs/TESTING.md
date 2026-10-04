# Test manual

Split into what can be verified without a phone/browser in the loop (Claude can run these directly) and what genuinely needs you (hardware, permissions, or a human judgment call on "does this feel good").

## What Claude can test directly

These don't need your phone or a live browser session — they can be run and checked from the dev machine:

- [ ] `scripts/dev-server.sh` starts `llama.cpp server` cleanly and `/v1/systemone` responds.
- [ ] `scripts/test-prompts.*` runs all sample sentences and returns well-formed JSON (right schema, no crashes, no timeouts) for every one.
- [ ] Quantization command (`llama-quantize`) produces a valid `Q4_K_M` file from the source checkpoint, and that file loads without error.
- [ ] Android project builds (`./gradlew assembleDebug`) without errors.
- [ ] Extension builds/lints cleanly (`web-ext lint`, or the Vite/CRXJS build) with no manifest or script errors.
- [ ] Unit tests for the debounce/suppression logic (the "clause changed vs unchanged" rule) — pure logic, testable without any device.
- [ ] Unit tests for the reply-context prompt assembly (given a fake "replying to X" + "draft Y", confirm the right prompt is built).

Claude will run these as each phase's code lands and report pass/fail before asking you to do the hands-on part.

## What needs you

These require real hardware, interactive permission grants, or subjective judgment — Claude cannot do these remotely:

### Phase 0 — model quality
- [ ] Run the local `llama.cpp server`, fire the test harness, read the 20 sample outputs yourself.
- [ ] Judge: are the top-3 emoji suggestions actually what you'd pick, for at least 15/20 samples?

### Phase 1 — Android latency
- [ ] Install the debug APK on your phone via `adb`.
- [ ] Run the same 20 test sentences through the on-device model (via the prototype's test screen).
- [ ] Report: ms per suggestion, RAM used, any noticeable heat/battery drain over ~5 minutes of continuous testing.
- [ ] Judge: does it feel instant after a typing pause, or does it feel laggy?

### Phase 2 — Android overlay, real-world use
- [ ] Grant the Accessibility permission (one-time, interactive).
- [ ] Open real WhatsApp, Instagram, and SMS conversations; type normally for at least one full day of real use.
- [ ] Confirm: suggestions never appear over a password field.
- [ ] Confirm: suggestions keep appearing across multiple consecutive emoji-heavy messages (the "heavy emoji user" case) and don't get stuck suppressed.
- [ ] Confirm: reply-aware suggestions feel noticeably better than draft-only, on at least a few real replies.
- [ ] Confirm: toggling off in settings immediately stops the overlay, with no lingering permission weirdness.

### Phase 3 — Browser extension, real-world use
- [ ] Load unpacked in your main browser, test on WhatsApp Web, Instagram, Snapchat, and at least one other site (Gmail, Discord web, etc.) to confirm the "anywhere" claim holds.
- [ ] Confirm the model downloads once, then works fully offline afterward (test with Wi-Fi off after first load).
- [ ] Confirm per-site quick-disable works without disabling the whole extension.

### Ongoing
- [ ] Spot-check the "0 network calls" claim yourself occasionally (browser devtools Network tab / Android network monitor) — it's the core trust claim of the whole project, worth verifying it stays true as features get added.

## Reporting format

When you run a [You] item, a one-line result back to Claude is enough to keep the plan moving, e.g.: "Phase 1: ~180ms per suggestion, 310MB RAM, no noticeable heat — feels fine." That's enough to decide whether to move to Phase 2 as-is or adjust quant level first.
