package ca.dstudio.marquee.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import ca.dstudio.marquee.catalog.TitleDetails
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * One wallpaper's ingredients: the title, where it came from, and its pictures as files. [wall] is
 * the list's posters in this run (Poster wall), [wallIndex] this title's among them, or -1.
 */
data class Scene(
    val details: TitleDetails,
    val listLabel: String,
    /** 0-based place among this run's titles of the same list. */
    val indexInList: Int,
    val listCount: Int,
    val backdrop: File,
    val logo: File?,
    val wall: List<File>,
    val wallIndex: Int,
)

/** The scene's pictures, decoded for one composition. */
internal class Art(val backdrop: Bitmap?, val logo: Bitmap?, val wall: List<Bitmap>, val wallLit: Int) {
    fun recycle() {
        backdrop?.recycle()
        logo?.recycle()
        wall.forEach { it.recycle() }
    }
}

/** The design space: always 1920 wide, as tall as the picture's shape makes it. */
internal class Frame(val h: Float) {
    val w = 1920f
}

/**
 * Draws a wallpaper with Canvas alone. Everything is laid out in a 1920 x 1080 design space and
 * scaled to the output (`s = width / 1920`, so 4K is s = 2). Runs on the generator's thread, one
 * picture at a time.
 */
class Composer(private val labels: Labels) {

    /** The layout [scene] is actually drawn in: Poster wall needs six posters of its list. */
    fun effectiveLayout(scene: Scene, look: Look): Layout =
        if (look.layout == Layout.POSTER_WALL && (scene.wall.size < MIN_WALL || scene.wallIndex !in scene.wall.indices)) {
            Layout.CINEMATIC
        } else {
            look.layout
        }

    fun compose(scene: Scene, look: Look, width: Int, height: Int): Bitmap {
        val s = width / 1920f
        val frame = Frame(height / s)
        var layout = effectiveLayout(scene, look)
        var wall = emptyList<Bitmap>()
        var lit = -1
        if (layout == Layout.POSTER_WALL) {
            val decoded = scene.wall.map { decodeFit(it, (240 * s).toInt(), (360 * s).toInt()) }
            if (decoded[scene.wallIndex] != null && decoded.count { it != null } >= MIN_WALL) {
                lit = decoded.take(scene.wallIndex).count { it != null }
                wall = decoded.filterNotNull()
            } else {
                decoded.forEach { it?.recycle() }
                layout = Layout.CINEMATIC
            }
        }
        val art = Art(
            backdrop = if (layout == Layout.POSTER_WALL) null else decodeCover(scene.backdrop, width, height),
            logo = if (layout == Layout.CLEAN) null else scene.logo?.let { decodeFit(it, (840 * s).toInt(), (200 * s).toInt()) }?.let(::legible),
            wall = wall,
            wallLit = lit,
        )
        try {
            val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            c.drawColor(BACKGROUND)
            c.scale(s, s)
            when (layout) {
                Layout.CINEMATIC -> CinematicLayout.draw(c, frame, scene, art, look, labels)
                Layout.MINIMAL -> MinimalLayout.draw(c, frame, scene, art, look, labels)
                Layout.POSTER_WALL -> PosterWallLayout.draw(c, frame, scene, art, look, labels)
                Layout.CLEAN -> CleanLayout.draw(c, frame, art, look)
            }
            return out
        } finally {
            art.recycle()
        }
    }

    companion object {
        /**
         * Bumped whenever the composer's pixels change for the same inputs; it is part of every
         * picture's id, so the launcher fetches the new pictures.
         */
        const val STYLE_VERSION = 4

        const val MIN_WALL = 6

        val BACKGROUND = Color.parseColor("#0C0D0F")

        /** Decoded no smaller than needed to cover [w] x [h], and no more than twice that. */
        internal fun decodeCover(file: File, w: Int, h: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val cover = max(w.toFloat() / bounds.outWidth, h.toFloat() / bounds.outHeight)
            return decode(file, sampleFor(cover))
        }

        /** Decoded no smaller than needed to fit in [w] x [h]. */
        internal fun decodeFit(file: File, w: Int, h: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val fit = min(w.toFloat() / bounds.outWidth, h.toFloat() / bounds.outHeight)
            return decode(file, sampleFor(fit))
        }

        /**
         * [logo] as it is, or as a white silhouette when it is too dark to read on the shade (a
         * black or deep-red logo). The dark one is recycled.
         */
        internal fun legible(logo: Bitmap): Bitmap {
            val pixels = IntArray(logo.width * logo.height)
            logo.getPixels(pixels, 0, logo.width, 0, 0, logo.width, logo.height)
            if (!LogoTone.isDark(pixels)) return logo
            val white = Bitmap.createBitmap(logo.width, logo.height, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            Canvas(white).drawBitmap(logo, 0f, 0f, paint)
            logo.recycle()
            return white
        }

        private fun sampleFor(scale: Float): Int {
            var sample = 1
            while (scale * sample * 2 <= 1f) sample *= 2
            return sample
        }

        private fun decode(file: File, sample: Int): Bitmap? = BitmapFactory.decodeFile(
            file.path,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )

        /** [bitmap] centre-cropped to fill [dst]. */
        internal fun drawCover(c: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint = Paint(Paint.FILTER_BITMAP_FLAG)) {
            val scale = max(dst.width() / bitmap.width, dst.height() / bitmap.height)
            val sw = dst.width() / scale
            val sh = dst.height() / scale
            val sx = (bitmap.width - sw) / 2f
            val sy = (bitmap.height - sh) / 2f
            c.drawBitmap(bitmap, Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt()), dst, paint)
        }
    }
}
