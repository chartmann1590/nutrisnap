package com.charles.nutrisnap.feature.scan

import android.graphics.Bitmap
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.ads.InterstitialAdManager
import com.charles.nutrisnap.ai.FoodEstimate
import com.charles.nutrisnap.ai.GemmaEngine
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.PipEvent
import com.charles.nutrisnap.data.PipEventBus
import com.charles.nutrisnap.data.PremiumAccess
import com.charles.nutrisnap.data.PremiumEntitlement
import com.charles.nutrisnap.data.PremiumPlan
import com.charles.nutrisnap.data.ScanQuota
import com.charles.nutrisnap.data.ScanQuotaRepository
import com.charles.nutrisnap.data.badge.BadgeDetector
import com.charles.nutrisnap.data.challenge.DailyChallengeRepository
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.data.report.AiReportApi
import com.charles.nutrisnap.data.report.AiReportCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

private fun Bitmap.downscaleForInference(maxPx: Int = 768): Bitmap {
    val (w, h) = if (width > height) {
        maxPx to ((height * maxPx) / width)
    } else {
        ((width * maxPx) / height) to maxPx
    }
    return Bitmap.createScaledBitmap(this, w, h, true)
}

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Capturing : ScanUiState
    data object Analyzing : ScanUiState
    data class Result(val estimate: FoodEstimate) : ScanUiState
    data object Paywall : ScanUiState
    data class Error(val message: String) : ScanUiState
}

data class ScanAccessUiState(
    val entitlement: PremiumEntitlement = PremiumEntitlement(),
    val quota: ScanQuota = ScanQuota(),
    val plans: List<PremiumPlan> = emptyList(),
    val billingMessage: String? = null,
) {
    val isPremium: Boolean
        get() = entitlement.isPremium

    val canUseAiScan: Boolean
        get() = isPremium || quota.hasFreeScans
}

sealed interface ScanEvent {
    data class NavigateToResult(val estimateKey: String) : ScanEvent
    data class NavigateToEntry(val mode: String, val bitmapKey: String?) : ScanEvent
    data class Logged(val navigated: Boolean = false) : ScanEvent
}

object BitmapCache {
    private const val MAX_ENTRIES = 3
    private val cache = LinkedHashMap<String, Bitmap>()

    @Synchronized
    fun put(bitmap: Bitmap): String {
        val key = UUID.randomUUID().toString()
        cache[key] = bitmap
        while (cache.size > MAX_ENTRIES) {
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
        }
        return key
    }

    @Synchronized
    fun get(key: String): Bitmap? = cache[key]

    @Synchronized
    fun remove(key: String) = cache.remove(key)
}

/**
 * Carries the [FoodEstimate] across the Scan -> ScanResult navigation hop. Each destination
 * gets its own ViewModel instance, so the analysis result must be handed over by key rather
 * than held in ViewModel state.
 */
object EstimateCache {
    private const val MAX_ENTRIES = 5
    private val cache = LinkedHashMap<String, FoodEstimate>()
    private val photoUris = mutableMapOf<String, String>()

    @Synchronized
    fun put(estimate: FoodEstimate, photoUri: String? = null): String {
        val key = UUID.randomUUID().toString()
        cache[key] = estimate
        if (photoUri != null) photoUris[key] = photoUri
        while (cache.size > MAX_ENTRIES) {
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
            photoUris.remove(oldestKey)
        }
        return key
    }

    @Synchronized
    fun get(key: String): FoodEstimate? = cache[key]

    @Synchronized
    fun getPhotoUri(key: String): String? = photoUris[key]

    @Synchronized
    fun remove(key: String) {
        cache.remove(key)
        photoUris.remove(key)
    }
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val gemmaEngine: GemmaEngine,
    private val mealRepository: MealRepository,
    private val premiumAccess: PremiumAccess,
    private val scanQuotaRepository: ScanQuotaRepository,
    private val badgeDetector: BadgeDetector,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val pipEventBus: PipEventBus,
    private val interstitialAdManager: InterstitialAdManager,
    private val aiReportApi: AiReportApi,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>()
    val events: SharedFlow<ScanEvent> = _events.asSharedFlow()

    sealed interface ReportSubmitState {
        data object Idle : ReportSubmitState
        data object Submitting : ReportSubmitState
        data object Success : ReportSubmitState
        data class Error(val message: String) : ReportSubmitState
    }

