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
| Open of day | She clips on a wired lapel mic, phone in pocket or pouch, taps once | One tap |
| Trading | Continuous close-talk capture; silence dropped; stored encrypted on device | **None** |
| Understanding | Candidate sale segments detected, then recognised and decided, in batches when idle or charging | None |
| Close of day | The phone speaks her day back in her language; she confirms or corrects | A few minutes |
| Ledger | Only confirmed rows are committed | — |

Four properties of this shape matter.

**Close-talk, not far-field.** A wired lapel microphone with the phone in her pocket costs a
few dollars and converts the acoustic problem from far-field to close-talk. This is the
single change that makes capture viable at Kejetia's 68–77 dBA, and the feasibility memo
(§7.2) already said so; the field study protocol already specifies this exact rig.

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

---

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

1. **Verbalisation.** Two or three hours of real stall audio, lapel mic, consent, one
   stall. Hand-label every sale. Measure: what share of sales have item, quantity and
   price recoverable from audio alone? *This is the experiment that decides whether the
   product is possible.*
2. **Candidate detection.** On that audio, can sale segments be found at all, without
   transcribing everything?
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
- Not opened: the two t.co links and the Gemini share link were blocked from this
  environment. Their content is not reflected here.
