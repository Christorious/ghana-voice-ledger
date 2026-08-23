# Ghana Voice Ledger

A voice-first ledger for Ghanaian traders and market vendors: **speak a sale, and it's recorded** — in cedis, offline, with the running daily total always in view.

> **Status — honest version.** This repository previously contained a large (134-file)
> Android codebase that was documented as "v1.0.0, production-ready" but **had never
> compiled** — it was assembled by merging conflicting generated branches, leaving
> truncated files, duplicate declarations, and cross-file schema conflicts throughout.
> That source has been replaced with a small, coherent **core app that actually builds and
> runs**. The valuable ideas (voice capture, the Room data model, and the cultural-cognition
> research) are being reintroduced on this working foundation rather than debugged in place.

## What the core app does today

- **Record a sale by voice** — tap *Record sale* and say something like
  *"sold 3 tilapia for 20 cedis"*. It uses the device's built-in speech recognition
  (no cloud keys, no bundled ML models).
- **Parse it** into product, quantity, and amount with a simple, transparent heuristic
  (`TransactionParser`) — including spoken number words ("two yams 15 cedis").
- **Store it** in a local Room database (`transactions` table).
- **See the running total** for today plus a scrollable history; delete entries you got wrong.
- **Add manually** as a fallback when voice isn't available.

## Tech stack (core)

- Kotlin, Jetpack Compose (Material 3)
- Room (SQLite) for local persistence
- `AndroidViewModel` + `StateFlow` for state
- Android `RecognizerIntent` for speech-to-text
- Min SDK 24, target/compile SDK 34, JDK 17

Deliberately **not** in the core yet (removed with the broken source; to be reintroduced
as working features): Hilt DI, on-device TensorFlow Lite / speaker ID, Google Cloud Speech,
Firebase, the offline-sync queue, and the multi-flavor release/deploy pipeline.

## Project structure

```
app/src/main/java/com/voiceledger/ghana/
├── MainActivity.kt              # Compose host + speech-recognition launcher
├── VoiceLedgerApplication.kt
├── data/                        # Room: Transaction, TransactionDao, LedgerDatabase
├── ui/                          # LedgerViewModel, LedgerScreen, theme/
└── voice/                       # TransactionParser (speech → structured sale)
```

## Build & run

```bash
./gradlew assembleDebug        # produces app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on a connected device/emulator
./gradlew testDebugUnitTest    # unit tests
```

Requires Android SDK 34 and a JDK 17. Set `sdk.dir` in `local.properties`.

## The idea behind it

The design goal is to meet traders where they already are: capturing a sale *the way it is
spoken*, in Ghana Cedis, without imposing a Western accounting mental model. The research
that motivates this lives in [`CULTURAL_FINANCIAL_COGNITION_RESEARCH.md`](CULTURAL_FINANCIAL_COGNITION_RESEARCH.md).

## Roadmap

- Twi/Ga/Ewe number words and product vocabulary in the parser
- Daily summaries and simple insights
- Optional speaker identification (returning-customer recognition)
- Cloud backup / sync

## Note on the older documentation

Many top-level `*.md` files (build-status reports, APK guides, "final verification" notes)
describe the previous non-compiling codebase and its aspirational feature set. They are
retained for history but should not be taken as an accurate description of the current app.
