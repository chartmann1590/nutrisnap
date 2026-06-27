# Pip Reactive Mascot & Motion Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn Pip (the mango mascot) into a reactive character that responds to the user's real day, and add richer app-wide motion — all in native Jetpack Compose.

**Architecture:** A pure `pipMoodFor(state)` function maps live dashboard data to a `PipMood`; the `Pip` composable is a mood-driven drawing/animation state machine (blink, squash-stretch, tap-to-poke). Data→mood logic is pure and unit-tested; mood→pixels lives in Compose and is verified by running the app. Transient celebrations are layered on top via existing event hooks.

**Tech Stack:** Kotlin, Jetpack Compose (Canvas + animation-core), JUnit 4, Gradle.

## Global Constraints

- No new external dependencies. No Lottie. Compose Canvas + `androidx.compose.animation.core` only.
- Pip keeps its core look: mango body, green leaf, white eyes with cocoa pupils, rosy cheeks, cocoa smile.
- Mascot moods are never shaming (the "over goal" mood is gentle/woozy, not negative).
- Theme colors come from existing `com.charles.nutrisnap.ui.theme` (`Mango`, `Mint`, `Berry`, `Cocoa`). Do not hardcode hex.
- Unit tests run with: `./gradlew testDebugUnitTest`
- Single test class: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.feature.dashboard.PipMoodForTest"`
- Compile check (no device needed): `./gradlew compileDebugKotlin`

---

### Task 1: PipMood enum + pure `pipMoodFor` mapping

Defines the mood vocabulary and the pure data→mood function. This is the only
unit-testable piece, so it is done strictly TDD.

**Files:**
- Create: `app/src/main/java/com/charles/nutrisnap/ui/components/PipMood.kt`
- Create: `app/src/main/java/com/charles/nutrisnap/feature/dashboard/PipMoodMapping.kt`
- Test: `app/src/test/java/com/charles/nutrisnap/feature/dashboard/PipMoodForTest.kt`

**Interfaces:**
- Produces: `enum class PipMood { Content, Sleepy, Celebrate, Proud, Stuffed, Thinking }`
- Produces: `fun pipMoodFor(state: DashboardUiState): PipMood` (in package `com.charles.nutrisnap.feature.dashboard`)
- Consumes: `DashboardUiState` (existing, `feature/dashboard/DashboardViewModel.kt`), `Remaining.kcalRemaining: Int` (existing, `data/GoalRepository.kt`).
- Note: `Celebrate` and `Thinking` are NEVER returned by `pipMoodFor` — they are transient/explicit moods set by the UI (Task 3). `pipMoodFor` only returns steady-state moods: `Stuffed`, `Sleepy`, `Proud`, `Content`.

- [ ] **Step 1: Create the PipMood enum**

Create `app/src/main/java/com/charles/nutrisnap/ui/components/PipMood.kt`:

```kotlin
package com.charles.nutrisnap.ui.components

/**
 * The set of moods Pip can express. [pipMoodFor] derives steady-state moods from
 * dashboard data; [Celebrate] and [Thinking] are set explicitly by the UI for
 * transient events (a meal just logged) or specific screens (AI analysis).
 */
enum class PipMood {
    Content,
    Sleepy,
    Celebrate,
    Proud,
    Stuffed,
    Thinking,
}
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/charles/nutrisnap/feature/dashboard/PipMoodForTest.kt`:

