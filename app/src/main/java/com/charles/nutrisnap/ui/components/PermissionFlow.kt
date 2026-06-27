package com.charles.nutrisnap.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect

@Composable
fun PermissionRationale(
    permission: String,
    title: String,
    message: String,
    onGrant: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(onClick = onGrant) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        },
    )
}

@Composable
fun CameraPermissionGate(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
) {
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onGranted() else onDenied()
    }

    if (showRationale) {
        PermissionRationale(
            permission = Manifest.permission.CAMERA,
            title = "Camera access needed",
            message = "NutriSnap uses your camera to scan food labels and snap meals for AI analysis.",
            onGrant = {
                showRationale = false
                launcher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = {
                showRationale = false
                onDenied()
            },
        )
    }

    LaunchedEffect(Unit) {
        showRationale = true
    }
}
