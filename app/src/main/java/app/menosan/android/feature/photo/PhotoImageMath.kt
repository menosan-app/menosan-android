package app.menosan.android.feature.photo

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

data class PixelSize(val width: Int, val height: Int) {
    val longSide: Int get() = max(width, height)
}

data class ExifTransform(val rotationDegrees: Int, val flipHorizontal: Boolean) {
    val swapsSides: Boolean get() = rotationDegrees == 90 || rotationDegrees == 270
    val isIdentity: Boolean get() = rotationDegrees == 0 && !flipHorizontal
}

class EncodedJpeg(val bytes: ByteArray, val longSide: Int, val quality: Int)

data class PixelRect(val x: Int, val y: Int, val width: Int, val height: Int)

enum class CropHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val isFull: Boolean get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    fun dragged(handle: CropHandle, dx: Float, dy: Float): CropRect = when (handle) {
        CropHandle.MOVE -> {
            val width = right - left
            val height = bottom - top
            val newLeft = (left + dx).coerceIn(0f, 1f - width)
            val newTop = (top + dy).coerceIn(0f, 1f - height)
            CropRect(newLeft, newTop, newLeft + width, newTop + height)
        }
        CropHandle.TOP_LEFT -> copy(left = movedStart(left, dx, right), top = movedStart(top, dy, bottom))
        CropHandle.TOP_RIGHT -> copy(right = movedEnd(right, dx, left), top = movedStart(top, dy, bottom))
        CropHandle.BOTTOM_LEFT -> copy(left = movedStart(left, dx, right), bottom = movedEnd(bottom, dy, top))
        CropHandle.BOTTOM_RIGHT -> copy(right = movedEnd(right, dx, left), bottom = movedEnd(bottom, dy, top))
    }

    fun handleAt(x: Float, y: Float, displayWidth: Float, displayHeight: Float, touchRadius: Float): CropHandle? {
        val corners = listOf(
            CropHandle.TOP_LEFT to (left to top),
            CropHandle.TOP_RIGHT to (right to top),
            CropHandle.BOTTOM_LEFT to (left to bottom),
            CropHandle.BOTTOM_RIGHT to (right to bottom),
        )
        val nearest = corners.minByOrNull { (_, corner) -> distance(x, y, corner.first * displayWidth, corner.second * displayHeight) }
        if (nearest != null) {
            val (handle, corner) = nearest
            if (distance(x, y, corner.first * displayWidth, corner.second * displayHeight) <= touchRadius) return handle
        }
        val inside = x in left * displayWidth..right * displayWidth && y in top * displayHeight..bottom * displayHeight
        return if (inside) CropHandle.MOVE else null
    }

    fun toPixels(size: PixelSize): PixelRect {
        val x = (left * size.width).roundToInt().coerceIn(0, size.width - 1)
        val y = (top * size.height).roundToInt().coerceIn(0, size.height - 1)
        val endX = (right * size.width).roundToInt().coerceIn(x + 1, size.width)
        val endY = (bottom * size.height).roundToInt().coerceIn(y + 1, size.height)
        return PixelRect(x, y, endX - x, endY - y)
    }

    private fun movedStart(start: Float, delta: Float, end: Float) = (start + delta).coerceIn(0f, end - MIN_SIZE)

    private fun movedEnd(end: Float, delta: Float, start: Float) = (end + delta).coerceIn(start + MIN_SIZE, 1f)

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float = hypot(x1 - x2, y1 - y2)

    companion object {
        const val MIN_SIZE = 0.15f

        val FULL = CropRect(0f, 0f, 1f, 1f)
    }
}

object PhotoImageMath {
    const val MAX_LONG_SIDE = 1280

    const val START_QUALITY = 80
    const val MIN_QUALITY = 50
    const val QUALITY_STEP = 10

    const val MAX_BYTES = 2 * 1024 * 1024 - 64 * 1024

    const val SHRINK_FACTOR = 0.75

    const val MIN_LONG_SIDE = 320

    fun sampleSize(source: PixelSize, maxLongSide: Int = MAX_LONG_SIDE): Int {
        require(source.width > 0 && source.height > 0) { "Empty image" }
        var sample = 1
        while (source.longSide / (sample * 2) >= maxLongSide) sample *= 2
        return sample
    }

    fun scaledSize(source: PixelSize, maxLongSide: Int = MAX_LONG_SIDE): PixelSize {
        if (source.longSide <= maxLongSide) return source
        val scale = maxLongSide.toDouble() / source.longSide
        return PixelSize(
            width = (source.width * scale).roundToInt().coerceAtLeast(1),
            height = (source.height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    fun exifTransform(orientation: Int): ExifTransform = when (orientation) {
        2 -> ExifTransform(0, flipHorizontal = true)
        3 -> ExifTransform(180, flipHorizontal = false)
        4 -> ExifTransform(180, flipHorizontal = true)
        5 -> ExifTransform(90, flipHorizontal = true)
        6 -> ExifTransform(90, flipHorizontal = false)
        7 -> ExifTransform(270, flipHorizontal = true)
        8 -> ExifTransform(270, flipHorizontal = false)
        else -> ExifTransform(0, flipHorizontal = false)
    }

    fun uprightSize(stored: PixelSize, transform: ExifTransform): PixelSize =
        if (transform.swapsSides) PixelSize(stored.height, stored.width) else stored

    fun encodeWithinBudget(
        startLongSide: Int,
        maxBytes: Int = MAX_BYTES,
        encode: (longSide: Int, quality: Int) -> ByteArray,
    ): EncodedJpeg? {
        var longSide = startLongSide
        while (true) {
            var quality = START_QUALITY
            while (quality >= MIN_QUALITY) {
                val bytes = encode(longSide, quality)
                if (bytes.size <= maxBytes) return EncodedJpeg(bytes, longSide, quality)
                quality -= QUALITY_STEP
            }
            val next = (longSide * SHRINK_FACTOR).roundToInt()
            if (next < MIN_LONG_SIDE) return null
            longSide = next
        }
    }
}