```kotlin
package com.charles.nutrisnap.feature.dashboard

import com.charles.nutrisnap.data.Remaining
import com.charles.nutrisnap.data.db.DayTotals
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.feature.onboarding.DailyGoal
import com.charles.nutrisnap.ui.components.PipMood
import org.junit.Assert.assertEquals
import org.junit.Test

class PipMoodForTest {

    private fun remaining(goalKcal: Int, eatenKcal: Int) = Remaining(
        goal = DailyGoal(calories = goalKcal, proteinG = 100, carbsG = 200, fatG = 60),
        totals = DayTotals(totalKcal = eatenKcal, proteinG = 0, carbsG = 0, fatG = 0),
    )

    private fun meal() = MealEntity(
        timestampMs = 0L,
        mealType = MealType.LUNCH,
        name = "Test",
        totalKcal = 500,
        proteinG = 10,
        carbsG = 20,
        fatG = 5,
        source = MealSource.MANUAL,
    )

    @Test
    fun `no goal yet is Content`() {
        val state = DashboardUiState(remaining = null)
        assertEquals(PipMood.Content, pipMoodFor(state))
    }

    @Test
    fun `over calorie goal is Stuffed`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 2200),
            todayMeals = listOf(meal()),
        )
        assertEquals(PipMood.Stuffed, pipMoodFor(state))
    }

    @Test
    fun `under goal with no meals is Sleepy`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 0),
            todayMeals = emptyList(),
        )
        assertEquals(PipMood.Sleepy, pipMoodFor(state))
    }

    @Test
    fun `meals logged with active streak is Proud`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 800),
            todayMeals = listOf(meal()),
            streak = 5,
        )
        assertEquals(PipMood.Proud, pipMoodFor(state))
    }

    @Test
    fun `meals logged with short streak is Content`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 800),
            todayMeals = listOf(meal()),
            streak = 1,
        )
        assertEquals(PipMood.Content, pipMoodFor(state))
    }

    @Test
    fun `over goal beats active streak`() {
        val state = DashboardUiState(
            remaining = remaining(goalKcal = 2000, eatenKcal = 2500),
            todayMeals = listOf(meal()),
            streak = 9,
        )
        assertEquals(PipMood.Stuffed, pipMoodFor(state))
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.feature.dashboard.PipMoodForTest"`
Expected: FAIL with unresolved reference `pipMoodFor`.

- [ ] **Step 4: Write the mapping**

Create `app/src/main/java/com/charles/nutrisnap/feature/dashboard/PipMoodMapping.kt`:

```kotlin
package com.charles.nutrisnap.feature.dashboard

import com.charles.nutrisnap.ui.components.PipMood

/**
 * Maps a dashboard snapshot to Pip's steady-state mood. Pure — no Compose, no IO.
 * Priority: over-goal (gentle) > nothing logged > active streak > content.
 * Never returns [PipMood.Celebrate] or [PipMood.Thinking]; those are set by the UI.
 */
fun pipMoodFor(state: DashboardUiState): PipMood {
    val remaining = state.remaining ?: return PipMood.Content
    return when {
        remaining.kcalRemaining < 0 -> PipMood.Stuffed
        state.todayMeals.isEmpty() -> PipMood.Sleepy
        state.streak >= 2 -> PipMood.Proud
        else -> PipMood.Content
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.charles.nutrisnap.feature.dashboard.PipMoodForTest"`
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/ui/components/PipMood.kt \
        app/src/main/java/com/charles/nutrisnap/feature/dashboard/PipMoodMapping.kt \
        app/src/test/java/com/charles/nutrisnap/feature/dashboard/PipMoodForTest.kt
git commit -m "feat: add PipMood and pure pipMoodFor mapping"
```

---

### Task 2: Reactive Pip composable

Rewrite `Pip` into a mood-driven state machine with blink, squash-and-stretch,
and tap-to-poke. Visual/animation behavior is verified by compiling and running
(not unit tests). The existing call sites (`EmptyState`, `LoadingState`,
`ScanScreens`) keep compiling because `mood` has a default.

**Files:**
- Modify (full rewrite): `app/src/main/java/com/charles/nutrisnap/ui/components/Pip.kt`

**Interfaces:**
- Consumes: `PipMood` (Task 1), theme colors `Mango`, `Mint`, `Berry`, `Cocoa`.
- Produces: `@Composable fun Pip(modifier: Modifier = Modifier, size: Dp = 128.dp, mood: PipMood = PipMood.Content, animated: Boolean = true, onPoke: () -> Unit = {})`
  - Backward compatible: existing calls `Pip(size = X, animated = true)` still compile (mood defaults to `Content`).

- [ ] **Step 1: Rewrite Pip.kt**

Replace the entire contents of `app/src/main/java/com/charles/nutrisnap/ui/components/Pip.kt`:

```kotlin
package com.charles.nutrisnap.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charles.nutrisnap.ui.theme.Berry
import com.charles.nutrisnap.ui.theme.Cocoa
import com.charles.nutrisnap.ui.theme.Mango
import com.charles.nutrisnap.ui.theme.Mint
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pip — the NutriSnap mascot, a cute mango with a leaf, big eyes and rosy cheeks.
 * Drawn vectorially so it stays crisp at any size. Pip reacts to [mood]: posture,
 * eyes and mouth change per mood. Always-on touches when [animated]: a bob with
 * squash-and-stretch, and periodic blinking. Tapping Pip triggers a poke wobble
 * and calls [onPoke].
 */
