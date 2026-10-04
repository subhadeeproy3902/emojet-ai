# EmoJet — Browser extension

Not started yet — Phase 3 (see [`../docs/PLAN.md`](../docs/PLAN.md)).

Plan ([`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md)): Manifest V3; a content script that works on any text field (Shadow-DOM suggestion strip, `insertText`-based insertion for editors like WhatsApp Web's); the Laya engine ("emojet-core", the decision code from `llama-server` + libllama) compiled to WebAssembly and run in an offscreen document; model stored in OPFS after one download. Optional fast path: a local `llama-server` if the user runs one.
