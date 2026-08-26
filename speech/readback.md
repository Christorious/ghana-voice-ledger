# Voice read-back — speak the parsed sale back in Twi/Ga

The trust mechanism for non-literate traders: after parsing, the app **says the entry back in her
language** — *"Tilapia, mmiɛnsa, aduonu cedi — aane?"* — and she confirms by ear. Showing text a
trader can't read is not confirmation; speaking it is.

## What `readback.py` does
1. **Composes** a Twi/Ga confirmation sentence, turning the amount/quantity into spoken
   **number-words** (the hard part — MMS-TTS reads text, so we must say "aduonu", not "20").
   Pure stdlib; runs now with `--text-only`.
2. **Synthesizes** it with Meta **MMS-TTS** (open, offline VITS): `facebook/mms-tts-aka` (Twi),
   `facebook/mms-tts-gaa` (Ga). Needs `pip install transformers torch scipy`, or run on Colab.

```bash
python readback.py --lang twi --product Tilapia --qty 3 --amount 20 --text-only
python readback.py --lang twi --product Rice --amount 45.50 --out rice.wav
```

## How it plugs into the app (later)
- In the **confirm sheet**, after `TransactionParser`/`ExpenseParser` fill the draft, call the
  read-back on the parsed fields and **play it before Save** (with a "🔊 replay" button).
- **On-device:** MMS-TTS VITS is small and CPU-friendly — export to **ONNX** (`optimum`) and run
  with ONNX Runtime Mobile, or (cheapest) pre-render a small bank of clips for the number-words +
  currency + top products and concatenate. Either way, fully offline.
- Pairs with the confirm-sheet **flywheel**: hearing it back also nudges her to fix mishears,
  which gives us the correction pairs that improve the ASR.

## Honest gap — Ga numerals
Twi numerals 0–9999 are implemented and tested. **Ga is complete only for 1–10**; 11+ are left
empty on purpose — the script raises rather than speak a wrong amount. Filling
`GA['teens'] / ['tens'] / ['hundreds']` in `readback.py` is a ~15-minute pass with a native Ga
speaker. Do that before relying on Ga read-back for real amounts.
