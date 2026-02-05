package com.hereliesaz.halfhashedkitty.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hereliesaz.halfhashedkitty.QrCodeAnalyzer

/**
 * A reusable Composable that displays a camera preview and scans for QR codes.
 * <p>
 * This screen handles:
 * <ul>
 *     <li>Requesting Camera runtime permissions.</li>
 *     <li>Initializing the CameraX library.</li>
 *     <li>Binding the camera preview to the lifecycle.</li>
 *     <li>Analyzing frames using the {@link QrCodeAnalyzer} to detect codes.</li>
 * </ul>
 * </p>
 *
 * @param instructionText Text to display below the camera preview.
 * @param onQrCodeScanned Callback function invoked when a QR code is detected. Passes the string content.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun ScannerScreen(instructionText: String, onQrCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Future for CameraProvider (async initialization).
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // State to track permission status.
    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }

    // Launcher for the permission request dialog.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    // Request permission on first launch if not granted.
    LaunchedEffect(key1 = true) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            // Embed the legacy View-based Camera Preview into Compose.
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val preview = Preview.Builder().build()
                    // Select back camera.
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    // Setup ImageAnalysis for QR processing.
                    val imageAnalysis = ImageAnalysis.Builder()
                        // STRATEGY_KEEP_ONLY_LATEST drops older frames if analysis is slow, keeping the UI responsive.
                        .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    // Assign the custom analyzer.
                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(ctx),
                        QrCodeAnalyzer { result ->
                            // Pass the result up.
                            onQrCodeScanned(result)
                        }
                    )

                    // Bind camera to lifecycle.
                    try {
                        cameraProviderFuture.get().bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis
                        )
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                    previewView
                }
            )
        } else {
            // Fallback UI if permission is denied.
            Text(
                text = "Camera permission is required to scan QR codes.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Instruction Text.
        Text(
            text = instructionText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}
