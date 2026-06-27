# Talk to Pip — Chat Companion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tap Pip to open a full-screen chat where he talks about the user's tracking, powered by the existing on-device Gemma model, with streamed replies and locally persisted history.

**Architecture:** A multi-turn `ChatSession` is added on top of the existing `GemmaEngine`, using litertlm's streaming `sendMessageAsync(text): Flow<Message>`. A pure `PipContextBuilder` turns a tracking snapshot into a context string that, with a `PipPersona` preamble and a short recent-history excerpt, becomes the conversation's system instruction. Chat history persists in a new Room table. A `PipChatViewModel` orchestrates snapshot → session → streamed reply → persistence; `PipChatScreen` renders it with a live-reacting Pip.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, kotlinx.coroutines Flow, litertlm (`com.google.ai.edge.litertlm` 0.13.1), JUnit4 + Robolectric + Turbine.

## Global Constraints

- No new external dependencies. Reuse litertlm, Room, Hilt, Compose already present.
- On-device model only; chat shares the single `litertlm.Engine` instance and the existing `Mutex` in `LiteRtGemmaEngine` (a chat stream holds the mutex for its duration).
- Streaming contract: `ChatSession.sendStreaming(userText): Flow<String>` emits the **cumulative** reply text (each emission = full text so far); normal completion ends the flow; failures propagate as exceptions.
- Pip's voice: cute/playful/encouraging, concise (1–3 sentences), stays on nutrition & tracking, **never** medical/diagnostic advice, no calorie shaming. Defined once in `PipPersona`.
- Read-only: Pip never logs meals or mutates data in v1.
- Pure logic (`PipContextBuilder`) has no Compose/IO and is unit-tested. Persistence and model I/O are kept separate from it.
- DB migration must preserve existing user data (meals, weight) — add a real `Migration(1,2)`, do not rely on destructive fallback.
- Reuse `PipMood` (`com.charles.nutrisnap.ui.components.PipMood`): `Thinking` while generating, `Content` otherwise.
- Commands (Windows, run from repo root):
  - Unit tests: `./gradlew testDebugUnitTest`
  - One class: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.<FQN>"`
  - Compile main: `./gradlew compileDebugKotlin`
  - Compile tests: `./gradlew compileDebugUnitTestKotlin`

---

### Task 1: Multi-turn streaming `ChatSession` on `GemmaEngine`

