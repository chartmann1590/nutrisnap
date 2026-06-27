package com.charles.nutrisnap

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.charles.nutrisnap.ai.LiteRtGemmaEngine
import com.charles.nutrisnap.ui.NutriSnapApp as NutriSnapAppUi
import com.charles.nutrisnap.ui.theme.NutriSnapTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), ComponentCallbacks2 {

    @Inject
    lateinit var gemmaEngine: LiteRtGemmaEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NutriSnapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NutriSnapAppUi()
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == 60 || level == 80) {
            gemmaEngine.release()
        }
    }
}
