package com.charles.nutrisnap.ui.sound

import android.content.Context
import android.speech.tts.TextToSpeech
import com.charles.nutrisnap.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipVoiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPreferencesRepository,
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context, this)
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
        val enabled = runBlocking { userPrefsRepository.pipVoiceEnabled.first() }
        if (!ready || !enabled) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pip_${System.currentTimeMillis()}")
    }

    fun stop() = tts?.stop()

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