    private val _reportState = MutableStateFlow<ReportSubmitState>(ReportSubmitState.Idle)
    val reportState: StateFlow<ReportSubmitState> = _reportState.asStateFlow()

    fun submitReport(estimate: FoodEstimate, category: AiReportCategory, description: String) {
        viewModelScope.launch {
            _reportState.value = ReportSubmitState.Submitting
            runCatching { aiReportApi.submitReport(category, description, estimate) }
                .onSuccess { _reportState.value = ReportSubmitState.Success }
                .onFailure {
                    _reportState.value = ReportSubmitState.Error(it.message ?: "Failed to send report")
                }
        }
    }

    fun resetReportState() {
        _reportState.value = ReportSubmitState.Idle
    }

    val accessState: StateFlow<ScanAccessUiState> = combine(
        premiumAccess.entitlement,
        scanQuotaRepository.quota,
        premiumAccess.plans,
        premiumAccess.billingMessage,
    ) { entitlement, quota, plans, billingMessage ->
        ScanAccessUiState(
            entitlement = entitlement,
            quota = quota,
            plans = plans,
            billingMessage = billingMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanAccessUiState(),
    )

    /** Compile the on-device engine ahead of time so the first analysis isn't a cold start. */
    fun warmUp() {
        if (gemmaEngine.isReady()) return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { gemmaEngine.warmUp() }
        }
    }

    fun onCaptured(bitmap: Bitmap, hint: String? = null, photoUri: String? = null) {
        viewModelScope.launch {
            val isPremium = premiumAccess.entitlement.value.isPremium
            if (!isPremium && !scanQuotaRepository.canUseFreeScan()) {
                _state.value = ScanUiState.Paywall
                return@launch
            }

            _state.value = ScanUiState.Analyzing
            val bitmapKey = BitmapCache.put(bitmap)
            val result = withContext(Dispatchers.Default) {
                gemmaEngine.analyzeFood(bitmap.downscaleForInference(), hint)
            }
            if (!isPremium) {
                scanQuotaRepository.recordFreeScan()
            }
            result.onSuccess { estimate ->
                if (estimate.confidence < 0.5f) {
                    _state.value = ScanUiState.Idle
                    _events.emit(ScanEvent.NavigateToEntry("scan_fallback", bitmapKey))
                } else {
                    val estimateKey = EstimateCache.put(estimate, photoUri)
                    _state.value = ScanUiState.Result(estimate)
                    _events.emit(ScanEvent.NavigateToResult(estimateKey))
                }
            }.onFailure {
                _state.value = ScanUiState.Idle
                _events.emit(ScanEvent.NavigateToEntry("scan_fallback", bitmapKey))
            }
        }
    }

    fun buyPremium(activity: Activity, plan: PremiumPlan) {
        premiumAccess.startPurchase(activity, plan)
    }

    /** Shows an interstitial ad every few meal logs; premium users never see ads. */
    fun maybeShowInterstitial(activity: Activity, onDone: () -> Unit) {
        if (premiumAccess.entitlement.value.isPremium) {
            onDone()
        } else {
            interstitialAdManager.maybeShow(activity, onDone)
        }
    }

    fun restorePurchases() {
        premiumAccess.restorePurchases()
    }

    fun onAnalyzed(bitmapKey: String, estimate: FoodEstimate) {
        _state.value = ScanUiState.Result(estimate)
    }

    fun logMeal(estimate: FoodEstimate, mealType: MealType, photoUri: String?) {
        viewModelScope.launch {
            val isFirstMeal = mealRepository.observeTodayMeals().first().isEmpty()
            val now = System.currentTimeMillis()
            val meal = MealEntity(
                timestampMs = now,
                mealType = mealType,
                name = estimate.name,
                totalKcal = estimate.kcal,
                proteinG = estimate.proteinG,
                carbsG = estimate.carbsG,
                fatG = estimate.fatG,
                photoUri = photoUri,
                source = MealSource.SCAN,
                confidence = estimate.confidence,
            )
            mealRepository.logMeal(meal)
            // Gamification
            if (isFirstMeal) pipEventBus.emit(PipEvent.FirstMealOfDay)
            pipEventBus.emit(PipEvent.MealLogged)
            badgeDetector.checkAndAward()
            dailyChallengeRepository.checkAndComplete(mealRepository.todayEpochDay())
            _events.emit(ScanEvent.Logged())
        }
    }

    fun resetState() {
        _state.value = ScanUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
    }
}
