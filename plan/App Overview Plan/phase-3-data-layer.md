# Phase 3 — Data Layer ✅ Done

> Status: implemented & green. Room DB (meals + weight), DAOs, repositories
> (Meal/Weight/Goal), `StreakCalculator`, and `DataModule` (Hilt) are in place. All 42 unit tests
> pass, including the Robolectric `MealRepositoryTest` and `StreakCalculatorTest`. Build green.
> (`ksp { room.schemaLocation }` set; `testOptions` enables Robolectric resources.)

Goal: durable local persistence for meals, foods, and weight, plus streak logic — all behind clean
repository interfaces. No cloud.

## Room database — `data/db/`
- `AppDatabase.kt` — Room database (v1), exports schema, provided via Hilt.
- Entities:
  - `MealEntity` — id, timestamp (epoch millis), mealType (BREAKFAST/LUNCH/DINNER/SNACK),
    name, totalKcal, proteinG, carbsG, fatG, photoUri?, source (SCAN/MANUAL/BARCODE), confidence?.
  - `WeightEntryEntity` — id, dateEpochDay, weightKg.
  - (Optional) `FoodItemEntity` for multi-item meals; v1 may store one food per meal row and keep it
    simple — decide during implementation, migrate later if needed.
- DAOs:
  - `MealDao` — insert/update/delete; `observeForDay(epochDay)`, `observeRange(start,end)`,
    `observeDayTotals(epochDay)` (sum kcal + macros), `observeDistinctLoggedDays()` for streaks.
  - `WeightDao` — upsert by day, `observeLatest()`, `observeRange()`.
- Type converters for enums + dates.

## Repositories — `data/`
- `MealRepository` — wraps `MealDao`; exposes today's meals + totals as Flows; `log(meal)`,
  `delete(id)`, `mealsForDay(epochDay)`, `weekTotals()`.
- `WeightRepository` — log/observe weight; latest + trend.
- `GoalRepository` — reads the `DailyGoal` from `UserPreferencesRepository` (DataStore from Phase 2);
  combine with `MealRepository` day totals to compute "remaining" + ring progress.
- `StreakCalculator` (pure) — from the set of logged days, compute current + best streak; unit-tested.

## DI
- `di/DataModule.kt` — provide `AppDatabase`, DAOs, and repository singletons.

## Tests
- `MealRepositoryTest` / `WeightRepositoryTest` — in-memory Room (Robolectric or instrumented) for
  insert/observe/totals.
- `StreakCalculatorTest` — pure unit tests (consecutive days, gaps, today-included logic, best streak).

## Verify
- `./gradlew test` green; DAOs return expected aggregates; streak logic correct across edge cases.
