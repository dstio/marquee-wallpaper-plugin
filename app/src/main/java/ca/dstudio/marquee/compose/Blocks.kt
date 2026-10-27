package ca.dstudio.marquee.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import java.util.Locale

// The pieces an info column is built from. Sizes are in the 1920 x 1080 design space; a line's
// height and baseline follow the CSS line box the design page is drawn with, so a block measures
// the same as the page does.

internal enum class Align { START, END }

internal fun textPaint(
    face: Typeface,
    size: Float,
    alpha: Float = 1f,
    spacingEm: Float = 0f,
    color: Int = Color.WHITE,
): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
    typeface = face
    textSize = size
    letterSpacing = spacingEm
    this.color = color
    this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
}

/** The baseline of a line whose CSS line box starts at [top] and is [lineHeight] tall. */
internal fun baseline(paint: Paint, top: Float, lineHeight: Float): Float {
    val fm = paint.fontMetrics
    return top + (lineHeight - (fm.descent - fm.ascent)) / 2f - fm.ascent
}

/** CSS `line-height: normal` for [paint]. */
internal fun normalLineHeight(paint: Paint): Float = paint.fontMetrics.let { it.descent - it.ascent + it.leading }

/**
 * [text] broken into at most [maxLines] lines of [width], the last one ellipsized when the text
 * does not fit.
 */
internal fun breakLines(text: String, paint: TextPaint, width: Float, maxLines: Int): List<String> {
    if (text.isEmpty() || maxLines <= 0) return emptyList()
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    return (0 until minOf(layout.lineCount, maxLines)).map { i ->
        val start = layout.getLineStart(i)
        if (layout.getEllipsisCount(i) > 0) {
            // Cut back to the last whole word, as long as that keeps most of the line.
            val end = (start + layout.getEllipsisStart(i)).coerceAtMost(text.length)
            var shown = text.substring(start, end)
            val cutMidWord = end < text.length && !text[end].isWhitespace()
            val space = shown.lastIndexOf(' ')
            if (cutMidWord && space > shown.length / 2) shown = shown.substring(0, space)
            shown.trimEnd().trimEnd(',', ';', ':', '-') + "…"
        } else {
            text.substring(start, layout.getLineEnd(i).coerceAtMost(text.length)).trim()
        }
    }
}

/** [text] cut to [width] with an ellipsis when it is wider. */
internal fun fitLine(text: String, paint: TextPaint, width: Float): String =
    TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()

private fun Canvas.drawAligned(text: String, paint: Paint, left: Float, width: Float, align: Align, y: Float) {
    val x = if (align == Align.START) left else left + width - paint.measureText(text)
    drawText(text, x, y, paint)
}

internal interface Block {
    val height: Float
    fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align)
}

/** A column of blocks, each with its margin above. */
internal class Column(private val width: Float, private val align: Align) {
    private val items = ArrayList<Pair<Float, Block>>()

    fun add(marginTop: Float, block: Block?): Column {
        if (block != null) items += (if (items.isEmpty()) 0f else marginTop) to block
        return this
    }

    val height: Float get() = items.sumOf { (m, b) -> (m + b.height).toDouble() }.toFloat()

    fun draw(c: Canvas, left: Float, top: Float) {
        var y = top
        for ((m, b) in items) {
            y += m
            b.draw(c, left, y, width, align)
            y += b.height
        }
    }
}

/** A picture (the title's logo) fitted inside [boxW] x [boxH], top-aligned. */
internal class ImageBlock(private val bitmap: Bitmap, boxW: Float, boxH: Float) : Block {
    private val scale = minOf(boxW / bitmap.width, boxH / bitmap.height)
    private val w = bitmap.width * scale
    override val height = bitmap.height * scale

    override fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align) {
        val x = if (align == Align.START) left else left + width - w
        c.drawBitmap(bitmap, null, RectF(x, top, x + w, top + height), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }
}

/**
 * The title set in Fraunces when there is no logo: upper case, two lines at most, shrunk in 4 px
 * steps from [maxSize] to [minSize] until it fits; a leading "The" goes on a line of its own.
 */
internal class TitleBlock(
    name: String,
    locale: Locale,
    width: Float,
    maxSize: Float,
    minSize: Float,
    theSize: Float,
) : Block {
    private val the: String?
    private val lines: List<String>
    private val paint: TextPaint
    private val thePaint = textPaint(Fonts.fraunces(400), theSize, spacingEm = 0.5f, color = Color.parseColor("#D9D1C2"))
    private val lineHeight: Float
    private val theLineHeight = theSize * LINE_HEIGHT
    override val height: Float

    init {
        val match = Regex("^(the)\\s+(.+)$", RegexOption.IGNORE_CASE).find(name.trim())
        val rest = (match?.groupValues?.get(2) ?: name.trim()).uppercase(locale)
        the = match?.groupValues?.get(1)?.uppercase(locale)
        var size = maxSize
        var chosen: List<String>? = null
        var p = mainPaint(size)
        while (size >= minSize) {
            p = mainPaint(size)
            val words = rest.split(Regex("\\s+"))
            if (words.all { p.measureText(it) <= width }) {
                val broken = breakLines(rest, p, width, 3)
                if (broken.size <= 2) {
                    chosen = broken
                    break
                }
            }
            size -= 4f
        }
        if (chosen == null) {
            size = minSize
            p = mainPaint(size)
            chosen = breakLines(rest, p, width, 2)
        }
        paint = p
        lines = chosen
        lineHeight = size * LINE_HEIGHT
        height = (if (the != null) theLineHeight + THE_GAP else 0f) + lines.size * lineHeight
    }

    private fun mainPaint(size: Float) =
        textPaint(Fonts.fraunces(500), size, spacingEm = 0.05f, color = Color.parseColor("#F2EEE6"))

    override fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align) {
        var y = top
        if (the != null) {
            c.drawAligned(the, thePaint, left, width, align, baseline(thePaint, y, theLineHeight))
            y += theLineHeight + THE_GAP
        }
        for (line in lines) {
            c.drawAligned(line, paint, left, width, align, baseline(paint, y, lineHeight))
            y += lineHeight
        }
    }

    private companion object {
        const val LINE_HEIGHT = 0.92f
        const val THE_GAP = 14f
    }
}

