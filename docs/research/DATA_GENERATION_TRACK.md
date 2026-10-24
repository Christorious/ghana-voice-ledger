# Data generation as a tracked skill

**Owner:** Christian Dzidula Dotsey (KNUST)
**Opened:** 20 October 2026
**Status:** standing plan. Not a reading list — each method is learned by applying it to a data
problem this project already has, and each one produces something a thesis chapter can cite.

---

## Why this document exists

The owner's words: *"knowing how to generate data for research in Africa would be a gold mine…
I really want to know how to do this thing… I just have to focus, and focusing on this thing
for a long time is something I want help with."*

Two things make that worth formalising rather than leaving in a conversation.

**The instinct is correct and measurable.** Longpre et al. audited roughly 4,000 public datasets
spanning 1990–2024 and found that organisations affiliated with Africa or South America account
for **under 0.2% of all tokens or hours** in any modality, that creator concentration for speech
sits at a Gini coefficient of 0.86, and that **geographic representation has not improved since
2013**. The same audit found that while fewer than a third of datasets are restrictively
licensed, **over 80% of the source content inside widely-used datasets carries non-commercial
restrictions**. That is a measured hole where a skill should be, and this project walked into
the licensing half of it twice in one day (§4.3.4 of the plan of record).

**The skill and the thesis are the same work.** This project needs Twi audio it does not have.
Learning data generation on anything else would be a second job. Everything below is scoped so
that the method, the project deliverable and the chapter section are one artefact.

## The frame, before any method

Say the hard part first so it does not get lost in enthusiasm.

**Found data and synthetic data cannot answer the one question that decides this product.**
Whether a Ghanaian market trader says the item and the price out loud, and whether it is audible
from where her own phone sits, is a fact about a setting and a microphone position. It is not a
property of audio that exists somewhere else. A vlogger's camera microphone is not a phone in a
cloth pouch, and no found video comes with the hand-written ledger to check the transcript
against.

**But the sequencing in §6 of the plan of record was wrong, and the owner's instinct fixed it.**
Scraped Ghanaian market audio can give a first estimate of verbalisation, the vocabulary that
actually occurs, and the real noise profile — for nothing, with no ethics clearance, before
anyone goes anywhere. That turns the stall recording from a discovery exercise into a
confirmation, and makes two hours at one stall worth several times what it would have been.
So: **the field recording shrinks, it does not vanish.** Do the found-data work first.

**Step zero, unverified and cheap.** Nobody has checked whether Ghanaian market video exists in
usable quantity, with audible Twi transaction speech rather than music beds and voiceover. An
hour of searching settles it, and if the answer is no, Method 1 changes shape rather than the
whole plan collapsing.

---

## Method 1 — The found-data pipeline

