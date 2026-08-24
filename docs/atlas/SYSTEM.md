# Ghana Voice Ledger — System Definition

_**This file is the living source of truth for the app's architecture.** The interactive atlas and `SYSTEM.md` are both built from it._

_Question status: **3 open · 8 resolved**._

## One paragraph

Ghana Voice Ledger is a small, offline Android app for market traders. A trader taps a mic and speaks a sale ("sold 3 tilapia for 20 cedis"); the device turns speech to text, a transparent parser turns text into a structured entry, a confirm sheet lets the trader fix any mishear, and it saves to a local Room database. A second tab is the credit ledger — who owes you, with partial payments. It builds, runs, and is unit-tested. Not built yet: multi-view UI, daily summaries, cloud sync, on-device ML.

## Decisions locked

| Axis | Decision | ADR |
|---|---|---|
| Runtime | Native Android (Kotlin + Jetpack Compose), single module. Chosen after the prior 134-file app never compiled — a small coherent core over a broken whole. | — |
| Persistence | Room/SQLite, fully **local / offline-first**. No cloud sync yet. | — |
| Dependency injection | **None** — manual wiring, a DB singleton held by the app. Dropped Hilt to remove the kapt failure that broke the old build. | — |
| Voice | Device **speech-to-text** + a **transparent heuristic parser** (no ML, no cloud keys). Predictable and offline. | — |
| Trust | **Confirm before save** — nothing persists until the trader okays the parsed entry (voice mishears are common). | — |
| Credit model | A saved **Customer** table + **Debt** rows with `amountPaid` for **partial payments**; balances aggregate per customer. | — |

## Cost model

## Reading order (the atlas chapters)

1. **You and the ledger** — Strip everything away and this is the app: a trader, a screen, and a mic button. _(adds TODAY, SHELL)_
2. **Hearing the sale** — Tap the mic and speak — the phone writes down what it heard. _(adds MIC)_
3. **Understanding it** — A plain sentence becomes an amount, a quantity, and a product. _(adds PARSER)_
4. **Confirm before saving** — Voice mishears — so nothing is saved until you okay it. _(adds CONFIRM)_
5. **Saving to the ledger** — A confirmed sale lands in the local database and the screen updates itself. _(adds TVM, DB)_
6. **The credit ledger** — The second tab: who owes you, and paying it down. _(adds CVM, CREDIT)_
7. **Planned & deferred** — Designed for, not switched on. _(adds SUM, META, CLOUD)_
8. **The whole system** — Everything at once, for free exploration.

## Structures

### Record a sale

#### T · Today screen

**In one line.** The sales tab: today's total and the list of sales.

**What it does.** The first thing a trader sees — a big "Today's total" card, the day's sales, and a mic button to record a new one.

**How it's built.** `LedgerScreen.kt` (Compose Material 3). Collects `transactions` and `todayTotal` from the view model as `StateFlow`; shows a Ghanaian greeting (Maakye/Maaha/Maadwo).

**Steps in execution.**

1. **Show total** — Big hero card, GHS today.
2. **List sales** — Tap a row to edit.
3. **Record** — Mic FAB → speech.

#### A · App shell

**In one line.** Hosts the two tabs and launches the microphone.

**What it does.** The frame around everything: it holds the Today and Credit tabs and, when you tap a mic, opens speech recognition and hands the words to whichever tab you're on.

**How it's built.** `MainActivity.kt` + `AppRoot.kt`. A bottom `NavigationBar` switches tabs; `registerForActivityResult` launches `RecognizerIntent` and routes the result to `ledgerViewModel` or `creditViewModel`.

**Steps in execution.**

1. **Route tab** — Today ↔ Credit.
2. **Launch mic** — RecognizerIntent.
3. **Deliver text** — To the active view model.

**Questions.**

- **Q-A1** Should the mic also auto-detect a credit phrase from the Today tab? → _Roadmap_

#### M · Speech-to-text

