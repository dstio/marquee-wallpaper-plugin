package ca.dstudio.marquee.compose

import android.graphics.Canvas
import android.graphics.RectF

/** Clean: the backdrop alone, with a light bottom shade while it is switched on. */
internal object CleanLayout {

    fun draw(c: Canvas, f: Frame, art: Art, look: Look) {
        art.backdrop?.let { Composer.drawCover(c, it, RectF(0f, 0f, f.w, f.h)) }
        if (look.bottomShade) Shade.gradient(c, f.w, f.h, Shade.Edge.BOTTOM, listOf(0f to 0.5f, 0.40f to 0f))
    }
}
