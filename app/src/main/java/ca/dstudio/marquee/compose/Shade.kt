package ca.dstudio.marquee.compose

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/**
 * The shades laid over a backdrop so the text reads on any picture: linear gradients from one edge,
 * given as stops of (position from that edge, opacity).
 */
internal object Shade {

    enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

    fun gradient(c: Canvas, w: Float, h: Float, edge: Edge, stops: List<Pair<Float, Float>>, rgb: Int = Color.BLACK) {
        val (x0, y0, x1, y1) = when (edge) {
            Edge.LEFT -> listOf(0f, 0f, w, 0f)
            Edge.RIGHT -> listOf(w, 0f, 0f, 0f)
            Edge.TOP -> listOf(0f, 0f, 0f, h)
            Edge.BOTTOM -> listOf(0f, h, 0f, 0f)
        }
        val colors = stops.map { (_, a) ->
            Color.argb((a * 255).toInt().coerceIn(0, 255), Color.red(rgb), Color.green(rgb), Color.blue(rgb))
        }.toIntArray()
        val positions = stops.map { it.first }.toFloatArray()
        val paint = Paint(Paint.DITHER_FLAG)
        paint.shader = LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
    }

    /** Cinematic and Poster wall's bottom shade: black .72 at the edge to nothing 44 % up. */
    fun bottom(c: Canvas, w: Float, h: Float) = gradient(c, w, h, Edge.BOTTOM, listOf(0f to 0.72f, 0.44f to 0f))

    /** Cinematic's side shade, from the text's side. */
    fun cinematicSide(c: Canvas, w: Float, h: Float, edge: Edge) =
        gradient(c, w, h, edge, listOf(0f to 0.82f, 0.32f to 0.58f, 0.60f to 0f))

    /** Minimal's softer side shade. */
    fun minimalSide(c: Canvas, w: Float, h: Float, edge: Edge) = gradient(c, w, h, edge, listOf(0f to 0.6f, 0.42f to 0f))

    /** Behind the bottom panel, for a grid at the top. */
    fun top(c: Canvas, w: Float, h: Float) = gradient(c, w, h, Edge.TOP, listOf(0f to 0.62f, 0.34f to 0f))

    /** The solid panel the text sits on at the bottom left. */
    fun panel(c: Canvas, rect: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.argb((0.82f * 255).toInt(), 0x0C, 0x0D, 0x0F)
        c.drawRoundRect(rect, 14f, 14f, paint)
    }
}
