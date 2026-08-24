// Single source of truth for the Ghana Voice Ledger atlas.
// Build: node docs/atlas/build.mjs  → writes atlas.html + SYSTEM.md into this folder.

export const META = {
  title: 'Ghana Voice Ledger',
  artifactUrl: '',                 // fill after publishing; keep stable across rebuilds
  sourcePath: 'docs/atlas/data.mjs',
  buildCmd: 'node docs/atlas/build.mjs',
  outDir: '.',                     // write atlas.html + SYSTEM.md beside this file
  stats: [
    { k: 'App', v: 'ghana-voice-ledger · v0.1-core' },
    { k: 'Stack', v: 'Kotlin · Compose · Room' },
  ],
  intro: `_**This file is the living source of truth for the app's architecture.** The interactive atlas and \`SYSTEM.md\` are both built from it._`,
  onePara: `Ghana Voice Ledger is a small, offline Android app for market traders. A trader taps a mic and speaks a sale ("sold 3 tilapia for 20 cedis"); the device turns speech to text, a transparent parser turns text into a structured entry, a confirm sheet lets the trader fix any mishear, and it saves to a local Room database. A second tab is the credit ledger — who owes you, with partial payments. It builds, runs, and is unit-tested. Not built yet: multi-view UI, daily summaries, cloud sync, on-device ML.`,
  costModel: [],
  deepDive: '',
  platformGives: `Android gives us on-device speech-to-text (<code>RecognizerIntent</code>), Room/SQLite persistence, Jetpack Compose + Material 3 UI, and <code>ViewModel</code>/<code>StateFlow</code> lifecycle. No cloud, no API keys, no ML runtime.`,
  weOwn: `The parsers (number words incl. Twi/Ga/Ewe, cedis+pesewas, product/name extraction), the confirm-before-save flow, the ledger + credit data model with partial payments, the two-tab UI, and the warm theme.`,
  filesystem: `app/src/main/java/com/voiceledger/ghana/\n  MainActivity.kt              # Compose host + speech launcher + tab routing\n  VoiceLedgerApplication.kt\n  voice/  AmountParser · TransactionParser · DebtParser\n  data/   Transaction · Customer · Debt · *Dao · LedgerDatabase\n  ui/     AppRoot · LedgerScreen · CreditScreen · *ViewModel · theme/`,
};

export const DECISIONS = [
  { axis: 'Runtime', decision: 'Native Android (Kotlin + Jetpack Compose), single module. Chosen after the prior 134-file app never compiled — a small coherent core over a broken whole.', adr: '—' },
  { axis: 'Persistence', decision: 'Room/SQLite, fully **local / offline-first**. No cloud sync yet.', adr: '—' },
  { axis: 'Dependency injection', decision: '**None** — manual wiring, a DB singleton held by the app. Dropped Hilt to remove the kapt failure that broke the old build.', adr: '—' },
  { axis: 'Voice', decision: 'Device **speech-to-text** + a **transparent heuristic parser** (no ML, no cloud keys). Predictable and offline.', adr: '—' },
  { axis: 'Trust', decision: '**Confirm before save** — nothing persists until the trader okays the parsed entry (voice mishears are common).', adr: '—' },
  { axis: 'Credit model', decision: 'A saved **Customer** table + **Debt** rows with `amountPaid` for **partial payments**; balances aggregate per customer.', adr: '—' },
];

export const GROUPS = [
  { id: 'loop', title: 'Record a sale' },
  { id: 'credit', title: 'The credit ledger' },
  { id: 'data', title: 'Where it lives' },
  { id: 'planned', title: 'Planned / deferred' },
];

