# Voice Ledger — Plan of Record

**Status: DRAFT, 19 October 2026.** Second attempt. The first version proposed push-to-talk
capture and was wrong at the premise; it is replaced entirely, not amended.

---

## 1. The vision, in the owner's words

An intelligent tool on the phone that keeps the record of her sales **from the interaction
itself**, so that she only has to focus on the customer and on serving the customer.

The model is a human bookkeeper. Christian sat at his mother's stall and noted what she
sold, what it cost, roughly when — and at the end of the day collated it and told her what
she had sold. The tool is that bookkeeper. The customer matters only because the sale
happens in the interaction between them; the trader is who the tool serves.

Later, from the same record: what is in demand, who owes her, what is moving and therefore
what her stock is doing.

### The non-negotiable

**Zero burden during trade.** She does not press a button per sale, does not speak to the
phone, does not look at it. A design that requires her to stop serving in order to record
has not solved her problem — it has given her a second job. This is the whole point of the
product and every other decision is subordinate to it.

The previous draft of this document violated exactly this and must not be repeated.

---

## 2. Architecture

| Stage | What happens | Her involvement |
|---|---|---|
| Open of day | Phone in her pouch or paired to whatever earpiece she already owns; taps once | One tap |
| Trading | Mic open, cheap VAD running; her voice wakes capture, silence sends it back to sleep; stored encrypted on device | **None** |
| Understanding | Candidate sale segments detected, then recognised and decided, in batches when idle or charging | None |
| Close of day | The phone speaks her day back in her language; she confirms or corrects | A few minutes |
| Ledger | Only confirmed rows are committed | — |

Four properties of this shape matter.

**No special equipment.** A lapel microphone is out: a trader will not have one, and a tool
that requires hardware she does not own is not a tool she will use. Capture uses the phone
itself or a Bluetooth earpiece she already owns. §2.1 sets out the paths to measure and
their ceilings.

**One tap, not per-sale.** Android 14 forbids *starting* a microphone foreground service
from the background or at boot. It does not forbid a long foreground session the user
started. One tap at opening is legal and preserves zero burden.

**Asynchronous, not real-time.** The bookkeeper collates at the end of the day. Nothing has
to be understood within a second, which removes the latency constraint, allows heavier
models, and allows re-processing. Dukawalla's finding in Nairobi was that owners cannot
stop to record while customers wait — separating capture from confirmation answers it.

**Errors surface in a review she controls**, never silently in her books. A missed sale she
notices and can add. A wrong sale corrupts her record and, where an assistant is involved,
can look like theft.

### 2.1 Capture paths, and what each costs

Four paths, to be measured side by side rather than argued about. None requires her to buy
anything she would not already have.

| Path | Audio quality | Cost to her | Risk |
|---|---|---|---|
| Phone in her waist pouch or apron pocket | Full rate (16 kHz+), muffled by cloth, semi-close | Nothing | Cloth occlusion; unknown how much survives |
| Phone face-up on the stall surface | Full rate, far-field | Nothing | Worst acoustics; 70 dBA market at a metre |
| Wired earbud with inline mic | Full rate, genuine close-talk | A few cedis, and many already own one | Cable at a stall; may not wear it all day |
| Bluetooth earpiece (HFP) | **8 kHz narrowband, or 16 kHz if the headset negotiates mSBC** | Nothing if she has one | Codec ceiling; battery on both devices; holds the call audio route |

The Bluetooth ceiling is the thing to know before committing to it. Hands-free Bluetooth
carries voice over HFP, which is either 8 kHz narrowband with the old CVSD codec or 16 kHz
wideband with mSBC. Android negotiates in the order LC3, mSBC, then CVSD, so a headset that
supports mSBC gets wideband and a cheap one falls back to 8 kHz — telephone quality, with
everything above 4 kHz gone. There is no high-quality *input* profile in classic Bluetooth;
A2DP is output only. LE Audio fixes this but needs Android 13+ and hardware a budget phone
will not have.

