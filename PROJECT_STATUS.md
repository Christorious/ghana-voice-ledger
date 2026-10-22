# Ghana Voice Ledger — Project Status

_Last updated: 2026-08-24. This is the single source of truth for what the app **is** today
versus what was **envisioned**. It supersedes the many older status/verification/"complete"
reports that described a previous codebase which **never compiled**._

## One-line summary

We replaced a large app that never built with a **small one that actually works** — nailing
the core loop (**voice → parse → ledger**) and the **credit ledger** the research says matters
most — and deliberately deferred the heavy, unproven pieces (on-device ML, multi-view UI,
cloud sync, security hardening).

## The idea we set out to build

From `CULTURAL_FINANCIAL_COGNITION_RESEARCH.md` and `CULTURALLY_CONGRUENT_UI_GUIDE.md`:
a voice-first ledger for Ghanaian market traders that is **congruent with how they already
think about money** — by customer, by time of day, "good day / bad day," and informal credit —
rather than imposing a Western ledger. Around that core the original docs also described a very
large surface: continuous background listening, on-device ML speaker identification, multiple
metaphor-driven views ("Money Pile," "Progress Journey," etc.), Twi/Ga/Ewe UI, daily/weekly/
monthly summaries read aloud, cloud sync, SQLCipher encryption, Firebase analytics, beta/A-B
infrastructure, and a full multi-flavour release pipeline.

## What actually exists today

A single-module Android app (Kotlin, Jetpack Compose Material 3, Room) that **builds, runs on a
device, is unit-tested, and produces an APK**. CI is green.

| Capability | Status | Notes |
| --- | --- | --- |
| Record a sale by voice (cedis) | ✅ Done | Android speech-to-text → parse → save |
| Ghana-specific parsing | ✅ Done | Product, quantity, cedis **+ pesewas**; English + Twi/Ga/Ewe number words; product canonicalisation; 21 unit tests |
| Confirm-before-save + edit entries | ✅ Done | Fixes voice mishears; every entry is editable |
| Today's total + history + "good day" moment | ✅ Partial | Daily total + list; no weekly/monthly yet |
| **Credit ledger (who owes me)** | ✅ Done | Saved customers, per-customer running balances, **partial payments**, settle. The single highest-value feature for this user |
| Warm, culturally-congruent UI | ✅ Partial | Calm cream + kente-gold/green, Ghanaian time-of-day greeting, rounded/spacious. **Not** the multi-view metaphors yet |
| Offline-first | ✅ Done | Fully local (Room/SQLite). No cloud sync |
| Multi-language **UI** (Twi/Ga/Ewe labels) | ⛔ Not yet | Only number-word parsing + greetings so far |
| ML speaker ID / smart categorisation | ⛔ Deferred | Dropped with the non-compiling source |
| Daily-summary TTS, weekly/monthly reports | ⛔ Not yet | |
| SQLCipher encryption / biometric lock | ⛔ Deferred | |
| Firebase / analytics / beta / A-B | ⛔ Deferred | |
| Continuous background listening | ⛔ Not planned | Push-to-talk instead — simpler, battery-safe, more private |
| Full CI/CD + Play Store pipeline | ⛔ Simplified | One green build+test workflow |

## App structure (current)

```
app/src/main/java/com/voiceledger/ghana/
├── MainActivity.kt              # Compose host + speech-recognition + tab routing
├── VoiceLedgerApplication.kt
├── data/                        # Room: Transaction, Customer, Debt + DAOs + LedgerDatabase
├── ui/                          # AppRoot (bottom nav), LedgerScreen (Today), CreditScreen, ViewModels, theme
└── voice/                       # AmountParser, TransactionParser, DebtParser (heuristic, no ML)
```

## Roadmap (next)

1. **Daily summary + WhatsApp share** — a "good day" recap you can send (stickiness + growth).
2. **Customer autocomplete** — pick a saved name as you type (polish on the credit flow).
3. **Twi/Ga/Ewe UI strings** — real multi-language labels, not just number words.
4. **A metaphor view** from the UI guide (e.g. "Money Pile" / "Progress Journey") as an
   optional way to see the day — the most distinctive unbuilt idea.
5. Later, only if validated with real traders: cloud backup, speaker ID, encryption.

## Speech-to-text direction

**Today** the app uses the device's built-in recogniser (`RecognizerIntent`) — free, no keys,
no bundled model — as **push-to-talk**: tap the mic, speak, the parser + confirm sheet catch
mishears. Good enough to ship; limited on offline use and on Ghanaian languages.

**North star — the "listening stall".** The trader is serving a customer with both hands and
all her attention; the phone is in her pouch / pocket / under the table. She never looks at it.
The app should capture the sale/credit from how she naturally speaks.

**The honest constraints.** True always-on ambient capture stacks ~6 frontier problems at once:
far-field + cloth-occluded mic (low SNR), multiple speakers (needs diarization), always-on
battery cost, **privacy/consent** (recording customers/bystanders), Ghanaian languages +
code-switch, and pulling the transaction out of natural conversation. That is a multi-year
research program, not a fine-tune.

**Two model axes — keep them separate.**
- **Language** (understand Twi/Ga/Ewe + Pidgin) → **Meta MMS** (open, 1,100+ langs incl.
  Akan/Ewe/Ga) and/or **GhanaNLP Khaya / Abena** (Twi + Ghana-Pidgin ASR). *Not* Vosk — Vosk
  has no Ghanaian model, and text corpora (our `sikabook-models`, GhanaNLP) build the
  language-model half, not the acoustic model.
- **Noise** (hear her over the market) → source separation / speech enhancement. **SAM-Audio**
  (Meta, audio source separation) is the right *category* but is a heavy (~500M–3B) **server-side**
  front-end — you cannot fine-tune Vosk "into" it (separator vs. recogniser are different models).

**Realistic path to hands-free (the bridge, small not big):**
1. **Now** — push-to-talk + a light on-device denoiser.
2. **Next** — a **wake-word / keyword spotter** (tiny, always-on-feasible on cheap phones, like
   "Hey Google") → short capture → **end-of-day review** (she fixes the batch when free, since
   she isn't looking at the phone). Feels hands-free, ships on cheap phones, respects privacy.
3. **Later** — a **compact speech-enhancement model** (RNNoise/DTLN-class, ~1 MB, real-time)
   **fine-tuned on pouch-audio collected via the app** — the tractable cousin of SAM-Audio.
4. **Data flywheel** — every confirm-sheet correction is a **(audio, correct text)** pair in our
   exact domain/dialect/noise. The app is the harvester; `sikabook-models` is the seed corpus.
   Ship constrained recognition now, collect (with consent), fine-tune MMS later.
5. **Cheap levers that beat any model** — phone **face-up on the table** vs. a pouch; a **clip-on
   / Bluetooth lapel mic** for a few cedis solves far-field better than a research lab.

_Discussed 2026-08-24. Model note: SAM-Audio and other post-cutoff releases must be
**web-verified**, not recalled from memory._

## A note on the older documentation

This repository previously contained ~70 markdown files, most of which described the earlier
non-compiling codebase — build-status notes, ProGuard/Firebase/deployment how-tos, and a series
of "completion / verification / v1.0.0 release" reports for features that never shipped. Those
have been removed to stop the repo from misrepresenting itself. The **vision** documents are
kept and clearly labelled as vision:

- `CULTURAL_FINANCIAL_COGNITION_RESEARCH.md` — the research/thesis behind the product.
- `CULTURALLY_CONGRUENT_UI_GUIDE.md` — the aspirational UI design (mostly **not yet built**).

Everything removed is still recoverable from git history.
