package ca.dstudio.marquee.ui.explore

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.LruCache
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.dstudio.marquee.R
import ca.dstudio.marquee.catalog.Candidate
import ca.dstudio.marquee.catalog.Languages
import ca.dstudio.marquee.catalog.Media
import ca.dstudio.marquee.catalog.RunReport
import ca.dstudio.marquee.catalog.Skip
import ca.dstudio.marquee.compose.Fonts
import ca.dstudio.marquee.source.ImageKind
import ca.dstudio.marquee.source.Source
import ca.dstudio.marquee.ui.panel.Design
import ca.dstudio.marquee.ui.panel.FooterView
import ca.dstudio.marquee.ui.panel.Hint
import ca.dstudio.marquee.ui.panel.frame
import ca.dstudio.marquee.ui.panel.jost
import ca.dstudio.marquee.ui.panel.px
import ca.dstudio.marquee.ui.panel.pxf
import ca.dstudio.marquee.ui.panel.rounded
import java.util.Locale
import java.util.concurrent.ExecutorService

/**
 * Debug (the class keeps its first name, Explore): a wide dialog over the wallpaper listing every
 * list of the last run, its candidates in
 * list order - a poster, the title, `year · Film|Show · #rank` - and what became of each: "Shown",
 * or why it was left out. UP and DOWN move, BACK closes ([handleKey]).
 *
 * The report is read and the posters decoded on [io]; a poster is taken from the source cache only,
 * never fetched, and a plain tile stands in for one that is not there.
 */
