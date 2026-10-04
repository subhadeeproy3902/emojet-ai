# Third-party notices

EmoJet itself is MIT-licensed (see `LICENSE`). It uses or redistributes:

## Laya (model)
- Source: https://huggingface.co/convaiinnovations/laya — GGUF conversion: https://huggingface.co/ggml-org/Laya-GGUF
- Licence: Apache License 2.0 — https://www.apache.org/licenses/LICENSE-2.0
- Changes made by EmoJet: none so far (the model file is downloaded unmodified from the pinned revision in `docs/HUGGINGFACE.md`). Any re-quantized copy we publish will be listed here with the llama.cpp commit used.

## llama.cpp / ggml (inference engine)
- Source: https://github.com/ggml-org/llama.cpp
- Licence: MIT — Copyright (c) 2023-2026 The ggml authors
- Used as: `llama-server` (development, Android engine) and, later, compiled into the Android/WASM engine.

## Unicode emoji data + CLDR annotations (`shared/emoji-catalog.json`)
- Sources: https://unicode.org/Public/emoji/latest/emoji-test.txt (Emoji 18.0) and https://github.com/unicode-org/cldr-json (CLDR 48, English annotations)
- Licence: Unicode License v3 — https://www.unicode.org/license.txt — Copyright © Unicode, Inc.
- Changes: reduced to one entry per base emoji with name, group and keywords.

## Training data (Phase T, when used)
- emojinize-multilingual — https://huggingface.co/datasets/yagizgencer/emojinize-multilingual — CC-BY-4.0
- GoEmotions — https://huggingface.co/datasets/google-research-datasets/go_emotions — Apache-2.0

## Not used
- emoji-jev (https://github.com/colinmcdermott/emoji-jev) inspired the idea. It has no licence, so none of its code or emoji data is copied.

The full licence texts are shipped inside the APK (`assets/licenses/`) and the extension zip (`licenses/`) when those builds exist.
