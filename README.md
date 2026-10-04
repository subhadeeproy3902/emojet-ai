# EmoJet

Free, offline, real-time emoji suggestions — as an overlay on Android (works over WhatsApp, Instagram, SMS, anywhere you type) and as a browser extension (WhatsApp Web, Instagram, Snapchat, Gmail, anywhere with a text field).

Powered entirely by **[Laya](https://huggingface.co/ggml-org/Laya-GGUF)** — an open, Apache-2.0, non-autoregressive "typed decision" model, and the free alternative to the paid [Jev](https://github.com/colinmcdermott/emoji-jev) model this idea was inspired by. No API keys, no backend server, no account, no cost. Every suggestion is computed on your own device.

## Why this exists

Real-time emoji suggestion tools exist, but the good ones (like Jev) bill per keystroke through a hosted API. EmoJet reuses the same "answer several typed questions about the text in one fast forward pass" trick, but runs it locally with Laya, so the whole thing costs $0 to run — for you and for anyone else who installs it.

## How it's distributed

No app stores (no Play Store listing, no Chrome Web Store listing) — intentionally, to keep this at zero cost and zero review friction:
- **Android**: a signed APK, sideloaded.
- **Browser**: an unpacked extension folder (`chrome://extensions` → Load unpacked, or `about:debugging` on Firefox), or a zipped release you download and unpack.

## Status

Early planning stage. See [`docs/PLAN.md`](docs/PLAN.md) for the full phase-by-phase roadmap, [`docs/RESEARCH.md`](docs/RESEARCH.md) for why Laya was chosen over alternatives, and [`docs/TESTING.md`](docs/TESTING.md) for the test manual.

## License

MIT — see [`LICENSE`](LICENSE). Laya itself is Apache-2.0; llama.cpp is MIT.

## Contributing

Open source, contributions welcome once the first working prototype lands (see Phase 0/1 in the plan).
