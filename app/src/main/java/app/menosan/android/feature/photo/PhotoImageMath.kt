package app.menosan.android.feature.photo

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
