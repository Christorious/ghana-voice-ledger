#!/usr/bin/env python3
"""Watch Hugging Face for releases that matter to this project. Stdlib only, no token.

  python3 scripts/hf_watch.py                  # what changed in the last 7 days
  python3 scripts/hf_watch.py --since-days 30  # a wider window
  python3 scripts/hf_watch.py --quiet          # print nothing when there is nothing

Why it exists: on 20 October 2026 alone, Voxtral 3, Gradium Phonon and Paradee-8M all landed,
and two of the three changed the plan of record. Small streaming speech models and anything in
a Ghanaian language are moving faster than we can follow by hand, and the owner's X feed is not
reachable from the session that does the following.

What it looks for, in the order the digest prints:

  1. GHANAIAN LANGUAGES - anything in Akan, Twi, Fante, Ewe or Dagbani, models and datasets
     both. Highest value and lowest volume: a new Akan corpus or voice is a direct input, and
     `google/WaxalNLP` is the reason the plan's mouth is buildable at all.
  2. TRACKED PUBLISHERS - the labs whose releases we already depend on or evaluate against.
  3. SMALL AND STREAMING SPEECH - new TTS/ASR whose name signals it is tiny or realtime. This
     is the slice that decides whether the on-device stack stays feasible.

Deliberately NOT stateful. It reports a trailing window rather than remembering what it has
already shown, so there is no state file to commit, nothing to drift, and a missed run loses
nothing as long as the window overlaps the schedule. The scheduled task passes 8 days against a
weekly fire for exactly that overlap.

Filtering note: `search=akan` also returns users called akanametov, akanksh82 and Akan4ik, so a
hit only counts when it is a speech model (or a dataset), which is what removes them.
"""
import argparse, json, re, sys, urllib.parse, urllib.request
from datetime import datetime, timedelta, timezone

API = "https://huggingface.co/api"
UA = {"User-Agent": "sikabook-hf-watch/1.0 (+ghana-voice-ledger)"}

# Ghanaian-language terms. Hugging Face search matches substrings, so every term here is
# re-checked as a DELIMITED TOKEN afterwards (see `mentions`). Without that, "twi" matches
# twin, twist and twilight, and "akan" matches the users akanametov, akanksh82 and Akan4ik -
# measured on the first run, where 21 hits contained 4 real ones.
LANGS = ["akan", "twi", "fante", "fanti", "asante", "akuapem", "ghana", "ghanaian",
         "ewe", "dagbani", "dagaare", "ga-dangme", "gadangme"]

# Publishers we depend on, evaluate against, or expect the next relevant thing from.
# ghanaopenai is here because it publishes Ghanaian speech resources under its own name, which
# the token test alone would miss.
AUTHORS = ["mistralai", "gradium", "sahilmahendrakar", "asr-africa", "FunAudioLLM",
           "kyutai", "hexgrad", "speechbrain", "ghanaopenai", "facebook", "nvidia", "google"]

# Modalities that are never relevant here, however the name matched.
OFF_TOPIC = re.compile(r"(?:^|[^a-z0-9])(?:image|video|vision|depth|lidar|lerobot|robot|"
                       r"tabular|timeseries|time-series|digital[-_]?twin|reactor|molecul)", re.I)

SPEECH_TAGS = {"automatic-speech-recognition", "text-to-speech", "text-to-audio",
               "audio-to-audio", "voice-activity-detection", "audio-classification",
               "feature-extraction", "automatic-speech-translation", "any-to-any"}

# Name markers that say "small" or "streaming" - the two properties that decide whether a model
# can live on a 2-4 GB phone at all. Parameter counts under 1B count; 1B and over do not.
SMALL = re.compile(r"(?:^|[^a-z0-9])(?:"
                   r"\d{1,3}m|0\.\d+b|"                       # 31m, 40m, 0.5b
                   r"nano|micro|tiny|mini|small|pico|mame|"
                   r"realtime|real-time|streaming|causal|"
                   r"int8|int4|q4|q8|gguf|onnx|tflite|coreml|distil|distill"
                   r")(?:[^a-z0-9]|$)", re.I)
BIG = re.compile(r"(?:^|[^a-z0-9])(?:[1-9]\d{0,2}b|\d{1,3}xb)(?:[^a-z0-9]|$)", re.I)


def get(path, **params):
    url = f"{API}/{path}?" + urllib.parse.urlencode(params)
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=45) as r:
            return json.load(r)
    except Exception as e:                      # one dead query must not sink the digest
        print(f"  ! query failed ({path} {params}): {e}", file=sys.stderr)
        return []


def when(row):
    t = row.get("lastModified") or row.get("createdAt") or ""
    try:
        return datetime.fromisoformat(t.replace("Z", "+00:00"))
    except ValueError:
        return datetime.min.replace(tzinfo=timezone.utc)


def licence(row):
    for t in row.get("tags", []):
        if isinstance(t, str) and t.startswith("license:"):
            return t.split(":", 1)[1]
    return "unstated"


def mentions(row, term):
    """Does `term` appear as a whole token in the id or the declared languages?

    Hugging Face search is substring-based, so this is where the false positives die: `twi`
    must not match twin/twist/twilight, and `akan` must not match akanametov. Tokens are
    delimited by anything that is not a letter or digit, which also means `ghana` will not
    match `ghanaopenai` - that publisher is tracked by name in AUTHORS instead.
    """
    tok = re.compile(rf"(?<![a-z0-9]){re.escape(term)}(?![a-z0-9])", re.I)
    if tok.search(row["id"]):
        return True
    langs = (row.get("cardData") or {}).get("language") or []
    if isinstance(langs, str):
        langs = [langs]
    return any(tok.search(str(l)) for l in langs) or any(
        tok.search(t.split(":", 1)[1]) for t in row.get("tags", [])
        if isinstance(t, str) and t.startswith("language:"))