Whether 8 kHz close-talk beats 16 kHz far-field in a 70 dBA market is an empirical
question, not a matter of opinion, and it is cheap to settle. It also interacts with speaker
verification, which is harder at 8 kHz — so the capture path and the voice gate must be
measured together, not separately.

---

## 2.2 The voice gate: wake on her, sleep otherwise

This is a requirement, not an optimisation, and it puts speaker verification back at the
centre of the work.

Four tiers, cheapest always running:

1. **Mic open, energy/VAD only.** Speech or not. Silero-class VAD is small and cheap.
2. **Speech detected → speaker verification.** A short embedding compared against her
   enrolled voice. Hers, or not hers.
3. **Hers → capture the segment**, including a rolling pre-roll buffer of a second or two,
   so the opening words are not lost while the gate was still deciding. Without the
   pre-roll, "fifty cedis" arrives as "cedis", which is a wrong row rather than a missing
   one.
4. **Sustained silence or not-her → back to sleep.** Nothing buffered, nothing understood.

**One expectation to set honestly: the microphone stays open all day.** The gate saves the
expensive work — recognition and understanding — not the microphone. So the floor on battery
is mic plus VAD for a whole market day on a 3000–5000 mAh phone, and that floor has to be
measured on a real Tecno before anything is promised. Recognition runs on her segments
only, batched, preferably while charging.

**Tuning.** A false accept books a customer's words as her sale — a wrong row, which is the
error that corrupts her books. A false reject loses a sale, which she has a chance of
spotting in the day-end review. So the threshold is tuned for low false accept, and the
review is what makes the false rejects survivable.

**What this needs that does not exist.** A real speaker embedding model — ECAPA-TDNN or
WeSpeaker class, converted for on-device use — an enrolment flow of a minute or two of her
speech, and a calibrated threshold. There is currently zero evidence here: the asset in the
repository is a 217-byte text placeholder. This is the component with the most risk and the
least work done, and the owner identified it as a contribution area before I wrongly
demoted it.

## 3. What the recognition step actually has to do

This is the point I had wrong, and the Jev material clarifies it.

The pipeline does not need a correct transcript. It needs a **sequence of typed decisions
over closed option sets**: is this segment a sale? which of her products? which unit? what
quantity? what amount? cash, credit or part payment? Each answer comes with a calibrated
confidence that drives whether the row is booked or queued for her review.

Choosing one product from her forty, plus a number, out of a noisy transcript is a far
easier task than transcribing market speech. **So the 45% word error rate on free Twi
speech is the wrong number to plan against.** The number to plan against is sale-row
accuracy, and nobody has measured it.

### Correction to the demo's implication

Caine Ardayfio's post reads "Mobile Use + Jev for audio". On the sources available, **Jev
reads text, not audio** — state as a string, JSON or array, up to about 32k tokens, with
media judged on text fields such as transcripts. So a speech-recognition step stays on the
critical path. The decision model does not remove ASR; it lowers the bar ASR has to clear.

Two further cautions, both to be verified rather than assumed: a third-party evaluation
(the Pinocchio paper) reports Jev's calibration as mediocre — 0.666 AUROC, ECE 0.133 — so
"calibrated" should be tested on our own data before any threshold depends on it. And
whether any of these decision models handle Twi at all is untested.

---

## 4. What transfers from the shared work, and what does not

**Silas Asamoah's sika-agent — the most directly reusable thing.** An open RL environment
for training agents in English, Twi and Twi–English, with a task suite and a completion
metric. He reports Qwen3.5-9B at 86% in English and 51% in Twi, with frontier models above
90% in every language, and names the gap as the thing to close for small local-language
agents. That is our evaluation methodology, and `sikabook-models/verifier/ledger_env.py`
is already the same shape. Adopt the pattern: a ledger task suite, scored by completion,
reported per language, English versus Twi versus code-switched.

**Jev's shape — typed decisions with confidence, not generated text.** Adopt the pattern
whether or not we use that specific model. It gives the confirmation threshold its number.