export const NODES = [
  { id: 'TODAY', code: 'T', name: 'Today screen', short: 'TODAY', group: 'loop', gx: 2, gy: 10, w: 2, d: 2, h: 40, kind: 'screen',
    one: `The sales tab: today's total and the list of sales.`,
    what: `The first thing a trader sees — a big "Today's total" card, the day's sales, and a mic button to record a new one.`,
    how: `<code>LedgerScreen.kt</code> (Compose Material 3). Collects <code>transactions</code> and <code>todayTotal</code> from the view model as <code>StateFlow</code>; shows a Ghanaian greeting (Maakye/Maaha/Maadwo).`,
    steps: [['Show total', 'Big hero card, GHS today.'], ['List sales', 'Tap a row to edit.'], ['Record', 'Mic FAB → speech.']],
    cond: [] },

  { id: 'SHELL', code: 'A', name: 'App shell', short: 'APP SHELL', group: 'loop', gx: 6, gy: 3, w: 3, d: 2, h: 40, kind: 'box',
    one: `Hosts the two tabs and launches the microphone.`,
    what: `The frame around everything: it holds the Today and Credit tabs and, when you tap a mic, opens speech recognition and hands the words to whichever tab you're on.`,
    how: `<code>MainActivity.kt</code> + <code>AppRoot.kt</code>. A bottom <code>NavigationBar</code> switches tabs; <code>registerForActivityResult</code> launches <code>RecognizerIntent</code> and routes the result to <code>ledgerViewModel</code> or <code>creditViewModel</code>.`,
    steps: [['Route tab', 'Today ↔ Credit.'], ['Launch mic', 'RecognizerIntent.'], ['Deliver text', 'To the active view model.']],
    cond: [{ q: 'Should the mic also auto-detect a credit phrase from the Today tab?', to: 'Roadmap' }] },

  { id: 'MIC', code: 'M', name: 'Speech-to-text', short: 'SPEECH', group: 'loop', gx: 10, gy: 1, w: 2, d: 2, h: 26, kind: 'slab',
    one: `The device turns spoken words into text (push-to-talk today).`,
    what: `Today: Android's own recogniser, push-to-talk — tap, speak, it returns a best-guess sentence. North star: the "listening stall" — the phone in her pouch while she serves, capturing the sale hands-free.`,
    how: `Now: <code>RecognizerIntent.ACTION_RECOGNIZE_SPEECH</code> (free, no key, device-provided). Direction: split the two axes — <mark>language</mark> (Twi/Ga/Ewe/Pidgin) via <code>Meta MMS</code> / <code>GhanaNLP Khaya</code>, <mark>noise</mark> via a small speech-enhancement front-end. Bridge to hands-free = a tiny <mark>wake-word</mark> + short capture + end-of-day review. Vosk has no Ghanaian model; text corpora build the LM, not the acoustic model.`,
    steps: [['Listen', 'Push-to-talk prompt + record.'], ['Transcribe', 'On device / recogniser.'], ['Return', 'Best hypothesis string.'], ['(Next) Wake-word', 'Tiny always-on trigger → capture.']],
    cond: [
      { q: 'Understand Twi/Ga/Ewe + Pidgin?', to: 'Language axis — Meta MMS / GhanaNLP Khaya (not Vosk)' },
      { q: 'Hands-free "listening stall" — phone in pouch while serving?', to: 'Wake-word + short capture + end-of-day review' },
      { q: 'Hear her over market noise?', to: 'Small on-device speech-enhancement front-end (SAM-Audio is heavy / server-side)' },
      { q: 'Where does Ghanaian training data come from?', r: 'The confirm sheet: every correction is an (audio, text) pair; sikabook-models is the seed corpus (2026-08-24).' },
    ] },

  { id: 'PARSER', code: 'P', name: 'Parsers', short: 'PARSER', group: 'loop', gx: 12, gy: 5, w: 3, d: 3, h: 66, kind: 'tall',
    one: `Turns a spoken sentence into a structured sale or credit.`,
    what: `The interpreter. It reads "sold 3 tilapia for 20 cedis" and pulls out the amount (20), quantity (3), and product (Tilapia) — fixing common speech mishears and understanding local number words.`,
    how: `<code>AmountParser</code> (number words incl. Twi/Ga/Ewe + compounds; cedis+pesewas), reused by <code>TransactionParser</code> (product/quantity, canonicalisation) and <code>DebtParser</code> (customer name + note). Pure Kotlin, 21 unit tests. No ML.`,
    steps: [['Numbers → digits', '"twenty five" → 25; "baako" → 1.'], ['Amount', 'cedis + optional pesewas.'], ['Sale: qty + product', 'canonicalise mishears.'], ['Credit: name + note', '"Ama … for fish".']],
    cond: [{ q: 'How rich should the product/number vocabulary get?', r: 'Start small and transparent; expand from real usage (2026-08-24).' }] },

  { id: 'CONFIRM', code: 'C', name: 'Confirm sheet', short: 'CONFIRM', group: 'loop', gx: 8, gy: 7, w: 2, d: 2, h: 38, kind: 'gate',
    one: `Nothing saves until the trader okays it.`,
    what: `A small editable card that appears after parsing — item, quantity, amount (or customer, amount, note) — so a mishear can be fixed before it's recorded. Also opens when you tap an entry to edit it.`,
    how: `A Compose dialog bound to an editable draft (<code>TransactionDraft</code> / <code>DebtDraft</code>) in the view model; validates the amount on <mark>Save</mark>, then inserts or updates.`,
    steps: [['Prefill', 'From the parse.'], ['Fix', 'Edit any field.'], ['Save', 'Validate → persist.']],
    cond: [] },

  { id: 'TVM', code: 'L', name: 'Today view model', short: 'TODAY VM', group: 'loop', gx: 4, gy: 6, w: 2, d: 2, h: 34, kind: 'box',
    one: `Holds the sales state and saves sales.`,
    what: `Keeps the day's list and running total live on screen, and turns a confirmed draft into a saved sale (or an edit).`,
    how: `<code>LedgerViewModel</code> (AndroidViewModel). Exposes <code>transactions</code> + <code>todayTotal</code> as <code>StateFlow</code> from the DAO; <code>saveDraft()</code> inserts/updates via <code>TransactionDao</code>.`,
    steps: [['Observe', 'DAO Flows → StateFlow.'], ['Begin', 'Parse → draft.'], ['Save', 'Insert or update.']],
    cond: [] },

  { id: 'CVM', code: 'R', name: 'Credit view model', short: 'CREDIT VM', group: 'credit', gx: 12, gy: 9, w: 2, d: 2, h: 34, kind: 'box',
    one: `Holds who-owes-what and records payments.`,
    what: `Drives the credit tab: the list of debtors with balances, one customer's credits, recording a new credit, and taking a (partial or full) payment.`,
    how: `<code>CreditViewModel</code>. <code>observeDebtors()</code> gives per-customer balances; <code>saveDraft()</code> finds-or-creates the customer then inserts a <code>Debt</code>; <code>confirmPayment()</code> raises <code>amountPaid</code>.`,
    steps: [['Debtors', 'Balances via SQL.'], ['Open customer', 'Their credits.'], ['Payment', 'amountPaid += , settle.']],
    cond: [{ q: 'Add customer autocomplete (pick a saved name as you type)?', to: 'Roadmap' }] },

  { id: 'CREDIT', code: 'K', name: 'Credit screen', short: 'CREDIT', group: 'credit', gx: 15, gy: 12, w: 2, d: 2, h: 40, kind: 'screen',
    one: `The credit tab: who owes you, and each customer's balance.`,
    what: `An "Owed to you" total, a list of people who owe with their balance, and a per-customer detail with each credit and a "Received" button for payments.`,
    how: `<code>CreditScreen.kt</code>. List ↔ detail via a selected-customer state + <code>BackHandler</code>; partial payments show as "GHS 10.00 of GHS 15.00".`,
    steps: [['Total owed', 'Across everyone.'], ['Debtor list', 'Sorted by balance.'], ['Detail', 'Credits + Received.']],
    cond: [] },

  { id: 'DB', code: 'D', name: 'Room database', short: 'LEDGER DB', group: 'data', gx: 7, gy: 12, w: 3, d: 3, h: 26, kind: 'store',
    one: `The local SQLite store — sales, customers, debts.`,
    what: `Everything lives on the phone. Three tables: sales (transactions), customers, and debts (with how much has been paid). No server; works fully offline.`,
    how: `<code>LedgerDatabase</code> (Room v2). Tables <code>transactions</code>, <code>customers</code>, <code>debts</code> (FK → customer, cascade); DAOs expose <code>Flow</code> queries incl. the per-customer balance aggregate.`,
    steps: [['Write', 'insert / update / delete.'], ['Observe', 'Flow queries.'], ['Aggregate', 'SUM(amount − amountPaid).']],
    cond: [{ q: 'Real migrations vs destructive reset?', r: 'Destructive fallback while pre-release; write migrations before real users (2026-08-24).' }] },

  // --- planned / deferred (ghosts) ---
  { id: 'SUM', code: 'S', name: 'Daily summary + share', short: 'SUMMARY', group: 'planned', ghost: true, gx: 3, gy: 13, w: 2, d: 2, h: 34, kind: 'box',
    one: `Later: a "good day" recap you can send to WhatsApp.`,
    what: `An end-of-day summary (total, best product, number of sales) you can share — the stickiness-and-growth feature. Next on the roadmap.`,
    how: `Planned: derive from the same DAOs; a share intent to WhatsApp. Not built.`,
    steps: [['Summarise', 'Totals by product/time.'], ['Share', 'System share sheet.']],
    cond: ['Next to build.'] },

  { id: 'META', code: 'V', name: 'Metaphor views', short: 'MONEY-PILE', group: 'planned', ghost: true, gx: 1, gy: 6, w: 2, d: 2, h: 30, kind: 'screen',
    one: `Later: culturally-congruent ways to see the day.`,
    what: `From the UI vision: optional views like "Money Pile" (money stacking up) or "Progress Journey" (walking toward a daily goal), instead of only a list.`,
    how: `Designed in <code>CULTURALLY_CONGRUENT_UI_GUIDE.md</code>; not implemented.`,
    steps: [['Pick view', 'List / pile / journey.'], ['Animate', 'Coins, path, milestones.']],
    cond: ['The most distinctive unbuilt idea.'] },

  { id: 'CLOUD', code: 'X', name: 'Cloud + ML (deferred)', short: 'CLOUD/ML', group: 'planned', ghost: true, gx: 16, gy: 6, w: 2, d: 2, h: 30, kind: 'store',
    one: `Deferred: backup/sync, speaker ID, encryption.`,
    what: `The heavy pieces from the original vision — cloud backup/sync, on-device speaker identification (recognise returning customers), and database encryption. Deliberately deferred until validated with real traders.`,
    how: `Removed with the old non-compiling source; to be reconsidered later.`,
    steps: [['Sync', 'Backup / restore.'], ['Speaker ID', 'On-device model.'], ['Encrypt', 'SQLCipher + biometric.']],
    cond: ['Only if real usage demands it.'] },
];

