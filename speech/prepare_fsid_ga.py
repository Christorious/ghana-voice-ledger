#!/usr/bin/env python3
"""
Prepare the Financial Inclusion Speech Dataset (Ga) for Whisper finetuning.

The dataset (Ashesi University / Nokwary Technologies, Lacuna Fund) ships as:
    fsid-ga/fisd-ga-90p/{data.csv, audios/*.ogg}   # train split (~24.5k clips)
    fsid-ga/fisd-ga-10p/{data.csv, audios/*.ogg}   # test  split (~2.8k clips)

Each data.csv is TAB-separated with columns:
    <index>  Audio Filepath  Transcription  Translation
where "Audio Filepath" is a *logical* path (e.g. lacuna-audios-train/ga/audios/<name>.ogg);
we join to the real audio by BASENAME against --audio-root.

This script is model-agnostic and pure-stdlib: it writes JSONL manifests
({"audio": <path>, "sentence": <ga text>}) that the Colab notebook turns into a
HuggingFace Dataset. It also prints corpus stats and (optionally) oversamples the
number/money phrases that matter most for the market-trading use case.

Usage:
    python prepare_fsid_ga.py --data-csv fisd-ga-90p/data.csv \
        --audio-root fisd-ga-90p/audios --out train.jsonl --oversample-money 2
    python prepare_fsid_ga.py --data-csv fisd-ga-10p/data.csv \
        --audio-root fisd-ga-10p/audios --out test.jsonl
"""
import argparse
import json
import os
import re
import sys
from collections import Counter

# Ga text contains ɛ ɔ ŋ; make stdout UTF-8 so printing stats never crashes on Windows (cp1252).
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

# Ga uses the Latin script plus ɛ ɔ ŋ (and combining tone marks); keep those.
_PUNCT = re.compile(r"[?!.,;:\"“”()\[\]]")
_WS = re.compile(r"\s+")

# Rough money/number cue words (Ga + English loans) to flag in-domain utterances.
MONEY_NUM_CUES = [
    "shika", "cedi", "pesewa", "credit", "unit", "account", "balance",
    "he",  # "mahe" = buy
    "je",  # "majemɔ" = send
    "eko", "enyɔ", "etɛ", "ejwɛ", "enumɔ",  # ga numbers 1-5 (romanised variants)
    "kome", "enyo", "ete", "ejwe", "enumo",
]


def normalize(text: str) -> str:
    text = text.strip().strip('"').strip("'")
    text = _PUNCT.sub(" ", text)
    text = _WS.sub(" ", text).strip()
    return text.lower()


def is_money_num(sentence: str) -> bool:
    s = sentence.lower()
    return any(cue in s for cue in MONEY_NUM_CUES)


def read_rows(data_csv: str):
    with open(data_csv, encoding="utf-8") as f:
        header = f.readline()  # skip header
        for line in f:
            cols = line.rstrip("\n").split("\t")
            if len(cols) < 4:
                continue
            _idx, filepath, transcription, translation = cols[0], cols[1], cols[2], cols[3]
            yield os.path.basename(filepath), transcription, translation


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data-csv", required=True)
    ap.add_argument("--audio-root", required=True,
                    help="Directory that actually contains the .ogg files (joined by basename).")
    ap.add_argument("--out", required=True, help="Output JSONL manifest.")
    ap.add_argument("--oversample-money", type=int, default=1,
                    help="Repeat number/money utterances N times (>=1) to bias the model. Default 1 = off.")
    ap.add_argument("--check-audio", action="store_true",
                    help="Warn (and drop) rows whose audio file is missing under --audio-root.")
    args = ap.parse_args()

    n_in = n_out = n_missing = n_money = 0
    unique_sents = Counter()
    speakers = set()

    with open(args.out, "w", encoding="utf-8") as out:
        for basename, transcription, _translation in read_rows(args.data_csv):
            n_in += 1
            audio_path = os.path.join(args.audio_root, basename)
            if args.check_audio and not os.path.isfile(audio_path):
                n_missing += 1
                continue
            sentence = normalize(transcription)
            if not sentence:
                continue
            unique_sents[sentence] += 1
            m = re.match(r"^(Ga(?:Fm|Ma)\d+)", basename)
            if m:
                speakers.add(m.group(1))

            reps = 1
            if is_money_num(sentence):
                n_money += 1
                reps = max(1, args.oversample_money)

            rec = {"audio": audio_path.replace("\\", "/"), "sentence": sentence}
            for _ in range(reps):
                out.write(json.dumps(rec, ensure_ascii=False) + "\n")
                n_out += 1

    print(f"[prepare_fsid_ga] {args.data_csv}")
    print(f"  rows read            : {n_in}")
    print(f"  manifest lines written: {n_out}  -> {args.out}")
    print(f"  unique sentences     : {len(unique_sents)}")
    print(f"  unique speakers      : {len(speakers)}")
    print(f"  money/number rows     : {n_money} ({(n_money/max(n_in,1)):.0%})"
          f"  [oversampled x{args.oversample_money}]")
    if args.check_audio:
        print(f"  missing audio dropped: {n_missing}")
    top = unique_sents.most_common(5)
    print("  top sentences        :")
    for s, c in top:
        print(f"     {c:>5}  {s[:60]}")


if __name__ == "__main__":
    sys.exit(main())