**Mobile Use (minitap-ai/mobile-use, Apache-2.0) — the agentic loop, not the UI driving.**
I could not confirm Caine's relationship to that project; the open project matching the
name is Minitap's. But I want to be plain about the fit: an agent that taps and types
through a phone's interface is not what a bookkeeper at a stall needs. The bookkeeper needs
capture, understanding and recording — it does not need to operate apps. What transfers is
the perceive-decide-act structure and the on-device latency discipline, not the screen
control. Importing more than that would repeat the mobile-money mistake: taking something
because it is nearby rather than because it serves the trader.

**Dropped: mobile-money SMS ingestion.** Ghanaian market traders take cash. This was in the
previous draft because the memo rated it high value per unit of effort; it is low value to
*her*. Optional at best, and not in the core.

---

## 4.1 Platform: is Android right, and which Android?

**Android is not a choice so much as the only option**, and the reason is this product
specifically. All-day ambient capture needs a microphone we control, a long-running
process, and local compute. Measured against that:

- **Android app** — can do all three, with the OEM caveat below.
- **iOS** — wrong demographic, and App Store review is hostile to always-listening.
- **Web app or PWA** — cannot hold the microphone with the screen off. Dead end.
- **WhatsApp voice notes** — she has to send them. That is push-to-talk wearing a different
  hat, and it breaks the non-negotiable in §1.
- **Telephony or USSD** — reaches feature phones but cannot capture ambiently at all.
- **A dedicated device** — solves power and microphone cleanly, but cost and distribution
  rule it out for this user, and the feasibility memo found the wearable-recorder stack
  does not transfer.

So the real question is not the operating system. It is **which Android versions, how much
RAM, and which manufacturer skins a day-long foreground session has to survive.**

On Statcounter's Ghana figures for mid-2026, Android 13 is the most common single version
at roughly 17%, Android 14 second at about 13.5%, with 11, 12, 15 and 16 each somewhere
near 10–12%. The distribution is flat and fragmented; there is no single version to target.

Two warnings about using that number. Statcounter measures web traffic, which skews toward
people who browse, and therefore toward better phones than a market trader is likely to
carry — so it is an optimistic estimate of our floor. And the figure that actually binds is
not the Android version: it is **Transsion's HiOS and XOS power managers, which kill
long-running services regardless of foreground status.** Tecno, Infinix and itel together
are roughly a third of the Ghanaian market and dominate entry-level unit sales at 2–4 GB of
RAM, often as Android Go editions with tighter background limits. A day-long capture session
that a Tecno kills at noon is useless however current the Android version is.

The current `minSdk` of 24 is probably too conservative and costs us real complexity, but it
should not be raised on Statcounter's evidence. **The answer comes from counting the phones
at twenty stalls** — make and model — which is one line on the field study's observation
sheet and gives us the floor that matters.

## 4.2 What makes the app relevant, in numbers

The app is relevant if the list it shows her at closing is **complete enough to replace her
memory and correct enough to trust.** Four numbers decide it, and `sikabook-models` already
measures three.

**1. Coverage — of the sales she made, how many appear at all?**
This is the product-defining number and the one with no measurement, because it needs real
market audio. It is the product of three things: how much of a sale is spoken at all (never
measured for Ghana), whether the gate catches her saying it, and whether sale detection
finds it. If coverage is 50% she keeps her mental ledger and the app is dead weight she
still has to check.

**2. Correctness — wrong rows per 100 sales.** Measured: harmful writes 1–2%. This is good
and it is the number to protect, not improve at coverage's expense.

**3. Review burden — how many rows must she correct at closing?** Unmeasured. If it is
thirty, paper wins.

**4. Does it beat her head at anything?** She roughly knows her daily total. What memory
genuinely cannot do is credit across weeks and demand across months. So relevance
concentrates in **who owes her** and **what is moving** — which makes counterparty names
matter more than expected, and proper nouns are the hardest thing for a recogniser. The
Luxembourg Mooré ledger corpus scored entities by **phonetic skeleton** rather than exact
match for exactly this reason; adopt that.

### The bottleneck is the gate, by a wide margin

