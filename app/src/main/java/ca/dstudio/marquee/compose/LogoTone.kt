package ca.dstudio.marquee.compose

/**
 * Whether a logo is too dark to read on the shade. Pure, over `0xAARRGGBB` pixels as
 * `Bitmap.getPixels` gives them (not premultiplied).
 */
internal object LogoTone {

    /** Below this average luminance (0 black to 1 white) a logo is drawn as a white silhouette. */
    const val DARK_BELOW = 0.35

    /**
     * The average luminance of the pixels more than half opaque, or null when there are none.
     * Rec. 709 weights over the 8-bit channels.
     */
    fun averageLuminance(pixels: IntArray): Double? {
        var sum = 0.0
        var count = 0
        for (p in pixels) {
            val a = (p ushr 24) and 0xFF
            if (a * 2 <= 0xFF) continue
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            sum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            count++
        }
        return if (count == 0) null else sum / count
    }

    /** Dark: an average luminance below [DARK_BELOW]. A logo with no opaque pixel is not. */
    fun isDark(pixels: IntArray): Boolean = averageLuminance(pixels)?.let { it < DARK_BELOW } ?: false
}
