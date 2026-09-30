# Draft email to the SnT TruX group (University of Luxembourg)

Addressees: Dr Maimouna Ouattara, Dr Abdoul Kader Kaboré, Prof. Jacques Klein, Prof. Tegawendé F. Bissyandé. Find current addresses on the SnT people pages (for example https://www.uni.lu/snt-en/people/maimouna-ouattara/) rather than guessing them. Send from an institutional address if you have one. Fill the bracketed fields before sending.

---

**Subject:** Voice-based financial record keeping for Ghanaian market traders: your Mooré-French corpus and GG-AbS

Dear Dr Ouattara, Dr Kaboré, Professor Klein and Professor Bissyandé,

I am [your name], [role and affiliation, city], and I am building an open, non-commercial voice bookkeeper for informal market traders in Ghana. The aim is to let a trader who may not read or write record a sale or a credit entry by speaking in Twi, Ga or Ewe, with English numerals and Pidgin mixed in as they are in the market, and to receive a spoken daily summary and a record that can later support access to credit. The code is public (github.com/Christorious/ghana-voice-ledger and github.com/Christorious/sikabook-models), there is no revenue model behind it, and any corpus I collect will be released under CC-BY-4.0 with the consent of the speakers.

Your LoResLM 2025 position paper and your forthcoming LaTeLL 2026 paper on generator-guided amount recovery in Mooré-French code-switched speech describe, as far as I can find, the only quantified voice-to-ledger pipeline for an African language. I read the code release with great interest. Your finding that amount recovery rather than general transcription is the central difficulty matches what I see on GhanaNLP's Akan benchmark, where the best open model reaches roughly 45 percent word error on general Twi speech and about 19 percent on finance-domain read speech, and where every frontier speech model fails. I already have a deterministic Twi numeral parser with the Akan tens and hundreds forms, which looks like half of your GG-AbS module, and I would like to build the other half properly rather than reinvent it.

I am writing with three requests, and I fully understand if the answer to any of them must wait until your camera-ready version is out. First, would it be possible to obtain the Mooré, French and code-switched audio, or failing that the transcripts, ground-truth ledgers and the collection protocol, including the prompts, consent form, speaker recruitment and recording conditions, for research use? My immediate purpose is to replicate your evaluation before attempting the same task in Akan, and to copy a protocol that has already passed review. Second, could you point me to the parts of the wakir generator and the frozen drift-rewrite rules that you consider language-specific, so that an Akan port respects your design rather than approximating it? Third, would you be open to a cross-lingual extension of the work, in which the same pipeline is evaluated on Twi, Ga and Ewe with English rather than French as the embedded language, with a view to a joint paper? I can contribute Ghanaian recordings collected under University of Ghana or KNUST ethics approval, evaluation on the GhanaNLP benchmark's finance split, and on-device measurements on the low-cost Android phones traders actually own.

Two smaller studies I am running may also interest you: a literature review on robust voice activation in the non-stationary noise of Ghanaian markets, where afternoon levels reach 71 to 77 dBA, and an assessment of federated learning for improving the recogniser from consented on-device recordings without uploading audio.

Thank you for making the code public. I would be glad to talk by video call at a time that suits you.

With best regards,

[Your name]
[Affiliation, city]
[Email, phone]
[Links: repositories, any prior work]
