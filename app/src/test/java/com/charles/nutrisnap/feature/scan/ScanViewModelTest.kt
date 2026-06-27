package com.charles.nutrisnap.feature.scan

import android.graphics.Bitmap
import app.cash.turbine.test
import com.charles.nutrisnap.ai.FakeGemmaEngine
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScanViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is idle`() {
        val vm = ScanViewModel(FakeGemmaEngine(), fakeMealRepo())
        Assert.assertTrue(vm.state.value is ScanUiState.Idle)
    }

    @Test
    fun `capturing triggers analyzing state`() = runTest {
        val vm = ScanViewModel(FakeGemmaEngine(), fakeMealRepo())
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        vm.onCaptured(bitmap)
        Assert.assertTrue(vm.state.value is ScanUiState.Analyzing)
    }

    @Test
    fun `fake analyze returns result`() = runTest {
        val vm = ScanViewModel(FakeGemmaEngine(), fakeMealRepo())
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        vm.events.test {
            vm.onCaptured(bitmap)
            val event = awaitItem()
            Assert.assertTrue(event is ScanEvent.NavigateToResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `log meal emits logged event`() = runTest {
        val vm = ScanViewModel(FakeGemmaEngine(), fakeMealRepo())
        val estimate = FakeGemmaEngine()
            .analyzeFood(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
            .getOrThrow()
        vm.events.test {
            vm.logMeal(estimate, MealType.LUNCH, null)
            val event = awaitItem()
            Assert.assertTrue(event is ScanEvent.Logged)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset state works`() {
        val vm = ScanViewModel(FakeGemmaEngine(), fakeMealRepo())
        vm.resetState()
        Assert.assertTrue(vm.state.value is ScanUiState.Idle)
    }
}

private fun fakeMealRepo() = object : MealRepository(null) {
    override suspend fun logMeal(meal: MealEntity): Long = 1L
    override suspend fun deleteMeal(id: Long) {}
}