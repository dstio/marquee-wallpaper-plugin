package ca.dstudio.marquee.source.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import ca.dstudio.marquee.compose.Fonts
import java.util.Locale
import kotlin.random.Random

/**
 * Paints the sample source's pictures: quiet landscapes in muted colours, a horizon and a few
 * shapes, and posters with the title set in type. Deterministic per title, so a picture painted
 * again is the same picture.
 */
internal object SamplePainter {

    private enum class Scene { COAST, SNOW, DESERT, ORCHARD, HARBOUR, ORBIT, CITY, TOWER, MARSH, RADIO, STREET, LAKE }

    private class Palette(
        val sky: IntArray,
        val ground: IntArray,
        val far: Int,
        val near: Int,
        val accent: Int,
    )

    private fun c(hex: String) = Color.parseColor(hex)

    private val PALETTES = listOf(
        Palette(intArrayOf(c("#27313A"), c("#55616B"), c("#9C8A7A"), c("#B49A82")), intArrayOf(c("#3A454E"), c("#0F1418")), c("#4A535B"), c("#15191D"), c("#D6D1C8")),
        Palette(intArrayOf(c("#161E27"), c("#34414E"), c("#7C8791")), intArrayOf(c("#5D6873"), c("#262E36")), c("#3B4753"), c("#10161C"), c("#E2B46A")),
        Palette(intArrayOf(c("#7E8E97"), c("#BDB3A0"), c("#D9C9AA")), intArrayOf(c("#A9825C"), c("#4E3624")), c("#8C5D3F"), c("#38322D"), c("#E8DDC6")),
        Palette(intArrayOf(c("#4F5A54"), c("#9AA08A"), c("#D8C9A0")), intArrayOf(c("#5E6B45"), c("#232A1B")), c("#6F7A5C"), c("#2C3524"), c("#C9D3CF")),
        Palette(intArrayOf(c("#2A2C3A"), c("#5B5267"), c("#B07D6E")), intArrayOf(c("#2F3644"), c("#0E1117")), c("#3A3D4B"), c("#14161C"), c("#E9B56F")),
        Palette(intArrayOf(c("#06080D"), c("#0F1624"), c("#1E2B3F")), intArrayOf(c("#0F1624"), c("#06080D")), c("#5C7FA8"), c("#0A0D14"), c("#BFD6EE")),
        Palette(intArrayOf(c("#1C1B2B"), c("#3B3550"), c("#76637A")), intArrayOf(c("#1E1D2A"), c("#0B0B12")), c("#2A2838"), c("#121119"), c("#F0C27A")),
        Palette(intArrayOf(c("#101418"), c("#262E33"), c("#45504F")), intArrayOf(c("#1D2326"), c("#0A0C0E")), c("#2B3336"), c("#0E1113"), c("#D9D08F")),
        Palette(intArrayOf(c("#6D7C86"), c("#B9AE96"), c("#E3C79C")), intArrayOf(c("#7A7559"), c("#2F3326")), c("#8D8A6E"), c("#3B3D2E"), c("#F2E3C2")),
        Palette(intArrayOf(c("#0E1420"), c("#24304A"), c("#4F5B73")), intArrayOf(c("#1A2130"), c("#090C12")), c("#2A3346"), c("#0C1018"), c("#D9483B")),
        Palette(intArrayOf(c("#3B3436"), c("#6E5F5E"), c("#A08C82")), intArrayOf(c("#3A3233"), c("#141011")), c("#4C4344"), c("#1B1617"), c("#C9A27A")),
        Palette(intArrayOf(c("#2E3B48"), c("#6D8396"), c("#B7C4CC")), intArrayOf(c("#9FB0BC"), c("#3B4A57")), c("#56687A"), c("#202A33"), c("#EEF2F4")),
    )

    fun backdrop(n: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(w / 1920f, h / 1080f)
        val p = PALETTES[n % PALETTES.size]
        val r = Random(7919 * (n + 1))
        when (Scene.entries[n % Scene.entries.size]) {
            Scene.COAST -> coast(canvas, p, r)
            Scene.SNOW -> snow(canvas, p, r)
            Scene.DESERT -> desert(canvas, p, r)
            Scene.ORCHARD -> orchard(canvas, p, r)
            Scene.HARBOUR -> harbour(canvas, p, r)
            Scene.ORBIT -> orbit(canvas, p, r)
            Scene.CITY -> city(canvas, p, r)
            Scene.TOWER -> tower(canvas, p, r)
            Scene.MARSH -> marsh(canvas, p, r)
            Scene.RADIO -> radio(canvas, p, r)
            Scene.STREET -> street(canvas, p, r)
            Scene.LAKE -> lake(canvas, p, r)
        }
        grain(canvas, r)
        return bmp
    }