At the dev-chosen threshold the gate scores FAR 10% and **FRR 46%**. In her day that is:
nearly half of her own utterances rejected, so half her sales never reach the ledger
however good the extractor is; and one bystander utterance in ten accepted as hers, which
is a wrong row. No amount of work on slots or amounts matters while this holds.

Three things follow. The order below is the leverage **as since measured**, which inverted
the first two: calibration was expected to be the lever and the ranker the convenience, and
it is the other way round. Done in `sikabook-models`, branch `claude/voice-gate-v1`.

**a. Make the gate a ranker, not a filter.** With day-end confirmation, FRR 46% is only
fatal if rejected audio is thrown away. Keep uncertain segments and surface them for
review. The gate's job becomes prioritising what she checks, not deciding what exists —
which trades FRR against review burden, a knob we control, instead of against coverage,
which we cannot recover. **Measured: this is the dominant lever.** Three tiers (accept /
review / reject) raise coverage of her own speech from roughly 75% to roughly 96% at an
unchanged accept threshold, on every calibration and every seed, and on the synthetic audio
corpus they cut lost speech from 42% to 6%. The binary gate stays reachable, and every
number measured before the tier existed still reproduces.

**b. Calibrate per trader instead of one global threshold.** Dev EER 13% against test EER
26%, with the dev threshold transferring to FAR 10% / FRR 46%, looked like threshold-transfer
failure across unseen speakers, so a fixed `GATE = 0.68` could not work. Implemented as
cohort normalisation: her voiceprint is scored at enrolment against a cohort that ships with
the app, and later scores are expressed in units of her own distribution. The enrolment-side
form reduces at runtime to **two stored numbers per trader and a plain cosine**, with no
cohort arithmetic while she trades. **Measured: second-order, and weaker than claimed.** The
symmetric form consistently lowers test EER (33.3% → 22.9% on the synthetic corpus) and grows
with encoder quality; the enrolment-side form does not reliably do anything; and *neither*
closed the dev-to-test threshold gap across seeds, which was the reason for writing them. All
three are reported side by side and the real corpora decide.

**c. Replace the embedding floor.** The current voice print is log-mel plus MFCC statistics
into LDA, which the script honestly calls "the honest floor, not a deep model". ECAPA-TDNN
is already wired behind `--encoder ecapa` but frozen and VoxCeleb-trained with no Twi. Use
it properly, enrol on more than one clip, and exploit the structural fact nobody has used
yet: **across a whole market day, the most frequent speaker at the stall is her**, so her
voiceprint can be adapted from her own audio without labels. Session adaptation on the
gate's own accepts — no labels at all — is implemented behind `--adapt` and reported as a
session gain, never as a held-out one. Using ECAPA properly on the real corpora is the next
job and the one most likely to move the numbers.

### Two caveats over every number above

**The gate's numbers are not yet measurements.** `fit_lda` iterated an unordered set, so the
LDA basis followed `PYTHONHASHSEED` and changed from process to process; on the synthetic
corpus that alone swung dev EER between 0% and 33% with nothing else altered. Dev EER 13% /
test EER 26% / FAR 10% / FRR 46% are therefore one draw from a wide distribution and must be
re-run. Fixed and verified identical across hash seeds; `train_tiny.py` was checked and is
unaffected, so its 81% accuracy line stands.

**Read speech flatters.** Every result in `sikabook-models` is on FISD and KasaSpeech read
speech mixed with GESMA noise, as the feasibility memo says plainly. The numbers are honest
about their method and they are still not evidence about a market.

### The grader is currently scoring against the product

Worth recording separately, because it would have misdirected everything downstream. The
verifier pays +1.0 for cleanly blocking a bystander and +0.5 for recovering one of her sales,
so when the review tier cut lost speech from 42% to 6% the total score **fell**, 68.0 to
56.5. On that scale one cleanly blocked bystander is worth two recovered sales, and the
grader would rather lose four sales in ten than put twenty lines in front of her at closing
time. That is the opposite of "her sales get recorded without her having to think about
anything else". The four numbers are now explicit (`verifier.Rewards`, defaults unchanged and
tested to reproduce every earlier result) so the question can be settled by changing a number
and re-running — but it is a product judgement, so it is flagged rather than retuned.

