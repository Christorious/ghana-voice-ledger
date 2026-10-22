# Ghana Voice Ledger — Architecture Atlas

An **explorable, isometric map** of the app plus a generated **text twin**. One data file
drives both; edit only the data file and rebuild.

| File | What it is |
| --- | --- |
| `data.mjs` | **The only file you edit.** Structures, flows, chapters, decisions, questions. |
| `build.mjs` | Builds `atlas.html` + `SYSTEM.md` from `data.mjs`. |
| `template.html` | The renderer (injected at build). |
| `atlas.html` | **Generated** — open in a browser. Interactive map: hover to read, click to pin, → to go inside, chapters that reveal the system a few pieces at a time, a flow picker. |
| `SYSTEM.md` | **Generated** — the text twin: decisions table, every structure, flows, open questions by ID. |

## Rebuild

```bash
node docs/atlas/build.mjs
```

Never hand-edit `atlas.html` or `SYSTEM.md` — change `data.mjs` and rebuild.
