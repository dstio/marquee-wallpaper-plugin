package ca.dstudio.marquee.ui.panel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A key the footer names, and what pressing it does here. [icons] draws the key as glyphs (the
 * arrow keys) instead of its name.
 */
class Hint(val key: String, val action: String, val icons: List<Int> = emptyList())

/**
 * The panel's footer: a key chip (white, dark text) followed by what the key does, for each key
 * that does something on the row under the cursor. A thin line above it parts it from the list.
 */
@SuppressLint("ViewConstructor")
internal class FooterView(context: Context) : LinearLayout(context) {

    private val line = Paint().apply { color = Design.FRAME }
    private var shown: List<Pair<String, String>> = emptyList()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Design.FOOTER)
        setPadding(px(30f), px(18f), px(30f), px(18f))
        setWillNotDraw(false)
    }

    fun show(hints: List<Hint>) {
        val next = hints.map { (it.key + it.icons) to it.action }
        if (next == shown) return
        shown = next
        removeAllViews()
        hints.forEachIndexed { i, hint ->
            addView(if (hint.icons.isEmpty()) chip(hint.key) else iconChip(hint.icons), LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                if (i > 0) marginStart = px(28f)
            })
            addView(TextView(context).apply {
                jost(22f, 400, alpha = 0.7f)
                maxLines = 1
                text = hint.action
            }, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = px(14f)
            })
        }
    }

    private fun chip(key: String) = TextView(context).apply {
        jost(18f, 500)
        setTextColor(Design.CHIP_TEXT)
        background = rounded(Design.CHIP, 8f)
        gravity = Gravity.CENTER
        minWidth = px(54f)
        minHeight = px(36f)
        setPadding(px(10f), 0, px(10f), 0)
        maxLines = 1
        text = key
    }

    private fun iconChip(icons: List<Int>) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        background = rounded(Design.CHIP, 8f)
        minimumWidth = px(54f)
        minimumHeight = px(36f)
        setPadding(px(8f), 0, px(8f), 0)
        for (res in icons) {
            addView(ImageView(context).apply {
                setImageResource(res)
                setColorFilter(Design.CHIP_TEXT)
            }, LayoutParams(px(22f), px(22f)))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), 1f, line)
    }
}
