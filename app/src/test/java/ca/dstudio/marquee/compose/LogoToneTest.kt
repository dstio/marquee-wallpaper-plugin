package ca.dstudio.marquee.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoToneTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    private val transparentWhite = argb(0, 255, 255, 255)
    private val faintWhite = argb(100, 255, 255, 255)

    @Test
    fun blackAndDeepRedAreDark() {
        assertTrue(LogoTone.isDark(IntArray(50) { argb(255, 0, 0, 0) }))
        assertTrue(LogoTone.isDark(IntArray(50) { argb(255, 150, 10, 20) }))
    }

    @Test
    fun whiteAndGoldAreNotDark() {
        assertFalse(LogoTone.isDark(IntArray(50) { argb(255, 255, 255, 255) }))
        assertFalse(LogoTone.isDark(IntArray(50) { argb(255, 220, 180, 60) }))
    }

    @Test
    fun pixelsHalfTransparentOrLessAreIgnored() {
        // A black logo on a clear (white, alpha 0) ground, with a faint white halo: still dark.
        val pixels = IntArray(100) { i ->
            when {
                i < 20 -> argb(255, 0, 0, 0)
                i < 40 -> faintWhite
                else -> transparentWhite
            }
        }
        assertTrue(LogoTone.isDark(pixels))
    }

    @Test
    fun aLogoWithNoOpaquePixelIsNotDark() {
        assertFalse(LogoTone.isDark(IntArray(10) { transparentWhite }))
        assertFalse(LogoTone.isDark(IntArray(0)))
    }
}
