package ca.dstudio.marquee.ui.pages

import android.content.Context
import ca.dstudio.marquee.compose.Look
import ca.dstudio.marquee.settings.MarqueeSettings
import ca.dstudio.marquee.ui.panel.Page
import ca.dstudio.marquee.work.Generator

/** The current set as the settings show it: read off the main thread, kept here. */
data class SetSummary(val count: Int, val createdAt: Long)

/** What the pages need from the settings screen. */
interface SettingsHost {
    val context: Context
    val settings: MarqueeSettings

    /** The current set, or null while there is none (as of the last look at the store). */
    val set: SetSummary?

    /** The generator's state as of the last look. */
    val status: Generator.Status

    /** When Update now was last pressed here, or 0. */
    val updateRequestedAt: Long

    fun open(page: Page)

    /** Redraws the page on top. */
    fun refresh()

    /** Recomposes the picture behind the panel with [look]. */
    fun preview(look: Look)

    /** A look setting was stored: the set is redrawn when the Look page is left. */
    fun lookChanged()

    /** The Look page was left: redraws the set if a look setting changed there. */
    fun leftLook()

    /** A setting that changes which titles are picked was stored: a full run when settings close. */
    fun titlesChanged()

    /** The picture size was stored: the set is redrawn when settings close. */
    fun sizeChanged()

    /** The update interval was stored. */
    fun intervalChanged()

    fun updateNow()

    fun openExplore()
}
