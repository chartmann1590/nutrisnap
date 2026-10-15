# Phase 2 — Onboarding + Model Download ✅ Done

> Status: implemented & building. `./gradlew test` (GoalCalculatorTest) and `:app:assembleDebug`
> both pass. Files delivered match the component list below; first-run gating via `AppStartViewModel`.

Goal: first-run flow that personalizes the calorie goal and downloads the Gemma 4 model, gating
entry to the app until the model is ready.

## Components

### 1. Goal math (pure Kotlin, unit-tested)
- `feature/onboarding/GoalModels.kt` — `Sex`, `ActivityLevel` (with factors 1.2/1.375/1.55/1.725),
  `Goal`, `OnboardingInput` (sex, age, heightCm, weightKg, activity, goal, rateKgPerWeek),
  `DailyGoal` (calories, proteinG, carbsG, fatG).
- `feature/onboarding/GoalCalculator.kt` — Mifflin-St Jeor BMR → ×activity = TDEE →
  ± goal delta (`rateKgPerWeek × 7700 / 7`). Macros: protein `1.8 g/kg`, fat `25%` of calories,
  carbs = remainder. Calorie floor 1200.
- `test/.../GoalCalculatorTest.kt` — covers BMR (M/F), TDEE, maintain/lose/gain deltas, calorie
  floor, protein scaling, and macro→calorie reconstruction.

### 2. Preferences / goal persistence (DataStore)
- `data/UserPreferencesRepository.kt` — DataStore Preferences storing: `onboardingComplete` flag,
  the `DailyGoal` (calories + macros), units (metric/imperial), and exposing them as Flows.
- Provided as a singleton via Hilt.

### 3. Model state + download
- `data/ModelState.kt` — sealed: `NotDownloaded`, `Downloading(percent, bytesSoFar, totalBytes)`,
  `Ready(path)`, `Failed(reason)`.
- `data/ModelRepository.kt` — single source of truth for model status: checks the final model file
  on disk, observes the active WorkManager job's progress, exposes `state: Flow<ModelState>`, and
  `startDownload()` / `cancel()`.
- `download/ModelDownloadWorker.kt` — Hilt `CoroutineWorker`:
  - Reads `BuildConfig.MODEL_URL` / `MODEL_FILE_NAME`.
  - **Resumable**: downloads to `<files>/models/<name>.part`; on restart sends `Range: bytes=<have>-`
    and appends; honors `Content-Range`/`Content-Length` for total size.
  - Streams with OkHttp, reports progress via `setProgress` (percent + bytes), checks `isStopped`.
  - On completion atomically renames `.part` → final file; sets foreground notification.
  - Constraint: `NetworkType.UNMETERED` (Wi-Fi) — surfaced in UI; retried with backoff.
- `download/ModelDownloadManager.kt` — thin wrapper enqueuing the unique work
  (`KEEP`/`REPLACE`) and mapping `WorkInfo` → `ModelState`.

### 4. DI — `di/AppModule.kt`
Provides: `@ApplicationContext` helpers, `DataStore<Preferences>`, `OkHttpClient`, `WorkManager`,
and a `models/` dir path. Worker uses `@HiltWorker` + `@AssistedInject`.

### 5. UI
- `feature/onboarding/OnboardingViewModel.kt` — holds questionnaire state, computes a live
  `DailyGoal` preview via `GoalCalculator`, persists it + `onboardingComplete=true` on finish.
- `feature/onboarding/OnboardingScreen.kt` — multi-step questionnaire matching Stitch screen 02:
  progress bar + step, sex chips, age stepper, height/weight cards + unit toggle, activity list,
  goal pills, a live "your daily target" summary card, mango Continue button (candy pop shadow),
  Pip in the corner.
- `feature/onboarding/DownloadViewModel.kt` — exposes `ModelState`, triggers `startDownload()`.
- `feature/onboarding/DownloadScreen.kt` — matches Stitch screen 03: bouncing Pip, headline
  "Teaching your phone to see food…", thick mango→berry progress bar with `%` and `GB`, Wi-Fi info
  pill, background-friendly copy; auto-advances to Home on `Ready`.

### 6. First-run gating
- A start-destination decision (e.g. `AppStartViewModel` reading DataStore + `ModelRepository`):
  `!onboardingComplete → ONBOARDING`; else `model != Ready → DOWNLOAD`; else `HOME`.
- Wire into `NutriNavHost` `startDestination` (or a lightweight splash gate) so the bars/FAB only
  show once inside the app shell.

## Acceptance / verify
- `./gradlew test` — `GoalCalculatorTest` passes.
- `./gradlew :app:assembleDebug` — builds.
- Manual (device): questionnaire produces a sensible kcal goal + macros; download shows live
  progress, survives app backgrounding, resumes after kill, and lands on `Ready`; relaunch with a
  completed model skips straight to Home; relaunch mid-download resumes.

## Notes
- If `MODEL_URL` requires auth, the worker should accept a token (later) or use a self-hosted mirror.
- Keep all LiteRT/Gemma loading out of this phase — Phase 2 only fetches the file; Phase 4 loads it.
