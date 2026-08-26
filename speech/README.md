# speech/ — making the voice part work (offline, Ghanaian languages)

The plan to give Ghana Voice Ledger real **offline speech recognition in local languages**,
starting with **Ga**, using the Financial Inclusion Speech Dataset we already have.

**The decision in three lines:** finetune `whisper-base` on the ~40 h Ga corpus → convert to
`ggml`/q5 → run offline with `whisper.cpp` on Android, behind a `SpeechRecognizer` interface
that keeps today's `RecognizerIntent` as a fallback. MIT-licensed, ~60–80 MB, cheap-phone-ready.
The transparent parser + confirm sheet absorb the model's roughness — and the confirm sheet
becomes a data flywheel that improves the model from real use.

## Read in this order
1. **`STRATEGY.md`** — dataset facts, the model decision (and why not Vosk/MMS), roadmap, risks, sources.
2. **`prepare_fsid_ga.py`** — turn the dataset `data.csv` into training manifests (runs anywhere; pure stdlib).
3. **`COLAB.md`** — copy-paste runbook to train on a free GPU and download the model `.bin`.
4. **`finetune_whisper_ga.py`** — the actual HuggingFace finetuning pipeline + ggml export.
5. **`android_whisper.md`** — how to put the model in the app behind the existing voice interface.
6. **`readback.py` / `readback.md`** — speak the parsed sale back in Twi/Ga (MMS-TTS) so a
   non-literate trader confirms by ear. Runs text-only now; audio on Colab / once TTS deps installed.

## Status
- ✅ Dataset analysed (`D:\Documents\11-fsid_ga.zip` = Ga slice of FISD; ~40 h, 117 financial sentences, 53 speakers).
- ✅ Prep pipeline written + verified (produces `train.jsonl` / `test.jsonl`).
- ⏳ Finetuning: ready to run on Colab (needs a GPU — not done on this machine).
- ⏳ Android whisper.cpp integration: planned (`android_whisper.md`), not yet coded.

Dataset: Ashesi University + Nokwary Technologies, funded by Lacuna Fund —
https://github.com/Ashesi-Org/Financial-Inclusion-Speech-Dataset (confirm license before commercial ship).
