package ca.dstudio.marquee.ui.panel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * A row's checkbox: a 40 px rounded box (design pixels) in a 48 px cell. Off, the box is a dim
 * white wash; on, it is filled with the cursor's blue and carries a white check. On the cursor's
 * blue row the colours swap: a white box with a blue check, or a brighter wash when off. It follows
 * its row's focus (duplicate parent state), so it is drawn from the row's state alone.
 */
@SuppressLint("ViewConstructor")
internal class CheckView(context: Context) : View(context) {

    var on: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val box = RectF()
    private val check = Path()

    init {
        isDuplicateParentStateEnabled = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(px(CELL), px(CELL))
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cursor = drawableState.contains(android.R.attr.state_focused)
        val unit = width / CELL
        val inset = (CELL - BOX) / 2f * unit
        box.set(inset, inset, width - inset, height - inset)
        fill.color = when {
            on && cursor -> Color.WHITE
            on -> Design.CURSOR
            cursor -> Design.white(0.28f)
            else -> Design.white(0.16f)
        }
        canvas.drawRoundRect(box, RADIUS * unit, RADIUS * unit, fill)
        if (!on) return
        // The check sits on the same proportions at every size: a short stroke down, a long one up.
        val s = BOX * unit
        check.reset()
        check.moveTo(box.left + 0.25f * s, box.top + 0.525f * s)
        check.lineTo(box.left + 0.425f * s, box.top + 0.70f * s)
        check.lineTo(box.left + 0.75f * s, box.top + 0.35f * s)
        stroke.strokeWidth = 0.12f * s
        stroke.color = if (cursor) Design.CURSOR else Color.WHITE
        canvas.drawPath(check, stroke)
    }

    private companion object {
        const val CELL = 48f
        const val BOX = 40f
        const val RADIUS = 9f
    }
}
