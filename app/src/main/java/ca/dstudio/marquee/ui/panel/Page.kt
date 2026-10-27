package ca.dstudio.marquee.ui.panel

import androidx.annotation.DrawableRes

/**
 * One page of the panel: its header and its rows. [items] is called whenever the page is shown or
 * redrawn, so a page reads its state fresh each time and never holds on to rows.
 */
abstract class Page {

    @get:DrawableRes
    abstract val icon: Int

    abstract fun title(): String

    open fun subtitle(): String? = null

    abstract fun items(): List<Item>

    /** Called when the page is left for the one under it (BACK), not when another opens over it. */
    open fun onLeave() {}

    /** The row the cursor was on, so the page comes back to it. */
    internal var focusKey: String? = null
}
