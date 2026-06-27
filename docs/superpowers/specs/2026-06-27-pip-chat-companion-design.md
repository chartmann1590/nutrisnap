# Talk to Pip — On-Device Chat Companion — Design

**Date:** 2026-06-27
**Status:** Approved

## Goal

Let the user tap Pip (the mascot) to open a full-screen chat and have a
conversation with him about their tracking, powered by the existing on-device
Gemma model. Pip can *see* the user's nutrition data (read-only) and respond in
his cute, encouraging voice. Conversation history persists locally so Pip feels
like he remembers you. No new model, no new external dependency.

## Scope

### 1. Chat engine — multi-turn session on top of `GemmaEngine`

Today `GemmaEngine` (`ai/GemmaEngine.kt`, impl `ai/LiteRtGemmaEngine.kt`) only
does one-shot conversations (`analyzeFood`, `estimateFromText`) that create a
`litertlm.Conversation`, send one message, and close it. Add a multi-turn API:

```kotlin
interface GemmaEngine {
    // existing members unchanged
    suspend fun startChat(systemInstruction: String): ChatSession
}

interface ChatSession {
    // Emits the cumulative reply text as it streams (each emission = full text so far).
    // Normal completion ends the flow; failures propagate as exceptions.
    fun sendStreaming(userText: String): Flow<String>
    fun close()
}
```

- `LiteRtGemmaEngine.startChat` creates and holds open one `litertlm.Conversation`
  (`eng.createConversation(ConversationConfig(systemInstruction = Contents.of(...)))`).
  The open conversation provides in-session multi-turn memory natively.
  `close()` calls `conv.cancelProcess()` then `conv.close()`.
- **Streaming:** `ChatSession.sendStreaming` uses the library's streaming flow
  `conv.sendMessageAsync(userText): Flow<com.google.ai.edge.litertlm.Message>`,
  accumulating each emitted chunk's text into a running buffer and emitting the
  cumulative string. The whole stream is wrapped so the existing `Mutex` is held
  for its duration (acquire when collection starts, release on completion/cancel),
  and run with `flowOn(Dispatchers.Default)`. Cancelling the collector cancels
  generation (`cancelProcess()` on close).
  - Library emit semantics (delta vs cumulative chunks) are confirmed by device
    test. The implementation accumulates deltas (append); if a chunk turns out to
    be cumulative, switch accumulation to "use latest" — a one-line change. The
    ViewModel and its tests are unaffected because the fake controls emissions.
- The chat shares the single `Engine` instance and `Mutex`, so a chat stream and
  a food analysis never run concurrently (acceptable: one model).
- `FakeGemmaEngine` (test) implements `startChat` returning a fake `ChatSession`
  whose `sendStreaming` emits a deterministic sequence of growing strings (e.g.
  "Hey", "Hey there", "Hey there!") then completes — so the ViewModel's streaming
  and persistence logic is testable without the model. A configurable failure
  mode lets a test make the flow throw.
- If the model is not ready (`ModelRepository.isReady() == false`), `startChat`
  fails; the ViewModel surfaces a friendly Pip message rather than an error.

### 2. Grounding — `PipContextBuilder` (pure) + `PipPersona`

- `PipPersona`: a constant system-preamble string defining Pip's voice and rules
  — cute/playful/encouraging, concise (1–3 sentences), stays on nutrition &
  tracking, never gives medical/diagnostic advice, no calorie shaming.
- `PipContextBuilder.build(...)`: a **pure** function (no Compose, no IO) that
  takes a snapshot data class of the user's current tracking and returns a
  compact human-readable context string. Inputs:
  - today's totals (kcal + protein/carbs/fat) and remaining vs goal,
  - daily goal (calories + macro targets),
  - current streak (days),
  - up to the 3 most recent meals (name + kcal),
  - latest weight entry and a simple trend vs the prior entry (up/down/flat), if
    weight data exists.
  Output is kept compact (a handful of short lines) to fit the on-device context
  window. Absent data (e.g. no weight logged) is omitted, not faked.
- The conversation's `systemInstruction` = `PipPersona` + the context string +
  a short "recent conversation" excerpt (see §3).

### 3. Persistent history — `ChatRepository` + Room

