package com.charles.nutrisnap.ui.sound

import android.content.Context
import android.speech.tts.TextToSpeech
import com.charles.nutrisnap.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipVoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPreferencesRepository,
) : TextToSpeech.OnInitListener {

    @Volatile private var voiceEnabled = false   // default matches DataStore default

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context, this)
        scope.launch {
            userPrefsRepository.pipVoiceEnabled.collect { voiceEnabled = it }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setPitch(1.3f)
            tts?.setSpeechRate(0.9f)
            ready = true
        }
    }

    fun speak(text: String) {
        if (!ready || !voiceEnabled) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pip_${System.currentTimeMillis()}")
    }

    fun stop() = tts?.stop()

    fun shutdown() {
        scope.cancel()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
