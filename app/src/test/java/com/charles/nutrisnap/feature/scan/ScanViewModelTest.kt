package com.charles.nutrisnap.feature.scan

import android.app.Activity
import android.graphics.Bitmap
import app.cash.turbine.test
import com.charles.nutrisnap.ai.FakeGemmaEngine
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.PremiumAccess
import com.charles.nutrisnap.data.PremiumEntitlement
import com.charles.nutrisnap.data.PremiumPlan
import com.charles.nutrisnap.data.ScanQuota
import com.charles.nutrisnap.data.ScanQuotaRepository
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        val vm = scanViewModel()
        Assert.assertTrue(vm.state.value is ScanUiState.Idle)
    }

    @Test
    fun `free scan records quota usage`() = runTest {
        val quota = FakeScanQuotaRepository()
        val vm = scanViewModel(quota = quota)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        vm.events.test {
            vm.onCaptured(bitmap)
            awaitItem()
            Assert.assertEquals(1, quota.quota.value.used)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fake analyze returns result`() = runTest {
        val vm = scanViewModel()
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        vm.events.test {
            vm.onCaptured(bitmap)
            val event = awaitItem()
            Assert.assertTrue(event is ScanEvent.NavigateToResult)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exhausted free quota shows paywall`() = runTest {
        val vm = scanViewModel(quota = FakeScanQuotaRepository(used = 10))
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        vm.onCaptured(bitmap)
        Assert.assertTrue(vm.state.value is ScanUiState.Paywall)
    }

    @Test
    fun `premium user bypasses free quota`() = runTest {
        val vm = scanViewModel(
            premium = FakePremiumAccess(isPremium = true),
            quota = FakeScanQuotaRepository(used = 10),
        )
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
        val vm = scanViewModel()
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
        val vm = scanViewModel()
        vm.resetState()
        Assert.assertTrue(vm.state.value is ScanUiState.Idle)
    }
}

private fun scanViewModel(
    premium: PremiumAccess = FakePremiumAccess(),
    quota: FakeScanQuotaRepository = FakeScanQuotaRepository(),
) = ScanViewModel(FakeGemmaEngine(), fakeMealRepo(), premium, quota)

private class FakePremiumAccess(
    isPremium: Boolean = false,
) : PremiumAccess {
    override val entitlement: StateFlow<PremiumEntitlement> =
        MutableStateFlow(PremiumEntitlement(isPremium = isPremium))
    override val plans: StateFlow<List<PremiumPlan>> = MutableStateFlow(emptyList())
    override val billingMessage: StateFlow<String?> = MutableStateFlow(null)

    override fun refresh() {}
    override fun restorePurchases() {}
    override fun startPurchase(activity: Activity, plan: PremiumPlan) {}
}

private class FakeScanQuotaRepository(
    used: Int = 0,
) : ScanQuotaRepository(null) {
    override val quota = MutableStateFlow(ScanQuota(used = used))

    override suspend fun canUseFreeScan(): Boolean = quota.value.hasFreeScans

    override suspend fun recordFreeScan() {
        quota.value = quota.value.copy(used = quota.value.used + 1)
    }
}

private fun fakeMealRepo() = object : MealRepository(null) {
    override suspend fun logMeal(meal: MealEntity): Long = 1L
    override suspend fun deleteMeal(id: Long) {}
}
