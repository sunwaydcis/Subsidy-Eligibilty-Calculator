# Subsidy Eligibility Calculator

A standalone ScalaFX desktop application built for **PRG2104 (Object-Oriented
Programming)**, addressing **UN Sustainable Development Goal 1 — No
Poverty**.

A household enters its income, dependents, and assets; the application
determines which of four government subsidy programmes it qualifies for,
explains why, lists the documents required to apply, lets the household
track which documents it has gathered, and exports that checklist to a
plain text file — with every household and eligibility check persisted
between runs.

## Features

1. **Household Profile** — enter a household's ID and monthly income;
   add and remove dependents and assets in two editable tables; save the
   household to disk. All input is validated inline (non-negative income,
   dependent age 0–120, no blank names) with friendly per-field error
   messages rather than crashes or stack traces.
2. **Eligibility Check** — pick a saved household and evaluate it against
   all four subsidy programmes at once. Each result shows eligible/not
   eligible, the reason, and the documents required — colour-coded for
   quick scanning. Every check is recorded to history automatically.
3. **Checklist** — pick a household and one of the programmes it's
   currently eligible for; tick off each required document as it's
   gathered; export the checklist (programme, household, timestamp, and
   which documents are ticked) to a plain text file of your choosing.
4. **History** — every past eligibility check, most recent first, with a
   live search box that filters by household ID or programme name as you
   type.

## Tech stack

| Component | Version |
|---|---|
| Language | Scala 3.3.7 |
| UI library | ScalaFX 21.0.0-R32 (JavaFX 25.0.3 underneath) |
| Build tool | sbt 1.10.5 |
| JVM | Java 21+ |
| Persistence | Plain JSON files (no database) |

## Prerequisites

- **JDK 21 or newer** — check with `java -version`.
- **sbt 1.9+** — check with `sbt --version`. (The project itself pins the
  exact sbt version via `project/build.properties`; you just need *some*
  working sbt launcher installed.)

No other manual installation is needed — the first build downloads
ScalaFX and the correct JavaFX native binaries for your OS (Windows /
macOS / Linux are all detected automatically in `build.sbt`).

## Running the app

Clone or unzip this project, then from the project root (the folder
containing `build.sbt`):

```
sbt run
```

The very first run downloads dependencies and can take several minutes
depending on your connection — this is normal, not a hang. A window
titled **"Subsidy Eligibility Calculator"** should appear within a few
seconds after that.

Alternatively, in IntelliJ IDEA: open the project folder, let it import
as an sbt project, then right-click `Main.scala` and choose **Run**.

### Optional: a standalone runnable jar

```
sbt assembly
java -jar target/scala-3.3.7/subsidy-calculator-assembly-0.1.0-SNAPSHOT.jar
```

(Uses the `sbt-assembly` plugin, already configured in
`project/plugins.sbt` — comment it out there if you don't need a packaged
jar.)

## Data & persistence

Saved households and eligibility-check history are stored as plain JSON,
written to a folder in your home directory — **not** inside the project
folder, so they survive across runs regardless of where the project is
unzipped:

```
~/.subsidy-eligibility-calculator/households.json
~/.subsidy-eligibility-calculator/eligibility-history.json
```

This folder is created automatically on first save; deleting it resets
the application to a clean slate.

## Project structure

This follows the standard submission structure required by the module 
brief exactly — every path below is fixed, so the marker 
(or an AI-assisted marking tool) can navigate straight to each 
artifact without hunting through the ZIP:

```
Project_<23071335>/
├── README.md                 # setup, run, project summary, AI summary
├── build.sbt                 # Scala 3.3+ + ScalaFX 21
├── project/
│   └── build.properties      # sbt version
├── src/
│   ├── main/
│   │   ├── scala/            # all production source files
│   │   └── resources/        # sample data, assets, CSV/JSON
│   └── test/
│       └── scala/            # unit tests (optional but was recommended)
├── docs/
│   ├── UML.png                # class diagram, ≥5 classes
│   ├── reflection.md          # personal reflection (~350–700 words)
│   ├── ai_reflection.md       # AI integration reflection (300–500 words)
│   ├── dev_log.md              # ≥5 dated entries spanning ≥5 calendar dates
│   ├── citations.md            # third-party code/asset citations + licenses
│   └── demo.mp4                # ≤5 min walkthrough of all 4+ features
├── ai/
│   ├── interaction_log.md     # Template 2 — ≥10 entries
│   └── declaration.md         # Template 3 — signed
└── submission_manifest.md     # Checklist + self-reported metrics, signed
```

## Third-party libraries

| Library | URL | License |
|---|---|---|
| ScalaFX 21.0.0-R32 | https://www.scalafx.org | BSD 3-Clause |
| OpenJFX (JavaFX) 25.0.3 | https://openjfx.io | GPL v2 with the Classpath Exception |
| sbt-assembly 2.2.0 (optional, build-time only) | https://github.com/sbt/sbt-assembly | MIT |

No other third-party code or assets are used — all application logic,
UI layout, and styling is original.

## AI use

This project was developed under **PRG2104's Tier C (AI-Integrated)**
policy. AI assistance (Claude, Anthropic) was used throughout — for
scaffolding, refactoring toward DRY, and debugging compile/layout issues
— and every significant prompt is logged in [`ai/interaction_log.md`](ai/interaction_log.md),
with the signed declaration in [`ai/declaration.md`](ai/declaration.md).
AI-assisted blocks are tagged in source with `// ai-assisted: #N`, cross-referenced
to the matching log entry. See `docs/ai_reflection.md` for a fuller
reflection on where AI helped, where it was wrong, and what I did myself.
