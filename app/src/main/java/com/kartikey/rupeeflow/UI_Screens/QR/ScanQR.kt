package com.kartikey.rupeeflow.UI_Screens.QR

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun ScanQRScreen(
    onBackClick: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val localView = LocalView.current
    
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) 
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    var isScanned by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val scanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )

                        val executor = Executors.newSingleThreadExecutor()

                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            @OptIn(ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isScanned) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { qrValue ->
                                                if (!isScanned) {
                                                    isScanned = true
                                                    // Premium physical touch feel on scan
                                                    localView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                    onQrScanned(qrValue)
                                                }
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Premium Overlay UI
            ScannerOverlay()
            
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                mainArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required to scan QR.", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Floating Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = 48.dp, start = 16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_animation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val rectSize = canvasWidth * 0.7f
        val rectLeft = (canvasWidth - rectSize) / 2f
        val rectTop = (canvasHeight - rectSize) / 2f
        
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            
            // Dark transparent background
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = size
            )

            // Punch the scanner hole (Clear out pixels)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(rectLeft, rectTop),
                size = androidx.compose.ui.geometry.Size(rectSize, rectSize),
                cornerRadius = CornerRadius(40f, 40f),
                blendMode = BlendMode.Clear
            )
            
            restoreToCount(checkPoint)
        }

        // Focus Box Border
        drawRoundRect(
            color = Color(0xFF2E7D32),
            topLeft = Offset(rectLeft, rectTop),
            size = androidx.compose.ui.geometry.Size(rectSize, rectSize),
            cornerRadius = CornerRadius(40f, 40f),
            style = Stroke(width = 4f)
        )

        val currentLaserY = rectTop + (rectSize * laserY)
        
        // Laser Core
        drawLine(
            color = Color(0xFF2E7D32).copy(alpha = 0.8f),
            start = Offset(rectLeft + 20f, currentLaserY),
            end = Offset(rectLeft + rectSize - 20f, currentLaserY),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        
        // Laser Glow
        drawLine(
            color = Color(0xFF2E7D32).copy(alpha = 0.3f),
            start = Offset(rectLeft + 20f, currentLaserY),
            end = Offset(rectLeft + rectSize - 20f, currentLaserY),
            strokeWidth = 20f,
            cap = StrokeCap.Round
        )
    }
}
