# Pip Reactive Mascot & Motion Polish — Design

**Date:** 2026-06-27
**Status:** Approved

## Goal

Make Pip (the NutriSnap mango mascot) feel alive and fun: a reactive character
that responds to the user's real day, plus richer app-wide motion. All native
Jetpack Compose (Canvas + animation APIs). No new dependencies, no Lottie.

## Scope

### 1. Reactive mascot engine (`Pip.kt`)

Introduce a `PipMood` enum:

| Mood | Trigger | Expression / motion |
|------|---------|---------------------|
| `Content` (default) | On track, day in progress | Gentle bob + occasional blink |
| `Sleepy` | Nothing logged today / empty states | Droopy eyes, slow yawn |
| `Celebrate` | Meal logged, goal hit, streak extended | Jump + squash-stretch + confetti |
| `Proud` | Active streak (2+ days) | Sparkle/glow, small flame nearby |
| `Stuffed` | Over calorie goal | Rounder body, woozy sway (gentle, never shaming) |
| `Thinking` | During AI photo analysis | Looks up, "hmm" — replaces generic spinner |

`Pip(mood: PipMood, ...)` becomes a small state machine that draws per-mood eyes,
mouth, and posture on the existing Canvas, animating transitions between moods.

Always-on behaviors regardless of mood:
- **Blink** every ~4s (eyelids close briefly).
- **Squash-and-stretch** physics on the bounce (not just vertical translation).
- **Tap-to-poke**: tapping Pip triggers a one-shot giggle/wobble.

### 2. Dashboard home placement

- Pip gets a permanent spot on the Home screen, beside/near the calorie ring.
- A pure function `pipMoodFor(state: DashboardUiState): PipMood` maps live data to
  a mood. Pure and unit-tested. Priority order resolves conflicts (e.g. Celebrate
  beats Proud beats Stuffed beats Content; Sleepy when no meals logged today).
- Empty state, loading state, and scan/analysis screens reuse the same `Pip`
  component with explicit moods (`Sleepy`, `Thinking`).

### 3. App-wide motion polish

- Button press squish (scale-down on press) on primary buttons.
- `ConfettiBurst` wired to the `Celebrate` trigger on the dashboard.
- Livelier `StreakFlame` when a streak is active.
- Smooth screen transitions in the nav host (enter/exit fades/slides).

## Components & boundaries

- **`PipMood`** — enum, no logic. Defines the vocabulary of moods.
- **`pipMoodFor(state)`** — pure mapping from dashboard state → mood. Testable in
  isolation, no Compose dependency.
- **`Pip(mood, modifier, size)`** — the visual/animation state machine. Takes a
  mood, owns all drawing and transition animation. Knows nothing about app data.
- Existing `ConfettiBurst`, `StreakFlame`, `AnimatedRing` in `Motion.kt` are reused.

This keeps data→mood logic (pure, tested) separate from mood→pixels (Compose).

## Testing

- Unit tests for `pipMoodFor`: each trigger condition resolves to the expected
  mood, including priority/conflict cases (over goal + streak, etc.).
- Visual/animation behavior verified by running the app (manual / screenshot).

## Out of scope

- New external dependencies or Lottie assets.
- Redesigning Pip's core look (still the mango with leaf, eyes, cheeks).
- Sound effects.
- Pip on screens other than Home, empty/loading, and scan.
