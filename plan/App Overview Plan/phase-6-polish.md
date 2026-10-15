# Phase 6 — Polish ⬜ Pending

Goal: take the working app from functional to delightful and shippable.

## Visual fidelity
- **Real fonts:** swap `DisplayFontFamily`/`BodyFontFamily` in `ui/theme/Type.kt` to the actual faces
  (Fredoka/Quicksand + Nunito) via downloadable Google Fonts (`ui-text-google-fonts` + GMS cert
  resource) or bundled `res/font` files.
- Match Stitch screens pixel-close: spacing, shadows, the candy "pop" button depth, food photography
  thumbnails (Coil) for meal rows where available.

## Motion & feel
- Bouncy micro-interactions: Pip reactions, ring fill animations, streak flame, confetti on hitting
  a goal / extending a streak. Button press springs. Shared-element-ish transitions scan→result.
- Haptics on key actions (log, shutter, goal reached).

## States & robustness
- Empty / loading / error states for every screen (with Pip). Offline behavior verified.
- Permission flows (camera, notifications) with friendly rationale.
- Edge cases: model missing/corrupt → re-download prompt; low storage; download interrupted.

## Accessibility
- Content descriptions, touch target sizes, dynamic type, color-contrast pass on candy palette,
  TalkBack sweep of core flows.

## Dark mode
- Verify the dark `ColorScheme` + `NutriColors` across all screens; tune tints.

## Performance
- Gemma 4: warm-load once, measure inference latency, avoid re-init; bitmap downscale before
  inference; release on memory pressure. Compose: stable keys in lists, avoid recomposition hotspots.
- Baseline profile (optional) for startup.

## Release readiness
- App icon polish, splash, versioning, R8/ProGuard keep rules (LiteRT native + serialization),
  `release` build smoke test, basic crash/ANR check.

## Verify
- End-to-end manual run on a physical device through onboarding → download → scan → log →
  trends, in both light and dark mode, online and offline. `./gradlew test` + `assembleRelease` clean.
