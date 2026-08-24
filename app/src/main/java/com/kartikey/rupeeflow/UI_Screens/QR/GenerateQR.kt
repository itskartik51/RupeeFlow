package com.kartikey.rupeeflow.UI_Screens.QR

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@Composable
fun PremiumQRCode(
    data: String,
    size: Dp = 190.dp,
    qrColor: Color = Color(0xFF000000), // Pure Solid Jet Black #000000
    backgroundColor: Color = Color(0xFFFFFFFF),
    cornerRadius: Dp = 18.dp
) {
    val bitmap = remember(data) {
        generateRoundedQRCode(data, 600, qrColor.toArgb(), backgroundColor.toArgb())
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .padding(14.dp), // Safe white breathing margin for instant camera lock
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                filterQuality = FilterQuality.None, // Zero-blur sharp black rendering
                modifier = Modifier.size(size - 28.dp)
            )
        }
    }
}

fun generateRoundedQRCode(
    content: String,
    sizePx: Int,
    color: Int = 0xFF000000.toInt(),
    bgColor: Int = 0xFFFFFFFF.toInt()
): Bitmap? {
    try {
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        
        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        
        // Pure White Background
        val bgPaint = Paint().apply { 
            this.color = bgColor
            isAntiAlias = false
        }
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

        // Solid Black Paint for data modules
        val dotPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }

        val modW = sizePx.toFloat() / matrixWidth
        val modH = sizePx.toFloat() / matrixHeight
        val dotRadius = modW * 0.35f

        for (x in 0 until matrixWidth) {
            for (y in 0 until matrixHeight) {
                if (bitMatrix[x, y]) {
                    val isFinderZone = (x < 8 && y < 8) || 
                                       (x >= matrixWidth - 8 && y < 8) || 
                                       (x < 8 && y >= matrixHeight - 8)

                    val rect = RectF(
                        x * modW,
                        y * modH,
                        (x + 1) * modW,
                        (y + 1) * modH
                    )

                    if (isFinderZone) {
                        // Scan-safe solid blocks for finder eyes
                        canvas.drawRect(rect, dotPaint)
                    } else {
                        // Premium micro-squircle rounded modules for data
                        rect.inset(modW * 0.05f, modH * 0.05f)
                        canvas.drawRoundRect(rect, dotRadius, dotRadius, dotPaint)
                    }
                }
            }
        }

        return bmp
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// ==========================================
// CUSTOM VECTOR ICONS (EXACT REPLICAS)
// ==========================================

@Composable
fun CustomDownloadIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.085f

        // Downward Arrow Stem
        drawLine(
            color = tint,
            start = Offset(size.width * 0.5f, size.height * 0.16f),
            end = Offset(size.width * 0.5f, size.height * 0.60f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Downward Arrow Head
        val arrowHead = Path().apply {
            moveTo(size.width * 0.33f, size.height * 0.44f)
            lineTo(size.width * 0.5f, size.height * 0.61f)
            lineTo(size.width * 0.67f, size.height * 0.44f)
        }
        drawPath(
            path = arrowHead,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Rounded Bottom U-Tray
        val tray = Path().apply {
            moveTo(size.width * 0.14f, size.height * 0.64f)
            lineTo(size.width * 0.14f, size.height * 0.74f)
            quadraticBezierTo(
                size.width * 0.14f, size.height * 0.86f,
                size.width * 0.26f, size.height * 0.86f
            )
            lineTo(size.width * 0.74f, size.height * 0.86f)
            quadraticBezierTo(
                size.width * 0.86f, size.height * 0.86f,
                size.width * 0.86f, size.height * 0.74f
            )
            lineTo(size.width * 0.86f, size.height * 0.64f)
        }
        drawPath(
            path = tray,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CustomShareExportIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.088f

        // Open Rounded Box (Exact 1:1 layout as user reference)
        val boxPath = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.24f)
            lineTo(size.width * 0.28f, size.height * 0.24f)
            quadraticBezierTo(
                size.width * 0.15f, size.height * 0.24f,
                size.width * 0.15f, size.height * 0.37f
            )
            lineTo(size.width * 0.15f, size.height * 0.73f)
            quadraticBezierTo(
                size.width * 0.15f, size.height * 0.86f,
                size.width * 0.28f, size.height * 0.86f
            )
            lineTo(size.width * 0.72f, size.height * 0.86f)
            quadraticBezierTo(
                size.width * 0.85f, size.height * 0.86f,
                size.width * 0.85f, size.height * 0.73f
            )
            lineTo(size.width * 0.85f, size.height * 0.52f)
        }
        drawPath(
            path = boxPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Diagonal 45° Arrow Stem emerging from center
        drawLine(
            color = tint,
            start = Offset(size.width * 0.46f, size.height * 0.54f),
            end = Offset(size.width * 0.83f, size.height * 0.17f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Arrow Head Pointing Top-Right
        val arrowHead = Path().apply {
            moveTo(size.width * 0.58f, size.height * 0.17f)
            lineTo(size.width * 0.83f, size.height * 0.17f)
            lineTo(size.width * 0.83f, size.height * 0.42f)
        }
        drawPath(
            path = arrowHead,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ==========================================
// STORAGE & SHARE HELPERS
// ==========================================

fun saveQRToGallery(context: Context, data: String, roomName: String) {
    val bitmap = generateRoundedQRCode(data, 800) ?: return
    try {
        val filename = "Contri_QR_${System.currentTimeMillis()}.png"
        val outputStream: OutputStream?
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RupeeFlow")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(imagesDir, "RupeeFlow").apply { if (!exists()) mkdirs() }
            val file = File(appDir, filename)
            outputStream = FileOutputStream(file)
        }

        outputStream?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Toast.makeText(context, "QR saved to Gallery!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save QR", Toast.LENGTH_SHORT).show()
    }
}

fun shareQRCode(context: Context, data: String, roomName: String, roomCode: String, pin: String) {
    val bitmap = generateRoundedQRCode(data, 800) ?: return
    try {
        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "contri_qr.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Join my Contri room '$roomName' on RupeeFlow!\n\nRoom Code: $roomCode\nPin: $pin"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Contri QR"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share QR", Toast.LENGTH_SHORT).show()
    }
}