Adds the chat API to the engine interface, a streaming LiteRT implementation, and
a deterministic fake for tests. Verified by compiling main + tests (the fake is
exercised by Task 4's ViewModel tests).

**Files:**
- Modify: `app/src/main/java/com/charles/nutrisnap/ai/GemmaEngine.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/ai/LiteRtGemmaEngine.kt`
- Modify: `app/src/test/java/com/charles/nutrisnap/ai/FakeGemmaEngine.kt`

**Interfaces:**
- Produces:
  - `interface ChatSession { fun sendStreaming(userText: String): Flow<String>; fun close() }`
  - `suspend fun GemmaEngine.startChat(systemInstruction: String): ChatSession`
  - `FakeGemmaEngine.chatShouldFail: Boolean` (test knob)

- [ ] **Step 1: Add the chat API to the engine interface**

Replace the contents of `app/src/main/java/com/charles/nutrisnap/ai/GemmaEngine.kt`:

```kotlin
package com.charles.nutrisnap.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface GemmaEngine {
    suspend fun warmUp()
    suspend fun analyzeFood(image: Bitmap, hint: String? = null): Result<FoodEstimate>
    suspend fun estimateFromText(description: String): Result<FoodEstimate>
    fun isReady(): Boolean

    /** Start a multi-turn chat conversation seeded with [systemInstruction]. */
    suspend fun startChat(systemInstruction: String): ChatSession
}

/** A live multi-turn chat conversation backed by the on-device model. */
interface ChatSession {
    /**
     * Stream a reply to [userText]. Emits the cumulative reply text (each emission
     * is the full text so far). Completes when generation finishes; throws on error.
     */
    fun sendStreaming(userText: String): Flow<String>

    /** Cancel any in-flight generation and release the conversation. */
    fun close()
}
```

- [ ] **Step 2: Implement streaming in `LiteRtGemmaEngine`**

In `app/src/main/java/com/charles/nutrisnap/ai/LiteRtGemmaEngine.kt`, add these imports:

```kotlin
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
```

Add `startChat` as a member of the class (e.g. after `verifyModel`):

```kotlin
    override suspend fun startChat(systemInstruction: String): ChatSession {
        val eng = ensureEngine()
        val conv = eng.createConversation(
            ConversationConfig(systemInstruction = Contents.of(systemInstruction))
        )
        return LiteRtChatSession(conv, mutex)
    }
```

Add the session implementation as a private nested class inside `LiteRtGemmaEngine`:

```kotlin
    private class LiteRtChatSession(
        private val conv: Conversation,
        private val mutex: kotlinx.coroutines.sync.Mutex,
    ) : ChatSession {

        override fun sendStreaming(userText: String): Flow<String> = flow {
            mutex.withLock {
                val sb = StringBuilder()
                conv.sendMessageAsync(userText).collect { message ->
                    val delta = message.contents.contents.joinToString(separator = "") { content ->
                        when (content) {
                            is Content.Text -> content.text
                            else -> ""
                        }
                    }
                    sb.append(delta)
                    emit(sb.toString())
                }
            }
        }.flowOn(Dispatchers.Default)

        override fun close() {
            runCatching { conv.cancelProcess() }
            runCatching { conv.close() }
        }
    }
```

Note on emit semantics: litertlm's `sendMessageAsync` Flow emits incremental chunks,
so we append each chunk's text. If on-device testing shows chunks are already
cumulative (text appears duplicated), change `sb.append(delta)` + `emit(sb.toString())`
to `emit(delta)` — a one-line change. This does not affect the ViewModel or tests.

- [ ] **Step 3: Add `startChat` to the test `FakeGemmaEngine`**

In `app/src/test/java/com/charles/nutrisnap/ai/FakeGemmaEngine.kt`, add imports:

```kotlin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
```

Add a public knob and the `startChat` implementation to the class body:

```kotlin
    /** When true, chat streams fail (for testing the ViewModel's error path). */
    var chatShouldFail: Boolean = false

    override suspend fun startChat(systemInstruction: String): ChatSession {
        ready = true
        val fail = chatShouldFail
        return object : ChatSession {
            override fun sendStreaming(userText: String): Flow<String> =
                if (fail) flow { throw RuntimeException("chat failure") }
                else flowOf("Hey", "Hey there", "Hey there!")
            override fun close() {}
        }
    }
```

- [ ] **Step 4: Compile main and tests**

Run: `./gradlew compileDebugKotlin compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL. (Adding the interface method forces both the real and
fake engines to implement it; this confirms both do.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/ai/GemmaEngine.kt \
        app/src/main/java/com/charles/nutrisnap/ai/LiteRtGemmaEngine.kt \
        app/src/test/java/com/charles/nutrisnap/ai/FakeGemmaEngine.kt
git commit -m "feat: add streaming multi-turn ChatSession to GemmaEngine"
```

---

### Task 2: Chat history persistence (Room)

Adds the `chat_message` table, DAO, repository, and a data-preserving DB migration.

**Files:**
- Create: `app/src/main/java/com/charles/nutrisnap/data/db/ChatMessageEntity.kt`
- Create: `app/src/main/java/com/charles/nutrisnap/data/db/ChatDao.kt`
- Create: `app/src/main/java/com/charles/nutrisnap/data/ChatRepository.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/data/db/Converters.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/data/db/AppDatabase.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/di/DataModule.kt`
- Test: `app/src/test/java/com/charles/nutrisnap/data/ChatRepositoryTest.kt`

**Interfaces:**
- Consumes: existing `AppDatabase`, `Converters`, Room setup in `DataModule`.
- Produces:
  - `enum class ChatRole { USER, PIP }`
  - `data class ChatMessageEntity(id: Long = 0, role: ChatRole, text: String, timestampMs: Long)`
  - `interface ChatDao { ... }`
  - `class ChatRepository`: `fun observeHistory(): Flow<List<ChatMessageEntity>>`,
    `suspend fun append(role: ChatRole, text: String): Long`,
    `suspend fun recent(limit: Int): List<ChatMessageEntity>`, `suspend fun clear()`
  - `AppDatabase.chatDao(): ChatDao`, `AppDatabase.MIGRATION_1_2`

- [ ] **Step 1: Create the entity + role enum**

Create `app/src/main/java/com/charles/nutrisnap/data/db/ChatMessageEntity.kt`:

```kotlin
package com.charles.nutrisnap.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ChatRole { USER, PIP }

@Entity(
    tableName = "chat_message",
    indices = [Index(value = ["timestampMs"])],
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: ChatRole,
    val text: String,
    val timestampMs: Long,
)
```

- [ ] **Step 2: Add the `ChatRole` type converter**

In `app/src/main/java/com/charles/nutrisnap/data/db/Converters.kt`, add inside the
`Converters` class (after the existing converters):

```kotlin
    @TypeConverter
    fun fromChatRole(value: ChatRole): String = value.name

    @TypeConverter
    fun toChatRole(value: String): ChatRole = ChatRole.valueOf(value)
```

- [ ] **Step 3: Create the DAO**

Create `app/src/main/java/com/charles/nutrisnap/data/db/ChatDao.kt`:

```kotlin
package com.charles.nutrisnap.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_message ORDER BY timestampMs ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_message ORDER BY timestampMs DESC, id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ChatMessageEntity>

    @Query("DELETE FROM chat_message")
    suspend fun clearAll()
}
```

- [ ] **Step 4: Create the repository**

Create `app/src/main/java/com/charles/nutrisnap/data/ChatRepository.kt`:

```kotlin
package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.db.ChatDao
import com.charles.nutrisnap.data.db.ChatMessageEntity
import com.charles.nutrisnap.data.db.ChatRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
) {
    fun observeHistory(): Flow<List<ChatMessageEntity>> = chatDao.observeAll()

    suspend fun append(role: ChatRole, text: String): Long =
        chatDao.insert(
            ChatMessageEntity(role = role, text = text, timestampMs = System.currentTimeMillis())
        )

    /** Most recent [limit] messages, oldest-first (suitable for a prompt excerpt). */
    suspend fun recent(limit: Int): List<ChatMessageEntity> =
        chatDao.recent(limit).reversed()

    suspend fun clear() = chatDao.clearAll()
}
```

- [ ] **Step 5: Register the entity + DAO and bump the DB version (no migration yet)**

In `app/src/main/java/com/charles/nutrisnap/data/db/AppDatabase.kt`, update the
`@Database` annotation and add the dao accessor:

```kotlin
@Database(
    entities = [MealEntity::class, WeightEntryEntity::class, ChatMessageEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun weightDao(): WeightDao
    abstract fun chatDao(): ChatDao
}
```

- [ ] **Step 6: Build once to generate the v2 schema, then read the exact table SQL**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL, and Room writes `app/schemas/com.charles.nutrisnap.data.db.AppDatabase/2.json`.

Open that `2.json`, find the entity whose `tableName` is `chat_message`, and copy its
`createSql` value (it looks like the line below, with `${TABLE_NAME}` replaced by
`chat_message`). For this entity it will be:

```
CREATE TABLE IF NOT EXISTS `chat_message` (`id` INTEGER NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, `timestampMs` INTEGER NOT NULL, PRIMARY KEY(`id`))
```

Also note the index Room generates for `timestampMs` (in `2.json` under the entity's
`indices`, its `createSql`), which will be:

```
CREATE INDEX IF NOT EXISTS `index_chat_message_timestampMs` ON `chat_message` (`timestampMs`)
```

Use these EXACT strings in the next step. (If `2.json` differs from the above, use
what `2.json` says — it is the source of truth Room validates against.)

- [ ] **Step 7: Add the data-preserving migration and register it**

In `app/src/main/java/com/charles/nutrisnap/data/db/AppDatabase.kt`, add imports and a
companion object exposing the migration:

```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
```

```kotlin
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_message` (`id` INTEGER NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, `timestampMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_chat_message_timestampMs` ON `chat_message` (`timestampMs`)"
                )
            }
        }
    }
