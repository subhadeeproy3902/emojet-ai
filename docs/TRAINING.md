# Phase T — Train "Laya-Emoji"

**Goal:** one model that reads the **whole message** (and the message you're replying to) and scores **every emoji at once** — all 1,923 distinct emojis of Unicode 18.0 (3,963 including skin-tone variants) — in a single fast pass. Still Laya: it starts from Laya's own encoder weights (Apache-2.0) and is fine-tuned for emojis.

## Why the current setup can't do this

- Laya's built-in "choice" question holds at most 255 options inside a 192-token budget. 1,923 emojis can't fit, even as words. That's why Phase 0 uses ~25 mood words.
- Moods cover **feelings** well (18/23 right) but not **things**: "landed in Goa, beach time" should offer 🏖️🌴✈️, "pizza tonight?" 🍕, "India won!" 🇮🇳🏏. The mood design can only reach the emojis we hand-place in 25 rows.

## The design

```
reply context + whole draft (≤ 64 tokens, emojis stripped)
        │
        ▼
Laya encoder (ModernBERT-large, 28 layers, initialised from convaiinnovations/laya)
        │  [CLS] vector, 1024 numbers
        ▼
new emoji head: 1024 → 1,923 scores (one per base emoji, sigmoid = multi-label)
        │
        ▼
top 6 → apply skin-tone preference → personal boost → overlay
```

- **Whole sentence, always.** The input is the full draft + reply context; there is no per-word lookup in the model path. A message like "not bad at all, honestly loved it" must not trigger 👎 because of "bad".
- **Multi-label**: a message can deserve 🎉 and 🎂 and 🥳 at the same time — trained with one yes/no output per emoji, not "pick one".
- **Faster than today**: the prompt is only the message (~20–40 tokens) instead of question + 25 options (~80 tokens). Expected ~2× faster per suggestion, while covering 77× more emojis.
- **Format**: Hugging Face `ModernBertForSequenceClassification` with 1,923 labels → `convert_hf_to_gguf.py` (llama.cpp supports this class, labels are stored in the file) → `llama-quantize` Q4_0. Head adds ~2M parameters (~1–4 MB).

## Training data (all free, licences allow training)

| Source | What it teaches | Size | Licence |
|---|---|---|---|
| [`yagizgencer/emojinize-multilingual`](https://huggingface.co/datasets/yagizgencer/emojinize-multilingual) | **things/concepts** in full sentences → emojis (🇮🇹 🚀 ⚡ 🍕 …), 14 languages incl. Hindi | 108K sentences, ~430K emoji spans | CC-BY-4.0 (credit required) |
| [`google-research-datasets/go_emotions`](https://huggingface.co/datasets/google-research-datasets/go_emotions) | **feelings** in real chat-style comments (27 emotions) → mapped to emoji sets | 58K labelled comments | Apache-2.0 |
| Laya mood model (Phase 0) as teacher | chat tone on extra unlabelled sentences → soft labels | as many as we generate | own |
| Unicode CLDR names + keywords | a few template sentences for every emoji, so none of the 1,923 is never seen | ~6K | Unicode-3.0 |
| **Your ~50 messages** | real target style (English + Hinglish) | 50+ | yours — used as the **test set**, not for training |

Labels per sentence = the set of emojis attached to it. Emojis already inside the input text are removed (the model must predict them, not copy them). Rare emojis get weighted up so 😂 doesn't win every time.

Notes: emojinize was annotated by GPT models (stated in its card) and is encyclopedic, not chatty — GoEmotions + Laya-teacher data balance the tone. Credits go in `THIRD_PARTY_NOTICES.md`.

## Where to train (both $0) — owner decides

| | A. This laptop (RTX 2050, 4 GB) | B. Kaggle free GPU (2× T4, 30 h/week) |
|---|---|---|
| Setup | `sudo pacman -S python-pytorch-cuda python-transformers python-safetensors python-datasets` (~5 GB) | free Kaggle account + phone verification; Claude writes the notebook, you press Run |
| Method | 4 GB is too small for full fine-tuning → LoRA adapters + head, or freeze lower layers | full fine-tuning |
| Time per run | ~1–3 h, laptop GPU hot and loud | ~30–60 min |
| Who runs it | Claude, end to end | you start it; Claude prepares everything |
| Fits "low temperature" goal | no | yes |

Recommendation: **data prep + evaluation on the laptop (Claude), training on Kaggle (B)**; a small local pilot (A, frozen encoder + head only, ~minutes) first to prove the pipeline before spending Kaggle hours.

## Steps

1. [Claude] Load Laya's safetensors in PyTorch, confirm the encoder weights map 1:1 onto `ModernBertModel` (sanity check: same hidden states as llama.cpp for a test sentence).
2. [Claude] Build the dataset (`scripts/train/build-dataset.py`): sources above → `(text, reply, emojis[])`, split train / validation; your messages kept as the test set.
3. [Claude] Pilot: frozen encoder + head only, on the laptop CPU/GPU. Measure hit@6 on validation and on the 23 Phase-0 samples (mapped to acceptable emoji sets).
4. [Claude + You] Full fine-tune (Kaggle or local LoRA).
5. [Claude] Convert → GGUF → Q4_0; check llama.cpp gives the same scores as PyTorch; latency on the laptop.
6. [Claude] A/B against the Phase-0 mood model on your test set. Ship Laya-Emoji only if it wins.
7. [Claude] Publish the model file (GitHub Release asset, optional Hugging Face repo — see HUGGINGFACE.md) with a model card crediting Laya, emojinize, GoEmotions and CLDR.

## Metrics

- **hit@6**: at least one emoji you'd accept is among the 6 shown (main metric). Target ≥ 85% on your messages.
- **mAP over emojis**, coverage (how many of the 1,923 emojis ever reach a top-6 on validation).
- Latency on laptop and phone; model size.
