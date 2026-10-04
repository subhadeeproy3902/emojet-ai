<p align="center"><img src="assets/brand/emojet-logo-256.png" width="160" alt="EmoJet logo"></p>

<h1 align="center">EmoJet</h1>

Free, offline, real-time emoji suggestions while you type — as an overlay on Android (WhatsApp, Instagram, SMS, any app) and as a browser extension (WhatsApp Web, Instagram, Snapchat, Gmail, any site).

The only AI inside is **[Laya](https://huggingface.co/convaiinnovations/laya)** — an open (Apache-2.0) decision model and the free alternative to the paid [Jev](https://github.com/colinmcdermott/emoji-jev) API that inspired this project. No API keys, no server, no account, no cost. Every suggestion is computed on your own device.

## How it works (one paragraph)

Laya reads your draft (and the message you're replying to) and picks its **mood** from ~25 word labels — `laughing`, `grateful`, `tired`, `food` … — in a single forward pass. EmoJet turns the top moods into a row of 6 emojis ([`shared/moods.json`](shared/moods.json)). Asking Laya for emojis directly doesn't work (emojis are too many tokens for its option budget); moods do. Details: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Status — Phase 0 (laptop) + Phase 1 (phone) done

| | Result (CPU, 4 threads) |
|---|---|
| Right mood first try / in top 3 | 18/23 · 22/23 |
| Time per suggestion | ~183 ms laptop · **~306–356 ms on a REDMI Note 17 Pro 5G** (phone CPU, 4 threads) |
| Model file | 252 MB (Laya, 4-bit Q4_0) |
| Memory | ~265 MB |
| Reply-aware | 3/3 reply cases right with context, 0/3 without |

Phase 1 (phone speed) passed. Next: Phase T — train **Laya-Emoji**, a fine-tuned Laya that reads the whole message and scores all 1,923 distinct emojis of Unicode 18.0 (3,963 with skin tones) in one pass, plus the **tone of voice** (4 communication styles, 46 tones — sarcastic, passive-aggressive, empathetic, …) so emojis match how you're saying it ([docs/TRAINING.md](docs/TRAINING.md)). Full roadmap: [docs/PLAN.md](docs/PLAN.md). What the owner has to do by hand: [docs/MANUAL-WORK.md](docs/MANUAL-WORK.md).

## Try the engine on a laptop

Needs llama.cpp built from source (Laya support landed 2026-10-02) — see [docs/PLAN.md](docs/PLAN.md#prerequisites).

```bash
bash scripts/get-model.sh BF16 && bash scripts/quantize.sh Q4_0   # or just: bash scripts/get-model.sh  (449 MB Q8_0)
bash scripts/dev-server.sh &
python3 scripts/test-prompts.py
```

## Distribution

No app stores, on purpose (zero cost, zero review):
- **Android**: APK from GitHub Releases, sideloaded.
- **Browser**: zip from GitHub Releases → unzip → `chrome://extensions` → Developer mode → Load unpacked.

## Docs

| | |
|---|---|
| [PLAN](docs/PLAN.md) | phases, status, who does what |
| [WORKFLOWS](docs/WORKFLOWS.md) | the big picture: making the model, installing, typing, in-app updates |
| [ARCHITECTURE](docs/ARCHITECTURE.md) | how the Android app and extension work |
| [TRAINING](docs/TRAINING.md) | Laya-Emoji: covering all emojis from the whole sentence |
| [MANUAL-WORK](docs/MANUAL-WORK.md) | the owner's checklist (sudo, phone, accounts, decisions) |
| [TESTING](docs/TESTING.md) | automatic tests + hands-on test manual |
| [HUGGINGFACE](docs/HUGGINGFACE.md) | what we need from Hugging Face (spoiler: no account) |
| [RESEARCH](docs/RESEARCH.md) | Jev vs Laya, experiments and numbers |

## License

MIT — see [`LICENSE`](LICENSE). Laya is Apache-2.0, llama.cpp is MIT — see [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
