package ca.dstudio.marquee.compose

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max

/**
 * Poster wall: the list's posters in a tilted grid, every one but this title's dimmed, this one
 * lit, scaled up and outlined; the text column on the dark side with a kicker naming the list and
 * the title's place in it. At the top right the wall is mirrored to the left.
 */
internal object PosterWallLayout {

    private const val COLS = 6
    private const val ROWS = 5
    private const val PW = 226f
    private const val PH = 339f
    private const val GAP = 24f
    private const val RADIUS = 6f
    private const val ORIGIN_X = 760f
    private const val ORIGIN_Y = -250f
    private const val TILT = 8f

    /** The lit poster's cell: second row, second column. */
    private const val LIT = 7

    fun draw(c: Canvas, f: Frame, scene: Scene, art: Art, look: Look, labels: Labels) {
        val right = look.position == TextPosition.TOP_RIGHT
        wall(c, f, scene, art, right)

        val ground = Composer.BACKGROUND
        Shade.gradient(c, f.w, f.h, if (right) Shade.Edge.RIGHT else Shade.Edge.LEFT,
            listOf(0f to 1f, 0.36f to 0.95f, 0.60f to 0f), rgb = ground)
        Shade.bottom(c, f.w, f.h)

        val d = scene.details
        val kicker = labels.kicker(scene.listLabel, scene.indexInList + 1, scene.listCount)
        val w = InfoColumn.WIDTH
        Column(w, InfoColumn.align(look.position))
            .add(0f, InfoColumn.kicker(kicker, labels, w))
            .add(26f, InfoColumn.logoOrTitle(art.logo, d, labels, w, 840f, 200f, 104f, 72f, 30f))
            .add(28f, InfoColumn.facts(labels.facts(d), labels, 23f, w))
            .add(30f, InfoColumn.ratings(d, labels))
            .draw(c, InfoColumn.left(f, look.position), InfoColumn.TOP)
    }

    private fun wall(c: Canvas, f: Frame, scene: Scene, art: Art, mirrored: Boolean) {
        val posters = art.wall
        val n = posters.size
        if (n == 0) return
        val litIndex = art.wallLit.coerceIn(0, n - 1)
        val start = ((litIndex - LIT) % n + n) % n

        c.save()
        if (mirrored) {
            c.translate(f.w - ORIGIN_X, ORIGIN_Y)
            c.rotate(TILT)
        } else {
            c.translate(ORIGIN_X, ORIGIN_Y)
            c.rotate(-TILT)
        }
        val dim = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0.5f, 0.5f, 0.5f, 1f) })
        }
        for (i in 0 until COLS * ROWS) {
            if (i == LIT) continue
            poster(c, cell(i, mirrored), posters[(start + i) % n], dim)
        }
        val lit = cell(LIT, mirrored)
        c.save()
        c.scale(1.05f, 1.05f, lit.centerX(), lit.centerY())
        poster(c, lit, posters[litIndex], Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = Color.WHITE
        }
        // A 6 px outline 4 px outside the poster: its stroke runs 7 px out.
        val ring = RectF(lit).apply { inset(-7f, -7f) }
        c.drawRoundRect(ring, RADIUS + 7f, RADIUS + 7f, outline)
        c.restore()
        c.restore()
    }

    private fun cell(i: Int, mirrored: Boolean): RectF {
        val col = i % COLS
        val row = i / COLS
        val x = if (mirrored) -(col + 1) * PW - col * GAP else col * (PW + GAP)
        val y = row * (PH + GAP)
        return RectF(x, y, x + PW, y + PH)
    }

    /** [bitmap] centre-cropped into [rect] with rounded corners. */
    private fun poster(c: Canvas, rect: RectF, bitmap: Bitmap, paint: Paint) {
        val scale = max(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val m = Matrix()
        m.setScale(scale, scale)
        m.postTranslate(
            rect.left + (rect.width() - bitmap.width * scale) / 2f,
            rect.top + (rect.height() - bitmap.height * scale) / 2f,
        )
        val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setLocalMatrix(m)
        paint.shader = shader
        c.drawRoundRect(rect, RADIUS, RADIUS, paint)
        paint.shader = null
    }
}
