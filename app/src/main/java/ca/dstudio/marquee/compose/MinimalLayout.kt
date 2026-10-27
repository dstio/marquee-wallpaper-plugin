package ca.dstudio.marquee.compose

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Minimal: the picture first. The logo and one facts line, the rating as its last item, over a
 * soft shade on the text's side and the bottom shade.
 */
internal object MinimalLayout {

    private const val TOP = 150f

    fun draw(c: Canvas, f: Frame, scene: Scene, art: Art, look: Look, labels: Labels) {
        art.backdrop?.let { Composer.drawCover(c, it, RectF(0f, 0f, f.w, f.h)) }
        val d = scene.details
        val facts = labels.facts(d) + listOfNotNull(labels.tmdbPercent(d)?.let { "${labels.tmdb} $it" })
        if (look.position == TextPosition.BOTTOM_PANEL) {
            Shade.top(c, f.w, f.h)
            InfoColumn.drawPanel(c, f, InfoColumn.panelColumn(d, art.logo, labels, facts, 0))
            return
        }
        val right = look.position == TextPosition.TOP_RIGHT
        Shade.minimalSide(c, f.w, f.h, if (right) Shade.Edge.RIGHT else Shade.Edge.LEFT)
        Shade.bottom(c, f.w, f.h)
        val w = InfoColumn.WIDTH
        Column(w, InfoColumn.align(look.position))
            .add(0f, InfoColumn.logoOrTitle(art.logo, d, labels, w, 760f, 170f, 88f, 64f, 26f))
            .add(22f, InfoColumn.facts(facts, labels, 23f, w))
            .draw(c, InfoColumn.left(f, look.position), TOP)
    }
}
