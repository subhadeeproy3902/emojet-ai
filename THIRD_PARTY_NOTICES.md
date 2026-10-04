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

The full licence texts are shipped inside the APK (`assets/licenses/`) and the extension zip (`licenses/`) when those builds exist.
