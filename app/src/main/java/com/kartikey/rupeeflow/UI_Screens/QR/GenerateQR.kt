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
    size: Dp = 185.dp,
    qrColor: Color = Color(0xFF000000), // Pure Solid Hard Black
    backgroundColor: Color = Color(0xFFFFFFFF),
    cornerRadius: Dp = 16.dp
) {
    val bitmap = remember(data) {
        generateRoundedQRCode(data, 600, qrColor.toArgb(), backgroundColor.toArgb())
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                filterQuality = FilterQuality.None, // Prevents subpixel blur and keeps pitch black contrast
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(cornerRadius))
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
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
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
            isAntiAlias = false
        }

        val moduleWidth = sizePx.toFloat() / matrixWidth
        val moduleHeight = sizePx.toFloat() / matrixHeight

        // Draw regular data modules excluding 3 corner finder zones (7x7)
        for (x in 0 until matrixWidth) {
            for (y in 0 until matrixHeight) {
                val isTopLeft = x < 7 && y < 7
                val isTopRight = x >= matrixWidth - 7 && y < 7
                val isBottomLeft = x < 7 && y >= matrixHeight - 7

                if (!isTopLeft && !isTopRight && !isBottomLeft && bitMatrix[x, y]) {
                    canvas.drawRect(
                        x * moduleWidth,
                        y * moduleHeight,
                        (x + 1) * moduleWidth,
                        (y + 1) * moduleHeight,
                        dotPaint
                    )
                }
            }
        }

        // Custom Parallel Rounded Corner Finder Eyes
        val cornerPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val cornerRadius = moduleWidth * 2.8f
        val innerCornerRadius = moduleWidth * 1.5f

        // Helper to draw single directional eye
        fun drawDirectionalEye(startX: Float, startY: Float, outerRadii: FloatArray, centerRadii: FloatArray) {
            val outerRect = RectF(startX, startY, startX + (7 * moduleWidth), startY + (7 * moduleHeight))
            val cutoutRect = RectF(startX + moduleWidth, startY + moduleHeight, startX + (6 * moduleWidth), startY + (6 * moduleHeight))
            val centerRect = RectF(startX + (2 * moduleWidth), startY + (2 * moduleHeight), startX + (5 * moduleWidth), startY + (5 * moduleHeight))

            val outerPath = AndroidPath().apply {
                addRoundRect(outerRect, outerRadii, AndroidPath.Direction.CW)
            }
            val cutoutPath = AndroidPath().apply {
                addRoundRect(cutoutRect, innerCornerRadius, innerCornerRadius, AndroidPath.Direction.CW)
            }
            outerPath.op(cutoutPath, AndroidPath.Op.DIFFERENCE)
            canvas.drawPath(outerPath, cornerPaint)

            val centerPath = AndroidPath().apply {
                addRoundRect(centerRect, centerRadii, AndroidPath.Direction.CW)
            }
            canvas.drawPath(centerPath, cornerPaint)
        }

        // 1. Top-Left Eye (Outer Top-Left Corner Deep Rounded Parallel to White Card)
        drawDirectionalEye(
            startX = 0f,
            startY = 0f,
            outerRadii = floatArrayOf(cornerRadius, cornerRadius, 8f, 8f, 8f, 8f, 8f, 8f),
            centerRadii = floatArrayOf(innerCornerRadius, innerCornerRadius, 4f, 4f, 4f, 4f, 4f, 4f)
        )

        // 2. Top-Right Eye (Outer Top-Right Corner Deep Rounded)
        drawDirectionalEye(
            startX = (matrixWidth - 7) * moduleWidth,
            startY = 0f,
            outerRadii = floatArrayOf(8f, 8f, cornerRadius, cornerRadius, 8f, 8f, 8f, 8f),
            centerRadii = floatArrayOf(4f, 4f, innerCornerRadius, innerCornerRadius, 4f, 4f, 4f, 4f)
        )

        // 3. Bottom-Left Eye (Outer Bottom-Left Corner Deep Rounded)
        drawDirectionalEye(
            startX = 0f,
            startY = (matrixHeight - 7) * moduleHeight,
            outerRadii = floatArrayOf(8f, 8f, 8f, 8f, 8f, 8f, cornerRadius, cornerRadius),
            centerRadii = floatArrayOf(4f, 4f, 4f, 4f, 4f, 4f, innerCornerRadius, innerCornerRadius)
        )

        return bmp
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// ==========================================
// CUSTOM VECTOR ICONS (CLEAN & SUBTLE)
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
            moveTo(size.width * 0.13f, size.height * 0.64f)
            lineTo(size.width * 0.13f, size.height * 0.74f)
            quadraticBezierTo(
                size.width * 0.13f, size.height * 0.86f,
                size.width * 0.25f, size.height * 0.86f
            )
            lineTo(size.width * 0.75f, size.height * 0.86f)
            quadraticBezierTo(
                size.width * 0.87f, size.height * 0.86f,
                size.width * 0.87f, size.height * 0.74f
            )
            lineTo(size.width * 0.87f, size.height * 0.64f)
        }
        drawPath(
            path = tray,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CustomPaperPlaneIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.085f

        // Scaled inward to make it minimal and non-aggressive
        val plane = Path().apply {
            moveTo(size.width * 0.88f, size.height * 0.12f) // Top-Right Tip
            lineTo(size.width * 0.12f, size.height * 0.48f) // Left Wing
            lineTo(size.width * 0.42f, size.height * 0.58f) // Inner Fold
            lineTo(size.width * 0.52f, size.height * 0.88f) // Bottom Tip
            close()
        }
        drawPath(
            path = plane,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Center Fold Crease Line
        drawLine(
            color = tint,
            start = Offset(size.width * 0.88f, size.height * 0.12f),
            end = Offset(size.width * 0.42f, size.height * 0.58f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
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