**In one line.** The device turns spoken words into text (push-to-talk today).

**What it does.** Today: Android's own recogniser, push-to-talk — tap, speak, it returns a best-guess sentence. North star: the "listening stall" — the phone in her pouch while she serves, capturing the sale hands-free.

**How it's built.** Now: `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` (free, no key, device-provided). Direction: split the two axes — **language** (Twi/Ga/Ewe/Pidgin) via `Meta MMS` / `GhanaNLP Khaya`, **noise** via a small speech-enhancement front-end. Bridge to hands-free = a tiny **wake-word** + short capture + end-of-day review. Vosk has no Ghanaian model; text corpora build the LM, not the acoustic model.

**Steps in execution.**

1. **Listen** — Push-to-talk prompt + record.
2. **Transcribe** — On device / recogniser.
3. **Return** — Best hypothesis string.
4. **(Next) Wake-word** — Tiny always-on trigger → capture.

**Questions.**

- **Q-M1** Understand Twi/Ga/Ewe + Pidgin? → _Language axis — Meta MMS / GhanaNLP Khaya (not Vosk)_
- **Q-M2** Hands-free "listening stall" — phone in pouch while serving? → _Wake-word + short capture + end-of-day review_
- **Q-M3** Hear her over market noise? → _Small on-device speech-enhancement front-end (SAM-Audio is heavy / server-side)_
- ~~**Q-M4** Where does Ghanaian training data come from?~~ ✓ The confirm sheet: every correction is an (audio, text) pair; sikabook-models is the seed corpus (2026-08-24).

#### P · Parsers

**In one line.** Turns a spoken sentence into a structured sale or credit.

**What it does.** The interpreter. It reads "sold 3 tilapia for 20 cedis" and pulls out the amount (20), quantity (3), and product (Tilapia) — fixing common speech mishears and understanding local number words.

**How it's built.** `AmountParser` (number words incl. Twi/Ga/Ewe + compounds; cedis+pesewas), reused by `TransactionParser` (product/quantity, canonicalisation) and `DebtParser` (customer name + note). Pure Kotlin, 21 unit tests. No ML.

**Steps in execution.**

1. **Numbers → digits** — "twenty five" → 25; "baako" → 1.
2. **Amount** — cedis + optional pesewas.
3. **Sale: qty + product** — canonicalise mishears.
4. **Credit: name + note** — "Ama … for fish".

**Questions.**

- ~~**Q-P1** How rich should the product/number vocabulary get?~~ ✓ Start small and transparent; expand from real usage (2026-08-24).

#### C · Confirm sheet

**In one line.** Nothing saves until the trader okays it.

**What it does.** A small editable card that appears after parsing — item, quantity, amount (or customer, amount, note) — so a mishear can be fixed before it's recorded. Also opens when you tap an entry to edit it.

**How it's built.** A Compose dialog bound to an editable draft (`TransactionDraft` / `DebtDraft`) in the view model; validates the amount on **Save**, then inserts or updates.

**Steps in execution.**

1. **Prefill** — From the parse.
2. **Fix** — Edit any field.
3. **Save** — Validate → persist.

#### L · Today view model

**In one line.** Holds the sales state and saves sales.

**What it does.** Keeps the day's list and running total live on screen, and turns a confirmed draft into a saved sale (or an edit).

**How it's built.** `LedgerViewModel` (AndroidViewModel). Exposes `transactions` + `todayTotal` as `StateFlow` from the DAO; `saveDraft()` inserts/updates via `TransactionDao`.

**Steps in execution.**

1. **Observe** — DAO Flows → StateFlow.
2. **Begin** — Parse → draft.
3. **Save** — Insert or update.

### The credit ledger

#### R · Credit view model

**In one line.** Holds who-owes-what and records payments.

**What it does.** Drives the credit tab: the list of debtors with balances, one customer's credits, recording a new credit, and taking a (partial or full) payment.

