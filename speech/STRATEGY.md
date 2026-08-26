# Speech strategy — offline Ghanaian-language ASR for Ghana Voice Ledger

_Decision memo, 2026-08-26. Source of truth for how we make the voice part actually work._

## The goal (unchanged)

A market trader speaks a sale or expense in **her own language** and the app writes it
down — **fully offline, on a cheap Android phone**. The downstream parser only needs the
words *roughly* right (it fuzzy-matches numbers, "cedis/pesewas", products, names), so we
are not chasing dictation-grade accuracy — we are chasing *usable* recognition of a narrow
money/market domain.

## What we have: the dataset

`D:\Documents\11-fsid_ga.zip` is the **Ga** slice of the **Financial Inclusion Speech
Dataset** (Ashesi University + Nokwary Technologies, funded by Lacuna Fund). Measured:

| Fact | Value |
|---|---|
| Clips | 24,524 train (`fisd-ga-90p`) + 2,777 test (`fisd-ga-10p`) = **27,301** |
| Audio | **~39.9 hours** (avg **5.26 s**/clip), **Opus @ 48 kHz mono** `.ogg` |
| Speakers | ~53 in this slice (coded `GaFm##` / `GaMa##`, both genders) |
| Unique sentences | **~117** — a *read-speech, fixed-prompt* corpus |
| Domain | **Financial / mobile-money** by design (e.g. `Mahe credit` = "buy credit", `Majemɔ shika kɛha mi mami` = "send money to my mother", `...yɛ i account...`) |
| Transcripts | Ga text + English translation, TAB-separated `data.csv` |
| License | Open (Lacuna Fund); commonly CC BY-NC-SA — **confirm before commercial ship** |

**Update (2026-08-26): we now have the whole FISD family.** Ga + **Asante Twi** + **Akuapem Twi**
+ **Fante** — **~104k utterances / ~150 h**, same layout per language. Twi is Ghana's most-spoken
language, so Asante+Akuapem+Fante multiply the app's reach; the Akuapem set even contains English
code-mixing (e.g. *"Nnipa yɛ bad"*), which trains the code-switching robustness real traders need.
A separate 100-clip `26-sample_alpha` (`.wav`, Asante Twi) is a *preview* of a different corpus —
worth chasing the full version later, not part of FISD. **Plan: one multilingual finetune across
all four** (Whisper handles mixed languages; the Twi dialects share vocabulary), tagging manifests
with `--lang`, rather than shipping four separate models.

**Implication.** ~40 h across many voices is *plenty to adapt a pretrained multilingual
model to Ga acoustics*, but only 117 sentences means **thin lexical variety** — a model
trained on this alone will know Ga *sounds* well but generalise poorly to arbitrary market
words. So the winning shape is: **acoustic finetune on FISD-Ga + bias decoding / fuzzy
parsing with our own trading vocabulary** (`sikabook-models/trading_corpus.txt` + lexicon).

## The decision

> **Primary: finetune `openai/whisper-base` (74M) on FISD-Ga with HuggingFace →
> convert to `ggml` → run offline with `whisper.cpp` on Android.**

**This project is non-commercial, open-source, and free — built for the traders, not for
profit.** That removes the licensing constraint that shaped this table: **Meta MMS (CC-BY-NC)
is now fully usable.** Whisper-base stays the *primary* recommendation purely on **deployment
simplicity** (whisper.cpp is the easiest offline runtime on cheap phones, ~60–80 MB), but
**MMS is now a strong co-candidate worth A/B-testing** — it has already seen Ga in pretraining
and its adapters finetune well on little data. Plan: train both on Colab, compare WER on the
test split, ship whichever wins the accuracy/size/speed trade on a real budget phone.

Why Whisper-base as the default, and how the others compare:

| Option | Verdict | Reason |
|---|---|---|
| **Whisper-base → whisper.cpp** | ✅ **chosen** | Only path that is *all* of: proven for Ghanaian langs (Akan/Twi finetunes exist), proven on Android, **MIT-licensed (commercial-safe)**, small (~60–80 MB q5), workable on cheap phones for short clips. |
| Whisper-tiny (39M) | ✅ fallback for weakest phones | ~30–45 MB q5, fastest; higher WER. |
| Whisper-small (244M) | server/eval only | Best WER but too slow/heavy on budget CPUs. |
| **Vosk / Kaldi** | ❌ ruled out | No Ga acoustic model or phoneme lexicon exists; Kaldi isn't a finetuning tool; ~40 h is far below from-scratch needs. (Vosk's *runtime* is great — but there's no Ga model to run.) |
| **Meta MMS (Ga adapter)** | ✅ **now viable** (project is free/open-source) | Excellent Ga support (`gaa` is a supported language; adapters ~2.5M weights; strong low-data WER). CC-BY-NC 4.0 was the *only* blocker — **moot for a non-commercial, open-source app.** Worth A/B-testing against Whisper; deploy via sherpa-onnx / ONNX Runtime Mobile (~300–350 MB int8). |
| wav2vec2-CTC → **sherpa-onnx** | ⭐ documented fallback | If Whisper WER is inadequate: CTC is faster per clip on weak CPUs, and sherpa-onnx gives native **hotword biasing + KenLM shallow fusion**. Use a *permissively-licensed* wav2vec2 base (not MMS). ~300–350 MB int8 is the downside. |