@SuppressLint("ViewConstructor")
class ExploreView(
    context: Context,
    private val io: ExecutorService,
    private val onClose: () -> Unit,
) : FrameLayout(context) {

    private sealed class Row {
        class ListName(val text: String) : Row()
        class Note(val text: String) : Row()
        class Title(val c: Candidate, val line: String, val verdict: String, val shown: Boolean) : Row()
    }

    private val main = Handler(Looper.getMainLooper())
    private val titleView = TextView(context)
    private val subtitleView = TextView(context)
    private val list = RecyclerView(context)
    private val adapter = Rows()
    private var source: Source? = null
    private val posters = LruCache<String, Bitmap>(256)
    private val loading = HashSet<String>()
    private var closed = false

    init {
        setBackgroundColor(Design.VEIL)
        isClickable = true
        // Above the side panel, which is raised itself: the veil covers it too.
        elevation = pxf(96f)

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Design.SURFACE, 24f)
            foreground = frame(24f)
            elevation = pxf(32f)
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
        val head = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Design.BAND)
            setPadding(px(44f), px(30f), px(44f), px(30f))
        }
        titleView.jost(36f, 500)
        titleView.text = context.getString(R.string.explore_title)
        subtitleView.jost(24f, 400, alpha = 0.6f)
        subtitleView.maxLines = 2
        subtitleView.ellipsize = TextUtils.TruncateAt.END
        head.addView(titleView)
        head.addView(subtitleView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(6f)
        })
        card.addView(head)

        list.layoutManager = LinearLayoutManager(context)
        list.itemAnimator = null
        list.adapter = adapter
        list.isFocusable = false
        list.setPadding(px(28f), px(18f), px(28f), px(18f))
        list.clipToPadding = false
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.bottom = px(6f)
            }
        })
        card.addView(list, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val footer = FooterView(context)
        footer.show(listOf(Hint(context.getString(R.string.key_back), context.getString(R.string.hint_close))))
        card.addView(footer)

        addView(card, LayoutParams(px(1180f), LayoutParams.MATCH_PARENT, Gravity.CENTER).apply {
            topMargin = px(80f)
            bottomMargin = px(80f)
        })
    }

    /** Reads [report] for display, with [source] for the posters (either may be null). */
    fun show(report: RunReport?, source: Source?, age: String?) {
        this.source = source
        subtitleView.text = if (age != null) {
            context.getString(R.string.explore_subtitle, age)
        } else {
            context.getString(R.string.row_explore_description)
        }
        val rows = ArrayList<Row>()
        if (report == null || report.lists.isEmpty()) {
            rows += Row.Note(context.getString(R.string.explore_empty))
        } else {
            for (l in report.lists) {
                rows += Row.ListName(l.label)
                if (l.error != null && l.candidates.isEmpty()) rows += Row.Note(context.getString(R.string.explore_list_failed))
                for (c in l.candidates) rows += Row.Title(c, line(c), verdict(c, report), c.status == RunReport.USED)
            }
        }
        adapter.rows = rows
        adapter.notifyDataSetChanged()
        val first = rows.indexOfFirst { it is Row.Title }
        if (first >= 0) {
            list.scrollToPosition(0)
            list.post { list.findViewHolderForAdapterPosition(first)?.itemView?.requestFocus() }
        }
    }

    private fun line(c: Candidate): String {
        val kind = context.getString(if (Media.of(c.media) == Media.TV) R.string.explore_show else R.string.explore_film)
        return if (c.year != null) {
            context.getString(R.string.explore_line, c.year, kind, c.rank)
        } else {
            context.getString(R.string.explore_line_no_year, kind, c.rank)
        }
    }

    private fun verdict(c: Candidate, report: RunReport): String {
        val res = context.resources
        val language = languageName(report.lang)
        return when (c.status) {
            RunReport.USED -> res.getString(R.string.verdict_shown)
            Skip.OLDER.key -> res.getQuantityString(R.plurals.verdict_older, report.withinYears, report.withinYears)
            Skip.ADULT.key -> res.getString(R.string.verdict_adult)
            Skip.GENRE.key -> res.getString(R.string.verdict_genre)
            Skip.NO_LOGO.key -> res.getString(R.string.verdict_no_logo, language)
            Skip.NO_SYNOPSIS.key -> res.getString(R.string.verdict_no_synopsis, language)
            Skip.NO_BACKDROP.key -> res.getString(R.string.verdict_no_backdrop)
            Skip.REPEAT.key -> res.getString(R.string.verdict_repeat)
            Skip.DUPLICATE.key -> res.getString(R.string.verdict_duplicate)
            Skip.OVER_COUNT.key -> when {
                report.pick == "random" -> res.getString(R.string.verdict_not_drawn)
                report.titlesPerList > 0 -> res.getQuantityString(R.plurals.verdict_over_count, report.titlesPerList, report.titlesPerList)
                else -> res.getString(R.string.verdict_not_drawn)
            }
            else -> res.getString(R.string.verdict_failed)
        }
    }

    /** The run's language by name in the app's language ("English"), for the reasons that name it. */
    private fun languageName(tag: String): String {
        if (tag.isBlank()) return Locale.getDefault().getDisplayLanguage(Locale.getDefault())
        val appLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
        val name = Locale.forLanguageTag(tag).getDisplayLanguage(appLocale)
        return name.ifBlank { Languages.named(tag)?.name ?: tag }
    }

    /** BACK closes; UP and DOWN stay inside the list. True when it took the event. */
    fun handleKey(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        return when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (!down && !event.isCanceled) close()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> { if (down) move(-1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { if (down) move(1); true }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> true
            else -> false
        }
    }

    private fun move(delta: Int) {
        val rows = adapter.rows
        val focused = list.focusedChild
        val from = if (focused != null) list.getChildAdapterPosition(focused) else -1
        var at = from + delta
        while (at in rows.indices && rows[at] !is Row.Title) at += delta
        val lm = list.layoutManager as LinearLayoutManager
        if (at !in rows.indices) {
            if (delta < 0) list.smoothScrollToPosition(0)
            return
        }
        // Show the list's name above its first title.
        if (delta < 0 && at > 0 && rows[at - 1] is Row.ListName) list.smoothScrollToPosition(at - 1)
        val holder = list.findViewHolderForAdapterPosition(at)
        if (holder != null && lm.isViewPartiallyVisible(holder.itemView, true, true)) {
            holder.itemView.requestFocus()
        } else {
            lm.scrollToPosition(at)
            list.post { list.findViewHolderForAdapterPosition(at)?.itemView?.requestFocus() }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        onClose()
    }

    private fun poster(c: Candidate, into: ImageView) {
        val ref = c.poster
        into.tag = ref
        if (ref == null) {
            into.setImageDrawable(null)
            return
        }
        posters.get(ref)?.let {
            into.setImageBitmap(it)
            return
        }
        into.setImageDrawable(null)
        if (!loading.add(ref)) return
        val src = source ?: return
        val w = px(64f)
        val h = px(96f)
        io.execute {
            val bitmap = try {
                src.cachedImage(ref, ImageKind.POSTER)?.let { decode(it.path, w, h) }
            } catch (e: Exception) {
                null
            }
            main.post {
                loading.remove(ref)
                if (bitmap == null || closed) return@post
                posters.put(ref, bitmap)
                for (i in 0 until list.childCount) {
                    val v = list.getChildAt(i)
                    v.findViewWithTag<ImageView>(ref)?.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun decode(path: String, w: Int, h: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private inner class Rows : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var rows: List<Row> = emptyList()

        override fun getItemCount() = rows.size

        override fun getItemViewType(position: Int) = when (rows[position]) {
            is Row.ListName -> 0
            is Row.Note -> 1
            is Row.Title -> 2
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View = when (viewType) {
                0 -> TextView(parent.context).apply {
                    typeface = Fonts.fraunces(500)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f)
                    setTextColor(Color.WHITE)
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(px(16f), px(26f), px(16f), px(16f))
                }
                1 -> TextView(parent.context).apply {
                    jost(25f, 400, alpha = 0.6f)
                    setPadding(px(16f), px(12f), px(16f), px(12f))
                }
                else -> TitleRow(parent.context)
            }
            view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val row = rows[position]) {
                is Row.ListName -> (holder.itemView as TextView).text = row.text
                is Row.Note -> (holder.itemView as TextView).text = row.text
                is Row.Title -> (holder.itemView as TitleRow).bind(row)
            }
        }
    }

    /** One candidate: poster, title, its line, and the verdict at the right. */
    private inner class TitleRow(context: Context) : LinearLayout(context) {
        private val poster = ImageView(context)
        private val title = TextView(context)
        private val line = TextView(context)
        private val verdict = TextView(context)

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            setPadding(px(16f), px(12f), px(16f), px(12f))
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_focused), rounded(Design.LIGHT_CURSOR, 10f))
                addState(intArrayOf(), rounded(Color.TRANSPARENT, 10f))
            }
            poster.scaleType = ImageView.ScaleType.CENTER_CROP
            poster.background = rounded(Design.PLAIN_TILE, 4f)
            poster.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, pxf(4f))
                }
            }
            poster.clipToOutline = true
            addView(poster, LayoutParams(px(64f), px(96f)))

            val text = LinearLayout(context).apply { orientation = VERTICAL }
            title.jost(30f, 400)
            title.isDuplicateParentStateEnabled = true
            title.setTextColor(Design.onCursor(Design.DARK_TEXT, Color.WHITE))
            title.maxLines = 1
            title.ellipsize = TextUtils.TruncateAt.END
            line.jost(21f, 400, spacing = 0.04f)
            line.isDuplicateParentStateEnabled = true
            line.setTextColor(Design.onCursor(Design.withAlpha(Design.DARK_TEXT, 0.7f), Design.white(0.52f)))
            line.maxLines = 1
            text.isDuplicateParentStateEnabled = true
            text.addView(title)
            text.addView(line, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = px(8f) })
            addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = px(22f) })

            verdict.jost(22f, 400)
            verdict.isDuplicateParentStateEnabled = true
            verdict.maxLines = 1
            addView(verdict, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = px(22f) })
        }

        fun bind(row: Row.Title) {
            title.text = row.c.title
            line.text = row.line
            verdict.text = row.verdict
            verdict.setTextColor(
                Design.onCursor(
                    Design.withAlpha(Design.DARK_TEXT, if (row.shown) 1f else 0.7f),
                    if (row.shown) Color.WHITE else Design.white(0.5f),
                ),
            )
            poster(row.c, poster)
        }
    }
}
