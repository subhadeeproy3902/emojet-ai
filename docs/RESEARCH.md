# Research notes

## 1. Jev vs Laya

[emoji-jev](https://github.com/colinmcdermott/emoji-jev) is the inspiration: real-time emoji autocomplete that asks several typed questions about your draft in one model call (emoji choice from 64/128/254, emotion, mood/urgency/energy scores, sarcasm/joke/reply-wanted). It runs on **Jev**, a hosted model from TypeSafe AI, billed per call (~$0.0002 per keystroke). Not usable at $0.

[Laya](https://huggingface.co/convaiinnovations/laya) (convaiinnovations, Apache-2.0) is the open equivalent: a **non-autoregressive decision model** (ModernBERT encoder + decision head, trained with RL against proper scoring rules) that answers typed questions (`choice` / `score` / `noul` yes-no) in one forward pass per question. No billing, runs offline.

| Checkpoint | Base | Params | GGUF available |
|---|---|---|---|
| `laya` | ModernBERT-large | 421M | **yes** (`ggml-org/Laya-GGUF`) |
| `laya-multilingual` | mmBERT-base | 322M | no (safetensors only) |
| `laya-typed-decisions` | ModernBERT-large | 421M | no (safetensors only) |

## 2. Runtime: llama.cpp

- Laya support (+ julia-1, lev, openjev, kev) was added to llama.cpp in [PR #29818](https://github.com/ggml-org/llama.cpp/pull/29818), merged **2026-10-02**. It's newer than the Arch `llama-cpp` package, so llama.cpp is built from source.
- API: `POST /v1/systemone` on `llama-server`, compatible with the TypeSafe (Jev) API: `{state, questions: {id: {type, instructions, criteria}}}` → `{answers: {id: {choice, probabilities, confidence}}}`.
- Laya: up to **255 options** per choice question; each question is a separate forward pass (`n_variants = 1`); options + question share a **192-token budget** (`modern-bert.decision.max_head_tokens`), longer ones are cut evenly "the same way the model was trained".
- The prompt is `[cls] question [sep] ([mask] option)* [sep] state [sep]`; it's an encoder, so no prefix/KV caching between requests — cost is linear in prompt length.
- The decision logic lives in `tools/server/server-decision.cpp` (~800 lines), not in libllama → see ARCHITECTURE for how each client embeds it.
- Other Laya artifacts: `desplega/laya-onnx` (fp32 only, 1.3–1.7 GB, Node/Bun) — not used. `laya.tools/platform/gguf` lists ~27 community projects (servers, a desktop app, an npm package); none target Android or browsers.

## 3. Phase 0 experiments (2026-10-04, laptop: i5-12450H, CPU only)

### 3.1 Asking for emojis directly — fails

64 emojis as `choice` options (Jev-style), plus emotion + sarcasm questions:

- Picks were close to random (😂 for "thank you so much", 💜 for "huge spider help", ✌️ for "happy birthday").
- Cause: ❤️ alone is 6 tokens, 😂 is 3; with 64 options in a 192-token budget each option is cut to ~3 tokens → the model compares broken byte fragments, and descriptions are cut away entirely.
- Also slow: ~1.4 s for three questions (~360 tokens each).
- The **emotion** question (8 word labels) was mostly right in the same run → Laya understands the text; the option format was the problem.

### 3.2 Mood labels → emoji rows — works

25 word labels in one `choice` question (`shared/moods.json`), app maps top moods to emoji rows. 23 hand-labelled samples (incl. Hinglish and 3 reply cases), "correct" = a mood a human would accept.

| Variant (Q8_0, 8 threads) | Top-1 | Top-3 | Median latency | Prompt |
|---|---|---|---|---|
| labels + 2-word descriptions | 17/23 | 20/23 | 390–600 ms | ~155 tok |
| labels only | 17/23 | 21/23 | 334–390 ms | ~80 tok |

Descriptions add tokens without helping → off by default.

### 3.3 Model file size (labels only, 8 threads)

| File | Size | Top-1 | Top-3 | Median |
|---|---|---|---|---|
| Q8_0 (official) | 449 MB | 17 | 21 | 334 ms |
| Q4_K_M (made locally) | 287 MB | 17 | 20 | 250 ms |
| **Q4_0** (made locally) | **252 MB** | **18** | **22** | **196 ms** |
| BF16 (official) | 844 MB | 16 | 21 | 267 ms |

Differences of ±1 sample are noise at n=23, but Q4_0 is clearly not worse and is the smallest and fastest (x86 has fast repacked Q4_0 kernels; ARM does too). **Current pick: Q4_0.**

### 3.4 Threads and memory (Q4_0, lean server: ctx/batch 512, 1 slot, no prompt cache)

- 8 threads: 188 ms · 4 threads: 206 ms · 2 threads: 335 ms → 4 threads is the sweet spot.
- Memory: ~265 MB real (anonymous: weights repacked for fast CPU kernels + 25 MB compute buffer) + ~260 MB of the model file in the page cache, which the OS can drop.
- Cost ≈ 2.5 ms per prompt token on this CPU (4-option question = 130 ms, 25-option question = ~190 ms).

### 3.5 Reply awareness — confirmed useful

| | reply-same ("same here") | reply-where ("where exactly?") | reply-yes ("yes!!" to a concert invite) |
|---|---|---|---|
| with replied-to message | agree ✓ | travel ✓ (📍) | celebration ✓ |
| draft only | neutral | neutral | neutral ✗ |

### 3.6 Known misses (tuning backlog)

- angry delivery complaint → `sad`; "another monday meeting that could have been an email" → `neutral` (sarcasm); "you looked really cute today" → `proud`; "ok see you at 5" → `goodnight`; "sent that text to my boss" → `sorry` (awkward was wanted).
- Probabilities are very peaky (often 1.00), so blending the top 3 moods rarely mixes rows. Try flattening on the client (e.g. `p^0.3` before blending) so the 2nd mood contributes.
- 23 samples is too few to tune on without overfitting → needs the owner's ~50 real-style messages (MANUAL-WORK §B).

## 4. Phone expectations (to be measured in Phase 1)

Laptop ≈ 190 ms at 4 threads for an ~80-token prompt. Phone big cores are roughly 2–4× slower for this kind of math → expect ~0.4–0.8 s on a recent mid/high-end phone, more on budget chips. Levers: fewer moods (shorter prompt), shorter state, GPU backends (OpenCL on Adreno, Vulkan), and the optional instant keyword layer to hide the wait.
