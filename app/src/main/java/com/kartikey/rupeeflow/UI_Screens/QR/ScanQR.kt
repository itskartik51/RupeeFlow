package com.kartikey.rupeeflow.UI_Screens.QR

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import android.view.HapticFeedbackConstants
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.kartikey.rupeeflow.UI_Screens.bounceClick
import java.util.concurrent.Executors

@Composable
fun ScanQRScreen(
    onBackClick: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val localView = LocalView.current
    
    val primaryNeon = MaterialTheme.colorScheme.primary
    val cardSurface = MaterialTheme.colorScheme.surface
    
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) 
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isScanned by remember { mutableStateOf(false) }

    // Gallery QR Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            try {
                val inputImage = InputImage.fromFilePath(context, imageUri)
                val scanner = BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        val qrValue = barcodes.firstOrNull()?.rawValue
                        if (!qrValue.isNullOrBlank() && !isScanned) {
                            isScanned = true
                            localView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            onQrScanned(qrValue)
                        } else {
                            Toast.makeText(context, "No valid QR Code found in image", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to scan image", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to read selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

            // GPay-Style Concentric 4-Corner Brackets & Soft Inset Camera Cutout
            ScannerOverlay(primaryColor = primaryNeon)
            
            // Full-Screen Floating UI Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = 76.dp), // Positioned above the bottom navigation bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar: Clean Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .bounceClick { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close, 
                            contentDescription = "Close", 
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Push "Upload from Gallery" cleanly below the viewfinder square
                Spacer(modifier = Modifier.fillMaxHeight(0.61f))

                // Upload from Gallery Pill Button
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .bounceClick { galleryLauncher.launch("image/*") }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Upload from Gallery",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom 4-Corner Rounded Information Card (Placed above the bottom bar)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Scan QR Code to Join any Contri",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Camera permission is required to scan QR.", color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryNeon)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun ScannerOverlay(primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_animation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val rectSize = canvasWidth * 0.74f
        val rectLeft = (canvasWidth - rectSize) / 2f
        val rectTop = canvasHeight * 0.16f
        val rectRight = rectLeft + rectSize
        val rectBottom = rectTop + rectSize
        
        // Exact Concentric Math: Inner Radius = Outer Radius - Inner Padding
        val outerRadius = 50f
        val innerPadding = 28f // Doubled gap for prominent floating bracket look
        val innerRadius = outerRadius - innerPadding // 22f for matching parallel curve

        val innerLeft = rectLeft + innerPadding
        val innerTop = rectTop + innerPadding
        val innerSize = rectSize - (innerPadding * 2f)

        // Punch hole out of dark translucent overlay
        with(drawContext.canvas.nativeCanvas) {
            val checkPoint = saveLayer(null, null)
            
            drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                size = size
            )

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(innerLeft, innerTop),
                size = androidx.compose.ui.geometry.Size(innerSize, innerSize),
                cornerRadius = CornerRadius(innerRadius, innerRadius),
                blendMode = BlendMode.Clear
            )
            
            restoreToCount(checkPoint)
        }

        // 4 GPay-Style Bold Disconnected Rounded Corner Brackets
        val cornerArm = 85f
        
        val cornerPath = Path().apply {
            // 1. Top-Left Corner
            moveTo(rectLeft, rectTop + cornerArm)
            lineTo(rectLeft, rectTop + outerRadius)
            quadraticBezierTo(rectLeft, rectTop, rectLeft + outerRadius, rectTop)
            lineTo(rectLeft + cornerArm, rectTop)

            // 2. Top-Right Corner
            moveTo(rectRight - cornerArm, rectTop)
            lineTo(rectRight - outerRadius, rectTop)
            quadraticBezierTo(rectRight, rectTop, rectRight, rectTop + outerRadius)
            lineTo(rectRight, rectTop + cornerArm)

            // 3. Bottom-Right Corner
            moveTo(rectRight, rectBottom - cornerArm)
            lineTo(rectRight, rectBottom - outerRadius)
            quadraticBezierTo(rectRight, rectBottom, rectRight - outerRadius, rectBottom)
            lineTo(rectRight - cornerArm, rectBottom)

            // 4. Bottom-Left Corner
            moveTo(rectLeft + cornerArm, rectBottom)
            lineTo(rectLeft + outerRadius, rectBottom)
            quadraticBezierTo(rectLeft, rectBottom, rectLeft, rectBottom - outerRadius)
            lineTo(rectLeft, rectBottom - cornerArm)
        }

        drawPath(
            path = cornerPath,
            color = primaryColor,
            style = Stroke(width = 15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Animated Neon Laser Line
        val currentLaserY = innerTop + (innerSize * laserY)
        
        // Laser Core Beam
        drawLine(
            color = primaryColor.copy(alpha = 0.95f),
            start = Offset(innerLeft + 16f, currentLaserY),
            end = Offset(innerLeft + innerSize - 16f, currentLaserY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
        
        // Laser Soft Glow
        drawLine(
            color = primaryColor.copy(alpha = 0.35f),
            start = Offset(innerLeft + 16f, currentLaserY),
            end = Offset(innerLeft + innerSize - 16f, currentLaserY),
            strokeWidth = 18f,
            cap = StrokeCap.Round
        )
    }
}