- New Room entity `ChatMessageEntity`: `id` (autoGenerate), `role`
  (enum `ChatRole { USER, PIP }`), `text` (String), `timestampMs` (Long).
- New DAO `ChatDao`: `insert(message)`, `observeRecent(limit)` returning a
  `Flow<List<ChatMessageEntity>>` ordered by time, `recent(limit)` (suspend
  one-shot for building the system-instruction excerpt), `clearAll()`.
- `ChatRepository` wraps the DAO and exposes: observe history, append a message,
  fetch the last N messages, clear history.
- `AppDatabase` version bumped; a migration adds the `chat_message` table.
- On screen open: full history loads into the displayed message list; the **last
  6 messages** are folded into the system instruction as a "Recent conversation:"
  excerpt so Pip has cross-session continuity without replaying everything into
  the limited context window.

### 4. UI & navigation — `feature/pip/`

- `PipChatViewModel`: on init, gathers the tracking snapshot from the existing
  repositories (`GoalRepository`, `MealRepository`, `WeightRepository`,
  `UserPreferencesRepository`, `StreakCalculator`), builds the system instruction
  (persona + context + recent-history excerpt), calls `startChat`, and loads
  persisted history. Exposes:
  - `messages: StateFlow<List<ChatMessageEntity>>`
  - `isGenerating: StateFlow<Boolean>`
  - `pipMood: StateFlow<PipMood>` (`Thinking` while generating, else `Content`)
  - `streamingText: StateFlow<String?>` — the in-progress Pip reply as it
    streams (null when not generating).
  - `send(text: String)`: appends + persists the user message, sets generating,
    collects `session.sendStreaming(text)` updating `streamingText` on each
    emission; on completion appends + persists Pip's final reply and clears
    `streamingText`/generating. On failure appends a gentle Pip fallback message.
  - clears the `ChatSession` in `onCleared()` (cancels any in-flight stream).
- `PipChatScreen`: a full-screen layout — a large animated `Pip(mood = ...)` at
  the top reacting as he talks, a scrolling list of message bubbles
  (user vs Pip styling), a "Pip is thinking…" typing indicator shown until the
  first token then replaced by the live-streaming text bubble, and a text input +
  send bar. Back button returns to the dashboard.
- Navigation: add `Routes.PIP_CHAT` and a `composable(Routes.PIP_CHAT)` in
  `NutriNavHost`. The dashboard header `Pip` gets an `onPoke`/tap that navigates
  to `Routes.PIP_CHAT` (replacing the current poke-only behavior on the
  dashboard; the poke wobble itself stays available elsewhere).

## Components & boundaries

- **`ChatSession` / `GemmaEngine.startChat`** — multi-turn inference. Knows
  nothing about app data or UI.
- **`PipPersona`** — static voice/rules text. No logic.
- **`PipContextBuilder`** — pure data→context-string mapping. Unit-tested.
- **`ChatRepository` + `ChatDao` + `ChatMessageEntity`** — persistence. No UI, no
  model.
- **`PipChatViewModel`** — orchestrates: snapshot → system instruction →
  session; owns message/generating/mood state. Tested against the fake engine.
- **`PipChatScreen`** — rendering only.

Data→context logic (pure, tested) and persistence are kept separate from
model I/O and from Compose.

## Testing

- Unit tests for `PipContextBuilder`: produces expected compact lines for a
  representative snapshot; omits absent sections (no weight, no meals); reflects
  over-goal vs under-goal correctly.
- Unit tests for `PipChatViewModel` against `FakeGemmaEngine`: sending a message
  appends the user message; `streamingText` reflects the growing chunks during
  generation; on completion the final Pip reply is appended and persisted and
  `streamingText` is cleared; `isGenerating`/`pipMood` toggle around the call; a
  failing stream appends the fallback message and leaves the UI usable.
- `ChatRepository`/DAO covered by an in-memory Room test (insert → observe →
  clear), following the existing `MealRepositoryTest`/Room test pattern.
- Chat UI and Pip animation verified on device (manual; synthetic taps cannot
  drive the app's interactive screens).

## Out of scope (v1)

- Pip taking actions (logging meals, setting reminders) — read-only chat only.
- Voice input/output.
- Summarizing/condensing long history beyond the last-6-messages excerpt.