**How it's built.** `CreditViewModel`. `observeDebtors()` gives per-customer balances; `saveDraft()` finds-or-creates the customer then inserts a `Debt`; `confirmPayment()` raises `amountPaid`.

**Steps in execution.**

1. **Debtors** — Balances via SQL.
2. **Open customer** — Their credits.
3. **Payment** — amountPaid += , settle.

**Questions.**

- **Q-R1** Add customer autocomplete (pick a saved name as you type)? → _Roadmap_

#### K · Credit screen

**In one line.** The credit tab: who owes you, and each customer's balance.

**What it does.** An "Owed to you" total, a list of people who owe with their balance, and a per-customer detail with each credit and a "Received" button for payments.

**How it's built.** `CreditScreen.kt`. List ↔ detail via a selected-customer state + `BackHandler`; partial payments show as "GHS 10.00 of GHS 15.00".

**Steps in execution.**

1. **Total owed** — Across everyone.
2. **Debtor list** — Sorted by balance.
3. **Detail** — Credits + Received.

### Where it lives

#### D · Room database

**In one line.** The local SQLite store — sales, customers, debts.

**What it does.** Everything lives on the phone. Three tables: sales (transactions), customers, and debts (with how much has been paid). No server; works fully offline.

**How it's built.** `LedgerDatabase` (Room v2). Tables `transactions`, `customers`, `debts` (FK → customer, cascade); DAOs expose `Flow` queries incl. the per-customer balance aggregate.

**Steps in execution.**

1. **Write** — insert / update / delete.
2. **Observe** — Flow queries.
3. **Aggregate** — SUM(amount − amountPaid).

**Questions.**

- ~~**Q-D1** Real migrations vs destructive reset?~~ ✓ Destructive fallback while pre-release; write migrations before real users (2026-08-24).

### Planned / deferred

#### S · Daily summary + share _(not switched on)_

**In one line.** Later: a "good day" recap you can send to WhatsApp.

**What it does.** An end-of-day summary (total, best product, number of sales) you can share — the stickiness-and-growth feature. Next on the roadmap.

**How it's built.** Planned: derive from the same DAOs; a share intent to WhatsApp. Not built.

**Steps in execution.**

1. **Summarise** — Totals by product/time.
2. **Share** — System share sheet.

**Questions.**

- **Q-S1** Next to build.

#### V · Metaphor views _(not switched on)_

**In one line.** Later: culturally-congruent ways to see the day.

**What it does.** From the UI vision: optional views like "Money Pile" (money stacking up) or "Progress Journey" (walking toward a daily goal), instead of only a list.

**How it's built.** Designed in `CULTURALLY_CONGRUENT_UI_GUIDE.md`; not implemented.

**Steps in execution.**

1. **Pick view** — List / pile / journey.
2. **Animate** — Coins, path, milestones.

**Questions.**

- **Q-V1** The most distinctive unbuilt idea.

#### X · Cloud + ML (deferred) _(not switched on)_

**In one line.** Deferred: backup/sync, speaker ID, encryption.

**What it does.** The heavy pieces from the original vision — cloud backup/sync, on-device speaker identification (recognise returning customers), and database encryption. Deliberately deferred until validated with real traders.

**How it's built.** Removed with the old non-compiling source; to be reconsidered later.

**Steps in execution.**

1. **Sync** — Backup / restore.
2. **Speaker ID** — On-device model.
3. **Encrypt** — SQLCipher + biometric.

**Questions.**

- **Q-X1** Only if real usage demands it.

## Flows (representative packets)

Payload shapes are what the design implies, not measured traffic.

### Record a sale

