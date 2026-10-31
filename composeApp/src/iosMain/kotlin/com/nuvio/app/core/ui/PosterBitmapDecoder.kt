package com.nuvio.app.core.ui

import coil3.ImageLoader
import coil3.asImage
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.impl.Managed
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Coil 3.6.3's stock Skia decoder (iOS and desktop) downsizes with nearest-neighbour sampling,
 * before Compose can filter it, which made phone posters and heroes look jagged next to the PC.
 * This one resizes with Lanczos3, matching Pillow's LANCZOS downscale; see [lanczosResize].
 */
internal class PosterBitmapDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {
    @OptIn(ExperimentalCoilApi::class)
    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        return Image.makeFromEncoded(bytes).closing { image ->
            val (width, height) = DecodeUtils.computeDstSize(
                image.width, image.height, options.size, options.scale, options.maxBitmapSize,
            )
            var multiplier = DecodeUtils.computeSizeMultiplier(
                image.width, image.height, width, height, options.scale, options.maxBitmapSize,
            )
            if (options.precision == Precision.INEXACT) multiplier = multiplier.coerceAtMost(1.0)
            val bitmap = Bitmap()
            try {
                check(bitmap.allocN32Pixels(
                    (image.width * multiplier).toInt().coerceAtLeast(1),
                    (image.height * multiplier).toInt().coerceAtLeast(1),
                ))
                if (image.width == bitmap.width && image.height == bitmap.height) {
                    check(image.readPixels(bitmap, 0, 0))
                } else {
                    Bitmap().closing { original ->
                        check(original.allocN32Pixels(image.width, image.height))
                        check(image.readPixels(original, 0, 0))
                        check(bitmap.installPixels(lanczosResize(original, bitmap.width, bitmap.height)))
                    }
                }
                bitmap.setImmutable()
                DecodeResult(
                    image = bitmap.asImage(),
                    isSampled = bitmap.width < image.width || bitmap.height < image.height,
                )
            } catch (error: Throwable) {
                bitmap.close()
                throw error
            }
        }
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            // Do not claim SVGs (including embedded-image posters) or other decoder-specific formats.
            val source = result.source.source()
            val raster = source.rangeEquals(0, "ffd8ff".decodeHex()) ||
                source.rangeEquals(0, "89504e470d0a1a0a".decodeHex()) ||
                (source.rangeEquals(0, "RIFF".encodeUtf8()) && source.rangeEquals(8, "WEBP".encodeUtf8()))
            return if (raster) PosterBitmapDecoder(result.source, options) else null
        }
    }

    /**
     * Normalised Lanczos3 taps for each output pixel, widened by [scale] when reducing (as Pillow does).
     * Same code as the PC build's decoder (nuvio-desktop poster-render), nested to match it.
     */
    internal class LanczosWeights(inputSize: Int, outputSize: Int, scale: Double) {
        private val first = IntArray(outputSize)
        private val count = IntArray(outputSize)
        private val taps: Int
        private val weights: FloatArray

        init {
            val filterScale = maxOf(scale, 1.0)
            val support = LanczosSupport * filterScale
            taps = ceil(support).toInt() * 2 + 1
            weights = FloatArray(outputSize * taps)
            for (out in 0 until outputSize) {
                val center = (out + 0.5) * scale
                val start = floor(center - support + 0.5).toInt().coerceAtLeast(0)
                val end = minOf(floor(center + support + 0.5).toInt(), inputSize).coerceAtLeast(start + 1)
                var total = 0.0
                val values = DoubleArray(end - start) { lanczos3((start + it + 0.5 - center) / filterScale) }
                values.forEach { total += it }
                first[out] = start
                count[out] = values.size
                for (i in values.indices) weights[out * taps + i] = (values[i] / total).toFloat()
            }
        }

        fun apply(out: Int, input: FloatArray, base: Int, stride: Int, output: FloatArray, target: Int) {
            var b = 0f; var g = 0f; var r = 0f; var a = 0f
            var index = base + first[out] * stride
            for (i in 0 until count[out]) {
                val weight = weights[out * taps + i]
                b += input[index] * weight
                g += input[index + 1] * weight
                r += input[index + 2] * weight
                a += input[index + 3] * weight
                index += stride
            }
            output[target] = b; output[target + 1] = g; output[target + 2] = r; output[target + 3] = a
        }
    }
}

