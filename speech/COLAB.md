# Train the Ga model on a free GPU — runbook

You need a free **Google Colab** (Runtime → Change runtime type → **T4 GPU**) or Kaggle
notebook. Total time on a T4: ~1–3 h for whisper-base, 6 epochs. Nothing here needs a paid GPU.

## 0. Get the files there
Upload to the Colab session (or push this `speech/` folder to a repo and `git clone` it):
- `prepare_fsid_ga.py`, `finetune_whisper_ga.py`
- `11-fsid_ga.zip` (the dataset — upload to the session or your Google Drive)

## 1. Install deps + ffmpeg
```bash
!apt-get -qq install -y ffmpeg
!pip -q install -U "transformers>=4.44" datasets accelerate evaluate jiwer soundfile librosa
```

## 2. Unzip + lay out audio
```bash
!unzip -q 11-fsid_ga.zip -d fsid
!mkdir -p audio/train audio/test manifests
# flatten the split audio dirs to audio/train and audio/test (basename join)
!cp fsid/fsid-ga/fisd-ga-90p/audios/*.ogg audio/train/
!cp fsid/fsid-ga/fisd-ga-10p/audios/*.ogg audio/test/
```

## 3. Build manifests (with 2× money-phrase oversampling)
```bash
!python prepare_fsid_ga.py \
    --data-csv fsid/fsid-ga/fisd-ga-90p/data.csv \
    --audio-root audio/train --out manifests/train.jsonl --oversample-money 2 --check-audio
!python prepare_fsid_ga.py \
    --data-csv fsid/fsid-ga/fisd-ga-10p/data.csv \
    --audio-root audio/test  --out manifests/test.jsonl  --check-audio
```

## 3b. (Optional) Multilingual — all four FISD languages at once
Repeat steps 2–3 for each zip with a `--lang` tag, then concatenate the manifests. Whisper
finetunes fine on mixed Ga + Asante/Akuapem Twi + Fante, covering far more of Ghana in one model:
```bash
# for each: unzip -> cp audios to audio/train_<lang> & audio/test_<lang> -> prep with --lang
!python prepare_fsid_ga.py --data-csv fsid/fsid-asanti-twi-data/fisd-asanti-twi-90p/data.csv \
    --audio-root audio/train_asante --out manifests/asante_train.jsonl --lang asante-twi --oversample-money 2 --check-audio
# ...repeat for akuapem-twi, fanti, ga...
!cat manifests/*_train.jsonl > manifests/train.jsonl
!cat manifests/*_test.jsonl  > manifests/test.jsonl
```
Add the **Alpha TWI** dataset (conversational/novel Twi, different format) the same way:
```bash
!python prepare_fsid_ga.py --data-csv sample_alpha_dataset/sample_train/train_transcription.csv \
    --audio-root audio/alpha_train --out manifests/alpha_train.jsonl --format alpha --lang asante-twi
# (.wav 44.1kHz stereo — the HF Audio(16000) cast in the trainer resamples/downmixes automatically)
```
Then finetune as below. Use `--language` matching Whisper's closest token (try `sw`/`ha`/`yo`);
the model learns the actual language from the data. Consider `whisper-small` for the multilingual
run if the GPU allows — more capacity helps across four languages (keep a `base`/`tiny` distill for phones).

## 4. Finetune
```bash
!python finetune_whisper_ga.py --model openai/whisper-base --epochs 6 --batch 16 --language sw
```
Watch `eval_wer` each epoch. Tips:
- If WER stalls high, try `--language ha` (Hausa) or `--language yo` (Yoruba) as the placeholder
  token, or `--model openai/whisper-small` (better WER, but keep for eval — too heavy for cheap phones).
- If you OOM, drop `--batch 8 --grad-accum 2`.

## 5. Export to whisper.cpp + quantize
```bash
!git clone -q https://github.com/ggml-org/whisper.cpp
!python whisper.cpp/models/convert-h5-to-ggml.py whisper-ga-base whisper-ga-base ./
!cmake -q -B whisper.cpp/build whisper.cpp && cmake --build whisper.cpp/build -j --config Release
!./whisper.cpp/build/bin/quantize ggml-model.bin ggml-ga-base-q5_0.bin q5_0
!ls -lh ggml-ga-base-q5_0.bin        # expect ~60-80 MB
```

## 6. Sanity-check the ggml model on a test clip
```bash
!ffmpeg -y -i audio/test/$(ls audio/test | head -1) -ar 16000 -ac 1 sample.wav
!./whisper.cpp/build/bin/whisper-cli -m ggml-ga-base-q5_0.bin -l sw sample.wav
```

## 7. Download the model
```python
from google.colab import files
files.download("ggml-ga-base-q5_0.bin")
```
Ship this `.bin` in the Android app — see `android_whisper.md`.

---
**Quantization choices:** `q5_0` is the size/quality sweet spot (~60–80 MB for base).
Use `q4_0` (~45–60 MB) for the weakest phones, or `q8_0` / no-quant for best quality on eval.