| # | From → To | Packet | Representative payload |
|---|---|---|---|
| 1 | TODAY → SHELL | tap record | `{}` |
| 2 | SHELL → MIC | listen | `{}` |
| 3 | MIC → SHELL | heard | `{"text":"sold 3 tilapia for 20 cedis"}` |
| 4 | SHELL → PARSER | parse | `{"text":"sold 3 tilapia for 20 cedis"}` |
| 5 | PARSER → CONFIRM | draft | `{"item":"Tilapia","qty":3,"amount":20}` |
| 6 | CONFIRM → TVM | save | `{"item":"Tilapia","amount":20}` |
| 7 | TVM → DB | insert | `{"table":"transactions"}` |
| 8 | DB → TVM | flow update | `{"todayTotal":20}` |
| 9 | TVM → TODAY | state | `{"list":"+1 sale"}` |

### Record a credit + payment

| # | From → To | Packet | Representative payload |
|---|---|---|---|
| 1 | CREDIT → SHELL | record credit | `{}` |
| 2 | SHELL → MIC | listen | `{}` |
| 3 | MIC → SHELL | heard | `{"text":"Ama owes 20 cedis for fish"}` |
| 4 | SHELL → PARSER | parse | `{"text":"Ama owes 20 cedis for fish"}` |
| 5 | PARSER → CONFIRM | draft | `{"customer":"Ama","amount":20,"note":"Fish"}` |
| 6 | CONFIRM → CVM | save | `{"customer":"Ama","amount":20}` |
| 7 | CVM → DB | find-or-create + insert | `{"tables":"customers, debts"}` |
| 8 | DB → CVM | balances | `{"Ama owes":20}` |
| 9 | CVM → CREDIT | debtors | `{"row":"Ama · GHS 20"}` |
| 10 | CREDIT → CVM | Received 10 | `{"pay":10}` |
| 11 | CVM → DB | update amountPaid | `{"debt":"Ama/Fish","amountPaid":10}` |
| 12 | DB → CVM | balance | `{"Ama owes":10}` |

## Questions — index

Reference by ID. ✓ resolved (with date) · otherwise open.

- **Q-A1** (A) Should the mic also auto-detect a credit phrase from the Today tab?
- **Q-M1** (M) Understand Twi/Ga/Ewe + Pidgin?
- **Q-M2** (M) Hands-free "listening stall" — phone in pouch while serving?
- **Q-M3** (M) Hear her over market noise?
- ~~**Q-M4**~~ (M) ✓ The confirm sheet: every correction is an (audio, text) pair; sikabook-models is the seed corpus (2026-08-24).
- ~~**Q-P1**~~ (P) ✓ Start small and transparent; expand from real usage (2026-08-24).
- **Q-R1** (R) Add customer autocomplete (pick a saved name as you type)?
- ~~**Q-D1**~~ (D) ✓ Destructive fallback while pre-release; write migrations before real users (2026-08-24).
- **Q-S1** (S) Next to build.
- **Q-V1** (V) The most distinctive unbuilt idea.
- **Q-X1** (X) Only if real usage demands it.

## What the platform gives vs what we own

**Platform gives:** Android gives us on-device speech-to-text (<code>RecognizerIntent</code>), Room/SQLite persistence, Jetpack Compose + Material 3 UI, and <code>ViewModel</code>/<code>StateFlow</code> lifecycle. No cloud, no API keys, no ML runtime.

**We own:** The parsers (number words incl. Twi/Ga/Ewe, cedis+pesewas, product/name extraction), the confirm-before-save flow, the ledger + credit data model with partial payments, the two-tab UI, and the warm theme.

## Planned filesystem

```
app/src/main/java/com/voiceledger/ghana/
  MainActivity.kt              # Compose host + speech launcher + tab routing
  VoiceLedgerApplication.kt
  voice/  AmountParser · TransactionParser · DebtParser
  data/   Transaction · Customer · Debt · *Dao · LedgerDatabase
  ui/     AppRoot · LedgerScreen · CreditScreen · *ViewModel · theme/
```

## How this file is maintained

Generated from `docs/atlas/data.mjs` by `node docs/atlas/build.mjs`, which also builds the interactive atlas (`atlas.html`). Edit the data file, rebuild, republish — never edit this file by hand.