/**
 * Lanczos3 resize of N32 premultiplied pixels, returned in the same layout.
 *
 * Large reductions first box-average by a whole factor that leaves at least 3× for the Lanczos pass
 * (Pillow's `reducing_gap = 3`), so a 2000×3000 original costs a box sum plus a ~3× filter instead of a
 * 9×-wide kernel over every source row. Sample positions stay in original coordinates, so the box step
 * shifts nothing. Overshoot is clamped to 0..alpha, which keeps Lanczos ringing to the reference's level.
 */
private fun lanczosResize(source: Bitmap, width: Int, height: Int): ByteArray {
    val factorX = (source.width.toDouble() / width / LanczosReducingGap).toInt().coerceAtLeast(1)
    val factorY = (source.height.toDouble() / height / LanczosReducingGap).toInt().coerceAtLeast(1)
    val reduced = boxReduce(source, factorX, factorY)
    val reducedWidth = ceilDiv(source.width, factorX)
    val reducedHeight = ceilDiv(source.height, factorY)

    val columns = PosterBitmapDecoder.LanczosWeights(reducedWidth, width, source.width.toDouble() / width / factorX)
    val rows = PosterBitmapDecoder.LanczosWeights(reducedHeight, height, source.height.toDouble() / height / factorY)
    val horizontal = FloatArray(reducedHeight * width * 4)
    for (y in 0 until reducedHeight) {
        val sourceRow = y * reducedWidth * 4
        val targetRow = y * width * 4
        for (x in 0 until width) columns.apply(x, reduced, sourceRow, 4, horizontal, targetRow + x * 4)
    }
    val result = FloatArray(width * height * 4)
    for (y in 0 until height) {
        for (x in 0 until width) rows.apply(y, horizontal, x * 4, width * 4, result, (y * width + x) * 4)
    }

    val pixels = ByteArray(result.size)
    var i = 0
    while (i < result.size) {
        // N32 is BGRA or RGBA, but alpha is always last.
        val alpha = result[i + 3].roundToInt().coerceIn(0, 255)
        pixels[i + 3] = alpha.toByte()
        for (channel in 0 until 3) pixels[i + channel] = result[i + channel].roundToInt().coerceIn(0, alpha).toByte()
        i += 4
    }
    return pixels
}

private const val LanczosReducingGap = 3
private const val LanczosSupport = 3.0

private fun ceilDiv(value: Int, divisor: Int) = (value + divisor - 1) / divisor

/** Averages each factorX×factorY block (smaller at the right/bottom edges) into one float pixel. */
private fun boxReduce(source: Bitmap, factorX: Int, factorY: Int): FloatArray {
    val width = ceilDiv(source.width, factorX)
    val height = ceilDiv(source.height, factorY)
    val output = FloatArray(width * height * 4)
    val sums = IntArray(width * 4)
    for (y in 0 until height) {
        val top = y * factorY
        val bandHeight = minOf(factorY, source.height - top)
        val band = checkNotNull(source.readPixels(
            ImageInfo.makeN32Premul(source.width, bandHeight), source.width * 4, 0, top,
        ))
        sums.fill(0)
        for (row in 0 until bandHeight) {
            var index = row * source.width * 4
            for (x in 0 until width) {
                // Sum the block's row in locals: adding into sums per pixel serialises on memory.
                var b = 0; var g = 0; var r = 0; var a = 0
                repeat(minOf(factorX, source.width - x * factorX)) {
                    b += band[index].toInt() and 255
                    g += band[index + 1].toInt() and 255
                    r += band[index + 2].toInt() and 255
                    a += band[index + 3].toInt() and 255
                    index += 4
                }
                sums[x * 4] += b; sums[x * 4 + 1] += g; sums[x * 4 + 2] += r; sums[x * 4 + 3] += a
            }
        }
        val out = y * width * 4
        for (x in 0 until width) {
            val count = (minOf(factorX, source.width - x * factorX) * bandHeight).toFloat()
            for (channel in 0 until 4) output[out + x * 4 + channel] = sums[x * 4 + channel] / count
        }
    }
    return output
}

private fun lanczos3(x: Double): Double = when {
    x == 0.0 -> 1.0
    x <= -LanczosSupport || x >= LanczosSupport -> 0.0
    else -> {
        val px = PI * x
        LanczosSupport * sin(px) * sin(px / LanczosSupport) / (px * px)
    }
}

/** Kotlin/Native's Skia objects aren't AutoCloseable, so the JVM's `use {}` isn't available here. */
private inline fun <T : Managed, R> T.closing(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        close()
    }