def on_topic(row):
    return not OFF_TOPIC.search(row["id"])


def speechy(row):
    if row.get("pipeline_tag") in SPEECH_TAGS:
        return True
    tags = {t for t in row.get("tags", []) if isinstance(t, str)}
    return bool(tags & SPEECH_TAGS) or "audio" in tags or "speech" in tags


SHIPPABLE = ("apache-2.0", "mit", "bsd", "cc0", "cc-by-4.0", "cc-by-3.0", "cc-by-sa-4.0",
             "cc-by-sa-3.0", "openrail", "llama", "gemma")


def shippable(row):
    """Can this go in the product at all? Almost every Ghanaian speech resource published by
    the community is CC-BY-NC, so a permissive licence is the scarce property, not the model.
    That is why the digest sorts on it rather than on recency alone."""
    lic = licence(row)
    if "-nc" in lic or "-nd" in lic:
        return False
    return any(lic.startswith(s) for s in SHIPPABLE)


def fmt(row, kind):
    lic = licence(row)
    if "-nc" in lic or "-nd" in lic:
        flag = "   [NON-COMMERCIAL - cannot ship]"
    elif shippable(row):
        flag = "   [SHIPPABLE]"
    else:
        flag = "   [licence unclear - check before use]"
    return (f"  - {row['id']}{flag}\n"
            f"      {kind}  {row.get('pipeline_tag') or 'untagged'}  licence {lic}  "
            f"{when(row).date()}  dl {row.get('downloads', 0)}\n"
            f"      https://huggingface.co/{'datasets/' if kind == 'dataset' else ''}{row['id']}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--since-days", type=int, default=7)
    ap.add_argument("--quiet", action="store_true", help="print nothing if nothing is new")
    ap.add_argument("--limit", type=int, default=60, help="rows per query")
    ap.add_argument("--max-per-section", type=int, default=12,
                    help="a digest nobody reads is a digest that does not work")
    a = ap.parse_args()
    cutoff = datetime.now(timezone.utc) - timedelta(days=a.since_days)
    fresh = lambda rows: [r for r in rows if when(r) >= cutoff]
    seen, out = set(), []

    def add(rows, kind, extra=None):
        hits = []
        for r in fresh(rows):
            if r["id"] in seen:
                continue
            if extra and not extra(r):
                continue
            r["_kind"] = kind                       # carry it; the printer must not guess
            seen.add(r["id"]); hits.append(r)
        # Shippable first, then newest: a permissive licence is the scarce thing, not novelty.
        return sorted(hits, key=lambda r: (shippable(r), when(r)), reverse=True)

    # 1. Ghanaian languages: models must be speech models; datasets count whatever they are
    #    tagged as, because a new Akan corpus is worth seeing even if it is mislabelled.
    lang_hits = []
    for term in LANGS:
        lang_hits += add(get("models", search=term, sort="lastModified", direction=-1,
                             limit=a.limit, full="true"), "model",
                         lambda r, t=term: mentions(r, t) and on_topic(r) and speechy(r))
        # Datasets are not required to be speech: a Twi TEXT corpus is now a direct input,
        # because Speech Back-Translation turns text into synthetic training audio (§4.3.3).
        lang_hits += add(get("datasets", search=term, sort="lastModified", direction=-1,
                             limit=a.limit, full="true"), "dataset",
                         lambda r, t=term: mentions(r, t) and on_topic(r))
    if lang_hits:
        out.append(("GHANAIAN LANGUAGES - read these first", lang_hits))

    # 2. Tracked publishers, speech releases only.
    pub_hits = []
    for who in AUTHORS:
        pub_hits += add(get("models", author=who, sort="lastModified", direction=-1,
                            limit=a.limit, full="true"), "model", speechy)
    if pub_hits:
        out.append(("TRACKED PUBLISHERS", pub_hits))

    # 3. New small or streaming speech models from anyone.
    small_fn = lambda r: SMALL.search(r["id"].split("/")[-1]) and not BIG.search(r["id"])
    small_hits = []
    for tag in ("text-to-speech", "automatic-speech-recognition", "text-to-audio"):
        small_hits += add(get("models", pipeline_tag=tag, sort="lastModified", direction=-1,
                              limit=a.limit, full="true"), "model", small_fn)
    if small_hits:
        out.append(("SMALL / STREAMING SPEECH", small_hits))

    total = sum(len(h) for _, h in out)
    if not total and a.quiet:
        return
    print(f"Hugging Face watch - {datetime.now(timezone.utc):%Y-%m-%d %H:%M} UTC, "
          f"last {a.since_days} days, {total} item{'s' * (total != 1)}\n")
    if not total:
        print("Nothing new in scope.")
        return
    for title, hits in out:
        shown = hits[:a.max_per_section]
        n_ship = sum(1 for r in hits if shippable(r))
        print(f"{title}  ({len(hits)}, {n_ship} shippable)")
        for r in shown:
            print(fmt(r, r["_kind"]))
        if len(hits) > len(shown):
            # Sorted shippable-first, so the tail is only all-unshippable when the cap already
            # covered every shippable one. Say which case this is rather than implying it.
            rest = len(hits) - len(shown)
            tail = "all non-commercial or unclear-licence" if n_ship <= len(shown) \
                else f"including {n_ship - len(shown)} shippable"
            print(f"  ... and {rest} more, {tail} (raise --max-per-section to see them)")
        print()
    print("Licences marked NON-COMMERCIAL cannot ship in the product (plan of record, §4.3).")


if __name__ == "__main__":
    main()
