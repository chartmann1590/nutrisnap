package com.charles.nutrisnap.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.charles.nutrisnap.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipSoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPrefsRepository: UserPreferencesRepository,
) {
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<PipSound, Int> = PipSound.values().associate { sound ->
        sound to soundPool.load(context, sound.rawResId, 1)
    }

    fun play(sound: PipSound) {
        val enabled = runBlocking { userPrefsRepository.pipSoundsEnabled.first() }
        if (!enabled) return
        soundIds[sound]?.let { id ->
            if (id != 0) soundPool.play(id, 0.7f, 0.7f, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