export const FLOWS = [
  { id: 'sale', name: 'Record a sale', hops: [
    ['TODAY', 'SHELL', 'tap record', {}, 'yx'],
    ['SHELL', 'MIC', 'listen', {}, 'xy'],
    ['MIC', 'SHELL', 'heard', { text: 'sold 3 tilapia for 20 cedis' }, 'xy'],
    ['SHELL', 'PARSER', 'parse', { text: 'sold 3 tilapia for 20 cedis' }, 'yx'],
    ['PARSER', 'CONFIRM', 'draft', { item: 'Tilapia', qty: 3, amount: 20.0 }, 'xy'],
    ['CONFIRM', 'TVM', 'save', { item: 'Tilapia', amount: 20.0 }, 'yx'],
    ['TVM', 'DB', 'insert', { table: 'transactions' }, 'xy'],
    ['DB', 'TVM', 'flow update', { todayTotal: 20.0 }, 'yx'],
    ['TVM', 'TODAY', 'state', { list: '+1 sale' }, 'yx'],
  ] },
  { id: 'credit', name: 'Record a credit + payment', hops: [
    ['CREDIT', 'SHELL', 'record credit', {}, 'yx'],
    ['SHELL', 'MIC', 'listen', {}, 'xy'],
    ['MIC', 'SHELL', 'heard', { text: 'Ama owes 20 cedis for fish' }, 'xy'],
    ['SHELL', 'PARSER', 'parse', { text: 'Ama owes 20 cedis for fish' }, 'yx'],
    ['PARSER', 'CONFIRM', 'draft', { customer: 'Ama', amount: 20.0, note: 'Fish' }, 'xy'],
    ['CONFIRM', 'CVM', 'save', { customer: 'Ama', amount: 20.0 }, 'yx'],
    ['CVM', 'DB', 'find-or-create + insert', { tables: 'customers, debts' }, 'xy'],
    ['DB', 'CVM', 'balances', { 'Ama owes': 20.0 }, 'yx'],
    ['CVM', 'CREDIT', 'debtors', { row: 'Ama · GHS 20' }, 'yx'],
    ['CREDIT', 'CVM', 'Received 10', { pay: 10.0 }, 'xy'],
    ['CVM', 'DB', 'update amountPaid', { debt: 'Ama/Fish', amountPaid: 10.0 }, 'xy'],
    ['DB', 'CVM', 'balance', { 'Ama owes': 10.0 }, 'yx'],
  ] },
];

