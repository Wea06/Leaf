package com.leaftodo.app.ui.theme

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.leaftodo.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.*

@Composable
fun PaperOverlay(effect: PaperEffect) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var dimensions by remember { mutableStateOf(IntSize.Zero) }
    var cachedSize by remember { mutableStateOf(IntSize.Zero) }
    var cachedDensity by remember { mutableFloatStateOf(0f) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(effect, dimensions, density) {
        if (effect != PaperEffect.FIBER || dimensions == IntSize.Zero ||
            (cachedSize == dimensions && cachedDensity == density)) return@LaunchedEffect
        if (bitmap != null) delay(250)
        val result = withContext(Dispatchers.Default) { renderFibers(dimensions, density) }
        bitmap = result
        cachedSize = dimensions
        cachedDensity = density
    }
    Canvas(Modifier.fillMaxSize().graphicsLayer().onSizeChanged { measured ->
        // Window bounds exclude the IME inset: opening the keyboard must not rebuild the texture.
        val manager = context.getSystemService(WindowManager::class.java)
        @Suppress("DEPRECATION")
        val full = if (Build.VERSION.SDK_INT >= 30) manager.currentWindowMetrics.bounds.let { IntSize(it.width(), it.height()) }
        else android.util.DisplayMetrics().let { manager.defaultDisplay.getRealMetrics(it); IntSize(it.widthPixels, it.heightPixels) }
        dimensions = if (full.width > 0 && full.height > 0) full else measured
    }) {
        when (effect) {
            PaperEffect.NONE -> Unit
            PaperEffect.FIBER -> bitmap?.let { drawImage(it, alpha = .6f) }
            PaperEffect.GRID -> {
                val step = 24f * density
                val width = max(1f, (.6f * density).roundToInt().toFloat())
                var position = (size.width % step) / 2
                var x = position.roundToInt() + width / 2
                while (x < size.width) {
                    drawLine(ink.copy(alpha = .09f), Offset(x, 0f), Offset(x, size.height), width)
                    position += step
                    x = position.roundToInt() + width / 2
                }
                position = (size.height % step) / 2
                var y = position.roundToInt() + width / 2
                while (y < size.height) {
                    drawLine(ink.copy(alpha = .09f), Offset(0f, y), Offset(size.width, y), width)
                    position += step
                    y = position.roundToInt() + width / 2
                }
            }
        }
    }
}

private fun renderFibers(size: IntSize, density: Float): ImageBitmap {
    val started = SystemClock.elapsedRealtime()
    val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    val blur = BlurMaskFilter(.6f * density, BlurMaskFilter.Blur.NORMAL)
    for (fiber in fiberSpecs(size.width / density, size.height / density)) {
        paint.color = if (fiber.dark) android.graphics.Color.rgb(110, 86, 62) else android.graphics.Color.rgb(255, 252, 245)
        paint.strokeWidth = fiber.width * density
        paint.maskFilter = if (fiber.soft) blur else null
        var x = fiber.x * density
        var y = fiber.y * density
        repeat(4) { segment ->
            val angle = fiber.angle + fiber.bend * segment / 4f
            val endX = x + cos(angle) * fiber.length * density / 4f
            val endY = y + sin(angle) * fiber.length * density / 4f
            paint.alpha = (255 * fiber.alpha * if (segment == 0 || segment == 3) .45f else 1f).roundToInt()
            canvas.drawLine(x, y, endX, endY, paint)
            x = endX; y = endY
        }
    }
    Log.d("LeafPaper", "${size.width}x${size.height}: ${SystemClock.elapsedRealtime() - started} ms")
    return bitmap.asImageBitmap()
}
