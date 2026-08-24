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
    qrColor: Color = Color(0xFF000000), // Pure Solid Hard Black #000000
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
                filterQuality = FilterQuality.None, // Zero-blur sharp jet black rendering
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
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        
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

        val modW = sizePx.toFloat() / matrixWidth
        val modH = sizePx.toFloat() / matrixHeight

        // 1. Draw regular data modules — Completely skipping standard 7x7 corner eyes
        for (x in 0 until matrixWidth) {
            for (y in 0 until matrixHeight) {
                val isTopLeft = x < 7 && y < 7
                val isTopRight = x >= matrixWidth - 7 && y < 7
                val isBottomLeft = x < 7 && y >= matrixHeight - 7

                if (!isTopLeft && !isTopRight && !isBottomLeft && bitMatrix[x, y]) {
                    canvas.drawRect(
                        x * modW,
                        y * modH,
                        (x + 1) * modW,
                        (y + 1) * modH,
                        dotPaint
                    )
                }
            }
        }

        // 2. Custom Finder Eyes (Parallel directional rounded squircle design)
        val eyePaint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val holePaint = Paint().apply {
            this.color = bgColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val cornerRadius = modW * 3.2f
        val innerRadius = modW * 2.1f
        val centerRadius = modW * 1.5f

        // Helper to render custom 3-layer directional squircle eye
        fun drawDirectionalEye(
            startX: Float, 
            startY: Float, 
            outerRadii: FloatArray, 
            innerRadii: FloatArray, 
            centerRadii: FloatArray
        ) {
            // Outer Frame (7x7)
            val outerRect = RectF(startX, startY, startX + (7 * modW), startY + (7 * modH))
            val outerPath = AndroidPath().apply { addRoundRect(outerRect, outerRadii, AndroidPath.Direction.CW) }
            canvas.drawPath(outerPath, eyePaint)

            // Inner Cutout (5x5 White Box)
            val innerRect = RectF(startX + modW, startY + modH, startX + (6 * modW), startY + (6 * modH))
            val innerPath = AndroidPath().apply { addRoundRect(innerRect, innerRadii, AndroidPath.Direction.CW) }
            canvas.drawPath(innerPath, holePaint)

            // Center Box (3x3 Solid Black)
            val centerRect = RectF(startX + (2 * modW), startY + (2 * modH), startX + (5 * modW), startY + (5 * modH))
            val centerPath = AndroidPath().apply { addRoundRect(centerRect, centerRadii, AndroidPath.Direction.CW) }
            canvas.drawPath(centerPath, eyePaint)
        }

        // Top-Left Eye (Top-Left outer corner deeply rounded parallel to card)
        drawDirectionalEye(
            startX = 0f,
            startY = 0f,
            outerRadii = floatArrayOf(cornerRadius, cornerRadius, 6f, 6f, 6f, 6f, 6f, 6f),
            innerRadii = floatArrayOf(innerRadius, innerRadius, 4f, 4f, 4f, 4f, 4f, 4f),
            centerRadii = floatArrayOf(centerRadius, centerRadius, 3f, 3f, 3f, 3f, 3f, 3f)
        )

        // Top-Right Eye (Top-Right outer corner deeply rounded parallel to card)
        drawDirectionalEye(
            startX = (matrixWidth - 7) * modW,
            startY = 0f,
            outerRadii = floatArrayOf(6f, 6f, cornerRadius, cornerRadius, 6f, 6f, 6f, 6f),
            innerRadii = floatArrayOf(4f, 4f, innerRadius, innerRadius, 4f, 4f, 4f, 4f),
            centerRadii = floatArrayOf(3f, 3f, centerRadius, centerRadius, 3f, 3f, 3f, 3f)
        )

        // Bottom-Left Eye (Bottom-Left outer corner deeply rounded parallel to card)
        drawDirectionalEye(
            startX = 0f,
            startY = (matrixHeight - 7) * modH,
            outerRadii = floatArrayOf(6f, 6f, 6f, 6f, 6f, 6f, cornerRadius, cornerRadius),
            innerRadii = floatArrayOf(4f, 4f, 4f, 4f, 4f, 4f, innerRadius, innerRadius),
            centerRadii = floatArrayOf(3f, 3f, 3f, 3f, 3f, 3f, centerRadius, centerRadius)
        )

        return bmp
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

// ==========================================
// CUSTOM VECTOR ICONS
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
fun CustomShareExportIcon(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.085f

        // Rounded Outer Box with Top-Right Opening
        val boxPath = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.20f)
            lineTo(size.width * 0.26f, size.height * 0.20f)
            quadraticBezierTo(
                size.width * 0.16f, size.height * 0.20f,
                size.width * 0.16f, size.height * 0.30f
            )
            lineTo(size.width * 0.16f, size.height * 0.76f)
            quadraticBezierTo(
                size.width * 0.16f, size.height * 0.86f,
                size.width * 0.26f, size.height * 0.86f
            )
            lineTo(size.width * 0.74f, size.height * 0.86f)
            quadraticBezierTo(
                size.width * 0.84f, size.height * 0.86f,
                size.width * 0.84f, size.height * 0.76f
            )
            lineTo(size.width * 0.84f, size.height * 0.50f)
        }
        drawPath(
            path = boxPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Diagonal Export Arrow Stem
        drawLine(
            color = tint,
            start = Offset(size.width * 0.44f, size.height * 0.56f),
            end = Offset(size.width * 0.83f, size.height * 0.18f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Diagonal Arrow Head (Pointing Top-Right)
        val arrowHead = Path().apply {
            moveTo(size.width * 0.58f, size.height * 0.18f)
            lineTo(size.width * 0.84f, size.height * 0.18f)
            lineTo(size.width * 0.84f, size.height * 0.44f)
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
