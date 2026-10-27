package ca.dstudio.marquee.ui.panel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ca.dstudio.marquee.R

/**
 * The settings' side panel: a header band (icon, title, subtitle), the page's rows, and a footer
 * naming the keys. It keeps a stack of [Page]s; every key the panel answers is handled here, so the
 * cursor never leaves it: UP and DOWN move between rows, OK acts on the row, LEFT and RIGHT change
 * a number, BACK closes an open list, else goes back a page, else asks the host to close.
 */
@SuppressLint("ViewConstructor")
class PanelView(context: Context, private val onLastPage: () -> Unit) : LinearLayout(context) {

    private val stack = ArrayList<Page>()

    private val iconView = ImageView(context)
    private val titleView = TextView(context)
    private val subtitleView = TextView(context)
    private val listFrame = FrameLayout(context)
    private val list = RecyclerView(context)
    private val rows = Rows()
    private val dropdown = DropdownView(context) { key -> afterDropdown(key) }
    private val footer = FooterView(context)

    private var focusedKey: String? = null
    private var refreshPending = false

    /** The page on top, or null. */
    val page: Page? get() = stack.lastOrNull()

    init {
        orientation = VERTICAL
        background = rounded(Design.SURFACE, 22f)
        foreground = frame(22f)
        elevation = pxf(32f)
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true

        addView(header(), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        list.layoutManager = LinearLayoutManager(context)
        list.itemAnimator = null
        list.adapter = rows
        list.isFocusable = false
        list.setPadding(px(18f), px(14f), px(18f), px(14f))
        list.clipToPadding = false
        list.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.bottom = px(2f)
            }
        })
        listFrame.addView(list, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        listFrame.addView(dropdown)
        addView(listFrame, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun header(): View {
        val band = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Design.BAND)
            setPadding(px(30f), px(26f), px(30f), px(26f))
        }
        val tile = FrameLayout(context).apply { background = rounded(Design.ICON_TILE, 12f) }
        tile.addView(iconView, FrameLayout.LayoutParams(px(32f), px(32f), Gravity.CENTER))
        band.addView(tile, LayoutParams(px(56f), px(56f)))
        val text = LinearLayout(context).apply { orientation = VERTICAL }
        titleView.apply {
            jost(36f, 500)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        subtitleView.apply {
            jost(24f, 400, alpha = 0.6f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        text.addView(titleView)
        text.addView(subtitleView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(6f)
        })
        band.addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = px(20f) })
        return band
    }

    /** Opens [next] over the current page; the current one keeps its cursor for when it comes back. */
    fun push(next: Page) {
        page?.focusKey = focusedKey
        stack += next
        render(next.focusKey, fresh = true)
    }

    /** Goes back a page; false when this was the last one. */
    fun back(): Boolean {
        val top = page ?: return false
        if (stack.size == 1) return false
        stack.removeAt(stack.lastIndex)
        top.onLeave()
        render(page?.focusKey, fresh = true)
        return true
    }

    /** Redraws the page on top from its current state, keeping the cursor on the same row. */
    fun refresh() {
        if (dropdown.isOpen) {
            refreshPending = true
            return
        }
        render(focusedKey, fresh = false)
    }

    /** Puts the cursor back on the row it was on, after something over the panel took it. */
    fun restoreFocus() = focusOn(focusedKey)

    private fun render(focus: String?, fresh: Boolean) {
        val top = page ?: return
        iconView.setImageResource(top.icon)
        titleView.text = top.title()
        val sub = top.subtitle()
        subtitleView.text = sub
        subtitleView.visibility = if (sub.isNullOrEmpty()) View.GONE else View.VISIBLE

        val next = top.items()
        val old = rows.items
        if (!fresh && old.map { it.key } == next.map { it.key }) {
            rows.items = next
            next.forEachIndexed { i, item -> if (item.signature != old[i].signature) rows.notifyItemChanged(i, REBIND) }
            focusedItem()?.let(::showHints)
            return
        }
        rows.items = next
        rows.notifyDataSetChanged()
        if (fresh) list.scrollToPosition(0)
        focusOn(focus)
    }

    /** Puts the cursor on the row named [key], or the page's first row. */
    private fun focusOn(key: String?) {
        val items = rows.items
        val first = items.indexOfFirst { it !is Item.Header }
        if (first < 0) {
            focusedKey = null
            showHints(null)
            return
        }
        val at = items.indexOfFirst { it.key == key && it !is Item.Header }.takeIf { it >= 0 } ?: first
        focusedKey = items[at].key
        showHints(items[at])
        reveal(at, first)
    }

    private fun reveal(at: Int, first: Int) {
        val lm = list.layoutManager as LinearLayoutManager
        val holder = list.findViewHolderForAdapterPosition(at)
        if (holder != null && lm.isViewPartiallyVisible(holder.itemView, true, true) && !list.isLayoutRequested) {
            holder.itemView.requestFocus()
            if (at == first) list.smoothScrollToPosition(0)
            return
        }
        if (at == first) lm.scrollToPositionWithOffset(0, 0) else lm.scrollToPosition(at)
        list.post {
            val h = list.findViewHolderForAdapterPosition(at)
            if (h != null) {
                h.itemView.requestFocus()
            } else {
                lm.scrollToPosition(at)
                list.post { list.findViewHolderForAdapterPosition(at)?.itemView?.requestFocus() }
            }
        }
    }

    private fun focusedItem(): Item? = rows.items.firstOrNull { it.key == focusedKey }

    /** The keys the panel answers; true when it took the event. */
    fun handleKey(event: KeyEvent): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        if (dropdown.isOpen) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> { if (down) dropdown.move(-1); true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { if (down) dropdown.move(1); true }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> true
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    if (!down) dropdown.cancel()
                    true
                }
                else -> false
            }
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { if (down) move(-1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { if (down) move(1); true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { if (down) step(-1); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { if (down) step(1); true }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (!down && !event.isCanceled && !back()) onLastPage()
                true
            }
            else -> false
        }
    }

    /** The cursor to the next row up or down that is not a header, scrolling it into view. */
    private fun move(delta: Int) {
        val items = rows.items
        val from = items.indexOfFirst { it.key == focusedKey }
        if (from < 0) {
            focusOn(null)
            return
        }
        var at = from + delta
        while (at in items.indices && items[at] is Item.Header) at += delta
        if (at !in items.indices) {
            // At the top, still bring a header above the first row into view.
            if (delta < 0) list.smoothScrollToPosition(0)
            return
        }
        val first = items.indexOfFirst { it !is Item.Header }
        focusedKey = items[at].key
        showHints(items[at])
        reveal(at, first)
    }

    private fun step(direction: Int) {
        val item = focusedItem() as? Item.Stepper ?: return
        val next = (item.value + direction * item.step).coerceIn(item.min, item.max)
        if (next != item.value) item.onChange(next)
    }

    private fun onRowClick(item: Item, view: RowView) {
        when (item) {
            is Item.Link -> item.onOpen()
            is Item.Toggle -> item.onToggle(!item.on)
            is Item.Action -> item.onPress()
            is Item.Choice -> {
                dropdown.open(view, item, listFrame)
                showHints(null)
            }
            is Item.Stepper, is Item.Info, is Item.Header -> Unit
        }
    }

    private fun afterDropdown(key: String) {
        if (refreshPending) {
            refreshPending = false
            render(key, fresh = false)
        }
        val at = rows.items.indexOfFirst { it.key == key }
        if (at >= 0) {
            focusedKey = key
            list.findViewHolderForAdapterPosition(at)?.itemView?.requestFocus()
            showHints(rows.items[at])
        } else {
            focusOn(null)
        }
    }

    private fun showHints(item: Item?) {
        val res = context.resources
        val ok = res.getString(R.string.key_ok)
        val hints = ArrayList<Hint>()
        if (dropdown.isOpen) {
            hints += Hint(ok, res.getString(R.string.hint_choose))
            hints += Hint(res.getString(R.string.key_back), res.getString(R.string.hint_close_list))
            footer.show(hints)
            return
        }
        when (item) {
            is Item.Link -> hints += Hint(ok, res.getString(R.string.hint_open))
            is Item.Toggle -> hints += Hint(ok, res.getString(R.string.hint_switch))
            is Item.Choice -> hints += Hint(ok, res.getString(R.string.hint_choose))
            is Item.Action -> hints += Hint(ok, res.getString(R.string.hint_start))
            is Item.Stepper -> hints += Hint(
                res.getString(R.string.key_left_right),
                res.getString(R.string.hint_change),
                listOf(R.drawable.ic_key_left, R.drawable.ic_key_right),
            )
            else -> Unit
        }
        val back = if (stack.size > 1) R.string.hint_back else R.string.hint_close
        hints += Hint(res.getString(R.string.key_back), res.getString(back))
        footer.show(hints)
    }

    private inner class Rows : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var items: List<Item> = emptyList()

        private val ids = HashMap<String, Long>()

        init {
            setHasStableIds(true)
        }

        override fun getItemCount() = items.size

        override fun getItemId(position: Int): Long = ids.getOrPut(items[position].key) { ids.size.toLong() }

        override fun getItemViewType(position: Int) = if (items[position] is Item.Header) HEADER else ROW

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View = if (viewType == HEADER) HeaderRowView(parent.context) else RowView(parent.context)
            view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
            val holder = object : RecyclerView.ViewHolder(view) {}
            if (view is RowView) {
                view.setOnClickListener {
                    val at = holder.bindingAdapterPosition
                    if (at >= 0) onRowClick(items[at], view)
                }
                view.onFocusChangeListener = View.OnFocusChangeListener { _, has ->
                    val at = holder.bindingAdapterPosition
                    if (has && at >= 0 && !dropdown.isOpen) {
                        focusedKey = items[at].key
                        showHints(items[at])
                    }
                }
            }
            return holder
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is Item.Header -> (holder.itemView as HeaderRowView).text = item.text
                else -> (holder.itemView as RowView).bind(item)
            }
        }
    }

    private companion object {
        const val HEADER = 0
        const val ROW = 1
        val REBIND = Any()
    }
}
