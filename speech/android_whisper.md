# Android integration — offline Ga recognition via whisper.cpp

Goal: replace/augment the current push-to-talk `RecognizerIntent` with an **on-device**
recognizer running our finetuned Ga model, **behind one interface** so the rest of the app
(the confirm-sheet parser flow) doesn't change. Keep `RecognizerIntent` as a fallback.

## Where it plugs in today

`MainActivity.startVoice(target)` launches `RecognizerIntent`, and the recognized string is
delivered to `ledgerViewModel.beginFromText` / `expenseViewModel.beginFromText` /
`creditViewModel.beginCreditFromText`. **We keep exactly that contract** — a function that
turns a recording into a `String` — and swap what produces the string.

## The interface

```kotlin
// ui/voice/OfflineRecognizer.kt
interface SpeechRecognizer {
    /** Record from the mic, transcribe, and return best-guess text (or null on failure). */
    suspend fun transcribe(): String?
}
```

Two implementations, chosen at runtime (Ga model if present, else device recognizer):

1. **`WhisperRecognizer`** — records ~a few seconds of 16 kHz mono PCM, runs whisper.cpp
   (JNI) with our `ggml-ga-base-q5_0.bin` from `assets/`, returns the transcript. Batch mode
   on short clips (see STRATEGY: avoid streaming on budget phones).
2. **`DeviceRecognizer`** — wraps the existing `RecognizerIntent` path (today's Stage 0).

## Bringing in whisper.cpp

Fastest route: copy the official Android example and its JNI, then point it at our model.

- Example: `whisper.cpp/examples/whisper.android` (Kotlin) in https://github.com/ggml-org/whisper.cpp
  — it already has the `libwhisper` CMake build, the JNI bridge (`WhisperContext`), and mic capture.
- Steps:
  1. Add `whisper.cpp` as a git submodule (or vendor the needed `ggml*`/`whisper*` sources).
  2. Add an `externalNativeBuild { cmake { ... } }` block in `app/build.gradle.kts` pointing at
     the whisper.cpp `CMakeLists.txt`; set `ndkVersion` and `abiFilters` to `arm64-v8a`
     (+ `armeabi-v7a` for old phones).
  3. Put `ggml-ga-base-q5_0.bin` in `app/src/main/assets/models/`. Copy it to files dir on
     first launch (whisper.cpp loads from a real path).
  4. Record 16 kHz mono `FloatArray` (AudioRecord) → `WhisperContext.transcribeData(floats)`.

## Domain biasing (cheap accuracy win)

whisper.cpp supports an initial **prompt** to bias decoding. Seed it with our market/money
vocabulary so numbers, "cedis/pesewas", and common products are favoured:

```kotlin
val prompt = "cedis pesewas sɛka tilapia gari kenkey banku waakye Ama Kofi Adwoa Kwame " +
             "eko enyɔ etɛ ejwɛ enumɔ ekpaa kpawo kpaanyo nɛhu nyɔŋmɔ"   // Ga numbers 1-10
whisperFullParams.initial_prompt = prompt
```

Reuse `sikabook-models/data/corpus/trading_corpus.txt` + `sikabook_lexicon.txt` to grow this list.

## Hand-off to the existing parser (no change needed)

The transcript goes straight into the current parser, which already:
- converts Twi/Ga/Ewe number words to digits (`AmountParser`),
- extracts amount / quantity / product (`TransactionParser`) or cost/category (`ExpenseParser`),
- and shows the **confirm sheet** so the trader fixes any mishear before saving.

That confirm step is also our **data flywheel**: log `(audio, rawText, correctedText)` on every
save to a local table; export periodically to re-finetune (Stage 2 in STRATEGY.md). Nothing
leaves the phone unless she opts in.

## Suggested build order

1. Land the `SpeechRecognizer` interface + refactor `MainActivity.startVoice` to use it, with
   only `DeviceRecognizer` wired (no behaviour change) — small, safe PR.
2. Add the whisper.cpp native build + `WhisperRecognizer`, gated behind a setting/flag, model in
   assets. Test on a real device with a Colab-trained `.bin`.
3. Add prompt biasing + the `(audio, correction)` logging table.
4. Measure real WER/latency on a cheap phone; if inadequate, evaluate the sherpa-onnx fallback
   (STRATEGY.md) which offers native hotword + KenLM biasing.