---

## 4.3 The three models, not one

Until now this document treated the model problem as a single hard thing: Twi ASR. That was
wrong, and it hid a dependency. **The product needs three models, and the one that was never
named is the one the whole day-end design rests on.**

She is low-literate. The closing review is therefore not a screen she reads — it is a list
**read back to her aloud, in Twi**. Android's built-in TextToSpeech has no Akan voice. So
every coverage number in §4.2 quietly assumed a review she can actually perform, and without
a Twi voice there is no review, and without a review the three-tier gate has nowhere to put
the utterances it holds. The mouth is not a nicety at the end; it is load-bearing.

| | job | target size | status |
|---|---|---|---|
| **Ears** | streaming ASR, Twi and Twi–English | 30–100 MB | open baselines exist in Akan |
| **Gate** | is this her speaking | ~8 MB | **built** (§4.2), phone-sized already |
| **Mouth** | read the day's ledger back in Twi | 10–70 MB | no Akan voice we can ship |

Roughly 50–180 MB resident in total. That fits a 2–4 GB phone. For the first time the whole
stack is plausibly on-device and offline, which also settles the capture-and-privacy argument
in §2.1: nothing has to leave the handset.

### The pattern that makes all three affordable: distil a big teacher into a tiny student

Two independent releases on 20 October 2026 demonstrate the same recipe at the size we need,
and one of them published the code.

- **Gradium Phonon** (`gradium/phonon-31m-en`, Apache-2.0): 31M params, **64.5 MB as int8
  GGUF**, flow-matching transformer over Kyutai's Mimi codec, streams while generating,
  explicitly sized for a phone CPU. No training code, no new-language guidance. Its sibling
  40M models — multilingual and per-language — are **CC-BY-NC-4.0 and cannot ship.**
- **Paradee** (`sahilmahendrakar/Paradee-8M-v1.1`, Apache-2.0 **including the training
  pipeline**): 7.88M params, **8.5 MB as int8 ONNX**, distilled from a frozen Kokoro-82M.
  UTMOS 4.39 against the teacher's 4.52 and WER 5.9% against 5.7% — roughly a tenth the size
  at near-teacher quality. ~62× real time on one CPU thread. Trained **on one MacBook Pro
  from 23.9 hours of teacher-generated audio.**

That last sentence is the one that matters for a thesis with no budget: a shippable voice is
a laptop week and a day of synthetic audio, *provided a teacher exists in the language.*
Paradee's own design shows why the language sits almost entirely outside the small model — it
consumes **phoneme IDs**, not letters, so what is language-specific is the grapheme-to-phoneme
front end and the teacher, not the 8 MB of weights. Akan orthography is close to phonemic,
which should make that front end easier than English; whether eSpeak NG or any open G2P covers
Akan is unverified and worth half an hour.

