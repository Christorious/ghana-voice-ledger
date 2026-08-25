// Single source of truth for the Ghana Voice Ledger atlas.
// Build: node docs/atlas/build.mjs  → writes atlas.html + SYSTEM.md into this folder.

export const META = {
  title: 'Ghana Voice Ledger',
  artifactUrl: '',                 // fill after publishing; keep stable across rebuilds
  sourcePath: 'docs/atlas/data.mjs',
  buildCmd: 'node docs/atlas/build.mjs',
  outDir: '.',                     // write atlas.html + SYSTEM.md beside this file
  stats: [
    { k: 'App', v: 'ghana-voice-ledger · v0.2-core' },
    { k: 'Stack', v: 'Kotlin · Compose · Room' },
  ],
  intro: `_**This file is the living source of truth for the app's architecture.** The interactive atlas and \`SYSTEM.md\` are both built from it._`,
  onePara: `Ghana Voice Ledger is a small, offline Android app for market traders. A trader taps a mic and speaks a sale ("sold 3 tilapia for 20 cedis"); the device turns speech to text, a transparent parser turns text into a structured entry, a confirm sheet lets the trader fix any mishear, and it saves to a local Room database. A second tab, Insights, is her little accountant — Day/Week/Month profit (sales − expenses), a trend, best sellers and a share-ready recap; expenses are spoken just like sales. A third tab is the credit ledger — who owes you, with partial payments. It builds, runs, and is unit-tested (31 parser tests). Not built yet: demand forecast, metaphor views, cloud sync, on-device ML.`,
  costModel: [],
  deepDive: '',
  platformGives: `Android gives us on-device speech-to-text (<code>RecognizerIntent</code>), Room/SQLite persistence, Jetpack Compose + Material 3 UI, and <code>ViewModel</code>/<code>StateFlow</code> lifecycle. No cloud, no API keys, no ML runtime.`,
  weOwn: `The parsers (number words incl. Twi/Ga/Ewe, cedis+pesewas, product/name/expense extraction), the confirm-before-save flow, the ledger + expenses + credit data model with partial payments, the profit/insights aggregation, the three-tab UI, and the warm theme.`,
  filesystem: `app/src/main/java/com/voiceledger/ghana/\n  MainActivity.kt              # Compose host + speech launcher + tab routing\n  VoiceLedgerApplication.kt\n  voice/  AmountParser · TransactionParser · DebtParser · ExpenseParser\n  data/   Transaction · Customer · Debt · Expense · *Dao · LedgerDatabase\n  ui/     AppRoot · LedgerScreen · InsightsScreen · CreditScreen · *ViewModel · theme/`,
};

export const DECISIONS = [
  { axis: 'Runtime', decision: 'Native Android (Kotlin + Jetpack Compose), single module. Chosen after the prior 134-file app never compiled — a small coherent core over a broken whole.', adr: '—' },
  { axis: 'Persistence', decision: 'Room/SQLite, fully **local / offline-first**. No cloud sync yet.', adr: '—' },
  { axis: 'Dependency injection', decision: '**None** — manual wiring, a DB singleton held by the app. Dropped Hilt to remove the kapt failure that broke the old build.', adr: '—' },
  { axis: 'Voice', decision: 'Device **speech-to-text** + a **transparent heuristic parser** (no ML, no cloud keys). Predictable and offline.', adr: '—' },
  { axis: 'Trust', decision: '**Confirm before save** — nothing persists until the trader okays the parsed entry (voice mishears are common).', adr: '—' },
  { axis: 'Credit model', decision: 'A saved **Customer** table + **Debt** rows with `amountPaid` for **partial payments**; balances aggregate per customer.', adr: '—' },
  { axis: 'Profit', decision: 'A separate, categorised **Expense** table subtracted from sales per period — sales and costs never blur; **profit** is simply their difference.', adr: '—' },
];

