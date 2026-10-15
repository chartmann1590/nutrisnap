# Phase 0 — Design ✅ Done

Goal: lock a distinctive, playful visual direction and design every screen before building.

## What was produced
1. **Hand-built HTML north star** — `design/style-tile.html`
   - Defines the "Snackable" aesthetic: mango+berry candy palette on warm cream, super-rounded
     chunky cards with soft warm shadows, Fredoka (display) + Nunito (body), and **Pip** the mango
     mascot drawn in SVG.
   - Shows palette swatches, type scale, core components, and 3 phone mockups (Home, scan result,
     model download). Rendered/verified with Playwright (`design/north-star.png`).
2. **Google Stitch project + design system** — project *NutriSnap — On-Device AI Calorie Tracker*
   - Design system **"Snackable"** (`assets/1506157177310453552`): primary `#FF9F1C`, secondary
     `#FF5D73`, tertiary `#2EC4A6`, neutral `#FFF6EC`, Quicksand headline, Nunito Sans body,
     roundness ROUND_TWELVE, FRUIT_SALAD variant, plus a full design-MD describing tone + components.
3. **9 Stitch screens** (all `status: COMPLETE`, screenshots in `design/stitch/`):
   - 01 Home dashboard · 02 Onboarding questionnaire · 03 Model download · 04 Camera capture ·
     05 AI scan result · 06 Manual/barcode entry · 07 Diary · 08 Trends · 09 Settings.

## Notes / lessons
- Stitch's **Pro/3.1 Pro** model timed out at the MCP layer; the **Flash** model
  (`GEMINI_3_FLASH`) generates reliably and synchronously — use Flash for further screens.
- Each screen was generated against the shared design system for consistency; Stitch auto-generated
  real food photography + a Pip mascot image for hero/empty contexts.
- Designs + HTML reference live in `design/`. Editable Stitch designs live in the user's Stitch account.

## How to regenerate / iterate a screen
```
mcp__stitch__generate_screen_from_text(
  projectId = "15309379711849515878",
  designSystem = "assets/1506157177310453552",
  modelId = "GEMINI_3_FLASH",
  deviceType = "MOBILE",
  prompt = "<screen description in Snackable terms>")
```
Download the returned `screenshot.downloadUrl` to `design/stitch/`.
