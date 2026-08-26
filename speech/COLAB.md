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
