# Phase 5 — Feature Wiring ⬜ Pending

Goal: connect the screens (Phase 0/1 designs) to real data (Phase 3) and the AI engine (Phase 4),
replacing all placeholders with working features.

## Dashboard — `feature/dashboard/`
- `DashboardViewModel` — combine `GoalRepository` + `MealRepository` day totals → calorie ring
  progress, "remaining", macro bars, today's meals, streak (from `StreakCalculator`).
- Replace mock data in `DashboardScreen`; tap a meal → detail/edit; empty state with Pip.

## Scan — `feature/scan/`
- `ScanScreen` — CameraX preview (matches Stitch screen 04): viewfinder brackets, shutter, gallery
  picker, flash toggle, permission handling. Capture → `Bitmap`.
- `ScanViewModel` — call `GemmaEngine.analyzeFood`; states: capturing → analyzing (Pip/spinner) →
  result | fallback.
- `ScanResultScreen` — editable result sheet (matches Stitch 05): food name, confidence pill, big
  editable kcal, macro tiles, portion; "Add to diary" → `MealRepository.log()` → back to Home.
  On failure/low confidence → route to manual entry with the photo attached.

## Manual & barcode entry — `feature/entry/`
- `EntryScreen` (matches Stitch 06): Describe/Barcode segmented toggle.
  - Describe → `GemmaEngine.estimateFromText` → editable estimate rows → log.
  - Barcode → ML Kit Barcode scanning via CameraX → look up / fill product → estimate → log.
- Recent foods list for quick re-add.

## History — `feature/history/`
- `DiaryScreen` (Stitch 07): grouped by meal with section totals, day progress bar, add-food rows,
  day switcher; backed by `MealRepository`.
- `TrendsScreen` (Stitch 08): weekly calorie bar chart (Compose Canvas), streak + weight stat cards,
  weight logging row → `WeightRepository`. Monthly toggle optional.

## Settings & profile — `feature/settings/`, `feature/profile/`
- `SettingsScreen` (Stitch 09): On-device AI card (Gemma 4 E2B active, E2B↔E4B segmented control →
  re-download via `ModelRepository`), daily goal (re-run `GoalCalculator`), units toggle, dark mode,
  privacy note. `ProfileScreen` summarizes goal + links to settings.

## Cross-cutting
- ViewModels via Hilt + `hilt-navigation-compose`; UI state as `StateFlow`; one-shot events via
  channels. Pass IDs/args through nav routes (e.g. scan result, edit meal).

## Tests
- ViewModel unit tests (Turbine) for dashboard aggregation + scan state machine.
- Compose UI tests: dashboard renders totals; onboarding→home happy path; log-a-meal updates ring.

## Verify
- Full loop on device: scan/manual/barcode → editable result → log → dashboard + diary + trends +
  streak all update; settings can switch model variant.
