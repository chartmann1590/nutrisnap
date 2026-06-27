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
import org.junit.Assert.assertTrue
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
        testScheduler.advanceUntilIdle()
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
        testScheduler.advanceUntilIdle()
        vm.send("hi")
        assertFalse(vm.isGenerating.value)
        assertNull(vm.streamingText.value)
        assertEquals(PipMood.Content, vm.pipMood.value)
    }

    @Test
    fun `failed stream appends fallback and stays usable`() = runTest {
        val engine = FakeGemmaEngine().apply { chatShouldFail = true }
        val vm = PipChatViewModel(engine, chatRepo, fakeSnapshot)
        testScheduler.advanceUntilIdle()
        vm.messages.test {
            awaitItem()
            vm.send("hi")
            var latest = awaitItem()
            while (latest.size < 2) latest = awaitItem()
            assertEquals(ChatRole.PIP, latest[1].role)
            assertTrue(latest[1].text.contains("tangled"))
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.isGenerating.value)
    }
}
