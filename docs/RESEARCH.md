# Research notes

## Jev vs Laya

[emoji-jev](https://github.com/colinmcdermott/emoji-jev) is the inspiration: a real-time emoji autocomplete that answers several "typed questions" about your draft text in one fast model call — emoji choice, primary emotion, mood/urgency/energy scores, sarcasm/joke/reply-wanted booleans — in ~100ms. It runs on a hosted model called **Jev** (TypeSafe AI), billed per call (~$0.0002/keystroke via `TYPESAFE_AI_API_KEY` or the Vercel AI Gateway). Not usable for a zero-cost project.

[Laya](https://huggingface.co/ggml-org/Laya-GGUF) (convaiinnovations, Apache-2.0) is the free, open equivalent: a **non-autoregressive "System 1" decision model** that answers typed questions (choice / score / yes-no) over text in a single forward pass. Same core trick as Jev, no billing, runs fully offline once downloaded.

Checkpoints:

| Model | Base | Params | Context |
|---|---|---|---|
| `laya` | ModernBERT-large | 421M | 512 |
| `laya-multilingual` | mmBERT-base | 322M | 1024 (up to 8192) |
| `laya-typed-decisions` | ModernBERT-large | 421M | 1024 |

Why non-autoregressive matters for speed: a chat LLM of the same size would generate output **token by token** (N forward passes for N output tokens). Laya does **one forward pass per query**, regardless of how many typed questions you ask it — that's what makes a ~400M model plausible for a real-time UI, unlike a same-size generative model.

## Available artifacts

- **`ggml-org/Laya-GGUF`**: `Q8_0` = 449MB, `BF16` = 844MB. Runs via `llama.cpp`, which has a **purpose-built `/v1/systemone` endpoint** specifically for Laya's typed-decision interface — not a generic chat endpoint. This is the artifact we're building on.
- **`desplega/laya-onnx`**: fp32 only, 1.3–1.7GB, Node/Bun-only (via `laya-js`). Too large and wrong runtime target for mobile/browser — not used.
- **`laya.tools/platform/gguf`**: a showcase of ~27 community projects built on the GGUF model (Rust HTTP servers, a desktop workbench app, an npm package). None target Android or a browser — EmoJet is the first mobile/browser integration of Laya, as far as this research found. No existing project to fork from for that piece; we're building the JNI/WASM bridges ourselves.

## Decision

Use Laya GGUF only (`Q8_0`, with `Q4_K_M` self-quantized as a fallback if size/speed demands it). Everything downstream (Android JNI binding, browser WASM binding, the typed-decision prompt schema) is new work, not a fork of an existing integration.