export const GROUPS = [
  { id: 'loop', title: 'Record a sale' },
  { id: 'insights', title: 'Insights & growth' },
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
    what: `The frame around everything: it holds the Today, Insights and Credit tabs and, when you tap a mic, opens speech recognition and hands the words to whichever tab you're on.`,
    how: `<code>MainActivity.kt</code> + <code>AppRoot.kt</code>. A bottom <code>NavigationBar</code> switches the three tabs; <code>registerForActivityResult</code> launches <code>RecognizerIntent</code> and routes the result to the sales, expense or credit view model.`,
    steps: [['Route tab', 'Today · Insights · Credit.'], ['Launch mic', 'RecognizerIntent.'], ['Deliver text', 'To the active view model.']],
    cond: [{ q: 'Should the mic also auto-detect a credit phrase from the Today tab?', to: 'Roadmap' }] },

  { id: 'MIC', code: 'M', name: 'Speech-to-text', short: 'SPEECH', group: 'loop', gx: 10, gy: 1, w: 2, d: 2, h: 26, kind: 'slab',
    one: `The device turns spoken words into text (push-to-talk today).`,
    what: `Today: Android's own recogniser, push-to-talk — tap, speak, it returns a best-guess sentence. North star: the "listening stall" — the phone in her pouch while she serves, capturing the sale hands-free.`,
    how: `Now: <code>RecognizerIntent.ACTION_RECOGNIZE_SPEECH</code> (free, no key, device-provided). Direction: split the two axes — <mark>language</mark> (Twi/Ga/Ewe/Pidgin) via <code>Meta MMS</code> / <code>GhanaNLP Khaya</code>, <mark>noise</mark> via a small speech-enhancement front-end. Bridge to hands-free = a tiny <mark>wake-word</mark> + short capture + end-of-day review. Vosk has no Ghanaian model; text corpora build the LM, not the acoustic model.`,
    steps: [['Listen', 'Push-to-talk prompt + record.'], ['Transcribe', 'On device / recogniser.'], ['Return', 'Best hypothesis string.'], ['(Next) Wake-word', 'Tiny always-on trigger → capture.'], ['(Next) Know her voice', 'Speaker ID picks her out from customers.']],
    cond: [
      { q: 'Understand Twi/Ga/Ewe + Pidgin?', to: 'Language axis — Meta MMS / GhanaNLP Khaya (not Vosk)' },
      { q: 'Hands-free "listening stall" — phone in pouch while serving?', to: 'Wake-word + short capture + end-of-day review' },
      { q: 'Hear her over market noise?', to: 'Small on-device speech-enhancement front-end (SAM-Audio is heavy / server-side)' },
      { q: 'Where does Ghanaian training data come from?', r: 'The confirm sheet: every correction is an (audio, text) pair; sikabook-models is the seed corpus (2026-08-24).' },
    ] },

  { id: 'PARSER', code: 'P', name: 'Parsers', short: 'PARSER', group: 'loop', gx: 12, gy: 5, w: 3, d: 3, h: 66, kind: 'tall',
    one: `Turns a spoken sentence into a structured sale or credit.`,
    what: `The interpreter. It reads "sold 3 tilapia for 20 cedis" and pulls out the amount (20), quantity (3), and product (Tilapia) — fixing common speech mishears and understanding local number words.`,
    how: `<code>AmountParser</code> (number words incl. Twi/Ga/Ewe + compounds; cedis+pesewas), reused by <code>TransactionParser</code> (product/quantity, canonicalisation), <code>DebtParser</code> (customer name + note) and <code>ExpenseParser</code> (cost + keyword category guess). Pure Kotlin, 31 unit tests. No ML.`,
    steps: [['Numbers → digits', '"twenty five" → 25; "baako" → 1.'], ['Amount', 'cedis + optional pesewas.'], ['Sale: qty + product', 'canonicalise mishears.'], ['Credit: name + note', '"Ama … for fish".'], ['Expense: cost + category', '"rice stock" → Stock.']],
    cond: [{ q: 'How rich should the product/number vocabulary get?', r: 'Start small and transparent; expand from real usage (2026-08-24).' }] },

  { id: 'CONFIRM', code: 'C', name: 'Confirm sheet', short: 'CONFIRM', group: 'loop', gx: 8, gy: 7, w: 2, d: 2, h: 38, kind: 'gate',
    one: `Nothing saves until the trader okays it.`,
    what: `A small editable card that appears after parsing — item, quantity, amount (or customer, amount, note; or expense, amount, category) — so a mishear can be fixed before it's recorded. Also opens when you tap an entry to edit it.`,
    how: `A Compose dialog bound to an editable draft (<code>TransactionDraft</code> / <code>ExpenseDraft</code> / <code>DebtDraft</code>) in the view model; validates the amount on <mark>Save</mark>, then inserts or updates.`,
    steps: [['Prefill', 'From the parse.'], ['Fix', 'Edit any field.'], ['Save', 'Validate → persist.']],
    cond: [] },

  { id: 'TVM', code: 'L', name: 'Today view model', short: 'TODAY VM', group: 'loop', gx: 4, gy: 6, w: 2, d: 2, h: 34, kind: 'box',
    one: `Holds the sales state and saves sales.`,
    what: `Keeps the day's list and running total live on screen, and turns a confirmed draft into a saved sale (or an edit).`,
    how: `<code>LedgerViewModel</code> (AndroidViewModel). Exposes <code>transactions</code> (day-scoped) + <code>todayTotal</code> as <code>StateFlow</code> from the DAO; <code>saveDraft()</code> inserts/updates via <code>TransactionDao</code>.`,
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
    one: `The local SQLite store — sales, expenses, customers, debts.`,
    what: `Everything lives on the phone. Four tables: sales (transactions), expenses, customers, and debts (with how much has been paid). No server; works fully offline.`,
    how: `<code>LedgerDatabase</code> (Room v3). Tables <code>transactions</code>, <code>expenses</code>, <code>customers</code>, <code>debts</code> (FK → customer, cascade); DAOs expose <code>Flow</code> queries incl. per-customer balance and per-period totals.`,
    steps: [['Write', 'insert / update / delete.'], ['Observe', 'Flow queries.'], ['Aggregate', 'SUM per period · balances.']],
    cond: [{ q: 'Real migrations vs destructive reset?', r: 'Destructive fallback while pre-release; write migrations before real users (2026-08-24).' }] },

  // --- insights & growth (built) ---
  { id: 'SUM', code: 'S', name: 'Insights & growth', short: 'INSIGHTS', group: 'insights', gx: 4, gy: 15, w: 2, d: 2, h: 40, kind: 'screen',
    one: `Her day's clarity: sales, expenses, profit, and growth.`,
    what: `Her little accountant's report. For Day / Week / Month: profit (sales − expenses) up top with a sales-vs-expenses breakdown and growth against the previous period, a 7/30-day trend, best sellers, the recent-expenses list, and a recap to share on WhatsApp.`,
    how: `<code>InsightsScreen</code> + <code>InsightsViewModel</code>. A period selector drives <code>flatMapLatest</code> over combined DAO flows (sales totals/counts, top products, daily trend) plus expense totals; profit and per-period growth are computed on top. Share via <code>ACTION_SEND</code>.`,
    steps: [['Period', 'Day / Week / Month.'], ['Profit', 'Sales − expenses, vs last period.'], ['Trend + top', '7/30-day bars, best sellers.'], ['Share', 'A recap to WhatsApp.']],
    cond: [{ q: 'Show profit once expenses are tracked?', r: 'Done — expenses now feed profit (2026-08-25).' }] },

  { id: 'EXP', code: 'E', name: 'Expenses', short: 'EXPENSES', group: 'insights', gx: 1, gy: 16, w: 2, d: 2, h: 32, kind: 'box',
    one: `Costs the trader pays — so profit becomes real.`,
    what: `The other half of the money picture: restocking, transport, table toll, light bill, wages. Spoken like a sale ("bought rice stock for 200 cedis"), auto-sorted into a category, and subtracted from sales to give profit. Recorded from the Insights tab's gold mic.`,
    how: `<code>ExpenseViewModel</code> + <code>ExpenseParser</code> (reuses <code>AmountParser</code>; keyword category guess) write <code>Expense</code> rows via <code>ExpenseDao</code>; the recent-expenses list and confirm sheet live on the Insights tab.`,
    steps: [['Hear', 'Mic → "bought rice stock 200".'], ['Categorise', 'Stock / Transport / Rent / Utilities / Wages / Other.'], ['Confirm', 'Editable sheet → save.'], ['Subtract', 'Profit = sales − expenses.']],
    cond: [{ q: 'Sharpen auto-categorisation as vocabulary grows?', r: 'Keyword guess now; expand from real usage.' }] },

  // --- planned / deferred (ghosts) ---
  { id: 'DEMAND', code: 'F', name: 'Demand forecast', short: 'DEMAND', group: 'planned', ghost: true, gx: 1, gy: 11, w: 2, d: 2, h: 30, kind: 'box',
    one: `Later: what sells, and when — the seasonal rhythm.`,
    what: `From her own history: what's in demand most, the seasonal pattern of what she sells, and a sense of what will sell and when it's coming — so she can stock and plan ahead.`,
    how: `Planned: patterns over the ledger, later a small on-device model. Not built.`,
    steps: [['Rank', 'Top products / times.'], ['Seasonality', 'Weekly / monthly cycles.'], ['Look ahead', '"Yam season is coming."']],
    cond: ['Depends on months of real data.'] },

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
  { id: 'expense', name: 'Record an expense → profit', hops: [
    ['SUM', 'SHELL', 'add expense', {}, 'yx'],
    ['SHELL', 'MIC', 'listen', {}, 'xy'],
    ['MIC', 'SHELL', 'heard', { text: 'bought rice stock for 200 cedis' }, 'xy'],
    ['SHELL', 'PARSER', 'parse', { text: 'bought rice stock for 200 cedis' }, 'yx'],
    ['PARSER', 'CONFIRM', 'draft', { desc: 'Rice stock', amount: 200.0, category: 'Stock' }, 'xy'],
    ['CONFIRM', 'EXP', 'save', { desc: 'Rice stock', amount: 200.0 }, 'yx'],
    ['EXP', 'DB', 'insert', { table: 'expenses' }, 'xy'],
    ['DB', 'SUM', 'flow update', { profit: 'sales − expenses' }, 'yx'],
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

  { id: 'money', title: 'The money picture', reveal: ['EXP', 'SUM'],
    lede: `Sales are only half of it — costs make profit real.`,
    story: `<p>The <mark>Insights</mark> tab is her little accountant: pick Day / Week / Month and see <mark>profit</mark> (sales − expenses), a sales-vs-expenses split, growth against the previous period, a trend, best sellers, and a recap to share. <mark>Expenses</mark> are spoken like sales ("bought rice stock for 200 cedis") and auto-sorted into a category, then subtracted from sales.</p>`,
    flow: [['SUM', 'SHELL', 'add expense', {}], ['SHELL', 'MIC', 'listen', {}], ['MIC', 'PARSER', 'heard', { text: 'bought rice stock 200' }], ['PARSER', 'CONFIRM', 'draft', { desc: 'Rice stock', amount: 200.0, category: 'Stock' }], ['CONFIRM', 'EXP', 'save', {}], ['EXP', 'DB', 'insert', { table: 'expenses' }], ['DB', 'SUM', 'profit', { profit: 'sales − expenses' }]] },

  { id: 'creditch', title: 'The credit ledger', reveal: ['CVM', 'CREDIT'],
    lede: `The second tab: who owes you, and paying it down.`,
    story: `<p>Speaking "Ama owes 20 cedis for fish" runs the same mic → parser → confirm path into the <mark>credit view model</mark>. Customers are saved and reused, balances add up per person, and <mark>partial payments</mark> ("Received 10") settle a debt over time.</p>`,
    flow: [['CONFIRM', 'CVM', 'save', { customer: 'Ama', amount: 20.0 }], ['CVM', 'DB', 'customer + debt', {}], ['DB', 'CVM', 'balances', { 'Ama owes': 20.0 }], ['CVM', 'CREDIT', 'debtors', { row: 'Ama · GHS 20' }], ['CREDIT', 'CVM', 'Received 10', { pay: 10.0 }], ['CVM', 'DB', 'amountPaid', { amountPaid: 10.0 }]] },

  { id: 'planned', title: 'Planned & deferred', reveal: ['DEMAND', 'META', 'CLOUD'],
    lede: `Designed for, not switched on.`,
    story: `<p>Dashed boxes are the roadmap: <mark>demand forecast</mark> (what sells and when), the <mark>metaphor views</mark> from the UI vision (Money Pile, Progress Journey), and the deferred heavy lifting — cloud sync, speaker ID, encryption — parked until real traders ask for it. See <mark>VISION.md</mark> for the north star.</p>`,
    flow: [['DB', 'DEMAND', 'patterns', { top: 'Tilapia' }], ['DEMAND', 'META', 'render', {}]] },

  { id: 'all', title: 'The whole system', reveal: [],
    lede: `Everything at once, for free exploration.`,
    story: `<p>Choose which flow runs (bottom left): a sale, an expense → profit, or a credit + payment. Hover anything to read it; click to pin; <mark>→ goes inside</mark> a structure to see its steps. The <mark>Open questions</mark> tab lists every open decision by ID.</p>`,
    flow: null },
];

export const HOW_HTML = `<div class="eyebrow">Ghana Voice Ledger · v0.2-core</div><h1 class="t">How it's built</h1><div class="sub">a small offline Android app: speak → parse → confirm → local ledger → insights</div>
<h3 class="sec">The shape</h3><p>One Android module. The device does speech-to-text; a transparent Kotlin parser structures it; a confirm sheet guards every write; Room holds sales, expenses, customers and debts on the phone. Three tabs — Today (sales), Insights (profit + growth) and Credit (who owes you) — share the same mic → parse → confirm path.</p>
<h3 class="sec">Filesystem</h3><pre>app/src/main/java/com/voiceledger/ghana/
  MainActivity.kt          host + mic + tab routing
  voice/   AmountParser · TransactionParser · DebtParser · ExpenseParser
  data/    Transaction · Customer · Debt · Expense · *Dao · LedgerDatabase
  ui/      AppRoot · LedgerScreen · InsightsScreen · CreditScreen · *ViewModel · theme/</pre>
<h3 class="sec">Not built yet</h3><p>Demand forecast, metaphor views (Money Pile / Progress Journey), Twi/Ga/Ewe labels, cloud sync, speaker ID, encryption.</p>`;