export const CH = [
  { id: 'you', title: 'You and the ledger', reveal: ['TODAY', 'SHELL'],
    lede: `Strip everything away and this is the app: a trader, a screen, and a mic button.`,
    story: `<p>The <mark>Today screen</mark> shows the day's total and sales; the <mark>App shell</mark> hosts it and owns the microphone. Everything else exists to fill this screen with an accurate number.</p>`,
    flow: [['TODAY', 'SHELL', 'tap record', {}], ['SHELL', 'TODAY', 'open sheet', {}]] },

  { id: 'hear', title: 'Hearing the sale', reveal: ['MIC'],
    lede: `Tap the mic and speak — the phone writes down what it heard.`,
    story: `<p>The <mark>device's own speech-to-text</mark> does the listening. No cloud, no keys — whatever the phone supports. It hands back a plain sentence like "sold 3 tilapia for 20 cedis".</p>`,
    flow: [['TODAY', 'SHELL', 'tap record', {}], ['SHELL', 'MIC', 'listen', {}], ['MIC', 'SHELL', 'heard', { text: 'sold 3 tilapia for 20 cedis' }]] },

  { id: 'understand', title: 'Understanding it', reveal: ['PARSER'],
    lede: `A plain sentence becomes an amount, a quantity, and a product.`,
    story: `<p>The <mark>parser</mark> is the only clever part, and it's deliberately transparent — regexes and word lists, no ML. It knows cedis and pesewas, English and Twi/Ga/Ewe numbers, and fixes common mishears ("talapia" → Tilapia).</p>`,
    flow: [['SHELL', 'PARSER', 'parse', { text: 'sold 3 tilapia for 20 cedis' }], ['PARSER', 'SHELL', 'structured', { item: 'Tilapia', qty: 3, amount: 20.0 }]] },

  { id: 'confirm', title: 'Confirm before saving', reveal: ['CONFIRM'],
    lede: `Voice mishears — so nothing is saved until you okay it.`,
    story: `<p>The <mark>confirm sheet</mark> shows the parsed entry with editable fields. Fix the amount, then Save. The same sheet opens when you tap an existing entry to edit it — the app never silently records a wrong number.</p>`,
    flow: [['PARSER', 'CONFIRM', 'draft', { item: 'Tilapia', qty: 3, amount: 20.0 }], ['CONFIRM', 'SHELL', 'confirmed', {}]] },

  { id: 'save', title: 'Saving to the ledger', reveal: ['TVM', 'DB'],
    lede: `A confirmed sale lands in the local database and the screen updates itself.`,
    story: `<p>The <mark>Today view model</mark> writes through a DAO into the <mark>Room database</mark> on the phone, and because the screen observes the database as a stream, the total and list update on their own. Fully offline.</p>`,
    flow: [['CONFIRM', 'TVM', 'save', { amount: 20.0 }], ['TVM', 'DB', 'insert', { table: 'transactions' }], ['DB', 'TVM', 'flow update', { todayTotal: 20.0 }], ['TVM', 'TODAY', 'state', { list: '+1 sale' }]] },

  { id: 'creditch', title: 'The credit ledger', reveal: ['CVM', 'CREDIT'],
    lede: `The second tab: who owes you, and paying it down.`,
    story: `<p>Speaking "Ama owes 20 cedis for fish" runs the same mic → parser → confirm path into the <mark>credit view model</mark>. Customers are saved and reused, balances add up per person, and <mark>partial payments</mark> ("Received 10") settle a debt over time.</p>`,
    flow: [['CONFIRM', 'CVM', 'save', { customer: 'Ama', amount: 20.0 }], ['CVM', 'DB', 'customer + debt', {}], ['DB', 'CVM', 'balances', { 'Ama owes': 20.0 }], ['CVM', 'CREDIT', 'debtors', { row: 'Ama · GHS 20' }], ['CREDIT', 'CVM', 'Received 10', { pay: 10.0 }], ['CVM', 'DB', 'amountPaid', { amountPaid: 10.0 }]] },

  { id: 'planned', title: 'Planned & deferred', reveal: ['SUM', 'META', 'CLOUD'],
    lede: `Designed for, not switched on.`,
    story: `<p>Dashed boxes are the roadmap: a <mark>daily summary you can share to WhatsApp</mark> (next), the <mark>metaphor views</mark> from the UI vision, and the deferred heavy lifting — cloud sync, speaker ID, encryption — parked until real traders ask for it.</p>`,
    flow: [['TVM', 'SUM', 'daily recap', { total: 'GHS 240' }], ['SUM', 'CREDIT', 'share', { to: 'WhatsApp' }]] },

  { id: 'all', title: 'The whole system', reveal: [],
    lede: `Everything at once, for free exploration.`,
    story: `<p>Choose which flow runs (bottom left): a sale, or a credit + payment. Hover anything to read it; click to pin; <mark>→ goes inside</mark> a structure to see its steps. The <mark>Open questions</mark> tab lists every open decision by ID.</p>`,
    flow: null },
];

export const HOW_HTML = `<div class="eyebrow">Ghana Voice Ledger · v0.1-core</div><h1 class="t">How it's built</h1><div class="sub">a small offline Android app: speak → parse → confirm → local ledger</div>
<h3 class="sec">The shape</h3><p>One Android module. The device does speech-to-text; a transparent Kotlin parser structures it; a confirm sheet guards every write; Room holds sales, customers and debts on the phone. Two tabs — Today (sales) and Credit (who owes you) — share the same mic → parse → confirm path.</p>
<h3 class="sec">Filesystem</h3><pre>app/src/main/java/com/voiceledger/ghana/
  MainActivity.kt          host + mic + tab routing
  voice/   AmountParser · TransactionParser · DebtParser
  data/    Transaction · Customer · Debt · *Dao · LedgerDatabase
  ui/      AppRoot · LedgerScreen · CreditScreen · *ViewModel · theme/</pre>
<h3 class="sec">Not built yet</h3><p>Daily summary + share, multi-view UI, Twi/Ga/Ewe labels, cloud sync, speaker ID, encryption.</p>`;
