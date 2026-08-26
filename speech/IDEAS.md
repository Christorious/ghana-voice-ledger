# Brilliant ideas for the speech part

_Where the ambition lives. Grounded in what we now know: the FISD-Ga corpus is 117 fixed
financial sentences read by ~53 voices (~40 h); the trader speaks a small, semi-formulaic
"language"; the phone is cheap; the confirm sheet already exists; the mission is dignity and
ownership. Ideas are ordered by leverage, not difficulty._

## The reframe that changes everything

**This is not dictation — it's slot-filling over a tiny grammar.** A sale is
`{quantity, product, amount, currency}`. An expense is `{category, amount}`. A credit is
`{name, amount, note}`. We are not transcribing arbitrary speech; we are **filling 3–4 slots**.
Every idea below falls out of taking that seriously. General ASR is the wrong bar — and a much
harder one than we actually need.

---

## 1. Numbers as a protected channel (the safety net) — *do this early*
Amounts are the highest-value output **and** a *closed set* in every Ghanaian language (ten-ish
number words + "cedi/pesewa"). So run a **dedicated tiny number+currency spotter in parallel**
with the main model and **cross-check the amount**. Even when the general transcript is rough,
the money is nailed. On the CTC/sherpa-onnx path this becomes **grammar-constrained decoding** —
force outputs into valid `{number}…{cedis}` shapes with a WFST. Numbers are where errors hurt
most and are cheapest to get right; protect them explicitly instead of hoping the ASR nails them.

## 2. Speak the sale back in her language (trust + accessibility) — *the mission-critical one*
For a trader who can't read, showing text is **not** confirmation. Use **Ghanaian-language TTS
(GhanaNLP Khaya)** to say the parsed entry back in Ga/Twi: *"Tilapia, three, twenty cedis —
yes?"* She confirms by voice. This is the difference between an app that *demos* and one a
non-literate trader can actually *trust and own*. Small effort, enormous payoff — arguably the
single most important idea here, because it makes the whole confirm-before-save philosophy real
for the actual user.

## 3. End-to-end speech → structured record (the moonshot)
Because the domain is tiny, finetune the model to emit the **structured entry directly** —
audio → `SELL tilapia 3 20` (or JSON) — instead of a transcript we then parse. Fewer error
stages, and the model learns the *grammar of a sale*, not a language. Bootstrap from FISD + a
Ga sale-grammar + synthetic audio (idea 4). Even if we keep the transcript+parser for v1, this
is the endgame: **speech-to-intent**, not speech-to-text.

## 4. Break the 117-sentence ceiling with TTS augmentation
FISD-Ga teaches Ga *acoustics* beautifully but only knows 117 sentences. Generate **thousands of
in-domain market utterances** from a grammar (`{Ga number} {product} {amount} cedis`, sends,
credits, balances), synthesize them with **Ga TTS**, and train on real + synthetic. Real speech
for how Ga *sounds*; synthetic for the *vocabulary breadth* the app needs. A standard, proven
low-resource trick — and we have the exact grammar the app cares about.

## 5. The confirm sheet is the real model (the flywheel, on purpose)
Every correction is a labeled **(audio, text) pair in HER exact acoustic world** — her voice, her
market's noise, her phone's mic. That's the highest-value data that exists. Design for it:
- **Log** corrections on-device (opt-in, nothing leaves the phone by default).
- **Personalize**: a small on-device **LoRA/adapter** specializes the model to her voice within
  days — a generic model becomes *hers*.
- **Federated**: share only model *deltas*, never raw audio, to improve the shared Ga model
  across markets while keeping every recording private.
This is "owned by her" turned into architecture. It also means the model that ships doesn't have
to be great — it has to be good enough to start the loop.

## 6. Build for code-switching from day one
Real traders say *"Ama owes twenty cedis for fish"* — Ga/Twi grammar, **English numbers, English
"cedis," English product names**, all in one breath. A monolingual Ga model breaks on this;
Whisper's multilingual base handles it far better. Make the augmented/training data
**code-switched on purpose**. Most low-resource ASR efforts miss this and fail in the field.

## 7. Distill small → tiny for the cheapest phones
Finetune **whisper-small** (accurate) as a *teacher*, then **distill into whisper-tiny** for
deployment. You get much of small's accuracy at tiny's size/speed — better than finetuning tiny
directly. The cheap-phone constraint met without giving up accuracy.

## 8. Cascade for the "listening stall"
A **tiny always-on wake-word / number-spotter** (battery-cheap) that wakes the heavy model only
when a sale is likely. This is what makes ambient, hands-free capture viable on a cheap phone
without draining it — the technical bridge to the "phone-in-the-pouch" vision.

## 9. Know her voice from the customers' (diarization)
In ambient mode, **speaker diarization / ID** (x-vector/ECAPA, enrolled in a day) keeps only
*her* utterances as entries, ignores customers, and feeds idea 5's personalization. Ties the
flywheel to the listening-stall dream.

## 10. Redundancy = free accuracy
Use the structure to self-check: `quantity × unit-price ≈ stated total`? If it disagrees, ask.
The app knows things the ASR doesn't — let it catch mistakes the model can't.

---

## The path I'd actually take

1. **Ship offline Ga** — whisper-base finetune on FISD-Ga + the existing fuzzy parser _(Stage 1, already scoped in STRATEGY.md)_.
2. **Add Ga voice read-back (idea 2)** — biggest trust/accessibility win for the least code.
3. **Number-spotter safety net (idea 1)** — protect the amounts.
4. **TTS augmentation + code-switch data (ideas 4, 6)** — break the vocabulary ceiling.
5. **Confirm-sheet flywheel + per-trader adapter (idea 5)** — the model becomes hers.
6. **Then** the moonshot (speech→intent, idea 3) and the cascade/diarization (8, 9) for the stall.

**Practical accelerator:** don't start from vanilla whisper-base if an **already-Ghanaian
checkpoint** exists (Akan/Twi Whisper finetunes are public) — warm-start from the closest one.
(Being verified by the research pass; will fold model ids + TTS licenses in here.)
