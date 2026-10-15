# Phase 4 — AI Engine (Gemma 4 / LiteRT-LM) 🚧 Scaffolded (stubbed)

> Status: interface + structure in place and compiling. `ai/` has `GemmaEngine`, `FoodEstimate`,
> `ResponseParser` (with a tested, fixed JSON extractor — handles fences/preamble/multi-line/nested),
> `FakeGemmaEngine` (used in DEBUG via `AiModule`), and `LiteRtGemmaEngine` (real impl, with the
> LiteRT-LM session/inference calls **stubbed**: they throw `NotImplementedError` until the AAR is
> linked). `ResponseParserTest` passes.
>
> **Remaining to finish Phase 4:** add the real LiteRT-LM dependency (AAR) to the catalog +
> `app/build.gradle.kts`, implement `createSession()` / `generateResponse()` in `LiteRtGemmaEngine`
> per `google-ai-edge/LiteRT-LM`, flip `AiModule` to use the real engine, and add the on-device
> instrumented test. ProGuard keep rules for `com.google.ai.edge.litert.**` are already in place.

Goal: on-device food recognition. Load the downloaded Gemma 4 `.litertlm` model with LiteRT-LM and
turn a meal photo (or text description) into a structured calorie + macro estimate.

## Interface — `ai/`
- `FoodEstimate.kt` — `@Serializable` result: `name`, `portionDescription`, `grams`, `kcal`,
  `proteinG`, `carbsG`, `fatG`, `confidence` (0–1), plus optional `items: List<FoodItem>`.
- `GemmaEngine.kt` (interface):
  - `suspend fun warmUp()` — load the model once (expensive); idempotent.
  - `suspend fun analyzeFood(image: Bitmap, hint: String?): Result<FoodEstimate>`
  - `suspend fun estimateFromText(description: String): Result<FoodEstimate>`
  - `fun isReady(): Boolean`
- UI/features depend only on this interface (swappable runtime).

## Implementation — `ai/LiteRtGemmaEngine.kt`
- Resolve model path from `ModelRepository` (Phase 2). Initialize the **LiteRT-LM** engine/session
  with vision enabled; reuse a single warm session (mutex-guarded) — init is heavy.
- Convert `Bitmap` → the runtime's image input; build a multimodal prompt (system + image + text).
- **Prompt strategy:** instruct Gemma 4 to return **strict JSON only** with the FoodEstimate fields;
  low temperature. Include the user hint if provided ("this is ~1 cup").
- **Parsing:** robust JSON extraction (strip code fences / preamble), tolerant deserialization;
  on parse failure or `confidence` below threshold → `Result.failure` so UI falls back to manual entry.
- Run inference on a background dispatcher; surface partial/streamed status if practical.
- Memory: release/keep policy for the session; handle low-memory by recreating on demand.

## DI — `di/AiModule.kt`
- Bind `GemmaEngine` → `LiteRtGemmaEngine` (singleton). Provide any LiteRT-LM objects.
- Add the LiteRT-LM dependency/AAR to the version catalog + `app/build.gradle.kts` (per current
  `google-ai-edge/LiteRT-LM` Android setup). Add ProGuard keep rules for native classes.

## Tests
- Unit-test the **JSON parser** with good/garbled/fenced/low-confidence samples (no model needed).
- **Instrumented** test on a physical device: `warmUp()` then `analyzeFood(sampleFoodBitmap)` returns
  a plausible estimate within a timeout. (Skipped on emulator/CI — model + RAM required.)

## Verify
- Parser unit tests green. On device: a real meal photo yields name + kcal + macros in a few seconds,
  fully offline (airplane mode). Graceful fallback path exercised with a non-food image.

## Notes / risks
- First-load latency + RAM pressure: warm-load once, E2B default, show confidence, always editable.
- LiteRT-LM API surface evolves — follow the official Android sample for session/config calls.