**The paper.** Javed, Doddapaneni, Raman, Bhogale, Ramesh, Kunchukuttan, Kumar & Khapra,
*Towards Building ASR Systems for the Next Billion Users*, AAAI 2022
([arXiv 2111.03945](https://arxiv.org/abs/2111.03945), 86 citations). They curated **17,000
hours of raw speech across 40 Indian languages** from YouTube and a government radio
broadcaster, and the paper writes the pipeline out rather than gesturing at it:

1. A **manual check per video** — is the language actually stated, is the audio free of
   background music — with workers recording URL and target language in a sheet.
2. `youtube-dl` to fetch, `FFmpeg` to extract audio.
3. **Resample to 16 kHz mono.** Their sources ranged from 8 kHz to 44 kHz and some were not mono.
4. `py-webrtcvad` to strip long silences, with an aggressiveness parameter from 0 to 3.

The detail worth stealing is why their second source needed none of step 1: **government radio
was already clean**, professionally produced, with the language stated. Institutional audio beats
user-generated audio per hour of human effort.

**Supporting work, for source triage.** VoxPopuli (Wang et al., ACL 2021) took **400,000
unlabelled hours** from parliamentary proceedings using energy-based VAD, 15–30 second clips, a
maximum of 2 seconds of continuous silence, dropping ~16% of the data. XEUS (Chen et al., EMNLP
2024) went the other way and crawled **WikiTongues**, a grassroots preservation archive — 821
recordings, ~70 hours, ~700 languages, CC-BY-NC — proving that a niche archive can beat YouTube
for coverage of rare languages. And *Towards True Speech-to-Speech* (ICLR 2026) offers a clean
heuristic from 9 million crawled hours: **podcasts for anything needing reliable transcripts**
(cleaner recordings, ASR works on them), **video for unsupervised pre-training** (noisier, but
that noise buys robustness).

**Applied here.** Three Ghanaian source classes, in order of effort per usable hour:
Ghanaian radio and parliamentary broadcast (institutional, clean, Twi and Twi–English);
market and street-food vlogs (noisy, but the only source with trader–customer exchanges);
and church or Bible audio, which is abundant and in-register for nothing we need — note that
Akuapem Twi in `ghana-speech` appears to be Bible text, and register mismatch is why that
matters.

### 1.1 How to actually run it (Chai, October 2026)

A practitioner's write-up — Bella Chai, *"You Found a Million Audio Links. How Many Can You
Actually Use?"*, 20 October 2026 — gives the operational detail the papers omit, from running
this at a scale of tens of millions of URLs. The engineering is mostly overscaled for us; the
**discipline** is not, and three parts transfer directly.

**Start from indexes, not searches.** Podcast directories (Apple Podcasts, gPodder) expose RSS
feeds, and an RSS feed enumerates every episode's audio URL in one document — title, date,
GUID, `enclosure` URL — without visiting a single episode page. Same move as the papers, stated
as a procedure. Film and TV catalogues and the YouTube Data API are the other entry points.

**Read the whole feed. This is the finding worth the most to us.** Her 200 feeds contained
~71,000 episode entries; a 99-item-per-feed cap in her parser meant she read about 20,000 —
**28%** — and nothing told her. Her conclusion: *"before looking for more sources, I needed to
check whether I was fully reading the ones I already had. Data missed during discovery won't be
recovered automatically at the download stage."* At her scale that is waste. At ours it is
fatal, because Ghanaian-language feeds will be scarce and losing 70% of a thin seam leaves
nothing. Parsing fully still cannot recover episodes a publisher has dropped from the current
feed, so expect a hole in older history and say so.

**Provenance must survive deduplication — and this binds harder for us than for her.** The same
show appears in several directories, so the same feed and the same audio URLs get discovered
repeatedly. Deleting duplicates shrinks the inventory and destroys the record of where each item
came from. She merges duplicate feeds while keeping every directory → show → episode link, and
records both the original `asset_url` and the post-redirect `final_url`, deduplicating on the
latter. **Method 3 of this document depends on exactly that chain**: a corpus whose provenance
was deduplicated away cannot be licence-audited, which makes it unusable for a thesis regardless
of its size.

**Verify before downloading, and record why a check failed.** HTTP `HEAD` first — status,
`Content-Type`, `Content-Length`. When that is not enough (many servers omit metadata or reject
HEAD), a `Range: bytes=0-4095` GET and inspect the first 4 KiB for an audio magic number; a
200 with an HTML body is common. Keep the method, response and failure reason, because **a 404
and a timeout are different facts**: one is a dead resource, the other is an unfinished check.

**What to skip.** Her Source Library, her concurrency tuning and her resumable checkpointing are
answers to a scale we will not reach. Her own concurrency experiment is instructive about
restraint rather than about concurrency: moving 2,000 URLs from 16 to 32 workers raised
throughput 50.3%, raised timeouts from 10 to 15, and let four *fewer* URLs pass — and she says
plainly that *"one experiment with such a small difference doesn't establish that 16 is better
than 32."* At a few thousand URLs this whole stage is a few hundred lines of Python.

**And skip the film and television half entirely.** Her acquisition route there is BitTorrent
with a cached-HTTPS fallback. We need conversational Twi, not dubbed film audio, and the
legal position is bad. She draws the line herself, and it is the same wall this project hit with
`nano-twi`: *"A publicly accessible resource isn't automatically authorized for model training.
Access, permission to download, and the right to use data for training are separate matters."*

### 1.2 Where this runs: not in a cloud session

Attempting step zero from the cloud container settled this. A probe of the public iTunes Search
API for Twi, Akan, Ewe, Ga, Dagbani and Ghana podcast feeds returned `403 Forbidden` on the
proxy's CONNECT for every query, as `treg.to`, `mistral.ai`, `gradium.ai` and others had before
it. The pipeline is network work almost end to end, against hosts a restricted egress policy
refuses by default.

Widening the allowlist would fix the symptom, but three properties say this belongs elsewhere:
it transfers audio at volume, it runs for hours and resumes, and its output must persist. A
container that is reclaimed after inactivity has none of those. **Build it on the owner's own
machine** — the bridge environments already point at `DESKTOP-DOTS-SYSTEMS` — and bring the
resulting manifest, not the audio, back into the repository.

**Artefact.** A URL manifest plus pipeline code plus a first verbalisation estimate — and
**not** redistributed audio. Javed et al. released URLs and code, not the audio, and that is
the move that keeps a corpus publishable. Assume you cannot redistribute scraped audio.

**Pass/fail.** Pass: ≥20 hours of segmented Twi speech with timestamps and provenance, plus a
verbalisation estimate from at least 50 observable transactions. Fail: market video turns out to
be voiceover over music, in which case radio and parliament carry the method and verbalisation
waits for the stall. Before either, the unresolved prior question stands: **do Ghanaian-language
feeds exist in usable quantity at all?** One hour against an unblocked index answers it, and
nothing downstream is worth building until it does.

---

## Method 2 — Synthetic generation and back-translation

**The paper.** Wang, Xu, Lu & Cheng, *From Tens of Hours to Tens of Thousands: Scaling
Back-Translation for Speech Recognition*, EMNLP 2025. **Tens of hours of real transcribed speech
are enough to train a TTS that then generates synthetic speech at hundreds of times the original
volume.** They produced over 500,000 hours across ten languages, continued pre-training
Whisper-large-v3 on it, and cut transcription error by **more than 30% on average**. Critically
they also built an **intelligibility-based assessment framework with explicit thresholds for
when synthetic data starts helping rather than hurting** — which is the part most people skip and
the part that makes it science.

**Supporting work.** Bartelds, San, McDonnell, Jurafsky & Wieling, *Making More of Little Data*,
ACL 2023: from **24 minutes** of transcribed speech, self-training bought up to **20.5%**
relative WER reduction and **TTS augmentation bought up to 25.5%** — the synthetic voice beat
self-training. Their TTS was FastSpeech2 on about two hours of single-speaker read speech.
Pine, Wells, Brinklow, Littell & Richmond, ACL 2022: neural TTS from **25 minutes to 3.46
hours**, with **FastSpeech2 at one hour matching Tacotron2 at ten** — and an explicit warning
not to let Tacotron2 set anyone's sense of the data floor. And Baevski, Hsu, Conneau & Auli,
*Unsupervised Speech Recognition* (wav2vec-U, NeurIPS 2021, 309 citations): a recogniser from
**no labelled data at all**, phoneme error rate 11.3 on TIMIT, WER 5.9 on Librispeech
test-other, tested on Swahili, Kyrgyz and Tatar.

**Applied here.** This is already the plan of record's §4.3.3 and it has a concrete path: train
the Twi voice on Waxal's `data/TTS/twi/` read-script data (CC-BY-4.0), then back-translate Twi
text into synthetic speech to train the ears. Text sources: `ffatty/plaintext-wikipedia-full-twi`
(CC-BY-SA-3.0, but only 4.27 MB and wrong register) plus **market-domain text we write
ourselves** — prices, quantities, fish names, customer names. Writing 5,000 in-domain Twi
sentences is a day's work with native speakers and is worth more than 10× that volume of
Wikipedia.

**Artefact.** The Twi voice, a synthetic corpus, and **the intelligibility threshold measured
for Twi** — where synthetic data stops helping. The last one is the publishable part, because
nobody has measured it for a tonal Ghanaian language.

**Pass/fail.** Pass: synthetic augmentation improves held-out CER on real Twi speech. Fail: it
does not, in which case the threshold finding is still a result and still worth reporting.

---

## Method 3 — Quality, bias and provenance auditing

**The paper.** Lau, Chen, Fang, Xu, Chen & Golik, *Data Quality Issues in Multilingual Speech
Datasets: The Need for Sociolinguistic Awareness and Proactive Language Planning*, ACL 2025.
They audited **Mozilla Common Voice 17.0, FLEURS and VoxPopuli** and found quality problems
severe enough to "obfuscate downstream evaluation results while creating an illusion of
success" — with macro-level problems concentrated in **less institutionalised, under-resourced
languages**. Their four-point checklist for creating a new corpus:

1. **Sociolinguistic assessment first** — demographics, literacy rates, writing systems,
   diglossia, language ideologies.
2. **Language planning inside the dataset design** — vernacular or formal register, which
   orthography, decided *with* linguists and native speakers rather than assumed.
3. **Proactive prescription** — explicit transcription and recording guidelines, which matters
   most exactly where literacy is low or orthography unstandardised.
4. **Multi-level quality assurance** on both text and audio, automatic checks plus human review.

**Applied here, and it is urgent rather than educational.** Twi is a textbook case of their
"less institutionalised" category: tone unmarked in standard orthography, three dialects with
Asante and Akuapem and Fante pulling apart, ATR vowel harmony ambiguities that someone has
already built [a grapheme fixer](https://github.com/GhanaOpenAI/twi-grapheme-fixer) for. And
this project has a live mess to clean: `nano-twi` is tagged MIT while trained on CC-BY-NC data,
and **KasaSpeech's licence is disputed** between this project's own matrix (Apache-2.0) and the
`xlsr-twi-codeswitch-ipa` card (AGPL-3.0) — and `sikabook-models` already trains on KasaSpeech.

**Artefact.** A licence-and-provenance audit of every Akan speech resource, with each chain
traced to its training data and stated as a table. This is desk work costing hours, the project
is blocked without it, and a careful version of it is a resource paper in its own right —
because as far as we can tell nobody has done it for Ghanaian speech.

**Pass/fail.** Pass: every resource in the plan's licence matrix has its chain traced to primary
sources and the KasaSpeech question is settled. Fail is not an option here; this one gates the
others.

---

## Method 4 — Governance and licensing

**The paper.** Rajab, Aremu, Chimoto, Dunbar, Morrissey, Thior, Potgieter, Ojo, Tonja, Nekoto,
Moiloa, Abbott, Marivate & Rosman, *The Esethu Framework: Reimagining Sustainable Dataset
Governance and Curation for Low-Resource Languages*, ACL 2025
([arXiv 2502.15916](https://arxiv.org/abs/2502.15916)). A data curation framework built to
**ensure equitable benefit-sharing with the communities whose language it is**, carrying a novel
community-centric **Esethu licence**, proven out on an isiXhosa speech corpus (ViXSD). The paper
compares licence regimes — Nwulite Obodo, CC-BY-SA 4.0, Apache 2.0, Kaitiakitanga, Esethu —
against what each actually provides.

Their related-work section doubles as the map of this field: Oshiwambo (5,000+ sentences,
**never released because no African-centred licence existed that would benefit the creators** —
the cautionary tale), AfroDigit across 38 African languages, BibleTTS (86 hours, 10 languages),
AfricanVoices, AfriSpeech-200 (200 hours pan-African), Kencorpus (177 hours, 3 Kenyan
languages), Zambezi Voice (79 labelled hours plus 525 hours of radio), NaijaVoices (~1,800 hours
of Yoruba, Igbo and Hausa), Lwazi, NCHLT. Their critique of all of it: **top-down, with no
mechanism for community governance or equitable attribution.**

**Applied here.** This project will, if it does fieldwork, hold recordings of Ghanaian market
traders and their customers. Under what licence, held by whom, and with what benefit to the
traders? That has to be answered **before collection**, not after, and it belongs in the ethics
submission that is already in progress. The Oshiwambo story is what happens when it is answered
late: a finished corpus nobody can use.

**Artefact.** A written licence decision for this project's own collected data, with the Esethu
comparison applied to the Ghanaian case, filed alongside
`FIELD_STUDY_PROTOCOL_AND_CONSENT.md`.

**Pass/fail.** Pass: the licence is decided and written before the first recording. Fail: a
corpus exists that cannot be shared.

---

## Method 5 — Preference data: making a voice better by ranking its own output

**The source.** Bella Chai, *"Your TTS Model Can Speak. Why Does It Still Sound Wrong?"*,
18 October 2026, drawing on Hussain et al., *Koel-TTS* (EMNLP 2025) and *Preference Alignment
Improves Language Model-Based TTS*. This is the only method here that generates its data from a
model we already have, which is why it comes last.

**Sequencing first, because the post assumes a stage we are not at.** Post-training presumes a
base model that already produces speech. We have no Twi voice at all. The order is: train from
Waxal (Method 2), measure, and reach for preference alignment only if the result is
intelligible-but-wrong rather than absent. Two of its decisions nevertheless bind *before* the
first training run:

- **Classifier-free guidance requires condition dropout during base training.** Koel-TTS drops
  the text and reference-audio conditioning with probability 0.1 while training the base model,
  which teaches one model to make both conditional and unconditional predictions; at inference
  they are combined as `γ·ℓ_c + (1−γ)·ℓ_u`. Retrofitting is not possible. Decide now.
- **The evaluation harness comes before the model**, which is the same habit the Semitic and
  Hebrew groups follow (§4.3.1 of the plan of record) and the same one Method 3 argues for.

**How preference pairs are built.** For each text and reference clip, hold the conditioning and
sampling settings fixed, draw several samples, score them, rank them, and keep a strong one as
*chosen* and a weak one as *rejected*. Three metrics are used because all three automate:

| Dimension | Metric | Direction |
|---|---|---|
| Text accuracy | CER or WER from an ASR pass over the output | lower better |
| Speaker similarity | cosine between speaker embeddings of output and reference | higher better |
| Perceptual quality | a proxy MOS predictor such as UTMOS | higher better |

Two practical findings worth having. **Do not use ground-truth recordings as the chosen side**:
pairing a human recording against a model generation performed *worse* than drawing both sides
from the model's own output, because the distribution gap in discrete token space makes the
comparison too easy to teach anything about quality. And **a difficult-text set built from
observed failures** beats more random text — she collected real failure cases, grouped them by
error type and expanded each group with an LLM, then verified the new items actually trip the
base model. Ours is better defined than hers: tone-minimal pairs plus the ledger vocabulary of
numbers, prices, fish names and customer names.

### 5.1 Every one of those three metrics is blind to tone

This is the contribution, and it follows from facts already established in this project.

- **CER cannot see a tone error.** It transcribes the output and compares against the input
  text. Standard Akan orthography marks no tone, so the reference carries none, so a tone error
  produces no character error. Blind by construction.
- **Speaker similarity cannot see it.** It asks whether this is the right voice. A correctly
  voiced utterance with the wrong tone scores high.
- **Proxy MOS cannot see it.** UTMOS predicts naturalness, trained overwhelmingly on English and
  Japanese. It has no notion that a pitch contour is *lexically* wrong in Akan — and whether it
  transfers to a tonal African language at all is itself unvalidated.

So a Twi voice can score well on all three while saying *Ama dà ha*, "Ama is sleeping here",
when the text said *Ama dá ha*, "Ama sleeps here" — the Adebara and Abdul-Mageed example
already cited in the plan of record. Optimising DPO against these three would make the model
confidently better at everything except the distinction the language actually uses.

**Three independent lines now converge on the same hole.** MFCCs smear tone across the low mel
bands rather than representing it (`verifier/tests/pitch_leak.py`). pGSLM's answer to exactly
this, in the textless-speech literature, is to model prosody as a **separate stream** beside the
content units. And the standard TTS preference metrics cannot measure it. A **fourth preference
dimension — tone accuracy** — is therefore a real and well-motivated contribution rather than a
tidy-up.

**A buildable first version.** Extract F0 over voiced regions of the generated and the reference
audio, mean-normalise each utterance in log-F0 to remove speaker pitch, and score contour
agreement on syllable nuclei. Validate it against a targeted test set of tone-minimal pairs
drawn from the Waxal Akan transcripts — which is the same count Method 3 produces, reused.

**Artefact.** A tone-accuracy metric with a validation study, plus the preference pairs it
labels. The metric is publishable on its own; the pairs are only useful once a base voice exists.

**Pass/fail.** Pass: the metric separates tone-minimal pairs that human Twi speakers also
distinguish, and disagrees with CER often enough to be adding information rather than restating
it. Fail: it tracks CER, in which case it is redundant and should be dropped rather than
defended.

---

## Order of work

Not the order they are numbered. The order their dependencies and costs imply:

1. **Method 3 first.** Hours, not days, and the project is blocked on it — `sikabook-models`
   trains on a corpus whose licence is in dispute.
2. **Method 4 next**, because it has to precede any collection and the ethics paperwork is
   running now anyway. Four to eight weeks of waiting is exactly the time to settle it.
3. **Method 1 during that wait**, on the owner's own machine rather than in a cloud session
   (§1.2). No clearance needed, and it is what makes the eventual stall visit short and
   well-aimed. Begin with step zero: do Ghanaian-language feeds exist in usable quantity?
4. **Method 2 next**, because it needs the voice, which needs Waxal, which needs nothing but a
   laptop week — but a laptop week spent after Method 3 has established what we are allowed to
   train on. Decide condition dropout (§5) *before* this run, not after.
5. **Method 5 last**, and only partly. The tone-accuracy metric (§5.1) can and should be built
   early, because it is the contribution and because Method 2 needs something to be judged by.
   The preference pairs themselves wait until a base voice exists.

## Where the value actually is

Worth being precise, because it is not where it looks.

**Scraping technique is not scarce.** It is a published recipe with an 86-citation paper
attached; anyone can follow it. Three things are scarce:

1. **Knowing which questions found data can and cannot answer.** The judgement that opens this
   document — that verbalisation needs a stall and vocabulary does not — is the expertise. It is
   also the thing that stops a year being spent on a corpus that answers nothing.
2. **Governance and licensing.** Over 80% of source content in widely-used datasets carries
   non-commercial restrictions, which is why this project cannot ship the best Twi voice it
   found. Whoever solves that for African speech data will not be short of work.
3. **Access.** Being the person who can get data nobody else can. This is structural rather
   than technical, and the owner already has it: he is at KNUST, which supplied the Akan half of
   `google/WaxalNLP`, and RAIL KNUST produced GESMA. Mistral built its Arabic model with
   Morocco's national lab. That partnership shape is available here and is not available to
   most people reading the same papers.

One honest note on the money question. Waxal was funded by Google and the Gates Foundation
explicitly to make African language data openly accessible — someone was paid to build it. The
funding for this work exists. What it is paid for is rarely the scraping.

## What has not been verified

Stated plainly so it is not mistaken for fact:

- Whether Ghanaian market video exists in usable quantity with audible transaction speech.
- Per-language hours for Waxal's `data/TTS/twi/` and `data/TTS/fat/` — not in the card.
- Whether Ghanaian radio or parliamentary audio is accessible in bulk, and under what terms.
- The KasaSpeech licence, now disputed between two sources.
- Every licence tag in the plan of record's matrix is as-tagged, not legally reviewed. None of
  this is legal advice and a licence decision for collected data should not rest on it alone.
- Both Chai posts are a practitioner's blog, not peer-reviewed, and she says herself of the
  second that she does not yet have a complete experimental record to share. Her numbers are
  taken as reported. The §5.1 tone argument does not depend on them — it follows from how CER,
  speaker similarity and proxy MOS are each defined.
- Whether UTMOS, or any proxy MOS predictor, transfers to Akan at all. It is trained
  predominantly on English and Japanese. Using it on Twi without checking would be the same
  class of error as reporting WER for a language whose orthography omits tone.

## Primary reading

In the order they should be read, not cited:

1. Lau et al., *Data Quality Issues in Multilingual Speech Datasets*, ACL 2025 — the checklist.
2. Rajab et al., *The Esethu Framework*, ACL 2025 — governance, and the map of the field.
3. Javed et al., *Towards Building ASR Systems for the Next Billion Users*, AAAI 2022 — the
   pipeline, written out.
4. Wang et al., *From Tens of Hours to Tens of Thousands*, EMNLP 2025 — synthetic scaling and
   the intelligibility threshold.
5. Longpre et al., *Bridging the Data Provenance Gap Across Text, Speech, and Video*, ICLR 2025
   — the audit that quantifies why any of this matters.
6. Bartelds et al., *Making More of Little Data*, ACL 2023, and Pine et al., ACL 2022 — the two
   that between them set the real data floor.
7. Ahia et al., *Voices Unheard*, EMNLP 2024, and Elmadany et al., *Voice of a Continent*,
   EMNLP 2025 — dialect fieldwork, and the benchmark to measure against.
8. Hussain et al., *Koel-TTS: Enhancing LLM based Speech Generation with Preference Alignment
   and Classifier Free Guidance*, EMNLP 2025, CC BY 4.0 — preference alignment and CFG for TTS,
   and the source of the condition-dropout decision in §5.

Practitioner write-ups, read for operational detail rather than cited as evidence: Bella Chai,
*"You Found a Million Audio Links. How Many Can You Actually Use?"* (20 Oct 2026) and *"Your TTS
Model Can Speak. Why Does It Still Sound Wrong?"* (18 Oct 2026).
