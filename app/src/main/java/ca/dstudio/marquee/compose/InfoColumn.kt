package ca.dstudio.marquee.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import ca.dstudio.marquee.catalog.TitleDetails

/** The parts every text layout shares: where the column goes, its rows, and the bottom panel. */
internal object InfoColumn {

    const val SIDE = 112f
    const val TOP = 140f
    const val WIDTH = 840f
    /** At 1080p a column at the top ends above this, clear of a grid in the lower half. */
    const val BOTTOM_LIMIT = 640f

    const val PANEL_X = 80f
    const val PANEL_BOTTOM = 80f
    const val PANEL_WIDTH = 940f
    const val PANEL_PAD_TOP = 48f
    const val PANEL_PAD_SIDE = 52f
    const val PANEL_PAD_BOTTOM = 48f
    const val PANEL_INNER = PANEL_WIDTH - 2 * PANEL_PAD_SIDE

    fun align(position: TextPosition) = if (position == TextPosition.TOP_RIGHT) Align.END else Align.START

    fun left(f: Frame, position: TextPosition) = if (position == TextPosition.TOP_RIGHT) f.w - SIDE - WIDTH else SIDE

    /** The logo fitted in [boxW] x [boxH], or the title set in type when there is none. */
    fun logoOrTitle(
        logo: Bitmap?,
        d: TitleDetails,
        labels: Labels,
        width: Float,
        boxW: Float,
        boxH: Float,
        titleSize: Float,
        titleMin: Float,
        theSize: Float,
    ): Block = if (logo != null) {
        ImageBlock(logo, boxW, boxH)
    } else {
        TitleBlock(d.name, labels.locale, width, titleSize, titleMin, theSize)
    }

    fun facts(items: List<String>, labels: Labels, size: Float, width: Float): Block? {
        if (items.isEmpty()) return null
        val paint = textPaint(Fonts.jost(500), size, alpha = 0.82f, spacingEm = 0.16f)
        return RowBlock(items.map { it.uppercase(labels.locale) }, paint, 30f, width, size)
    }

    fun ratings(d: TitleDetails, labels: Labels): Block? =
        labels.tmdbPercent(d)?.let { RatingsBlock(listOf(it to labels.tmdb), labels.locale) }

    fun synopsis(d: TitleDetails, size: Float, lines: Int, width: Float): Block? {
        if (lines <= 0 || d.overview.isBlank()) return null
        val paint = textPaint(Fonts.jost(300), size, alpha = 0.88f)
        return ParagraphBlock(d.overview, paint, width, lines, size * 1.48f).takeUnless { it.isEmpty }
    }

    /** Where the title streams when that is known, else the list it came from. */
    fun footer(d: TitleDetails, listLabel: String, labels: Labels, width: Float): Block =
        if (d.streaming.isNotEmpty()) {
            FooterBlock(labels.streamingOn, d.streaming.joinToString(", "), labels.locale, width)
        } else {
            FooterBlock(labels.from, listLabel, labels.locale, width)
        }

    fun kicker(text: String, labels: Labels, width: Float): Block =
        LineBlock(text.uppercase(labels.locale), textPaint(Fonts.jost(500), 22f, alpha = 0.7f, spacingEm = 0.22f), width, 22f)

    /** The bottom-left panel with [column] inside it. */
    fun drawPanel(c: Canvas, f: Frame, column: Column) {
        val h = PANEL_PAD_TOP + column.height + PANEL_PAD_BOTTOM
        val rect = RectF(PANEL_X, f.h - PANEL_BOTTOM - h, PANEL_X + PANEL_WIDTH, f.h - PANEL_BOTTOM)
        Shade.panel(c, rect)
        column.draw(c, PANEL_X + PANEL_PAD_SIDE, rect.top + PANEL_PAD_TOP)
    }

    /** Logo and facts on the panel; Cinematic adds two lines of synopsis. */
    fun panelColumn(d: TitleDetails, logo: Bitmap?, labels: Labels, facts: List<String>, synopsisLines: Int): Column =
        Column(PANEL_INNER, Align.START)
            .add(0f, logoOrTitle(logo, d, labels, PANEL_INNER, 760f, 150f, 76f, 56f, 24f))
            .add(22f, facts(facts, labels, 21f, PANEL_INNER))
            .add(22f, synopsis(d, 27f, synopsisLines, PANEL_INNER))
}