/** One line of text, ellipsized to the column. */
internal class LineBlock(text: String, private val paint: TextPaint, width: Float, private val lineHeight: Float) : Block {
    private val text = fitLine(text, paint, width)
    override val height = lineHeight

    override fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align) {
        c.drawAligned(text, paint, left, width, align, baseline(paint, top, lineHeight))
    }
}

/** A paragraph of at most [maxLines] lines, ellipsized. */
internal class ParagraphBlock(text: String, private val paint: TextPaint, width: Float, maxLines: Int, private val lineHeight: Float) : Block {
    private val lines = breakLines(text, paint, width, maxLines)
    override val height = lines.size * lineHeight
    val isEmpty: Boolean get() = lines.isEmpty()

    override fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align) {
        lines.forEachIndexed { i, line ->
            c.drawAligned(line, paint, left, width, align, baseline(paint, top + i * lineHeight, lineHeight))
        }
    }
}

/** Items in a row [gap] apart, wrapping onto more rows when they do not fit. */
internal class RowBlock(items: List<String>, private val paint: TextPaint, private val gap: Float, width: Float, private val lineHeight: Float) : Block {
    private val rows: List<List<Pair<String, Float>>>
    override val height: Float

    init {
        val out = ArrayList<MutableList<Pair<String, Float>>>()
        var used = 0f
        for (raw in items) {
            val text = fitLine(raw, paint, width)
            val w = paint.measureText(text)
            val row = out.lastOrNull()
            if (row == null || used + gap + w > width) {
                out += mutableListOf(text to w)
                used = w
            } else {
                row += text to w
                used += gap + w
            }
        }
        rows = out
        height = rows.size * lineHeight
    }

    override fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align) {
        rows.forEachIndexed { r, row ->
            val total = row.sumOf { it.second.toDouble() }.toFloat() + gap * (row.size - 1)
            var x = if (align == Align.START) left else left + width - total
            val y = baseline(paint, top + r * lineHeight, lineHeight)
            for ((text, w) in row) {
                c.drawText(text, x, y, paint)
                x += w + gap
            }
        }
    }
}

/**
 * Ratings as figures: value and label on one baseline, 10 apart; several are divided by a hairline
 * with 26 on each side. Right-aligned, every rating has the hairline on its left.
 */
internal class RatingsBlock(private val ratings: List<Pair<String, String>>, locale: Locale) : Block {
    private val value = textPaint(Fonts.jost(500), 36f)
    private val label = textPaint(Fonts.jost(400), 19f, alpha = 0.62f, spacingEm = 0.12f)
    private val labels = ratings.map { it.second.uppercase(locale) }
    private val line = Paint().apply { color = Color.WHITE; alpha = (0.28f * 255).toInt() }
    override val height = LINE

    private fun itemWidth(i: Int) = value.measureText(ratings[i].first) + GAP + label.measureText(labels[i])

    override fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align) {
        val y = baseline(value, top, LINE)
        val leadingRule = align == Align.END
        var total = 0f
        for (i in ratings.indices) {
            if (i > 0 || leadingRule) total += PAD + 1f + PAD
            total += itemWidth(i)
        }
        var x = if (align == Align.START) left else left + width - total
        for (i in ratings.indices) {
            if (i > 0 || leadingRule) {
                x += PAD
                c.drawRect(x, top, x + 1f, top + LINE, line)
                x += 1f + PAD
            }
            c.drawText(ratings[i].first, x, y, value)
            x += value.measureText(ratings[i].first) + GAP
            c.drawText(labels[i], x, y, label)
            x += label.measureText(labels[i])
        }
    }

    private companion object {
        const val LINE = 36f
        const val GAP = 10f
        const val PAD = 26f
    }
}

/** "FROM" and where the title came from, on one baseline, 18 apart. */
internal class FooterBlock(labelText: String, valueText: String, locale: Locale, width: Float) : Block {
    private val label = textPaint(Fonts.jost(400), 19f, alpha = 0.58f, spacingEm = 0.16f)
    private val value = textPaint(Fonts.jost(500), 25f, alpha = 0.9f)
    private val labelText = labelText.uppercase(locale)
    private val valueText = fitLine(valueText, value, width - label.measureText(this.labelText) - GAP)
    override val height = normalLineHeight(value)

    override fun draw(c: Canvas, left: Float, top: Float, width: Float, align: Align) {
        val total = label.measureText(labelText) + GAP + value.measureText(valueText)
        var x = if (align == Align.START) left else left + width - total
        val y = baseline(value, top, height)
        c.drawText(labelText, x, y, label)
        x += label.measureText(labelText) + GAP
        c.drawText(valueText, x, y, value)
    }

    private companion object {
        const val GAP = 18f
    }
}
