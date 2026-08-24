package com.kartikey.rupeeflow.UI_Screens.QR

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
    size: Dp = 200.dp,
    qrColor: Color = Color(0xFF000000), // Solid Hard Black #000000
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
    color: Int = 0xFF000000.toInt(), // Pure hard black
    bgColor: Int = 0xFFFFFFFF.toInt()
): Bitmap? {
    try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        
        // Anti-aliased pure white background
        val bgPaint = Paint().apply { 
            this.color = bgColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Solid Black Dots Paint
        val dotPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }

        val moduleWidth = width.toFloat() / bitMatrix.width
        val moduleHeight = height.toFloat() / bitMatrix.height
        val cornerRadius = moduleWidth / 2.5f

        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                if (bitMatrix[x, y]) {
                    val rect = RectF(
                        x * moduleWidth,
                        y * moduleHeight,
                        (x + 1) * moduleWidth,
                        (y + 1) * moduleHeight
                    )
                    rect.inset(moduleWidth * 0.05f, moduleHeight * 0.05f)
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, dotPaint)
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
// CUSTOM VECTOR ICONS (EXACT USER REPLICAS)
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

        // Paper Airplane Outer Wings
        val plane = Path().apply {
            moveTo(size.width * 0.94f, size.height * 0.06f) // Top-Right Tip
            lineTo(size.width * 0.06f, size.height * 0.45f) // Left Wing
            lineTo(size.width * 0.40f, size.height * 0.58f) // Inner Crease
            lineTo(size.width * 0.55f, size.height * 0.94f) // Bottom Tip
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
            start = Offset(size.width * 0.94f, size.height * 0.06f),
            end = Offset(size.width * 0.40f, size.height * 0.58f),
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
