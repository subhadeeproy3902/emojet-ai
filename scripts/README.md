# Scripts

Not started yet. See [`../docs/PLAN.md`](../docs/PLAN.md) Phase 0.

Will contain:
- `dev-server.sh` — starts a local `llama.cpp server` with the Laya GGUF model, exposing `/v1/systemone`.
- `test-prompts.*` — sends a fixed set of sample sentences through the typed-decision schema and prints the results, for eyeballing suggestion quality.
- quantization helper for producing a `Q4_K_M` variant if the `Q8_0` default proves too slow/large on-device.
