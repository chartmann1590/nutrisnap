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
    suspend fun send(userText: String): Result<String>
    fun close()
}
```

- `LiteRtGemmaEngine.startChat` creates and holds open one `litertlm.Conversation`
  (`eng.createConversation(ConversationConfig(systemInstruction = Contents.of(...)))`).
  `ChatSession.send` calls `conv.sendMessage(userText)` and returns
  `extractText(response)`; the open conversation provides in-session multi-turn
  memory natively. `close()` closes the conversation.
- The chat shares the single `Engine` instance and the existing `Mutex`, so a
  chat inference and a food analysis never run concurrently (acceptable: one
  model). `send` runs on `Dispatchers.Default` and is cancellable.
- `FakeGemmaEngine` (test) implements `startChat` returning a fake `ChatSession`
  whose `send` returns a canned/echo reply, so the ViewModel is testable without
  the model.
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
  - `send(text: String)`: appends the user message (persist + show), sets
    generating, calls `session.send`, appends Pip's reply (persist + show),
    clears generating. On failure appends a gentle Pip fallback message.
  - clears the `ChatSession` in `onCleared()`.
- `PipChatScreen`: a full-screen layout — a large animated `Pip(mood = ...)` at
  the top reacting as he talks, a scrolling list of message bubbles
  (user vs Pip styling), a "Pip is thinking…" typing indicator while generating,
  and a text input + send bar. Back button returns to the dashboard.
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
  appends the user message then a Pip reply; `isGenerating`/`pipMood` toggle
  around the call; a failing `send` appends the fallback message and leaves the
  UI usable.
- `ChatRepository`/DAO covered by an in-memory Room test (insert → observe →
  clear), following the existing `MealRepositoryTest`/Room test pattern.
- Chat UI and Pip animation verified on device (manual; synthetic taps cannot
  drive the app's interactive screens).

## Out of scope (v1)

- Pip taking actions (logging meals, setting reminders) — read-only chat only.
- Token streaming — v1 returns the full reply with a typing indicator;
  streaming is a clean later enhancement.
- Voice input/output.
- Summarizing/condensing long history beyond the last-6-messages excerpt.