The same teacher-to-student pattern applies to the ears. `mistralai/Voxtral-Mini-4B-Realtime-2602`
is Apache-2.0 streaming ASR with per-language fine-tunes already published for Arabic (with
Morocco's MTNRA, code-switching-focused), Estonian, Turkish and Bengali, plus GGUF conversions
by ggml-org and ONNX by onnx-community. At 4.4 B it is too large to ship on a budget handset,
so it is a **teacher and an annotation engine**: fine-tune on Twi, pseudo-label stall audio,
distil into the phone-sized student. That turns the absence of labelled Twi market audio from a
blocker into a pipeline. Mistral's Voxtral 3 announcement of the same day is API-first and its
weights were not on Hugging Face when checked; do not plan on them.

### What already exists in Akan, which changes the contribution claim

This had not been checked before, and it should have been. Public Akan speech models exist,
including licence-clean ones. From Hugging Face listings — **licences as tagged, none of these
verified for quality, dialect or card accuracy:**

- **ASR:** `asr-africa/wav2vec2-xls-r-akan-50-hours` and `-20-hours` (Apache-2.0, hours stated
  in the name), `techolise/mms-1b-akan-asante-twi` (Asante Twi specifically), plus several
  Whisper-Akan fine-tunes with no licence tag.
- **TTS:** `prince4332/CosyVoice2-0.5B-Akan` and `-Akan-Bible` (**Apache-2.0** — the only
  licence-clean Akan teacher candidate found), and several MMS/VITS Akan voices which are
  CC-BY-NC-4.0 and therefore unusable in a product.

Two consequences. First, the thesis cannot claim Twi ASR as untouched ground; it has to
situate against the asr-africa 20-hour and 50-hour baselines, and the honest contribution is
narrower and better defined — streaming, speaker-gated, code-switched, market-noise, on-device.
Second, the mouth has a candidate path that is licence-clean end to end: CosyVoice2-Akan as
teacher, Paradee's open pipeline as the distiller, ~8–20 MB as the student.

### Licence matrix correction

Several of the most capable options are non-commercial and therefore **barred from shipping**,
not merely discouraged. Distilling from a non-commercial teacher does not launder this: the
generated audio is a derivative work. This constrains the teacher choice, not just the student.

| asset | licence | shippable |
|---|---|---|
| `Voxtral-Mini-4B-Realtime-2602` + Arabic/Estonian fine-tunes | Apache-2.0 | yes |
| `Voxtral-4B-TTS-2603` | CC-BY-NC-4.0 | **no** |
| `gradium/phonon-31m-en` (weights, tokenizer, voices) | Apache-2.0 | yes |
| `gradium/phonon-40m-*` (multilingual and per-language) | CC-BY-NC-4.0 | **no** |
| `Paradee-8M-v1.1` + training pipeline | Apache-2.0 | yes |
| `prince4332/CosyVoice2-0.5B-Akan` | Apache-2.0 (as tagged) | yes, verify |
| `facebook/mms-tts` (incl. `aka`), MMS-derived Akan voices | CC-BY-NC-4.0 | **no** |
| `asr-africa/wav2vec2-xls-r-akan-{20,50}-hours` | Apache-2.0 | yes |
| UGSpeechData | CC-BY-NC-ND | **no** — ND forbids derivatives, so no training at all |
| KasaSpeech, FISD, GESMA | Apache-2.0 / CC-BY-4.0 | yes |

### What this does and does not change

It does not change the first thing to do. Three of the four numbers in §4.2 are still
unmeasured and the verbalisation ratio still decides whether any of this is worth building.
No release moves that. What it changes is the shape of the risk: the model problem is now
three named problems with two credible open paths and one already built, and the binding
constraint has moved off model availability and onto **Twi audio and a Twi G2P**. Those are
data and linguistics problems, and they are the ones a KNUST project is actually well placed
to solve — Mistral did Arabic with Morocco's national lab, and RAIL KNUST already produced
GESMA under CC-BY-4.0.

Unverified and load-bearing, in priority order: whether any open G2P covers Akan; whether
CosyVoice2-Akan is good enough to teach and genuinely Apache-2.0; what the asr-africa Akan
baselines actually score; and whether 23.9 hours of teacher audio is enough for a tonal
language whose standard orthography does not mark tone.

## 5. Open risks, stated honestly

1. **Is the sale even spoken?** The verbalisation ratio — how much of item, quantity, unit
   and price is recoverable from speech alone — has never been measured for a Ghanaian
   market. If it is low, ambient capture cannot work no matter how good the models are.
   This is the first thing to find out and it is cheap to find out.
2. ASR remains on the critical path (§3), though open Akan baselines now exist (§4.3).
3. **The day-end review has no voice.** She cannot read the list, so it must be spoken in
   Twi, and there is no Akan voice we are allowed to ship (§4.3). If this is not solved the
   review tier has nowhere to put what the gate holds, and the coverage numbers in §4.2
   describe a review that cannot happen.
4. **Tone and orthography.** Standard Akan orthography does not mark tone, and the one
   open distillation recipe we have was validated on English. Whether ~24 hours of teacher
   audio suffices for a tonal language is unknown, and whether any open grapheme-to-phoneme
   front end covers Akan is unverified.
5. Decision-model calibration and Twi coverage are unverified.
6. Transsion HiOS and XOS power managers kill long-running services regardless of
   foreground status. Needs the battery-optimisation exemption and testing on a real Tecno.
7. Battery and storage across a full market day, on a 2–4 GB phone.
8. Customer consent for ambient capture, and the assistant's standing if one sells at the
   stall.

---

## 6. First experiments, in order

Each is cheap and each can kill or confirm the one after it. None needs the Android app.

1. **Verbalisation, across capture paths.** Two or three hours of real stall audio at one
   stall, with consent, recorded on all four paths in §2.1 at once — pouch, table, wired
   earbud, Bluetooth earpiece. Hand-label every sale. Measure, per path: what share of
   sales have item, quantity and price recoverable from audio alone? *This is the
   experiment that decides whether the product is possible, and it settles the capture
   argument with data in the same pass.* Log the headset's negotiated codec so we know
   whether we got 8 kHz or 16 kHz.
2. **The voice gate.** On the same audio: can VAD plus speaker verification separate her
   turns from the customer's, per capture path? Report false accept and false reject at the
   chosen threshold. Then: can sale segments be found without transcribing everything?
3. **Row accuracy.** Candidates through recognition plus typed decisions. Measure sale-row
   accuracy and wrong rows per 100 sales, before and after review.
4. **Only then** build the app slice.

Ethics and Data Protection Commission registration gate experiment 1 and take four to eight
weeks, so that paperwork starts now, in parallel.

**What can run during those weeks, needing no field data, no consent and no clearance** —
because experiments 1–3 are all blocked on approval and the model work is not:

- **A Twi voice, end to end, on a laptop.** Check whether an open grapheme-to-phoneme front
  end covers Akan; confirm `prince4332/CosyVoice2-0.5B-Akan` is usable and genuinely
  Apache-2.0; generate a few hours from it; run Paradee's open distillation pipeline; and
  judge the ~8 MB result by pairwise listening tests with Twi speakers, not by a text metric.
  Pass: Twi speakers prefer it to nothing and can repeat back amounts and names correctly.
  This de-risks risk 3, costs a laptop week, and if it fails it fails now rather than after
  the fieldwork.
- **Score the synthesiser with the recogniser.** Run an Akan ASR model over the TTS output
  and track the error rate as a regression test, the way Phonon and Paradee both report
  theirs. Two models we need anyway, checking each other, with no human in the loop for
  routine checks.
- **Re-run the gate's numbers.** Every figure in §4.2 predates the determinism fix (§4.2,
  caveats), so they are one draw rather than a measurement and have to be measured again.
  Then situate the result against the public Akan ASR baselines in §4.3, which nobody has
  checked yet.

