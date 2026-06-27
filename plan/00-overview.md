# NutriSnap — Project Overview & Master Plan

> On-device AI calorie tracker for Android. Snap a meal → **Gemma 4** (via **LiteRT-LM**)
> identifies the food and estimates calories + macros, fully offline. Inspired by
> "Pookies Food AI" (Cal-AI style), with a playful "Snackable" aesthetic.

Package: **`com.charles.nutrisnap`** · App name: **NutriSnap** · Mascot: **Pip** 🥭

---

## Locked decisions
- **Platform:** Native Android — Kotlin + Jetpack Compose (Material 3).
- **On-device model:** **Gemma 4 E2B** (`litert-community/gemma-4-E2B-it-litert-lm`, `.litertlm`),
  natively multimodal (text + image + audio → text). E4B optional higher-accuracy download.
- **Runtime:** **LiteRT-LM** (Google AI Edge, built on LiteRT). Reference: AI Edge Gallery +
  `google-ai-edge/LiteRT-LM`.
- **Features (v1):** AI photo food scan · daily dashboard + personalized goals ·
  meal history + streaks · manual & barcode entry.
- **Design:** Google **Stitch** (9 screens, "Snackable" design system) +
  hand-built HTML north star (`design/style-tile.html`). Translated to a Compose theme.
- **Visual personality:** Playful & cute — mango+berry candy palette, super-rounded chunky
  cards, soft warm shadows, Pip the mascot, bouncy micro-animations, emoji-forward.

## "Snackable" design tokens
- Cream bg `#FFF6EC`, card `#FFFDF9`, text cocoa `#3A2A21`, muted `#8A776B`.
- Mango `#FF9F1C` (primary, deep `#F4860A`), Berry `#FF5D73` (accent).
- Macros: Protein = Mint `#2EC4A6`, Carbs = Grape `#7C5CFC`, Fat = Butter `#FFD66B`. Info = Sky `#5BC0EB`.
- Display/numbers: Fredoka/Quicksand. Body: Nunito/Nunito Sans. Very rounded; candy "pop" button shadow.

---

## Architecture

Single Gradle module `:app`, layered by responsibility:

```
com.charles.nutrisnap
  ui/            theme (Color/Type/Shape/Theme), components, nav, NutriSnapApp root scaffold
  feature/
    onboarding/  goal questionnaire + GoalCalculator + model download screen
    scan/        camera capture + AI inference result
    dashboard/   home rings + today log
    history/     diary, trends, streaks, weight
    entry/       manual + barcode
    profile/     profile + entry to settings
    settings/    goals, units, model management
  ai/            GemmaEngine interface + LiteRT-LM (Gemma 4) impl
  data/          Room DB, repositories, DataStore (prefs/goals/model state)
  download/      ModelDownloadManager + ModelDownloadWorker (WorkManager + resumable HTTP)
  di/            Hilt modules
```

### Key isolation boundaries (interfaces)
- `GemmaEngine` — `analyzeFood(image, hint) : FoodEstimate`, `estimateFromText(desc) : FoodEstimate`.
  One LiteRT-LM-backed impl; UI never touches the runtime directly.
- `ModelRepository` — exposes `ModelState { NotDownloaded | Downloading(pct) | Ready | Failed }`.
- `MealRepository`, `GoalRepository`, `WeightRepository` over Room + DataStore.

### Data flow (scan)
Camera/Gallery → `Bitmap` → `GemmaEngine.analyzeFood` (structured-JSON prompt → name, grams, kcal,
macros, confidence) → parse → editable result sheet → confirm → `MealRepository.log()` →
dashboard rings + streak update. Low-confidence/parse-failure → graceful fallback to manual entry.

---

## Tech stack
Kotlin · Jetpack Compose + Material 3 · Navigation-Compose · Hilt · Room · DataStore ·
WorkManager + OkHttp · CameraX · ML Kit Barcode · Coil · kotlinx.serialization · Coroutines/Flow ·
**LiteRT-LM** (Gemma 4). Min SDK 26, target/compile 34. JDK 17 / Gradle 8.5 / AGP 8.3.2.
Tests: JUnit + Turbine + MockK + coroutines-test; Compose UI tests.

## Build phases (each detailed in its own file)
| Phase | File | Status |
|------|------|--------|
| 0 — Design | [phase-0-design.md](phase-0-design.md) | ✅ Done |
| 1 — Project + theme + nav | [phase-1-project-theme-nav.md](phase-1-project-theme-nav.md) | ✅ Done |
| 2 — Onboarding + model download | [phase-2-onboarding-model-download.md](phase-2-onboarding-model-download.md) | ✅ Done |
| 3 — Data layer | [phase-3-data-layer.md](phase-3-data-layer.md) | ✅ Done |
| 4 — AI engine (Gemma 4 / LiteRT-LM) | [phase-4-ai-engine.md](phase-4-ai-engine.md) | 🚧 Scaffolded (stubbed) |
| 5 — Feature wiring | [phase-5-feature-wiring.md](phase-5-feature-wiring.md) | ⬜ Pending |
| 6 — Polish | [phase-6-polish.md](phase-6-polish.md) | ⬜ Pending |

## Open items / deferred decisions
- **Model host URL:** `BuildConfig.MODEL_URL` defaults to the `litert-community` `.litertlm` asset.
  Provide a HuggingFace token or self-hosted mirror if the repo gates anonymous download.
- **Fonts:** display/body currently map to a system default behind `DisplayFontFamily`/`BodyFontFamily`
  constants in `ui/theme/Type.kt`; swap to real Fredoka/Quicksand + Nunito in Phase 6.
- **Device requirement:** Gemma 4 needs a physical device (≥6–8 GB RAM); emulator can't run it.

## Verification (end-to-end)
- `./gradlew :app:assembleDebug` builds; `./gradlew test` passes unit tests.
- On a physical device: onboarding → sensible kcal goal → model download (resumable, survives
  backgrounding) → photograph a meal offline → editable result → log → dashboard updates →
  manual/barcode entry + history/streak/weight work.

## Open risk
On-device Gemma 4 vision is heavy: first-load latency, RAM pressure, accuracy variance.
Mitigation: E2B default (≈3× faster, ≈60% less battery than E4B), warm-load the engine once,
always show confidence + allow manual edit, fall back to manual entry on low confidence/parse failure.
