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