    fun poster(n: Int, title: String, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = PALETTES[n % PALETTES.size]
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(p.sky.last(), p.sky[p.sky.size / 2], p.near),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
        paint.color = p.accent
        paint.alpha = 210
        canvas.drawCircle(w * (0.3f + 0.4f * ((n * 37) % 10) / 10f), h * 0.3f, w * 0.17f, paint)
        paint.color = p.far
        paint.alpha = 255
        val ridge = Path().apply {
            moveTo(0f, h * 0.62f)
            var x = 0f
            val r = Random(n + 3)
            while (x < w) {
                x += w / 6f
                lineTo(x, h * (0.5f + r.nextFloat() * 0.14f))
            }
            lineTo(w.toFloat(), h.toFloat()); lineTo(0f, h.toFloat()); close()
        }
        canvas.drawPath(ridge, paint)
        paint.shader = LinearGradient(0f, h * 0.55f, 0f, h.toFloat(), Color.TRANSPARENT, Color.argb(220, 0, 0, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, h * 0.55f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        val pad = w * 0.08f
        val type = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = if (n % 2 == 0) Fonts.fraunces(500) else Fonts.jost(500)
            textSize = w * 0.13f
            letterSpacing = 0.04f
            color = Color.parseColor("#F2EEE6")
        }
        val text = title.uppercase(Locale.ROOT)
        // Small enough that no word is broken.
        val longest = text.split(' ').maxByOrNull { type.measureText(it) }.orEmpty()
        while (type.measureText(longest) > w - 2 * pad && type.textSize > 8f) type.textSize *= 0.92f
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, type, (w - 2 * pad).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 0.9f)
            .setIncludePad(false)
            .setMaxLines(3)
            .build()
        val caption = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Fonts.jost(400)
            textSize = w * 0.035f
            letterSpacing = 0.14f
            color = Color.argb(180, 255, 255, 255)
        }
        val captionY = h - pad
        canvas.drawText("A MARQUEE SAMPLE", pad, captionY, caption)
        canvas.save()
        canvas.translate(pad, captionY - caption.textSize - w * 0.04f - layout.height)
        layout.draw(canvas)
        canvas.restore()
        return bmp
    }

    // ---- scenes, in a 1920 x 1080 space ----

    private fun sky(c: Canvas, colors: IntArray, bottom: Float) {
        val paint = Paint()
        paint.shader = LinearGradient(0f, 0f, 0f, bottom, colors, null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, 1920f, bottom, paint)
    }

    private fun ground(c: Canvas, colors: IntArray, top: Float) {
        val paint = Paint()
        paint.shader = LinearGradient(0f, top, 0f, 1080f, colors, null, Shader.TileMode.CLAMP)
        c.drawRect(0f, top, 1920f, 1080f, paint)
    }

    private fun fill(color: Int, alpha: Int = 255) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.alpha = alpha }

    /** A jagged ridge from x 0 to 1920 around [base], [rough] high, closed down to [floor]. */
    private fun ridge(c: Canvas, r: Random, base: Float, rough: Float, step: Float, floor: Float, paint: Paint) {
        val path = Path()
        path.moveTo(0f, base)
        var x = 0f
        while (x < 1920f) {
            x += step * (0.6f + r.nextFloat() * 0.8f)
            path.lineTo(x.coerceAtMost(1920f), base - r.nextFloat() * rough)
        }
        path.lineTo(1920f, floor); path.lineTo(0f, floor); path.close()
        c.drawPath(path, paint)
    }

    private fun haze(c: Canvas, r: Random, color: Int, top: Float, bottom: Float) {
        val paint = fill(color, 9)
        repeat(4) {
            val y = top + r.nextFloat() * (bottom - top)
            val x = r.nextFloat() * 1300f
            val w = 500f + r.nextFloat() * 900f
            val h = 12f + r.nextFloat() * 16f
            c.drawRoundRect(RectF(x, y, x + w, y + h), h / 2, h / 2, paint)
        }
    }

    private fun sun(c: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = RadialGradient(x, y, radius * 3f, Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)),
            Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(x, y, radius * 3f, paint)
        paint.shader = null
        paint.color = color
        paint.alpha = 220
        c.drawCircle(x, y, radius, paint)
    }

    private fun trees(c: Canvas, r: Random, base: Float, height: Float, color: Int) {
        val path = Path()
        path.moveTo(0f, base)
        var x = 0f
        while (x < 1920f) {
            val w = 26f + r.nextFloat() * 22f
            path.lineTo(x + w / 2, base - height * (0.6f + r.nextFloat() * 0.4f))
            path.lineTo(x + w, base)
            x += w
        }
        path.lineTo(1920f, base + 40f); path.lineTo(0f, base + 40f); path.close()
        c.drawPath(path, fill(color))
    }

    private fun skyline(c: Canvas, r: Random, base: Float, maxH: Float, color: Int, windows: Int?, from: Float = 0f, to: Float = 1920f) {
        var x = from
        val body = fill(color)
        val lit = windows?.let { fill(it, 200) }
        while (x < to) {
            val w = 60f + r.nextFloat() * 110f
            val h = maxH * (0.3f + r.nextFloat() * 0.7f)
            c.drawRect(x, base - h, x + w - 6f, base, body)
            if (lit != null) {
                var wy = base - h + 18f
                while (wy < base - 20f) {
                    var wx = x + 10f
                    while (wx < x + w - 22f) {
                        if (r.nextFloat() < 0.22f) c.drawRect(wx, wy, wx + 8f, wy + 12f, lit)
                        wx += 18f
                    }
                    wy += 26f
                }
            }
            x += w
        }
    }

    private fun stars(c: Canvas, r: Random, bottom: Float, count: Int) {
        val paint = fill(Color.WHITE)
        repeat(count) {
            paint.alpha = 60 + r.nextInt(160)
            c.drawCircle(r.nextFloat() * 1920f, r.nextFloat() * bottom, 0.8f + r.nextFloat() * 1.6f, paint)
        }
    }

    private fun grain(c: Canvas, r: Random) {
        val paint = Paint()
        repeat(9000) {
            paint.color = if (r.nextBoolean()) Color.argb(10, 255, 255, 255) else Color.argb(12, 0, 0, 0)
            val x = r.nextFloat() * 1920f
            val y = r.nextFloat() * 1080f
            c.drawRect(x, y, x + 2f, y + 2f, paint)
        }
    }

    private fun coast(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 800f)
        haze(c, r, p.accent, 340f, 540f)
        ridge(c, r, 760f, 50f, 140f, 806f, fill(p.far))
        ground(c, p.ground, 800f)
        val glint = fill(p.sky.last(), 56)
        repeat(6) { c.drawRect(940f + r.nextFloat() * 300f, 826f + it * 24f, 1300f + r.nextFloat() * 200f, 829f + it * 24f, glint) }
        val cliff = Path().apply {
            moveTo(1380f, 1080f); lineTo(1440f, 780f); lineTo(1540f, 742f); lineTo(1620f, 690f); lineTo(1740f, 698f)
            lineTo(1920f, 640f); lineTo(1920f, 1080f); close()
        }
        c.drawPath(cliff, fill(p.near))
        c.drawRect(1648f, 500f, 1678f, 690f, fill(p.accent))
        c.drawRect(1648f, 548f, 1678f, 566f, fill(Color.parseColor("#8E3B33")))
        c.drawRect(1648f, 610f, 1678f, 628f, fill(Color.parseColor("#8E3B33")))
        c.drawRoundRect(RectF(1643f, 478f, 1683f, 502f), 2f, 2f, fill(Color.parseColor("#EDE0B9")))
        c.drawPath(Path().apply { moveTo(1638f, 478f); lineTo(1663f, 456f); lineTo(1688f, 478f); close() }, fill(p.near))
    }

    private fun snow(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 760f)
        stars(c, r, 400f, 60)
        ridge(c, r, 640f, 180f, 220f, 780f, fill(p.far))
        ground(c, p.ground, 760f)
        trees(c, r, 820f, 120f, p.near)
        c.drawRect(1270f, 842f, 1340f, 882f, fill(Color.parseColor("#1A2026")))
        c.drawPath(Path().apply { moveTo(1262f, 842f); lineTo(1305f, 816f); lineTo(1348f, 842f); close() }, fill(Color.parseColor("#2B333B")))
        c.drawRect(1292f, 854f, 1306f, 866f, fill(p.accent))
        c.drawRect(0f, 900f, 1920f, 1080f, fill(Color.parseColor("#1C232A"), 140))
    }

    private fun desert(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 692f)
        sun(c, 560f, 420f, 46f, p.accent)
        ground(c, p.ground, 690f)
        val mesa = fill(p.far)
        c.drawPath(Path().apply { moveTo(860f, 692f); lineTo(900f, 560f); lineTo(1050f, 552f); lineTo(1084f, 692f); close() }, mesa)
        c.drawPath(Path().apply { moveTo(1420f, 692f); lineTo(1462f, 500f); lineTo(1700f, 494f); lineTo(1752f, 592f); lineTo(1812f, 592f); lineTo(1834f, 692f); close() }, mesa)
        c.drawPath(Path().apply { moveTo(160f, 692f); lineTo(214f, 604f); lineTo(380f, 600f); lineTo(420f, 692f); close() }, fill(p.far, 210))
        c.drawPath(Path().apply { moveTo(1170f, 692f); lineTo(1236f, 692f); lineTo(1640f, 1080f); lineTo(820f, 1080f); close() }, fill(p.near))
        val dash = fill(p.accent)
        c.drawPath(Path().apply { moveTo(1201f, 704f); lineTo(1205f, 704f); lineTo(1209f, 728f); lineTo(1198f, 728f); close() }, dash)
        c.drawPath(Path().apply { moveTo(1196f, 760f); lineTo(1210f, 760f); lineTo(1217f, 806f); lineTo(1189f, 806f); close() }, dash)
        c.drawPath(Path().apply { moveTo(1184f, 868f); lineTo(1222f, 868f); lineTo(1236f, 958f); lineTo(1170f, 958f); close() }, dash)
    }

    private fun orchard(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 700f)
        haze(c, r, p.accent, 300f, 520f)
        ridge(c, r, 640f, 60f, 260f, 720f, fill(p.far))
        ground(c, p.ground, 700f)
        for (row in 0 until 5) {
            val y = 740f + row * 70f
            val rad = 18f + row * 7f
            val paint = fill(p.near, 200 + row * 10)
            var x = (row % 2) * 40f - 20f
            while (x < 1960f) {
                c.drawRect(x - 3f, y, x + 3f, y + rad * 1.4f, fill(p.near))
                c.drawCircle(x, y, rad, paint)
                x += 90f + row * 30f
            }
        }
        c.drawRoundRect(RectF(1500f, 640f, 1720f, 720f), 40f, 40f, fill(p.accent, 120))
    }

    private fun harbour(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 760f)
        sun(c, 1320f, 690f, 60f, p.accent)
        skyline(c, r, 760f, 220f, p.far, null)
        val crane = fill(p.near)
        for (x in listOf(420f, 760f)) {
            c.drawRect(x, 470f, x + 18f, 760f, crane)
            c.drawRect(x - 160f, 470f, x + 240f, 486f, crane)
        }
        ground(c, p.ground, 760f)
        val glint = fill(p.accent, 70)
        repeat(10) { val y = 790f + it * 26f; c.drawRect(1180f + r.nextFloat() * 60f, y, 1460f - r.nextFloat() * 60f, y + 3f, glint) }
    }

    private fun orbit(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 1080f)
        stars(c, r, 1080f, 260)
        val planet = Paint(Paint.ANTI_ALIAS_FLAG)
        planet.shader = RadialGradient(1500f, 1700f, 1000f, intArrayOf(p.far, p.ground[0], p.near), floatArrayOf(0f, 0.8f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(1500f, 1700f, 980f, planet)
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = p.accent; alpha = 150 }
        c.drawCircle(1500f, 1700f, 984f, rim)
        c.drawRect(1300f, 300f, 1440f, 312f, fill(p.accent, 200))
        c.drawRect(1360f, 270f, 1380f, 342f, fill(p.accent, 200))
    }

    private fun city(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 820f)
        stars(c, r, 300f, 40)
        skyline(c, Random(r.nextInt()), 760f, 320f, p.far, null)
        skyline(c, r, 860f, 420f, p.near, p.accent)
        ground(c, p.ground, 860f)
    }

    private fun tower(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 1080f)
        skyline(c, r, 900f, 200f, p.far, null)
        c.drawRect(1240f, 140f, 1460f, 1080f, fill(p.near))
        val lit = fill(p.accent, 190)
        for (floor in 0 until 30) {
            for (col in 0 until 6) {
                if (r.nextFloat() < 0.12f || floor == 21) c.drawRect(1256f + col * 34f, 170f + floor * 30f, 1278f + col * 34f, 186f + floor * 30f, lit)
            }
        }
        val fog = Paint()
        fog.shader = LinearGradient(0f, 760f, 0f, 1080f, Color.TRANSPARENT, Color.argb(150, 120, 130, 130), Shader.TileMode.CLAMP)
        c.drawRect(0f, 760f, 1920f, 1080f, fog)
    }

    private fun marsh(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 720f)
        sun(c, 1380f, 640f, 54f, p.accent)
        ground(c, p.ground, 718f)
        // Channels of water catching the sky, wider and further apart as they come nearer.
        val water = fill(p.sky.last(), 70)
        var y = 734f
        repeat(7) {
            val x = r.nextFloat() * 1300f
            val h = 2f + it * 1.2f
            c.drawRoundRect(RectF(x, y, x + 240f + r.nextFloat() * 520f, y + h), h / 2, h / 2, water)
            y += 22f + it * 18f
        }
        ridge(c, r, 718f, 20f, 160f, 724f, fill(p.far))
    }

    private fun radio(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 900f)
        stars(c, r, 520f, 120)
        ridge(c, r, 760f, 110f, 300f, 900f, fill(p.far))
        skyline(c, r, 900f, 90f, p.near, Color.parseColor("#E2B46A"), 200f, 1300f)
        ground(c, p.ground, 896f)
        val mast = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = p.near; strokeWidth = 6f }
        c.drawLine(1580f, 260f, 1520f, 780f, mast)
        c.drawLine(1580f, 260f, 1640f, 780f, mast)
        for (i in 0 until 10) { val y = 300f + i * 48f; val d = (y - 260f) * 0.115f; c.drawLine(1580f - d, y, 1580f + d, y, mast) }
        sun(c, 1580f, 252f, 7f, p.accent)
    }

    private fun street(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 820f)
        haze(c, r, p.accent, 200f, 480f)
        skyline(c, r, 820f, 520f, p.far, null, 0f, 700f)
        skyline(c, r, 820f, 520f, p.far, null, 1220f, 1920f)
        ground(c, p.ground, 820f)
        c.drawPath(Path().apply { moveTo(900f, 820f); lineTo(1020f, 820f); lineTo(1500f, 1080f); lineTo(420f, 1080f); close() }, fill(p.near))
        for (i in 0 until 4) {
            val x = 1130f + i * 120f; val top = 640f - i * 60f
            c.drawRect(x, top, x + 6f + i * 2f, 820f + i * 60f, fill(p.near))
            sun(c, x + 3f, top, 8f + i * 3f, p.accent)
        }
    }

    private fun lake(c: Canvas, p: Palette, r: Random) {
        sky(c, p.sky, 620f)
        val mountains = Path()
        val peaks = listOf(0f to 560f, 240f to 380f, 520f to 520f, 760f to 300f, 1040f to 500f, 1300f to 340f, 1600f to 520f, 1920f to 420f)
        mountains.moveTo(0f, 620f)
        peaks.forEach { (x, y) -> mountains.lineTo(x, y) }
        mountains.lineTo(1920f, 620f); mountains.close()
        c.drawPath(mountains, fill(p.far))
        ground(c, p.ground, 620f)
        val mirror = Path()
        mirror.moveTo(0f, 620f)
        peaks.forEach { (x, y) -> mirror.lineTo(x, 620f + (620f - y) * 0.8f) }
        mirror.lineTo(1920f, 620f); mirror.close()
        c.drawPath(mirror, fill(p.far, 120))
        val ice = fill(p.accent, 40)
        repeat(8) { val y = 660f + it * 52f; c.drawRect(0f, y, 1920f, y + 2f, ice) }
        c.drawRect(0f, 616f, 1920f, 624f, fill(p.near))
    }
}
