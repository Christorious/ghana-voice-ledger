# Voice Ledger — Plan of Record

**Status: DRAFT, 19 October 2026. Needs Christian's confirmation and a supervisor's sign-off before it governs anything.**

This document exists because the build and the research had drifted apart. It does not
introduce new analysis. It takes the findings already in
`VOICE_BOOKKEEPER_DATA_AND_FEASIBILITY.md` (30 September 2026) and turns them into
decisions, scope boundaries and gates, so that the next piece of work can be checked
against something.

Where this document and that memo disagree, the memo wins and this document is wrong.

---

## 1. The misalignment

The memo assessed four capture architectures and rejected one of them as the product's
core. The app implements that one.

| Ground | Finding (memo §7) |
|---|---|
| Acoustic | Kejetia market is 68–77 dBA. Normal speech to a phone half a metre away is at or below 0 dB SNR. |
| Recognition | Best measured Twi free-speech word error rate is ~45%. |
| Platform | Android 14+ forbids starting a microphone foreground service from the background or at boot; Transsion HiOS/XOS kill background services anyway. |
| Field evidence | Microsoft Research's Dukawalla study in Nairobi — the only published field trial of voice business-data capture in Africa — hit exactly these problems, plus discomfort at speaking amounts aloud. Its participants valued the generated charts more than the voice channel. |

Four independent grounds, one conclusion: an always-listening agent that hears bargaining
at a stall and books the sale does not work today.

The repository nevertheless contains `VoiceAgentService` as a microphone foreground
service, `VoiceSessionCoordinator`, `VADManager`, `PowerOptimizationService` for the
battery cost of continuous listening, and a speaker-verification pipeline whose purpose is
to decide whose voice woke it. That is the rejected architecture, built out.

**This was not a planning failure. The plan was written. It was never allowed to change
the build.** The fix is therefore procedural as much as architectural: see §7.

---

## 2. What has to be settled before any more code

The memo's §12 is explicit that the open questions are not technical. Two must be answered
by talking to traders, not by us:

**2.1 Which product is this?** The origin story is an owner wanting to know what was sold
while she was away — that is an owner *monitoring an assistant*. An owner recording *her
own* sales is a different product with different ethics: the assistant is a person being
recorded at work, with weaker standing, and a misheard amount can look like theft. The
project must pick one, and if both, the order.

**2.2 What does she actually want?** Nobody has quantified where value leaks at a Ghanaian
stall — forgotten credit, mistaken change, spoilage, sales made by others. Without that,
there is no value hypothesis and no evaluation baseline.

**Immediate action, and it is not a coding task:** go back to the trader in Tema, and to
two or three traders like her. Twenty minutes each. What would you want a phone to do about
your money, and what would you never allow it to do? This needs ethics approval and Data
Protection Commission registration, which the memo puts at four to eight weeks — so the
paperwork starts now, in parallel with everything in §5.

---

## 3. Decisions

**D1 — Capture is trader-initiated. There is no ambient mode in v1.**
Push-to-talk, or a voice note. The phone goes to the mouth, which converts the acoustic
problem from far-field to close-talk; the trader initiates every capture, which fixes the
consent story; and a short utterance over a closed product vocabulary with a dedicated
number grammar is a far easier recognition task than open transcription. Ambient mode is
not cancelled, it is *unproven* — it returns only if a later stage earns it.

**D2 — Every booked amount is read back before it is committed.**
Numbers are over-represented in recognition errors, and constrained decoding fixes format,
not meaning: a wrongly heard amount arrives well formed. The amount slot needs its own
defence — a number-spotting grammar, inverse text normalisation for Twi and Pidgin cedi
forms, and explicit spoken read-back with confirmation. This is not a UX nicety; it is the
mechanism that makes the error rate survivable.

**D3 — Mobile-money SMS ingestion is a first-class input, not a later feature.**
The memo calls it "the highest-value component per unit of effort": exact amounts for the
digital share of sales with no speech at all, leaving voice for cash and credit entries,
which are the entries traders most want tracked.

**D4 — Speaker verification is demoted from contribution to component.**
This is a reversal. Under D1 the trader presses a button, so the system does not need to
decide whose voice woke it. A speaker gate still has a narrower job — rejecting a
customer's voice captured inside her utterance — but it is no longer load-bearing and it is
no longer where the research contribution sits. The 217-byte placeholder model in
`app/src/main/assets/speaker_embedding_model.tflite` should be deleted rather than
replaced.

**D5 — Local-first, and private from more than the tax authority.**
The list of people the ledger must be private from includes husbands and relatives sharing
a phone, neighbouring traders, market association officers, the assistant, and us. That
means: no account, storage on the device by default, spoken read-back that does not require
anyone else to see the screen, an obvious delete-everything control, and nothing leaving the
phone unless she sends it.

---

## 4. The thesis claim

The memo names three gaps nobody has filled: a field-validated numeric confirmation
dialogue for low-literacy users; a voice ledger in any Ghanaian language; and any
randomised comparison of voice against touch or paper on persistence and accuracy.

**Proposed claim:** a trader-initiated, read-back-confirmed voice ledger in Twi can record
cash sales accurately enough, and be trusted enough, that Ghanaian market traders keep
using it where paper and typed apps are abandoned.

That is one claim with four chapters, matching the four axes but ordering them:

