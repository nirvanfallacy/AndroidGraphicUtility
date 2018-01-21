package rapiddecoder

import android.graphics.Bitmap

internal class ResourceFullBitmapDecoder(source: BitmapSource) : ResourceBitmapDecoder(source) {
    private var densityScaledWidth = INVALID_SIZE
    private var densityScaledHeight = INVALID_SIZE

    override val width: Int
        get() {
            if (densityScaledWidth == INVALID_SIZE) {
                densityScaledWidth = Math.round(sourceWidth * densityScale)
            }
            return densityScaledWidth
        }

    override val height: Int
        get() {
            if (densityScaledHeight == INVALID_SIZE) {
                densityScaledHeight = Math.round(sourceHeight * densityScale)
            }
            return densityScaledHeight
        }

    override fun region(left: Int, top: Int, right: Int, bottom: Int): BitmapLoader {
        if (hasMetadata(MetadataType.SIZE) &&
                left <= 0 && top <= 0 && right >= width && bottom >= height) {
            return this
        }
        return ResourceRegionBitmapDecoder(source, left, top, right, bottom)
    }

    override fun decodeResource(options: LoadBitmapOptions,
                                input: BitmapDecodeInput,
                                output: BitmapDecodeOutput): Bitmap {
        val opts = output.options
        val bitmap = source.decode(opts) ?: throw DecodeFailedException()

        imageMimeType = opts.outMimeType
        if (bitmapDensityScale.isNaN()) {
            val scale = if (source.densityScaleSupported &&
                    opts.inTargetDensity != 0 && opts.inDensity != 0) {
                opts.inTargetDensity.toDouble() / opts.inDensity
            } else {
                1.0
            }
            bitmapDensityScale = scale.toFloat()
        }
        if (opts.inSampleSize == 1) {
            bitmapWidth = opts.outWidth
            bitmapHeight = opts.outHeight
            densityScaledWidth = bitmap.width
            densityScaledHeight = bitmap.height
        }

        return bitmap
    }

    override fun hasMetadata(type: MetadataType): Boolean {
        return synchronized(decodeLock) {
            when (type) {
                MetadataType.SIZE -> densityScaledWidth != INVALID_SIZE
                MetadataType.DENSITY_SCALE -> !bitmapDensityScale.isNaN()
                MetadataType.MIME_TYPE -> imageMimeType != null
                MetadataType.SOURCE_SIZE -> bitmapWidth != INVALID_SIZE
            }
        }
    }
}