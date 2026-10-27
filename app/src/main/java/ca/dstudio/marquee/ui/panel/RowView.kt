package ca.dstudio.marquee.ui.panel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ca.dstudio.marquee.R

/**
 * One focusable row: a title (and a description under it), a value at the right, and a chevron or
 * a checkbox at the end. The cursor fills it blue; while its dropdown is open it is *activated* and
 * shows a quiet fill instead, so the list's cursor is the one highlight on screen.
 */
@SuppressLint("ViewConstructor")
internal class RowView(context: Context) : LinearLayout(context) {

    private val title = TextView(context)
    private val description = TextView(context)
    private val value = TextView(context)
    private val chevron = ImageView(context)
    private val check = CheckView(context)
    private val logoSpot = FrameLayout(context)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        minimumHeight = px(76f)
        setPadding(px(16f), px(14f), px(16f), px(14f))
        background = StateListDrawable().apply {
            // Activated before focused: an open dropdown's row keeps focus-like state in some
            // transitions, and its open look has to win.
            addState(intArrayOf(android.R.attr.state_activated), rounded(Design.OPEN_ROW, 12f))
            addState(intArrayOf(android.R.attr.state_focused), rounded(Design.CURSOR, 12f))
            addState(intArrayOf(), rounded(Color.TRANSPARENT, 12f))
        }

        val text = LinearLayout(context).apply {
            orientation = VERTICAL
            isDuplicateParentStateEnabled = true
        }
        title.apply {
            jost(32f, 300)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        description.apply {
            jost(23f, 400, lineHeight = 30f)
            isDuplicateParentStateEnabled = true
            setTextColor(Design.onCursor(Design.white(0.92f), Design.white(0.58f)))
            ellipsize = TextUtils.TruncateAt.END
        }
        text.addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        text.addView(description, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(4f)
        })
        addView(text, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        value.apply {
            jost(25f, 400)
            isDuplicateParentStateEnabled = true
            setTextColor(Design.onCursor(Design.white(0.92f), Design.white(0.66f)))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = px(230f)
            gravity = Gravity.END
        }
        addView(value, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = px(16f)
        })

        // Kept for a logo (TMDB's, on About); empty and zero-sized until one is put in it.
        addView(logoSpot, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        check.isDuplicateParentStateEnabled = true
        addView(check, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = px(16f)
        })

        chevron.apply {
            alpha = 0.6f
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        addView(chevron, LayoutParams(px(22f), px(22f)).apply { marginStart = px(16f) })
    }

    fun bind(item: Item) {
        var desc: String? = null
        var shown: String? = null
        var chevronRes = 0
        var hasCheck = false
        var descLines = 2
        when (item) {
            is Item.Link -> {
                title.text = item.title
                desc = item.description
                shown = item.value
                chevronRes = R.drawable.ic_chevron_right
            }
            is Item.Choice -> {
                title.text = item.title
                desc = item.description
                shown = item.shown
                // An up chevron only while its list is open (the row is activated then).
                chevronRes = if (isActivated) R.drawable.ic_chevron_up else 0
            }
            is Item.Toggle -> {
                title.text = item.title
                desc = item.description
                hasCheck = true
                check.on = item.on
            }
            is Item.Stepper -> {
                title.text = item.title
                desc = item.description
                shown = item.label(item.value)
            }
            is Item.Action -> {
                title.text = item.title
                desc = item.description
            }
            is Item.Info -> {
                title.text = item.title
                desc = item.description
                descLines = 4
            }
            is Item.Header -> error("a header is not a row")
        }
        description.text = desc
        description.maxLines = descLines
        description.visibility = if (desc.isNullOrEmpty()) View.GONE else View.VISIBLE
        value.text = shown
        value.visibility = if (shown.isNullOrEmpty()) View.GONE else View.VISIBLE
        check.visibility = if (hasCheck) View.VISIBLE else View.GONE
        logoSpot.visibility = if (item is Item.Info && item.logoSpot) View.VISIBLE else View.GONE
        if (chevronRes != 0) {
            chevron.setImageResource(chevronRes)
            chevron.visibility = View.VISIBLE
        } else {
            chevron.visibility = View.GONE
        }
    }

    /** Marks the row as the anchor of an open dropdown, or no longer. */
    fun setOpen(open: Boolean) {
        isActivated = open
        if (open) {
            chevron.setImageResource(R.drawable.ic_chevron_up)
            chevron.visibility = View.VISIBLE
        } else {
            chevron.visibility = View.GONE
        }
    }
}

/** A section's name over its rows: small capitals, not focusable. */
@SuppressLint("ViewConstructor")
internal class HeaderRowView(context: Context) : TextView(context) {
    init {
        jost(21f, 500, alpha = 0.5f, spacing = 0.1f)
        isAllCaps = true
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        isFocusable = false
        setPadding(px(16f), px(18f), px(16f), px(8f))
    }
}