| Axis | Role |
|---|---|
| Numeric confirmation dialogue for low-literacy users | **The contribution.** Unfilled, measurable, and ours. |
| Low-resource Twi/Ga ASR | Adapted component. We fine-tune and report amount-slot accuracy; we do not claim to beat frontier labs on WER. |
| On-device performance on a Tecno Spark-class phone | Constraint. Must be measured, not claimed. |
| Field study with traders | Validation, and the source of the behaviour evidence. |

**The metric that matters is not WER and not EER.** For a ledger, a missed sale is
recoverable — she notices and repeats it. A *wrong* sale silently corrupts her books and, in
the assistant case, can look like theft. So the headline number is **wrong rows per 100
sales after confirmation**, and the system is tuned for precision over recall throughout.

---

## 5. Gates

Taken from memo §9. Nothing proceeds to the next stage until the gate is met.

**Stage 1 — Make the app truthful and measurable.** No new data needed.
Replace the simulated recogniser with a real one (sherpa-onnx); load a real model and
Silero VAD; measure real-time factor, memory and battery on a Tecno Spark-class device;
remove the mock speaker identifier; correct the licence matrix and the numeral glosses
(`dunum` is 15 and `aduonum` is 50, not 10 and 20 — the Kiro prompt documents are wrong and
the Python rules are right); build the evaluation set of a few hundred self-recorded market
utterances with ground-truth tuples, because read-speech error rates flatter every model.
*Gate: the app transcribes one real utterance on a real phone, and we have measured numbers
for it.*

**Stage 2 — Data pilot**, run in parallel with ethics and DPC registration.
*Gate: amount-slot accuracy on the self-recorded market set, after read-back and
confirmation, above 90%, with sale-event F1 beating the rules baseline by a measurable
margin.*

**Stage 3 — Closed pilot**, 30–50 traders recruited through market associations, susu
collectors or mobile-money agents. Push-to-talk entry, mobile-money SMS ingestion, spoken
daily summary.
*Gate: day-30 retention, entries per day, share of credit entries, confirmation-correction
rate.*

Only after Stage 3 does ambient mode, a feature-phone channel or a credit export deserve
any investment.

### Stated in advance: what would count as failure

Memo §12 asks for this, so that the origin story does not become a reason to continue past
the point where traders have answered. Proposed thresholds, to be fixed with the supervisor
before the pilot starts:

- Wrong rows above 5 per 100 sales after confirmation → the recognition path is not safe to
  book from; fall back to typed amounts with voice for the item only.
- Day-30 retention below 30% → the channel is not wanted; stop and ask why.
- Traders declining to be recorded above some agreed share → the premise is wrong.

---

## 6. App scope under D1

The app is frozen at "compiles and installs". It now gets smaller, not larger.

**Keep, because the vertical slice needs it:** `MainActivity`, the dashboard and today's
total, `AudioCaptureController` (button-triggered), entity extraction and the Twi number
rules, Room with `Transaction` and `DailySummary`, the transaction repositories, history,
and the spoken daily summary.

**Build, because it does not exist and D2 depends on it:** the read-back confirmation
dialogue; mobile-money SMS ingestion; a real speech recognition engine.

**Remove or disable, because it serves the rejected architecture or an absent server:**
`VoiceAgentService` as an ambient foreground service, `VoiceSessionCoordinator`,
`PowerOptimizationService` and `PowerManager`'s continuous-listening management,
the speaker-identification pipeline and `SpeakerProfile` enrolment, `BetaTestingManager`,
`UsageDashboardService` and its Firestore client, `OfflineQueueManager`,
`ConflictResolver` and `OfflineFirstRepository` (they sync to a server that does not
exist), biometric auth, `AnalyticsIntegration` and `PerformanceMonitoringService`.

`VADManager` survives in reduced form: endpointing *within* a press, not deciding when to
wake.

This is a large deletion and it is the point. Roughly 230 files were written for a product
shape the evidence does not support.

---

## 7. Working rules

The procedural half of the fix. These exist because their absence is what produced 234
syntax errors, 607 test compile errors, ~60 status documents asserting a build that had
never built, and a placeholder standing in for the core model.

1. **Nothing enters the repository that has not compiled.** CI is the authority on this, not
   a status document.
2. **No status document asserts a capability.** Capabilities are asserted by a test or a
   measurement, with the number and the device named.
3. **A placeholder is named in the file.** A 217-byte text file called
   `speaker_embedding_model.tflite` is a trap. If an artefact does not exist, the code path
   that needs it reports unavailable.
4. **Research findings are binding.** A memo in `docs/research/` that rejects an
   architecture closes that architecture until a later memo reopens it. If we disagree, we
   write the disagreement down and decide — we do not route around it in code.
5. **The field study has lead time.** Ethics and DPC registration run in parallel with
   engineering from day one, not after it.

---

## 8. Open items

- §2.1 and §2.2 are unanswered and only traders can answer them.
- Supervisor sign-off on §4's claim and §5's thresholds.
- The `finetune_mms.py` `norm()` bug is unfixed: `re.sub(r"\d+", lambda m: f" {NUM.get(m.group(), '')} ", t)`
  silently deletes any number not in `NUM`, which covers only 1–10, 20, 50 and 100. "30
  cedis" trains as " cedis". This destroys precisely the tokens D2 exists to protect, and it
  must be fixed before any fine-tuning run is trusted.
- Licence matrix corrections from memo §11 are not yet applied in `sikabook-models`.
- `NetworkUtilsTest` is two test files welded together — 39 tests, 22 Robolectric, 15 mockk,
  with a setup block supporting only the mockk half. Under §6 most of what it tests may be
  deleted anyway; resolve scope first.
