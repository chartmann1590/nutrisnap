package com.charles.nutrisnap.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.ai.LiteRtGemmaEngine
import com.charles.nutrisnap.data.ModelRepository
import com.charles.nutrisnap.data.ModelState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val gemmaEngine: LiteRtGemmaEngine,
) : ViewModel() {

    private val _state = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val state: StateFlow<ModelState> = _state

    private val _verified = MutableStateFlow(false)
    val verified: StateFlow<Boolean> = _verified

    init {
        if (modelRepository.isReady()) {
            onReady(modelRepository.lastPath() ?: "")
        } else {
            viewModelScope.launch {
                modelRepository.state.collect { s ->
                    when (s) {
                        is ModelState.Ready -> onReady(s.path)
                        is ModelState.Downloading -> _state.value = s
                        is ModelState.Failed -> _state.value = s
                        ModelState.NotDownloaded -> _state.value = ModelState.NotDownloaded
                        is ModelState.Verifying -> {} // not emitted by repo
                    }
                }
            }
        }
    }

    private fun onReady(path: String) {
        _state.value = ModelState.Verifying(path)
        viewModelScope.launch {
            gemmaEngine.verifyModel().fold(
                onSuccess = {
                    _state.value = ModelState.Ready(path)
                    _verified.value = true
                },
                onFailure = { e ->
                    modelRepository.markCorrupt(path)
                    _state.value = ModelState.Failed(
                        "Model verification failed (${e.message}). Please re-download."
                    )
                },
            )
        }
    }

    fun start() = modelRepository.startDownload()
    fun retry() = modelRepository.retryDownload()
}
