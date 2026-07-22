package com.kartikey.rupeeflow.UI_Screens.QR

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun PremiumQRCode(
    data: String,
    size: Dp = 200.dp,
    qrColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val bitmap = remember(data) {
        generateRoundedQRCode(data, 600, qrColor.toArgb(), backgroundColor.toArgb())
    }

    Box(contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier.size(size)
            )
        }
    }
}

private fun generateRoundedQRCode(
    content: String,
    sizePx: Int,
    color: Int,
    bgColor: Int
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
        
        // Premium Anti-aliased Background
        val bgPaint = Paint().apply { 
            this.color = bgColor
            isAntiAlias = true
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Smooth Round Dots Paint
        val dotPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }

        val moduleWidth = width.toFloat() / bitMatrix.width
        val moduleHeight = height.toFloat() / bitMatrix.height
        val cornerRadius = moduleWidth / 2.5f // Creates the premium roundish feel

        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                if (bitMatrix[x, y]) {
                    val rect = RectF(
                        x * moduleWidth,
                        y * moduleHeight,
                        (x + 1) * moduleWidth,
                        (y + 1) * moduleHeight
                    )
                    // Slightly inset to make them look like individual rounded blocks
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
