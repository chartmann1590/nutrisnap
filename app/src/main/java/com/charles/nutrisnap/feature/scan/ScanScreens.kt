package com.charles.nutrisnap.feature.scan

import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.ui.components.ConfidencePill
import com.charles.nutrisnap.ui.components.MacroTile
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.components.SecondaryButton
import com.charles.nutrisnap.ui.components.Stepper
import com.charles.nutrisnap.ui.theme.NutriTheme
import java.io.File
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    onClose: () -> Unit,
    onAnalyzed: (String) -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var flashEnabled by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val photoDir = remember { File(context.cacheDir, "scan_photos").apply { mkdirs() } }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val bitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
            bitmap?.let { bm -> viewModel.onCaptured(bm) }
        }
    }

    val camLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) onManualEntry()
    }

    LaunchedEffect(Unit) { camLauncher.launch(Manifest.permission.CAMERA) }

    // Start compiling the on-device engine now so it's ready by the time a photo is taken.
    LaunchedEffect(Unit) { viewModel.warmUp() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ScanEvent.NavigateToResult -> onAnalyzed(event.estimateKey)
                is ScanEvent.NavigateToEntry -> onManualEntry()
                else -> {}
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val provider = ProcessCameraProvider.getInstance(ctx)
                provider.addListener({
                    val cameraProvider = provider.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val builder = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.display.rotation)
                    builder.setFlashMode(
                        if (flashEnabled) ImageCapture.FLASH_MODE_ON
                        else ImageCapture.FLASH_MODE_OFF
                    )
                    val imageCaptureUseCase = builder.build()
                    imageCapture = imageCaptureUseCase
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCaptureUseCase,
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart).padding(16.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)).size(44.dp),
        ) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }

        IconButton(
            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier
                .align(Alignment.BottomStart).padding(24.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)).size(52.dp),
        ) { Icon(Icons.Rounded.PhotoLibrary, "Gallery", tint = Color.White) }

        IconButton(
            onClick = { flashEnabled = !flashEnabled },
            modifier = Modifier
                .align(Alignment.BottomEnd).padding(24.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50)).size(52.dp),
        ) { Icon(if (flashEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, "Flash", tint = Color.White) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter).padding(bottom = 32.dp).size(72.dp)
                .clip(RoundedCornerShape(50)).background(Color.White).padding(4.dp)
                .clip(RoundedCornerShape(50)).background(Color.White)
                .clickable {
                    imageCapture?.let { capture ->
                        val file = File(photoDir, "scan_${System.currentTimeMillis()}.jpg")
                        capture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    BitmapFactory.decodeFile(file.absolutePath)?.let {
                                        viewModel.onCaptured(it)
                                    }
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    onManualEntry()
                                }
                            },
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(62.dp).clip(RoundedCornerShape(50)).background(Color.White)) }

        if (state is ScanUiState.Analyzing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Pip(size = 110.dp, animated = true)
                Spacer(Modifier.height(20.dp))
                Text(
                    "Analyzing your food…",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your phone is reading the photo on-device.\nThis can take a few seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
        }
    }
}

@Composable
fun ScanResultScreen(
    estimateKey: String,
    onLogged: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var editableKcal by remember { mutableStateOf("") }
    var editableProtein by remember { mutableStateOf("") }
    var editableCarbs by remember { mutableStateOf("") }
    var editableFat by remember { mutableStateOf("") }

    // The estimate is handed over from the Scan screen's analysis via EstimateCache (this
    // destination has its own ViewModel instance, so it can't read the analyzing VM's state).
    val estimate = remember(estimateKey) { EstimateCache.get(estimateKey) }

    LaunchedEffect(estimate) {
        estimate?.let {
            editableKcal = it.kcal.toString()
            editableProtein = it.proteinG.toString()
            editableCarbs = it.carbsG.toString()
            editableFat = it.fatG.toString()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ScanEvent.Logged) onLogged()
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        if (estimate != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(estimate.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                ConfidencePill(percent = (estimate.confidence * 100).toInt())
            }

            Spacer(Modifier.height(16.dp))

            NutriCard(cornerRadius = 24.dp, padding = 18.dp, modifier = Modifier.fillMaxWidth()) {
                Text("CALORIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Stepper(value = editableKcal, unit = "kcal",
                    onMinus = { editableKcal = (editableKcal.toIntOrNull()?.let { (it - 10).coerceAtLeast(0) } ?: 0).toString() },
                    onPlus = { editableKcal = (editableKcal.toIntOrNull()?.let { it + 10 } ?: 0).toString() })
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MacroTile("Protein", "${editableProtein}g", NutriTheme.colors.protein, Modifier.weight(1f))
                MacroTile("Carbs", "${editableCarbs}g", NutriTheme.colors.carbs, Modifier.weight(1f))
                MacroTile("Fat", "${editableFat}g", NutriTheme.colors.fat, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Text("MEAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MealType.entries.forEach { type ->
                    val isSelected = selectedMealType == type
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .clickable { selectedMealType = type }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = "Add to diary", onClick = {
                val k = editableKcal.toIntOrNull() ?: estimate.kcal
                val p = editableProtein.toIntOrNull() ?: estimate.proteinG
                val c = editableCarbs.toIntOrNull() ?: estimate.carbsG
                val f = editableFat.toIntOrNull() ?: estimate.fatG
                viewModel.logMeal(estimate.copy(kcal = k, proteinG = p, carbsG = c, fatG = f), selectedMealType, null)
            }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Discard", onClick = onClose, modifier = Modifier.fillMaxWidth())
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Pip(size = 96.dp, animated = true)
            }
        }
    }
}