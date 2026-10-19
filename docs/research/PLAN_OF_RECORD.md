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

## 5. Open risks, stated honestly

1. **Is the sale even spoken?** The verbalisation ratio — how much of item, quantity, unit
   and price is recoverable from speech alone — has never been measured for a Ghanaian
   market. If it is low, ambient capture cannot work no matter how good the models are.
   This is the first thing to find out and it is cheap to find out.
2. ASR remains on the critical path (§3).
3. Decision-model calibration and Twi coverage are unverified.
4. Transsion HiOS and XOS power managers kill long-running services regardless of
   foreground status. Needs the battery-optimisation exemption and testing on a real Tecno.
5. Battery and storage across a full market day, on a 2–4 GB phone.
6. Customer consent for ambient capture, and the assistant's standing if one sells at the
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
- Not opened: the two t.co links and the Gemini share link were blocked from this
  environment. Their content is not reflected here.
