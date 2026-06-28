package com.charles.nutrisnap.feature.pip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.ai.ChatSession
import com.charles.nutrisnap.ai.GemmaEngine
import com.charles.nutrisnap.data.ChatRepository
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.db.ChatMessageEntity
import com.charles.nutrisnap.data.db.ChatRole
import com.charles.nutrisnap.ui.components.PipMood
import com.charles.nutrisnap.ui.sound.PipVoiceManager
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
    private val pipVoiceManager: PipVoiceManager,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {

    companion object {
        private const val RECENT_HISTORY = 6
        private const val FALLBACK =
            "Oops — my brain got a little tangled there. Mind trying again? \uD83E\uDD7A"
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

    val pipVoiceEnabled: StateFlow<Boolean> = prefs.pipVoiceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun speak(text: String) {
        pipVoiceManager.speak(text)
    }

    private var session: ChatSession? = null

    init {
        viewModelScope.launch {
            session = createSession()
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return
        viewModelScope.launch {
            chatRepository.append(ChatRole.USER, trimmed)
            _isGenerating.value = true
            _pipMood.value = PipMood.Thinking
            val s = session ?: createSession().also { session = it }
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

    private suspend fun createSession(): ChatSession? {
        val context = PipContextBuilder.build(snapshotSource.snapshot())
        val recent = chatRepository.recent(RECENT_HISTORY)
        val excerpt = if (recent.isEmpty()) "" else {
            "\n\nRecent conversation:\n" + recent.joinToString("\n") {
                val who = if (it.role == ChatRole.USER) "User" else "Pip"
                "$who: ${it.text}"
            }
        }
        val systemInstruction = PipPersona.SYSTEM + "\n\n" + context + excerpt
        return runCatching { engine.startChat(systemInstruction) }.getOrNull()
    }

    override fun onCleared() {
        session?.close()
        session = null
    }
}
