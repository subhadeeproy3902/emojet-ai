# Scripts

E: is mounted without the exec bit, so run everything through `bash` / `python3`.

| Script | What it does |
|---|---|
| `bash scripts/get-model.sh [Q8_0\|BF16]` | Download the official Laya GGUF from Hugging Face (no account needed) into `models/`. |
| `bash scripts/quantize.sh [Q4_0\|Q4_K_M]` | Make a smaller model file from BF16 (downloads BF16 first if missing). Q4_0 is the current pick. |
| `bash scripts/dev-server.sh` | Start `llama-server` with Laya on `127.0.0.1:8080` (env: `MODEL`, `PORT`, `THREADS`). Uses Q4_0 if present, else Q8_0. |
| `python3 scripts/build-emoji-catalog.py` | Regenerate `shared/emoji-catalog.json` from the latest Unicode emoji list + CLDR keywords. |
| `python3 scripts/test-tones.py` | Score communication style + tone of voice (`shared/tones.json`) on 25 labelled messages. |
| `bash scripts/phone-bench.sh [threads]` | Copy the Android engine + model to a USB-connected phone (`/data/local/tmp/emojet`), run the same test there, stop it afterwards. |
| `python3 scripts/test-prompts.py` | Score the mood picks on the labelled samples + measure latency. Flags: `--no-desc`, `--no-reply`, `--sarcasm`, `--url`. |

Typical loop:

```bash
bash scripts/dev-server.sh &           # leave running
python3 scripts/test-prompts.py --no-desc
# edit shared/moods.json, run the test again — the score must not drop
```

Needs llama.cpp built in `~/build/llama.cpp` (see `docs/PLAN.md` → Prerequisites).
