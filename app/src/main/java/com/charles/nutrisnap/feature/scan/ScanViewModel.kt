package com.charles.nutrisnap.feature.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.ai.FoodEstimate
import com.charles.nutrisnap.ai.GemmaEngine
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.db.MealEntity
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    data class Error(val message: String) : ScanUiState
}

sealed interface ScanEvent {
    data class NavigateToResult(val estimateKey: String) : ScanEvent
    data class NavigateToEntry(val mode: String, val bitmapKey: String?) : ScanEvent
    data class Logged(val navigated: Boolean = false) : ScanEvent
}

object BitmapCache {
    private val cache = mutableMapOf<String, Bitmap>()

    fun put(bitmap: Bitmap): String {
        val key = UUID.randomUUID().toString()
        cache[key] = bitmap
        return key
    }

    fun get(key: String): Bitmap? = cache[key]

    fun remove(key: String) = cache.remove(key)
}

/**
 * Carries the [FoodEstimate] across the Scan -> ScanResult navigation hop. Each destination
 * gets its own ViewModel instance, so the analysis result must be handed over by key rather
 * than held in ViewModel state.
 */
object EstimateCache {
    private val cache = mutableMapOf<String, FoodEstimate>()

    fun put(estimate: FoodEstimate): String {
        val key = UUID.randomUUID().toString()
        cache[key] = estimate
        return key
    }

    fun get(key: String): FoodEstimate? = cache[key]

    fun remove(key: String) = cache.remove(key)
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val gemmaEngine: GemmaEngine,
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ScanEvent>()
    val events: SharedFlow<ScanEvent> = _events.asSharedFlow()

    /** Compile the on-device engine ahead of time so the first analysis isn't a cold start. */
    fun warmUp() {
        if (gemmaEngine.isReady()) return
        viewModelScope.launch(Dispatchers.Default) {
            runCatching { gemmaEngine.warmUp() }
        }
    }

    fun onCaptured(bitmap: Bitmap, hint: String? = null) {
        _state.value = ScanUiState.Analyzing
        val bitmapKey = BitmapCache.put(bitmap)
        viewModelScope.launch(Dispatchers.Default) {
            val result = gemmaEngine.analyzeFood(bitmap.downscaleForInference(), hint)
            result.onSuccess { estimate ->
                if (estimate.confidence < 0.5f) {
                    _state.value = ScanUiState.Idle
                    _events.emit(ScanEvent.NavigateToEntry("scan_fallback", bitmapKey))
                } else {
                    val estimateKey = EstimateCache.put(estimate)
                    _state.value = ScanUiState.Result(estimate)
                    _events.emit(ScanEvent.NavigateToResult(estimateKey))
                }
            }.onFailure {
                _state.value = ScanUiState.Idle
                _events.emit(ScanEvent.NavigateToEntry("scan_fallback", bitmapKey))
            }
        }
    }

    fun onAnalyzed(bitmapKey: String, estimate: FoodEstimate) {
        _state.value = ScanUiState.Result(estimate)
    }

    fun logMeal(estimate: FoodEstimate, mealType: MealType, photoUri: String?) {
        viewModelScope.launch {
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