**Honest accuracy expectation — better than first thought.** The "Benchmarking Akan ASR" paper
(arXiv 2507.02407) finetunes Whisper/MMS/wav2vec2 **on FISD itself** and reports **~10 % WER /
~6 % CER in-domain** (vs 86–95 % WER for out-of-domain/generic models). Since we finetune on
FISD-Ga and use it in the *same* domain, **~10–20 % WER is a realistic target** — and **effective
task accuracy is higher still** because the parser only needs numbers/keywords right, we add a
number/currency spotter, and fuzzy lexicon correction. Read-speech → noisy-market-speech will
cost some accuracy; the confirm-sheet flywheel closes that gap. Bottom line: this can be
genuinely accurate, not merely "good enough."

**TTS for two jobs (now concrete).** Meta **MMS-TTS** ships open, offline Ghanaian voices —
`facebook/mms-tts-gaa` (Ga), `-aka` (Twi), `-ewe` (Ewe), CC-BY-NC. Use them to (a) **speak the
parsed entry back in Ga** so a non-literate trader can confirm by ear, and (b) **synthesize an
augmentation corpus** to break FISD's 117-sentence ceiling (TTS augmentation shows 8–38 % WER
reductions in the literature). See `IDEAS.md`.

## The staged roadmap

- **Stage 0 — today (shipped).** Android `RecognizerIntent` push-to-talk. Works for
  English-ish input; poor on Ga. **Keep it as a fallback** behind the recognizer interface.
- **Stage 1 — offline Ga (this package).** Finetune whisper-base on FISD-Ga → ggml/q5 →
  `whisper.cpp` in the app, behind a new `OfflineRecognizer` interface. Add whisper.cpp
  **prompt/hotword biasing** for money/number/product terms. Route output into the existing
  transparent parser (which already fuzzy-handles Twi/Ga numbers, cedis+pesewas).
- **Stage 2 — the data flywheel.** The **confirm sheet already captures (audio, corrected
  text) pairs** every time the trader fixes a mishear. Bank those; periodically re-finetune.
  This is how we close the gap between clean read-speech and noisy spontaneous market speech.
- **Stage 3 — hands-free "listening stall."** Wake-word + short capture + end-of-day review.
  Deferred; batch (not streaming) whisper.cpp on short clips is the near-term shape.

## What's in this folder

| File | What |
|---|---|
| `prepare_fsid_ga.py` | Model-agnostic prep: `data.csv` → normalized JSONL manifests, stats, money-phrase oversampling. Pure stdlib. |
| `finetune_whisper_ga.py` | The HuggingFace finetuning pipeline (Seq2SeqTrainer + WER) + ggml export/quantize steps. Runs on a free Colab/Kaggle GPU. |
| `COLAB.md` | Copy-paste runbook to train end-to-end on free GPU and download the `.bin`. |
| `android_whisper.md` | How to drop whisper.cpp into the app behind the existing voice interface, with hotword biasing and the parser hand-off. |

## Open risks / to confirm

1. **License** — project is non-commercial/open-source, so FISD (Lacuna, likely CC BY-NC-SA) and
   MMS (CC-BY-NC) are both fine to use. Honour attribution + share-alike: **release the finetuned
   models open-source** too. (If the goal ever changes to a paid product, revisit MMS.)
2. **Read-speech → spontaneous-speech gap** — mitigated by Stage 2 flywheel; consider recording a
   few hundred real market utterances to add.
3. **Language token** for Whisper: Ga isn't a native Whisper language; use a placeholder/related
   token (byte-level BPE means no OOV) — validate empirically in the notebook.
4. **Akan/Twi/Ewe**: same recipe, same dataset family (FISD also covers Akan). Ga first, then
   fold in the others as separate finetunes or a multilingual finetune.

## Sources
- Akan Whisper finetune (closest precedent): https://huggingface.co/GiftMark/akan-whisper-model
- Whisper finetuning guide: https://huggingface.co/learn/audio-course/en/chapter5/fine-tuning
- whisper.cpp (Android examples): https://github.com/ggml-org/whisper.cpp
- Akan ASR benchmark (uses FISD): https://arxiv.org/pdf/2507.02407
- FISD dataset: https://github.com/Ashesi-Org/Financial-Inclusion-Speech-Dataset
- sherpa-onnx (fallback runtime): https://github.com/k2-fsa/sherpa-onnx
- MMS (Ga/adapters; note CC-BY-NC): https://huggingface.co/docs/transformers/en/model_doc/mms
