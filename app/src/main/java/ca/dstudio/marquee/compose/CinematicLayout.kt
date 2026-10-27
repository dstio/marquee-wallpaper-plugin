package ca.dstudio.marquee.compose

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Cinematic: the backdrop, a shade from the text's side and one from the bottom, and a column of
 * logo, facts, rating, synopsis and the list it came from. At the top the column ends above
 * y 640: the footer goes first, then synopsis lines.
 */
internal object CinematicLayout {

    fun draw(c: Canvas, f: Frame, scene: Scene, art: Art, look: Look, labels: Labels) {
        art.backdrop?.let { Composer.drawCover(c, it, RectF(0f, 0f, f.w, f.h)) }
        val d = scene.details
        if (look.position == TextPosition.BOTTOM_PANEL) {
            Shade.top(c, f.w, f.h)
            InfoColumn.drawPanel(c, f, InfoColumn.panelColumn(d, art.logo, labels, labels.facts(d), minOf(2, look.synopsisLines)))
            return
        }
        val right = look.position == TextPosition.TOP_RIGHT
        Shade.cinematicSide(c, f.w, f.h, if (right) Shade.Edge.RIGHT else Shade.Edge.LEFT)
        Shade.bottom(c, f.w, f.h)
        column(scene, art, look, labels).draw(c, InfoColumn.left(f, look.position), InfoColumn.TOP)
    }

    private fun column(scene: Scene, art: Art, look: Look, labels: Labels): Column {
        val room = InfoColumn.BOTTOM_LIMIT - InfoColumn.TOP
        // The synopsis matters more than the footer: the footer goes first, then synopsis lines.
        for (lines in look.synopsisLines downTo 0) {
            for (footer in listOf(true, false)) {
                val col = build(scene, art, look, labels, lines, footer)
                if (col.height <= room) return col
            }
        }
        return build(scene, art, look, labels, 0, footer = false)
    }

    private fun build(scene: Scene, art: Art, look: Look, labels: Labels, lines: Int, footer: Boolean): Column {
        val d = scene.details
        val w = InfoColumn.WIDTH
        return Column(w, InfoColumn.align(look.position))
            .add(0f, InfoColumn.logoOrTitle(art.logo, d, labels, w, 840f, LOGO_HEIGHT, 104f, 72f, 30f))
            .add(22f, InfoColumn.facts(labels.facts(d), labels, 23f, w))
            .add(24f, InfoColumn.ratings(d, labels))
            .add(24f, InfoColumn.synopsis(d, 30f, lines, w))
            .add(24f, if (footer) InfoColumn.footer(d, scene.listLabel, labels, w) else null)
    }

    /** A logo is fitted in 840 x this, so a tall one still leaves room for three synopsis lines. */
    private const val LOGO_HEIGHT = 150f
}