@Composable
fun Pip(
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    mood: PipMood = PipMood.Content,
    animated: Boolean = true,
    onPoke: () -> Unit = {},
) {
    // Bob speed/height vary by mood (Celebrate is fast & big; Sleepy is slow & small).
    val bobPeriodMs = when (mood) {
        PipMood.Celebrate -> 520
        PipMood.Sleepy -> 3400
        PipMood.Stuffed -> 3000
        else -> 2400
    }
    val bobHeight = when (mood) {
        PipMood.Celebrate -> 0.16f
        PipMood.Sleepy -> 0.03f
        else -> 0.06f
    }

    val transition = rememberInfiniteTransition(label = "pip")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(bobPeriodMs), RepeatMode.Reverse),
        label = "bob",
    )
    // Slow woozy sway for the Stuffed mood (degrees).
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "sway",
    )

    // Blink: eyes are open (1f) most of the time, snapping shut briefly at random.
    val eyeOpen = remember { Animatable(1f) }
    if (animated) {
        androidx.compose.runtime.LaunchedEffect(mood) {
            // Sleepy keeps eyes half-closed and blinks rarely.
            val base = if (mood == PipMood.Sleepy) 0.35f else 1f
            eyeOpen.snapTo(base)
            while (true) {
                kotlinx.coroutines.delay(Random.nextLong(2400, 4800))
                eyeOpen.animateTo(0.08f, tween(70))
                eyeOpen.animateTo(base, tween(110))
            }
        }
    }

    // Tap-to-poke: one-shot 0->1->0 that adds extra squish + a small spin.
    val poke = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Canvas(
        modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onPoke()
                        scope.launch {
                            poke.snapTo(0f)
                            poke.animateTo(1f, tween(120))
                            poke.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 320f))
                        }
                    },
                )
            },
    ) {
        val w = this.size.width
        val h = this.size.height
        val dy = -bob * h * bobHeight

        // Squash-and-stretch: stretch tall near the top of the bob, squash wide at the
        // bottom. Poke adds an extra squash pulse.
        val stretch = (bob - 0.5f) * 2f
        val pokeSquash = sin(poke.value * Math.PI.toFloat())
        val sx = 1f + 0.05f * stretch + 0.10f * pokeSquash
        val sy = 1f - 0.05f * stretch - 0.10f * pokeSquash

        val swayDeg = if (mood == PipMood.Stuffed) sway * 4f else 0f
        val pokeSpinDeg = poke.value * 8f

        fun x(p: Float) = w * p
        fun y(p: Float) = h * p + dy

        // soft shadow (drawn outside the squash transform so the ground stays put)
        drawOval(
            color = Color.Black.copy(alpha = 0.08f),
            topLeft = Offset(x(0.25f), h * 0.9f),
            size = Size(w * 0.5f, h * 0.06f),
        )

        rotate(degrees = swayDeg + pokeSpinDeg, pivot = Offset(w * 0.5f, h * 0.6f)) {
            scale(scaleX = sx, scaleY = sy, pivot = Offset(w * 0.5f, h * 0.95f)) {
                // body — Stuffed mood is a touch wider
                val widen = if (mood == PipMood.Stuffed) 0.04f else 0f
                val body = Path().apply {
                    moveTo(x(0.5f), y(0.13f))
                    cubicTo(x(0.84f + widen), y(0.13f), x(0.92f + widen), y(0.45f), x(0.92f + widen), y(0.6f))
                    cubicTo(x(0.92f + widen), y(0.86f), x(0.72f), y(0.95f), x(0.5f), y(0.95f))
                    cubicTo(x(0.28f), y(0.95f), x(0.08f - widen), y(0.86f), x(0.08f - widen), y(0.6f))
                    cubicTo(x(0.08f - widen), y(0.45f), x(0.16f - widen), y(0.13f), x(0.5f), y(0.13f))
                    close()
                }
                drawPath(body, Mango)

                // leaf
                val leaf = Path().apply {
                    moveTo(x(0.52f), y(0.14f))
                    cubicTo(x(0.54f), y(0.02f), x(0.66f), y(0.0f), x(0.66f), y(0.0f))
                    cubicTo(x(0.66f), y(0.0f), x(0.66f), y(0.1f), x(0.58f), y(0.13f))
                    cubicTo(x(0.54f), y(0.145f), x(0.52f), y(0.14f), x(0.52f), y(0.14f))
                    close()
                }
                drawPath(leaf, Mint)

                // Proud mood: a little sparkle by the leaf
                if (mood == PipMood.Proud) {
                    val spark = w * 0.03f
                    val cx = x(0.78f)
                    val cy = y(0.16f)
                    drawLine(Color.White, Offset(cx - spark, cy), Offset(cx + spark, cy), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
                    drawLine(Color.White, Offset(cx, cy - spark), Offset(cx, cy + spark), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
                }

                // cheeks
                drawCircle(Berry.copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(x(0.33f), y(0.62f)))
                drawCircle(Berry.copy(alpha = 0.5f), radius = w * 0.05f, center = Offset(x(0.67f), y(0.62f)))

                // eyes — pupils look up for Thinking; eyes squeeze to happy arcs for Celebrate
                val open = eyeOpen.value
                val pupilDy = if (mood == PipMood.Thinking) -w * 0.02f else 0f
                if (mood == PipMood.Celebrate) {
                    // happy upturned arcs ^ ^
                    val arc = w * 0.05f
                    drawArc(
                        color = Cocoa,
                        startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(x(0.4f) - arc, y(0.52f) - arc),
                        size = Size(arc * 2, arc * 2),
                        style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Cocoa,
                        startAngle = 200f, sweepAngle = 140f, useCenter = false,
                        topLeft = Offset(x(0.6f) - arc, y(0.52f) - arc),
                        size = Size(arc * 2, arc * 2),
                        style = Stroke(width = w * 0.022f, cap = StrokeCap.Round),
                    )
                } else {
                    // round eyes that close vertically when blinking (scaled by `open`)
                    val eyeR = w * 0.06f
                    drawOval(
                        Color.White,
                        topLeft = Offset(x(0.4f) - eyeR, y(0.52f) - eyeR * open),
                        size = Size(eyeR * 2, eyeR * 2 * open),
                    )
                    drawOval(
                        Color.White,
                        topLeft = Offset(x(0.6f) - eyeR, y(0.52f) - eyeR * open),
                        size = Size(eyeR * 2, eyeR * 2 * open),
                    )
                    if (open > 0.4f) {
                        drawCircle(Cocoa, radius = w * 0.03f, center = Offset(x(0.41f), y(0.53f) + pupilDy))
                        drawCircle(Cocoa, radius = w * 0.03f, center = Offset(x(0.61f), y(0.53f) + pupilDy))
                    }
                }

                // mouth per mood
                when (mood) {
                    PipMood.Celebrate -> {
                        // big open happy mouth
                        val mouth = Path().apply {
                            moveTo(x(0.43f), y(0.64f))
                            cubicTo(x(0.47f), y(0.74f), x(0.53f), y(0.74f), x(0.57f), y(0.64f))
                            close()
                        }
                        drawPath(mouth, Cocoa)
                    }
                    PipMood.Sleepy -> {
                        // small yawning "o"
                        drawCircle(Cocoa, radius = w * 0.025f, center = Offset(x(0.5f), y(0.67f)))
                    }
                    PipMood.Stuffed -> {
                        // flat wavy line
                        val mouth = Path().apply {
                            moveTo(x(0.43f), y(0.66f))
                            cubicTo(x(0.47f), y(0.63f), x(0.53f), y(0.69f), x(0.57f), y(0.66f))
                        }
                        drawPath(mouth, Cocoa, style = Stroke(width = w * 0.022f, cap = StrokeCap.Round))
                    }
                    PipMood.Thinking -> {
                        // tiny pursed line
                        drawLine(Cocoa, Offset(x(0.46f), y(0.66f)), Offset(x(0.54f), y(0.66f)), strokeWidth = w * 0.022f, cap = StrokeCap.Round)
                    }
                    else -> {
                        // gentle smile (Content, Proud)
                        val smile = Path().apply {
                            moveTo(x(0.43f), y(0.64f))
                            cubicTo(x(0.47f), y(0.70f), x(0.53f), y(0.70f), x(0.57f), y(0.64f))
                        }
                        drawPath(smile, Cocoa, style = Stroke(width = w * 0.025f, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Compile to verify it builds and existing call sites still work**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (`EmptyState`, `LoadingState`, and `ScanScreens` call `Pip(size = ..., animated = true)` — they compile because `mood` defaults to `Content`.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/ui/components/Pip.kt
git commit -m "feat: make Pip a mood-driven reactive mascot with blink, squash-stretch, tap-to-poke"
```

---

### Task 3: Wire Pip into the dashboard + reactive moods at call sites

Give Pip a home on the dashboard reacting to live data, drive a transient
`Celebrate` when a meal is logged, and set explicit moods at the empty/scan call
sites.

**Files:**
- Modify: `app/src/main/java/com/charles/nutrisnap/feature/dashboard/DashboardScreen.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/ui/components/StateViews.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/feature/scan/ScanScreens.kt:205` and `:316`

**Interfaces:**
- Consumes: `pipMoodFor(state)` (Task 1), `Pip(mood = ...)` (Task 2), `PipMood`.
- Produces: `EmptyState(..., mood: PipMood = PipMood.Sleepy)` — adds an optional `mood` param.

- [ ] **Step 1: Add a `mood` param to EmptyState and pass it to Pip**

In `app/src/main/java/com/charles/nutrisnap/ui/components/StateViews.kt`, update `EmptyState`. Change its signature and the `Pip(...)` call:

```kotlin
@Composable
fun EmptyState(
    pipSize: Dp = 100.dp,
    title: String,
    subtitle: String?,
    modifier: Modifier,
    mood: PipMood = PipMood.Sleepy,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Pip(size = pipSize, mood = mood, animated = true)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
```

Add the import at the top of the file:

```kotlin
import com.charles.nutrisnap.ui.components.PipMood
```

(If the IDE flags a same-package self-import, drop the import — `PipMood` is in the same package `com.charles.nutrisnap.ui.components`, so no import is needed. Remove the import line in that case.)

- [ ] **Step 2: Set the Thinking mood on the scan screens**

In `app/src/main/java/com/charles/nutrisnap/feature/scan/ScanScreens.kt`, the analyzing view at line ~205 currently reads `Pip(size = 110.dp, animated = true)`. Change it to:

```kotlin
Pip(size = 110.dp, mood = com.charles.nutrisnap.ui.components.PipMood.Thinking, animated = true)
```

Leave the result-screen `Pip(size = 96.dp, animated = true)` at line ~316 as-is (default `Content` is correct for a finished result).

- [ ] **Step 3: Add Pip to the dashboard header and drive its mood from state**

In `app/src/main/java/com/charles/nutrisnap/feature/dashboard/DashboardScreen.kt`:

Add imports:

```kotlin
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PipMood
```

Replace the `HeaderSection` composable with a version that shows Pip reacting to mood. The displayed mood is `Celebrate` while a celebration is active, otherwise `pipMoodFor(state)`:

```kotlin
@Composable
private fun HeaderSection(state: DashboardUiState, celebrating: Boolean) {
    val mood = if (celebrating) PipMood.Celebrate else pipMoodFor(state)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Pip(size = 56.dp, mood = mood, animated = true)
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

- [ ] **Step 4: Trigger a celebration when a meal is logged, and pass `celebrating` down**

In `DashboardScreen`, the screen already tracks `showConfetti` and `previousMealCount`. Make a meal being added also celebrate, and feed `showConfetti` into the header. Replace the `LaunchedEffect(state.todayMeals.size)` block and the `HeaderSection(state)` call:

Replace:

```kotlin
    LaunchedEffect(state.todayMeals.size) {
        previousMealCount = state.todayMeals.size
    }
```

with:

```kotlin
    LaunchedEffect(state.todayMeals.size) {
        if (state.todayMeals.size > previousMealCount && previousMealCount >= 0) {
            showConfetti = true
        }
        previousMealCount = state.todayMeals.size
    }
```

And replace the header item:

```kotlin
            item {
                HeaderSection(state)
            }
```

with:

```kotlin
            item {
                HeaderSection(state, celebrating = showConfetti)
            }
```

- [ ] **Step 5: Use the Sleepy mood explicitly for the empty meals state**

In `DashboardScreen`, the empty state call already defaults to `Sleepy` (Task 3 Step 1). Make it explicit for clarity — replace the `EmptyState(...)` call in the meals section with:

```kotlin
                    EmptyState(
                        pipSize = 100.dp,
                        title = "No meals yet",
                        subtitle = "Snap a photo to get started!",
                        modifier = Modifier,
                        mood = PipMood.Sleepy,
                    )
```

- [ ] **Step 6: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run the app and visually verify**

Build and install: `./gradlew installDebug` (device/emulator required).
Verify on the Home screen:
- Pip appears in the header beside "Hey there!".
- With no meals logged today, Pip looks Sleepy (half-closed eyes, small yawn).
- After logging a meal, confetti fires and Pip briefly Celebrates (big bounce + happy face), then settles.
- With a 2+ day streak and meals logged, Pip shows the Proud sparkle.
- Tapping Pip makes it wobble/squish.
- On the scan analyzing screen, Pip looks up (Thinking).

(If no device is available, stop after Step 6's successful compile and note that visual verification is pending.)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/feature/dashboard/DashboardScreen.kt \
        app/src/main/java/com/charles/nutrisnap/ui/components/StateViews.kt \
        app/src/main/java/com/charles/nutrisnap/feature/scan/ScanScreens.kt
git commit -m "feat: Pip lives on the dashboard and reacts to meals, streaks, and analysis"
```

---

### Task 4: App-wide motion polish — screen transitions + streak-driven flame

Add smooth screen transitions to the nav host and make the streak flame livelier
as the streak grows. Verified by compile + run.

**Files:**
- Modify: `app/src/main/java/com/charles/nutrisnap/ui/nav/NutriNavHost.kt`
- Modify: `app/src/main/java/com/charles/nutrisnap/ui/components/Motion.kt`

**Interfaces:**
- Produces: `StreakFlame(modifier: Modifier = Modifier, size: Dp = 24.dp, intensity: Float = 1f)` — adds an optional `intensity` controlling pulse speed/brightness. Existing calls with no `intensity` keep working.

- [ ] **Step 1: Add default enter/exit transitions to the NavHost**

In `app/src/main/java/com/charles/nutrisnap/ui/nav/NutriNavHost.kt`, add imports:

```kotlin
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIntoContainer
import androidx.compose.animation.slideOutOfContainer
```

Then add transition lambdas to the `NavHost(...)` call (these apply to all `composable` destinations by default). Update the `NavHost(` invocation to:

```kotlin
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) +
                fadeIn(tween(280))
        },
        exitTransition = {
            fadeOut(tween(200))
        },
        popEnterTransition = {
            fadeIn(tween(280))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) +
                fadeOut(tween(280))
        },
    ) {
```

(Leave the body of the `NavHost` — all the `composable(...)` blocks — unchanged.)

- [ ] **Step 2: Make StreakFlame intensity-aware**

In `app/src/main/java/com/charles/nutrisnap/ui/components/Motion.kt`, replace the `StreakFlame` composable with:

```kotlin
@Composable
fun StreakFlame(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    intensity: Float = 1f,
) {
    val i = intensity.coerceIn(0f, 1f)
    val periodMs = (820 - 320 * i).toInt() // hotter streak = faster flicker
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.55f + 0.1f * i,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs), RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(modifier.size(size)) {
        val w = size.toPx()
        val h = size.toPx()
        val flame = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            cubicTo(w * 0.55f, h * 0.3f, w * 0.7f, h * 0.45f, w * 0.7f, h * 0.6f)
            cubicTo(w * 0.7f, h * 0.75f, w * 0.6f, h * 0.9f, w * 0.5f, h * 0.9f)
            cubicTo(w * 0.4f, h * 0.9f, w * 0.3f, h * 0.75f, w * 0.3f, h * 0.6f)
            cubicTo(w * 0.3f, h * 0.45f, w * 0.45f, h * 0.3f, w * 0.5f, h * 0.1f)
            close()
        }
        drawPath(flame, Mango.copy(alpha = alpha))

        val glow = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.52f, h * 0.3f, w * 0.6f, h * 0.4f, w * 0.6f, h * 0.55f)
            cubicTo(w * 0.6f, h * 0.7f, w * 0.55f, h * 0.8f, w * 0.5f, h * 0.8f)
            cubicTo(w * 0.45f, h * 0.8f, w * 0.4f, h * 0.7f, w * 0.4f, h * 0.55f)
            cubicTo(w * 0.4f, h * 0.4f, w * 0.48f, h * 0.3f, w * 0.5f, h * 0.15f)
            close()
        }
        drawPath(glow, Color.White.copy(alpha = alpha * (0.4f + 0.3f * i)))
    }
}
```

- [ ] **Step 3: Feed streak intensity where StreakFlame is used**

Find usages: `grep -rn "StreakFlame(" app/src/main`. For any call site that has access to a streak count (e.g. inside `StreakPill`), pass `intensity = (streak / 7f).coerceIn(0f, 1f)`. If a call site has no streak value in scope, leave it at the default (`intensity = 1f`) — do not thread new parameters through unrelated composables just for this.

- [ ] **Step 4: Compile**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run and visually verify**

Build and install: `./gradlew installDebug` (device/emulator required).
Verify:
- Navigating between screens slides/fades smoothly; back navigation slides the other way.
- The streak flame flickers faster/brighter as the streak grows.
(If no device is available, stop after Step 4's successful compile and note visual verification is pending.)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/charles/nutrisnap/ui/nav/NutriNavHost.kt \
        app/src/main/java/com/charles/nutrisnap/ui/components/Motion.kt
git commit -m "feat: smooth screen transitions and streak-driven flame liveliness"
```

---

## Notes for the implementer

- Pip's bob uses `RepeatMode.Reverse`, so `bob` is a smooth 0↔1 triangle-ish value; squash-and-stretch is derived from it directly — there is no separate physics engine.
- `pipMoodFor` deliberately never returns `Celebrate`/`Thinking`. Those are owned by the UI: `Celebrate` is transient (driven by `showConfetti` on the dashboard), `Thinking` is the scan-analysis screen. Keep this separation — it's what makes the mapping pure and testable.
- The blink loop is an infinite `while (true)` inside a `LaunchedEffect` keyed on `mood`; it is automatically cancelled when Pip leaves composition or `mood` changes. Do not move it outside `LaunchedEffect`.
- If `./gradlew installDebug` is unavailable in the environment, completing the compile step is acceptable; flag that visual verification is pending so a human can eyeball Pip.
```