```

In `app/src/main/java/com/charles/nutrisnap/di/DataModule.kt`, register the migration
(keep the destructive fallback as a last resort) and provide the DAO. Update
`provideAppDatabase` and add `provideChatDao`:

```kotlin
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "nutrisnap.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
```

Add the import `import com.charles.nutrisnap.data.db.ChatDao` to `DataModule.kt`.

- [ ] **Step 8: Write the failing repository test**

Create `app/src/test/java/com/charles/nutrisnap/data/ChatRepositoryTest.kt`:

```kotlin
package com.charles.nutrisnap.data

import androidx.room.Room
import app.cash.turbine.test
import com.charles.nutrisnap.data.db.AppDatabase
import com.charles.nutrisnap.data.db.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ChatRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = ChatRepository(db.chatDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `append then observe returns messages oldest first`() = runTest {
        repo.append(ChatRole.USER, "hi")
        repo.append(ChatRole.PIP, "hey!")

        repo.observeHistory().test {
            val list = awaitItem()
            assertEquals(2, list.size)
            assertEquals(ChatRole.USER, list[0].role)
            assertEquals("hi", list[0].text)
            assertEquals(ChatRole.PIP, list[1].role)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recent returns last N oldest-first`() = runTest {
        repo.append(ChatRole.USER, "m1")
        repo.append(ChatRole.PIP, "m2")
        repo.append(ChatRole.USER, "m3")

        val recent = repo.recent(2)
        assertEquals(2, recent.size)
        assertEquals("m2", recent[0].text)
        assertEquals("m3", recent[1].text)
    }

    @Test
    fun `clear removes all messages`() = runTest {
        repo.append(ChatRole.USER, "hi")
        repo.clear()
        repo.observeHistory().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 9: Run the test to verify it fails, then passes**

Run: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.data.ChatRepositoryTest"`
Expected: PASS (3 tests). If it fails to compile first, fix the referenced names to
match Steps 1–7, then re-run until green.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/data/db/ChatMessageEntity.kt \
        app/src/main/java/com/charles/nutrisnap/data/db/ChatDao.kt \
        app/src/main/java/com/charles/nutrisnap/data/ChatRepository.kt \
        app/src/main/java/com/charles/nutrisnap/data/db/Converters.kt \
        app/src/main/java/com/charles/nutrisnap/data/db/AppDatabase.kt \
        app/src/main/java/com/charles/nutrisnap/di/DataModule.kt \
        app/src/test/java/com/charles/nutrisnap/data/ChatRepositoryTest.kt \
        app/schemas
git commit -m "feat: persist Pip chat history with Room (chat_message table + migration)"
```

---

### Task 3: `PipPersona` + pure `PipContextBuilder`

The voice preamble and the pure data→context-string mapping. Strictly TDD.

**Files:**
- Create: `app/src/main/java/com/charles/nutrisnap/feature/pip/PipContext.kt`
- Test: `app/src/test/java/com/charles/nutrisnap/feature/pip/PipContextBuilderTest.kt`

**Interfaces:**
- Produces:
  - `data class PipMealBrief(val name: String, val kcal: Int)`
  - `enum class WeightTrend { UP, DOWN, FLAT, NONE }`
  - `data class PipTrackingSnapshot(goalKcal: Int?, eatenKcal: Int, goalProteinG: Int, eatenProteinG: Int, goalCarbsG: Int, eatenCarbsG: Int, goalFatG: Int, eatenFatG: Int, streakDays: Int, recentMeals: List<PipMealBrief>, latestWeightKg: Double?, weightTrend: WeightTrend)`
  - `object PipPersona { const val SYSTEM: String }`
  - `object PipContextBuilder { fun build(snapshot: PipTrackingSnapshot): String }`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/charles/nutrisnap/feature/pip/PipContextBuilderTest.kt`:

```kotlin
package com.charles.nutrisnap.feature.pip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipContextBuilderTest {

    private fun snapshot(
        goalKcal: Int? = 2000,
        eatenKcal: Int = 900,
        streakDays: Int = 3,
        recentMeals: List<PipMealBrief> = listOf(PipMealBrief("Pizza", 450)),
        latestWeightKg: Double? = 75.0,
        weightTrend: WeightTrend = WeightTrend.DOWN,
    ) = PipTrackingSnapshot(
        goalKcal = goalKcal, eatenKcal = eatenKcal,
        goalProteinG = 133, eatenProteinG = 30,
        goalCarbsG = 361, eatenCarbsG = 90,
        goalFatG = 73, eatenFatG = 40,
        streakDays = streakDays, recentMeals = recentMeals,
        latestWeightKg = latestWeightKg, weightTrend = weightTrend,
    )

    @Test
    fun `includes calories remaining and macros when goal set`() {
        val text = PipContextBuilder.build(snapshot())
        assertTrue(text.contains("2000"))
        assertTrue(text.contains("1100")) // 2000 - 900 remaining
        assertTrue(text.contains("30/133"))
    }

    @Test
    fun `no goal omits calorie math`() {
        val text = PipContextBuilder.build(snapshot(goalKcal = null))
        assertTrue(text.contains("No calorie goal"))
        assertFalse(text.contains("left of"))
    }

    @Test
    fun `no meals says so`() {
        val text = PipContextBuilder.build(snapshot(recentMeals = emptyList()))
        assertTrue(text.contains("No meals logged today"))
    }

    @Test
    fun `no streak omits streak brag`() {
        val text = PipContextBuilder.build(snapshot(streakDays = 0))
        assertTrue(text.contains("No active streak"))
    }

    @Test
    fun `weight omitted when absent`() {
        val text = PipContextBuilder.build(snapshot(latestWeightKg = null))
        assertFalse(text.lowercase().contains("weight"))
    }

    @Test
    fun `weight trend rendered when present`() {
        val text = PipContextBuilder.build(snapshot(latestWeightKg = 75.0, weightTrend = WeightTrend.DOWN))
        assertTrue(text.contains("75"))
        assertTrue(text.lowercase().contains("down"))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.feature.pip.PipContextBuilderTest"`
Expected: FAIL — unresolved references (`PipContextBuilder`, etc.).

- [ ] **Step 3: Implement persona, snapshot, and builder**

Create `app/src/main/java/com/charles/nutrisnap/feature/pip/PipContext.kt`:

```kotlin
package com.charles.nutrisnap.feature.pip

data class PipMealBrief(val name: String, val kcal: Int)

enum class WeightTrend { UP, DOWN, FLAT, NONE }

/** A pure snapshot of the user's tracking, the only input to [PipContextBuilder]. */
data class PipTrackingSnapshot(
    val goalKcal: Int?,
    val eatenKcal: Int,
    val goalProteinG: Int,
    val eatenProteinG: Int,
    val goalCarbsG: Int,
    val eatenCarbsG: Int,
    val goalFatG: Int,
    val eatenFatG: Int,
    val streakDays: Int,
    val recentMeals: List<PipMealBrief>,
    val latestWeightKg: Double?,
    val weightTrend: WeightTrend,
)

/** Pip's fixed personality and rules, used as the chat system preamble. */
object PipPersona {
    const val SYSTEM: String =
        "You are Pip, a cute, upbeat mango mascot inside a calorie-tracking app. " +
        "You chat with the user about their food tracking and gently cheer them on. " +
        "Keep replies short and friendly — 1 to 3 sentences, warm and playful, with the " +
        "occasional emoji. Use the tracking facts below to give specific, helpful answers " +
        "(e.g. how many calories or grams of protein are left, ideas to hit their goals). " +
        "Never give medical, diagnostic, or weight-loss-prescription advice, and never shame " +
        "the user about what or how much they ate. If you are unsure, say so kindly."
}

/** Pure mapping from a tracking snapshot to a compact context string for the prompt. */
object PipContextBuilder {
    fun build(snapshot: PipTrackingSnapshot): String {
        val lines = mutableListOf<String>()
        lines += "Here is the user's tracking right now:"

        if (snapshot.goalKcal != null) {
            val remaining = snapshot.goalKcal - snapshot.eatenKcal
            lines += "- Calories: ${snapshot.eatenKcal} eaten, $remaining left of a ${snapshot.goalKcal} kcal goal."
            lines += "- Macros so far: protein ${snapshot.eatenProteinG}/${snapshot.goalProteinG}g, " +
                "carbs ${snapshot.eatenCarbsG}/${snapshot.goalCarbsG}g, fat ${snapshot.eatenFatG}/${snapshot.goalFatG}g."
        } else {
            lines += "- No calorie goal is set yet."
        }

        lines += if (snapshot.streakDays >= 1) {
            "- Logging streak: ${snapshot.streakDays} day(s) in a row."
        } else {
            "- No active streak right now."
        }

        lines += if (snapshot.recentMeals.isNotEmpty()) {
            "- Recent meals: " + snapshot.recentMeals.joinToString(", ") { "${it.name} (${it.kcal} kcal)" } + "."
        } else {
            "- No meals logged today yet."
        }

        if (snapshot.latestWeightKg != null) {
            val trend = when (snapshot.weightTrend) {
                WeightTrend.UP -> ", trending up"
                WeightTrend.DOWN -> ", trending down"
                WeightTrend.FLAT -> ", holding steady"
                WeightTrend.NONE -> ""
            }
            lines += "- Latest weight: ${snapshot.latestWeightKg} kg$trend."
        }

        return lines.joinToString("\n")
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.feature.pip.PipContextBuilderTest"`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/feature/pip/PipContext.kt \
        app/src/test/java/com/charles/nutrisnap/feature/pip/PipContextBuilderTest.kt
git commit -m "feat: add PipPersona and pure PipContextBuilder"
```

---

### Task 4: `PipChatViewModel` + snapshot source

Orchestrates snapshot → system instruction → streaming session → persistence, and
exposes UI state. Tested against the fake engine and an in-memory chat repository.

**Files:**
- Create: `app/src/main/java/com/charles/nutrisnap/feature/pip/PipSnapshotSource.kt`
- Create: `app/src/main/java/com/charles/nutrisnap/feature/pip/PipChatViewModel.kt`
- Create: `app/src/main/java/com/charles/nutrisnap/di/PipModule.kt`
- Test: `app/src/test/java/com/charles/nutrisnap/feature/pip/PipChatViewModelTest.kt`

**Interfaces:**
- Consumes: `GemmaEngine.startChat`/`ChatSession` (Task 1); `ChatRepository`, `ChatRole`,
  `ChatMessageEntity` (Task 2); `PipPersona`, `PipContextBuilder`, `PipTrackingSnapshot`,
  `PipMealBrief`, `WeightTrend` (Task 3); existing `GoalRepository.observeRemaining()`,
  `MealRepository.observeTodayMeals()`/`observeDistinctLoggedDays()`,
  `WeightRepository.observeRange(start,end)`, `StreakCalculator.currentStreak(set, today)`,
  `Remaining` (`goal: DailyGoal?` via `remaining?.goal`, `totals`), `PipMood`.
- Produces:
  - `interface PipSnapshotSource { suspend fun snapshot(): PipTrackingSnapshot }`
  - `class DefaultPipSnapshotSource @Inject constructor(goalRepository, mealRepository, weightRepository) : PipSnapshotSource`
  - `class PipChatViewModel(engine: GemmaEngine, chatRepository: ChatRepository, snapshotSource: PipSnapshotSource)` exposing
    `messages: StateFlow<List<ChatMessageEntity>>`, `isGenerating: StateFlow<Boolean>`,
    `streamingText: StateFlow<String?>`, `pipMood: StateFlow<PipMood>`, `fun send(text: String)`.

- [ ] **Step 1: Create the snapshot source**

Create `app/src/main/java/com/charles/nutrisnap/feature/pip/PipSnapshotSource.kt`:

```kotlin
package com.charles.nutrisnap.feature.pip

import com.charles.nutrisnap.data.GoalRepository
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.StreakCalculator
import com.charles.nutrisnap.data.WeightRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface PipSnapshotSource {
    suspend fun snapshot(): PipTrackingSnapshot
}

class DefaultPipSnapshotSource @Inject constructor(
    private val goalRepository: GoalRepository,
    private val mealRepository: MealRepository,
    private val weightRepository: WeightRepository,
) : PipSnapshotSource {

    override suspend fun snapshot(): PipTrackingSnapshot {
        val remaining = goalRepository.observeRemaining().first()
        val meals = mealRepository.observeTodayMeals().first()
        val loggedDays = mealRepository.observeDistinctLoggedDays().first().toSet()
        val todayEpochDay = System.currentTimeMillis() / 86_400_000L
        val streak = StreakCalculator.currentStreak(loggedDays, todayEpochDay)

        val recentMeals = meals
            .sortedByDescending { it.timestampMs }
            .take(3)
            .map { PipMealBrief(name = it.name, kcal = it.totalKcal) }

        val weights = weightRepository.observeRange(todayEpochDay - 30, todayEpochDay).first()
        val latestWeight = weights.lastOrNull()?.weightKg
        val trend = when {
            weights.size < 2 -> WeightTrend.NONE
            else -> {
                val prev = weights[weights.size - 2].weightKg
                val last = weights[weights.size - 1].weightKg
                when {
                    last > prev + 0.05 -> WeightTrend.UP
                    last < prev - 0.05 -> WeightTrend.DOWN
                    else -> WeightTrend.FLAT
                }
            }
        }

        val goal = remaining?.goal
        return PipTrackingSnapshot(
            goalKcal = goal?.calories,
            eatenKcal = meals.sumOf { it.totalKcal },
            goalProteinG = goal?.proteinG ?: 0,
            eatenProteinG = meals.sumOf { it.proteinG },
            goalCarbsG = goal?.carbsG ?: 0,
            eatenCarbsG = meals.sumOf { it.carbsG },
            goalFatG = goal?.fatG ?: 0,
            eatenFatG = meals.sumOf { it.fatG },
            streakDays = streak,
            recentMeals = recentMeals,
            latestWeightKg = latestWeight,
            weightTrend = trend,
        )
    }
}
```

- [ ] **Step 2: Create the Hilt binding module**

Create `app/src/main/java/com/charles/nutrisnap/di/PipModule.kt`:

```kotlin
package com.charles.nutrisnap.di

import com.charles.nutrisnap.feature.pip.DefaultPipSnapshotSource
import com.charles.nutrisnap.feature.pip.PipSnapshotSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PipModule {
    @Binds
    @Singleton
    abstract fun bindPipSnapshotSource(impl: DefaultPipSnapshotSource): PipSnapshotSource
}
```

- [ ] **Step 3: Create the ViewModel**

Create `app/src/main/java/com/charles/nutrisnap/feature/pip/PipChatViewModel.kt`:

```kotlin
package com.charles.nutrisnap.feature.pip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.ai.ChatSession
import com.charles.nutrisnap.ai.GemmaEngine
import com.charles.nutrisnap.data.ChatRepository
import com.charles.nutrisnap.data.db.ChatMessageEntity
import com.charles.nutrisnap.data.db.ChatRole
import com.charles.nutrisnap.ui.components.PipMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PipChatViewModel @Inject constructor(
    private val engine: GemmaEngine,
    private val chatRepository: ChatRepository,
    private val snapshotSource: PipSnapshotSource,
) : ViewModel() {

    companion object {
        private const val RECENT_HISTORY = 6
        private const val FALLBACK =
            "Oops — my brain got a little tangled there. Mind trying again? 🥺"
    }

    val messages: StateFlow<List<ChatMessageEntity>> =
        chatRepository.observeHistory().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _pipMood = MutableStateFlow(PipMood.Content)
    val pipMood: StateFlow<PipMood> = _pipMood.asStateFlow()

    private var session: ChatSession? = null

    init {
        viewModelScope.launch {
            val context = PipContextBuilder.build(snapshotSource.snapshot())
            val recent = chatRepository.recent(RECENT_HISTORY)
            val excerpt = if (recent.isEmpty()) "" else {
                "\n\nRecent conversation:\n" + recent.joinToString("\n") {
                    val who = if (it.role == ChatRole.USER) "User" else "Pip"
                    "$who: ${it.text}"
                }
            }
            val systemInstruction = PipPersona.SYSTEM + "\n\n" + context + excerpt
            session = runCatching { engine.startChat(systemInstruction) }.getOrNull()
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return
        viewModelScope.launch {
            chatRepository.append(ChatRole.USER, trimmed)
            _isGenerating.value = true
            _pipMood.value = PipMood.Thinking
            val s = session
            try {
                if (s == null) {
                    chatRepository.append(ChatRole.PIP, FALLBACK)
                } else {
                    var last = ""
                    s.sendStreaming(trimmed).collect { full ->
                        last = full
                        _streamingText.value = full
                    }
                    chatRepository.append(
                        ChatRole.PIP,
                        last.ifBlank { FALLBACK },
                    )
                }
            } catch (e: Exception) {
                chatRepository.append(ChatRole.PIP, FALLBACK)
            } finally {
                _streamingText.value = null
                _isGenerating.value = false
                _pipMood.value = PipMood.Content
            }
        }
    }

    override fun onCleared() {
        session?.close()
        session = null
    }
}
```

- [ ] **Step 4: Write the failing ViewModel test**

Create `app/src/test/java/com/charles/nutrisnap/feature/pip/PipChatViewModelTest.kt`:

```kotlin
package com.charles.nutrisnap.feature.pip

import androidx.room.Room
import app.cash.turbine.test
import com.charles.nutrisnap.ai.FakeGemmaEngine
import com.charles.nutrisnap.data.ChatRepository
import com.charles.nutrisnap.data.db.AppDatabase
import com.charles.nutrisnap.data.db.ChatRole
import com.charles.nutrisnap.ui.components.PipMood
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PipChatViewModelTest {
    private lateinit var db: AppDatabase
    private lateinit var chatRepo: ChatRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeSnapshot = object : PipSnapshotSource {
        override suspend fun snapshot() = PipTrackingSnapshot(
            goalKcal = 2000, eatenKcal = 800,
            goalProteinG = 133, eatenProteinG = 30,
            goalCarbsG = 361, eatenCarbsG = 90,
            goalFatG = 73, eatenFatG = 40,
            streakDays = 3, recentMeals = listOf(PipMealBrief("Pizza", 450)),
            latestWeightKg = 75.0, weightTrend = WeightTrend.DOWN,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        chatRepo = ChatRepository(db.chatDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `send appends user message then streamed pip reply`() = runTest {
        val vm = PipChatViewModel(FakeGemmaEngine(), chatRepo, fakeSnapshot)
        vm.messages.test {
            awaitItem() // initial empty
            vm.send("how am I doing?")
            // collect until both messages are present
            var latest = awaitItem()
            while (latest.size < 2) latest = awaitItem()
            assertEquals(ChatRole.USER, latest[0].role)
            assertEquals("how am I doing?", latest[0].text)
            assertEquals(ChatRole.PIP, latest[1].role)
            assertEquals("Hey there!", latest[1].text) // last cumulative chunk from the fake
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `generation flags reset after completion`() = runTest {
        val vm = PipChatViewModel(FakeGemmaEngine(), chatRepo, fakeSnapshot)
        vm.send("hi")
        assertFalse(vm.isGenerating.value)
        assertNull(vm.streamingText.value)
        assertEquals(PipMood.Content, vm.pipMood.value)
    }

    @Test
    fun `failed stream appends fallback and stays usable`() = runTest {
        val engine = FakeGemmaEngine().apply { chatShouldFail = true }
        val vm = PipChatViewModel(engine, chatRepo, fakeSnapshot)
        vm.messages.test {
            awaitItem()
            vm.send("hi")
            var latest = awaitItem()
            while (latest.size < 2) latest = awaitItem()
            assertEquals(ChatRole.PIP, latest[1].role)
            // fallback message body
            org.junit.Assert.assertTrue(latest[1].text.contains("tangled"))
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.isGenerating.value)
    }
}
```

- [ ] **Step 5: Run the test to verify it fails, then passes**

Run: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.feature.pip.PipChatViewModelTest"`
Expected: after implementing Steps 1–3, PASS (3 tests). Fix name mismatches until green.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/feature/pip/PipSnapshotSource.kt \
        app/src/main/java/com/charles/nutrisnap/feature/pip/PipChatViewModel.kt \
        app/src/main/java/com/charles/nutrisnap/di/PipModule.kt \
        app/src/test/java/com/charles/nutrisnap/feature/pip/PipChatViewModelTest.kt
git commit -m "feat: add PipChatViewModel with streamed replies and persistence"
```

---

### Task 5: `PipChatScreen` + navigation + dashboard entry

The full-screen chat UI, the nav route, and tapping Pip on the dashboard to open it.
Verified by compile; on-device behavior verified by the user.

**Files:**
- Create: `app/src/main/java/com/charles/nutrisnap/feature/pip/PipChatScreen.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/ui/nav/Destinations.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/ui/nav/NutriNavHost.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/feature/dashboard/DashboardScreen.kt`

**Interfaces:**
- Consumes: `PipChatViewModel` (Task 4), `Pip`/`PipMood` (existing), `ChatRole`,
  `ChatMessageEntity`, `Routes` (existing `ui/nav/Destinations.kt`), `NutriCard`/theme.
- Produces: `Routes.PIP_CHAT` constant; `PipChatScreen(onBack)`; `DashboardScreen` gains
  an `onOpenPipChat: () -> Unit` parameter.

- [ ] **Step 1: Add the route constant**

In `app/src/main/java/com/charles/nutrisnap/ui/nav/Destinations.kt`, add a `PIP_CHAT`
route alongside the existing route constants (match the existing style, e.g.):

```kotlin
    const val PIP_CHAT = "pip_chat"
```

(If `Routes` defines routes as members of an `object Routes`, add the constant there.)

- [ ] **Step 2: Create the chat screen**

Create `app/src/main/java/com/charles/nutrisnap/feature/pip/PipChatScreen.kt`:

```kotlin
package com.charles.nutrisnap.feature.pip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.db.ChatMessageEntity
import com.charles.nutrisnap.data.db.ChatRole
import com.charles.nutrisnap.ui.components.Pip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipChatScreen(
    onBack: () -> Unit,
    viewModel: PipChatViewModel = hiltViewModel(),
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val pipMood by viewModel.pipMood.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingText) {
        val count = messages.size + if (streamingText != null) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with Pip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                Pip(size = 96.dp, mood = pipMood, animated = true)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            ) {
                items(messages, key = { it.id }) { msg -> MessageBubble(msg.role, msg.text) }
                if (streamingText != null) {
                    item(key = "streaming") {
                        if (streamingText!!.isBlank()) TypingIndicator()
                        else MessageBubble(ChatRole.PIP, streamingText!!)
                    }
                } else if (isGenerating) {
                    item(key = "typing") { TypingIndicator() }
                }
            }
            ChatInput(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    if (draft.isNotBlank()) {
                        viewModel.send(draft)
                        draft = ""
                    }
                },
                enabled = !isGenerating,
            )
        }
    }
}

@Composable
private fun MessageBubble(role: ChatRole, text: String) {
    val mine = role == ChatRole.USER
    val bg = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .background(bg, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(text, color = fg, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                "Pip is thinking…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask Pip about your day…") },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() }),
        )
        Spacer(Modifier.height(0.dp))
        IconButton(onClick = { if (enabled) onSend() }, enabled = enabled) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
```

- [ ] **Step 3: Register the route in the nav host**

In `app/src/main/java/com/charles/nutrisnap/ui/nav/NutriNavHost.kt`, add the import:

```kotlin
import com.charles.nutrisnap.feature.pip.PipChatScreen
```

Add a destination inside the `NavHost { ... }` body (next to the other `composable`
blocks):

```kotlin
        composable(Routes.PIP_CHAT) {
            PipChatScreen(onBack = { navController.popBackStack() })
        }
```

Update the HOME destination to pass the chat-open callback:

```kotlin
        composable(Routes.HOME) {
            DashboardScreen(
                onOpenMeal = { mealId -> navController.navigate(Routes.editMeal(mealId)) },
                onAddMeal = { navController.navigate(Routes.entry("manual")) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPipChat = { navController.navigate(Routes.PIP_CHAT) },
            )
        }
```

- [ ] **Step 4: Wire the dashboard Pip tap to open chat**

In `app/src/main/java/com/charles/nutrisnap/feature/dashboard/DashboardScreen.kt`:

Add the `onOpenPipChat` parameter to `DashboardScreen` (with a default so previews/tests
compile):

```kotlin
fun DashboardScreen(
    onOpenMeal: (Long) -> Unit = {},
    onAddMeal: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenPipChat: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
```

Thread it into the header. Change the header item call:

```kotlin
            item {
                HeaderSection(state, celebrating = showConfetti, onPipTap = onOpenPipChat)
            }
```

And update `HeaderSection` to accept and use it on Pip's tap:

```kotlin
@Composable
private fun HeaderSection(state: DashboardUiState, celebrating: Boolean, onPipTap: () -> Unit) {
    val mood = if (celebrating) PipMood.Celebrate else pipMoodFor(state)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Pip(size = 56.dp, mood = mood, animated = true, onPoke = onPipTap)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Hey there!", style = MaterialTheme.typography.headlineMedium)
            Text(
                dayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.streak > 0) {
            StreakPill(days = state.streak)
        }
    }
}
```

- [ ] **Step 5: Compile main and tests**

Run: `./gradlew compileDebugKotlin compileDebugUnitTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run the full unit suite**

Run: `./gradlew testDebugUnitTest`
Expected: BUILD SUCCESSFUL (all tests, including Tasks 2–4, pass).

- [ ] **Step 7: On-device verification (manual — performed by a human with a device)**

Build/install the signed release (preserves the downloaded model — debug has a
different signature):
`./gradlew assembleRelease` then
`adb -s <device> install -r app/build/outputs/apk/release/app-release.apk`.
Verify:
- Tapping Pip on the dashboard opens the full-screen chat with a big Pip on top.
- Sending a message shows the user bubble, then "Pip is thinking…", then Pip's reply
  **streams in token-by-token**; Pip shows the Thinking mood while generating and
  returns to Content after.
- The reply reflects real data (e.g. asking "how many calories do I have left?" yields a
  number consistent with the dashboard).
- Closing and reopening the chat shows prior messages (persisted) and Pip still
  responds with continuity.
- Confirm streamed text is not duplicated (if it is, apply the one-line emit fix noted
  in Task 1 Step 2 and reinstall).
(If no device is available, stop after Step 6's green suite and note device verification
is pending.)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/feature/pip/PipChatScreen.kt \
        app/src/main/java/com/charles/nutrisnap/ui/nav/Destinations.kt \
        app/src/main/java/com/charles/nutrisnap/ui/nav/NutriNavHost.kt \
        app/src/main/java/com/charles/nutrisnap/feature/dashboard/DashboardScreen.kt
git commit -m "feat: Pip chat screen, nav route, and dashboard entry point"
```

---

## Notes for the implementer

- `PipContextBuilder` and `PipPersona` are pure/static — keep all data→text logic there,
  not in the ViewModel, so it stays unit-tested.
- `pipMoodFor` is in the dashboard package and already used by `HeaderSection`; Task 5
  only adds the `onPipTap` wiring — don't change the mood logic.
- The chat conversation maintains in-session memory natively (one open `Conversation`);
  cross-session continuity comes only from the last-6-messages excerpt folded into the
  system instruction. Don't try to replay full history into the model.
- The DB migration is the one piece not covered by unit tests (in-memory Room starts at
  the current version). Get the `chat_message` `createSql` from the generated
  `app/schemas/.../2.json` verbatim (Task 2 Step 6) — a mismatch crashes existing-data
  upgrades on device.
- If `assembleRelease`/device install is unavailable, a green `testDebugUnitTest` plus
  clean `compileDebugKotlin` is the stopping point; flag device verification as pending.
```
