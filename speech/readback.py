#!/usr/bin/env python3
"""
Speak a parsed sale/expense back to the trader in her language, so a NON-LITERATE trader can
confirm by ear: "Tilapia, mmiɛnsa, aduonu cedi — aane?" (Tilapia, three, twenty cedis — right?)

Two stages:
  1. Compose a natural Twi/Ga confirmation sentence (numbers → number-WORDS). Pure stdlib —
     runs anywhere, right now, with --text-only.
  2. Synthesize it to audio with Meta MMS-TTS (open, offline, CPU-friendly VITS):
       facebook/mms-tts-aka (Akan/Twi) · facebook/mms-tts-gaa (Ga)
     Needs `pip install transformers torch scipy` (or run on Colab). On-device later: export the
     VITS model to ONNX and play from the app's confirm sheet.

Usage:
  python readback.py --lang twi --product Tilapia --qty 3 --amount 20 --text-only
  python readback.py --lang twi --product Rice --amount 45.50 --out rice.wav

NOTE ON NUMERALS: getting the AMOUNT right is the whole point (trust). Twi numerals 0–9999 are
implemented. Ga numerals 1–10 are in; **11+ need a native-speaker table** — the script refuses to
invent them rather than speak a wrong amount. Fill GA in NUMERALS below (a 15-min native pass).
"""
import argparse
import sys

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

# --- number words -----------------------------------------------------------------------------
# Single source of truth; edit/verify with a native speaker. Twi = Asante Twi romanisation
# consistent with the app's AmountParser.

TWI = {
    "ones": ["", "baako", "mmienu", "mmiɛnsa", "ɛnan", "enum", "nsia", "nson", "nwɔtwe", "nkron"],
    "ten": "du",
    "teens": {11: "dubaako", 12: "dumienu", 13: "dumiɛnsa", 14: "dunan", 15: "dunum",
              16: "dunsia", 17: "dunson", 18: "dunwɔtwe", 19: "dunkron"},
    "tens": {20: "aduonu", 30: "aduasa", 40: "aduanan", 50: "aduonum",
             60: "aduosia", 70: "aduɔson", 80: "aduɔwɔtwe", 90: "aduɔkron"},
    "hundreds": {1: "ɔha", 2: "ahaanu", 3: "ahaasa", 4: "ahaanan", 5: "ahaanum",
                 6: "ahaasia", 7: "ahaason", 8: "ahaawɔtwe", 9: "ahaakron"},
    "thousand": "apem",
}

# Ga ones consistent with AmountParser; tens/hundreds intentionally EMPTY until verified.
GA = {
    "ones": ["", "ekome", "enyɔ", "etɛ", "ejwɛ", "enumɔ", "ekpaa", "kpawo", "kpaanyɔ", "nɛɛhu"],
    "ten": "nyɔŋmaa",
    "teens": {},     # TODO(native): Ga 11–19
    "tens": {},      # TODO(native): Ga 20,30,…,90
    "hundreds": {},  # TODO(native): Ga 100,200,…
    "thousand": None,
}


def _twi_below_100(n, t):
    if n < 10:
        return t["ones"][n]
    if n == 10:
        return t["ten"]
    if 11 <= n <= 19:
        return t["teens"][n]
    tens, unit = (n // 10) * 10, n % 10
    return t["tens"][tens] if unit == 0 else f"{t['tens'][tens]} {t['ones'][unit]}"


def twi_number(n):
    if n == 0:
        return "hwee"
    if n < 0 or n > 9999:
        raise ValueError(f"Twi numeral out of supported range 0–9999: {n}")
    parts = []
    th, rem = divmod(n, 1000)
    if th:
        parts.append(TWI["thousand"] if th == 1 else f"{twi_number(th)} {TWI['thousand']}")
    h, rem = divmod(rem, 100)
    if h:
        parts.append(TWI["hundreds"][h])
    if rem:
        parts.append(_twi_below_100(rem, TWI))
    return " ".join(parts)


def ga_number(n):
    if 0 <= n <= 10:
        return "efo" if n == 0 else (GA["ten"] if n == 10 else GA["ones"][n])
    raise ValueError(
        f"Ga numeral {n} needs a verified table (11+ not yet filled). "
        "Add GA['teens']/['tens']/['hundreds'] with a native speaker, then re-run."
    )


NUMERALS = {"twi": twi_number, "ga": ga_number}

# --- language templates -----------------------------------------------------------------------
LANGS = {
    "twi": {"mms": "facebook/mms-tts-aka", "cedi": "cedi", "pesewa": "pesewa", "tail": "aane?"},
    "ga":  {"mms": "facebook/mms-tts-gaa", "cedi": "cedi", "pesewa": "pesewa", "tail": "oyɛ?"},
}


def compose(lang, product, qty, amount, currency=True):
    """Return the spoken confirmation string in `lang`."""
    num = NUMERALS[lang]
    cfg = LANGS[lang]
    bits = []
    if product:
        bits.append(product.strip())
    if qty:
        bits.append(num(int(qty)))
    if amount is not None:
        cedis = int(amount)
        pes = round((amount - cedis) * 100)
        money = f"{num(cedis)} {cfg['cedi']}" if currency else num(cedis)
        if pes:
            money += f", {num(pes)} {cfg['pesewa']}"
        bits.append(money)
    sentence = ", ".join(b for b in bits if b)
    if cfg["tail"]:
        sentence += f" — {cfg['tail']}"
    return sentence


def synthesize(text, lang, out_wav):
    """Meta MMS-TTS (VITS). Imported lazily so --text-only needs no deps."""
    try:
        import numpy as np
        import torch
        from scipy.io.wavfile import write as write_wav
        from transformers import VitsModel, AutoTokenizer
    except ImportError as e:
        raise SystemExit(
            f"Audio needs extra packages ({e.name}). Install: pip install transformers torch scipy\n"
            "Or run --text-only to just see the sentence."
        )
    model_id = LANGS[lang]["mms"]
    tok = AutoTokenizer.from_pretrained(model_id)
    model = VitsModel.from_pretrained(model_id)
    inputs = tok(text, return_tensors="pt")
    with torch.no_grad():
        wav = model(**inputs).waveform[0].cpu().numpy()
    write_wav(out_wav, model.config.sampling_rate, (wav * 32767).astype(np.int16))
    print(f"wrote {out_wav}  ({len(wav)/model.config.sampling_rate:.1f}s, {model_id})")


def main():
    ap = argparse.ArgumentParser(description="Speak a parsed sale back in Twi/Ga (MMS-TTS).")
    ap.add_argument("--lang", choices=["twi", "ga"], required=True)
    ap.add_argument("--product", default="")
    ap.add_argument("--qty", type=int, default=None)
    ap.add_argument("--amount", type=float, default=None)
    ap.add_argument("--no-currency", action="store_true", help="Don't append the cedi/pesewa word.")
    ap.add_argument("--out", default="readback.wav")
    ap.add_argument("--text-only", action="store_true", help="Print the sentence; no audio (no deps).")
    args = ap.parse_args()

    try:
        text = compose(args.lang, args.product, args.qty, args.amount, currency=not args.no_currency)
    except ValueError as e:
        raise SystemExit(f"[readback] {e}")

    print(f"[{args.lang}] {text}")
    if not args.text_only:
        synthesize(text, args.lang, args.out)


if __name__ == "__main__":
    main()