---

## 7. App scope

Frozen at compiles-and-installs. The slice, when we get to it, is: start-of-day session,
continuous capture, local encrypted storage, day-end review screen with spoken summary in
Twi, confirmed rows into Room. Not push-to-talk.

Out of scope until the experiments say otherwise: beta-programme management, the Firestore
usage dashboard, biometric auth, the offline sync queue and conflict resolver (they sync to
a server that does not exist), and speaker enrolment. The 217-byte placeholder model should
be deleted rather than replaced.

Note that speaker identification comes *back* into scope under this architecture, unlike
the previous draft: when the phone is listening all day, something has to tell her voice
from a customer's. It is a component of capture, not the research contribution, and it
needs a real model.

---

## 8. Working rules

These exist because their absence produced 234 syntax errors, 607 test compile errors,
~60 status documents asserting a build that had never built, and a text file standing in
for the core model.

1. Nothing enters the repository that has not compiled. CI is the authority, not a status
   document.
2. No document asserts a capability. Capabilities are asserted by a test or a measurement,
   with the number and the device named.
3. A placeholder is named in the file, and the code path that needs it reports unavailable.
4. A research finding that rejects a design closes that design until a later finding
   reopens it — and a finding about *one implementation* does not close the whole idea.
   That second clause is why the previous draft of this document was wrong.

