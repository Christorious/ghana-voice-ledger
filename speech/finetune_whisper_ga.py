#!/usr/bin/env python3
"""
Finetune openai/whisper-base on the Ga Financial Inclusion Speech Dataset.

Designed to run on a FREE Colab / Kaggle GPU (T4 is enough for whisper-base).
See COLAB.md for the copy-paste runbook. Locally it needs a CUDA GPU to be practical.

Pipeline: JSONL manifests (from prepare_fsid_ga.py)  ->  HF Dataset (16 kHz mono)
          ->  WhisperForConditionalGeneration finetune  ->  eval WER  ->  save.
Then convert the saved HF checkpoint to ggml + quantize for whisper.cpp (see bottom).

Deps:
  pip install -U "transformers>=4.44" datasets accelerate evaluate jiwer soundfile librosa
  # ffmpeg must be present to decode Opus/.ogg (Colab has it; else apt-get install ffmpeg)
"""
import argparse
import os
from dataclasses import dataclass
from typing import Any, Dict, List, Union

import torch


def build_dataset(train_jsonl: str, test_jsonl: str, audio_train: str, audio_test: str):
    """Load JSONL manifests, rewrite audio paths to the real dirs, cast to 16 kHz Audio."""
    from datasets import Audio, load_dataset

    ds = load_dataset(
        "json",
        data_files={"train": train_jsonl, "test": test_jsonl},
    )

    def fix_path(root):
        def _f(batch):
            batch["audio"] = os.path.join(root, os.path.basename(batch["audio"]))
            return batch
        return _f

    ds["train"] = ds["train"].map(fix_path(audio_train))
    ds["test"] = ds["test"].map(fix_path(audio_test))
    # Whisper wants 16 kHz mono; the Audio feature resamples Opus/48k on the fly via ffmpeg.
    ds = ds.cast_column("audio", Audio(sampling_rate=16000))
    return ds


@dataclass
class DataCollatorSpeechSeq2SeqWithPadding:
    processor: Any
    decoder_start_token_id: int

    def __call__(self, features: List[Dict[str, Union[List[int], torch.Tensor]]]) -> Dict[str, torch.Tensor]:
        input_features = [{"input_features": f["input_features"]} for f in features]
        batch = self.processor.feature_extractor.pad(input_features, return_tensors="pt")
        label_features = [{"input_ids": f["labels"]} for f in features]
        labels_batch = self.processor.tokenizer.pad(label_features, return_tensors="pt")
        labels = labels_batch["input_ids"].masked_fill(labels_batch.attention_mask.ne(1), -100)
        # If BOS was appended at tokenization, drop it (added back by the model).
        if (labels[:, 0] == self.decoder_start_token_id).all().cpu().item():
            labels = labels[:, 1:]
        batch["labels"] = labels
        return batch


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="openai/whisper-base")
    ap.add_argument("--train-jsonl", default="manifests/train.jsonl")
    ap.add_argument("--test-jsonl", default="manifests/test.jsonl")
    ap.add_argument("--audio-train", default="audio/train")
    ap.add_argument("--audio-test", default="audio/test")
    ap.add_argument("--out", default="whisper-ga-base")
    # Ga is not a native Whisper language; a related placeholder works because Whisper uses
    # byte-level BPE (no OOV). Swahili ("sw") is a reasonable African placeholder; try a few.
    ap.add_argument("--language", default="sw")
    ap.add_argument("--epochs", type=float, default=6.0)
    ap.add_argument("--batch", type=int, default=16)
    ap.add_argument("--grad-accum", type=int, default=1)
    ap.add_argument("--lr", type=float, default=1e-5)
    args = ap.parse_args()

    from transformers import (
        Seq2SeqTrainer,
        Seq2SeqTrainingArguments,
        WhisperForConditionalGeneration,
        WhisperProcessor,
    )
    import evaluate

    processor = WhisperProcessor.from_pretrained(args.model, language=args.language, task="transcribe")
    ds = build_dataset(args.train_jsonl, args.test_jsonl, args.audio_train, args.audio_test)

    def prepare(batch):
        audio = batch["audio"]
        batch["input_features"] = processor.feature_extractor(
            audio["array"], sampling_rate=audio["sampling_rate"]
        ).input_features[0]
        batch["labels"] = processor.tokenizer(batch["sentence"]).input_ids
        return batch

    ds = ds.map(prepare, remove_columns=ds["train"].column_names, num_proc=2)

    model = WhisperForConditionalGeneration.from_pretrained(args.model)
    model.generation_config.language = args.language
    model.generation_config.task = "transcribe"
    model.generation_config.forced_decoder_ids = None

    collator = DataCollatorSpeechSeq2SeqWithPadding(
        processor=processor, decoder_start_token_id=model.config.decoder_start_token_id
    )
    wer_metric = evaluate.load("wer")

    def compute_metrics(pred):
        pred_ids = pred.predictions
        label_ids = pred.label_ids
        label_ids[label_ids == -100] = processor.tokenizer.pad_token_id
        pred_str = processor.tokenizer.batch_decode(pred_ids, skip_special_tokens=True)
        label_str = processor.tokenizer.batch_decode(label_ids, skip_special_tokens=True)
        return {"wer": 100 * wer_metric.compute(predictions=pred_str, references=label_str)}

    training_args = Seq2SeqTrainingArguments(
        output_dir=args.out,
        per_device_train_batch_size=args.batch,
        gradient_accumulation_steps=args.grad_accum,
        learning_rate=args.lr,
        warmup_ratio=0.1,
        num_train_epochs=args.epochs,
        gradient_checkpointing=True,
        fp16=torch.cuda.is_available(),
        eval_strategy="epoch",
        save_strategy="epoch",
        predict_with_generate=True,
        generation_max_length=128,
        logging_steps=50,
        report_to=[],
        load_best_model_at_end=True,
        metric_for_best_model="wer",
        greater_is_better=False,
    )

    trainer = Seq2SeqTrainer(
        args=training_args,
        model=model,
        train_dataset=ds["train"],
        eval_dataset=ds["test"],
        data_collator=collator,
        compute_metrics=compute_metrics,
        tokenizer=processor.feature_extractor,
    )

    trainer.train()
    metrics = trainer.evaluate()
    print("FINAL:", metrics)
    trainer.save_model(args.out)
    processor.save_pretrained(args.out)
    print(f"Saved HF checkpoint to {args.out}/  (WER={metrics.get('eval_wer'):.1f}%)")


if __name__ == "__main__":
    main()

# ---------------------------------------------------------------------------
# Export to whisper.cpp (run after training; see COLAB.md for the exact cell):
#
#   git clone https://github.com/ggml-org/whisper.cpp
#   python whisper.cpp/models/convert-h5-to-ggml.py whisper-ga-base whisper-ga-base ./
#   # -> produces ggml-model.bin
#   cmake -B whisper.cpp/build whisper.cpp && cmake --build whisper.cpp/build -j --config Release
#   ./whisper.cpp/build/bin/quantize ggml-model.bin ggml-ga-base-q5_0.bin q5_0
#   # ship ggml-ga-base-q5_0.bin (~60-80 MB) in the Android app's assets/.
# ---------------------------------------------------------------------------
