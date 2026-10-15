# Phase 1 — Project + Theme + Navigation ✅ Done

Goal: a buildable, navigable Compose app shell with the Snackable theme and reusable components.

## Outcome
- **`./gradlew :app:assembleDebug` succeeds** (exit 0). App runs and navigates between all screens.
- Package **`com.charles.nutrisnap`** (namespace + applicationId + all sources).

## Build configuration
- `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, `local.properties` (SDK dir),
  Gradle wrapper 8.5.
- Version catalog: `gradle/libs.versions.toml` — AGP 8.3.2, Kotlin 1.9.23, KSP 1.9.23-1.0.20,
  Compose BOM 2024.05.00 (compiler ext 1.5.11), Hilt 2.51.1, Room 2.6.1, DataStore 1.1.1,
  WorkManager 2.9.0, CameraX 1.3.3, ML Kit barcode 17.2.0, OkHttp 4.12.0, Coil 2.6.0,
  kotlinx-serialization 1.6.3, coroutines 1.8.0; test: JUnit, Turbine, MockK, coroutines-test.
- `app/build.gradle.kts`: compileSdk 34, minSdk 26, JDK 17, Compose enabled, `buildConfig = true`
  with `MODEL_URL`, `MODEL_FILE_NAME`, `MODEL_SIZE_BYTES` fields.

## App manifest & resources
- `AndroidManifest.xml`: INTERNET / NETWORK_STATE / CAMERA / POST_NOTIFICATIONS perms; camera feature
  optional; `NutriSnapApp` (Hilt + WorkManager Configuration.Provider); `MainActivity` launcher;
  WorkManager default initializer removed (Hilt provides the factory).
- Adaptive launcher icon (mango bg + camera/leaf foreground): `res/mipmap-anydpi-v26/*`,
  `res/drawable/ic_launcher_foreground.xml`, `res/values/ic_launcher_background.xml`.
- `res/values/strings.xml`, `res/values/themes.xml` (transparent status bar, cream nav bar).

## Theme (Snackable → Compose) — `ui/theme/`
- `Color.kt` — palette constants (Cream, Mango, Berry, Mint, Grape, Butter, Sky, tints…).
- `Type.kt` — Material `Typography`; `DisplayFontFamily`/`BodyFontFamily` constants (currently
  `FontFamily.Default`; swap to real fonts in Phase 6).
- `Shape.kt` — very rounded Material `Shapes` (12–34dp).
- `Theme.kt` — `NutriSnapTheme`, light/dark `ColorScheme`, plus `NutriColors` (macro hues, pop shadow,
  ring track, tints) exposed via `NutriTheme.colors` / `LocalNutriColors`.

## Reusable components — `ui/components/`
- `Buttons.kt` — `PrimaryButton` (candy offset "pop" shadow, presses into shadow), `SecondaryButton`.
- `NutriCard.kt` — soft rounded surface container.
- `Rings.kt` — `CalorieRing` (animated thick arc + centered value), `MacroBar` (animated macro bar).
- `Pip.kt` — the mascot drawn with Canvas (body, leaf, eyes, cheeks, smile), gentle bob animation.
- `Pills.kt` — `NutriPill`, `StreakPill`, `ConfidencePill`, `InfoPill`.
- `Placeholder.kt` — `ComingSoon` scaffold with Pip for not-yet-built screens.

## Navigation — `ui/nav/` + `ui/NutriSnapApp.kt`
- `Destinations.kt` — `Routes` (onboarding, download, home, diary, trends, profile, scan,
  scan_result, entry, settings) + `TopLevelTab` enum (Home/Diary/Trends/Me + icons).
- `NutriNavHost.kt` — NavHost wiring every screen; start = HOME (gating added in Phase 2).
- `NutriSnapApp.kt` — root scaffold: `NavigationBar` (4 tabs with a center gap) + floating
  berry→mango **camera FAB**, shown only on top-level tabs.

## Screens (Phase 1 state)
- `feature/dashboard/DashboardScreen.kt` — real, styled Home with mock data (calorie ring, macro
  bars, today's meals). Showcases the design system.
- All others (`onboarding`, `scan`, `entry`, `history`, `profile`, `settings`) are on-brand
  `ComingSoon` placeholders, replaced in later phases.

## Verify
```
./gradlew :app:assembleDebug   # BUILD SUCCESSFUL
```