---

## 9. Sources read for §3 and §4

- Silas Asamoah (@geeksilas) on sika-agent — fetched directly.
- Caine Ardayfio (@CaineArdayfio), "Mobile Use + Jev for audio" — fetched directly.
- Jev: [Towards AI hands-on](https://pub.towardsai.net/jev-by-typesafe-ai-a-hands-on-look-at-a-model-that-only-makes-decisions-7b22cf5f6608),
  [how Jev works](https://newsletter.victordibia.com/p/how-jev-works-calibrated-decision),
  [Pinocchio paper](https://arxiv.org/pdf/2609.24881) for the calibration figures.
- Mobile Use: [minitap-ai/mobile-use](https://gittrend.io/repo/minitap-ai/mobile-use).
- Android versions in Ghana: [Statcounter](https://gs.statcounter.com/android-version-market-share/all/ghana).
- Bluetooth voice: [Android SCO audio management](https://source.android.com/docs/core/audio/sco-audio-mgmt),
  [mSBC codec](https://ittiam.com/driving-high-performance-bluetooth-devices-msbc-codec/).
- Not opened: the Gemini share link, and `x.com` and `mistral.ai` and `gradium.ai` are all
  blocked from this environment (HTTP 402 and DNS refusal respectively). Where a post could
  not be read, the owner pasted its text and that is noted as the source.

Sources read for §4.3 (20 October 2026). Every size and licence below comes from the model
card or the Hugging Face API, not from an announcement or press coverage:

- Voxtral: [`Voxtral-Mini-4B-Realtime-2602`](https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-2602),
  [`Voxtral-Mini-4B-Realtime-Arabic`](https://huggingface.co/mistralai/Voxtral-Mini-4B-Realtime-Arabic)
  (4.4 B, Apache-2.0, co-developed with Morocco's MTNRA, CER 8.82% at 480 ms delay against
  7.91% non-streaming), and the HF model listing for the Estonian, Turkish and Bengali
  fine-tunes and the ggml-org/onnx-community/MLX conversions. Voxtral 3 was announced the
  same day via the owner's pasted post text; no weights were on Hugging Face when checked.
- Phonon: [`gradium/phonon-31m-en`](https://huggingface.co/gradium/phonon-31m-en) (Apache-2.0,
  64.5 MB q8 GGUF, Mimi codec, built on Kyutai's Pocket TTS),
  [`gradium/phonon-40m-multilingual`](https://huggingface.co/gradium/phonon-40m-multilingual)
  (CC-BY-NC-4.0, five languages, Elo and WER tables). The 7 ms time-to-first-audio figure is
  from [their release post](https://gradium.ai/blog/release-phonon-open-weight) via search,
  not the card; the card gives no latency number.
- Paradee: [`sahilmahendrakar/Paradee-8M-v1.1`](https://huggingface.co/sahilmahendrakar/Paradee-8M-v1.1)
  and [the code](https://github.com/sahilmahendrakar/paradee) — 7.88 M params, 8.5 MB int8
  ONNX, Apache-2.0 training pipeline, distilled from a frozen Kokoro-82M on 23.9 hours over
  12,000 WikiText-103 sentences, UTMOS 4.39 vs 4.52 and WER 5.9% vs 5.7%, trained on one
  MacBook Pro. Announced via the owner's pasted post text.
- Existing Akan models: Hugging Face API listing, searched for "akan". Licences as tagged;
  **none of these has been downloaded, listened to, or evaluated**, and the Akan entry in
  `facebook/mms-tts` was confirmed only as far as the `aka` config files.
- Name collision worth noting: `FermionResearch/Phonon-1`/`Phonon-2` and their CoreML, ONNX,
  GGUF and MLX ports are **ASR** models unrelated to Gradium's TTS Phonon.
